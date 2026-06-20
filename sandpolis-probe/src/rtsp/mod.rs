#[cfg(any(feature = "agent", feature = "server"))]
use anyhow::Result;
#[cfg(any(feature = "agent", feature = "server"))]
use retina::client::{SessionGroup, SetupOptions};
#[cfg(any(feature = "agent", feature = "server"))]
use retina::codec::CodecItem;
#[cfg(any(feature = "agent", feature = "server"))]
use sandpolis_instance::network::StreamResponder;
#[cfg(any(feature = "agent", feature = "server"))]
use sandpolis_macros::Stream;
use serde::{Deserialize, Serialize};
#[cfg(any(feature = "agent", feature = "server"))]
use std::sync::Arc;
#[cfg(any(feature = "agent", feature = "server"))]
use tokio::sync::RwLock;
#[cfg(any(feature = "agent", feature = "server"))]
use tokio::sync::mpsc::Sender;
#[cfg(any(feature = "agent", feature = "server"))]
use tracing::debug;
#[cfg(any(feature = "agent", feature = "server"))]
use url::Url;

/// Request message for RTSP stream sessions.
#[derive(Serialize, Deserialize)]
pub enum RtspSessionStreamRequest {
    /// Start streaming from the given RTSP URL
    Start {
        /// Full RTSP URL (e.g., rtsp://user:pass@host:554/stream)
        url: String,

        /// Transport protocol preference
        transport: RtspTransport,
    },
    /// Stop the stream
    Stop,
}

/// Transport protocol for RTSP streaming.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, Default)]
pub enum RtspTransport {
    /// UDP transport (lower latency, may have packet loss)
    Udp,
    /// TCP interleaved transport (more reliable)
    #[default]
    Tcp,
}

/// Response message containing video/audio frame data.
#[derive(Serialize, Deserialize)]
pub struct RtspSessionStreamResponse {
    /// The stream index (0 for video, 1 for audio typically)
    pub stream_index: usize,

    /// Frame data
    pub frame: RtspFrame,
}

/// A single frame from the RTSP stream.
#[derive(Serialize, Deserialize)]
pub enum RtspFrame {
    /// Codec parameters (sent once before frames). For H.264 `extra_data` is the
    /// AVCDecoderConfigurationRecord containing SPS/PPS, which the client needs to
    /// initialize its decoder.
    Parameters {
        /// AVCDecoderConfigurationRecord (AVCC) bytes.
        extra_data: Vec<u8>,
        /// Display width in pixels.
        width: u32,
        /// Display height in pixels.
        height: u32,
    },
    /// H.264 video frame
    H264 {
        /// NAL units
        data: Vec<Vec<u8>>,
        /// Presentation timestamp in 90kHz units
        timestamp: i64,
        /// Whether this is a keyframe (IDR)
        is_keyframe: bool,
    },
    /// H.265/HEVC video frame
    H265 {
        /// NAL units
        data: Vec<Vec<u8>>,
        /// Presentation timestamp in 90kHz units
        timestamp: i64,
        /// Whether this is a keyframe
        is_keyframe: bool,
    },
    /// AAC audio frame
    Aac {
        /// Raw AAC data
        data: Vec<u8>,
        /// Presentation timestamp
        timestamp: i64,
    },
    /// G.711 audio frame
    G711 {
        /// Raw audio samples
        data: Vec<u8>,
        /// Presentation timestamp
        timestamp: i64,
    },
    /// Stream ended or error occurred
    End { reason: String },
}

/// Stream responder that connects to an RTSP source and forwards frames.
#[cfg(any(feature = "agent", feature = "server"))]
#[derive(Stream, Default)]
pub struct RtspSessionStreamResponder {
    /// Flag to signal the stream should stop
    stop_flag: Arc<RwLock<bool>>,
}

#[cfg(any(feature = "agent", feature = "server"))]
impl StreamResponder for RtspSessionStreamResponder {
    type In = RtspSessionStreamRequest;
    type Out = RtspSessionStreamResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        match request {
            RtspSessionStreamRequest::Start { url, transport } => {
                // Reset stop flag
                *self.stop_flag.write().await = false;

                // Parse the URL
                let parsed_url = Url::parse(&url)?;
                debug!(
                    "Connecting to RTSP stream: {}",
                    parsed_url.host_str().unwrap_or("unknown")
                );

                // Create session options based on transport preference
                let session_group = Arc::new(SessionGroup::default());
                let mut session = retina::client::Session::describe(
                    parsed_url,
                    retina::client::SessionOptions::default().session_group(session_group),
                )
                .await?;

                // Setup all streams
                for i in 0..session.streams().len() {
                    let setup_options = match transport {
                        RtspTransport::Udp => SetupOptions::default()
                            .transport(retina::client::Transport::Udp(Default::default())),
                        RtspTransport::Tcp => SetupOptions::default()
                            .transport(retina::client::Transport::Tcp(Default::default())),
                    };
                    session.setup(i, setup_options).await?;
                }

                // Capture codec parameters (SPS/PPS) so the client can decode.
                let mut param_msgs = Vec::new();
                for (i, stream) in session.streams().iter().enumerate() {
                    if let Some(retina::codec::ParametersRef::Video(v)) = stream.parameters() {
                        let (width, height) = v.pixel_dimensions();
                        param_msgs.push(RtspSessionStreamResponse {
                            stream_index: i,
                            frame: RtspFrame::Parameters {
                                extra_data: v.extra_data().to_vec(),
                                width,
                                height,
                            },
                        });
                    }
                }

                // Start playing
                let mut session = session
                    .play(retina::client::PlayOptions::default())
                    .await?
                    .demuxed()?;

                let stop_flag = self.stop_flag.clone();

                // Send codec parameters before any frames.
                for msg in param_msgs {
                    if sender.send(msg).await.is_err() {
                        return Ok(());
                    }
                }

                // Read frames in a loop
                loop {
                    // Check stop flag
                    if *stop_flag.read().await {
                        let _ = sender
                            .send(RtspSessionStreamResponse {
                                stream_index: 0,
                                frame: RtspFrame::End {
                                    reason: "Stopped by request".to_string(),
                                },
                            })
                            .await;
                        break;
                    }

                    use futures::StreamExt;
                    match session.next().await {
                        Some(Ok(item)) => {
                            let response = match item {
                                CodecItem::VideoFrame(frame) => {
                                    let stream_id = frame.stream_id();
                                    let is_keyframe = frame.is_random_access_point();
                                    let timestamp = frame.timestamp().timestamp();
                                    let data = frame.into_data();

                                    RtspSessionStreamResponse {
                                        stream_index: stream_id,
                                        frame: RtspFrame::H264 {
                                            data: vec![data],
                                            timestamp,
                                            is_keyframe,
                                        },
                                    }
                                }
                                CodecItem::AudioFrame(frame) => {
                                    let timestamp = frame.timestamp().timestamp();
                                    let data = frame.data().to_vec();

                                    RtspSessionStreamResponse {
                                        stream_index: frame.stream_id(),
                                        frame: RtspFrame::Aac { data, timestamp },
                                    }
                                }
                                CodecItem::MessageFrame(_) => continue,
                                _ => continue,
                            };

                            if sender.send(response).await.is_err() {
                                break;
                            }
                        }
                        Some(Err(e)) => {
                            let _ = sender
                                .send(RtspSessionStreamResponse {
                                    stream_index: 0,
                                    frame: RtspFrame::End {
                                        reason: e.to_string(),
                                    },
                                })
                                .await;
                            break;
                        }
                        None => {
                            let _ = sender
                                .send(RtspSessionStreamResponse {
                                    stream_index: 0,
                                    frame: RtspFrame::End {
                                        reason: "Stream ended".to_string(),
                                    },
                                })
                                .await;
                            break;
                        }
                    }
                }
            }
            RtspSessionStreamRequest::Stop => {
                *self.stop_flag.write().await = true;
            }
        }
        Ok(())
    }
}

#[cfg(any(feature = "agent", feature = "server"))]
impl Drop for RtspSessionStreamResponder {
    fn drop(&mut self) {
        debug!("RTSP session responder dropped");
        // Signal stop in case the stream is still running
        if let Ok(mut flag) = self.stop_flag.try_write() {
            *flag = true;
        }
    }
}

/// Registers [`RtspSessionStreamResponder`] on each connection.
#[cfg(any(feature = "agent", feature = "server"))]
pub struct RtspResponderRegistration;

#[cfg(any(feature = "agent", feature = "server"))]
impl sandpolis_instance::network::RegisterResponders for RtspResponderRegistration {
    fn register_responders(&self, registry: &sandpolis_instance::network::StreamRegistry) {
        registry.register_responder(RtspSessionStreamResponder::default);
    }
}

#[cfg(any(feature = "agent", feature = "server"))]
inventory::submit!(sandpolis_instance::network::ResponderRegistration(
    &RtspResponderRegistration
));

#[cfg(feature = "client-gui")]
mod client {
    use super::{RtspFrame, RtspSessionStreamRequest, RtspSessionStreamResponse};
    use anyhow::Result;
    use openh264::formats::YUVSource;
    use sandpolis_instance::network::StreamRequester;
    use sandpolis_macros::Stream;
    use std::sync::Mutex;
    use tokio::sync::mpsc::{Sender, UnboundedReceiver, UnboundedSender, unbounded_channel};

    /// A decoded RGBA8 frame ready to upload to a GUI texture.
    pub struct RtspFrameRgba {
        pub width: u32,
        pub height: u32,
        /// Tightly packed RGBA8 (`width * height * 4` bytes).
        pub rgba: Vec<u8>,
    }

    /// Events surfaced to the GUI as an RTSP stream progresses.
    pub enum RtspStreamEvent {
        Started { width: u32, height: u32 },
        Frame(RtspFrameRgba),
        Stopped,
    }

    /// Per-stream decoder state.
    struct DecoderState {
        decoder: Option<openh264::decoder::Decoder>,
        /// SPS/PPS in Annex-B form, prepended to each keyframe.
        sps_pps: Vec<u8>,
        /// NAL length prefix size from the AVCDecoderConfigurationRecord.
        nal_length_size: usize,
        started: bool,
    }

    impl Default for DecoderState {
        fn default() -> Self {
            Self {
                decoder: None,
                sps_pps: Vec::new(),
                nal_length_size: 4,
                started: false,
            }
        }
    }

    /// Client side of an RTSP stream: decodes incoming H.264 to RGBA8 and forwards
    /// frames to the GUI through an unbounded channel.
    #[derive(Stream)]
    pub struct RtspSessionStreamRequester {
        events: UnboundedSender<RtspStreamEvent>,
        state: Mutex<DecoderState>,
    }

    impl RtspSessionStreamRequester {
        /// Construct a requester paired with the receiver the GUI drains.
        pub fn channel() -> (Self, UnboundedReceiver<RtspStreamEvent>) {
            let (events, rx) = unbounded_channel();
            (
                Self {
                    events,
                    state: Mutex::new(DecoderState::default()),
                },
                rx,
            )
        }
    }

    impl StreamRequester for RtspSessionStreamRequester {
        type In = RtspSessionStreamResponse;
        type Out = RtspSessionStreamRequest;

        async fn new(_: Self::Out, _: Sender<Self::Out>) -> Result<Self> {
            // The GUI path constructs this via `channel()` and registers it
            // directly; the registry's `new` path is unused.
            anyhow::bail!("RtspSessionStreamRequester must be constructed directly")
        }

        async fn on_message(&self, response: Self::In, _: Sender<Self::Out>) -> Result<()> {
            match response.frame {
                RtspFrame::Parameters {
                    extra_data,
                    width,
                    height,
                } => {
                    let (sps_pps, nal_length_size) = parse_avcc_record(&extra_data);
                    let mut st = self.state.lock().unwrap();
                    st.sps_pps = sps_pps;
                    st.nal_length_size = nal_length_size.max(1);
                    if st.decoder.is_none() {
                        st.decoder = openh264::decoder::Decoder::new().ok();
                    }
                    if !st.started {
                        st.started = true;
                        let _ = self.events.send(RtspStreamEvent::Started { width, height });
                    }
                }
                RtspFrame::H264 {
                    data, is_keyframe, ..
                } => {
                    let mut st = self.state.lock().unwrap();
                    let nls = st.nal_length_size;

                    // Build an Annex-B access unit, prepending SPS/PPS on keyframes.
                    let mut au = Vec::new();
                    if is_keyframe {
                        au.extend_from_slice(&st.sps_pps);
                    }
                    for nal in &data {
                        avcc_to_annexb(nal, nls, &mut au);
                    }

                    if st.decoder.is_none() {
                        st.decoder = openh264::decoder::Decoder::new().ok();
                    }
                    if let Some(decoder) = st.decoder.as_mut() {
                        match decoder.decode(&au) {
                            Ok(Some(yuv)) => {
                                let (w, h) = yuv.dimensions();
                                let mut rgba = vec![0u8; w * h * 4];
                                yuv.write_rgba8(&mut rgba);
                                let _ = self.events.send(RtspStreamEvent::Frame(RtspFrameRgba {
                                    width: w as u32,
                                    height: h as u32,
                                    rgba,
                                }));
                            }
                            Ok(None) => {}
                            Err(e) => tracing::debug!(error = %e, "H.264 decode error"),
                        }
                    }
                }
                RtspFrame::End { .. } => {
                    let _ = self.events.send(RtspStreamEvent::Stopped);
                }
                // H.265/audio frames are not decoded for MVP.
                _ => {}
            }
            Ok(())
        }
    }

    /// Parse an AVCDecoderConfigurationRecord into Annex-B SPS/PPS bytes and the
    /// NAL length prefix size used by frame data.
    fn parse_avcc_record(data: &[u8]) -> (Vec<u8>, usize) {
        let mut out = Vec::new();
        // Minimum header is 6 bytes; bail to sane defaults otherwise.
        if data.len() < 7 {
            return (out, 4);
        }
        let nal_length_size = (data[4] & 0x03) as usize + 1;
        let mut pos = 5;

        // SPS set.
        let num_sps = (data[pos] & 0x1f) as usize;
        pos += 1;
        for _ in 0..num_sps {
            if pos + 2 > data.len() {
                return (out, nal_length_size);
            }
            let len = u16::from_be_bytes([data[pos], data[pos + 1]]) as usize;
            pos += 2;
            if pos + len > data.len() {
                return (out, nal_length_size);
            }
            out.extend_from_slice(&[0, 0, 0, 1]);
            out.extend_from_slice(&data[pos..pos + len]);
            pos += len;
        }

        // PPS set.
        if pos >= data.len() {
            return (out, nal_length_size);
        }
        let num_pps = data[pos] as usize;
        pos += 1;
        for _ in 0..num_pps {
            if pos + 2 > data.len() {
                return (out, nal_length_size);
            }
            let len = u16::from_be_bytes([data[pos], data[pos + 1]]) as usize;
            pos += 2;
            if pos + len > data.len() {
                return (out, nal_length_size);
            }
            out.extend_from_slice(&[0, 0, 0, 1]);
            out.extend_from_slice(&data[pos..pos + len]);
            pos += len;
        }

        (out, nal_length_size)
    }

    /// Convert one AVCC (length-prefixed) buffer of NAL units into Annex-B
    /// (start-code prefixed), appending to `out`.
    fn avcc_to_annexb(data: &[u8], nal_length_size: usize, out: &mut Vec<u8>) {
        let mut pos = 0;
        while pos + nal_length_size <= data.len() {
            let mut len = 0usize;
            for i in 0..nal_length_size {
                len = (len << 8) | data[pos + i] as usize;
            }
            pos += nal_length_size;
            if len == 0 || pos + len > data.len() {
                break;
            }
            out.extend_from_slice(&[0, 0, 0, 1]);
            out.extend_from_slice(&data[pos..pos + len]);
            pos += len;
        }
    }

    #[cfg(test)]
    mod tests {
        use super::*;

        #[test]
        fn avcc_to_annexb_two_nals() {
            // Two NAL units: lengths 3 and 2, with 4-byte length prefixes.
            let avcc = [
                0, 0, 0, 3, 0xAA, 0xBB, 0xCC, // NAL 1
                0, 0, 0, 2, 0xDD, 0xEE, // NAL 2
            ];
            let mut out = Vec::new();
            avcc_to_annexb(&avcc, 4, &mut out);
            assert_eq!(
                out,
                vec![0, 0, 0, 1, 0xAA, 0xBB, 0xCC, 0, 0, 0, 1, 0xDD, 0xEE]
            );
        }

        #[test]
        fn avcc_to_annexb_truncated_is_safe() {
            // Declares length 9 but only 2 bytes follow: must not panic.
            let avcc = [0, 0, 0, 9, 0x01, 0x02];
            let mut out = Vec::new();
            avcc_to_annexb(&avcc, 4, &mut out);
            assert!(out.is_empty());
        }

        #[test]
        fn parse_record_extracts_sps_pps() {
            // version, profile, compat, level, lengthSizeMinusOne=3
            // numSPS=1, SPS len=2 [0x67,0x42], numPPS=1, PPS len=2 [0x68,0xCE]
            let record = [
                1, 0x42, 0x00, 0x1e, 0xff, 0xe1, 0x00, 0x02, 0x67, 0x42, 0x01, 0x00, 0x02, 0x68,
                0xce,
            ];
            let (annexb, nls) = parse_avcc_record(&record);
            assert_eq!(nls, 4);
            assert_eq!(
                annexb,
                vec![0, 0, 0, 1, 0x67, 0x42, 0, 0, 0, 1, 0x68, 0xce]
            );
        }
    }
}

#[cfg(feature = "client-gui")]
pub use client::{RtspFrameRgba, RtspSessionStreamRequester, RtspStreamEvent};

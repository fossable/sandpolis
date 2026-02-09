use anyhow::Result;
use retina::client::{SessionGroup, SetupOptions};
use retina::codec::CodecItem;
use sandpolis_instance::network::StreamResponder;
use sandpolis_macros::Stream;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::RwLock;
use tokio::sync::mpsc::Sender;
use tracing::debug;
use url::Url;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct RtspConfig {
    pub port: u16,
    pub username: String,
    pub password: String,
    pub path: String,
}

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

                // Start playing
                let mut session = session
                    .play(retina::client::PlayOptions::default())
                    .await?
                    .demuxed()?;

                let stop_flag = self.stop_flag.clone();

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

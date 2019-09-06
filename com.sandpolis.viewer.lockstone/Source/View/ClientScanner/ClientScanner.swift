/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import AVFoundation
import UIKit

class ClientScanner: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
	
    @IBOutlet weak var progress: UIActivityIndicatorView!

    override var prefersStatusBarHidden: Bool {
		return true
	}
	
	override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
		return .portrait
	}

	private var captureSession: AVCaptureSession!
	
	override func viewDidLoad() {
		super.viewDidLoad()
		
		view.backgroundColor = UIColor.black
		captureSession = AVCaptureSession()
		
		guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return }
		let videoInput: AVCaptureDeviceInput
		
		do {
			videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
		} catch {
			return
		}
		
		if captureSession.canAddInput(videoInput) {
			captureSession.addInput(videoInput)
		} else {
			failed()
			return
		}
		
		let metadataOutput = AVCaptureMetadataOutput()
		
		if captureSession.canAddOutput(metadataOutput) {
			captureSession.addOutput(metadataOutput)
			
			metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
			metadataOutput.metadataObjectTypes = [.qr]
		} else {
			failed()
			return
		}
		
		let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
		previewLayer.frame = view.layer.bounds
		previewLayer.videoGravity = .resizeAspectFill
        view.layer.insertSublayer(previewLayer, at: 0)
		
		captureSession.startRunning()
	}
	
	override func viewWillAppear(_ animated: Bool) {
		super.viewWillAppear(animated)
		
		if !captureSession.isRunning {
			captureSession.startRunning()
		}
	}
	
	override func viewWillDisappear(_ animated: Bool) {
		super.viewWillDisappear(animated)
		
		if captureSession.isRunning {
			captureSession.stopRunning()
		}
	}
	
    @IBAction func cancel(_ sender: Any) {
        dismiss(animated: true)
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
		captureSession.stopRunning()
		
		if let metadataObject = metadataObjects.first {
			guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
			guard let stringValue = readableObject.stringValue else { return }
			AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
			found(token: stringValue)
		}

		//dismiss(animated: true)
	}
	
	private func failed() {
		let alert = UIAlertController(title: "Scanning not supported", message: "Your device does not support QR code scanning.", preferredStyle: .alert)
		alert.addAction(UIAlertAction(title: "OK", style: .default))
		present(alert, animated: true)
		captureSession = nil
	}
	
	private func found(token: String) {
        progress.startAnimating()
		
        CloudUtil.addCloudClient(token: token) { json, error in
            print("Error:", error)
            print("JSON:", json)
        }
	}
}

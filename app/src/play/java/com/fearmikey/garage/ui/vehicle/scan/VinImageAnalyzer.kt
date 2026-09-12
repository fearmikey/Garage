package com.fearmikey.garage.ui.vehicle.scan

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fearmikey.garage.util.VinValidator
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A CameraX [ImageAnalysis.Analyzer] that looks for a VIN in each camera frame.
 *
 * Most vehicles carry their VIN as a Code 39 barcode (door-jamb sticker), so
 * that's tried first since it's fast and unambiguous. If no barcode is found,
 * this falls back to OCR (ML Kit Text Recognition) and looks for a
 * VIN-shaped token in the recognized text, which covers dashboard plates and
 * title/registration documents that only print the VIN as plain text.
 *
 * [onVinDetected] is invoked at most once (per analyzer instance) with a
 * validated, 17-character VIN.
 */
class VinImageAnalyzer(
    private val onVinDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93, Barcode.FORMAT_CODE_128)
            .build(),
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Guards against firing [onVinDetected] more than once while frames keep streaming in. */
    private val hasReported = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (hasReported.get() || (mediaImage == null)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val vinFromBarcode = barcodes.firstNotNullOfOrNull { barcode ->
                    barcode.rawValue?.takeIf(VinValidator::isValidVin)
                }
                if (vinFromBarcode != null) {
                    report(vinFromBarcode)
                    imageProxy.close()
                } else {
                    recognizeText(image, imageProxy)
                }
            }
            .addOnFailureListener {
                recognizeText(image, imageProxy)
            }
    }

    private fun recognizeText(image: InputImage, imageProxy: ImageProxy) {
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                VinValidator.findVinInText(text.text)?.let(::report)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun report(vin: String) {
        if (hasReported.compareAndSet(false, true)) {
            onVinDetected(vin)
        }
    }

    fun close() {
        barcodeScanner.close()
        textRecognizer.close()
    }
}

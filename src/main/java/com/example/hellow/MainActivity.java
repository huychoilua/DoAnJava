package com.example.hellow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hellow.data.AppDatabase;
import com.example.hellow.data.HistoryLog;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button btnDetect;
    private Button btnViewHistory;

    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    private FaceDetectorYN faceDetectorYN;
    private FaceRecognizerSF faceRecognizerSF;
    private Mat sampleUserEmbedding;
    private final String sampleUserName = "Nguyễn Đại Minh Huy";
    private final String sampleUserId = "22200073";

    private enum DetectionState { IDLE, BLINK_DETECTION, FACE_RECOGNITION }
    private volatile DetectionState currentState = DetectionState.IDLE;

    private FaceDetector mlKitFaceDetector;
    private Handler detectionHandler;
    private Runnable blinkTimeoutRunnable;
    private Runnable recognitionTimeoutRunnable;

    private static final long BLINK_DETECTION_TIMEOUT_MS = 4000;
    private static final long RECOGNITION_TIMEOUT_MS = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        btnDetect = findViewById(R.id.btnDetect);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        cameraExecutor = Executors.newSingleThreadExecutor();

        btnDetect.setEnabled(false);
        btnViewHistory.setEnabled(false);
        btnDetect.setText("Đang tải AI...");

        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        mlKitFaceDetector = FaceDetection.getClient(highAccuracyOpts);
        detectionHandler = new Handler(Looper.getMainLooper());

        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "Không thể tải OpenCV!", Toast.LENGTH_SHORT).show();
        } else {
            loadAiModelsAndData();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }

        btnDetect.setOnClickListener(v -> {
            if (currentState == DetectionState.IDLE) {
                startBlinkDetection();
            } else {
                stopDetection();
            }
        });

        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void startBlinkDetection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Cần cấp quyền camera!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("STATE", "Bắt đầu Giai đoạn 1: Kiểm tra chớp mắt.");
        currentState = DetectionState.BLINK_DETECTION;
        startCamera();
        runOnUiThread(() -> {
            btnDetect.setText("Dừng");
            Toast.makeText(this, "Vui lòng chớp mắt...", Toast.LENGTH_SHORT).show();
        });

        blinkTimeoutRunnable = () -> {
            if (currentState == DetectionState.BLINK_DETECTION) {
                Log.d("STATE", "Hết thời gian chờ chớp mắt.");
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Không phát hiện chớp mắt. Vui lòng thử lại.", Toast.LENGTH_LONG).show());
                stopDetection();
            }
        };
        detectionHandler.postDelayed(blinkTimeoutRunnable, BLINK_DETECTION_TIMEOUT_MS);
    }

    private void stopDetection() {
        Log.d("STATE", "Dừng tất cả các quá trình.");
        if (cameraProviderFuture != null) {
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("CAMERA", "Lỗi khi dừng camera", e);
                }
            }, ContextCompat.getMainExecutor(this));
        }
        currentState = DetectionState.IDLE;
        detectionHandler.removeCallbacksAndMessages(null);

        runOnUiThread(() -> {
            btnDetect.setText("Nhận diện");
            btnDetect.setEnabled(true);
        });
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build();
        Preview preview = new Preview.Builder().setResolutionSelector(resolutionSelector).build();
        this.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setResolutionSelector(resolutionSelector).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, this.cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(ImageProxy imageProxy) {
        if (currentState == DetectionState.IDLE) {
            imageProxy.close();
            return;
        }
        try {
            if (currentState == DetectionState.BLINK_DETECTION) {
                detectBlink(imageProxy);
            } else if (currentState == DetectionState.FACE_RECOGNITION) {
                detectFaces(imageProxy);
            } else {
                imageProxy.close();
            }
        } catch (Exception e) {
            Log.e("PROCESS_PROXY", "Lỗi trong processImageProxy", e);
            if (imageProxy != null) imageProxy.close();
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void detectBlink(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        mlKitFaceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    boolean bothEyesClosed = false;
                    if (!faces.isEmpty()) {
                        for (Face face : faces) {
                            if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
                                if (face.getLeftEyeOpenProbability() < 0.4f && face.getRightEyeOpenProbability() < 0.4f) {
                                    bothEyesClosed = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (bothEyesClosed) {
                        Log.d("STATE", "Phát hiện chớp mắt! Chuyển sang Giai đoạn 2: Nhận diện.");
                        detectionHandler.removeCallbacks(blinkTimeoutRunnable);
                        currentState = DetectionState.FACE_RECOGNITION;

                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Đã chớp mắt, đang nhận diện...", Toast.LENGTH_SHORT).show());

                        recognitionTimeoutRunnable = () -> {
                            if (currentState == DetectionState.FACE_RECOGNITION) {
                                Log.d("STATE", "Hết thời gian nhận diện.");
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Hết thời gian nhận diện. Vui lòng thử lại.", Toast.LENGTH_LONG).show());
                                stopDetection();
                            }
                        };
                        detectionHandler.postDelayed(recognitionTimeoutRunnable, RECOGNITION_TIMEOUT_MS);
                    }
                })
                .addOnFailureListener(e -> Log.e("MLKIT", "Lỗi phát hiện khuôn mặt ML Kit: ", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private String getPathFromAssets(String fileName) throws IOException {
        File cacheDir = getCacheDir();
        File outFile = new File(cacheDir, fileName);
        if (!outFile.exists()) {
            try (InputStream is = getAssets().open(fileName); FileOutputStream os = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
        return outFile.getAbsolutePath();
    }

    private void loadAiModelsAndData() {
        cameraExecutor.execute(() -> {
            try {
                final String detectionModelPath = getPathFromAssets("face_detection_yunet.onnx");
                final String recognitionModelPath = getPathFromAssets("face_recognition_sface_2021dec.onnx");
                // SỬA LỖI 1: Trả lại đúng tên tệp embedding
                final String embeddingPath = getPathFromAssets("huy_embedding.bin");

                // SỬA LỖI 2: Khôi phục lại Size(0,0) để linh hoạt
                faceDetectorYN = FaceDetectorYN.create(detectionModelPath, "", new Size(0, 0), 0.85f, 0.3f, 5000);
                faceRecognizerSF = FaceRecognizerSF.create(recognitionModelPath, "");

                InputStream isEmbedding = new java.io.FileInputStream(embeddingPath);
                ByteArrayOutputStream bufferEmbedding = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int nRead;
                while ((nRead = isEmbedding.read(data, 0, data.length)) != -1) {
                    bufferEmbedding.write(data, 0, nRead);
                }
                isEmbedding.close();
                byte[] featureBytes = bufferEmbedding.toByteArray();
                FloatBuffer floatBuffer = ByteBuffer.wrap(featureBytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
                float[] featureFloats = new float[floatBuffer.remaining()];
                floatBuffer.get(featureFloats);
                Mat featureMat = new Mat(1, 128, CvType.CV_32F);
                featureMat.put(0, 0, featureFloats);
                this.sampleUserEmbedding = featureMat.clone();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Sẵn sàng nhận diện!", Toast.LENGTH_SHORT).show();
                    btnDetect.setEnabled(true);
                    btnViewHistory.setEnabled(true);
                    btnDetect.setText("Nhận diện");
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Log.e("OpenCV_DNN", "Lỗi nghiêm trọng khi tải tài nguyên AI: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Lỗi tải mô hình AI: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    btnDetect.setText("Lỗi AI");
                });
            }
        });
    }

    private Mat imageProxyToMat(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        Mat yuvMat = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        yuvMat.put(0, 0, nv21);
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3);
        yuvMat.release();
        return rgbMat;
    }

    private void detectFaces(ImageProxy imageProxy) {
        if (faceDetectorYN == null || faceRecognizerSF == null || sampleUserEmbedding == null || currentState != DetectionState.FACE_RECOGNITION) {
            imageProxy.close();
            return;
        }
        try {
            Mat mat = imageProxyToMat(imageProxy);

            // SỬA LỖI 3: Khôi phục logic xoay ảnh mạnh mẽ ban đầu
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            if (rotationDegrees == 90) { Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE); }
            else if (rotationDegrees == 180) { Core.rotate(mat, mat, Core.ROTATE_180); }
            else if (rotationDegrees == 270) { Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE); }
            Core.flip(mat, mat, 1);

            faceDetectorYN.setInputSize(mat.size());
            Mat faces = new Mat();
            faceDetectorYN.detect(mat, faces);

            String message = "";

            if (faces.rows() > 0) {
                Mat primaryFace = faces.row(0);
                Mat alignedFace = new Mat();
                faceRecognizerSF.alignCrop(mat, primaryFace, alignedFace);
                Mat currentFeature = new Mat();
                faceRecognizerSF.feature(alignedFace, currentFeature);
                double score = faceRecognizerSF.match(this.sampleUserEmbedding, currentFeature, FaceRecognizerSF.FR_COSINE);

                if (score > 0.6) {
                    message = "Nhận diện thành công: " + this.sampleUserName;
                    HistoryLog newLog = new HistoryLog(this.sampleUserId, this.sampleUserName, System.currentTimeMillis());
                    AppDatabase.databaseWriteExecutor.execute(() -> AppDatabase.getDatabase(getApplicationContext()).historyLogDao().insertLog(newLog));
                    stopDetection(); // Dừng ngay khi thành công
                } else {
                    // Nếu không thành công, chúng ta có thể thêm một thông báo tạm thời để dễ debug
                    // nhưng trong logic cuối cùng, chúng ta sẽ để im lặng và thử lại ở khung hình tiếp theo
                     message = "Đang nhận diện... Score: " + String.format("%.2f", score);
                }

                alignedFace.release();
                currentFeature.release();
            }
            
            // Chỉ hiển thị Toast khi có thông báo quan trọng
            if (!message.isEmpty() && (message.contains("thành công") || faces.rows() == 0)) {
                 final String finalMessage = message;
                 runOnUiThread(() -> Toast.makeText(this, finalMessage, Toast.LENGTH_SHORT).show());
            }

            mat.release();
            faces.release();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (mlKitFaceDetector != null) {
            mlKitFaceDetector.close();
        }
        if (detectionHandler != null) {
            detectionHandler.removeCallbacksAndMessages(null);
        }
    }
}

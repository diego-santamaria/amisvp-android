package com.example.amisvp;

import static com.example.amisvp.FullscreenActivity.EXTRA_EXAM_INFO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amisvp.dialog.CancelVideoDialog;
import com.example.amisvp.dialog.StopVideoDialog;
import com.example.amisvp.interfaces.IVisionImageProcessor;
import com.example.amisvp.java.facedetector.FaceDetectorProcessor;
import com.example.amisvp.pojo.Exam;
import com.example.amisvp.preference.PreferenceUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoCaptureActivity extends AppCompatActivity
        implements StopVideoDialog.NoticeDialogListener,
        CancelVideoDialog.NoticeDialogListener,
        OnRequestPermissionsResultCallback,
        FaceDetectorProcessor.NoticeDetectorListener{
    private Exam examInfo;

    @Nullable private ProcessCameraProvider cameraProvider;
    @Nullable private Preview previewUseCase;
    @Nullable private VideoCapture videoCaptureUseCase;
    @Nullable private ImageAnalysis analysisUseCase;
    @Nullable private IVisionImageProcessor imageProcessor;

    private CameraSelector cameraSelector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderLiveData;
    private static final String TAG = "CameraXLivePreview";
    private static final int PERMISSION_REQUESTS = 1;
    private static final int TEXT_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 20.0f;

    private Button btnRecordVideo, btnCancelVideo;
    private ProgressBar progressBarOrientation, progressBarDetection;
    private ImageView imageViewOrientation, imageViewDetection;
    private TextView status1TextView, status2TextView;
    private boolean saveVideoByDefault = false;
    private boolean needUpdateGraphicOverlayImageSourceInfo;
    private final int lensFacing = CameraSelector.LENS_FACING_FRONT;

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_capture);

        Intent intent = getIntent();
        examInfo = (Exam)intent.getSerializableExtra(EXTRA_EXAM_INFO);

        //Camera selector use case
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        previewView = findViewById(R.id.previewView);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        btnRecordVideo = findViewById(R.id.record_video_button);
        btnCancelVideo = findViewById(R.id.cancel_button);
        progressBarOrientation = findViewById(R.id.orientationProgBar);
        imageViewOrientation = findViewById(R.id.orientation_done_imageView);
        status1TextView = findViewById(R.id.status1TextView);
        progressBarDetection = findViewById(R.id.faceProgBar);
        imageViewDetection = findViewById(R.id.face_done_imageView);
        status2TextView = findViewById(R.id.status2TextView);

        setTextViewStyle(status1TextView);
        setTextViewStyle(status2TextView);

        int orientation = this.getResources().getConfiguration().orientation;
        toggleByFulfillmentOfPreconditions(orientation, false);

        new ViewModelProvider(this, (ViewModelProvider.Factory) AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            if (allPermissionsGranted()) {
                                bindAllCameraUseCases();
                            }
                        });

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            if (videoCaptureUseCase == null) { // If no video is being recorded...
                bindAnalysisUseCase();
            } else {
                switchBetweenTwoUseCases();
            }
        }
    }

    private void unbindAnalysisUseCase(){
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (graphicOverlay != null) {
            graphicOverlay.clear();
        }
    }

    private void unbindPreviewUseCase(){
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }
    }

    private void switchBetweenTwoUseCases(){
        unbindAnalysisUseCase();
        //unbindPreviewUseCase();
        bindVideoCaptureUseCase();
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
    }

    @SuppressLint("RestrictedApi")
    private void bindVideoCaptureUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (videoCaptureUseCase != null) {
            cameraProvider.unbind(videoCaptureUseCase);
        }

        VideoCapture.Builder builder = new VideoCapture.Builder();
        Integer videoFrameRate = 15;
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        builder.setVideoFrameRate(videoFrameRate);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        videoCaptureUseCase = builder.build();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, videoCaptureUseCase);
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        try {
            Log.i(TAG, "Using Face Detector Processor");
            imageProcessor = new FaceDetectorProcessor(this);
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor for Face Detection", e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = false; //lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }
                    try {
                        imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
    }

    private void showResultIntent(String vidFilePath){
        Intent intent = new Intent(this, ResultActivity.class);
        examInfo.RutaVideo = vidFilePath;
        intent.putExtra(EXTRA_EXAM_INFO, examInfo);
        startActivity(intent);
        finish();
    }

    @SuppressLint("RestrictedApi")
    private void recordVideo() {
        if (videoCaptureUseCase != null) {
            File movieDir = new File(Environment.getExternalStorageDirectory() + "/DCIM", "/AMAV recordings");
            if (!movieDir.exists())
                movieDir.mkdir();
            if (!movieDir.exists())
                Toast.makeText(VideoCaptureActivity.this,"Directory not created/found.", Toast.LENGTH_SHORT).show();
            Date date = new Date();
            String timestamp = String.valueOf(date.getTime());
            String vidFilePath = movieDir.getAbsolutePath() + "/" + timestamp + ".mp4";
            File vidFile = new File(vidFilePath);
            vidFile.deleteOnExit();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            videoCaptureUseCase.startRecording(new VideoCapture.OutputFileOptions.Builder(vidFile).build(), getExecutor(), new VideoCapture.OnVideoSavedCallback() {
                @Override
                public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {

                    if (saveVideoByDefault == true) {
                        try {
                            Toast.makeText(VideoCaptureActivity.this,"Guardando evaluación. Por favor, espere.", Toast.LENGTH_SHORT).show();
                            showResultIntent(vidFile.getPath());
                        } catch (Exception e) {
                            Toast.makeText(VideoCaptureActivity.this,"Ha ocurrido un error interno.", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    Toast.makeText(VideoCaptureActivity.this, "Hubo un error al guardar el video: " + message, Toast.LENGTH_SHORT).show();
                    //videoCapture = null;
                }
            });
        }
    }

    @SuppressLint("RestrictedApi")
    public void recordVideo_onClick(View view) {
        // Iniciar evaluación
        if(btnRecordVideo.getText() == getResources().getString(R.string.btn_record_video))
        {
            switchBetweenTwoUseCases();
            btnRecordVideo.setText("Finalizar evaluación");
            btnCancelVideo.setEnabled(true);
            splashCountDownAsyncTask();
            recordVideo();
        } else { // Finalizar evaluación
            //prompt
            FragmentManager sfm = ((AppCompatActivity)this).getSupportFragmentManager();
            DialogFragment dialog = new StopVideoDialog();
            dialog.show(sfm,null);
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        if (dialog.getClass() == CancelVideoDialog.class)
        {
            Toast.makeText(this,"Evaluación cancelada.", Toast.LENGTH_SHORT).show();
            saveVideoByDefault = false;
            stopRecording();
            startActivity(new Intent(this, FullscreenActivity.class));
            finish();
        }
        if (dialog.getClass() == StopVideoDialog.class)
        {
            saveVideoByDefault = true;
            stopRecording();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    }

    @SuppressLint("RestrictedApi")
    public void cancelVideo_onClick(View view){
        //prompt
        FragmentManager sfm = ((AppCompatActivity)this).getSupportFragmentManager();
        DialogFragment dialog = new CancelVideoDialog();
        dialog.show(sfm,null);
    }

    @SuppressLint("RestrictedApi")
    private void stopRecording(){
        //btnRecordVideo.setText(getResources().getString(R.string.btn_record_video));
        //btnCancelVideo.setEnabled(false);
        if (videoCaptureUseCase != null)
            videoCaptureUseCase.stopRecording();
    }

    @Override
    public void onBackPressed () { }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        toggleByFulfillmentOfPreconditions(newConfig.orientation, false);

        bindAllCameraUseCases();
    }

    private void toggleByFulfillmentOfPreconditions(int orientation, boolean faceDetected){
        if (orientation == Configuration.ORIENTATION_UNDEFINED)
            orientation = isLandscapeMode(this) ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_UNDEFINED;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            progressBarOrientation.setVisibility(View.INVISIBLE);
            imageViewOrientation.setVisibility(View.VISIBLE);
        } else {
            progressBarOrientation.setVisibility(View.VISIBLE);
            imageViewOrientation.setVisibility(View.INVISIBLE);
        }

        if (faceDetected){
            progressBarDetection.setVisibility(View.INVISIBLE);
            imageViewDetection.setVisibility(View.VISIBLE);

        } else {
            progressBarDetection.setVisibility(View.VISIBLE);
            imageViewDetection.setVisibility(View.INVISIBLE);
        }

        btnRecordVideo.setEnabled(orientation == Configuration.ORIENTATION_LANDSCAPE && faceDetected);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        //bundle.putString(STATE_SELECTED_MODEL, selectedModel);
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            bindAllCameraUseCases();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void setTextViewStyle(TextView textView){
        if (textView != null){
            textView.setTextColor(TEXT_COLOR);
            textView.setTextSize(TEXT_SIZE);
            textView.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
        }
    }

    @Override
    public void onSuccess(List<Face> faces) {
        // A face has been successfully detected
        toggleByFulfillmentOfPreconditions(0, !faces.isEmpty());
    }
    @Override
    public void onFailure(Exception e) {
        // No face has been detected
        toggleByFulfillmentOfPreconditions(0, false);
        String error = "Failed to process. Error: " + e.getLocalizedMessage();
        Toast.makeText(VideoCaptureActivity.this, error, Toast.LENGTH_SHORT).show();
    }

    private void splashCountDownAsyncTask()
    {
        // For more reference: https://stackoverflow.com/questions/51489399/splash-screen-android-count-down-timer-display
        final TextView countDown_textView = (TextView) findViewById(R.id.countDown_TextView);
        countDown_textView.setTextColor(TEXT_COLOR);
        countDown_textView.setTextSize(150.0f);
        countDown_textView.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);

        // Create a count down timer object.It will count down every 0.1 seconds and last for milliSeconds milliseconds..
        final int time= 4000; //3600000*5;
        CountDownTimer countDownTimer = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long l) {
                long Seconds = l / 1000 % 60;
                if (Seconds != 0)
                    countDown_textView.setText(String.format("%2d", Seconds));
                else{
                    countDown_textView.setText("");
                }
            }
            @Override
            public void onFinish() {
                countDown_textView.setVisibility(View.INVISIBLE);
            }
        };
        // Start the count down timer.
        countDownTimer.start();
    }
}
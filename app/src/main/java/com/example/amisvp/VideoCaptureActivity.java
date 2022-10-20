package com.example.amisvp;

import static com.example.amisvp.FullscreenActivity.EXTRA_EXAM_INFO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.amisvp.dialog.StopVideoDialog;
import com.example.amisvp.pojo.Exam;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

public class VideoCaptureActivity extends AppCompatActivity
        implements StopVideoDialog.NoticeDialogListener,
        OnRequestPermissionsResultCallback {
    private Exam examInfo;

    @Nullable private ProcessCameraProvider cameraProvider;
    @Nullable private Preview previewUseCase;
    @Nullable private VideoCapture videoCaptureUseCase;
    @Nullable private ImageAnalysis analysisUseCase;
    //@Nullable private VisionImageProcessor imageProcessor;
    private CameraSelector cameraSelector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderLiveData;
    private static final String TAG = "CameraXLivePreview";
    private static final int PERMISSION_REQUESTS = 1;

    Button btnRecordVideo, btnCancelVideo;
    ProgressBar progressBarOrientation;
    ImageView imageViewOrientation;
    private boolean saveVideoByDefault = true;

    PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_capture);

        Intent intent = getIntent();
        examInfo = (Exam)intent.getSerializableExtra(EXTRA_EXAM_INFO);

        //Camera selector use case
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        previewView = findViewById(R.id.previewView);
        btnRecordVideo = findViewById(R.id.record_video_button);
        btnCancelVideo = findViewById(R.id.cancel_button);
        progressBarOrientation = findViewById(R.id.orientationProgBar);
        imageViewOrientation = findViewById(R.id.orientation_done_imageView);

        imageViewOrientation.setVisibility(View.INVISIBLE);
        btnCancelVideo.setEnabled(false);
        btnRecordVideo.setEnabled(false);

        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // code for portrait mode
            btnCancelVideo.setVisibility(View.INVISIBLE);
            btnRecordVideo.setVisibility(View.INVISIBLE);
        } else {
            // code for landscape mode
            btnCancelVideo.setVisibility(View.VISIBLE);
            btnRecordVideo.setVisibility(View.VISIBLE);
        }

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

    private void bindAllCameraUseCases() {
        bindPreviewUseCase();
        bindVideoCaptureUseCase();
        bindAnalysisUseCase();
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        //Size targetResolution = new Size(720,480);
        //builder.setTargetResolution(targetResolution);
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
        Size targetResolution = new Size(720,480);
        builder.setVideoFrameRate(videoFrameRate);
        builder.setTargetResolution(targetResolution);
        videoCaptureUseCase = builder.build();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, videoCaptureUseCase);
    }

    private void bindAnalysisUseCase() {

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
                    try {
                        if (saveVideoByDefault == true) {
                            Toast.makeText(VideoCaptureActivity.this,"Guardando evaluación. Por favor, espere.", Toast.LENGTH_SHORT).show();
                            showResultIntent(vidFile.getPath());
                        }
                    } catch (Exception e) {
                        Toast.makeText(VideoCaptureActivity.this,"Ha ocurrido un error interno.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    //videoCapture = null;
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
    public void onClick(View view) {
        saveVideoByDefault = true;
        if(btnRecordVideo.getText() == getResources().getString(R.string.btn_record_video)){
            btnRecordVideo.setText("Finalizar evaluación");
            btnCancelVideo.setEnabled(true);
            recordVideo();
        } else {
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
        setDefaultState();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    }

    @SuppressLint("RestrictedApi")
    public void cancelVideo_onClick(View view){
        //Toast.makeText(this,"Cancelado.", Toast.LENGTH_SHORT).show;
        saveVideoByDefault = false;
        setDefaultState();
    }

    @SuppressLint("RestrictedApi")
    private void setDefaultState(){
        btnRecordVideo.setText(getResources().getString(R.string.btn_record_video));
        btnCancelVideo.setEnabled(false);
        videoCaptureUseCase.stopRecording();
    }

    @Override
    public void onBackPressed () { }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int newOrientation = newConfig.orientation;

        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Do certain things when the user has switched to landscape.
            btnRecordVideo.setEnabled(true);
            progressBarOrientation.setVisibility(View.INVISIBLE);
            imageViewOrientation.setVisibility(View.VISIBLE);
            btnCancelVideo.setVisibility(View.VISIBLE);
            btnRecordVideo.setVisibility(View.VISIBLE);

        } else {
            btnRecordVideo.setEnabled(false);
            progressBarOrientation.setVisibility(View.VISIBLE);
            imageViewOrientation.setVisibility(View.INVISIBLE);
            btnCancelVideo.setVisibility(View.INVISIBLE);
            btnRecordVideo.setVisibility(View.INVISIBLE);
        }
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
}
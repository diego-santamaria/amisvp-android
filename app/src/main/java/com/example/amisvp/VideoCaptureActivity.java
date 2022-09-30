package com.example.amisvp;

import static com.example.amisvp.FullscreenActivity.EXTRA_EXAM_INFO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.amisvp.dialog.StopVideoDialog;
import com.example.amisvp.pojo.Exam;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class VideoCaptureActivity extends AppCompatActivity implements StopVideoDialog.NoticeDialogListener{
    private Exam examInfo;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private VideoCapture videoCapture;
    Button btnRecordVideo, btnCancelVideo;
    private boolean saveVideoByDefault = true;

    PreviewView previewView;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_capture);

        Intent intent = getIntent();
        examInfo = (Exam)intent.getSerializableExtra(EXTRA_EXAM_INFO);

        previewView = findViewById(R.id.previewView);
        btnRecordVideo = findViewById(R.id.record_video_button);
        btnCancelVideo = findViewById(R.id.cancel_button);

        btnCancelVideo.setEnabled(false);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        //Camera selector use case
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        //Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //Video capture use case
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(15)
                .setTargetResolution(new Size(720,480))
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, videoCapture);
    }

    @SuppressLint("RestrictedApi")
    private void recordVideo() {
        if (videoCapture != null) {
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
            videoCapture.startRecording(new VideoCapture.OutputFileOptions.Builder(vidFile).build(), getExecutor(), new VideoCapture.OnVideoSavedCallback() {
                @Override
                public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                    try {
                        if (saveVideoByDefault == true) {
                            Toast.makeText(VideoCaptureActivity.this,"Guardando evaluación. Por favor, espere.", Toast.LENGTH_SHORT).show();
                            //new BlobHelper().uploadBlobToContainerTask(vidFile.getPath(), "recordings");
                            showResultIntent(vidFile.getPath());

                        }
                    } catch (Exception e) {
                        Toast.makeText(VideoCaptureActivity.this,"Ha ocurrido un error interno.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    videoCapture = null;
                }

                @Override
                public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    Toast.makeText(VideoCaptureActivity.this, "Hubo un error al guardar el video: " + message, Toast.LENGTH_SHORT).show();
                    videoCapture = null;
                }
            });
        }
    }

    private void showResultIntent(String vidFilePath){
        Intent intent = new Intent(this, ResultActivity.class);
        examInfo.RutaVideo = vidFilePath;
        intent.putExtra(EXTRA_EXAM_INFO, examInfo);
        startActivity(intent);
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
        saveVideoByDefault = false;
        setDefaultState();
    }

    @SuppressLint("RestrictedApi")
    private void setDefaultState(){
        btnRecordVideo.setText(getResources().getString(R.string.btn_record_video));
        btnCancelVideo.setEnabled(false);
        videoCapture.stopRecording();
    }

    @Override
    public void onBackPressed () { }
}
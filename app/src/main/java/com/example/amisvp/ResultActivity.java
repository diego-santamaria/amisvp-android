package com.example.amisvp;

import static com.example.amisvp.FullscreenActivity.EXTRA_EXAM_INFO;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amisvp.helper.AuthenticationHelper;
import com.example.amisvp.interfaces.IAPIClient;
import com.example.amisvp.interfaces.IBlobEvents;
import com.example.amisvp.pojo.Exam;
import com.example.amisvp.task.BlobTask;

import java.net.URI;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity implements IBlobEvents {
    private Exam examInfo;
    Button btnRetry, btnAcept;
    TextView txtStep1, txtStep2;
    ImageView imageViewSaveDone, imageViewUpdDone, imageViewSaveError, imageViewUpdError;
    ProgressBar progressBarVideo, progressBarUri;

    IAPIClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        btnRetry = findViewById(R.id.retry_button);
        btnAcept = findViewById(R.id.ok_button);
        txtStep1 = findViewById(R.id.status1TextView);
        txtStep2 = findViewById(R.id.status2TextView);
        imageViewSaveDone = findViewById(R.id.save_done_imageView);
        imageViewUpdDone = findViewById(R.id.update_done_imageView);
        imageViewSaveError = findViewById(R.id.save_error_imageView);
        imageViewUpdError = findViewById(R.id.update_error_imageView);
        progressBarVideo = findViewById(R.id.saveVideoProgBar);
        progressBarUri = findViewById(R.id.saveUriProgBar);

        setResultStatusView("loading", 0);

        Intent intent = getIntent();
        examInfo = (Exam)intent.getSerializableExtra(EXTRA_EXAM_INFO);

        startUpload();
    }

    private void startUpload()
    {
        BlobTask blobTask = new BlobTask(this);
        blobTask.uploadAsync(examInfo.RutaVideo, "recordings");
    }

    @Override
    public void onBackPressed () { }

    @Override
    public void uploadSuccessfully(URI blobUri) {
        setResultStatusView("success", 1);
        examInfo.RutaVideo = blobUri.toString();
        apiClient = ServiceGenerator.createService(IAPIClient.class, ServiceGenerator.authToken);
        updateUri();
    }

    @Override
    public void uploadFailed(String errorMessage) {
        // retry
        setResultStatusView("failed", 1);
    }

    public void retry_onClick(View view)
    {
        setResultStatusView("retry", 0);
        startUpload();
    }

    private void setResultStatusView(String status, int step){
        switch (status) {
            case "loading":
            case "retry":
                txtStep1.setText(R.string.txt_status1);
                btnRetry.setEnabled(false);
                txtStep1.setTypeface(txtStep1.getTypeface(), Typeface.BOLD);

                progressBarVideo.setVisibility(View.VISIBLE);
                imageViewSaveDone.setVisibility(View.INVISIBLE);
                imageViewSaveError.setVisibility(View.INVISIBLE);

                progressBarUri.setVisibility(View.VISIBLE);
                imageViewUpdDone.setVisibility(View.INVISIBLE);
                imageViewUpdError.setVisibility(View.INVISIBLE);
                break;
            case "success":
                if (step == 1)
                {
                    txtStep1.setTypeface(txtStep1.getTypeface(), Typeface.NORMAL);
                    txtStep2.setTypeface(txtStep2.getTypeface(), Typeface.BOLD);
                    txtStep1.setText("Video guardado.");
                    txtStep2.setText("Actualizando información...");

                    progressBarVideo.setVisibility(View.INVISIBLE);
                    imageViewSaveDone.setVisibility(View.VISIBLE);
                    imageViewSaveError.setVisibility(View.INVISIBLE);
                }
                else if (step == 2) {
                    txtStep2.setText("Información actualizada.");
                    progressBarUri.setVisibility(View.GONE);
                    imageViewUpdDone.setVisibility(View.VISIBLE);

                    progressBarUri.setVisibility(View.INVISIBLE);
                    imageViewUpdDone.setVisibility(View.VISIBLE);
                    imageViewUpdError.setVisibility(View.INVISIBLE);
                }


                break;
            case "failed":
                btnRetry.setEnabled(true);
                if (step == 1)
                    txtStep1.setText("Video no guardado. Por favor, reintente.");
                if (step == 2)
                    txtStep2.setText("Actualización fallida. Por favor, reintente.");

                progressBarVideo.setVisibility(View.INVISIBLE);
                imageViewSaveDone.setVisibility(View.INVISIBLE);
                imageViewSaveError.setVisibility(View.VISIBLE);

                progressBarUri.setVisibility(View.INVISIBLE);
                imageViewUpdDone.setVisibility(View.INVISIBLE);
                imageViewUpdError.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void acept_onClick(View view)
    {
        startActivity(new Intent(this, FullscreenActivity.class));
        finish();
    }

    private void updateUri(){
         //
        Call<String> call = apiClient.SetVideoURI(examInfo);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()){
                    Log.d("OK",response.code()+" > "+response.body());
                    //String result = response.body();
                    setResultStatusView("success", 2);
                } else {
                    if (response.code() == 401) // Unauthorized
                    {
                        AuthenticationHelper.Authenticate(getApplicationContext());
                        Toast.makeText(getApplicationContext(),"Sin conexión. Por favor, reintente.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Actualización fallida. Por favor, reintente.",Toast.LENGTH_SHORT).show();
                    }
                    setResultStatusView("failed", 2);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Error",t.getMessage()+"");
                Toast.makeText(getApplicationContext(),"Sin conexión.",Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });
    }
}
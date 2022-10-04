package com.example.amisvp;

import static com.example.amisvp.FullscreenActivity.EXTRA_EXAM_INFO;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

    IAPIClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        btnRetry = findViewById(R.id.retry_button);
        btnRetry.setEnabled(false);
        btnAcept = findViewById(R.id.ok_button);
        txtStep1 = findViewById(R.id.status1TextView);
        txtStep1.setTypeface(txtStep1.getTypeface(), Typeface.BOLD);
        txtStep2 = findViewById(R.id.status2TextView);

        Intent intent = getIntent();
        examInfo = (Exam)intent.getSerializableExtra(EXTRA_EXAM_INFO);

        startUpload();
    }

    private void startUpload()
    {
        BlobTask blobTask = new BlobTask(this);
        blobTask.uploadAsync(examInfo.RutaVideo, "recordings");

        //UploadBlobAsyncTask blobThread = new UploadBlobAsyncTask(examInfo.RutaVideo, "recordings");

    }

    @Override
    public void onBackPressed () { }

    @Override
    public void uploadSuccessfully(URI blobUri) {
        txtStep1.setTypeface(txtStep1.getTypeface(), Typeface.NORMAL);
        txtStep2.setTypeface(txtStep2.getTypeface(), Typeface.BOLD);
        txtStep1.setText("Video guardado.");
        txtStep2.setText("Actualizando información...");
        examInfo.RutaVideo = blobUri.toString();
        apiClient = ServiceGenerator.createService(IAPIClient.class, ServiceGenerator.authToken);
        updateUri();
    }

    @Override
    public void uploadFailed(String errorMessage) {
        // retry
        btnRetry.setEnabled(true);
        txtStep1.setText("Video no guardado. Por favor, reintente.");
    }

    public void retry_onClick(View view)
    {
        txtStep1.setText(R.string.txt_status1);
        btnRetry.setEnabled(false);
        startUpload();
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
                    //showCandidateInfoIntent(examInfo);
                    txtStep2.setText("Información actualizada.");
                } else {
                    Toast.makeText(getApplicationContext(),"Llamada inválida.",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Error",t.getMessage()+"");
                call.cancel();
            }
        });
    }
}
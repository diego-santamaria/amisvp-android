package com.example.amisvp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class CandidateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String tokenStr = intent.getStringExtra(FullscreenActivity.EXTRA_TOKEN);

        getCandidateInfoByToken(tokenStr);



    }

    private void getCandidateInfoByToken(String tokenStr){
        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.tokenTextView);
        textView.setText("Token: " + tokenStr.toUpperCase());
    }

    private void setCandidateInfo(){

    }
}
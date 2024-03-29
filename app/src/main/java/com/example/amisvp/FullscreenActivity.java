package com.example.amisvp;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amisvp.databinding.ActivityFullscreenBinding;
import com.example.amisvp.interfaces.IAPIClient;
import com.example.amisvp.pojo.Auth;
import com.example.amisvp.pojo.Exam;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    public static final String EXTRA_TOKEN = "";
    public static final String EXTRA_EXAM_INFO = "";
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler(Looper.myLooper());
    IAPIClient apiClient;
    EditText tokenEditText;
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            /*
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            */
            mControlsView.setVisibility(View.VISIBLE);
            // Clear background text
            TextView welcomeTextView = findViewById(R.id.fullscreen_content);
            welcomeTextView.setText("");
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };
    private ActivityFullscreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        findViewById(R.id.indeterminateBar).setVisibility(View.GONE);

        mVisible = true;
        mControlsView = binding.fullscreenContentControls;
        mContentView = binding.fullscreenContent;

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        //binding.dummyButton.setOnTouchListener(mDelayHideTouchListener);

        tokenEditText = findViewById(R.id.tokenEditText);
        tokenEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    searchByToken_onClick(view);
                }
                return false;
            }
        });

        Authenticate();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);

        // Set background text from string.xml
        TextView welcomeTextView = findViewById(R.id.fullscreen_content);
        welcomeTextView.setText(R.string.welcome_content);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mContentView.getWindowInsetsController().show(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * Called when the user touches the only button available
     */
    public void searchByToken_onClick(View view) {
        // Do something in response to button click

        tokenEditText = findViewById(R.id.tokenEditText);
        String tokenStr = tokenEditText.getText().toString();

        apiClient = ServiceGenerator.createService(IAPIClient.class, ServiceGenerator.authToken);

        getExamInfoByToken(tokenStr);
    }

    private void initiateLoading(boolean loading){
        Dialog mOverlayDialog;
        if (loading){
            findViewById(R.id.indeterminateBar).setVisibility(View.VISIBLE);
            findViewById(R.id.main_button).setClickable(false);
            Toast.makeText(FullscreenActivity.this,"Obteniendo información. Por favor, espere.", Toast.LENGTH_SHORT).show();
        }
        else{
            findViewById(R.id.indeterminateBar).setVisibility(View.GONE);
            findViewById(R.id.main_button).setClickable(true);
        }
    }

    private void showCandidateInfoIntent(Exam examInfo){
        Intent intent = new Intent(this, CandidateActivity.class);
        intent.putExtra(EXTRA_EXAM_INFO, examInfo);
        startActivity(intent);
    }

    private void getExamInfoByToken(String tokenStr){
        initiateLoading(true);
        Call<Exam> call = apiClient.getByToken(tokenStr);
        call.enqueue(new Callback<Exam>() {
            @Override
            public void onResponse(Call<Exam> call, Response<Exam> response) {
                if (response.isSuccessful()){
                    Log.d("OK",response.code()+"");
                    Exam examInfo = response.body();
                    showCandidateInfoIntent(examInfo);
                } else {
                    Toast.makeText(getApplicationContext(),"Token inválido.",Toast.LENGTH_SHORT).show();
                }
                initiateLoading(false);
            }

            @Override
            public void onFailure(Call<Exam> call, Throwable t) {
                Log.d("Error",t.getMessage()+"");
                call.cancel();
                initiateLoading(false);
            }
        });
    }

    private void Authenticate() {
        Auth auth = getAuth();
        apiClient = ServiceGenerator.createService(IAPIClient.class);
        Call<String> call = apiClient.loginService(auth);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    ServiceGenerator.authToken = response.body();
                    Toast.makeText(getApplicationContext(),"En línea",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(),"Sin conexión",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("Error", t.getMessage());
                call.cancel();
            }
        });
    }

    private Auth getAuth() {
        Auth auth = new Auth();
        auth.Username = "admin";
        auth.Password = "12345";
        return auth;
    }
}
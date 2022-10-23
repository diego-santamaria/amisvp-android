package com.example.amisvp.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.amisvp.ServiceGenerator;
import com.example.amisvp.interfaces.IAPIClient;
import com.example.amisvp.pojo.Auth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthenticationHelper {

    private static IAPIClient apiClient;

    public static void Authenticate(Context context) {
        if (ServiceGenerator.authToken == null)
        {
            Auth auth = getAuth();
            apiClient = ServiceGenerator.createService(IAPIClient.class);
            Call<String> call = apiClient.loginService(auth);
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        ServiceGenerator.authToken = response.body();
                        Toast.makeText(context,"En línea",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context,"Sin conexión",Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(context,"Sin conexión",Toast.LENGTH_SHORT).show();
                    call.cancel();
                }
            });
        }
    }

    private static Auth getAuth() {
        Auth auth = new Auth();
        auth.Username = "admin";
        auth.Password = "12345";
        return auth;
    }
}

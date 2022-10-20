package com.example.amisvp.interfaces;

import com.example.amisvp.pojo.Auth;
import com.example.amisvp.pojo.Exam;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface IAPIClient {

    @GET("/api/evaluacion/ObtenerPorToken?")
    Call<Exam> getByToken(@Query("strToken") String strToken);

    @POST("/api/Evaluacion/ActualizarRutaVideo")
    Call<String> SetVideoURI(@Body Exam exam);

    @POST("/api/login/authenticate")
    Call<String> loginService(@Body Auth auth);
}

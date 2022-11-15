package com.example.amisvp.pojo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Exam implements Serializable {
    @SerializedName("IdEvaluacion")
    @Expose
    public Integer Id;
    @SerializedName("IdPersona")
    @Expose
    public Integer idPersona;
    @SerializedName("NroRegistroLicencia")
    @Expose
    public String nroRegistroLicencia;
    @SerializedName("Clase")
    @Expose
    public String clase;
    @SerializedName("Categoria")
    @Expose
    public String categoria;
    @SerializedName("FechaEvaluacion")
    @Expose
    public String fechaEvaluacion;
    @SerializedName("Token")
    @Expose
    public String token;
    @SerializedName("RutaFoto")
    @Expose
    public String rutaFoto;
    @SerializedName("SituacionRegistro")
    @Expose
    public String situacionRegistro;
    @SerializedName("UsuarioRegistro")
    @Expose
    public String usuarioRegistro;
    @SerializedName("FechaRegistro")
    @Expose
    public String fechaRegistro;
    @SerializedName("UsuarioCambio")
    @Expose
    public String usuarioCambio;
    @SerializedName("FechaCambio")
    @Expose
    public String fechaCambio;
    @SerializedName("Persona")
    @Expose
    public Person persona;
    @SerializedName("RutaVideo")
    @Expose
    public String RutaVideo;
    @SerializedName("Procesado")
    @Expose
    public Integer Procesado;
}


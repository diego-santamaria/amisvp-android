package com.example.amisvp.pojo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Person implements Serializable {
    @SerializedName("IdPersona")
    @Expose
    public Integer id;
    @SerializedName("TipoPersona")
    @Expose
    public String tipoPersona;
    @SerializedName("Nombres")
    @Expose
    public String nombres;
    @SerializedName("ApellidoPaterno")
    @Expose
    public String apellidoPaterno;
    @SerializedName("ApellidoMaterno")
    @Expose
    public String apellidoMaterno;
    @SerializedName("TipoDocumento")
    @Expose
    public String tipoDocumento;
    @SerializedName("NumeroDocumento")
    @Expose
    public String numeroDocumento;
    @SerializedName("Correo")
    @Expose
    public String correo;
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
}

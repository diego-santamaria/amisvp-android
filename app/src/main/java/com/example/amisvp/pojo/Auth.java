package com.example.amisvp.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Auth {
    @SerializedName("Username")
    @Expose
    public String Username;
    @SerializedName("Password")
    @Expose
    public String Password;
}

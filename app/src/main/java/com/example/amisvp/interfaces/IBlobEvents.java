package com.example.amisvp.interfaces;

import java.net.URI;

public interface IBlobEvents {
    public void uploadSuccessfully(URI blobUri);
    public void uploadFailed(String errorMessage);
}

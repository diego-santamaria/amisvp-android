package com.example.amisvp.interfaces;

import java.net.URI;

public interface IBlobEvents {
    void uploadSuccessfully(URI blobUri);
    void uploadFailed(String errorMessage);
}

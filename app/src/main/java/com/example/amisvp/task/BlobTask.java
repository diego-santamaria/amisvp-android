package com.example.amisvp.task;

import com.example.amisvp.helper.AsyncTaskHelper;
import com.example.amisvp.interfaces.IBlobEvents;

public class BlobTask {

    private final IBlobEvents blobEventsInterface;
    public BlobTask(IBlobEvents blobEventsInterface){
        this.blobEventsInterface = blobEventsInterface;
    }

    public void uploadAsync(String filePath, String containerPathName)
    {
        new AsyncTaskHelper().executeAsync(new UploadBlobTask(filePath, containerPathName), (blob) -> {
            if (blob != null){
                blobEventsInterface.uploadSuccessfully(blob.getUri());
            }else{
                blobEventsInterface.uploadFailed("File not loaded.");
            }
        });

    }
}

package com.example.amisvp.task;

import com.example.amisvp.helper.BlobHelper;
import com.example.amisvp.interfaces.IBlobEvents;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.nio.file.Path;
import java.nio.file.Paths;

public class UploadBlobTask {

    private IBlobEvents blobEventsInterface;
    public UploadBlobTask(IBlobEvents blobEventsInterface){
        this.blobEventsInterface = blobEventsInterface;
    }


    /*
    public void registerBlobEventsListener(IBlobEvents blobEventsInterface){
        this.blobEventsInterface = blobEventsInterface;
    }
    */

    public void uploadAsync(String filePath, String containerPathName){
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Path p = Paths.get(filePath);
                            String fileName = p.getFileName().toString();
                            CloudBlockBlob blob = BlobHelper.UploadAsync(filePath, fileName, containerPathName);
                            blobEventsInterface.uploadSuccessfully(blob.getUri());
                        } catch (Exception e) {
                            blobEventsInterface.uploadFailed(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }
}

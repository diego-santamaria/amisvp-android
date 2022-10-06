package com.example.amisvp.task;

import com.example.amisvp.helper.BlobHelper;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.nio.file.Path;
import java.nio.file.Paths;

public class UploadBlobAsyncTask extends Thread{
    private final String filePath;
    private final String containerPathName;

    public UploadBlobAsyncTask(String filePath, String containerPathName){
        this.filePath = filePath;
        this.containerPathName = containerPathName;
    }

    @Override
    public final void run() {
        try {
            Path p = Paths.get(filePath);
            String fileName = p.getFileName().toString();
            CloudBlockBlob blob = BlobHelper.UploadAsync(filePath, fileName, containerPathName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

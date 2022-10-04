package com.example.amisvp.task;

import android.os.Handler;
import android.os.Looper;

import com.example.amisvp.helper.BlobHelper;
import com.example.amisvp.interfaces.IBlobEvents;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadBlobTask implements Callable<CloudBlockBlob> {
    private String filePath;
    private String containerPathName;

    public UploadBlobTask(String filePath, String containerPathName){
        this.filePath = filePath;
        this.containerPathName = containerPathName;
    }

    @Override
    public CloudBlockBlob call() throws Exception {
        Path p = Paths.get(filePath);
        String fileName = p.getFileName().toString();
        return BlobHelper.UploadAsync(filePath, fileName, containerPathName);
    }
}

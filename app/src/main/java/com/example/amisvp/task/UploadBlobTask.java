package com.example.amisvp.task;

import com.example.amisvp.helper.BlobHelper;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class UploadBlobTask implements Callable<CloudBlockBlob> {
    private final String filePath;
    private final String containerPathName;

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

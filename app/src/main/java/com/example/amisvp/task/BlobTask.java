package com.example.amisvp.task;

import android.os.Handler;
import android.os.Looper;

import com.example.amisvp.helper.AsyncTaskHelper;
import com.example.amisvp.helper.BlobHelper;
import com.example.amisvp.interfaces.IBlobEvents;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlobTask {

    private IBlobEvents blobEventsInterface;
    public BlobTask(IBlobEvents blobEventsInterface){
        this.blobEventsInterface = blobEventsInterface;
    }


    /*
    public void registerBlobEventsListener(IBlobEvents blobEventsInterface){
        this.blobEventsInterface = blobEventsInterface;
    }
    */

/*    public void uploadAsync3(String filePath, String containerPathName)
    {
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
    public void uploadAsync2(String filePath, String containerPathName)
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            CloudBlockBlob blob = null;
            //Background work here
            try {
                Path p = Paths.get(filePath);
                String fileName = p.getFileName().toString();
                blob = BlobHelper.UploadAsync(filePath, fileName, containerPathName);
            } catch (Exception e) {
                blobEventsInterface.uploadFailed(e.getMessage());
                e.printStackTrace();
            }


            CloudBlockBlob finalBlob = blob;
            handler.post(() -> {
                blobEventsInterface.uploadSuccessfully(finalBlob.getUri());
            });
        });

    }*/

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

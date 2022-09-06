package com.example.amisvp.helper;
import android.os.AsyncTask;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/*public class BlobHelper  {

    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;" +
                    "AccountName=thesisprojectstoacc;" +
                    "AccountKey=cHhwPJI1YFDwm4IW8VrYSlrS7/eBNAl0yjhCzJItjRbwOKaaTRE+3CGLbQ8trWUYVt2JmcmgtxVM+ASteqlIAw==;EndpointSuffix=core.windows.net";


}*/

public class UploadBlobUsingSASUrl {
    public static String fileToUpload; //[Full path of the file you wish to upload]";
    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;" +
            "AccountName=thesisprojectstoacc;" +
            "AccountKey=cHhwPJI1YFDwm4IW8VrYSlrS7/eBNAl0yjhCzJItjRbwOKaaTRE+3CGLbQ8trWUYVt2JmcmgtxVM+ASteqlIAw==;EndpointSuffix=core.windows.net";

    public static void main(String[] args) throws IOException {

        UploadBlobUsingSASUrl uploadHandler = new UploadBlobUsingSASUrl();

        String sasSignature = "?sv=2021-06-08&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2023-09-06T06:18:14Z&st=2022-09-05T22:18:14Z&spr=https,http&sig=Uc03IhYpE0Ml7QKr0%2BMPr6iWKAbX36SK4wfQ%2BngeBl0%3D";
        String blobStorageEndpoint = "https://thesisprojectstoacc.blob.core.windows.net/recordings" + sasSignature;

        InputStream inStream = new FileInputStream(fileToUpload);
        BufferedInputStream bis = new BufferedInputStream(inStream);
        List<String> blockIds = new ArrayList<String>();

        int counter = 1;
        while (bis.available() > 0) {
            int blockSize = 4 * 1024 * 1024; // 4 MB;
            int bufferLength = bis.available() > blockSize ? blockSize : bis.available();

            byte[] buffer = new byte[bufferLength];
            bis.read(buffer, 0, buffer.length);
            String blockId = Base64.getEncoder().encodeToString(("Block-" + counter++).getBytes("UTF-8"));
            uploadHandler.UploadBlock(blobStorageEndpoint, buffer, blockId);
            blockIds.add(blockId);
        }

        uploadHandler.CommitBlockList(blobStorageEndpoint, blockIds);

        bis.close();
        inStream.close();
    }

    public void UploadBlock(String baseUri, byte[] blockContents, String blockId) throws IOException {

        OkHttpClient client = new OkHttpClient();

        MediaType mime = MediaType.parse("");
        RequestBody body = RequestBody.create(mime, blockContents);

        String uploadBlockUri = baseUri + "&comp=block&blockId=" + blockId;

        Request request = new Request.Builder()
                .url(uploadBlockUri)
                .put(body)
                .addHeader("x-ms-version", "2015-12-11")
                .addHeader("x-ms-blob-type", "BlockBlob")
                .build();

        client.newCall(request).execute();

    }

    public void CommitBlockList(String baseUri, List<String> blockIds) throws IOException {

        OkHttpClient client = new OkHttpClient();

        StringBuilder blockIdsPayload = new StringBuilder();
        blockIdsPayload.append("<?xml version='1.0' ?><BlockList>");
        for (String blockId : blockIds) {
            blockIdsPayload.append("<Latest>").append(blockId).append("</Latest>");
        }
        blockIdsPayload.append("</BlockList>");

        String putBlockListUrl = baseUri + "&comp=blocklist";
        MediaType contentType = MediaType.parse("");
        RequestBody body = RequestBody.create(contentType, blockIdsPayload.toString());

        Request request = new Request.Builder()
                .url(putBlockListUrl)
                .put(body)
                .addHeader("x-ms-version", "2015-12-11")
                .build();

        client.newCall(request).execute();
    }
}
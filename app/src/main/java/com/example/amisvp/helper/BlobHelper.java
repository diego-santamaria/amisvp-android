package com.example.amisvp.helper;

import android.util.Log;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.BlockEntry;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlobHelper {

    private static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;" +
                    "AccountName=savprojectstoacc;" +
                    "AccountKey=VC/jGYDv93NJSDD554P7+oSbb1Kf7sHIu/j0jqumBrPHyR4iUHvgaiwztZtlymM0HgglCfKHbXnz+AStupB3yA==;" +
                    "EndpointSuffix=core.windows.net";

    public static CloudBlockBlob UploadAsync(String filePath, String fileName, String containerPathName) throws URISyntaxException, InvalidKeyException, StorageException, IOException {

        // Setup the cloud storage account.
        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);

        // Create a blob service client
        CloudBlobClient blobClient = account.createCloudBlobClient();

        // Get a reference to a container
        // The container name must be lower case
        // Append a random UUID to the end of the container name so that
        // this sample can be run more than once in quick succession.
        containerPathName = containerPathName != null ? containerPathName : UUID.randomUUID().toString().replace("-", "");
        CloudBlobContainer container = blobClient.getContainerReference(containerPathName);

        // Create the container if it does not exist
        container.createIfNotExists();

        // Make the container public
        // Create a permissions object
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

        // Include public access in the permissions object
        containerPermissions
                .setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

        // Set the permissions on the container
        container.uploadPermissions(containerPermissions);

        // Upload the blob(s)
        // Get a reference to a blob in the container
        CloudBlockBlob blob = container
                .getBlockBlobReference(fileName != null ? fileName : "defaultNameForBlob");

        BlobRequestOptions blobRequestOptions = new BlobRequestOptions();
        blobRequestOptions.setConcurrentRequestCount(8);
        blobRequestOptions.setSingleBlobPutThresholdInBytes(65000000); // 62 MB

        AccessCondition accessCondition = new AccessCondition();
        OperationContext operationContext = new OperationContext();

        // Upload video to the blob
        blob.uploadFromFile(filePath, accessCondition, blobRequestOptions, operationContext);
        Log.d("getRequestResults: ",operationContext.getRequestResults()+"");
        return blob;
    }
}

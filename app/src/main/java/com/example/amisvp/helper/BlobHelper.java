package com.example.amisvp.helper;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.UUID;

public class BlobHelper {

    private static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;" +
                    "AccountName=thesisprojectstoacc;" +
                    "AccountKey=cHhwPJI1YFDwm4IW8VrYSlrS7/eBNAl0yjhCzJItjRbwOKaaTRE+3CGLbQ8trWUYVt2JmcmgtxVM+ASteqlIAw==;EndpointSuffix=core.windows.net";

    private void uploadBlobToContainer(String filePath, String fileName, String containerPathName) throws URISyntaxException, InvalidKeyException, StorageException, IOException {
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

        // Upload video to the blob
        blob.uploadFromFile(filePath);

        //blob.getUri();
    }

    public void uploadBlobToContainerTask(String filePath, String containerPathName){
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Path p = Paths.get(filePath);
                            String fileName = p.getFileName().toString();
                            uploadBlobToContainer(filePath, fileName, containerPathName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }

}

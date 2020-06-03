package com.zimbra.cs.redolog;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.XmlParserException;

public class MinIORedoBlobStore extends RedoLogBlobStore {

    private final MinioClient client;
    private final String bucketName;

    public MinIORedoBlobStore(BlobReferenceManager refManager) throws ServiceException {
        super(refManager);
        client = MinIOClientHolder.getInstance().getClient();
        bucketName = LC.backup_blob_store_s3_bucket.value();
    }

    @Override
    public Blob fetchBlob(String identifier) throws ServiceException {
        try {
            InputStream obj = client.getObject(bucketName, identifier);
            // Temp code
            Boolean compressed = false;
            return StoreManager.getInstance().storeIncoming(obj, compressed);
        } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                | XmlParserException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - fetchBlob failed: ", e);
        }
    }

    @Override
    protected void storeBlobData(InputStream in, long size, String digest) throws ServiceException {
        try {
            client.putObject(bucketName, digest, in, null);
        } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                | XmlParserException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - storeBlobData failed: ", e);
        }
    }

    @Override
    protected void deleteBlobData(String digest) {
        // TODO Auto-generated method stub
    }
}

package com.zimbra.cs.redolog;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.RedisRedoBlobStore.RedisReferenceManager;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.errors.XmlParserException;

public class MinIORedoBlobStore extends RedoLogBlobStore {

    private final MinioClient client;
    private final String bucketName;

    private enum BlobMetaData {
        SIZE;
    }

    private void createBucket() throws ServiceException {
        try {
            boolean bucketExists = client.bucketExists(bucketName);
            if (!bucketExists) {
                ZimbraLog.redolog.debug("MinIORedoBlobStore - createBucket - going to make bucket");
                client.makeBucket(bucketName);
            }
        } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                | XmlParserException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - createBucket failed: ", e);
        } catch (RegionConflictException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - createBucket failed - RegionConflictException: ", e);
        }
    }

    public MinIORedoBlobStore(BlobReferenceManager refManager) throws ServiceException {
        super(refManager);
        client = MinIOClientHolder.getInstance().getClient();
        bucketName = LC.backup_blob_store_s3_bucket.value();
        createBucket();
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
            Map<String, String> metadataMap = new HashMap<String, String>();
            metadataMap.put(BlobMetaData.SIZE.toString(), Long.toString(size));

            PutObjectOptions options = new PutObjectOptions(in.available(), -1);
            options.setHeaders(metadataMap);

            client.putObject(bucketName, digest, in, options);
        } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                | XmlParserException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - storeBlobData failed: ", e);
        }
    }

    @Override
    protected void deleteBlobData(String digest) throws ServiceException {
        try {
            client.removeObject(bucketName, digest);
        } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                | XmlParserException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - deleteBlobData failed: ", e);
        }
    }

    public static class Factory implements RedoLogBlobStore.Factory {

        @Override
        public MinIORedoBlobStore getRedoLogBlobStore() throws ServiceException {
            //Temporary code to use RedisReferenceManager as a BlobReferenceManager
            return new MinIORedoBlobStore(new RedisReferenceManager());
        }
    }
}

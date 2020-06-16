package com.zimbra.cs.redolog;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

import io.minio.MinioClient;
import io.minio.ObjectStat;
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
        BLOBSIZE("x-amz-meta-blobsize");
        private final String key;
        BlobMetaData(String key) {
            this.key = key;
        }
        public String getKey() {
            return key;
        }
    }

    private void createBucket() throws ServiceException {
        try {
            boolean bucketExists = client.bucketExists(bucketName);
            if (!bucketExists) {
                ZimbraLog.redolog.debug("MinIORedoBlobStore - createBucket - going to make bucket");
                client.makeBucket(bucketName);
            }
            ZimbraLog.redolog.debug("MinIORedoBlobStore - createBucket - bucket already exists");
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
        ZimbraLog.redolog.debug("MinIORedoBlobStore - fetchBlob - digest: %s", identifier);
        InputStream obj = null;
        try {
            obj = client.getObject(bucketName, identifier);
            ObjectStat objectStat = client.statObject(bucketName, identifier);
            Map<String, List<String>> metadataMap = objectStat.httpHeaders();
            List<String> sizeList = metadataMap.get(BlobMetaData.BLOBSIZE.getKey());
            if (sizeList == null) {
                throw ServiceException.NOT_FOUND("MinIORedoBlobStore - fetchBlob - Missing meta information: "
                        + BlobMetaData.BLOBSIZE.toString());
            }
            Long size = Long.parseLong(sizeList.get(0));
            ZimbraLog.redolog.debug("MinIORedoBlobStore - fetchBlob - size: %d", size);
            Boolean compressed = obj.available() < size;
            return StoreManager.getInstance().storeIncoming(obj, compressed);
        } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                | XmlParserException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - fetchBlob failed: ", e);
        } catch (IndexOutOfBoundsException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - fetchBlob failed - Missing data: ", e);
        } finally {
            ByteUtil.closeStream(obj);
        }
    }

    @Override
    protected void storeBlobData(InputStream in, long size, String digest) throws ServiceException {
        ZimbraLog.redolog.debug("MinIORedoBlobStore - storeBlobData - size: %d, digest: %s", size, digest);
        try {
            Map<String, String> metadataMap = new HashMap<String, String>();
            metadataMap.put(BlobMetaData.BLOBSIZE.toString(), Long.toString(size));

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

    public static class MinIOReferenceManager extends RedoLogBlobStore.BlobReferenceManager {

        MinIOReferenceManager() {
        }

        @Override
        public boolean addRefs(String digest, Collection<Integer> mboxIds) {

            return true;
        }

        @Override
        public boolean removeRefs(String digest, Collection<Integer> mboxIds) {
            return true;
        }
    }

    public static class Factory implements RedoLogBlobStore.Factory {

        @Override
        public MinIORedoBlobStore getRedoLogBlobStore() throws ServiceException {
            return new MinIORedoBlobStore(new MinIOReferenceManager());
        }
    }
}

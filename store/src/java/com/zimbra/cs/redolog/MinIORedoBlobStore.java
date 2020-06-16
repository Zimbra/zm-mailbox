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

import io.minio.ErrorCode;
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

    public MinIORedoBlobStore(BlobReferenceManager refManager) throws ServiceException {
        super(refManager);
        client = MinIOClientHolder.getInstance().getClient();
        bucketName = LC.backup_blob_store_s3_bucket.value();
        MinIOUtil.createBucket(client, bucketName);
    }

    @Override
    public Blob fetchBlob(String identifier) throws ServiceException {
        ZimbraLog.redolog.debug("MinIORedoBlobStore - fetchBlob - digest: %s", identifier);
        InputStream obj = null;
        try {
            obj = MinIOUtil.getObject(client, bucketName, identifier);
            if(obj == null) {
                throw ServiceException.NOT_FOUND("MinIORedoBlobStore - fetchBlob failed, object does not exist for identifier: "+ identifier);
            }

            ObjectStat objectStat = MinIOUtil.statObject(client, bucketName, identifier);

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
        } catch (IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - fetchBlob failed: ", e);
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

            MinIOUtil.putObject(client, bucketName, digest, in, options);
        } catch (IllegalArgumentException | IOException e) {
            throw ServiceException.FAILURE("MinIORedoBlobStore - storeBlobData failed: ", e);
        }
    }

    @Override
    protected void deleteBlobData(String digest) throws ServiceException {
        MinIOUtil.deleteObject(client, bucketName, digest);
    }

    public static class MinIOReferenceManager extends RedoLogBlobStore.BlobReferenceManager {

        private final MinioClient client;
        private final String bucketName;

        MinIOReferenceManager() throws ServiceException {
            client = MinIOClientHolder.getInstance().getClient();
            bucketName = LC.backup_blob_store_s3_bucket.value();
            MinIOUtil.createBucket(client, bucketName);
        }

        /**
         * Adds the Ids against the digest key. Returns true on successful addition else
         * false.
         */
        @Override
        public boolean addRefs(String digest, Collection<Integer> mboxIds) {

            return true;
        }

        /**
         * Remove the Ids mapped to the digest key. Returns true if the cardinality of
         * values mapped to key is equal to zero else false. if the cardinality of
         * values mapped to key is equal to zero delete the key.
         */
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

    private static class MinIOUtil {

        public static void createBucket(MinioClient client, String bucketName) throws ServiceException {
            try {
                boolean bucketExists = client.bucketExists(bucketName);
                if (!bucketExists) {
                    ZimbraLog.redolog.debug("MinIOUtil - createBucket - going to make bucket");
                    client.makeBucket(bucketName);
                }
                ZimbraLog.redolog.debug("MinIOUtil - createBucket - bucket already exists");
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidBucketNameException | InvalidResponseException
                    | NoSuchAlgorithmException | XmlParserException | IOException e) {
                throw ServiceException.FAILURE("MinIOUtil - createBucket failed: ", e);
            } catch (RegionConflictException e) {
                throw ServiceException.FAILURE("MinIOUtil - createBucket failed - RegionConflictException: ", e);
            }
        }

        private static void putObject(MinioClient client, String bucketName, String key, InputStream is,
                PutObjectOptions options) throws ServiceException {
            try {
                client.putObject(bucketName, key, is, options);
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidBucketNameException | InvalidResponseException
                    | NoSuchAlgorithmException | XmlParserException | IOException e) {
                throw ServiceException.FAILURE("MinIOUtil - putObject failed: ", e);
            }
        }

        private static InputStream getObject(MinioClient client, String bucketName, String key)
                throws ServiceException {
            try {
                return client.getObject(bucketName, key);
            } catch (InvalidKeyException | IllegalArgumentException | InsufficientDataException | InternalException
                    | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException
                    | XmlParserException | IOException e) {
                throw ServiceException.FAILURE("MinIOUtil - getObject failed: ", e);
            } catch (ErrorResponseException e) {
                ErrorCode code = e.errorResponse().errorCode();
                if (code == ErrorCode.NO_SUCH_OBJECT) {
                    return null;
                }
                throw ServiceException.FAILURE("MinIOUtil - getObject failed: ", e);
            }
        }

        private static ObjectStat statObject(MinioClient client, String bucketName, String key)
                throws ServiceException {
            try {
                return client.statObject(bucketName, key);
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidBucketNameException | InvalidResponseException
                    | NoSuchAlgorithmException | XmlParserException | IOException e) {
                throw ServiceException.FAILURE("MinIOUtil - statObject failed: ", e);
            }
        }

        private static void deleteObject(MinioClient client, String bucketName, String key) throws ServiceException {
            try {
                client.removeObject(bucketName, key);
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidBucketNameException | InvalidResponseException
                    | NoSuchAlgorithmException | XmlParserException | IOException e) {
                throw ServiceException.FAILURE("MinIOUtil - deleteObject failed: ", e);
            }
        }
    }
}

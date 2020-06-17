package com.zimbra.cs.redolog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
        private final String metaPrefix;

        MinIOReferenceManager() throws ServiceException {
            client = MinIOClientHolder.getInstance().getClient();
            bucketName = LC.backup_blob_store_s3_bucket.value();
            metaPrefix = LC.backup_blob_store_s3_object_ref_prefix.value();
            MinIOUtil.createBucket(client, bucketName);
        }

        private String getKey(String digest) {
            return metaPrefix + digest;
        }

        /**
         * Adds the Ids against the digest key. Returns true on successful addition else
         * false.
         * @throws ServiceException
         */
        @Override
        public boolean addRefs(String digest, Collection<Integer> mboxIds) throws ServiceException {
            InputStream iS = null;
            ObjectInputStream objIS = null;
            InputStream modifiedIS = null;
            ByteArrayOutputStream bAOS = null;
            ObjectOutputStream objOS = null;

            try {
                iS = MinIOUtil.getObject(client, bucketName, getKey(digest));
                HashMap<Integer, Integer> digestRefHolder = null;

                if (iS != null) {
                    objIS = new ObjectInputStream(iS);
                    Object obj = objIS.readObject();

                    if (obj instanceof HashMap<?,?>) {
                        digestRefHolder = (HashMap<Integer, Integer>) objIS.readObject();
                    } else {
                        throw ServiceException.NOT_FOUND(
                                "MinIORedoBlobStore - addRefs - deserialization failed for digest: " + digest);
                    }
                } else {
                    digestRefHolder = new HashMap<Integer, Integer>();
                }

                digestRefHolder.replaceAll((k, v) -> {
                    if (mboxIds.contains(k))
                        return v + 1;
                    else {
                        return v;
                    }
                });

                for (Integer id : mboxIds) {
                    if (!digestRefHolder.containsKey(id))
                        digestRefHolder.put(id, 1);
                }

                byte[] bArray = null;
                bAOS = new ByteArrayOutputStream();
                objOS = new ObjectOutputStream(bAOS);
                objOS.writeObject(digestRefHolder);
                bArray = bAOS.toByteArray();
                modifiedIS = new ByteArrayInputStream(bArray);

                PutObjectOptions options = new PutObjectOptions(modifiedIS.available(), -1);
                MinIOUtil.putObject(client, bucketName, digest, modifiedIS, options);

                return true;
            } catch (IOException | ClassNotFoundException e) {
                throw ServiceException.FAILURE("MinIORedoBlobStore - addRefs failed: ", e);
            } finally {
                ByteUtil.closeStream(iS);
                ByteUtil.closeStream(objIS);
                ByteUtil.closeStream(modifiedIS);
                ByteUtil.closeStream(bAOS);
                ByteUtil.closeStream(objOS);
            }
        }

        /**
         * Remove the Ids mapped to the digest key. Returns true if the cardinality of
         * values mapped to key is equal to zero else false. if the cardinality of
         * values mapped to key is equal to zero delete the key.
         * 
         * @throws ServiceException
         */
        @Override
        public boolean removeRefs(String digest, Collection<Integer> mboxIds) throws ServiceException {
            InputStream iS = null;
            ObjectInputStream objIS = null;
            InputStream modifiedIS = null;
            ByteArrayOutputStream bAOS = null;
            ObjectOutputStream objOS = null;

            try {
                iS = MinIOUtil.getObject(client, bucketName, getKey(digest));
                if (iS == null) {
                    return true;
                }

                objIS = new ObjectInputStream(iS);
                Object obj = objIS.readObject();

                if (obj instanceof HashMap<?, ?>) {
                    HashMap<Integer, Integer> digestRefHolder = (HashMap<Integer, Integer>) objIS.readObject();

                    List<Integer> tracker = new ArrayList<Integer>();

                    digestRefHolder.replaceAll((k, v) -> {
                        if (mboxIds.contains(k)) {
                            Integer val = v - 1;
                            if (val <= 0) {
                                tracker.add(k);
                            }
                            return val;
                        } else {
                            return v;
                        }
                    });

                    for (Integer id : tracker) {
                        digestRefHolder.remove(id);
                    }

                    if (digestRefHolder.isEmpty()) {
                        MinIOUtil.deleteObject(client, bucketName, getKey(digest));
                        return true;
                    }

                    byte[] bArray = null;
                    bAOS = new ByteArrayOutputStream();
                    objOS = new ObjectOutputStream(bAOS);
                    objOS.writeObject(digestRefHolder);
                    bArray = bAOS.toByteArray();

                    modifiedIS = new ByteArrayInputStream(bArray);

                    PutObjectOptions options = new PutObjectOptions(modifiedIS.available(), -1);
                    MinIOUtil.putObject(client, bucketName, digest, modifiedIS, options);

                    return false;
                }
                throw ServiceException
                        .NOT_FOUND("MinIORedoBlobStore - removeRefs - deserialization failed for digest: " + digest);
            } catch (IOException | ClassNotFoundException e) {
                throw ServiceException.FAILURE("MinIORedoBlobStore - fetchBlob failed: ", e);
            } finally {
                ByteUtil.closeStream(iS);
                ByteUtil.closeStream(objIS);
                ByteUtil.closeStream(modifiedIS);
                ByteUtil.closeStream(bAOS);
                ByteUtil.closeStream(objOS);
            }
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

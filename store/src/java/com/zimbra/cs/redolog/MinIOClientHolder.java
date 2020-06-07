package com.zimbra.cs.redolog;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;

public final class MinIOClientHolder {

    private static MinIOClientHolder instance = null;
    private MinioClient minioClient;

    private MinIOClientHolder() {
    }

    void initClient() throws ServiceException {
        try {
            minioClient = new MinioClient(LC.backup_blob_store_service_uri.value(),
                    LC.backup_blob_store_service_port.intValue(), LC.backup_blob_store_access_key.value(),
                    LC.backup_blob_store_secret_key.value(), false);

        } catch (InvalidEndpointException | InvalidPortException e) {
            throw ServiceException.FAILURE("MinIOClientHolder - initClient failed: ", e);
        }
    }

    MinioClient getClient() {
        return minioClient;
    }

    public static MinIOClientHolder getInstance() throws ServiceException {
        if (instance != null) {
            return instance;
        }
        synchronized (MinIOClientHolder.class) {
            if (instance == null) {
                MinIOClientHolder minioInstance =  new MinIOClientHolder();
                minioInstance.initClient();
                instance = minioInstance;
            }
            return instance;
        }
    }
}

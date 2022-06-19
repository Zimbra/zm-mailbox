/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2022 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.util;

import com.zimbra.common.service.ServiceException;
// import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;

import com.zimbra.cs.account.Server;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Properties;

/**
import static com.zimbra.extended.common.Constants.GLOBAL_S3_BUCKET_CONFIG_FIELD_NAME;
import static com.zimbra.extended.common.Constants.GLOBAL_S3_BUCKET_CONFIG_FIELD_BUCKET_ID;
import static com.zimbra.extended.common.Constants.SERVER_VOLUME_ID_FIELD_NAME;
import static com.zimbra.extended.common.Constants.SERVER_VOLUME_GLOBAL_BUCKET_CONFIG_ID;
import static com.zimbra.extended.common.Constants.SERVER_VOLUMES_CONFIG_FIELD_NAME;
import static com.zimbra.extended.common.StoreManagersLogger.LOG;
**/

/**
 * LDAP based properties reader
 */
public class ExternalVolumeReader {

    /**
     * LDAP Provision instance
     */
    private Provisioning provisioning;

    public ExternalVolumeReader(Provisioning provisioning) {
        this.provisioning = provisioning;
    }

    /**
     * Get volume properties by volume ID
     * @param volumeId
     * @return Properties object as complete set of volume properties
     * @throws ServiceException
     * @throws JSONException
     */
    public Properties getProperties(int volumeId) throws ServiceException, JSONException {

        Server server = provisioning.getLocalServer();
        Properties serverLevelProps = readServerProperties(volumeId, server);
        /**
        String globalBucketConfigId = serverLevelProps.getProperty(SERVER_VOLUME_GLOBAL_BUCKET_CONFIG_ID);
        if (!StringUtil.isNullOrEmpty(globalBucketConfigId)) {
            Properties globalLevelProps = readGlobalProperties(globalBucketConfigId, server);
            for (String propertyKey : globalLevelProps.stringPropertyNames()) {

                serverLevelProps.putIfAbsent(propertyKey, globalLevelProps.getProperty(propertyKey));
            }
        }
        **/
        return serverLevelProps;
    }

    /**
     * Read server level external volume properties
     * @param volumeId
     * @param server
     * @return Properties as a server level volume properties
     * @throws JSONException
     */
    protected Properties readServerProperties(int volumeId, Server server) throws JSONException {
        Properties properties = new Properties();
        try {
            String serverExternalStoreConfigJson = server.getServerExternalStoreConfig();
            JSONObject jsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray volumePropsArray = jsonObject.getJSONArray("server/stores");
            for (int i = 0; i < volumePropsArray.length(); i++) {
                JSONObject volumeJsonObj = volumePropsArray.getJSONObject(i);
                if (volumeId == volumeJsonObj.getInt("volumeId")) {
                    convertFlatJsoObjectToProperties(properties, volumeJsonObj);
                    break;
                }
            }
        } catch (JSONException e) {
            // LOG.error("Error while processing ldap attribute GlobalExternalStoreConfig", e);
            throw e;
        }
        return properties;
    }

    /**
     * Get global level s3 config from LDAP global attr, by global bucket id
     * @param globalS3BucketId
     * @param server
     * @return Properties object as global bucket properties
     * @throws JSONException
     */
     /**
    protected Properties readGlobalProperties(String globalS3BucketId, Server server) throws JSONException {
        Properties properties = new Properties();
        try {
            String globalExternalStoreConfigJson = server.getGlobalExternalStoreConfig();
            JSONObject jsonObject = new JSONObject(globalExternalStoreConfigJson);
            JSONArray s3GlobalConfigArrayJson = jsonObject.getJSONArray(GLOBAL_S3_BUCKET_CONFIG_FIELD_NAME);
            for (int i = 0; i < s3GlobalConfigArrayJson.length(); i++) {
                JSONObject s3JsonObj = s3GlobalConfigArrayJson.getJSONObject(i);
                if (globalS3BucketId.equalsIgnoreCase(s3JsonObj.getString(GLOBAL_S3_BUCKET_CONFIG_FIELD_BUCKET_ID))) {
                    convertFlatJsoObjectToProperties(properties, s3JsonObj);
                    break;
                }
            }
        } catch (JSONException e) {
            LOG.error("Error while processing ldap attribute GlobalExternalStoreConfig", e);
            throw e;
        }
        return properties;
    }
    **/

    /**
     * Utility method for flat json to properties key-value
     * @param properties
     * @param flatJsonObject
     * @throws JSONException
     */
    protected void convertFlatJsoObjectToProperties(Properties properties, JSONObject flatJsonObject) throws JSONException {
        for (Iterator it = flatJsonObject.keys(); it.hasNext(); ) {
            Object fieldObject = it.next();
            if (fieldObject instanceof String) {
                String field = String.valueOf(fieldObject);
                properties.put(field, flatJsonObject.get(field));
            }
        }
    }

}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2022 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.soap.admin.type.VolumeExternalInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Properties;

/**
 * LDAP based properties handler
 */
public class ExternalVolumeInfoHandler {

    /**
     * LDAP Provision instance
     */
    private Provisioning provisioning;

    public ExternalVolumeInfoHandler(Provisioning provisioning) {
        this.provisioning = provisioning;
    }

    /**
     * Read server level external volume properties
     * @param volumeId
     * @param server
     * @return Properties as a server level volume properties
     * @throws JSONException, ServiceException
     */
    public Properties readServerProperties(int volumeId) throws ServiceException, JSONException {
        Properties properties = new Properties();
        try {

            // step 1: Fetch current JSON state object and current JSON state array
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // step 2: Iterate JSON state array
            for (int i = 0; i < currentJsonArray.length(); i++) {
                JSONObject tempJsonObj = currentJsonArray.getJSONObject(i);

                // step 3: Read required JSON object
                if (volumeId == tempJsonObj.getInt("volumeId")) {

                    // step 4: Convert JSON object to Properties
                    convertFlatJsonObjectToProperties(properties, tempJsonObj);
                    break;
                }
            }
        } catch (JSONException e) {
            throw e;
        }

        // step 5: Return Properties
        return properties;
    }

    /**
     * Delete server level external volume properties
     * @param volumeId
     * @param server
     * @throws JSONException, ServiceException
     */
    public void deleteServerProperties(int volumeId) throws ServiceException, JSONException {
        try {

            // step 1: Fetch current JSON state object and current JSON state array
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // step 2: Create new/empty updated JSON state array
            JSONArray updatedJsonArray = new JSONArray();

            // step 3: Copy currentJsonArray to updatedJsonArray except the element to be deleted
            for (int i = 0; i < currentJsonArray.length(); i++) {
                JSONObject tempJsonObj = currentJsonArray.getJSONObject(i);
                if (volumeId != tempJsonObj.getInt("volumeId")) {
                    updatedJsonArray.put(tempJsonObj);
                }
            }

            // step 4: Copy updatedJsonArray to currentJsonArray
            currentJsonArray = updatedJsonArray;

            // step 5: Copy updatedJsonArray to currentJsonObject
            currentJsonObject.put("server/stores", currentJsonArray);

            // step 6: Set ldap attribute
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());

        } catch (JSONException e) {
            throw e;
        }
    }

    /**
     * Add server level external volume properties
     * @param volumeId
     * @param server
     * @throws JSONException, ServiceException
     */
    public void addServerProperties(VolumeInfo volInfo) throws ServiceException, JSONException {
        VolumeExternalInfo volExtInfo = volInfo.getVolumeExternalInfo();
        try {
            // step 1: Create and update json object and array for new volume entry
            JSONObject volExtInfoObj = new JSONObject();
            volExtInfoObj.put("volumeId", String.valueOf(volInfo.getId()));
            volExtInfoObj.put("storageType", volExtInfo.getStorageType());
            volExtInfoObj.put("volumePrefix", volExtInfo.getVolumePrefix());
            volExtInfoObj.put("useInFrequentAccess", String.valueOf(volExtInfo.getUseInFrequentAccess()));
            volExtInfoObj.put("useIntelligentTiering", String.valueOf(volExtInfo.getUseIntelligentTiering()));
            volExtInfoObj.put("glbBucketConfigId", volExtInfo.getGlobalBucketConfigurationId());
            volExtInfoObj.put("useInFrequentAccessThreshold", String.valueOf(volExtInfo.getUseInFrequentAccessThreshold()));

            // step 2: Fetch current JSON state
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = null;
            if (StringUtil.isNullOrEmpty(serverExternalStoreConfigJson)) {
                // If current JSON state is already empty, initialise current JSON state Object
                currentJsonObject = new JSONObject();
                currentJsonObject.put("server/stores", new JSONArray());
            } else {
                // Else convert current JSON state string to current JSON state Object
                currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            }

            // step 3: Fetch current JSON state array
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // step 4: Append current JSON state array with new volume entry
            currentJsonArray.put(volExtInfoObj);

            // step 5: Overwrite Appended current JSON state array with current JSON state Object
            currentJsonObject.put("server/stores", currentJsonArray);

            // step 6: Set ldap attribute
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
        } catch (JSONException e) {
            throw e;
        }
    }

    /**
     * Modify server level external volume properties
     * @param volumeId
     * @param server
     * @throws JSONException, ServiceException
     */
    public void modifyServerProperties(VolumeInfo volInfo) throws ServiceException, JSONException {
        try {

            // step 1: Fetch current JSON state object and current JSON state array
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // step 2: Create new/empty updated JSON state array
            JSONArray updatedJsonArray = new JSONArray();

            // step 3: Do Modification and Copy currentJsonArray to updatedJsonArray
            for (int i = 0; i < currentJsonArray.length(); i++) {
                JSONObject tempJsonObj = currentJsonArray.getJSONObject(i);
                if (volInfo.getId() != tempJsonObj.getInt("volumeId")) {
                    updatedJsonArray.put(tempJsonObj);
                } else {
                    tempJsonObj.put("name", volInfo.getName());
                    updatedJsonArray.put(tempJsonObj);
                }
            }

            // step 4: Copy updatedJsonArray to currentJsonArray
            currentJsonArray = updatedJsonArray;

            // step 5: Copy updatedJsonArray to currentJsonObject
            currentJsonObject.put("server/stores", currentJsonArray);

            // step 6: Set ldap attribute
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
        }  catch (JSONException e) {
            throw e;
        }
    }

    /**
     * Utility method for flat json to properties key-value
     * @param properties
     * @param flatJsonObject
     * @throws JSONException
     */
    protected void convertFlatJsonObjectToProperties(Properties properties, JSONObject flatJsonObject) throws JSONException {
        if (flatJsonObject != null) {
            for (Iterator it = flatJsonObject.keys(); it.hasNext();) {
                Object fieldObject = it.next();
                if (fieldObject instanceof String) {
                    String field = String.valueOf(fieldObject);
                    properties.put(field, flatJsonObject.get(field));
                }
            }
        }
    }
}

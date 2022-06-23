/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2022 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Properties;

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
     * Read server level external volume properties
     * @param volumeId
     * @param server
     * @return Properties as a server level volume properties
     * @throws JSONException
     */
    public Properties readServerProperties(int volumeId) throws JSONException, ServiceException {
        // LOG.info("readServerProperties, volID : [%d]", volumeId);
        Properties properties = new Properties();
        try {
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
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
            // LOG.error("Error while processing ldap attribute ServerExternalStoreConfig", e);
            throw e;
        }
        return properties;
    }

    public void deleteServerProperties(int volumeId) throws JSONException, ServiceException {
        // LOG.info("deleteServerProperties, volID : [%d]", volumeId);
        try {
            // Step 1: Fetch current JSON state object and current JSON state array
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // Step 2: Create new/empty updated JSON state array
            JSONArray updatedJsonArray = new JSONArray();

            // Step 3: Copy currentJsonArray to updatedJsonArray except the element to be deleted
            for (int i = 0; i < currentJsonArray.length(); i++) {
                JSONObject tempJsonObj = currentJsonArray.getJSONObject(i);
                if (volumeId != tempJsonObj.getInt("volumeId")) {
                    updatedJsonArray.put(tempJsonObj);
                }
            }

            // Step 4: Copy updatedJsonArray to currentJsonArray
            currentJsonArray = updatedJsonArray;

            // Step 5: Copy updatedJsonArray to currentJsonObject
            currentJsonObject.put("server/stores", currentJsonArray);

            // Step 6: Set ldap attribute
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());

        } catch (JSONException e) {
            // LOG.error("Error while processing ldap attribute ServerExternalStoreConfig", e);
            throw e;
        }
    }

    public void addServerProperties(VolumeInfo volInfo) throws JSONException, ServiceException {
        // LOG.info("addToServerProperties, volID : []");
        VolumeExternalInfo volExtInfo = volInfo.getVolumeExternalInfo();
        try {

            // Step 1: Create and update json object and array for new volume entry
            JSONObject volExtInfoObj = new JSONObject();
            volExtInfoObj.put("storageType", volExtInfo.getStorageType());
            volExtInfoObj.put("volumePrefix", volExtInfo.getVolumePrefix());
            volExtInfoObj.put("storeProvider", volExtInfo.getStoreProvider());
            volExtInfoObj.put("glbBucketConfigId", volExtInfo.getGlobalBucketConfigurationId());
            volExtInfoObj.put("useInFrequentAccess", volExtInfo.getUseInFrequentAccess());
            volExtInfoObj.put("useIntelligentTiering", volExtInfo.getUseIntelligentTiering());
            volExtInfoObj.put("useInFrequentAccessThreshold", volExtInfo.getUseInFrequentAccessThreshold());
            volExtInfoObj.put("volumeId", volInfo.getId());
            volExtInfoObj.put("name", volInfo.getName());

            // Step 2: Fetch current JSON state
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = null;
            if(StringUtil.isNullOrEmpty(serverExternalStoreConfigJson)) {
                // If current JSON state is already empty, initialise current JSON state Object
                currentJsonObject = new JSONObject();
                currentJsonObject.put("server/stores", new JSONArray());
            } else {
                // Else convert current JSON state string to current JSON state Object
                currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            }

            // Step 3: Fetch current JSON state array
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // Step 4: Append current JSON state array with new volume entry
            currentJsonArray.put(volExtInfoObj);

            // Step 5: Overwrite Appended current JSON state array with current JSON state Object
            currentJsonObject.put("server/stores", currentJsonArray);

            // Step 6: Set ldap attribute
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
        }
        catch (JSONException e) {
            // LOG.error("Error while processing ldap attribute ServerExternalStoreConfig", e);
            throw e;
        }
    }

    public void ModifyServerProperties(VolumeInfo volInfo) throws JSONException, ServiceException {
        // LOG.info("addToServerProperties, volID : []");
        // VolumeExternalInfo volExtInfo = volInfo.getVolumeExternalInfo();
        try {
            // Step 1: Fetch current JSON state object and current JSON state array
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray currentJsonArray = currentJsonObject.getJSONArray("server/stores");

            // Step 2: Create new/empty updated JSON state array
            JSONArray updatedJsonArray = new JSONArray();

            // Step 3: Do Modification and Copy currentJsonArray to updatedJsonArray
            for (int i = 0; i < currentJsonArray.length(); i++) {
                JSONObject tempJsonObj = currentJsonArray.getJSONObject(i);
                if (volInfo.getId() != tempJsonObj.getInt("volumeId")) {
                    updatedJsonArray.put(tempJsonObj);
                }
                else {
                    tempJsonObj.put("name", volInfo.getName());
                    updatedJsonArray.put(tempJsonObj);
                }
            }

            // Step 4: Copy updatedJsonArray to currentJsonArray
            currentJsonArray = updatedJsonArray;

            // Step 5: Copy updatedJsonArray to currentJsonObject
            currentJsonObject.put("server/stores", currentJsonArray);

            // Step 6: Set ldap attribute
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
        }
        catch (JSONException e) {
            // LOG.error("Error while processing ldap attribute ServerExternalStoreConfig", e);
            throw e;
        }
    }


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

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
     * @return JSONObject as a server level volume properties
     * @throws JSONException, ServiceException
     */
    public JSONObject readServerProperties(int volumeId) throws ServiceException, JSONException {
        JSONObject retJsonObj = new JSONObject();
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
                    // step 4: Copy temp JSON object to return JSON object and break
                    retJsonObj = tempJsonObj;
                    break;
                }
            }
        } catch (JSONException e) {
            throw e;
        }

        // step 5: Return JSON object
        return retJsonObj;
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
                if (volumeId != tempJsonObj.getInt(AdminConstants.A_VOLUME_ID)) {
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
            volExtInfoObj.put(AdminConstants.A_VOLUME_ID, String.valueOf(volInfo.getId()));
            volExtInfoObj.put(AdminConstants.A_VOLUME_STORAGE_TYPE, volExtInfo.getStorageType());
            volExtInfoObj.put(AdminConstants.A_VOLUME_VOLUME_PREFIX, volExtInfo.getVolumePrefix());
            volExtInfoObj.put(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS, String.valueOf(volExtInfo.getUseInFrequentAccess()));
            volExtInfoObj.put(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING, String.valueOf(volExtInfo.getUseIntelligentTiering()));
            volExtInfoObj.put(AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID, volExtInfo.getGlobalBucketConfigurationId());
            volExtInfoObj.put(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD, String.valueOf(volExtInfo.getUseInFrequentAccessThreshold()));

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
                if (volInfo.getId() != tempJsonObj.getInt(AdminConstants.A_VOLUME_ID)) {
                    updatedJsonArray.put(tempJsonObj);
                } else {
                    tempJsonObj.put(AdminConstants.A_VOLUME_NAME, volInfo.getName());
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
     * Validate global bucket ID by reading LDAP atrribute
     * @param globalS3BucketId
     * @returns true if "global bucket ID" is valid
     * @throws ServiceException, JSONException
     */
    public Boolean validateGlobalBucketID(String globalS3BucketId) throws ServiceException {
        try {
            // step 1: Fetch globalS3Config JSON state object and globalS3Config JSON state array
            String globalExternalStoreConfigJSON = provisioning.getConfig().getGlobalExternalStoreConfig();
            JSONObject globalS3ConfigJSONObject = new JSONObject(globalExternalStoreConfigJSON);
            JSONArray globalS3ConfigJSONArray = globalS3ConfigJSONObject.getJSONArray("global/s3BucketConfigurations");

            // step 2: Find "globalBucketUUID" in current JSON array
            for (int i = 0; i < globalS3ConfigJSONArray.length(); i++) {
                // step 3: Mark validation as true if "globalBucketUUID" found
                if (globalS3BucketId.equalsIgnoreCase(globalS3ConfigJSONArray.getJSONObject(i).getString("globalBucketUUID"))) {
                    return true;
                }
            }
        } catch (JSONException e) {
            throw ServiceException.FAILURE("Error while validating GlobalBucketID", null);
        }
        return false;
    }

    /**
     * Validate if external volume entry is present in JSON or not
     * @param volumeId
     * @returns true if external volume entry is present
     * @throws ServiceException, JSONException
     */
    public Boolean isVolumePresentInJson(int volumeId) throws ServiceException, JSONException {
      String serverS3ConfigJSON = provisioning.getLocalServer().getServerExternalStoreConfig();
      JSONObject serverS3ConfigJSONObject = new JSONObject(serverS3ConfigJSON);
      JSONArray serverS3ConfigJSONArray = serverS3ConfigJSONObject.getJSONArray("server/stores");
      for (int i = 0; i < serverS3ConfigJSONArray.length(); i++) {
          if (volumeId == serverS3ConfigJSONArray.getJSONObject(i).getInt(AdminConstants.A_VOLUME_ID)) {
              return true;
          }
      }
      return false;
    }
}

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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.soap.admin.type.VolumeExternalInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.io.Writer;

/**
 * LDAP based properties handler
 */
public class ExternalVolumeInfoHandler {

    public static final String PREV_FILE_NAME = "zsesc_prev.json";
    public static final String PREV_DIR_PATH = File.separator + "opt" + File.separator + "zimbra" + File.separator + "smconfig";
    public static final String PREV_FILE_PATH = PREV_DIR_PATH + File.separator + PREV_FILE_NAME;

    public static final String CURR_FILE_NAME = "zsesc_curr.json";
    public static final String CURR_DIR_PATH = File.separator + "opt" + File.separator + "zimbra" + File.separator + "smconfig";
    public static final String CURR_FILE_PATH = CURR_DIR_PATH + File.separator + CURR_FILE_NAME;

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
     * Read backup server level external volume properties
     * @param volumeId
     * @param filePath
     *
     * @return JSONObject as a backup server level volume properties
     * @throws JSONException, ServiceException
     */
    public JSONObject readBackupServerProperties(int volumeId, String filePath) throws ServiceException, JSONException {
        JSONObject retJsonObj = new JSONObject();
        try {
            // step 1: Fetch current JSON state object and current JSON state array
            String serverExternalStoreConfigJson = readServerJSONFile(filePath);
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
            writeServerJSONFile(provisioning.getLocalServer().getServerExternalStoreConfig(), PREV_DIR_PATH, PREV_FILE_PATH);
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
            writeServerJSONFile(provisioning.getLocalServer().getServerExternalStoreConfig(), CURR_DIR_PATH, CURR_FILE_PATH);
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
            volExtInfoObj.put(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS, String.valueOf(volExtInfo.isUseInFrequentAccess()));
            volExtInfoObj.put(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING, String.valueOf(volExtInfo.isUseIntelligentTiering()));
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
            writeServerJSONFile(provisioning.getLocalServer().getServerExternalStoreConfig(), PREV_DIR_PATH, PREV_FILE_PATH);
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
            writeServerJSONFile(provisioning.getLocalServer().getServerExternalStoreConfig(), CURR_DIR_PATH, CURR_FILE_PATH);
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
            writeServerJSONFile(provisioning.getLocalServer().getServerExternalStoreConfig(), PREV_DIR_PATH, PREV_FILE_PATH);
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
            writeServerJSONFile(provisioning.getLocalServer().getServerExternalStoreConfig(), CURR_DIR_PATH, CURR_FILE_PATH);
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
            // step 1: Fetch globalS3Configs and globalS3ConfigList
            String globalExternalStoreConfig = provisioning.getConfig().getGlobalExternalStoreConfig();
            JSONObject globalS3Configs = new JSONObject(globalExternalStoreConfig);
            JSONArray globalS3ConfigList = globalS3Configs.getJSONArray("global/s3BucketConfigurations");

            // step 2: Find "globalBucketUUID" in current JSON array
            for (int i = 0; i < globalS3ConfigList.length(); i++) {
                // step 3: Mark validation as true if "globalBucketUUID" found
                if (globalS3BucketId.equalsIgnoreCase(globalS3ConfigList.getJSONObject(i).getString("globalBucketUUID"))) {
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
        String globalExternalStoreConfig = provisioning.getLocalServer().getServerExternalStoreConfig();
        JSONObject globalS3Configs = new JSONObject(globalExternalStoreConfig);
        JSONArray globalS3ConfigList = globalS3Configs.getJSONArray("server/stores");
        for (int i = 0; i < globalS3ConfigList.length(); i++) {
            if (volumeId == globalS3ConfigList.getJSONObject(i).getInt(AdminConstants.A_VOLUME_ID)) {
                return true;
            }
        }
        return false;
    }

    private void writeServerJSONFile(String data, String dirPath, String filePath) {
        try {
            File serverJSONBackupDir = new File(dirPath);
            File serverJSONBackupFile = new File(filePath);
            // check if dir exists
            if (!serverJSONBackupDir.exists()) {
                serverJSONBackupDir.mkdirs();
            }
            // check if file exists
            if (!serverJSONBackupFile.exists()) {
                serverJSONBackupFile.createNewFile();
            }
            Writer fileWriter = new FileWriter(serverJSONBackupFile, false); //overwrites file
            fileWriter.write(data);
            fileWriter.close();
        } catch (IOException e) {
            ZimbraLog.misc.error("Failure while writing Server JSON Backup File : ", e);
            System.out.println("Failure while reading Server JSON Backup File : " + e.getMessage());
        }
    }

    private String readServerJSONFile(String filePath) {
        String result = null;
        try {
            File serverJSONBackupFile = new File(filePath);
            // check if file exists
            if (serverJSONBackupFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(filePath));
                StringBuilder builder = new StringBuilder();
                while ((result = reader.readLine()) != null) {
                    builder.append(result + "\n");
                }
                reader.close();
            }
        } catch (IOException e) {
            ZimbraLog.misc.error("Failure while reading Server JSON Backup File : ", e);
            System.out.println("Failure while reading Server JSON Backup File : " + e.getMessage());
        }
        return result;
    }
}

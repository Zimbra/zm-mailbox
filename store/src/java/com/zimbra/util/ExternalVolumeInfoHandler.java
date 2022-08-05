/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2022 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.volume.Volume;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.soap.admin.type.VolumeExternalOpenIOInfo;
import com.zimbra.soap.admin.type.VolumeInfo;

/**
 * LDAP based properties handler
 */
public class ExternalVolumeInfoHandler {

    private static final String DIR_PATH = File.separator + "opt" + File.separator + "zimbra" + File.separator + "config" + File.separator + "sm";
    private static final String PREV_FILE_NAME = "zsesc_prev.json";
    public static final String PREV_FILE_PATH = DIR_PATH + File.separator + PREV_FILE_NAME;
    private static final String CURR_FILE_NAME = "zsesc_curr.json";
    private static final String CURR_FILE_PATH = DIR_PATH + File.separator + CURR_FILE_NAME;
    private static final String SERVER_STORES = "server/stores";

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
            JSONArray currentJsonArray = currentJsonObject.getJSONArray(SERVER_STORES);

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
            JSONArray currentJsonArray = currentJsonObject.getJSONArray(SERVER_STORES);

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
            JSONArray currentJsonArray = currentJsonObject.getJSONArray(SERVER_STORES);

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
            currentJsonObject.put(SERVER_STORES, currentJsonArray);

            // step 6: Set ldap attribute
            writeServerJSONFile(serverExternalStoreConfigJson, DIR_PATH, PREV_FILE_PATH);
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
            writeServerJSONFile(currentJsonObject.toString(), DIR_PATH, CURR_FILE_PATH);
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
        VolumeExternalOpenIOInfo volExtOpenIoInfo = volInfo.getVolumeExternalOpenIOInfo();

        try {
            // step 1: Create and update json object and array for new volume entry of S3
            JSONObject volExtInfoObj = new JSONObject();
            if (volInfo.getVolumeExternalInfo() != null && AdminConstants.A_VOLUME_S3.equalsIgnoreCase(volInfo.getVolumeExternalInfo().getStorageType())) {
                volExtInfoObj = volExtInfo.toJSON(volInfo);
            } else if (volInfo.getVolumeExternalOpenIOInfo() != null && AdminConstants.A_VOLUME_OPEN_IO.equalsIgnoreCase(volInfo.getVolumeExternalOpenIOInfo().getStorageType())) {
                volExtInfoObj = volExtOpenIoInfo.toJSON(volInfo);
            }

            // step 2: Fetch current JSON state
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject currentJsonObject = null;
            if (StringUtil.isNullOrEmpty(serverExternalStoreConfigJson)) {
                // If current JSON state is already empty, initialise current JSON state Object
                currentJsonObject = new JSONObject();
                currentJsonObject.put(SERVER_STORES, new JSONArray());
            } else {
                // Else convert current JSON state string to current JSON state Object
                currentJsonObject = new JSONObject(serverExternalStoreConfigJson);
            }

            // step 3: Fetch current JSON state array
            JSONArray currentJsonArray = currentJsonObject.getJSONArray(SERVER_STORES);

            // step 4: Append current JSON state array with new volume entry
            currentJsonArray.put(volExtInfoObj);

            // step 5: Overwrite Appended current JSON state array with current JSON state Object
            currentJsonObject.put(SERVER_STORES, currentJsonArray);

            // step 6: Set ldap attribute
            writeServerJSONFile(serverExternalStoreConfigJson, DIR_PATH, PREV_FILE_PATH);
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
            writeServerJSONFile(currentJsonObject.toString(), DIR_PATH, CURR_FILE_PATH);
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
            JSONArray currentJsonArray = currentJsonObject.getJSONArray(SERVER_STORES);

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
            currentJsonObject.put(SERVER_STORES, currentJsonArray);

            // step 6: Set ldap attribute
            writeServerJSONFile(serverExternalStoreConfigJson, DIR_PATH, PREV_FILE_PATH);
            provisioning.getLocalServer().setServerExternalStoreConfig(currentJsonObject.toString());
            writeServerJSONFile(serverExternalStoreConfigJson, DIR_PATH, CURR_FILE_PATH);
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

            if(!Strings.isNullOrEmpty(globalExternalStoreConfig)) {
                JSONObject globalS3Configs = new JSONObject(globalExternalStoreConfig);
                JSONArray globalS3ConfigList = globalS3Configs.getJSONArray("global/s3BucketConfigurations");

                // step 2: Find "globalBucketUUID" in current JSON array
                for (int i = 0; i < globalS3ConfigList.length(); i++) {
                    // step 3: Mark validation as true if "globalBucketUUID" found
                    if (globalS3BucketId.equalsIgnoreCase(globalS3ConfigList.getJSONObject(i).getString("globalBucketUUID"))) {
                        return true;
                    }
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
        JSONArray globalS3ConfigList = globalS3Configs.getJSONArray(SERVER_STORES);
        for (int i = 0; i < globalS3ConfigList.length(); i++) {
            if (volumeId == globalS3ConfigList.getJSONObject(i).getInt(AdminConstants.A_VOLUME_ID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Write server json file
     * @param data, dirPath, filePath
     * @returns
     * @throws
     */
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
        }
    }

    /**
     * Read server json file
     * @param filePath
     * @returns String
     * @throws
     */
    private String readServerJSONFile(String filePath) {
        String result = "";
        try {
            File serverJSONBackupFile = new File(filePath);
            // check if file exists
            if (serverJSONBackupFile.exists()) {
                StringBuilder strBuilder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(filePath));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    strBuilder.append(line).append("\n");
                }
                reader.close();
                result = strBuilder.toString();
            }
        } catch (IOException e) {
            ZimbraLog.misc.error("Failure while reading Server JSON Backup File : ", e);
        }
        return result;
    }

    /**
     * Builds volume external info from previously backed up json data (n-1 version)
     * @param volume
     * @returns VolumeExternalInfo
     * @throws
     */
    private static VolumeExternalInfo buildVolumeExternalInfoFromBackupData(Volume volume) {
        VolumeExternalInfo volExtInfo = null;
        try {
            ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
            JSONObject properties = extVolInfoHandler.readBackupServerProperties(volume.getId(), ExternalVolumeInfoHandler.PREV_FILE_PATH);
            volExtInfo = new VolumeExternalInfo();
            volExtInfo.setVolumePrefix(properties.getString(AdminConstants.A_VOLUME_VOLUME_PREFIX));
            volExtInfo.setGlobalBucketConfigurationId(properties.getString(AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID));
            volExtInfo.setStorageType(properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE));
            volExtInfo.setUseInFrequentAccess(Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS)));
            volExtInfo.setUseIntelligentTiering(Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING)));
            volExtInfo.setUseInFrequentAccessThreshold(Integer.parseInt(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD)));
        } catch (JSONException e) {
            ZimbraLog.misc.error("Failure-[buildVolumeExternalInfo()] : ", e);
        } catch (ServiceException e) {
            ZimbraLog.misc.error("Failure-[buildVolumeExternalInfo()] : ", e);
        }
        return volExtInfo;
    }

    /**
     * Builds volume info from previously backed up json data (n-1 version)
     * @param volume
     * @returns VolumeExternalInfo
     * @throws
     */
    public static VolumeInfo buildVolumeInfoFromBackupData(Volume volume) {
        VolumeInfo volInfo = volume.toJAXB();
        volInfo.setVolumeExternalInfo(ExternalVolumeInfoHandler.buildVolumeExternalInfoFromBackupData(volume));
        return volInfo;
    }
}

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
     * Get volume properties by volume ID
     * @param volumeId
     * @return Properties object as complete set of volume properties
     * @throws ServiceException
     * @throws JSONException
     */
    public Properties getProperties(short volumeId) throws ServiceException, JSONException {

        Server server = provisioning.getLocalServer();
        Properties serverLevelProps = readServerProperties(volumeId, server);
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
            // LOG.error("Error while processing ldap attribute ServerExternalStoreConfig", e);
            throw e;
        }
        return properties;
    }

    public void deleteServerProperties(int volumeId) throws ServiceException, JSONException {
        try {
            String serverExternalStoreConfigJson = provisioning.getLocalServer().getServerExternalStoreConfig();
            JSONObject jsonObject = new JSONObject(serverExternalStoreConfigJson);
            JSONArray volumePropsArray = jsonObject.getJSONArray("server/stores");
            JSONArray updatedVolumePropsArray = new JSONArray();

            for (int i = 0; i < volumePropsArray.length(); i++) {
                JSONObject volumeJsonObj = volumePropsArray.getJSONObject(i);
                if (volumeId != volumeJsonObj.getInt("volumeId")) {
                    updatedVolumePropsArray.put(volumeJsonObj);
                }
            }

            volumePropsArray = updatedVolumePropsArray;
            jsonObject.put("server/stores", volumePropsArray);

        } catch (JSONException e) {
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

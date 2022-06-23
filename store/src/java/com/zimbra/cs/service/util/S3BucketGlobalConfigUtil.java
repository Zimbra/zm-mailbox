/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.GlobalExternalStoreConfigConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.util.s3.S3Connection;
import com.zimbra.soap.admin.message.CreateS3BucketConfigRequest;
import com.zimbra.soap.admin.message.DeleteS3BucketConfigRequest;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.type.GlobalExternalStoreConfig;
import com.zimbra.soap.type.GlobalS3BucketConfiguration;

public class S3BucketGlobalConfigUtil {

    /**
     * Parse the json which comes from the GlobalExternalStoreConfig LDAP parameter
     * to object.
     *
     * @param gescLdapJson
     * @return GlobalExternalStoreConfig
     * @throws ServiceException
     */
    public static GlobalExternalStoreConfig parseJsonToGesc(String gescLdapJson) throws ServiceException {
        GlobalExternalStoreConfig gesc = new GlobalExternalStoreConfig();
        try {
            if (gescLdapJson != null && !gescLdapJson.isEmpty()) {
                JSONObject gescLdapJO = new JSONObject(gescLdapJson);
                JSONArray gs3bucketsJA = gescLdapJO
                        .getJSONArray(GlobalExternalStoreConfigConstants.E_GLOBAL_S3_BUCKET_CONFIGURATIONS);
                if (gs3bucketsJA != null) {
                    for (int i = 0; i < gs3bucketsJA.length(); i++) {
                        JSONObject gs3bucketConfJO = gs3bucketsJA.getJSONObject(i);
                        boolean bucketIsActive = ((String) gs3bucketConfJO
                                .get(GlobalExternalStoreConfigConstants.A_S3_BUCKET_STATUS))
                                        .equalsIgnoreCase(S3BucketEnum.ACTIVE.getValue());
                        if (bucketIsActive) {
                            GlobalS3BucketConfiguration gs3bucketConf = new GlobalS3BucketConfiguration();
                            for (Iterator it = gs3bucketConfJO.keys(); it.hasNext();) {
                                Object fieldObject = it.next();
                                if (fieldObject instanceof String) {
                                    String field = String.valueOf(fieldObject);
                                    String value = (String) gs3bucketConfJO.get(field);
                                    setValueByReflection(gs3bucketConf, field, value);
                                }
                            }
                            gesc.addGlobalS3BucketConfiguration(gs3bucketConf);
                        }
                    }
                }
            }

        } catch (JSONException e) {
            ZimbraLog.mailbox.warn("Failed to parse ldap global_external_store_config parameter", e);
            throw ServiceException.FAILURE("Failed to parse ldap global_external_store_config parameter", null);
        }
        return gesc;
    }

    /**
     * Parse the Attr list inside of request element to a
     * GlobalS3BucketConfiguration object
     *
     * @param request
     * @return GlobalS3BucketConfiguration
     * @throws ServiceException
     */
    public static GlobalS3BucketConfiguration parseCreateRequestToGlobalS3BucketConfig(
            CreateS3BucketConfigRequest request) throws ServiceException {
        GlobalS3BucketConfiguration gs3bc = new GlobalS3BucketConfiguration();
        for (Attr attr : request.getAttrs()) {
            setValueByReflection(gs3bc, attr.getKey(), attr.getValue());
        }
        return gs3bc;
    }

    /**
     * Parse the Attr list inside of request element to a
     * GlobalS3BucketConfiguration object
     *
     * @param request
     * @return GlobalS3BucketConfiguration
     * @throws ServiceException
     */
    public static GlobalS3BucketConfiguration parseDeleteRequestToGlobalS3BucketConfig(
            DeleteS3BucketConfigRequest request) throws ServiceException {
        GlobalS3BucketConfiguration gs3bc = new GlobalS3BucketConfiguration();
        for (Attr attr : request.getAttrs()) {
            setValueByReflection(gs3bc, attr.getKey(), attr.getValue());
        }
        return gs3bc;
    }

    /**
     * Given an object, set the value for a specific field using Java Reflection
     * API.
     *
     * @param object
     * @param fieldStr
     * @param valueStr
     * @return
     * @throws ServiceException
     */
    private static void setValueByReflection(Object object, String fieldStr, String valueStr) throws ServiceException {
        boolean methodToSetFound = false;
        try {
            if (object != null && fieldStr != null && !fieldStr.isEmpty()) {
                Class<? extends Object> clazz = object.getClass();
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getName().equals(fieldStr)) {
                        for (Method method : clazz.getMethods()) {
                            if ((method.getName().startsWith("set"))
                                    && (method.getName().length() == field.getName().length() + 3)
                                    && (method.getName().toLowerCase().endsWith(field.getName().toLowerCase()))) {

                                method.invoke(object, valueStr);
                                methodToSetFound = true;

                            }
                            if (methodToSetFound) {
                                break;
                            }
                        }
                    }
                    if (methodToSetFound) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Failed to execute set method by reflection", e);
            throw ServiceException.FAILURE("Failed to execute set method by reflection", null);
        }
    }

    /**
     * Validate the request parameters before to create a bucket
     *
     * @param request
     * @return
     * @throws ServiceException
     */
    public static void validateReqBeforeCreate(CreateS3BucketConfigRequest request) throws ServiceException {

        GlobalS3BucketConfiguration gs3bc = parseCreateRequestToGlobalS3BucketConfig(request);

        // General Validations

        if (StringUtil.isNullOrEmpty(gs3bc.getStoreProvider())) {
            throw ServiceException
                    .INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_STORE_PROVIDER + " is required", null);
        }
        if (!(gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.AWS_S3.getValue())
                || gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.OPENIO_S3.getValue())
                || gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.CEPH_S3.getValue())
                || gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.NETAPP_S3.getValue()))) {
            throw ServiceException
                    .INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_STORE_PROVIDER + " is invalid", null);
        }
        if (StringUtil.isNullOrEmpty(gs3bc.getUrl())) {
            throw ServiceException.INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_URL + " is required", null);
        }
        if (StringUtil.isNullOrEmpty(gs3bc.getBucketName())) {
            throw ServiceException.INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_BUCKET_NAME + " is required",
                    null);
        }
        if (StringUtil.isNullOrEmpty(gs3bc.getRegion())) {
            throw ServiceException.INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_REGION + " is required",
                    null);
        }
        if (StringUtil.isNullOrEmpty(gs3bc.getAccessKey())) {
            throw ServiceException.INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_ACCESS_KEY + " is required",
                    null);
        }
        if (StringUtil.isNullOrEmpty(gs3bc.getSecretKey())) {
            throw ServiceException.INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_SECRET_KEY + " is required",
                    null);
        }

        // Specific validations for Amazon

        if (gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.AWS_S3.getValue())) {

            // Checking required attributes

            if (StringUtil.isNullOrEmpty(gs3bc.getDestinationPath())) {
                throw ServiceException.INVALID_REQUEST(
                        GlobalExternalStoreConfigConstants.A_S3_DESTINATION_PATH + " is required", null);
            }

        }

        // Specific validations for OpenIO

        if (gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.OPENIO_S3.getValue())) {

            // Checking required attributes

            if (StringUtil.isNullOrEmpty(gs3bc.getAccount())) {
                throw ServiceException.INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_ACCOUNT + " is required",
                        null);
            }
            if (StringUtil.isNullOrEmpty(gs3bc.getNamespace())) {
                throw ServiceException
                        .INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_NAMESPACE + " is required", null);
            }
            if (StringUtil.isNullOrEmpty(gs3bc.getProxyPort())) {
                throw ServiceException
                        .INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_PROXY_PORT + " is required", null);
            }
            if (StringUtil.isNullOrEmpty(gs3bc.getAccountPort())) {
                throw ServiceException
                        .INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_ACCOUNT_PORT + " is required", null);
            }

        }

        // Specific validations for Ceph

        if (gs3bc.getStoreProvider().equalsIgnoreCase(S3BucketEnum.CEPH_S3.getValue())) {

            // Checking required attributes

            if (StringUtil.isNullOrEmpty(gs3bc.getDestinationPath())) {
                throw ServiceException.INVALID_REQUEST(
                        GlobalExternalStoreConfigConstants.A_S3_DESTINATION_PATH + " is required", null);
            }

        }

        // Checking connection to S3 Bucket

        boolean s3Connection = S3Connection.connect(gs3bc.getUrl(), gs3bc.getBucketName(), gs3bc.getRegion(),
                gs3bc.getAccessKey(), gs3bc.getSecretKey());
        if (!s3Connection) {
            throw ServiceException.INVALID_REQUEST("S3 bucket connection failed", null);
        }

    }

    /**
     * Create a new S3 Bucket
     *
     * @param req
     * @return String
     * @throws ServiceException
     */
    public static String createS3BucketConfig(CreateS3BucketConfigRequest req) throws ServiceException {

        JSONObject s3BucketConfigToAdd = new JSONObject();
        JSONObject newGescLdapJO = new JSONObject();
        String newGescLdapJson = null;

        try {

            // Getting the LDAP Property

            String gescLdapJson = Provisioning.getInstance().getConfig().getGlobalExternalStoreConfig();

            // creating the bucket that will be added

            s3BucketConfigToAdd.put(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID,
                    LdapUtil.generateUUID());
            s3BucketConfigToAdd.put(GlobalExternalStoreConfigConstants.A_S3_BUCKET_STATUS,
                    S3BucketEnum.ACTIVE.getValue());
            for (Attr attr : req.getAttrs()) {
                s3BucketConfigToAdd.put(attr.getKey(), attr.getValue());
            }

            // if it's the first time, create json from scratch

            if (gescLdapJson == null || gescLdapJson.isEmpty()) {
                JSONArray gs3bucketsConfJA = new JSONArray();
                gs3bucketsConfJA.put(s3BucketConfigToAdd);
                newGescLdapJO.put(GlobalExternalStoreConfigConstants.E_GLOBAL_S3_BUCKET_CONFIGURATIONS,
                        gs3bucketsConfJA);
            }

            // if there is already a list of objects, just add the new one

            else {
                JSONObject gescLdapJO = new JSONObject(gescLdapJson);
                JSONArray gs3bucketsConfJA = gescLdapJO
                        .getJSONArray(GlobalExternalStoreConfigConstants.E_GLOBAL_S3_BUCKET_CONFIGURATIONS);
                if (gs3bucketsConfJA != null) {
                    gs3bucketsConfJA.put(s3BucketConfigToAdd);
                    newGescLdapJO.put(GlobalExternalStoreConfigConstants.E_GLOBAL_S3_BUCKET_CONFIGURATIONS,
                            gs3bucketsConfJA);
                }
            }

            // Persisting changes in LDAP

            newGescLdapJson = newGescLdapJO.toString();
            Provisioning.getInstance().getConfig().setGlobalExternalStoreConfig(newGescLdapJson);

        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Failed trying to create a bucket config", e);
            throw ServiceException.FAILURE("Failed trying to create a bucket config", null);
        }

        // Returning the recent added bucket

        return s3BucketConfigToAdd.toString();

    }

    /**
     * Validate the request parameters before to delete a bucket
     *
     * @param request
     * @return
     * @throws ServiceException
     */
    public static void validateReqBeforeDelete(DeleteS3BucketConfigRequest request) throws ServiceException {

        GlobalS3BucketConfiguration gs3bc = parseDeleteRequestToGlobalS3BucketConfig(request);

        if (StringUtil.isNullOrEmpty(gs3bc.getGlobalBucketUUID())) {
            throw ServiceException
                    .INVALID_REQUEST(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID + " is required", null);
        }

    }

    /**
     * Delete an existing S3 Bucket. Delete in this case means set the bucketStatus
     * = DELETE
     *
     * @param uuid
     * @return String
     * @throws ServiceException
     */
    public static String deleteS3BucketConfig(String uuid) throws ServiceException {

        JSONObject s3BucketConfigToDelete = new JSONObject();
        JSONArray gs3bucketsConfJA = new JSONArray();
        JSONObject newGescLdapJO = new JSONObject();
        String newGescLdapJson = null;
        boolean s3BucketFound = false;

        try {

            // Getting the LDAP Property

            String gescLdapJson = Provisioning.getInstance().getConfig().getGlobalExternalStoreConfig();

            // Searching bucket config with the entry uuid, if found, it will be modified.

            JSONObject gescLdapJO = new JSONObject(gescLdapJson);
            gs3bucketsConfJA = gescLdapJO
                    .getJSONArray(GlobalExternalStoreConfigConstants.E_GLOBAL_S3_BUCKET_CONFIGURATIONS);
            if (gs3bucketsConfJA != null) {
                for (int i = 0; i < gs3bucketsConfJA.length(); i++) {
                    s3BucketConfigToDelete = gs3bucketsConfJA.getJSONObject(i);
                    String bucketUUID = (String) s3BucketConfigToDelete
                            .get(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID);
                    boolean bucketIsActive = ((String) s3BucketConfigToDelete
                            .get(GlobalExternalStoreConfigConstants.A_S3_BUCKET_STATUS)).equalsIgnoreCase("ACTIVE");
                    if (bucketUUID.equals(uuid) && bucketIsActive) {
                        s3BucketConfigToDelete.put(GlobalExternalStoreConfigConstants.A_S3_BUCKET_STATUS,
                                S3BucketEnum.DELETED.getValue());
                        gs3bucketsConfJA.put(i, s3BucketConfigToDelete);
                        s3BucketFound = true;
                        break;
                    }
                }
            }

            // Persisting changes in LDAP
            if (s3BucketFound) {
                newGescLdapJO.put(GlobalExternalStoreConfigConstants.E_GLOBAL_S3_BUCKET_CONFIGURATIONS,
                        gs3bucketsConfJA);
                newGescLdapJson = newGescLdapJO.toString();
                Provisioning.getInstance().getConfig().setGlobalExternalStoreConfig(newGescLdapJson);
            } else {
                throw ServiceException.INVALID_REQUEST(
                        GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID + " is invalid", null);
            }

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Failed trying to delete the bucket config", e);
            throw ServiceException.FAILURE("Failed trying to delete the bucket config", null);
        }

        // Returning the recent deleted bucket

        return s3BucketConfigToDelete.toString();

    }

    /**
     * Retrieves the fields and values from a flat json as a List of Attrs
     *
     * @param json
     * @return List<Attr>
     * @throws ServiceException
     */
    public static List<Attr> getAttrs(String json) {

        List<Attr> attrs = new ArrayList<>();

        try {

            JSONObject jo = new JSONObject(json);
            for (Iterator it = jo.keys(); it.hasNext();) {
                Object fieldObject = it.next();
                if (fieldObject instanceof String) {
                    String field = String.valueOf(fieldObject);
                    String value = (String) jo.get(field);
                    attrs.add(new Attr(field, value));
                }
            }

        } catch (JSONException e) {
            ZimbraLog.mailbox.warn("failed to manipulate JSONOBject", e);
        }

        return attrs;

    }

}

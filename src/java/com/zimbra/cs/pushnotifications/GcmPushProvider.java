/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pushnotifications;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class GcmPushProvider implements PushProvider {

    private String gcmUrl = null;
    private String gcmAuthorizationKey = null;

    private static final int DEFAULT_MAX_PAYLOAD_SIZE = 4096;

    public GcmPushProvider() {
        init();
    }

    private void init() {
        try {
            Config config = Provisioning.getInstance().getConfig();
            gcmUrl = config.getGCMUrl();
            gcmAuthorizationKey = config.getGCMAuthorizationKey();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("ZMG: Failed to get GCM Attributes", e);
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushProvider#push(com.zimbra.cs.
     * pushnotifications.PushNotification)
     */
    @Override
    public void push(PushNotification notification) {

        if (gcmUrl == null || gcmAuthorizationKey == null) {
            ZimbraLog.mailbox.warn("ZMG: Need GCM attributes to send notification");
            return;
        }

        String payload = notification.getPayload();
        int maxPayloadSize = notification.getDevice().getMaxPayloadSize() == 0 ? DEFAULT_MAX_PAYLOAD_SIZE
            : notification.getDevice().getMaxPayloadSize();
        int payloadSize;
        try {
            payloadSize = payload.getBytes(PushNotification.CHARCTER_ENCODING).length;
            ZimbraLog.mailbox.info("ZMG: GCM payload size -  %d", payloadSize);
            if (payload.isEmpty()) {
                return;
            }
            if (payloadSize > maxPayloadSize) {
                payload = truncatePayload(payload, payloadSize, maxPayloadSize);
                payloadSize = payload.getBytes(PushNotification.CHARCTER_ENCODING).length;
                ZimbraLog.mailbox.info("ZMG: GCM truncated payload size -  %d", payloadSize);
                if (payloadSize > maxPayloadSize) {
                    ZimbraLog.mailbox
                        .info("ZMG: GCM Paylaod size is greater than the maximum supported payload size");
                    return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.mailbox.warn("ZMG: unsupported encoding", e);
            return;
        }

        PostMethod post = new PostMethod(gcmUrl);
        post.addRequestHeader("Authorization", "key=" + gcmAuthorizationKey);
        post.setRequestHeader("Content-Type", "application/json;charset=" + PushNotification.CHARCTER_ENCODING);
        try {
            post.setRequestEntity(new StringRequestEntity(payload, "application/json",
                PushNotification.CHARCTER_ENCODING));
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.mailbox.warn("ZMG: Exception in forming GCM request", e);
            return;
        }

        try {
            HttpClient httpClient = ZimbraHttpConnectionManager.getExternalHttpConnMgr()
                .getDefaultHttpClient();
            int status = httpClient.executeMethod(post);
            if (status == HttpStatus.SC_OK) {
                ZimbraLog.mailbox.info("ZMG: GCM push completed: status=%d", status);
            } else {
                ZimbraLog.mailbox.info("ZMG: GCM push failed: status=%d, payload= %s", status,
                    notification.getPayload());
                ZimbraLog.mailbox.info("ZMG: GCM push failed: response = %s",
                    post.getResponseBodyAsString());
            }
        } catch (HttpException e) {
            ZimbraLog.mailbox.warn("ZMG: GCM push exception: " + gcmUrl, e);
        } catch (IOException e) {
            ZimbraLog.mailbox.warn("ZMG: GCM IO failed", e);
        } finally {
            post.releaseConnection();
        }
    }

    public String truncatePayload(String jsonString, int payloadSize, int maxPayloadSize) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject gcmData = jsonObject.getJSONObject(PushNotification.GCM_DATA);
            String subject = gcmData.getString(PushNotification.SUBJECT);
            int maxSubjectLength = subject.getBytes(PushNotification.CHARCTER_ENCODING).length
                - (payloadSize - maxPayloadSize);
            if (maxSubjectLength < 0) {
                return jsonString;
            }
            subject = StringUtil.truncateIfRequired(subject, maxSubjectLength);
            gcmData.put(PushNotification.SUBJECT, subject);
            return jsonObject.toString();
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn("ZMG: JSON Exception in truncating payload", e);
            return jsonString;
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.mailbox.warn("ZMG: Encoding Exception in truncating payload", e);
            return jsonString;
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("ZMG: Exception in truncating payload", e);
            return jsonString;
        }
    }
}

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

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZmgDevice;

public class ApnsPushProvider implements PushProvider {

    private static final int LEGACY_DEFAULT_MAX_PAYLOAD_SIZE = 256;
    private static final int DEFAULT_MAX_PAYLOAD_SIZE = 2048;
    private String certificatePassword;
    private boolean production = true;
    private byte[] certificate = null;

    public ApnsPushProvider() {
        init();
    }

    private void init() {
        try {
            Config config = Provisioning.getInstance().getConfig();
            certificatePassword = config.getAPNSCertificatePassword();
            production = config.isAPNSProduction();
            certificate = config.getAPNSCertificate();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("ZMG: Failed to get APNS Attributes", e);
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
    public void push(PushNotification zmgNotification) {
        try {
            if (certificate == null || certificatePassword == null) {
                ZimbraLog.mailbox.warn("ZMG: Need APNS attributes to send notification");
                return;
            }
            String jsonString = zmgNotification.getPayload();
            ZmgDevice zmgDevice = zmgNotification.getDevice();

            int payloadSize = jsonString.getBytes(PushNotification.CHARCTER_ENCODING).length;
            int maxPayloadSize = zmgDevice.getMaxPayloadSize();
            if (maxPayloadSize == 0 && zmgDevice.getOSVersionAsDouble() < 8.0) {
                maxPayloadSize = LEGACY_DEFAULT_MAX_PAYLOAD_SIZE;
            } else if (maxPayloadSize == 0 && zmgDevice.getOSVersionAsDouble() >= 8.0) {
                maxPayloadSize = DEFAULT_MAX_PAYLOAD_SIZE;
            }

            ZimbraLog.mailbox.debug("ZMG: APNS payload size -  %d", payloadSize);
            if (jsonString.isEmpty()) {
                return;
            }
            if (payloadSize > maxPayloadSize) {
                int originalPayloadSize = payloadSize;
                jsonString = truncatePayload(jsonString, payloadSize, maxPayloadSize);
                payloadSize = jsonString.getBytes(PushNotification.CHARCTER_ENCODING).length;
                ZimbraLog.mailbox.debug("ZMG: APNS truncated payload size -  %d", payloadSize);
                if (payloadSize > maxPayloadSize) {
                    ZimbraLog.mailbox
                        .info(
                            "ZMG: APNS Paylaod size is greater than the maximum supported payload size "
                                + "originalPayloadSize=%d ; truncatedPayloadSize=%d ; maxPayloadSize=%d",
                            originalPayloadSize, payloadSize, maxPayloadSize);
                    ZimbraLog.mailbox.info("ZMG: APNS Payload=%s", jsonString);
                    return;
                }
            }

            CustomApnsPayload payload = new CustomApnsPayload(jsonString, maxPayloadSize);

            List<PushedNotification> notifications = Push.payload(payload, certificate,
                certificatePassword, production, zmgDevice.getRegistrationId());
            ResponsePacket response = null;
            for (PushedNotification notification : notifications) {
                response = notification.getResponse();
                if (response != null) {
                    ZimbraLog.mailbox.info("ZMG: APNS push response = %s", response.getMessage());
                }
                if (notification.isSuccessful()) {
                    ZimbraLog.mailbox.debug("ZMG: APNS payload - %s", jsonString);
                    ZimbraLog.mailbox.info(
                        "ZMG: APNS push notification sent successfully - device token=%s",
                        notification.getDevice().getToken());
                } else {
                    String invalidToken = notification.getDevice().getToken();
                    ZimbraLog.mailbox.info("ZMG: APNS Payload=%s push invalid token to: %s",
                        jsonString, invalidToken);
                }
            }
        } catch (CommunicationException e) {
            ZimbraLog.mailbox.warn("ZMG: APNS push communication failed", e);
            return;
        } catch (KeystoreException e) {
            ZimbraLog.mailbox.warn("ZMG: APNS push SSL failed", e);
            return;
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("ZMG: APNS push failed", e);
            return;
        }
    }

    public String truncatePayload(String jsonString, int payloadSize, int maxPayloadSize) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject aps = jsonObject.getJSONObject(PushNotification.APNS_APS);
            String[] apsValues = aps.getString(PushNotification.APNS_ALERT).split("\n");
            String subject = apsValues[1];
            int maxSubjectLength = subject.getBytes(PushNotification.CHARCTER_ENCODING).length
                - (payloadSize - maxPayloadSize);
            if (maxSubjectLength < 0) {
                return jsonString;
            }
            subject = StringUtil.truncateIfRequired(subject, maxSubjectLength);
            aps.put(PushNotification.APNS_ALERT, apsValues[0] + "\n" + subject);
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

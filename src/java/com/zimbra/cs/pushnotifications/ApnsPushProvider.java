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

import java.util.List;
import java.util.Map;

import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class ApnsPushProvider implements PushProvider {

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

            PushNotificationPayload payload = PushNotificationPayload.complex();

            Map<String, String> params = zmgNotification.getPayload();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().equals(PushNotification.APNS_ALERT)) {
                    payload.addAlert(entry.getValue());
                } else if (entry.getKey().equals(PushNotification.APNS_SOUND)) {
                    payload.addSound(entry.getValue());
                } else if (entry.getKey().equals(PushNotification.APNS_BADGE)) {
                    payload.addBadge(Integer.parseInt(entry.getValue()));
                } else {
                    payload.addCustomDictionary(entry.getKey(), entry.getValue());
                }
            }

            List<PushedNotification> notifications = Push.payload(payload, certificate,
                certificatePassword, production, zmgNotification.getDevice().getRegistrationId());
            ResponsePacket response = null;
            for (PushedNotification notification : notifications) {
                response = notification.getResponse();
                if (response != null) {
                    ZimbraLog.mailbox.debug("ZMG: APNS push response = %s", response.getMessage());
                }
                if (notification.isSuccessful()) {
                    ZimbraLog.mailbox.debug("ZMG: APNS push notification sent successfully");
                } else {
                    String invalidToken = notification.getDevice().getToken();
                    ZimbraLog.mailbox.debug("ZMG: APNS push invalid token to: %s", invalidToken);
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

}

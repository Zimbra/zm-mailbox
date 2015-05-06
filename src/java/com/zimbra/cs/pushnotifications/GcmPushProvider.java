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
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class GcmPushProvider implements PushProvider {

    private String gcmUrl = null;
    private String gcmAuthorizationKey = null;

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

        PostMethod post = new PostMethod(gcmUrl);
        post.addRequestHeader("Authorization", "key=" + gcmAuthorizationKey);

        post.addParameter("registration_id", notification.getDevice().getRegistrationId());

        Map<String, String> params = notification.getPayload();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            post.addParameter(entry.getKey(), entry.getValue());
        }

        post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        try {
            int status = HttpClientUtil.executeMethod(post);
            if (status == HttpStatus.SC_OK) {
                String resp = post.getResponseBodyAsString();
                ZimbraLog.mailbox.debug("ZMG: GCM push completed: device=%s status=%d response=%s",
                    notification.getDevice().getRegistrationId(), status, resp);
            } else {
                ZimbraLog.mailbox.debug("ZMG: GCM push failed: status=%d", status);
            }
        } catch (HttpException e) {
            ZimbraLog.mailbox.warn("ZMG: GCM push exception: " + gcmUrl, e);
        } catch (IOException e) {
            ZimbraLog.mailbox.warn("ZMG: GCM IO failed", e);
        } finally {
            post.releaseConnection();
        }
    }
}

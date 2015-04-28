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

import com.zimbra.cs.account.ZmgDevice;

public class ProviderFactory {

    public static final String PROVIDER_IDENTIFIER_GCM = "gcm";
    public static final String PROVIDER_IDENTIFIER_APNS = "apns";

    public static PushProvider createProvider(ZmgDevice device) {
        String pushProvider = device.getPushProvider();
        if (PROVIDER_IDENTIFIER_GCM.equals(pushProvider)) {
            return new GcmPushProvider();
        } else if (PROVIDER_IDENTIFIER_APNS.equals(pushProvider)) {
            return new ApnsPushProvider();
        }
        return null;
    }
}

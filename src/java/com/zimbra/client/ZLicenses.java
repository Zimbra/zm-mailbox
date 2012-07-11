/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.client;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.soap.account.type.LicenseAttr;
import com.zimbra.soap.account.type.LicenseInfo;

import java.util.HashMap;


public class ZLicenses {

    private HashMap<String, String> licenseMap = new HashMap<String, String>();
    private final String LICENSE_VOICE = "VOICE";
    private final String LICENSE_SMIME = "SMIME";

    public ZLicenses(LicenseInfo licenses) {

        if (licenses != null) {
            for (LicenseAttr attr : licenses.getAttrs()) {
                licenseMap.put(attr.getName(), attr.getContent());
            }
        }
    }

    private String get(String name) {
        return licenseMap.get(name);
    }

    public boolean getBool(String name) {
        return ProvisioningConstants.TRUE.equals(get(name));
    }

    public boolean getVoice() {
        return getBool(LICENSE_VOICE);
    }

    public boolean getSmime() {
        return getBool(LICENSE_SMIME);
    }
}

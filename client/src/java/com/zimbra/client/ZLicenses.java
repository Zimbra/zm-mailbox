/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.soap.account.type.LicenseAttr;
import com.zimbra.soap.account.type.LicenseInfo;

import java.util.HashMap;


public class ZLicenses {

    private HashMap<String, String> licenseMap = new HashMap<String, String>();
    private final String LICENSE_VOICE = "VOICE";
    private final String LICENSE_SMIME = "SMIME";
    private final String LICENSE_TOUCHCLIENT = "TOUCHCLIENT";

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

    public boolean getTouchClient() {
        return getBool(LICENSE_TOUCHCLIENT);
    }
}

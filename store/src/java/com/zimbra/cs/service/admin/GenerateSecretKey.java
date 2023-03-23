/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.util.SecretKey;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.CacheEntryType;

import java.util.Map;

public class GenerateSecretKey extends AdminDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String randomString = SecretKey.generateRandomString();
        Provisioning.getInstance().getConfig().setFeatureMailRecallSecretKey(randomString);
        flushCache();

        Element response = zsc.createElement(AdminConstants.GENERATE_SECRET_KEY_RESPONSE);
        return response;
    }

    private void flushCache() {
        try {
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI(SoapProvisioning.getLocalConfigURI());
            sp.soapZimbraAdminAuthenticate();
            sp.flushCache(CacheEntryType.config.toString(), null, true);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Encountered exception during FlushCache after creating Secret Key", e);
        }
    }

}


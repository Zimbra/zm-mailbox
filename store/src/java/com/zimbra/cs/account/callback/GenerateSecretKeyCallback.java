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

package com.zimbra.cs.account.callback;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.soap.admin.message.GenerateSecretKeyRequest;

import java.util.Map;

public class GenerateSecretKeyCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
                          Map attrsToModify, Entry entry) throws ServiceException {
        String secretKey = null;
        try {
            if(value.equals("TRUE")) {
                secretKey = Provisioning.getInstance().getConfig().getFeatureMailRecallSecretKey();
                if (Strings.isNullOrEmpty(secretKey)) {
                    SoapProvisioning sp = SoapProvisioning.getAdminInstance();
                    sp.invokeJaxb(new GenerateSecretKeyRequest());
                }
            }
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Unable to initialize SecureRandom for mail recall", e);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}

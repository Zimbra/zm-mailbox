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

package com.zimbra.cs.service.account.zmg;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ZmgDevice;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.AddZmgDeviceRequest;
import com.zimbra.soap.account.type.ZmgDeviceSpec;

public class AddZmgDevice extends AccountDocumentHandler {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element,
     * java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }

        AddZmgDeviceRequest req = JaxbUtil.elementToJaxb(request);
        ZmgDeviceSpec device = req.getZmgDevice();
        String deviceId = device.getDeviceId();
        String regId = device.getRegistrationId();
        String pushProvider = device.getPushProvider();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        int result = ZmgDevice.add(mbox.getId(), deviceId, regId, pushProvider);

        if (result == 1 && !account.isPrefZmgPushNotificationEnabled()) {
            account.setPrefZmgPushNotificationEnabled(true);
        }

        Element response = zsc.createElement(AccountConstants.ADD_ZMG_DEVICE_RESPONSE);
        response.addUniqueElement(AccountConstants.E_SUCCESS).addText(Integer.toString(result));
        return response;
    }
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2025 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import java.util.Map;


import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraLicenseNotificationEmailForLicensePreExpiry;

public class LicensePreExpiryNotificationMail extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        MultiValueMod mod = multiValueMod(attrsToModify, A_zimbraLicenseNotificationEmailForLicensePreExpiry);
        if (mod != null && (mod.adding() || mod.replacing())) {
            String[] emails = Provisioning.getInstance().getConfig().getLicenseNotificationEmailForLicensePreExpiry();
            if (emails.length >= 5) {
                throw AccountServiceException.INVALID_ATTR_VALUE("The maximum number of emails allowed is 5.", null);
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {

    }
}

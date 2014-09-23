/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;


/**
 * @author sankumar
 * When  calendar feature is disabled, disable all sub features of calendar
 */
public class FeatureCalendarEnabled  extends AttributeCallback{
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
        @SuppressWarnings("rawtypes") Map attrsToModify, Entry entry) throws ServiceException {
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        ZimbraLog.misc.debug("attrName: %s", attrName);
        if (Provisioning.A_zimbraFeatureCalendarEnabled.equals(attrName)) {
            if (context.isDoneAndSetIfNot(AccountStatus.class)) {
                return;
            }
            Boolean isCalendarEnabled = Boolean.valueOf(entry.getAttr(Provisioning.A_zimbraFeatureCalendarEnabled));
            if (!isCalendarEnabled) {
                Map<String, String> attrs = new HashMap<>();
                attrs.put(ZAttrProvisioning.A_zimbraFeatureGroupCalendarEnabled, ProvisioningConstants.FALSE);
                attrs.put(ZAttrProvisioning.A_zimbraFeatureCalendarReminderDeviceEmailEnabled, ProvisioningConstants.FALSE);
                try {
                    Provisioning.getInstance().modifyAttrs(entry, attrs);
                    ZimbraLog.misc.debug("zimbraFeatureGroupCalendarEnabled and zimbraFeatureCalendarReminderDeviceEmailEnabled set to false as zimbraFeatureCalendarEnabled" +
                    "is also set to false.");
                } catch (ServiceException e) {
                    ZimbraLog.misc.error("Unable to set zimbraFeatureGroupCalendarEnabled or zimbraFeatureCalendarReminderDeviceEmailEnabled", e);
                } 
            }
        }
    }
}

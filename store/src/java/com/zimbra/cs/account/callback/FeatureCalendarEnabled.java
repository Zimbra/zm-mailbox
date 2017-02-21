/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

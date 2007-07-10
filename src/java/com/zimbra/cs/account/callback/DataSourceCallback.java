/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

/**
 * Validates <tt>DataSource</tt> attribute values.
 * 
 * @author bburtin
 */
public class DataSourceCallback implements AttributeCallback {

    /**
      * Confirms that the polling interval set on the data source is at least as long
      * as the minimum set for the account.
      */
    public void preModify(Map context, String attrName, Object attrValue, Map attrsToModify,
                          Entry entry, boolean isCreate) throws ServiceException {
        if (!Provisioning.A_zimbraDataSourcePollingInterval.equals(attrName)) {
            return;
        }
        DataSource ds = (DataSource) entry;
        Account account = ds.getAccount();
        if (account == null) {
            throw ServiceException.INVALID_REQUEST("Could not find account for data source " + ds.getName(), null);
        }
        long interval = DateUtil.getTimeInterval((String) attrValue, 0);
        long minInterval = account.getTimeInterval(Provisioning.A_zimbraDataSourceMinPollingInterval, 0); 
        if (interval < minInterval) {
            String msg = String.format(
                "Polling interval value %s is shorter than the allowed minimum of %s.",
                attrValue, account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
    }

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}

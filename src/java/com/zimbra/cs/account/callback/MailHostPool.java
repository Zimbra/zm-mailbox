/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.ServerBy;

public class MailHostPool implements AttributeCallback {

    /**
     * check to make sure zimbraMailHostPool points to a valid server id
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        String[] pool;
        
        if (value instanceof String)
            pool = new String[] { (String) value };
        else if (value instanceof String[])
            pool = (String[]) value;
        else 
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraMailHostPool+" not a String or String[]", null);

        Provisioning prov = Provisioning.getInstance();
        for (int i=0; i < pool.length; i++) {
            if (pool[i] == null || pool[i].equals("")) continue;
            if (prov.get(ServerBy.id, pool[i]) == null)
                    throw ServiceException.INVALID_REQUEST("specified "+Provisioning.A_zimbraMailHostPool+" does not correspond to a valid server: "+pool[i], null);
        }
    }

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}

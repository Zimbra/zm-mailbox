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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.util.Map;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

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
            if (prov.getServerById(pool[i]) == null)
                    throw ServiceException.INVALID_REQUEST("specified "+Provisioning.A_zimbraMailHostPool+" does not correspond to a valid server: "+pool[i], null);
        }
    }

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}

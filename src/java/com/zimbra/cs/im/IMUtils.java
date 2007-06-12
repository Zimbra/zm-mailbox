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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;

/**
 * 
 */
public class IMUtils {

    /**
     * XMPP doesn't support aliases, so we need to resolve the requested
     * address to a canonical address.     
     *  
     * @param addr Address or alias (may or may not be local)
     * @return The resolved address, or 'addr' if it could not be resolved.
     */
    public static String resolveAddress(String addr) {
        try {
            Account acct = Provisioning.getInstance().get(AccountBy.name, addr);
            if (acct != null) {
                return acct.getName();
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.info("Caught ServiceException while attemptin to resolve alias to local Account", ex);
        }
        // couldn't resolve it, so just return the original addr...
        return addr;
    }
    
}

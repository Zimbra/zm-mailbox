/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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

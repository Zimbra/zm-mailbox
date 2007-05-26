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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public abstract class ZimbraCustomAuth {
    
    private static Map<String, ZimbraCustomAuth> mHandlers;

    /*
     * Register custom authentication handler
     */
    public synchronized static void register(String handlerName, ZimbraCustomAuth handler) {
        
        if (mHandlers == null)
            mHandlers = new HashMap<String, ZimbraCustomAuth>();
        else {
            //  sanity check
            ZimbraCustomAuth obj = mHandlers.get(handlerName);
            if (obj != null) {
                ZimbraLog.account.warn("handler name " + handlerName + " is already registered, " +
                                       "registering of " + obj.getClass().getCanonicalName() + " is ignored");
                return;
            }    
        }
        mHandlers.put(handlerName, handler);
    }
    
    public synchronized static ZimbraCustomAuth getHandler(String handlerName) {
        if (mHandlers == null)
            return null;
        else    
            return mHandlers.get(handlerName);
    }
    
    /*
     * Method invoked for custom auth.
     * 
     * Returning from the authenticate method indicates a successful authtication.
     * An Exception should be thrown if the authentication failed.
     * 
     * For any registered handler name, the authenticate method is going to be invoked 
     * on the same resistered handler object.  It is the implementor's resposibility 
     * to ensure thread safety of the method.
     */
    public abstract void authenticate(Account acct, String password) throws Exception;
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.auth;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public abstract class ZimbraCustomAuth {
    
    private static Map<String, ZimbraCustomAuth> mHandlers;
    
    static {
        /*
         * register known custom auth 
         */
        ZimbraCustomAuth.register("hosted", new HostedAuth());
    }

    /*
     * Register a custom auth handler.
     * It should be invoked from the init() method of ZimbraExtension.
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
     * Method invoked by the framework to handle authentication requests.
     * A custom auth implementation must implement this abstract method.
     * 
     * @param account: The account object of the principal to be authenticated
     *                 all attributes of the account can be retrieved from this object.
     *                   
     * @param password: Clear-text password.
     * 
     * @param context: Map containing context information.  
     *                 A list of context data is defined in com.zimbra.cs.account.AuthContext
     * 
     * @param args: Arguments specified in the zimbraAuthMech attribute
     * 
     * @return Returning from this function indicating the authentication has succeeded. 
     *  
     * @throws Exception.  If authentication failed, an Exception should be thrown.
     */
    public abstract void authenticate(Account acct, String password, Map<String, Object> context, List<String> args) throws Exception;
    
    /*
     * This function is called by the framework after a successful authenticate.  If 
     * authenticate failed this method won't be invoked.
     *  
     * Overwrite this method to indicate to the framework if checking for password aging 
     * is desired in the custom auth.   Default is false.
     * 
     * It only makes sense to return true for a custom auth if the password is stored in 
     * the Zimbra directory.  
     * 
     * If checkPasswordAging returns false, password aging check will be completely skipped in the framework.
     * If checkPasswordAging returns true, password aging will be executed by the framework if enabled.
     */
    public boolean checkPasswordAging() {
        return false;
    }
}

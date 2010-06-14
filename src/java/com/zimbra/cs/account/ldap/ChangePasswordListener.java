/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public abstract class ChangePasswordListener {

    private static Map<String, ChangePasswordListener> mListeners = new HashMap<String, ChangePasswordListener>();

    /**
     * Register a change password listener.
     * It should be invoked from the init() method of ZimbraExtension.
     */
    public synchronized static void register(String listenerName, ChangePasswordListener listener) {
        
        //  sanity check
        ChangePasswordListener obj = mListeners.get(listenerName);
        if (obj != null) {
            ZimbraLog.account.warn("listener name " + listenerName + " is already registered, " +
                                   "registering of " + obj.getClass().getCanonicalName() + " is ignored");
            return;
        }    

        mListeners.put(listenerName, listener);
    }
    
    public synchronized static ChangePasswordListener getHandler(Account acct) throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomain(acct);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(acct.getDomainName());
        
        String listenerName = domain.getAttr(Provisioning.A_zimbraPasswordChangeListener);
        
        if (listenerName == null)
            return null;
        
        ChangePasswordListener listener = mListeners.get(listenerName);
        if (listener == null)
            throw ServiceException.FAILURE("change password listener " + listenerName + " for account " + acct.getName() + " not found", null);
        
        return listener;
    }
    
    
    /**
     * Called before password(userPassword) and applicable(e.g. zimbraPasswordHistory, zimbraPasswordModifiedTime) 
     * attributes are modified in LDAP.  If a ServiceException is thrown, no attributes will be modified. 
     * 
     * The attrsToModify map should not be modified, other then for adding attributes defined in 
     * a LDAP schema extension. 
     * 
     * @param account account object being modified
     * @param newPassword Clear-text new password
     * @param context place to stash data between invocations of pre/postModify
     * @param attrsToModify a map of all the attributes being modified
     * @return Returning from this function indicating preModify has succeeded. 
     * @throws Exception.  If an Exception is thrown, no attributes will be modified.
     */
    public abstract void preModify(Account acct, String newPassword, Map context, Map<String, Object> attrsToModify) throws ServiceException;
    
    /**
     * called after a successful modify of the attributes. should not throw any exceptions.
     * 
     * @param account account object being modified
     * @param newPassword Clear-text new password
     * @param context place to stash data between invocations of pre/postModify
     */
    public abstract void postModify(Account acct, String newPassword, Map context);
            
    static class DummyChangePasswordListener extends ChangePasswordListener {
            
        public void preModify(Account acct, String newPassword, Map context, Map<String, Object> attrsToModify) throws ServiceException {
            attrsToModify.put("zimbraNotes", "password changed to " + newPassword);
            context.put("foo", "bar");
        }
            
        public void postModify(Account acct, String newPassword, Map context) {
            String foo = (String)context.get("foo");
            System.out.println("foo is " + foo);
            System.out.println("new password is " + newPassword);
        }
    }
        
    public static void main(String[] args) throws Exception {
        
        ChangePasswordListener.register("dummy", new DummyChangePasswordListener());
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Provisioning.AccountBy.name, "user1");
        
        // setup domain for testing
        Domain domain = prov.getDomain(acct);
        Map attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPasswordChangeListener, "dummy");
        prov.modifyAttrs(domain, attrs);
        
        prov.changePassword(acct, "test123", "test123-new");
        
        // done testing, remove listener from the domain
        attrs.clear();
        attrs.put(Provisioning.A_zimbraPasswordChangeListener, "");
        prov.modifyAttrs(domain, attrs);
        
        // change password back
        prov.changePassword(acct, "test123-new", "test123");
    }

}

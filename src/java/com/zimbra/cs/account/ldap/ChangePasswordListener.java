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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public abstract class ChangePasswordListener {

    // internal listeners will be invoked by the enum order
    public enum InternalChangePasswordListenerId {
        CPL_SYNC;
    }
    
    /*
     * The collection's iterator will return the values in the order their corresponding keys appear in map, 
     * which is their natural order (the order in which the enum constants are declared). 
     */
    private static Map<InternalChangePasswordListenerId, ChangePasswordListener> mInternalListeners = 
        Collections.synchronizedMap(new EnumMap<InternalChangePasswordListenerId, ChangePasswordListener>(InternalChangePasswordListenerId.class));

    private static Map<String, ChangePasswordListener> mExternalListeners = 
        Collections.synchronizedMap(new HashMap<String, ChangePasswordListener>());
    
    /**
     * Register a change password listener.
     * It should be invoked from the init() method of ZimbraExtension.
     */
    public static void register(String listenerName, ChangePasswordListener listener) {
        
        //  sanity check
        ChangePasswordListener obj = mExternalListeners.get(listenerName);
        if (obj != null) {
            ZimbraLog.account.warn("listener name " + listenerName + " is already registered, " +
                                   "registering of " + obj.getClass().getCanonicalName() + " is ignored");
            return;
        }    

        mExternalListeners.put(listenerName, listener);
    }
    
    public static void registerInternal(InternalChangePasswordListenerId listenerEnum, ChangePasswordListener listener) {
        //  sanity check
        ChangePasswordListener obj = mInternalListeners.get(listenerEnum);
        if (obj != null) {
            ZimbraLog.account.warn("listener " + listenerEnum + " is already registered, " +
                                   "registering of " + obj.getClass().getCanonicalName() + " is ignored");
            return;
        }    

        mInternalListeners.put(listenerEnum, listener);
    }
    
    private static ChangePasswordListener getHandler(Account acct) throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomain(acct);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(acct.getDomainName());
        
        String listenerName = domain.getAttr(Provisioning.A_zimbraPasswordChangeListener);
        
        if (listenerName == null)
            return null;
        
        ChangePasswordListener listener = mExternalListeners.get(listenerName);
        if (listener == null)
            throw ServiceException.FAILURE("change password listener " + listenerName + " for account " + acct.getName() + " not found", null);
        
        return listener;
    }
    
    /*
     * class to stage context for each listener
     */
    static class ChangePasswordListenerContext {
        Map<InternalChangePasswordListenerId, Map<String, Object>> mInternalCtxts = 
            new EnumMap<InternalChangePasswordListenerId, Map<String, Object>>(InternalChangePasswordListenerId.class);
        
        ChangePasswordListener mExternalListener = null;
        Map<String, Object> mExternalCtxt = null;
          
    }
    
    public static void invokePreModify(Account acct, String newPassword, 
            ChangePasswordListenerContext ctxts, Map<String, Object> attrsToModify) throws ServiceException {
        
        // invoke internal listeners
        for (Map.Entry<InternalChangePasswordListenerId, ChangePasswordListener> listener : mInternalListeners.entrySet()) {
            InternalChangePasswordListenerId listenerEnum = listener.getKey();
            ChangePasswordListener listenerInstance = listener.getValue();
            Map<String, Object> context = new HashMap<String, Object>();
            ctxts.mInternalCtxts.put(listenerEnum, context);
            listenerInstance.preModify(acct, newPassword, context, attrsToModify);
        }
        
        // invoke external listener
        ChangePasswordListener cpListener = ChangePasswordListener.getHandler(acct);
        if (cpListener != null) {
            ctxts.mExternalListener = cpListener;
            
            Map<String, Object> context = new HashMap<String, Object>();
            ctxts.mExternalCtxt = context;
            cpListener.preModify(acct, newPassword, context, attrsToModify);
        }
    }
    
    public static void invokePostModify(Account acct, String newPassword, ChangePasswordListenerContext ctxts) {
        // invoke internal listeners
        for (Map.Entry<InternalChangePasswordListenerId, ChangePasswordListener> listener : mInternalListeners.entrySet()) {
            InternalChangePasswordListenerId listenerEnum = listener.getKey();
            ChangePasswordListener listenerInstance = listener.getValue();
            Map<String, Object> context = ctxts.mInternalCtxts.get(listenerEnum);
            listenerInstance.postModify(acct, newPassword, context);
        }
        
        if (ctxts.mExternalListener != null)
            ctxts.mExternalListener.postModify(acct, newPassword, ctxts.mExternalCtxt);
    }
    
    /**
     * Called before password(userPassword) and applicable(e.g. zimbraPasswordHistory, zimbraPasswordModifiedTime) 
     * attributes are modified in LDAP.  If a ServiceException is thrown, no attributes will be modified. 
     * 
     * The attrsToModify map should not be modified, other then for adding attributes defined in 
     * a LDAP schema extension. 
     * 
     * @param USER_ACCOUNT account object being modified
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
     * @param USER_ACCOUNT account object being modified
     * @param newPassword Clear-text new password
     * @param context place to stash data between invocations of pre/postModify
     */
    public abstract void postModify(Account acct, String newPassword, Map context);
            
    static class DummyChangePasswordListener extends ChangePasswordListener {
            
        public void preModify(Account acct, String newPassword, Map context, Map<String, Object> attrsToModify) throws ServiceException {
            System.out.println("preModify dummy");
        }
            
        public void postModify(Account acct, String newPassword, Map context) {
            System.out.println("postModify dummy");
        }
    }

    public static void main(String[] args) throws Exception {
        
        // an internal listener
        ChangePasswordListener.registerInternal(InternalChangePasswordListenerId.CPL_SYNC, new DummyChangePasswordListener());
        
        // an extermal listener
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

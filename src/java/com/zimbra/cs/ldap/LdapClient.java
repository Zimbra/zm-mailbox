/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.ldap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;
import com.zimbra.cs.ldap.LdapConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.ZSearchScope.ZSearchScopeFactory;
import com.zimbra.cs.ldap.jndi.JNDILdapClient;
import com.zimbra.cs.ldap.unboundid.UBIDLdapClient;
import com.zimbra.cs.util.Zimbra;

public abstract class LdapClient {
    
    private static LdapClient ldapClient;
    
    synchronized static LdapClient getInstance() {
        if (ldapClient == null) {
            String className = LC.zimbra_class_ldap_client.value();
            if (className != null && !className.equals("")) {
                try {
                    ldapClient = (LdapClient) Class.forName(className).newInstance();
                } catch (Exception e) {
                    ZimbraLog.system.error("could not instantiate LDAP client '" + className + 
                            "'; defaulting to JNDI LDAP SDK", e);
                }
            }
            if (ldapClient == null) {
                ldapClient = new JNDILdapClient();
            }
            
            try {
                ldapClient.init();
            } catch (LdapException e) {
                Zimbra.halt("failed to initialize LDAP client", e);
            }
        }
        return ldapClient;
    }
    
    // for unittest only
    public static void initialize() {
        LdapClient.getInstance();
    }
    
    @TODO  // called from unittest for now, call it from Zimbra
    public static void shutdown() {
        LdapClient.getInstance().terminate();
    }
    
    /* 
     * Bridging the legacy ZimbraLdapContext and the new ZLdapContext classes.
     */
    @TODO
    public static LegacyZimbraLdapContext toLegacyZimbraLdapContext(com.zimbra.cs.account.Provisioning prov, ILdapContext ldapContext) {
        if (!prov.getClass().equals(com.zimbra.cs.account.ldap.LdapProvisioning.class) &&
            !prov.getClass().equals(com.zimbra.cs.account.ldap.custom.CustomLdapProvisioning.class)) { // TODO: what to do with CustomLdapProvisioning?
            Zimbra.halt("Provisioning instance is not LdapProvisioning", 
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        if (!(getInstance() instanceof JNDILdapClient)) {
            Zimbra.halt("LdapClient instance is not JNDILdapClient", 
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        // just a safety check, this should really not happen at thin point
        if (ldapContext != null && !(ldapContext instanceof LegacyZimbraLdapContext)) {
            Zimbra.halt("ILdapContext instance is not ZimbraLdapContext", 
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        return (LegacyZimbraLdapContext)ldapContext;
    }
    
    @TODO
    public static ZLdapContext toZLdapContext(com.zimbra.cs.account.Provisioning prov, ILdapContext ldapContext) {
        if (!prov.getClass().equals(com.zimbra.cs.prov.ldap.LdapProvisioning.class)) {
            Zimbra.halt("Provisioning instance is not XXXLdapProvisioning",  // TODO, what would be the name?
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        if (!(getInstance() instanceof UBIDLdapClient)) {
            Zimbra.halt("LdapClient instance is not UBIDLdapClient", 
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        // just a safety check, this should really not happen at thin point
        if (ldapContext != null && !(ldapContext instanceof ZLdapContext)) {
            Zimbra.halt("ILdapContext instance is not ZLdapContext", 
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        return (ZLdapContext)ldapContext;
    }
    
    
    /*
     * ========================================================
     * static methods just to short-hand the getInstance() call
     * ========================================================
     */
    public static ZLdapContext getContext() throws ServiceException {
        return getInstance().getContextImpl();
    }
    
    public static ZLdapContext getContext(LdapServerType serverType) 
    throws ServiceException {
        return getInstance().getContextImpl(serverType);
    }
    
    public static ZLdapContext getContext(LdapServerType serverType, boolean useConnPool) 
    throws ServiceException {
        return getInstance().getContextImpl(serverType, useConnPool);
    }
    
    public static ZLdapContext getExternalContext(ExternalLdapConfig ldapConfig) 
    throws ServiceException {
        return getInstance().getExternalContextImpl(ldapConfig);
    }
    
    public static void closeContext(ZLdapContext lctxt) {
        if (lctxt != null) {
            lctxt.closeContext();
        }
    }
    
    public static ZMutableEntry createMutableEntry() {
        return getInstance().createMutableEntryImpl();
    }
    
    
    /*
     * ========================================================
     * abstract methods
     * ========================================================
     */
    protected void init() throws LdapException {
        ZSearchScope.init(getSearchScopeFactoryInstance());
        ZLdapFilterFactory.setInstance(getLdapFilterFactoryInstance());
    }
    
    protected abstract void terminate();
    
    protected abstract ZSearchScopeFactory getSearchScopeFactoryInstance(); 
    
    protected abstract ZLdapFilterFactory getLdapFilterFactoryInstance() throws LdapException;
    
    protected ZLdapContext getContextImpl() throws ServiceException {
        return getContext(LdapServerType.REPLICA);
    }
    
    protected abstract ZLdapContext getContextImpl(LdapServerType serverType) 
    throws ServiceException;
    
    protected abstract ZLdapContext getContextImpl(LdapServerType serverType, boolean useConnPool) 
    throws ServiceException;
    
    protected abstract ZLdapContext getExternalContextImpl(ExternalLdapConfig ldapConfig) 
    throws ServiceException;
    
    protected abstract ZMutableEntry createMutableEntryImpl();
    
    protected abstract ZSearchControls createSearchControlsImpl(
            ZSearchScope searchScope, int sizeLimit, String[] returnAttrs);


}

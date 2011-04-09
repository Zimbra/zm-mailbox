package com.zimbra.cs.ldap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.ldap.ZSearchControls.ZSearchControlsFactory;
import com.zimbra.cs.ldap.ZSearchScope.ZSearchScopeFactory;
import com.zimbra.cs.ldap.jndi.JNDILdapContext;
import com.zimbra.cs.ldap.jndi.JNDISearchControls;
import com.zimbra.cs.ldap.jndi.JNDISearchScope;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.unboundid.UBIDLdapContext;
import com.zimbra.cs.ldap.unboundid.UBIDSearchControls;
import com.zimbra.cs.ldap.unboundid.UBIDSearchScope;
import com.zimbra.cs.util.Zimbra;

public abstract class LdapClient {
    
    private static LdapClient ldapClient;
    
    private synchronized static LdapClient getInstance() {
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
    
    /* 
     * Bridging the legacy ZimbraLdapContext and the new ZLdapContext classes.
     */
    @TODO
    public static ZimbraLdapContext toZimbraLdapContext(com.zimbra.cs.account.Provisioning prov, ILdapContext ldapContext) {
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
        if (ldapContext != null && !(ldapContext instanceof ZimbraLdapContext)) {
            Zimbra.halt("ILdapContext instance is not ZimbraLdapContext", 
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        
        return (ZimbraLdapContext)ldapContext;
    }
    
    @TODO
    public static ZLdapContext toZLdapContext(com.zimbra.cs.account.Provisioning prov, ILdapContext ldapContext) {
        /*
        if (!prov.getClass().equals(com.zimbra.cs.prov.ldap.LdapProvisioning.class)) {
            Zimbra.halt("Provisioning instance is not XXXLdapProvisioning",  // TODO, what would be the name?
                    ServiceException.FAILURE("internal error, wrong ldap context instance", null));
        }
        */
        
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
        return getInstance().getCtxt();
    }
    
    public static ZLdapContext getContext(LdapServerType serverType) throws ServiceException {
        return getInstance().getCtxt(serverType);
    }
    
    public static ZLdapContext getContext(LdapServerType serverType, boolean useConnPool) throws ServiceException {
        return getInstance().getCtxt(serverType, useConnPool);
    }
    
    public static void closeContext(ZLdapContext lctxt) {
        getInstance().closeCtxt(lctxt);
    }
    
    
    ////////////////////////////////////////////////////////////////
    void init() throws LdapException {
        ZSearchScope.init(getSearchScopeFactory());
        ZSearchControls.init(getSearchControlsFactory());
    }
    
    protected abstract ZSearchScopeFactory getSearchScopeFactory(); 
    
    protected abstract ZSearchControlsFactory getSearchControlsFactory(); 
    
    protected ZLdapContext getCtxt() throws ServiceException {
        return getCtxt(LdapServerType.REPLICA);
    }
    
    protected abstract ZLdapContext getCtxt(LdapServerType serverType) throws ServiceException;
    
    protected abstract ZLdapContext getCtxt(LdapServerType serverType, boolean useConnPool) throws ServiceException;
    
    private void closeCtxt(ZLdapContext lctxt) {
        if (lctxt != null) {
            lctxt.closeContext();
        }
    }
    
    /**
     * =========
     * Unboundid
     * =========
     */
    public static class UBIDLdapClient extends LdapClient {
        @Override
        void init() throws LdapException {
            super.init();
            UBIDLdapContext.init();
        }
        
        @Override 
        protected ZSearchScopeFactory getSearchScopeFactory() {
            return new UBIDSearchScope.UBIDSearchScopeFactory();
        }
        
        @Override 
        protected ZSearchControlsFactory getSearchControlsFactory() {
            return new UBIDSearchControls.UBIDSearchControlsFactory();
        }
        
        @Override
        protected ZLdapContext getCtxt(LdapServerType serverType) throws ServiceException {
            return new UBIDLdapContext(serverType);
        }
        
        /**
         * useConnPool is always ignored
         */
        @Override
        protected ZLdapContext getCtxt(LdapServerType serverType, boolean useConnPool) throws ServiceException {
            return getCtxt(serverType);
        }

    }
    
    /**
     * ====
     * JNDI
     * ====
     */
    public static class JNDILdapClient extends LdapClient {
        @Override
        void init() throws LdapException {
            super.init();
        }
        
        @Override 
        protected ZSearchScopeFactory getSearchScopeFactory() {
            return new JNDISearchScope.JNDISearchScopeFactory();
        }
        
        @Override
        protected ZSearchControlsFactory getSearchControlsFactory() {
            return new JNDISearchControls.JNDISearchControlsFactory();
        }
        
        @Override
        protected ZLdapContext getCtxt(LdapServerType serverType) throws ServiceException {
            return new JNDILdapContext(serverType);
        }
        
        @Override
        protected ZLdapContext getCtxt(LdapServerType serverType, boolean useConnPool) throws ServiceException {
            return new JNDILdapContext(serverType, useConnPool);
        }

    }
    
    public static void main(String[] args) throws ServiceException {
        CliUtil.toolSetup(); // to get logs printed to console
        LC.zimbra_class_ldap_client.setDefault("com.zimbra.cs.ldap.LdapClient$UBIDLdapClient");
        
        /*
        com.zimbra.cs.account.Provisioning prov = com.zimbra.cs.account.Provisioning.getInstance();
        com.zimbra.cs.account.Provisioning prov = new com.zimbra.cs.account.ldap.UBIDLdapProvisioning();
        ZimbraLdapContext zlc = LdapClient.toZimbraLdapContext((LdapProvisioning)prov, null);
        */
        
        // ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER);
    }

}

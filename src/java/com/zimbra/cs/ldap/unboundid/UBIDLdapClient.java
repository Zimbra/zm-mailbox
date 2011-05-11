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
package com.zimbra.cs.ldap.unboundid;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.ZSearchScope.ZSearchScopeFactory;

public class UBIDLdapClient extends LdapClient {
    @Override
    protected void init() throws LdapException {
        super.init();
        UBIDLdapContext.init();
    }
    
    @Override
    protected void terminate() {
        ConnectionPool.closeAll();
    }
    
    @Override 
    protected ZSearchScopeFactory getSearchScopeFactoryInstance() {
        return new UBIDSearchScope.UBIDSearchScopeFactory();
    }
    
    @Override
    protected ZLdapFilterFactory getLdapFilterFactoryInstance() 
    throws LdapException {
        UBIDLdapFilterFactory.initialize();
        return new UBIDLdapFilterFactory();
    }
    
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType) 
    throws ServiceException {
        return new UBIDLdapContext(serverType);
    }
    
    /**
     * useConnPool is always ignored
     */
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType, boolean useConnPool) 
    throws ServiceException {
        return getContextImpl(serverType);
    }
    

    @Override
    protected ZLdapContext getExternalContextImpl(ExternalLdapConfig config)
    throws ServiceException {
        return new UBIDLdapContext(config);
    }
    

    @Override
    protected ZMutableEntry createMutableEntryImpl() {
        return new UBIDMutableEntry();
    }

    @Override
    protected ZSearchControls createSearchControlsImpl(
            ZSearchScope searchScope, int sizeLimit, String[] returnAttrs) {
        return new UBIDSearchControls(searchScope, sizeLimit, returnAttrs);
    }



}

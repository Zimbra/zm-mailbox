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
package com.zimbra.cs.ldap.jndi;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.ZSearchScope.ZSearchScopeFactory;

public class JNDILdapClient extends LdapClient {
    @Override
    protected void init() throws LdapException {
        super.init();
    }
    
    @Override
    protected void terminate() {
        // do nothing
    }
    
    @Override 
    protected ZSearchScopeFactory getSearchScopeFactoryInstance() {
        return new JNDISearchScope.JNDISearchScopeFactory();
    }

    @Override
    protected ZLdapFilterFactory getLdapFilterFactoryInstance() throws LdapException {
        return new JNDILdapFilterFactory();
    }
    
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType) throws ServiceException {
        return new JNDILdapContext(serverType);
    }
    
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType, boolean useConnPool) throws ServiceException {
        return new JNDILdapContext(serverType, useConnPool);
    }

    @Override
    protected ZMutableEntry createMutableEntryImpl() {
        return new JNDIMutableEntry();
    }

    @Override
    protected ZSearchControls createSearchControlsImpl(ZSearchScope searchScope, int sizeLimit, String[] returnAttrs) {
        return new JNDISearchControls(searchScope, sizeLimit, returnAttrs);
    }

}

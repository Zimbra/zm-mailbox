/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap.unboundid;

import java.util.Date;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.ZSearchScope.ZSearchScopeFactory;

public class UBIDLdapClient extends LdapClient {
    @Override
    protected void init(boolean alwaysUseMaster) throws LdapException {
        super.init(alwaysUseMaster);
        UBIDLdapContext.init(alwaysUseMaster);
    }
    
    @Override
    protected void terminate() {
        UBIDLdapContext.shutdown();
    }
    
    @Override
    protected void alwaysUseMaster() {
        UBIDLdapContext.alwaysUseMaster();
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
    protected void waitForLdapServerImpl() {
        while (true) {
            UBIDLdapContext zlc = null;
            try {
                zlc = new UBIDLdapContext(LdapServerType.REPLICA, LdapUsage.PING);
                break;
            } catch (ServiceException e) {
                // may called at server startup when logging is not up yet.
                System.err.println(new Date() + ": error communicating with LDAP (will retry)");
                e.printStackTrace();
                try {
                    Thread.sleep(LdapConstants.CHECK_LDAP_SLEEP_MILLIS);
                } catch (InterruptedException ie) {
                }
            } finally {
                if (zlc != null) {
                    zlc.closeContext(false);
                }
            }
        }
    }
    
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType, LdapUsage usage) 
    throws ServiceException {
        return new UBIDLdapContext(serverType, usage);
    }
    
    /**
     * useConnPool is always ignored
     */
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType, boolean useConnPool,
            LdapUsage usage) 
    throws ServiceException {
        return getContextImpl(serverType, usage);
    }

    @Override
    protected ZLdapContext getExternalContextImpl(ExternalLdapConfig config, LdapUsage usage)
    throws ServiceException {
        return new UBIDLdapContext(config, usage);
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

    @Override
    protected void externalLdapAuthenticateImpl(String[] urls,
            boolean wantStartTLS, String bindDN, String password, String note)
    throws ServiceException {
        UBIDLdapContext.externalLdapAuthenticate(urls, wantStartTLS,
                bindDN, password, note);
    }

    @Override
    protected void zimbraLdapAuthenticateImpl(String bindDN, String password) 
    throws ServiceException {
        UBIDLdapContext.zimbraLdapAuthenticate(bindDN, password);
    }

}

package com.zimbra.cs.ldap.unboundid;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.ZLdapContext;
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
    protected ZSearchScopeFactory getSearchScopeFactoryImpl() {
        return new UBIDSearchScope.UBIDSearchScopeFactory();
    }
    
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType) throws ServiceException {
        return new UBIDLdapContext(serverType);
    }
    
    /**
     * useConnPool is always ignored
     */
    @Override
    protected ZLdapContext getContextImpl(LdapServerType serverType, boolean useConnPool) throws ServiceException {
        return getContextImpl(serverType);
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

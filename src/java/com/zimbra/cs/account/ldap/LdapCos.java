/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapCos extends LdapNamedEntry implements Cos {

    private LdapProvisioning mProv;
    
    LdapCos(String dn, Attributes attrs, LdapProvisioning prov) throws ServiceException {
        super(dn, attrs);
    }

    public String getName() {
        return getAttr(Provisioning.A_cn);
    }

    public String getId() {
        return getAttr(Provisioning.A_liquidId);
    }
}

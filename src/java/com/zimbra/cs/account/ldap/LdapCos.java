/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.account.ldap;

import javax.naming.directory.Attributes;

import com.liquidsys.coco.account.Cos;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.ServiceException;

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

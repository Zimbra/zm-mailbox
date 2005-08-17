/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapServer extends LdapNamedEntry implements Server {

    private Provisioning mProv;
    
    LdapServer(String dn, Attributes attrs, Provisioning prov) {
        super(dn, attrs);
        mProv = prov;
    }

    public String getName() {
        return getAttr(Provisioning.A_cn);
    }

    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }
    
    public String getAttr(String name) {
        String v = super.getAttr(name);
        if (v != null)
            return v;
        try {
            Config c = mProv.getConfig();
            if (!c.isInheritedServerAttr(name))
                return null;
            else
                return c.getAttr(name);
        } catch (ServiceException e) {
            return null;
        }
    }

    public String[] getMultiAttr(String name) {
        String v[] = super.getMultiAttr(name);
        if (v.length > 0)
            return v;
        try {
            Config c = mProv.getConfig();
            if (!c.isInheritedServerAttr(name))
                return sEmptyMulti;
            else
                return c.getMultiAttr(name);
        } catch (ServiceException e) {
            return sEmptyMulti;
        }
    }

    public Map getAttrs() throws ServiceException {
        return getAttrs(true);
    }    

    public Map getAttrs(boolean applyConfig) throws ServiceException {
        HashMap attrs = new HashMap();
        try {
            // get all the server attrs
            LdapUtil.getAttrs(mAttrs, attrs, null);
            
            if (!applyConfig)
                return attrs;
            
            // then enumerate through all inheritable attrs and add them if needed
            Config c = mProv.getConfig();
            String[] inheritable = mProv.getConfig().getMultiAttr(Provisioning.A_zimbraServerInheritedAttr);
            for (int i=0; i < inheritable.length; i++) {
                Object value = attrs.get(inheritable[i]);
                if (value == null)
                    value = c.getMultiAttr(inheritable[i]);
                if (value != null)
                    attrs.put(inheritable[i], value);
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get attrs", e);
        }
        return attrs;
    }

}

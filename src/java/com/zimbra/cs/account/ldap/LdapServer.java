/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class LdapServer extends LdapNamedEntry implements Server {

    private Provisioning mProv;
    
    LdapServer(String dn, Attributes attrs, Provisioning prov) throws NamingException {
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
            if (!AttributeManager.getInstance().isServerInherited(name))
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
            if (!AttributeManager.getInstance().isServerInherited(name))
                return sEmptyMulti;
            else
                return c.getMultiAttr(name);
        } catch (ServiceException e) {
            return sEmptyMulti;
        }
    }

    public Map<String, Object> getAttrs() throws ServiceException {
        return getAttrs(true);
    }    

    public Map<String, Object> getAttrs(boolean applyConfig) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        // get all the server attrs
        attrs.putAll(super.getAttrs());
            
        if (!applyConfig)
            return attrs;
            
        // then enumerate through all inheritable attrs and add them if needed
        Config c = mProv.getConfig();
        Set<String> inheritable = AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.serverInherited);
        for (String attr : inheritable) {
            Object value = attrs.get(attr);
            if (value == null)
                value = c.getMultiAttr(attr);
            if (value != null)
                attrs.put(attr, value);
        }
        return attrs;
    }
}

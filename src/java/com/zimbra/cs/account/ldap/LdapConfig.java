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
package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.service.ServiceException;

public class LdapConfig extends LdapEntry implements Config {
    
    private Map<String, Object> mDomainDefaults = new HashMap<String, Object>();
    private Map<String, Object> mServerDefaults = new HashMap<String, Object>();    
    
    LdapConfig(String dn, Attributes attrs, Map<String,Object> defaults) throws NamingException {
        super(dn, attrs, defaults);
        resetData();
    }

    @Override
    public void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.domainInherited, mDomainDefaults);
            getDefaults(AttributeFlag.serverInherited, mServerDefaults);            
        } catch (ServiceException e) {
            // TODO log?
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDomainDefaults() throws ServiceException {
        return mDomainDefaults;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServerDefaults() throws ServiceException {
        return mServerDefaults;
    }
}

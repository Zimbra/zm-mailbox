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

import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * @author schemers
 */
 class LdapDataSource extends DataSource implements LdapEntry {
     
     static String getObjectClass(Type type) {
        switch (type) {
            case pop3: return "zimbraPop3DataSource";
            default: return null;
        }
     }
     
     static Type getObjectType(Attributes attrs) throws ServiceException {
         Attribute attr = attrs.get("objectclass");
         if (attr.contains("zimbraPop3DataSource")) 
             return Type.pop3;
         else
             throw ServiceException.FAILURE("unable to determine data source type from object class", null);
                
      }
     
     private String mDn;

    LdapDataSource(String dn, Attributes attrs) throws NamingException, ServiceException {
        super(getObjectType(attrs),
                LdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceName),
                LdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceId),                
                LdapUtil.getAttrs(attrs));
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }

}

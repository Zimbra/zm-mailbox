/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class ZimbraGalLdapAttrMap extends LdapUpgrade {

    ZimbraGalLdapAttrMap() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        Config config = mProv.getConfig();
        
        String[] values = 
        {
         "facsimileTelephoneNumber,fax=workFax",
         "homeTelephoneNumber,homePhone=homePhone",
         "mobileTelephoneNumber,mobile=mobilePhone",
         "pagerTelephoneNumber,pager=pager"
        };
         
        Map<String, Object> attr = new HashMap<String, Object>();
        attr.put("+" + Provisioning.A_zimbraGalLdapAttrMap, values);

        System.out.println("Adding workFax, homePhone, mobilePhone, pager attr maps to global config " + Provisioning.A_zimbraGalLdapAttrMap);
        mProv.modifyAttrs(config, attr);
    }

}

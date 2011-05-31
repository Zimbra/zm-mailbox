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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class BUG_14531 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        Config config = prov.getConfig();
        
        String value = 
            "zimbraSync:(&(|(displayName=*)(cn=*)(sn=*)(gn=*)(mail=*)(zimbraMailDeliveryAddress=*)(zimbraMailAlias=*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))";
        
        Map<String, Object> attr = new HashMap<String, Object>();
        attr.put("+" + Provisioning.A_zimbraGalLdapFilterDef, value);
        
        printer.println("Adding zimbraSync filter to global config " + Provisioning.A_zimbraGalLdapFilterDef);
        prov.modifyAttrs(config, attr);
    }
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
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
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BUG_11562 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapFilterDef();
    }

    @SuppressWarnings("unchecked")
    private void upgradeZimbraGalLdapFilterDef() throws ServiceException {
        Config config = prov.getConfig();

        Pair[] values = {
                new Pair<String, String>(
                        "ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))",
                        "ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))"),

                new Pair<String, String>(
                        "adAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(givenName=%s*)(mail=%s*))(!(msExchHideFromAddressLists=TRUE))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))",
                        "adAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(givenName=%s*)(mail=%s*))(!(msExchHideFromAddressLists=TRUE))(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))"),
        };

        Set<String> curValues = config.getMultiAttrSet(Provisioning.A_zimbraGalLdapFilterDef);

        Map<String, Object> attrs = new HashMap<String, Object>();
        for (Pair<String, String> change : values) {
            String oldValue = change.getFirst();
            String newValue = change.getSecond();

            if (curValues.contains(oldValue)) {
                StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapFilterDef, oldValue);
                StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapFilterDef, newValue);
            }
        }

        modifyAttrs(config, attrs);
    }
}

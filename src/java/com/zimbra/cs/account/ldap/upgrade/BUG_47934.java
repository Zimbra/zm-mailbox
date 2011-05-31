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
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class BUG_47934 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapFilterDef();
        upgradeZimbraGalLdapAttrMap();
    }
    
    void upgradeZimbraGalLdapFilterDef() throws ServiceException {
        Config config = prov.getConfig();
 
        
        Pair[] values = {
            new Pair<String, String>(
                    "zimbraAccounts:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))", 
                    "zimbraAccounts:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))"),
           
            new Pair<String, String>(
                    "zimbraAccountAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))",
                    "zimbraAccountAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(zimbraPhoneticFirstName=%s*)(zimbraPhoneticLastName=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))"),
                    
            new Pair<String, String>(
                    "zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))", 
                    "zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))"),    
        
            new Pair<String, String>(
                    "zimbraAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList)))", 
                    "zimbraAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(zimbraPhoneticFirstName=%s*)(zimbraPhoneticLastName=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList)))"),
            
            new Pair<String, String>(
                    "zimbraSearch:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList)))", 
                    "zimbraSearch:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList)))")
            
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
    
    void upgradeZimbraGalLdapAttrMap() throws ServiceException {
        Config config = prov.getConfig();
        
        String[] values = {
          "zimbraPhoneticCompany,ms-DS-Phonetic-Company-Name=phoneticCompany",
          "zimbraPhoneticFirstName,ms-DS-Phonetic-First-Name=phoneticFirstName",
          "zimbraPhoneticLastName,ms-DS-Phonetic-Last-Name=phoneticLastName"
        };
         
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+" + Provisioning.A_zimbraGalLdapAttrMap, values);

        modifyAttrs(config, attrs);
    }

}

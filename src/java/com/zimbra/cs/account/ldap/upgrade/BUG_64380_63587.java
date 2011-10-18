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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_64380_63587 extends UpgradeOp {
    
    private static final String ATTR_NAME = Provisioning.A_zimbraGalLdapFilterDef;
    
    private static List<Pair> VALUES = Lists.newArrayList(
            new Pair("zimbraSync:(&(|(displayName=*)(cn=*)(sn=*)(gn=*)(mail=*)(zimbraMailDeliveryAddress=*)(zimbraMailAlias=*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))",
                     "zimbraSync:(&(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(&(objectclass=zimbraCalendarResource)(!(zimbraAccountStatus=active))))(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))"), 
            
            new Pair("zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))",
                     "zimbraAccountSync:(&(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))"),
            
            new Pair("zimbraResourceSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraCalendarResource))",
                     "zimbraResourceSync:(&(objectclass=zimbraCalendarResource)(!(zimbraAccountStatus=active)))"),
            
            new Pair("zimbraGroupSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraDistributionList))",
                     "zimbraGroupSync:(objectclass=zimbraDistributionList)"),
                     
            new Pair("zimbraResources:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraCalendarResource))",
                     "zimbraResources:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraCalendarResource)(!(zimbraAccountStatus=active)))"),
            
            new Pair("zimbraResourceAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(objectclass=zimbraCalendarResource))",
                     "zimbraResourceAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(objectclass=zimbraCalendarResource)(!(zimbraAccountStatus=active)))")

            );

    @Override
    void doUpgrade() throws ServiceException {
        
        
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doGlobalConfig(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    @Override
    Description getDescription() {
        StringBuilder oldValues = new StringBuilder();
        StringBuilder newValues = new StringBuilder();
        for (Pair valuePair : VALUES) {
            oldValues.append(valuePair.getFirst() + "\n");
            newValues.append(valuePair.getSecond() + "\n");
        }
        
        return new Description(this, 
                new String[] {ATTR_NAME}, 
                new EntryType[] {EntryType.GLOBALCONFIG},
                oldValues.toString(), 
                newValues.toString(), 
                String.format("Upgrade zimbraSync, zimbraAccountSync, zimbraResourceSync, " +
                        "and zimbraGroupSync GAL filters on %s on global config from " + 
                        "matched old values to the corresponding new value.  " +
                		"This upgrade step covers bug 64380 and 63587.", 
                        ATTR_NAME));
    }

    private void doEntry(ZLdapContext zlc, Entry entry) throws ServiceException {
        String entryName = entry.getLabel();
        
        printer.println();
        printer.println("------------------------------");
        printer.println("Checking " + ATTR_NAME + " on " + entryName);
        
        Set<String> curValues = entry.getMultiAttrSet(ATTR_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (Pair<String, String> valuePair : VALUES) {
            String oldValue = valuePair.getFirst();
            String newValue = valuePair.getSecond();
            if (curValues.contains(oldValue)) {
                StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapFilterDef, oldValue);
                StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapFilterDef, newValue);
            }
        }
        modifyAttrs(entry, attrs);
    }
    
    private void doGlobalConfig(ZLdapContext zlc) throws ServiceException {
        doEntry(zlc, prov.getConfig());
    }
    
}

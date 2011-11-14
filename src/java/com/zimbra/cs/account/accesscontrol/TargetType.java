/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.soap.type.TargetBy;


public enum TargetType {
    account(true,       true,    AttributeClass.account,          com.zimbra.soap.type.TargetType.account,       "Account"),
    calresource(true,   true,    AttributeClass.calendarResource, com.zimbra.soap.type.TargetType.calresource,   "CalendarResource"),
    cos(true,           false,   AttributeClass.cos,              com.zimbra.soap.type.TargetType.cos,           "Cos"),
    dl(true,            true,    AttributeClass.distributionList, com.zimbra.soap.type.TargetType.dl,            "DistributionList"), // static group
    group(true,         true,    AttributeClass.group,            com.zimbra.soap.type.TargetType.group,         "DynamicGroup"),     // dynamic group
    domain(true,        false,   AttributeClass.domain,           com.zimbra.soap.type.TargetType.domain,        "Domain"),
    server(true,        false,   AttributeClass.server,           com.zimbra.soap.type.TargetType.server,        "Server"),
    xmppcomponent(true, false,   AttributeClass.xmppComponent,    com.zimbra.soap.type.TargetType.xmppcomponent, "XMPPComponent"),
    zimlet(true,        false,   AttributeClass.zimletEntry,      com.zimbra.soap.type.TargetType.zimlet,        "Zimlet"),
    config(false,       false,   AttributeClass.globalConfig,     com.zimbra.soap.type.TargetType.config,        "GlobalConfig"),
    global(false,       false,   AttributeClass.aclTarget,        com.zimbra.soap.type.TargetType.global,        "GlobalGrant");
    
    private boolean mNeedsTargetIdentity;
    private boolean mIsDomained;
    private AttributeClass mAttrClass;
    private com.zimbra.soap.type.TargetType jaxbTargetType;
    private String mPrettyName;
    
    //
    // mInheritedByTargetTypes and mInheritFromTargetTypes represents 
    // the same fact from two opposite directions
    //
    
    // set of target types that can inherit from this target type
    // e.g. if this target type is domain, the set would be 
    //      account, calresource, dl, group, domain
    private Set<TargetType> mInheritedByTargetTypes;
    
    // set of target types this target type can inherit from
    // e.g. if this target type is domain, the set would be 
    //      globalGrant, domain
    private Set<TargetType> mInheritFromTargetTypes;
    
    // pretty much like mInheritedByTargetTypes, but this is for LDAP 
    // search of sub-targets of a target type.  This Set is different 
    // from the mInheritedByTargetTypes that it does not contain self
    private Set<TargetType> mSubTargetTypes;

    static {
        init();
    }
    
    TargetType(boolean NeedsTargetIdentity, boolean isDomained,
            AttributeClass attrClass,
            com.zimbra.soap.type.TargetType jaxbTargetType,
            String prettyName) {
        mNeedsTargetIdentity = NeedsTargetIdentity;
        mIsDomained = isDomained;
        mAttrClass = attrClass;
        this.jaxbTargetType = jaxbTargetType;
        mPrettyName = prettyName;
    }

    /* return equivalent JAXB enum */
    public com.zimbra.soap.type.TargetType toJaxb() {
        return jaxbTargetType;
    }

    public static TargetType fromJaxb(com.zimbra.soap.type.TargetType jaxbTT) {
        for (TargetType tt :TargetType.values()) {
            if (tt.toJaxb() == jaxbTT) {
                return tt;
            }
        }
        throw new IllegalArgumentException("Unrecognised TargetType" + jaxbTT);
    }

    private void setInheritedByTargetTypes(TargetType[] targetTypes) {
        mInheritedByTargetTypes = new HashSet<TargetType>(Arrays.asList(targetTypes));
    }
    
    static void init() {
        TargetType.account.setInheritedByTargetTypes(
                new TargetType[]{account});
        
        TargetType.calresource.setInheritedByTargetTypes(
                new TargetType[]{calresource});
        
        TargetType.dl.setInheritedByTargetTypes(
                new TargetType[]{account, calresource, dl});
        
        TargetType.group.setInheritedByTargetTypes(
                new TargetType[]{account, calresource, group});
        
        TargetType.domain.setInheritedByTargetTypes(
                new TargetType[]{account, calresource, dl, group, domain});
        
        TargetType.cos.setInheritedByTargetTypes(
                new TargetType[]{cos});
        
        TargetType.server.setInheritedByTargetTypes(
                new TargetType[]{server});
        
        TargetType.xmppcomponent.setInheritedByTargetTypes(
                new TargetType[]{xmppcomponent});
        
        TargetType.zimlet.setInheritedByTargetTypes(
                new TargetType[]{zimlet});
        
        TargetType.config.setInheritedByTargetTypes(
                new TargetType[]{config});
        
        TargetType.global.setInheritedByTargetTypes(
                new TargetType[]{account, 
                                 calresource, 
                                 cos,
                                 dl,
                                 group,
                                 domain,
                                 server,
                                 xmppcomponent,
                                 zimlet,
                                 config,
                                 global});  // inherited by all
        
        // compute mInheritFromTargetTypes and mSubTargetTypes
        // from mInheritedByTargetTypes
        for (TargetType inheritFrom : TargetType.values()) {
            inheritFrom.mInheritFromTargetTypes = new HashSet<TargetType>();
            inheritFrom.mSubTargetTypes = new HashSet<TargetType>();
            
            for (TargetType inheritedBy : TargetType.values()) {
                if (inheritedBy.mInheritedByTargetTypes.contains(inheritFrom)) {
                    inheritFrom.mInheritFromTargetTypes.add(inheritedBy);
                }
            }
            
            for (TargetType tt: inheritFrom.mInheritedByTargetTypes) {
                if (inheritFrom != tt) {
                    inheritFrom.mSubTargetTypes.add(tt);
                }
            }
        }
        
        for (TargetType tt : TargetType.values()) {
            tt.mInheritedByTargetTypes = Collections.unmodifiableSet(tt.mInheritedByTargetTypes);
            tt.mInheritFromTargetTypes = Collections.unmodifiableSet(tt.mInheritFromTargetTypes);
            tt.mSubTargetTypes = Collections.unmodifiableSet(tt.mSubTargetTypes);
        }
        
        /*
        for (TargetType tt : TargetType.values()) {
            tt.dump();
        }
        */
    }
    
    private void dump() {
        System.out.println();
        System.out.println(mPrettyName);
        
        System.out.println("mInheritedByTargetTypes");
        for (TargetType tt : mInheritedByTargetTypes) {
            System.out.println("    " + tt);
        }
        
        System.out.println("mInheritFromTargetTypes");
        for (TargetType tt : mInheritFromTargetTypes) {
            System.out.println("    " + tt);
        }
        
        System.out.println("mSubTargetTypes");
        for (TargetType tt : mSubTargetTypes) {
            System.out.println("    " + tt);
        }
    }
    
    /**
     * returns if targetType can inherit from this targetType
     * 
     * @param targetType the targetType of question
     * @return
     */
    boolean isInheritedBy(TargetType targetType) {
        return mInheritedByTargetTypes.contains(targetType);
    }
    
    /**
     * returns the set of sub target types this target type can be inherited by.
     * do not include the target type itself.
     * 
     * e.g. if this is domain, then account, calresource, and dl will be returned
     * 
     * @return
     */
    Set<TargetType> subTargetTypes() {
        return mSubTargetTypes;
    }
    
    /**
     * returns the set of target types this target type can inherit from
     * @return
     */
    Set<TargetType> inheritFrom() {
        return mInheritFromTargetTypes;
    }
    
    public static boolean canBeInheritedFrom(Entry target) throws ServiceException {
        TargetType targetType = TargetType.getTargetType(target);
        return !targetType.subTargetTypes().isEmpty();
    }
    
    public static TargetType fromCode(String s) throws ServiceException {
        try {
            return TargetType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown target type: " + s, e);
        }
    }
    
    public String getCode() {
        return name();
    }
    
    public String getPrettyName() {
        return mPrettyName;
    }
    
    public boolean needsTargetIdentity() {
        return mNeedsTargetIdentity;
    }
    
    AttributeClass getAttributeClass() {
        return mAttrClass;
    }
    
    public static Entry lookupTarget(Provisioning prov, TargetType targetType, 
            TargetBy targetBy, String target) throws ServiceException {
        return lookupTarget(prov, targetType, targetBy, target, true);
    }
    
    /**
     * central place where a target should be loaded
     * 
     * @param prov
     * @param targetType
     * @param targetBy
     * @param target
     * @return
     * @throws ServiceException
     */
    public static Entry lookupTarget(Provisioning prov, TargetType targetType, TargetBy targetBy, 
            String target, boolean mustFind) throws ServiceException {
        Entry targetEntry = null;
        
        switch (targetType) {
        case account:
            targetEntry = prov.get(AccountBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(target); 
            }
            break;
        case calresource:
            targetEntry = prov.get(Key.CalendarResourceBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(target); 
            }
            break;
        case dl:
        case group:
            targetEntry = prov.getGroupBasic(Key.DistributionListBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(target); 
            }
            break;
        case domain:
            targetEntry = prov.get(Key.DomainBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_DOMAIN(target); 
            }
            break;
        case cos:
            targetEntry = prov.get(Key.CosBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_COS(target);
            }
            break;
        case server:
            targetEntry = prov.get(Key.ServerBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_SERVER(target); 
            }
            break;
        case xmppcomponent:
            targetEntry = prov.get(Key.XMPPComponentBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(target); 
            }
            break;    
        case zimlet:
            Key.ZimletBy zimletBy = Key.ZimletBy.fromString(targetBy.name());
            if (zimletBy != Key.ZimletBy.name) {
                throw ServiceException.INVALID_REQUEST("zimlet must be by name", null);
            }
            targetEntry = prov.getZimlet(target);
            if (targetEntry == null && mustFind) {
                throw AccountServiceException.NO_SUCH_ZIMLET(target); 
            }
            break;
        case config:
            targetEntry = prov.getConfig();
            break;
        case global:
            targetEntry = prov.getGlobalGrant();
            break;
        default:
            ServiceException.INVALID_REQUEST("invallid target type for lookupTarget:" + targetType.toString(), null);
        }
    
        return targetEntry;
    }

    public static Set<String> getAttrsInClass(Entry target) throws ServiceException {
        AttributeClass klass = TargetType.getAttributeClass(target);
        return AttributeManager.getInstance().getAllAttrsInClass(klass);
    }
    
    static AttributeClass getAttributeClass(Entry target) throws ServiceException {
        return TargetType.getTargetType(target).getAttributeClass();
    }
    
    public static TargetType getTargetType(Entry target) throws ServiceException{
        
        if (target instanceof CalendarResource)
            return TargetType.calresource;
        else if (target instanceof Account)
            return TargetType.account;
        else if (target instanceof Domain)
            return TargetType.domain;
        else if (target instanceof Cos)
            return TargetType.cos;
        else if (target instanceof DistributionList)
            return TargetType.dl;
        else if (target instanceof DynamicGroup)
            return TargetType.group;
        else if (target instanceof Server)
            return TargetType.server;
        else if (target instanceof Config)
            return TargetType.config;
        else if (target instanceof GlobalGrant)
            return TargetType.global;
        else if (target instanceof Zimlet)
            return TargetType.zimlet;
        else if (target instanceof XMPPComponent)
            return TargetType.xmppcomponent;
        else 
            throw ServiceException.FAILURE("internal error, target is : " + 
                    (target==null?"null":target.getClass().getCanonicalName()), null);
    }

    boolean isDomained() {
        return mIsDomained;
    }
    
    public boolean isGroup() {
        return (this == TargetType.dl || this == TargetType.group);
    }
    
    public static String getId(Entry target) {
        return (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
    }
    
    public static Domain getTargetDomain(Provisioning prov, Entry target) 
    throws ServiceException{
        
        if (target instanceof CalendarResource) {
            CalendarResource cr = (CalendarResource)target;
            return prov.getDomain(cr);
        } else if (target instanceof Account) {
            Account acct = (Account)target;
            return prov.getDomain(acct);
        } else if (target instanceof DistributionList) {
            DistributionList dl = (DistributionList)target;
            return prov.getDomain(dl);
        } else if (target instanceof DynamicGroup) {
            DynamicGroup group = (DynamicGroup)target;
            return prov.getDomain(group);
        } else
            return null;
    }
    
    public static String getTargetDomainName(Provisioning prov, Entry target) 
    throws ServiceException{
        
        if (target instanceof CalendarResource) {
            CalendarResource cr = (CalendarResource)target;
            return cr.getDomainName();
        } else if (target instanceof Account) {
            Account acct = (Account)target;
            return acct.getDomainName();
        } else if (target instanceof DistributionList) {
            DistributionList dl = (DistributionList)target;
            return dl.getDomainName();
        } else if (target instanceof DynamicGroup) {
            DynamicGroup group = (DynamicGroup)target;
            return group.getDomainName();
        } else {
            return null;
        }
    }
    
    static String getSearchBase(Provisioning prov, TargetType tt) 
    throws ServiceException {
        LdapDIT dit = ((LdapProv)prov).getDIT();
        
        String base;
        
        switch (tt) {
        case account:
        case calresource:
        case dl:
        case group:    
            base = dit.mailBranchBaseDN();
            break;
        case domain:
            base = dit.domainBaseDN();
            break;
        case cos:
            base = dit.cosBaseDN();
            break;    
        case server:
            base = dit.serverBaseDN();
            break;
        case xmppcomponent:
            base = dit.xmppcomponentBaseDN();
            break;    
        case zimlet:
            base = dit.zimletBaseDN();
            break;
        case config:
            base = dit.configDN();
            break;
        case global: 
            // is really an internal error, globalgrant should never appear in the 
            // targetTypes if we get here, because it is not a sub-target of any 
            // other target types.  
            base = dit.globalGrantDN();
            break;
        default:
            throw ServiceException.FAILURE("internal error", null);
        }
        
        return base;
    }
    

    static class SearchBaseAndOC {
        String mBase;
        List<String> mOCs;
    }
    
    /*
     * This method is called for searching for negative grants granted on a  
     * "sub-target" of a target on which we are granting a right.  If the 
     * granting account has any negative grants for a right that is a 
     * "sub-right" of the right he is trying to grant to someone else,
     * and this negative grant is on a "sub-target" of the target he is 
     * trying to grant on, then sorry, it is not allowed.  Because otherwise
     * the person receiving the grant can end up getting "more" rights 
     * than the granting person.
     * 
     * e.g. on domain D.com, adminA +domainAdminRights
     *      on dl dl@D.com,  adminA -setPassword
     *      
     *      When adminA tries to grant domainAdminRights to adminB on 
     *      domain D.com, it should not be allowed; otherwise adminB 
     *      can setPassword for accounts in dl@D.com, but adminA cannot.
     * 
     * The targetTypes parameter contains a set of target types that are 
     * "sub-target" of the target type on which we are trying to grant.
     * 
     * SO, for the search base:
     *   - for domain-ed targets, dls must be under the domain, but accounts 
     *     in dls can be in any domain, so the search base is the mail branch base.
     *   - for non domain-ed targets, the search base is the base DN for the type
     *   
     *   we go through all wanted target types, find the least common base  
     */ 
    static Map<String, Set<String>> getSearchBasesAndOCs(Provisioning prov, 
            Set<TargetType> targetTypes) throws ServiceException {
        
        // sanity check, is really an internal error if targetTypes is empty
        if (targetTypes.isEmpty())
            return null;
        
        Map<String, Set<String>> tempResult = new HashMap<String, Set<String>>();
         
                
        for (TargetType tt : targetTypes) {
            String base = getSearchBase(prov, tt);
            
            String oc = tt.getAttributeClass().getOCName();
            Set<String> ocs = tempResult.get(base);
            if (ocs == null)
                ocs = new HashSet<String>();
            ocs.add(oc);
            tempResult.put(base, ocs);
        }
        
        // optimize
        LdapDIT dit = ((LdapProv)prov).getDIT();
        String configBranchBase = dit.configBranchBaseDN();
        Set<String> mailBranchOCs = new HashSet<String>();
        Set<String> configBranchOCs = new HashSet<String>();
        
        String leastCommonBaseInMailBranch = null;
        String leastCommonBaseInConfigBranch = null;
        
        for (Map.Entry<String, Set<String>> entry : tempResult.entrySet()) {
             String base = entry.getKey();
             Set<String> ocs  = entry.getValue();
             
             boolean inConfigBranch = base.endsWith(configBranchBase);
             if (inConfigBranch) {
                 configBranchOCs.addAll(ocs);
                 
                 if (leastCommonBaseInConfigBranch == null) {
                     leastCommonBaseInConfigBranch = base;
                 } else {
                     leastCommonBaseInConfigBranch = 
                         getCommonBase(base, leastCommonBaseInConfigBranch);
                 }

             } else {
                 mailBranchOCs.addAll(ocs);
                 
                 if (leastCommonBaseInMailBranch == null) {
                     leastCommonBaseInMailBranch = base;
                 } else {
                     leastCommonBaseInMailBranch = 
                         getCommonBase(base, leastCommonBaseInMailBranch);
                 }
             }
        }
        
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        
        // if zimbra default DIT and both mail branch and config branch are needed, merge the two
        if (LdapDIT.isZimbraDefault(dit)) {
            if (leastCommonBaseInMailBranch != null && leastCommonBaseInConfigBranch != null) {
                // merge the two
                String commonBase = getCommonBase(leastCommonBaseInMailBranch, 
                        leastCommonBaseInConfigBranch);
                Set<String> allOCs = SetUtil.union(mailBranchOCs, configBranchOCs);
                result.put(commonBase, allOCs);
                return result;
            }
        } 
        
        // bug 48272, do two searches, one based at the mail branch, one based on the config branch.
        if (leastCommonBaseInMailBranch != null) {
            result.put(leastCommonBaseInMailBranch, mailBranchOCs);
        }
        if (leastCommonBaseInConfigBranch != null) {
            result.put(leastCommonBaseInConfigBranch, configBranchOCs);
        }

        return result;
    }
    
    static String getCommonBase(String dn1, String dn2) {
        String top = "";
        
        if (top.equals(dn1) || top.equals(dn2))
            return top;
        
        String[] rdns1 = dn1.split(",");
        String[] rdns2 = dn2.split(",");
        
        String[] shorter = rdns1.length < rdns2.length? rdns1 : rdns2;
        
        int i = 0;
        while (i < shorter.length) {
            if (!rdns1[rdns1.length-1-i].equals(rdns2[rdns2.length-1-i]))
                break;
            else;
                i++;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int j = shorter.length - i; j < shorter.length; j++) {
            if (j != shorter.length - i)
                sb.append(",");
            sb.append(shorter[j]);
        }
            
        return sb.toString();
    }

}

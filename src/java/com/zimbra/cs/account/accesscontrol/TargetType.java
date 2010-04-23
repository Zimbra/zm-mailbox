/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Provisioning.XMPPComponentBy;
import com.zimbra.cs.account.Provisioning.ZimletBy;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;


public enum TargetType {
    account(true,       true,    AttributeClass.account,          "Account"),
    calresource(true,   true,    AttributeClass.calendarResource, "CalendarResource"),
    cos(true,           false,   AttributeClass.cos,              "Cos"),
    dl(true,            true,    AttributeClass.distributionList, "DistributionList"),
    domain(true,        false,   AttributeClass.domain,           "Domain"),
    server(true,        false,   AttributeClass.server,           "Server"),
    xmppcomponent(true, false,   AttributeClass.xmppComponent,    "XMPPComponent"),
    zimlet(true,        false,   AttributeClass.zimletEntry,      "Zimlet"),
    config(false,       false,   AttributeClass.globalConfig,     "GlobalConfig"),
    global(false,       false,   AttributeClass.aclTarget,        "GlobalGrant");
    
    private boolean mNeedsTargetIdentity;
    private boolean mIsDomained;
    private AttributeClass mAttrClass;
    private String mPrettyName;
    
    //
    // mInheritedByTargetTypes and mInheritFromTargetTypes represents 
    // the same fact from two opposite directions
    //
    
    // set of designated target types for a right that can be 
    // inherited from this target type
    private Set<TargetType> mInheritedByTargetTypes;
    
    // set of target types from which the designated target type
    // for a right can inherit from
    private Set<TargetType> mInheritFromTargetTypes;
    
    // pretty much like mInheritedByTargetTypes, but this is for LDAP 
    // search of sub-targets of a target type.  This Set is different 
    // from the mInheritedByTargetTypes that it:
    // 1. does not contain self
    // 2. for calresource, does not contain account.  Because account 
    //    is not really "sub-target" of calresource.
    private Set<TargetType> mSubTargetTypes;

    static {
        init();
    }
    
    /**
     * 
     * @param NeedsTargetIdentity
     * @param attrClass
     * @param applicableTargetTypes target types of rights that can be granted on this target type
     *                              if null, all target types
     */
    TargetType(boolean NeedsTargetIdentity, boolean isDomained, AttributeClass attrClass, String prettyName) {
        mNeedsTargetIdentity = NeedsTargetIdentity;
        mIsDomained = isDomained;
        mAttrClass = attrClass;
        mPrettyName = prettyName;
    }
    
    private void setInheritedByTargetTypes(TargetType[] targetTypes) {
        mInheritedByTargetTypes = new HashSet<TargetType>(Arrays.asList(targetTypes));
    }
    
    static void init() {
        TargetType.account.setInheritedByTargetTypes(new TargetType[]{TargetType.account});
        TargetType.calresource.setInheritedByTargetTypes(new TargetType[]{TargetType.account, TargetType.calresource});
        TargetType.dl.setInheritedByTargetTypes(new TargetType[]{TargetType.account, TargetType.calresource, TargetType.dl});
        TargetType.domain.setInheritedByTargetTypes(new TargetType[]{TargetType.account, TargetType.calresource, TargetType.dl, TargetType.domain});
        TargetType.cos.setInheritedByTargetTypes(new TargetType[]{TargetType.cos});
        TargetType.server.setInheritedByTargetTypes(new TargetType[]{TargetType.server});
        TargetType.xmppcomponent.setInheritedByTargetTypes(new TargetType[]{TargetType.xmppcomponent});
        TargetType.zimlet.setInheritedByTargetTypes(new TargetType[]{TargetType.zimlet});
        TargetType.config.setInheritedByTargetTypes(new TargetType[]{TargetType.config});
        TargetType.global.setInheritedByTargetTypes(new TargetType[]{TargetType.account, 
                                                        TargetType.calresource, 
                                                        TargetType.cos,
                                                        TargetType.dl,
                                                        domain,
                                                        server,
                                                        xmppcomponent,
                                                        zimlet,
                                                        config,
                                                        global});  // inherited by all
        
        // compute mInheritFromTargetTypes and  mInheritedByOtherTargetTypes 
        // from mInheritedByTargetTypes
        for (TargetType inheritFrom : TargetType.values()) {
            inheritFrom.mInheritFromTargetTypes = new HashSet<TargetType>();
            inheritFrom.mSubTargetTypes = new HashSet<TargetType>();
            
            for (TargetType inheritedBy : TargetType.values()) {
                if (inheritedBy.mInheritedByTargetTypes.contains(inheritFrom))
                    inheritFrom.mInheritFromTargetTypes.add(inheritedBy);
            }
            
            for (TargetType tt: inheritFrom.mInheritedByTargetTypes) {
                // add this ugly check, see comments for mSubTargetTypes above
                if (inheritFrom == TargetType.calresource) {
                    if (inheritFrom != tt && tt != TargetType.account)
                        inheritFrom.mSubTargetTypes.add(tt);
                } else {
                    if (inheritFrom != tt)
                        inheritFrom.mSubTargetTypes.add(tt);
                }
            }
        }
        
        for (TargetType tt : TargetType.values()) {
            tt.mInheritedByTargetTypes = Collections.unmodifiableSet(tt.mInheritedByTargetTypes);
            tt.mInheritFromTargetTypes = Collections.unmodifiableSet(tt.mInheritFromTargetTypes);
            tt.mSubTargetTypes = Collections.unmodifiableSet(tt.mSubTargetTypes);
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
    
    public static Entry lookupTarget(Provisioning prov, TargetType targetType, TargetBy targetBy, String target) throws ServiceException {
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
    static Entry lookupTarget(Provisioning prov, TargetType targetType, TargetBy targetBy, String target, boolean mustFind) throws ServiceException {
        Entry targetEntry = null;
        
        switch (targetType) {
        case account:
            targetEntry = prov.get(AccountBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_ACCOUNT(target); 
            break;
        case calresource:
            targetEntry = prov.get(CalendarResourceBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(target); 
            break;
        case dl:
            targetEntry = prov.getAclGroup(DistributionListBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(target); 
            break;
        case domain:
            targetEntry = prov.get(DomainBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_DOMAIN(target); 
            break;
        case cos:
            targetEntry = prov.get(CosBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_COS(target); 
            break;
        case server:
            targetEntry = prov.get(ServerBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_SERVER(target); 
            break;
        case xmppcomponent:
            targetEntry = prov.get(XMPPComponentBy.fromString(targetBy.name()), target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(target); 
            break;    
        case zimlet:
            ZimletBy zimletBy = ZimletBy.fromString(targetBy.name());
            if (zimletBy != ZimletBy.name)
                throw ServiceException.INVALID_REQUEST("zimlet must be by name", null);
            targetEntry = prov.getZimlet(target);
            if (targetEntry == null && mustFind)
                throw AccountServiceException.NO_SUCH_ZIMLET(target); 
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

    static Set<String> getAttrsInClass(Entry target) throws ServiceException {
        AttributeClass klass = TargetType.getAttributeClass(target);
        return AttributeManager.getInstance().getAllAttrsInClass(klass);
    }
    
    static AttributeClass getAttributeClass(Entry target) throws ServiceException {
        
        if (target instanceof CalendarResource)
            return AttributeClass.calendarResource;
        else if (target instanceof Account)
            return AttributeClass.account;
        else if (target instanceof Domain)
            return AttributeClass.domain;
        else if (target instanceof Cos)
            return AttributeClass.cos;
        else if (target instanceof DistributionList)
            return AttributeClass.distributionList;
        else if (target instanceof Server)
            return AttributeClass.server;
        else if (target instanceof Config)
            return AttributeClass.globalConfig;
        else if (target instanceof GlobalGrant)
            return AttributeClass.aclTarget;
        else if (target instanceof Zimlet)
            return AttributeClass.zimletEntry;
        else if (target instanceof XMPPComponent)
            return AttributeClass.xmppComponent;
        else
            throw ServiceException.FAILURE("internal error", null);
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
            throw ServiceException.FAILURE("internal error", null);
    }

    boolean isDomained() {
        return mIsDomained;
    }
    
    public static String getId(Entry target) {
        return (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
    }
    
    public static Domain getTargetDomain(Provisioning prov, Entry target) throws ServiceException{
        
        if (target instanceof CalendarResource) {
            CalendarResource cr = (CalendarResource)target;
            return prov.getDomain(cr);
        } else if (target instanceof Account) {
            Account acct = (Account)target;
            return prov.getDomain(acct);
        } else if (target instanceof DistributionList) {
            DistributionList dl = (DistributionList)target;
            return prov.getDomain(dl);
        } else
            return null;
    }
    
    public static String getTargetDomainName(Provisioning prov, Entry target) throws ServiceException{
        
        if (target instanceof CalendarResource) {
            CalendarResource cr = (CalendarResource)target;
            return cr.getDomainName();
        } else if (target instanceof Account) {
            Account acct = (Account)target;
            return acct.getDomainName();
        } else if (target instanceof DistributionList) {
            DistributionList dl = (DistributionList)target;
            return dl.getDomainName();
        } else
            return null;
    }
    
    /*
     * This method is called for searching for negative grants granted on a  
     * "sub-target" of a target on which we are granting a right.  If the 
     * granting account has any negative grants for a right that is a 
     * "sub-right" of the right he is trying to grant to someone else,
     * and ths negative grant is on a "sub-target" of the target he is 
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
     *     in dls can be in any domain, so the search base is ''.
     *   - for non domain-ed targets, the search base is the base DN for the type
     *   
     *   we go through all wanted target types, find the least common base  
     */ 
    static Pair<String, Set<String>> getSearchBaseAndOCs(Provisioning prov, 
            Set<TargetType> targetTypes) throws ServiceException {
        
        // sanity check, is really an internal error if targetTypes is empty
        if (targetTypes.isEmpty())
            return null;
        
        LdapDIT dit = ((LdapProvisioning)prov).getDIT();
        
        String[] curRnds = null;
        String base = null;
        Set<String> ocs = new HashSet<String>();
        
        for (TargetType tt : targetTypes) {
            
            String bs;
            
            switch (tt) {
            case account:
            case calresource:
            case dl:
            case domain:
                bs = "";
                break;
            case cos:
                bs = dit.cosBaseDN();
                break;    
            case server:
                bs = dit.serverBaseDN();
                break;
            case xmppcomponent:
                bs = dit.xmppcomponentBaseDN();
                break;    
            case zimlet:
                bs = dit.zimletBaseDN();
                break;
            case config:
                bs = dit.configDN();
                break;
            case global: 
                // is really an internal error, globalgrant should never appear in the 
                // targetTypes if we get here, because it is not a sub-target of any 
                // other target types.  
                bs = dit.globalGrantDN();
                break;
            default:
                throw ServiceException.FAILURE("internal error", null);
            }
            
            // if we've reached the '', break
            if (bs.equals("")) {
                base = bs;
            } else {
                String[] rdns = bs.split(",");
                if (curRnds == null)
                    curRnds = rdns;
                else {
                    // find the least common base
                    int shorter = rdns.length < curRnds.length? rdns.length : curRnds.length;
                    for (int i = 1 ; i <= shorter; i++) {
                        if (!rdns[rdns.length-i].equals(curRnds[curRnds.length-i])) {
                            if (i == 1) {
                                base = "";
                                break;
                            } else {
                                
                                curRnds = new String[i-1];
                                for (int j = 0; j < i-1; j++)
                                    curRnds[curRnds.length-1-j] = rdns[rdns.length-1-j];
                            }
                        }
                    }
                    
                }
            }
            if (base != null)
                break;
        }
        
        if (base == null) {
            boolean first = true;
            for (int i=0; i<curRnds.length; i++) {
                if (first) {
                    base = curRnds[i];
                    first = false;
                } else {
                    base = base + "," + curRnds[i];
                }
            }
        }
        
        for (TargetType tt : targetTypes) {
            ocs.add(tt.getAttributeClass().getOCName());
        }
        
        return new Pair<String, Set<String>>(base, ocs);
    }
    
    private static void doTest(Provisioning prov, Set<TargetType> ttInheritBy) throws ServiceException {
        Pair<String, Set<String>> baseAndOcs = getSearchBaseAndOCs(prov, ttInheritBy);
        System.out.println("    base:" + baseAndOcs.getFirst());
        System.out.print("    OCs: ");
        for (String oc : baseAndOcs.getSecond())
            System.out.print(oc + " ");
        
        System.out.println("\n");
    }
    
    public static void main(String[] args) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        Set<TargetType> ttInheritBy;
        
        for (TargetType tt : TargetType.values()) {
            System.out.println("Target " + tt.name());  
            
            ttInheritBy = new HashSet<TargetType>();
            ttInheritBy.add(tt);
            doTest(prov, ttInheritBy);
            
            ttInheritBy = tt.subTargetTypes();
            
            System.out.print("    sub target types: ");
            for (TargetType ttib : ttInheritBy)
                System.out.print(ttib.name() + " ");
            System.out.println();
            
            if (ttInheritBy.isEmpty()) {
                System.out.println();
                continue;
            }
            
            doTest(prov, ttInheritBy);
        }
        
        System.out.println("==========");
        System.out.println("Test");
        ttInheritBy = new HashSet<TargetType>();
        ttInheritBy.add(TargetType.cos);
        ttInheritBy.add(TargetType.server);
        ttInheritBy.add(TargetType.zimlet);
        ttInheritBy.add(TargetType.global);
        doTest(prov, ttInheritBy);

    }
}

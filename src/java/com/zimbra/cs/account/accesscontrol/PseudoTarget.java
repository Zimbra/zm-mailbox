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
package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;

public class PseudoTarget {
    
    static class PseudoZimbraId {
        private static final String PSEUDO_ZIMBRA_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        
        static String getPseudoZimbraId() {
            return PSEUDO_ZIMBRA_ID;
        }
        
        static boolean isPseudoZimrbaId(String zid) {
            return (PSEUDO_ZIMBRA_ID.equals(zid));
        }
    }
    
    public static boolean isPseudoEntry(Entry entry) {
        if (entry instanceof PseudoAccount ||
            entry instanceof PseudoCalendarResource ||
            entry instanceof PseudoDistributionList ||
            entry instanceof PseudoCos ||
            entry instanceof PseudoDomain ||
            entry instanceof PseudoServer ||
            entry instanceof PseudoXMPPComponent ||
            entry instanceof PseudoZimlet) {
            return true;
        } else
            return false;
    }
    
    /*
     * PseudoAccount
     * PseudoCalendarResource
     * PseudoCalendarResource
     * 
     * can have a real or a pseudo domain
     *   - if having a pseudo domain, the getPseudoDomain method will return the pseudo domain
     *   - if having a real domain, the getPseudoDomain method will return null
     *   
     * can have   
     */
    
    static class PseudoAccount extends Account {
        Domain mPseudoDomain;
        AclGroups mAclGroups;
        
        PseudoAccount(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, 
                Provisioning prov, Domain pseudoDomain) {
            super(name, id, attrs, defaults, prov);
            mPseudoDomain = pseudoDomain;
        }
        
        /*
         * create a pseudo account that is a member of the specified group.
         * 
         * The acl groups this account belongs are essentially the acl groups 
         * of the specified group, plus the group.
         */
        PseudoAccount(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, 
                Provisioning prov, DistributionList group) throws ServiceException {
            super(name, id, attrs, defaults, prov);
            mAclGroups = prov.getAclGroups(group, false);
        }
        
        Domain getPseudoDomain() {
            return mPseudoDomain;
        }
        
        AclGroups getAclGroups() {
            return mAclGroups;
        }
    }
    
    static class PseudoCalendarResource extends CalendarResource {
        Domain mPseudoDomain;
        
        public PseudoCalendarResource(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, 
                Provisioning prov, Domain pseudoDomain) {
            super(name, id, attrs, defaults, prov);
            mPseudoDomain = pseudoDomain;
        }
        
        Domain getPseudoDomain() {
            return mPseudoDomain;
        }
    }
    
    static class PseudoDistributionList extends DistributionList {
        Domain mPseudoDomain;
        
        public PseudoDistributionList(String name, String id, Map<String, Object> attrs, 
                Provisioning prov, Domain pseudoDomain) {
            super(name, id, attrs, prov);
            mPseudoDomain = pseudoDomain;
        }
        
        Domain getPseudoDomain() {
            return mPseudoDomain;
        }
    }
    
    static class PseudoCos extends Cos{
        private PseudoCos(String name, String id, Map<String,Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }
    }
    
    static class PseudoDomain extends Domain {
        private PseudoDomain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
            super(name, id, attrs, defaults, prov);
        }
    }
    
    static class PseudoServer extends Server {
        private PseudoServer(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults, Provisioning prov) {
            super(name, id, attrs, defaults, prov);
        }
    }
    
    static class PseudoXMPPComponent extends XMPPComponent {
        private PseudoXMPPComponent(String name, String id, Map<String,Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }
    }
    
    static class PseudoZimlet extends Zimlet {
        private PseudoZimlet(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }
    }
    
    /**
     * short hand for PseudoTarget.createPseudoTarget(prov, TargetType.domain, null, null, false, null, null);
     * @param prov
     * @return
     * @throws ServiceException
     */
    public static Domain createPseudoDomain(Provisioning prov) throws ServiceException {
        return (Domain)createPseudoTarget(prov, TargetType.domain, null, null, false, null, null);
    }
    
    /**
     * construct a pseudo target
     * 
     * if targetType is a domain-ed type: account. cr, dl:
     * then exactly one of the following must be passed in:
     *    - domainBy == null
     *      domainStr == null
     *      createPseudoDomain == true
     *    or
     *    - domainBy != null
     *      domainStr != null
     *      createPseudoDomain == false   
     * 
     * @param prov
     * @param targetType
     * @param domainBy
     * @param domainStr
     * @param createPseudoDomain
     * @param cosBy
     * @param cosStr
     * @return
     * @throws ServiceException
     */
    public static Entry createPseudoTarget(Provisioning prov,
            TargetType targetType, 
            DomainBy domainBy, String domainStr, boolean createPseudoDomain,
            CosBy cosBy, String cosStr) throws ServiceException {
        
        Entry targetEntry = null;
        Config config = prov.getConfig();
        
        String zimbraId = PseudoZimbraId.getPseudoZimbraId();
        Map<String, Object> attrMap = new HashMap<String, Object>();
        attrMap.put(Provisioning.A_zimbraId, zimbraId);
        
        Domain pseudoDomain = null;
        Domain domain = null;
        if (targetType == TargetType.account ||
            targetType == TargetType.calresource ||
            targetType == TargetType.dl) {
            
            if (createPseudoDomain)
                domain = pseudoDomain = (Domain)createPseudoTarget(prov, TargetType.domain, null, null, false, null, null);
            else {
                if (domainBy == null || domainStr == null)
                    throw ServiceException.INVALID_REQUEST("domainBy and domain identifier is required", null);
                domain = prov.get(domainBy, domainStr);
            }
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domainStr);
        }
        
        switch (targetType) {
        case account:
        case calresource:
            Cos cos = null;
            if (cosBy != null && cosStr != null) {
                cos = prov.get(cosBy, cosStr);
                if (cos == null)
                    throw AccountServiceException.NO_SUCH_COS(cosStr);
                attrMap.put(Provisioning.A_zimbraCOSId, cos.getId());
            } else {
                String domainCosId = domain != null ? domain.getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null) : null;
                if (domainCosId != null) cos = prov.get(CosBy.id, domainCosId);
                if (cos == null) cos = prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME);
            }
            
            if (targetType == TargetType.account)
                targetEntry = new PseudoAccount("pseudo@"+domain.getName(),
                                           zimbraId,
                                           attrMap,
                                           cos.getAccountDefaults(),
                                           prov,
                                           pseudoDomain);
            else
                targetEntry = new PseudoCalendarResource("pseudo@"+domain.getName(),
                                           zimbraId,
                                           attrMap,
                                           cos.getAccountDefaults(),
                                           prov,
                                           pseudoDomain);
            break;
            
        case cos:  
            targetEntry = new PseudoCos("pseudocos", zimbraId, attrMap, prov);
            break;
        case dl:
            targetEntry = new PseudoDistributionList("pseudo@"+domain.getName(), zimbraId, attrMap, prov, pseudoDomain);
            DistributionList dl = (DistributionList)targetEntry;
            dl.turnToAclGroup();
            break;
        case domain:
            targetEntry = new PseudoDomain("pseudo.pseudo", zimbraId, attrMap, config.getDomainDefaults(), prov);
            break;
        case server:  
            targetEntry = new PseudoServer("pseudo.pseudo", zimbraId, attrMap, config.getServerDefaults(), prov);
            break;
        case xmppcomponent:
            targetEntry = new PseudoXMPPComponent("pseudo", zimbraId, attrMap, prov);
            break;
        case zimlet:
            targetEntry = new PseudoZimlet("pseudo", zimbraId, attrMap, prov);
            break;
        default: 
            throw ServiceException.INVALID_REQUEST("unsupported target for createPseudoTarget: " + targetType.getCode(), null);
        }
        
        return targetEntry;
    }


}

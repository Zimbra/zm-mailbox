package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.RightChecker.EffectiveACL;

public enum TargetType {
    account(true),
    resource(true),
    distributionlist(true),
    domain(true),
    cos(true),
    right(true),
    server(true),
    config(false),
    global(false);
    
    private boolean mNeedsTargetIdentity;
    
    TargetType(boolean NeedsTargetIdentity) {
        mNeedsTargetIdentity = NeedsTargetIdentity;
    }
    
    public static TargetType fromString(String s) throws ServiceException {
        try {
            return TargetType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown target type: " + s, e);
        }
    }
    
    public String getCode() {
        return name();
    }
    
    public boolean needsTargetIdentity() {
        return mNeedsTargetIdentity;
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
    public static Entry lookupTarget(Provisioning prov, TargetType targetType, TargetBy targetBy, String target) throws ServiceException {
        Entry targetEntry = null;
        
        switch (targetType) {
        case account:
            targetEntry = prov.get(AccountBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(target); 
            break;
        case resource:
            targetEntry = prov.get(CalendarResourceBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(target); 
            break;
        case distributionlist:
            targetEntry = prov.getAclGroup(DistributionListBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(target); 
            break;
        case domain:
            targetEntry = prov.get(DomainBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(target); 
            break;
        case cos:
            targetEntry = prov.get(CosBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_COS(target); 
            break;
        case right:
            throw ServiceException.FAILURE("TODO", null);
            // break;
        case server:
            targetEntry = prov.get(ServerBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_SERVER(target); 
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

    boolean IsRightApplicableOnTarget(Right right, Entry target) {
        
        if (target instanceof GlobalGrant)
            return true;
        else if (target instanceof Config)
            return right.applicableOnTargetType(TargetType.config);
        else if (target instanceof Server)
            return right.applicableOnTargetType(TargetType.server);
        else if (target instanceof Cos)
            return right.applicableOnTargetType(TargetType.cos);
        else if (target instanceof Domain)
            return right.applicableOnTargetType(TargetType.domain) ||
                   right.applicableOnTargetType(TargetType.distributionlist) ||
                   right.applicableOnTargetType(TargetType.resource) ||
                   right.applicableOnTargetType(TargetType.account);
        else if (target instanceof DistributionList)
            return right.applicableOnTargetType(TargetType.distributionlist) ||
                   right.applicableOnTargetType(TargetType.resource) ||
                   right.applicableOnTargetType(TargetType.account);
        else if (target instanceof CalendarResource)
            return right.applicableOnTargetType(TargetType.resource) ||
                   right.applicableOnTargetType(TargetType.account);
        else if (target instanceof Account)
            return right.applicableOnTargetType(TargetType.account);
        else
            return false;
    }
    
    /**
     * returns a List of EffectiveACL of all grants for the specified right granted 
     * on the target itself and all entries the target can inherit grants from. 
     * 
     * @param prov
     * @param target
     * @param right
     * @param attrs for RT_PSEUDO_GET_ATTRS/RT_PSEUDO_SET_ATTRS rights
     * @return
     * @throws ServiceException
     */
    static List<EffectiveACL> expandTarget(Provisioning prov, Entry target, Right right, Map<String, Object> attrs) throws ServiceException {
        
        List<EffectiveACL> result = new ArrayList<EffectiveACL>();
        
        if (target instanceof GlobalGrant)
            expandGlobalGrant(prov, target, right, attrs, result);
        else if (target instanceof Config)
            expandConfig(prov, target, right, attrs, result);
        else if (target instanceof Server)
            expandServer(prov, target, right, attrs, result);
        else if (target instanceof Cos)
            expandCos(prov, target, right, attrs, result);
        else if (target instanceof Domain)
            expandDomain(prov, target, right, attrs, result);
        else if (target instanceof DistributionList)
            expandDistributionList(prov, target, right, attrs, result);
        else if (target instanceof CalendarResource)
            expandAccount(prov, target, right, attrs, result);
        else if (target instanceof Account)
            expandAccount(prov, target, right, attrs, result);
        else
            throw ServiceException.FAILURE("internal error", null);
        
        return result;
    }
    
    
    /**
     * 
     * @param grantedOnEntryType  TargetType of the entry on which the right is granted
     * @param grantedOn           Target entry on which the right is granted
     * @param distanceToTarget    Distance to the actual target to be operated on
     * @param targetType          Target type of the actual target entry
     * @param right               the right
     * @param attrs               attrs to be operated on for get/set attrs rights
     * @param result              result to be appended to
     * @throws ServiceException
     */
    private static void processTargetEntry(TargetType grantedOnEntryType, Entry grantedOn, int distanceToTarget, 
                                           TargetType targetType,
                                           Right right, Map<String, Object> attrs, 
                                           List<EffectiveACL> result) throws ServiceException {
        List<ZimbraACE> aces;
        if (right == AdminRight.R_PSEUDO_GET_ATTRS || right == AdminRight.R_PSEUDO_SET_ATTRS)
            aces = PermUtil.getACEs(grantedOn, right, targetType, attrs.keySet());
        else
            aces = PermUtil.getACEs(grantedOn, right);
        if (aces != null && aces.size() > 0) {
            EffectiveACL effectiveAcl = new EffectiveACL(grantedOnEntryType, grantedOn, distanceToTarget, aces);
            result.add(effectiveAcl);
        }
   }
    
    private static void expandAccount(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        TargetType targeTtype = (target instanceof CalendarResource)?TargetType.resource:TargetType.account;
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(targeTtype, target, distance, targeTtype, right, attrs, result);
        
        // groups the account is directly or indirectly a member of
        List<MemberOf> groups = prov.getAclGroups((Account)target);
        int dist = distance;
        for (MemberOf group : groups) {
            DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
            dist = distance + group.getDistance();
            processTargetEntry(TargetType.distributionlist, dl, dist, targeTtype, right, attrs, result);
        }
        distance = dist;
        
        // domain of the account
        Domain domain = prov.getDomain((Account)target);
        processTargetEntry(TargetType.domain, domain, ++distance, targeTtype, right, attrs, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        processTargetEntry(TargetType.global, globalGrant, ++distance, targeTtype, right, attrs, result);
        
    }
    
    private static void expandDistributionList(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        // This path is called from AccessManager.canPerform, the target object can be a 
        // DistributionList obtained from prov.get(DistributionListBy).  
        // We require one from prov.getAclGroup(DistributionListBy) here, call getAclGroup to be sure.
        if (target instanceof DistributionList)
            target = prov.getAclGroup(DistributionListBy.id, ((DistributionList)target).getId());
        
        TargetType targetType = TargetType.distributionlist;
            
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.distributionlist, target, distance, targetType, right, attrs, result);
        
        List<MemberOf> groups = prov.getAclGroups((DistributionList)target);
        int dist = distance;
        for (MemberOf group : groups) {
            DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
            dist = distance + group.getDistance();
            processTargetEntry(TargetType.distributionlist, dl, dist, targetType, right, attrs, result);
        }
        distance = dist;
        
        // domain of the group
        Domain domain = prov.getDomain((DistributionList)target);
        processTargetEntry(TargetType.domain, domain, ++distance, targetType, right, attrs, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        processTargetEntry(TargetType.global, globalGrant, ++distance, targetType, right, attrs, result);
    }
    
    private static void expandDomain(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.domain;
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.domain, target, distance, targeType, right, attrs, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        processTargetEntry(TargetType.global, globalGrant, ++distance, targeType, right, attrs, result);
    }
    
    private static void expandCos(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.cos;
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.cos, target, distance, targeType, right, attrs, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        processTargetEntry(TargetType.global, globalGrant, ++distance, targeType, right, attrs, result);
    }
    
    private static void expandServer(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.server;
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.server, target, distance, targeType, right, attrs, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        processTargetEntry(TargetType.global, globalGrant, ++distance, targeType, right, attrs, result);
    }
    
    private static void expandConfig(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.config;
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.config, target, distance, targeType, right, attrs, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        processTargetEntry(TargetType.global, globalGrant, ++distance, targeType, right, attrs, result);
    }
    
    private static void expandGlobalGrant(Provisioning prov, Entry target, Right right, Map<String, Object> attrs, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.global;
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.global, target, distance, targeType, right, attrs, result);
    }



}

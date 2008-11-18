package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
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
import com.zimbra.cs.account.Provisioning.XMPPComponentBy;
import com.zimbra.cs.account.Provisioning.ZimletBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.RightChecker.EffectiveACL;

public enum TargetType {
    account(true),
    resource(true),
    distributionlist(true),
    domain(true),
    cos(true),
    right(true),
    server(true),
    xmppcomponent(true),
    zimlet(true),
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
        case xmppcomponent:
            targetEntry = prov.get(XMPPComponentBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(target); 
            break;    
        case zimlet:
            ZimletBy zimletBy = ZimletBy.fromString(targetBy.name());
            if (zimletBy != ZimletBy.name)
                throw ServiceException.INVALID_REQUEST("zimlet must be by name", null);
            targetEntry = prov.getZimlet(target);
            if (targetEntry == null)
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

    static boolean isRightApplicableOnTarget(Right right, Entry target) {
        
        if (target instanceof GlobalGrant)
            return true;
        else if (target instanceof Config)
            return right.applicableOnTargetType(TargetType.config);
        else if (target instanceof Zimlet)
            return right.applicableOnTargetType(TargetType.zimlet);
        else if (target instanceof XMPPComponent)
            return right.applicableOnTargetType(TargetType.xmppcomponent);
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
    
    static AttributeClass getAttributeClass(Entry target) throws ServiceException{
        if (target instanceof GlobalGrant)
            return AttributeClass.aclTarget;
        else if (target instanceof Config)
            return AttributeClass.globalConfig;
        else if (target instanceof Zimlet)
            return AttributeClass.zimletEntry;
        else if (target instanceof XMPPComponent)
            return AttributeClass.xmppComponent;
        else if (target instanceof Server)
            return AttributeClass.server;
        else if (target instanceof Cos)
            return AttributeClass.cos;
        else if (target instanceof Domain)
            return AttributeClass.domain;
        else if (target instanceof DistributionList)
            return AttributeClass.distributionList;
        else if (target instanceof CalendarResource)
            return AttributeClass.calendarResource;
        else if (target instanceof Account)
            return AttributeClass.account;
        else
            throw ServiceException.FAILURE("internal error", null);
    }
    
    
    private interface ExpandTargetVisitor {
        /**
         * @param grantedOnEntryType  TargetType of the entry on which the right is granted
         * @param grantedOn           Target entry on which the right is granted
         * @param distanceToTarget    Distance to the actual target to be operated on
         * @param targetType          Target type of the actual erspective target entry
         * @param result              result to be appended to
         * @throws ServiceException
         */
        void visit(TargetType grantedOnEntryType, Entry grantedOn, int distanceToTarget, 
                   TargetType targetType,
                   List<EffectiveACL> result) throws ServiceException;
    }
    
    private static class ExpandTargetVisitorByRight implements ExpandTargetVisitor {
        private Right mRight;
        
        ExpandTargetVisitorByRight(Right right) {
            mRight = right;
        }
        
        public void visit(TargetType grantedOnEntryType, Entry grantedOn, int distanceToTarget, 
                          TargetType targetType,
                          List<EffectiveACL> result) throws ServiceException {
            List<ZimbraACE> aces;
            if (mRight == AdminRight.R_PSEUDO_GET_ATTRS || mRight == AdminRight.R_PSEUDO_SET_ATTRS)
                aces = RightUtil.getACEs(grantedOn, mRight, targetType);
            else if (!mRight.isPresetRight())
                throw ServiceException.FAILURE("internal error", null);
            else
                aces = RightUtil.getACEs(grantedOn, mRight);
            if (aces != null && aces.size() > 0) {
                EffectiveACL effectiveAcl = new EffectiveACL(grantedOnEntryType, grantedOn, distanceToTarget, aces);
                result.add(effectiveAcl);
            }
        }
    }
    
    private static class ExpandTargetVisitorByGrantee implements ExpandTargetVisitor {
        private Account mGrantee;
        
        // set of zimbraIds of the grants we need to collect.  The set contains zimbraId
        // of the perspective account and all direct/indirect groups the account belong to.
        private Set<String> mGranteeIds;
        
        ExpandTargetVisitorByGrantee(Provisioning prov, Account grantee) throws ServiceException {
            mGrantee = grantee;
            mGranteeIds = new HashSet<String>();
            
            // add id for the perspective account
            mGranteeIds.add(grantee.getId());
            
            // add ids for all groups the perspecrtive account is a member of
            List<MemberOf> groups = prov.getAclGroups(grantee);
            for (MemberOf group : groups) {
                mGranteeIds.add(group.getId());
            }
        }
        
        public void visit(TargetType grantedOnEntryType, Entry grantedOn, int distanceToTarget, 
                         TargetType targetType,
                         List<EffectiveACL> result) throws ServiceException {
            List<ZimbraACE> aces  = RightUtil.getACEs(grantedOn, mGranteeIds, targetType);
            if (aces != null && aces.size() > 0) {
                EffectiveACL effectiveAcl = new EffectiveACL(grantedOnEntryType, grantedOn, distanceToTarget, aces);
                result.add(effectiveAcl);
            }
        }
    }
    
    /**
     * returns a List of EffectiveACL of all grants granted on the target itself and all entries 
     * the target can inherit grants from, for the specified right 
     * 
     * @param prov
     * @param target
     * @param right
     * @param attrs for RT_PSEUDO_GET_ATTRS/RT_PSEUDO_SET_ATTRS rights
     * @return
     * @throws ServiceException
     */
    static List<EffectiveACL> expandTargetByRight(Provisioning prov, Entry target, Right right) throws ServiceException {
        if (!isRightApplicableOnTarget(right, target))
            return null;
        
        ExpandTargetVisitorByRight visitor = new ExpandTargetVisitorByRight(right);
        return expandTarget(prov, target, visitor);
    }
    
    /**
     * returns a List of EffectiveACL of all grants granted on the target itself and all entries 
     * the target can inherit grants from, to the specified grantee 
     * 
     * @param prov
     * @param target
     * @param grantee
     * @return
     * @throws ServiceException
     */
    static List<EffectiveACL> expandTargetByGrantee(Provisioning prov, Entry target, Account grantee) throws ServiceException {
        ExpandTargetVisitorByGrantee visitor = new ExpandTargetVisitorByGrantee(prov, grantee);
        return expandTarget(prov, target, visitor);
    }
        
    private static List<EffectiveACL> expandTarget(Provisioning prov, Entry target, ExpandTargetVisitor visitor) throws ServiceException {
        List<EffectiveACL> result = new ArrayList<EffectiveACL>();
        
        if (target instanceof GlobalGrant)
            expandGlobalGrant(prov, target, visitor, result);
        else if (target instanceof Config)
            expandConfig(prov, target, visitor, result);
        else if (target instanceof Zimlet)
            expandZimlet(prov, target, visitor, result);
        else if (target instanceof XMPPComponent)
            expandXMPPComponent(prov, target, visitor, result);
        else if (target instanceof Server)
            expandServer(prov, target, visitor, result);
        else if (target instanceof Cos)
            expandCos(prov, target, visitor, result);
        else if (target instanceof Domain)
            expandDomain(prov, target, visitor, result);
        else if (target instanceof DistributionList)
            expandDistributionList(prov, target, visitor, result);
        else if (target instanceof CalendarResource)
            expandAccount(prov, target, visitor, result);
        else if (target instanceof Account)
            expandAccount(prov, target, visitor, result);
        else
            throw ServiceException.FAILURE("internal error", null);
        
        return result;
    }
    
    private static void expandAccount(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeTtype = (target instanceof CalendarResource)?TargetType.resource:TargetType.account;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(targeTtype, target, distance, targeTtype, result);
        
        // groups the account is directly or indirectly a member of
        List<MemberOf> groups = prov.getAclGroups((Account)target);
        int dist = distance;
        for (MemberOf group : groups) {
            DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
            dist = distance + group.getDistance();
            visitor.visit(TargetType.distributionlist, dl, dist, targeTtype, result);
        }
        distance = dist;
        
        // domain of the account
        Domain domain = prov.getDomain((Account)target);
        visitor.visit(TargetType.domain, domain, ++distance, targeTtype, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeTtype, result);
        
    }
    
    private static void expandDistributionList(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        // This path is called from AccessManager.canPerform, the target object can be a 
        // DistributionList obtained from prov.get(DistributionListBy).  
        // We require one from prov.getAclGroup(DistributionListBy) here, call getAclGroup to be sure.
        if (target instanceof DistributionList)
            target = prov.getAclGroup(DistributionListBy.id, ((DistributionList)target).getId());
        
        TargetType targetType = TargetType.distributionlist;
            
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.distributionlist, target, distance, targetType, result);
        
        List<MemberOf> groups = prov.getAclGroups((DistributionList)target);
        int dist = distance;
        for (MemberOf group : groups) {
            DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
            dist = distance + group.getDistance();
            visitor.visit(TargetType.distributionlist, dl, dist, targetType, result);
        }
        distance = dist;
        
        // domain of the group
        Domain domain = prov.getDomain((DistributionList)target);
        visitor.visit(TargetType.domain, domain, ++distance, targetType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targetType, result);
    }
    
    private static void expandDomain(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.domain;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.domain, target, distance, targeType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeType, result);
    }
    
    private static void expandCos(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.cos;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.cos, target, distance, targeType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeType, result);
    }
    
    private static void expandServer(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.server;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.server, target, distance, targeType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeType, result);
    }
    
    private static void expandXMPPComponent(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.xmppcomponent;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.xmppcomponent, target, distance, targeType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeType, result);
    }
    
    private static void expandZimlet(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.zimlet;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.zimlet, target, distance, targeType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeType, result);
    }
    
    private static void expandConfig(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.config;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.config, target, distance, targeType, result);
        
        // global grant
        Entry globalGrant = prov.getGlobalGrant();
        visitor.visit(TargetType.global, globalGrant, ++distance, targeType, result);
    }
    
    private static void expandGlobalGrant(Provisioning prov, Entry target, ExpandTargetVisitor visitor, List<EffectiveACL> result) throws ServiceException {
        TargetType targeType = TargetType.global;
        
        // get grants on the target itself
        int distance = 0;
        visitor.visit(TargetType.global, target, distance, targeType, result);
    }



}

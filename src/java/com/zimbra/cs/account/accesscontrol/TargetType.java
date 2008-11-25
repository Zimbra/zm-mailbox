package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import com.zimbra.cs.account.Provisioning.AclGroups;
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
    account(true,           AttributeClass.account),
    resource(true,          AttributeClass.calendarResource),
    distributionlist(true,  AttributeClass.distributionList),
    domain(true,            AttributeClass.domain),
    cos(true,               AttributeClass.cos),
    server(true,            AttributeClass.server),
    xmppcomponent(true,     AttributeClass.xmppComponent),
    zimlet(true,            AttributeClass.zimletEntry),
    config(false,           AttributeClass.globalConfig),
    global(false,           AttributeClass.aclTarget);
    
    private boolean mNeedsTargetIdentity;
    private AttributeClass mAttrClass;
    private Set<TargetType> mApplicableTargetTypes;
    
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
    TargetType(boolean NeedsTargetIdentity, AttributeClass attrClass) {
        mNeedsTargetIdentity = NeedsTargetIdentity;
        mAttrClass = attrClass;
    }
    
    void setApplicableTargetTypes(TargetType[] applicableTargetTypes) {
        mApplicableTargetTypes = (applicableTargetTypes==null)? null : new HashSet<TargetType>(Arrays.asList(applicableTargetTypes));
    }
    
    static void init() {
        TargetType.account.setApplicableTargetTypes(new TargetType[]{TargetType.account});
        TargetType.resource.setApplicableTargetTypes(new TargetType[]{TargetType.account, TargetType.resource});
        TargetType.distributionlist.setApplicableTargetTypes(new TargetType[]{TargetType.account, TargetType.resource, TargetType.distributionlist});
        TargetType.domain.setApplicableTargetTypes(new TargetType[]{TargetType.account, TargetType.resource, TargetType.distributionlist, TargetType.domain});
        TargetType.cos.setApplicableTargetTypes(new TargetType[]{TargetType.cos});
        TargetType.server.setApplicableTargetTypes(new TargetType[]{TargetType.server});
        TargetType.xmppcomponent.setApplicableTargetTypes(new TargetType[]{TargetType.xmppcomponent});
        TargetType.config.setApplicableTargetTypes(new TargetType[]{TargetType.config});
        TargetType.global.setApplicableTargetTypes(null);
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
    
    AttributeClass getAttributeClass() {
        return mAttrClass;
    }
    
    /**
     * returns if right is applicable on this target type(right granted on) entry
     * 
     * @param right must be a preset right
     * @return
     */
    boolean isRightApplicable(Right right) throws ServiceException {
        if (!right.isPresetRight())
            throw ServiceException.FAILURE("internal error", null);
        
        if (mApplicableTargetTypes == null)
            return true;
        else 
            return mApplicableTargetTypes.contains(right.getTargetType());
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


    
    static AttributeClass getAttributeClass(Entry target) throws ServiceException{
        
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
    
    static TargetType getTargetType(Entry target) throws ServiceException{
        
        if (target instanceof CalendarResource)
            return TargetType.resource;
        else if (target instanceof Account)
            return TargetType.account;
        else if (target instanceof Domain)
            return TargetType.domain;
        else if (target instanceof Cos)
            return TargetType.cos;
        else if (target instanceof DistributionList)
            return TargetType.distributionlist;
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
    


    /////////////////////////////////////////////////
    /////////////////////////////////////////////////
    /////////////     REMOVE BELOW   ////////////////
    /////////////////////////////////////////////////
    /////////////////////////////////////////////////
    
    static boolean isRightApplicableOnTarget_XXX(Right right, Entry target) {
        /*
         * tested in the order of how often acl are checked on each target types in normal 
         * server operation pattern.
         * 
         * CalendarResource is tested before account beore it is a subclass of Account.
         * Could remove testing for CalendarResource here because it would be the same 
         * as Account.  Leave it for now.
         *  
         */
        if (target instanceof CalendarResource)
            return right.applicableOnTargetType(TargetType.resource) ||
                   right.applicableOnTargetType(TargetType.account);
        else if (target instanceof Account)
            return right.applicableOnTargetType(TargetType.account);
        else if (target instanceof Domain)
            return right.applicableOnTargetType(TargetType.domain) ||
                   right.applicableOnTargetType(TargetType.distributionlist) ||
                   right.applicableOnTargetType(TargetType.resource) ||
                   right.applicableOnTargetType(TargetType.account);
        else if (target instanceof Cos)
            return right.applicableOnTargetType(TargetType.cos);
        else if (target instanceof DistributionList)
            return right.applicableOnTargetType(TargetType.distributionlist) ||
                   right.applicableOnTargetType(TargetType.resource) ||
                   right.applicableOnTargetType(TargetType.account);
        else if (target instanceof Server)
            return right.applicableOnTargetType(TargetType.server);
        else if (target instanceof Config)
            return right.applicableOnTargetType(TargetType.config); 
        else if (target instanceof GlobalGrant)
            return true;
        else if (target instanceof Zimlet)
            return right.applicableOnTargetType(TargetType.zimlet);
        else if (target instanceof XMPPComponent)
            return right.applicableOnTargetType(TargetType.xmppcomponent);
        else
            return false;
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
            
            // add ids for all groups the perspective account is a member of
            AclGroups groups = prov.getAclGroups(grantee);
            for (MemberOf group : groups.memberOf()) {
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
        if (!isRightApplicableOnTarget_XXX(right, target))
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
        AclGroups groups = prov.getAclGroups((Account)target);
        int dist = distance;
        for (MemberOf group : groups.memberOf()) {
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
        
        AclGroups groups = prov.getAclGroups((DistributionList)target);
        int dist = distance;
        for (MemberOf group : groups.memberOf()) {
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

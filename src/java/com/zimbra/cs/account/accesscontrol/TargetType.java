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

    /**
     * returns a List of EffectiveACL of all grants for the specified right granted 
     * on the target itself and all entries the target can inherit grants from. 
     * 
     * @param prov
     * @param target
     * @param right
     * @return
     * @throws ServiceException
     */
    static List<EffectiveACL> expandTarget(Provisioning prov, Entry target, Right right) throws ServiceException {
        
        List<EffectiveACL> result = new ArrayList<EffectiveACL>();
        TargetType targetType = right.getTargetType();
        
        switch (targetType) {
        case account:
        case resource:
            expandAccount(prov, target, right, result);
            break;
        case distributionlist:
            expandDistributionList(prov, target, right, result);
            break;
        case domain:
            expandDomain(prov, target, right, result);
            break;
        case cos:
            expandCos(prov, target, right, result);
            break;
        case right:
            throw ServiceException.FAILURE("TODO", null);
            // break;
        case server:
            expandServer(prov, target, right, result);
            break;
        case config:
            expandConfig(prov, target, right, result);
            break;
        case global:
            expandGlobalGrant(prov, target, right, result);
            break;
        }
        
        return result;
    }
    
    private static void processTargetEntry(TargetType grantedOnEntryType, Entry grantedOn, int distanceToTarget, Right right, List<EffectiveACL> result) throws ServiceException {
        List<ZimbraACE> aces = PermUtil.getACEs(grantedOn, right);
        if (aces != null && aces.size() > 0) {
            EffectiveACL effectiveAcl = new EffectiveACL(grantedOnEntryType, grantedOn, distanceToTarget, aces);
            result.add(effectiveAcl);
        }
   }
    
    private static void expandAccount(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // get grants on the target itself
        int distance = 0;
        processTargetEntry((target instanceof CalendarResource)?TargetType.resource:TargetType.account, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Account) {
            // groups the account is directly or indirectly a member of
            List<MemberOf> groups = prov.getAclGroups((Account)target);
            int dist = distance;
            for (MemberOf group : groups) {
                DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
                dist = distance + group.getDistance();
                processTargetEntry(TargetType.distributionlist, dl, dist, right, result);
            }
            distance = dist;
            
            // domain of the account
            Domain domain = prov.getDomain((Account)target);
            processTargetEntry(TargetType.domain, domain, ++distance, right, result);
            
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof DistributionList) {
            // groups the group is directly or indirectly a member of
            List<MemberOf> groups = prov.getAclGroups((DistributionList)target);
            int dist = distance;
            for (MemberOf group : groups) {
                DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
                dist = distance + group.getDistance();
                processTargetEntry(TargetType.distributionlist, dl, dist, right, result);
            }
            distance = dist;
            
            // domain of the group
            Domain domain = prov.getDomain((DistributionList)target);
            processTargetEntry(TargetType.domain, domain, ++distance, right, result);
            
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);

        } else if (target instanceof Domain) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandDistributionList(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // This path is called from AccessManager.canPerform, the target object can be a 
        // DistributionList obtained from prov.get(DistributionListBy).  
        // We require one from prov.getAclGroup(DistributionListBy) here, call getAclGroup to be sure.
        if (target instanceof DistributionList)
            target = prov.getAclGroup(DistributionListBy.id, ((DistributionList)target).getId());
        
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.distributionlist, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof DistributionList) {
            List<MemberOf> groups = prov.getAclGroups((DistributionList)target);
            int dist = distance;
            for (MemberOf group : groups) {
                DistributionList dl = prov.getAclGroup(DistributionListBy.id, group.getId());
                dist = distance + group.getDistance();
                processTargetEntry(TargetType.distributionlist, dl, dist, right, result);
            }
            distance = dist;
            
            // domain of the group
            Domain domain = prov.getDomain((DistributionList)target);
            processTargetEntry(TargetType.domain, domain, ++distance, right, result);
            
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);

        } else if (target instanceof Domain) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandDomain(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.domain, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Domain) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right: " + 
                                           "entry="+target.getLabel() + ", right=" + right.getName() + 
                                           "(right is only allowed on domain or global grant entries)",
                                           null);
    }
    
    private static void expandCos(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.cos, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Cos) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandServer(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.server, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Server) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandConfig(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.config, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Config) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(TargetType.global, globalGrant, ++distance, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandGlobalGrant(Provisioning prov, Entry target, Right right, List<EffectiveACL> result) throws ServiceException {
        // get grants on the target itself
        int distance = 0;
        processTargetEntry(TargetType.global, target, distance, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
}

package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
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
import com.zimbra.cs.account.Server;

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
    
    public static NamedEntry lookupTarget(Provisioning prov, TargetType targetType, TargetBy targetBy, String target) throws ServiceException {
        NamedEntry targetEntry = null;
        
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
            targetEntry = prov.get(DistributionListBy.fromString(targetBy.name()), target);
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
        default:
            ServiceException.INVALID_REQUEST("invallid target type for lookupTarget:" + targetType.toString(), null);
        }
    
        return targetEntry;
    }

    /**
     * returns a Map of all <{grantee-id}, GranteeType(usr or grp)> of all grants for the specified right granted 
     * on the target itself and all entries the target can inherit grants from. 
     * 
     * @param prov
     * @param target
     * @param right
     * @return
     * @throws ServiceException
     */
    static Set<ZimbraACE> expandTarget(Provisioning prov, Entry target, Right right) throws ServiceException {
        
        Set<ZimbraACE> result = new HashSet<ZimbraACE>();
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
    
    private static void processTargetEntry(Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        PermUtil.getACEs(target, right, result);
        // that all for now, maybe just have the expandXXX methods call PermUtil.getACEs directly.       
   }
    
    private static void expandAccount(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Account) {
            // groups the account is directly or indirectly a member of
            Set<String> groupIds = prov.getDistributionLists((Account)target);
            for (String groupId : groupIds) {
                DistributionList dl = prov.get(DistributionListBy.id, groupId);
                processTargetEntry(dl, right, result);
            }
            
            // domain of the account
            Domain domain = prov.getDomain((Account)target);
            processTargetEntry(domain, right, result);
            
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof DistributionList) {
            // groups the group is directly or indirectly a member of
            List<DistributionList> dls = prov.getDistributionLists((DistributionList)target, false, null);
            for (DistributionList dl : dls) {
                processTargetEntry(dl, right, result);
            }
            
            // domain of the group
            Domain domain = prov.getDomain((DistributionList)target);
            processTargetEntry(domain, right, result);
            
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);

        } else if (target instanceof Domain) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandDistributionList(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof DistributionList) {
            // groups the group is directly or indirectly a member of
            List<DistributionList> dls = prov.getDistributionLists((DistributionList)target, false, null);
            for (DistributionList dl : dls) {
                processTargetEntry(dl, right, result);
            }
            
            // domain of the group
            Domain domain = prov.getDomain((DistributionList)target);
            processTargetEntry(domain, right, result);
            
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);

        } else if (target instanceof Domain) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandDomain(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Domain) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right: " + 
                                           "entry="+target.getLabel() + ", right=" + right.getName() + 
                                           "(right is only allowed on domain or global grant entries)",
                                           null);
    }
    
    private static void expandCos(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Cos) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandServer(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Server) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandConfig(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof Config) {
            // global grant
            Entry globalGrant = prov.getGlobalGrant();
            processTargetEntry(globalGrant, right, result);
            
        } else if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
    
    private static void expandGlobalGrant(Provisioning prov, Entry target, Right right, Set<ZimbraACE> result) throws ServiceException {
        // get grants on the target itself
        processTargetEntry(target, right, result);
        
        // get grants on entries where the target can inherit from
        if (target instanceof GlobalGrant) {
            // nothing
        } else
            throw ServiceException.FAILURE("invalid target entry for right:" + "entry="+target.getLabel() + ", right=" + right.getName(), null);
    }
}

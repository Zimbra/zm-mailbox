package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.soap.ZimbraSoapContext;

public class PermUtil {
    
    /**
     * Get ACEs with specified rights
     * 
     * @param target 
     * @param rights specified rights.  If null, all ACEs in the ACL will be returned.
     * @return ACEs with right specified in rights
     * @throws ServiceException
     */
    public static Set<ZimbraACE> getACEs(Entry target, Set<Right> rights) throws ServiceException {
        ZimbraACL acl = getACL(target); 
        if (acl != null)
            return acl.getACEs(rights);
        else
            return null;
    }
    
    public static Set<ZimbraACE> grantAccess(Provisioning prov, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        for (ZimbraACE ace : aces)
            ZimbraACE.validate(ace);
        
        ZimbraACL acl = getACL(target); 
        Set<ZimbraACE> granted = null;
        
        if (acl == null) {
            acl = new ZimbraACL(aces);
            granted = acl.getACEs(null);
        } else {
            // make a copy so we don't interfere with others that are using the acl
            acl = acl.clone();
            granted = acl.grantAccess(aces); 
        }
        
        serialize(prov, target, acl);
        return granted;
    }
    
    /** Removes the right granted to the specified id.  If the right 
     *  was not previously granted to the target, no error is thrown.
     */
    public static Set<ZimbraACE> revokeAccess(Provisioning prov, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        ZimbraACL acl = getACL(target); 
        if (acl == null)
            return null;
        
        // make a copy so we don't interfere with others that are using the acl
        acl = acl.clone();
        Set<ZimbraACE> revoked = acl.revokeAccess(aces);
        serialize(prov, target, acl);
        return revoked;
    }
    
    private static void serialize(Provisioning prov, Entry target, ZimbraACL acl) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraACE, acl.serialize());
        // modifyAttrs will erase cached ACL on the target
        prov.modifyAttrs(target, attrs);
    }
    

    private static final String ACL_CACHE_KEY = "ENTRY.ACL_CACHE";
    
    static ZimbraACL getACL(Entry entry) throws ServiceException {
        ZimbraACL acl = (ZimbraACL)entry.getCachedData(ACL_CACHE_KEY);
        if (acl != null)
            return acl;
        else {
            String[] aces = entry.getMultiAttr(Provisioning.A_zimbraACE);
            if (aces.length == 0)
                return null;
            else {
                acl = new ZimbraACL(aces, RightManager.getInstance());
                entry.setCachedData(ACL_CACHE_KEY, acl);
            }
        }
        return acl;
    }

    /*
     * 
     * lookupEmailAddress, lookupGranteeByName, lookupGranteeByZimbraId borrowed from FolderAction
     * and transplanted to work with ACL in accesscontrol package.
     * 
     */
    
    // orig: FolderAction.lookupEmailAddress
    public static NamedEntry lookupEmailAddress(String name) throws ServiceException {
        NamedEntry nentry = null;
        Provisioning prov = Provisioning.getInstance();
        nentry = prov.get(AccountBy.name, name);
        if (nentry == null)
            nentry = prov.get(DistributionListBy.name, name);
        return nentry;
    }
    
    // orig: FolderAction.lookupGranteeByName
    public static NamedEntry lookupGranteeByName(String name, GranteeType type, ZimbraSoapContext zsc) throws ServiceException {
        if (type == GranteeType.GT_AUTHUSER || type == GranteeType.GT_PUBLIC || type == GranteeType.GT_GUEST || type == GranteeType.GT_KEY)
            return null;

        Provisioning prov = Provisioning.getInstance();
        // for addresses, default to the authenticated user's domain
        if ((type == GranteeType.GT_USER || type == GranteeType.GT_GROUP) && name.indexOf('@') == -1) {
            Account authacct = prov.get(AccountBy.id, zsc.getAuthtokenAccountId(), zsc.getAuthToken());
            String authname = (authacct == null ? null : authacct.getName());
            if (authacct != null)
                name += authname.substring(authname.indexOf('@'));
        }

        NamedEntry nentry = null;
        if (name != null)
            switch (type) {
                case GT_USER:    nentry = lookupEmailAddress(name);                 break;
                case GT_GROUP:   nentry = prov.get(DistributionListBy.name, name);  break;
            }

        if (nentry != null)
            return nentry;
        switch (type) {
            case GT_USER:    throw AccountServiceException.NO_SUCH_ACCOUNT(name);
            case GT_GROUP:   throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(name);
            default:  throw ServiceException.FAILURE("LDAP entry not found for " + name + " : " + type, null);
        }
    }

    // orig: FolderAction.lookupGranteeByZimbraId
    public static NamedEntry lookupGranteeByZimbraId(String zid, GranteeType type) {
        Provisioning prov = Provisioning.getInstance();
        try {
            switch (type) {
                case GT_USER:    return prov.get(AccountBy.id, zid);
                case GT_GROUP:   return prov.get(DistributionListBy.id, zid);
                case GT_GUEST:
                case GT_KEY:    
                case GT_AUTHUSER:
                case GT_PUBLIC:
                default:         return null;
            }
        } catch (ServiceException e) {
            return null;
        }
    }
    
    //
    // for admins
    //
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
            throw ServiceException.FAILURE("unsupported", null);
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
    
    //
    // for admins
    //
    public static NamedEntry lookupGrantee(Provisioning prov, GranteeType granteeType, GranteeBy granteeBy, String grantee) throws ServiceException {
        NamedEntry granteeEntry = null;
        
        switch (granteeType) {
        case GT_USER:
            granteeEntry = prov.get(AccountBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(grantee); 
            break;
        case GT_GROUP:
            granteeEntry = prov.get(DistributionListBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(grantee); 
            break;
        default:
            ServiceException.INVALID_REQUEST("invallid grantee type for lookupGrantee:" + granteeType.getCode(), null);
        }
    
        return granteeEntry;
    }

}

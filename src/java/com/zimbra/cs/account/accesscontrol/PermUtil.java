package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.soap.ZimbraSoapContext;

public class PermUtil {
    
    /**
     * Returns all ACEs granted on the entry.  
     * 
     * @param entry the entry on which rights are granted
     * @return all ACEs granted on the entry.  
     * @throws ServiceException
     */
    public static Set<ZimbraACE> getAllACEs(Entry entry) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null)
            return acl.getAllACEs();
        else
            return null;
    }
    
    /**
     * Returns a Set of ACEs with the specified rights granted on the entry.  
     * 
     * @param entry the entry on which rights are granted
     * @param rights rights of interest
     * @return a Set of ACEs with the specified rights granted on the entry.  
     * @throws ServiceException
     */
    public static Set<ZimbraACE> getACEs(Entry entry, Set<Right> rights) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null) {
            return acl.getACEs(rights);
        } else
            return null;
    }
    
    /**
     * Returns a List of ACEs with the specified right granted on the entry.  
     * Negative grants are in the front, positive grants are put in the rear of the 
     * returned List.
     * 
     * @param entry the entry on which rights are granted
     * @param right right of interest
     * @return a List of ACEs with the specified right granted on the entry. 
     * @throws ServiceException
     */
    public static List<ZimbraACE> getACEs(Entry entry, Right right) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null)
            return acl.getACEs(right);
        else
            return null;
    }
    
    public static List<ZimbraACE> getACEs(Entry entry, Right right, TargetType targetType, Set<String> attrs) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null)
            return acl.getACEs(right, targetType, attrs);
        else
            return null;
    }
    
    /**
     * Grant rights on a target entry.
     * 
     * @param prov
     * @param target
     * @param aces
     * @return
     * @throws ServiceException
     */
    public static Set<ZimbraACE> grantRight(Provisioning prov, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        for (ZimbraACE ace : aces)
            ZimbraACE.validate(ace);
        
        ZimbraACL acl = getACL(target); 
        Set<ZimbraACE> granted = null;
        
        if (acl == null) {
            acl = new ZimbraACL(aces);
            granted = acl.getAllACEs();
        } else {
            // make a copy so we don't interfere with others that are using the acl
            acl = acl.clone();
            granted = acl.grantAccess(aces); 
        }
        
        serialize(prov, target, acl);
        return granted;
    }
    
    /**
     * Revoke(remove) rights from a target entry.
     * If a right was not previously granted on the target, NO error is thrown.
     * 
     * @param prov
     * @param target
     * @param aces
     * @return a Set of grants that are actually revoked by this call
     * @throws ServiceException
     */
    public static Set<ZimbraACE> revokeRight(Provisioning prov, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        ZimbraACL acl = getACL(target); 
        if (acl == null)
            return null;
        
        // make a copy so we don't interfere with others that are using the acl
        acl = acl.clone();
        Set<ZimbraACE> revoked = acl.revokeAccess(aces);
        serialize(prov, target, acl);
        return revoked;
    }
    
    /**
     * Persists grants in LDAP
     * 
     * @param prov
     * @param entry
     * @param acl
     * @throws ServiceException
     */
    private static void serialize(Provisioning prov, Entry entry, ZimbraACL acl) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraACE, acl.serialize());
        // modifyAttrs will erase cached ACL on the target
        prov.modifyAttrs(entry, attrs);
    }
    

    private static final String ACL_CACHE_KEY = "ENTRY.ACL_CACHE";
    
    /**
     * Get cached grants, if not in cache, load from LDAP.
     * 
     * @param entry
     * @return
     * @throws ServiceException
     */
    private static ZimbraACL getACL(Entry entry) throws ServiceException {
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
     * lookupEmailAddress, lookupGranteeByName, lookupGranteeByZimbraId are borrowed from FolderAction
     * and transplanted to work with ACL in accesscontrol package for usr space account level rights.
     * 
     * The purpose is to match the existing folder grant SOAP interface, which is more flexible/liberal 
     * on identifying grantee and target.
     *   
     * These methods are *not* used for admin space ACL SOAPs. 
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


}

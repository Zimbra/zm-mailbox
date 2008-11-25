package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class RightUtil {
    
    /**
     * Returns all ACEs granted on the entry.  
     * 
     * @param entry the entry on which rights are granted
     * @return all ACEs granted on the entry.  
     * @throws ServiceException
     */
    public static List<ZimbraACE> getAllACEs(Entry entry) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null)
            return acl.getAllACEs();
        else
            return null;
    }
    
    public static Set<ZimbraACE> getAllowedACEs(Entry entry) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null)
            return acl.getAllowedACEs();
        else
            return null;
    }
    
    public static Set<ZimbraACE> getDeniedACEs(Entry entry) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null)
            return acl.getDeniedACEs();
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
    public static List<ZimbraACE> getACEs(Entry entry, Set<Right> rights) throws ServiceException {
        ZimbraACL acl = getACL(entry); 
        if (acl != null) {
            return acl.getACEs(rights);
        } else
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
    public static List<ZimbraACE> grantRight(Provisioning prov, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        for (ZimbraACE ace : aces)
            ZimbraACE.validate(ace);
        
        ZimbraACL acl = getACL(target); 
        List<ZimbraACE> granted = null;
        
        if (acl == null) {
            acl = new ZimbraACL(aces);
            granted = acl.getAllACEs();
        } else {
            // Make a copy so we don't interfere with others that are using the acl.
            // This instance of acl will never be used in any AccessManager code path.
            // It only lives within this method for serialization.
            // serialize will erase the cached ZimbraACL object on the target object.  
            // The new ACL will be loaded when it is needed.
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
    public static List<ZimbraACE> revokeRight(Provisioning prov, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        ZimbraACL acl = getACL(target); 
        if (acl == null)
            return null;
        
        // Make a copy so we don't interfere with others that are using the acl.
        // This instance of acl will never be used in any AccessManager code path.
        // It only lives within this method for serialization.
        // serialize will erase the cached ZimbraACL object on the target object.  
        // The new ACL will be loaded when it is needed.
        acl = acl.clone();
        List<ZimbraACE> revoked = acl.revokeAccess(aces);
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
    static ZimbraACL getACL(Entry entry) throws ServiceException {
        ZimbraACL acl = (ZimbraACL)entry.getCachedData(ACL_CACHE_KEY);
        if (acl != null)
            return acl;
        else {
            String[] aces = entry.getMultiAttr(Provisioning.A_zimbraACE);
            if (aces.length == 0)
                return null;
            else {
                acl = new ZimbraACL(aces, TargetType.getTargetType(entry), entry.getLabel());
                entry.setCachedData(ACL_CACHE_KEY, acl);
            }
        }
        return acl;
    }
    
}

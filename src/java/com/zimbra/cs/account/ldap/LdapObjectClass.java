package com.zimbra.cs.account.ldap;

// use LinkedHashSet to preserve the order and uniqueness of entries, not that the order/uniqueness matters
// LDAP server, just cleaner this way
import java.util.LinkedHashSet;
import java.util.Set;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;

public class LdapObjectClass {
    
    private static void addExtraObjectClasses(Set<String> ocs, LdapProvisioning prov, String extraOCAttr) throws ServiceException {
        String[] extraObjectClasses = prov.getConfig().getMultiAttr(extraOCAttr);
        for (String eoc : extraObjectClasses) {
            ocs.add(eoc);
        }
        
        /*
        if (additionalObjectClasses != null) {
            for (int i = 0; i < additionalObjectClasses.length; i++)
                ocs.add(additionalObjectClasses[i]);
        }
        */
    }
    
    public static Set<String> getAccountObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add("organizationalPerson");
        ocs.add(LdapProvisioning.C_zimbraAccount);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraAccountExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getCalendarResourceObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(LdapProvisioning.C_zimbraCalendarResource);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraCalendarResourceExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getCosObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(LdapProvisioning.C_zimbraCOS);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraCosExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getDomainObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add("dcObject");
        ocs.add("organization");
        ocs.add(LdapProvisioning.C_zimbraDomain);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraDomainExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getServerObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(LdapProvisioning.C_zimbraServer);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraServerExtraObjectClass);
        return ocs;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws ServiceException {
        LdapProvisioning prov = (LdapProvisioning)Provisioning.getInstance();
        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        for (String oc : ocs) {
            System.out.println(oc);
        }
        
    }

}

package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class LdapObjectClassHierarchy {

    private static Map<String, Set<String>> sSuperOCs = new HashMap<String, Set<String>>();
    
    private static synchronized void addToSuperOCCache(Map<String, Set<String>> addition) {
        sSuperOCs.putAll(addition);
    }
    
    private static synchronized Set<String> getFromCache(String key) {
        return sSuperOCs.get(key);
    }
    
    private static boolean checkCache(String oc1, String oc2) {
        oc1 = oc1.toLowerCase();
        oc2 = oc2.toLowerCase();
        
        Set<String> supers = getFromCache(oc1);

        if (supers == null)
            return false;
        
        if (supers.contains(oc2))
            return true;
        
        for (String superOC : supers)
            if (checkCache(superOC, oc2))
                return true;
        
        return false;
    }
    
    private static void searchOCsForSuperClasses(Map<String, Set<String>> ocs) {
        
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            DirContext schema = zlc.getSchema();
          
            Map<String, Object> attrs;
            for (Map.Entry<String, Set<String>> entry : ocs.entrySet()) {
                String oc = entry.getKey();
                Set<String> superOCs = entry.getValue();
                
                attrs = null;
                try {
                    ZimbraLog.account.debug("Looking up OC: " + oc);
                    DirContext ocSchema = (DirContext)schema.lookup("ClassDefinition/" + oc);
                    Attributes attributes = ocSchema.getAttributes("");
                    attrs = LdapUtil.getAttrs(attributes);
                } catch (NamingException e) {
                    ZimbraLog.account.debug("unable to load LDAP schema extension for objectclass: " + oc, e);
                }
                
                if (attrs == null)
                    continue;
                
                for (Map.Entry<String, Object> attr : attrs.entrySet()) {
                    String attrName = attr.getKey();
                    if ("SUP".compareToIgnoreCase(attrName) == 0) {
                         Object value = attr.getValue();
                        if (value instanceof String)
                            superOCs.add(((String)value).toLowerCase());
                        else if (value instanceof String[]) {
                            for (String v : (String[])value)
                                superOCs.add(v.toLowerCase());
                        }
                    }
                }
              
            }          

        } catch (NamingException e) {
            ZimbraLog.account.warn("unable to load LDAP schema", e);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to load LDAP schema", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    /**
     * get the most specific OC among oc1s and oc2
     * 
     * @param oc1s
     * @param oc2
     * @return
     */
    static String getMostSpecificOC(String[] oc1s, String oc2) {
        
        Map<String, Set<String>> ocsToLookFor = new HashMap<String, Set<String>>();
        
        for (String oc : oc1s) {
            String ocLower = oc.toLowerCase();
            // skip zimbra OCs
            if (ocLower.startsWith("zimbra"))
                continue;
            
            // publish in cache if not in yet
            if (getFromCache(ocLower) == null) {
                ocsToLookFor.put(ocLower, new HashSet<String>());
            }
        }
        
        // query LDAP schema if needed
        if (ocsToLookFor.size() > 0) {
            searchOCsForSuperClasses(ocsToLookFor);  // query LDAP
            addToSuperOCCache(ocsToLookFor);         // put in cache
        }
        
        String mostSpecific = oc2;
        for (String oc : oc1s) {
            if (checkCache(oc, mostSpecific))
                mostSpecific = oc;
        }
        
        return mostSpecific;
    }
    
    public static void main(String[] args) {
        System.out.println(getMostSpecificOC(new String[]{"zimbraAccount", "organizationalPerson", "person"}, "inetOrgPerson") + ", expecting inetOrgPerson");
        System.out.println(getMostSpecificOC(new String[]{"inetOrgPerson"}, "organizationalPerson")                            + ", expecting inetOrgPerson");
        System.out.println(getMostSpecificOC(new String[]{"organizationalPerson", "inetOrgPerson"}, "person")                  + ", expecting inetOrgPerson");
        System.out.println(getMostSpecificOC(new String[]{"inetOrgPerson"}, "bbb")                                             + ", expecting bbb");
        System.out.println(getMostSpecificOC(new String[]{"aaa"}, "inetOrgPerson")                                             + ", expecting inetOrgPerson");
        System.out.println(getMostSpecificOC(new String[]{"aaa"}, "inetOrgPerson")                                             + ", expecting inetOrgPerson");
        System.out.println(getMostSpecificOC(new String[]{"person", "inetOrgPerson"}, "organizationalPerson")                  + ", expecting inetOrgPerson");
    }

}

package com.zimbra.qa.unittest.prov.ldap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.LdapException.LdapSizeLimitExceededException;
import com.zimbra.cs.ldap.LdapServerConfig.GenericLdapConfig;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public class LdapExample {
    
    private GenericLdapConfig getLdapConfig() {
        String ldapUrl = "ldapi:///";
        boolean startTLSEnabled = false;
        String bindDN = "cn=config";
        String bindPassword = LC.ldap_root_password.value();
        
        return new GenericLdapConfig(ldapUrl, startTLSEnabled, bindDN, bindPassword);
    }
    
    public void getAttributes() throws Exception {
        
        GenericLdapConfig ldapConfig = getLdapConfig();
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(ldapConfig, LdapUsage.SEARCH);
            
            /*
             * get attributes zimbraId, cn and description on DN "cn=default,cn=cos,cn=zimbra"
             */
            ZAttributes attrs = zlc.getAttributes("cn=default,cn=cos,cn=zimbra", new String[]{"zimbraId", "cn", "description"});
            String zimbraId = attrs.getAttrString("zimbraId");
                        
            /*
             * get all attributes on DN "cn=default,cn=cos,cn=zimbra"
             */
            ZAttributes allAttrs = zlc.getAttributes("cn=default,cn=cos,cn=zimbra", null);
            
        } finally {
            // Note: this is important!! 
            LdapClient.closeContext(zlc);
        }
    }
    
    public void search() throws Exception {
        String base = "cn=servers,cn=zimbra";
        String filter = "(objectClass=zimbraServer)";
        String returnAttrs[] = new String[]{"objectClass", "cn"};
        
        ZLdapFilter zFilter = ZLdapFilterFactory.getInstance().fromFilterString(FilterId.ZMCONFIGD, filter);
        
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_SUBTREE, ZSearchControls.SIZE_UNLIMITED, 
                returnAttrs);
        
        GenericLdapConfig ldapConfig = getLdapConfig();
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(ldapConfig, LdapUsage.SEARCH);
            
            ZSearchResultEnumeration ne = zlc.searchDir(base, zFilter, searchControls);
            while (ne.hasMore()) {
                ZSearchResultEntry entry = ne.next();
                
                String dn = entry.getDN();
                
                ZAttributes attrs = entry.getAttributes();
                String cn = attrs.getAttrString("cn");
                String[] objectClasses = attrs.getMultiAttrString("objectClass");
                
                System.out.println("dn = " + dn);
                System.out.println("cn = " + cn);
                for (String objectClass : objectClasses) {
                    System.out.println("objetClass = " + objectClass);
                }
            }
            ne.close();
            
        } catch (LdapSizeLimitExceededException e) {
            e.printStackTrace();
            throw e;
        } finally {
            // Note: this is important!! 
            LdapClient.closeContext(zlc);
        }
    }
    
    public void createEntry() throws Exception {
        // dn of entry to create 
        String dn = "uid=user1,ou=people,dc=test,dc=com";
        
        GenericLdapConfig ldapConfig = getLdapConfig();
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(ldapConfig, LdapUsage.ADD);
            
            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.setDN(dn);
            
            entry.addAttr("objectClass", "inetOrgPerson");
            entry.addAttr("objectClass", "zimbraAccount");
            entry.addAttr("objectClass", "amavisAccount");

            entry.setAttr("uid", "user1");
            entry.setAttr("cn", "user1");
            entry.setAttr("sn", "lastname");
            entry.setAttr("zimbraAccountStatus", "active");
            entry.setAttr("zimbraId", "ba6198a3-bb49-4425-94b0-d4e9354e87b5");
            entry.addAttr("mail", "user1@trest.com");
            entry.addAttr("mail", "user-one@test.com");
            
            zlc.createEntry(entry);
        } finally {
            // Note: this is important!! 
            LdapClient.closeContext(zlc);
        }
    }
    
    public void deleteEntry() throws Exception {
        String dn = "uid=user1,ou=people,dc=test,dc=com";
        
        GenericLdapConfig ldapConfig = getLdapConfig();
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(ldapConfig, LdapUsage.DELETE);
            zlc.deleteEntry(dn);
        } finally {
            // Note: this is important!! 
            LdapClient.closeContext(zlc);
        }
    }
    
    public void modifyEntry() throws Exception {
        String dn = "cn=config,cn=zimbra";
        GenericLdapConfig ldapConfig = getLdapConfig();
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(ldapConfig, LdapUsage.MOD);
            
            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.setAttr("description", "so this gets modified");
            zlc.replaceAttributes(dn, entry.getAttributes());
        } finally {
            // Note: this is important!! 
            LdapClient.closeContext(zlc);
        }
    }
    
    public static void main(String[] args) throws Exception {
        // only needs to be called once per JVM  
        LdapClient.initialize();
        
        LdapExample test = new LdapExample();
        
        test.getAttributes();
        test.search();
        test.createEntry();
        test.deleteEntry();
        test.modifyEntry();
    }
}

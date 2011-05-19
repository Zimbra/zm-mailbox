package com.zimbra.cs.account.ldap.upgrade;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class DisableBriefcase extends LdapUpgrade {

    private static String ATTR_SPREADSHEET = Provisioning.A_zimbraFeatureBriefcaseSpreadsheetEnabled;
    private static String ATTR_SLIDES = Provisioning.A_zimbraFeatureBriefcaseSlidesEnabled;
    private static String ATTR_NOTEBOOK = Provisioning.A_zimbraFeatureNotebookEnabled;
    
    DisableBriefcase() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        LegacyZimbraLdapContext zlc = new LegacyZimbraLdapContext(true);
        try {
            doCos(zlc);
            doAccount(zlc);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private static class DisableBriefcaseVisitor extends SearchLdapVisitor {
        private LegacyZimbraLdapContext mModZlc;
        
        DisableBriefcaseVisitor(LegacyZimbraLdapContext modZlc) {
            mModZlc = modZlc;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            Attributes modAttrs = new BasicAttributes(true);
            
            try {
                if (ldapAttrs.getAttrString( ATTR_SPREADSHEET) != null)
                    modAttrs.put(ATTR_SPREADSHEET, LdapConstants.LDAP_FALSE);
                
                if (ldapAttrs.getAttrString(ATTR_SLIDES) != null)
                    modAttrs.put(ATTR_SLIDES, LdapConstants.LDAP_FALSE);
                
                if (ldapAttrs.getAttrString(ATTR_NOTEBOOK) != null)
                    modAttrs.put(ATTR_NOTEBOOK, LdapConstants.LDAP_FALSE);
                
                if (modAttrs.size() > 0) {
                    System.out.println("Modifying " + dn);
                    mModZlc.replaceAttributes(dn, modAttrs);
                }
            } catch (NamingException e) {
                // log and continue
                System.out.println("Caught NamingException while modifying " + dn);
                e.printStackTrace();
            } catch (ServiceException e) {
                // log and continue
                System.out.println("Caught ServiceException while modifying " + dn);
                e.printStackTrace();
            }
        }
    }
    
    private void upgrade(LegacyZimbraLdapContext modZlc, String bases[], String query) {
        SearchLdapOptions.SearchLdapVisitor visitor = new DisableBriefcaseVisitor(modZlc);

        String attrs[] = new String[] {ATTR_SPREADSHEET, ATTR_SLIDES, ATTR_NOTEBOOK};
        
        for (String base : bases) {
            try {
                mProv.searchLdapOnMaster(base, query, attrs, visitor);
            } catch (ServiceException e) {
                // log and continue
                System.out.println("Caught ServiceException while searching " + query + " under base " + base);
                e.printStackTrace();
            }
        }
    }
    
    private String query() {
        return "(|(" + ATTR_SPREADSHEET + "=" + LdapConstants.LDAP_TRUE + ")" + 
                 "(" + ATTR_SLIDES + "=" + LdapConstants.LDAP_TRUE + ")" + 
                 "(" + ATTR_NOTEBOOK + "=" + LdapConstants.LDAP_TRUE + ")" +
               ")";
    }
    
    private void doCos(LegacyZimbraLdapContext modZlc) {
        String bases[] = mProv.getDIT().getSearchBases(Provisioning.SD_COS_FLAG);
        String query = "(&" + LegacyLdapFilter.allCoses() + query() + ")";
        upgrade(modZlc, bases, query);
    }
    
    private void doAccount(LegacyZimbraLdapContext modZlc) {
        String bases[] = mProv.getDIT().getSearchBases(Provisioning.SA_ACCOUNT_FLAG);
        String query = "(&" + LegacyLdapFilter.allAccounts() + query() + ")";
        upgrade(modZlc, bases, query);
    }
}

package com.zimbra.cs.account.ldap.upgrade;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class Bug53745 extends LdapUpgrade {

    private static String ATTR_IMPORTEXPORT = Provisioning.A_zimbraFeatureImportExportFolderEnabled;
    private static String ATTR_IMPORT = Provisioning.A_zimbraFeatureImportFolderEnabled;
    private static String ATTR_EXPORT = Provisioning.A_zimbraFeatureExportFolderEnabled;
    
    Bug53745() throws ServiceException {
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
    
    private static class Bug53745Visitor extends SearchLdapVisitor {
        private LegacyZimbraLdapContext mModZlc;
        
        Bug53745Visitor(LegacyZimbraLdapContext modZlc) {
            mModZlc = modZlc;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            Attributes modAttrs = new BasicAttributes(true);
            
            try {
                String importExportVal = ldapAttrs.getAttrString(ATTR_IMPORTEXPORT);
                String importVal = ldapAttrs.getAttrString(ATTR_IMPORT);
                String exportVal = ldapAttrs.getAttrString(ATTR_EXPORT);
                
                if (importExportVal != null) {
                    if (importVal == null) {
                        modAttrs.put(ATTR_IMPORT, importExportVal);
                    }
                    
                    if (exportVal == null) {
                        modAttrs.put(ATTR_EXPORT, importExportVal);
                    }
                } 
                
                if (modAttrs.size() > 0) {
                    System.out.print("Modifying " + dn + ": " + modAttrs.toString());
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
        SearchLdapOptions.SearchLdapVisitor visitor = new Bug53745Visitor(modZlc);

        String attrs[] = new String[] {ATTR_IMPORTEXPORT, ATTR_IMPORT, ATTR_EXPORT};
        
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
        return "(" + ATTR_IMPORTEXPORT + "=*)";
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

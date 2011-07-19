package com.zimbra.cs.account.ldap.upgrade.legacy;

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

public class Bug50458  extends LegacyLdapUpgrade {
    
    private static final String VALUE_TO_REMOVE = "syncListener";
    
    Bug50458() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        LegacyZimbraLdapContext zlc = new LegacyZimbraLdapContext(true);
        try {
            doDomain(zlc);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
        }

    }
    
    private void doDomain(LegacyZimbraLdapContext modZlc) {
        String bases[] = mProv.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String query = "(&" + LegacyLdapFilter.allDomains() + 
            "(" + Provisioning.A_zimbraPasswordChangeListener + "=" + VALUE_TO_REMOVE + ")"+ ")";
        
        upgrade(modZlc, bases, query);
    }
    
   
    private void upgrade(LegacyZimbraLdapContext modZlc, String bases[], String query) {
        SearchLdapOptions.SearchLdapVisitor visitor = new Bug50458Visitor(modZlc);

        String attrs[] = new String[] {Provisioning.A_zimbraPasswordChangeListener};
        
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
    
    private static class Bug50458Visitor extends SearchLdapOptions.SearchLdapVisitor {
        private LegacyZimbraLdapContext mModZlc;
        
        Bug50458Visitor(LegacyZimbraLdapContext modZlc) {
            mModZlc = modZlc;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            Attributes modAttrs = new BasicAttributes(true);
            
            try {
                modAttrs.put(Provisioning.A_zimbraPasswordChangeListener, VALUE_TO_REMOVE);
                
                System.out.println("Modifying " + dn + 
                        ": removing " + Provisioning.A_zimbraPasswordChangeListener + "=" + VALUE_TO_REMOVE );
                mModZlc.removeAttributes(dn, modAttrs);

            } catch (NamingException e) {
                // log and continue
                System.out.println("Caught NamingException while modifying " + dn);
                e.printStackTrace();
            }
        }
    }
}

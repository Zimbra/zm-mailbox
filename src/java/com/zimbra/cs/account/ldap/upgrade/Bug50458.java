package com.zimbra.cs.account.ldap.upgrade;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapFilter;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.account.ldap.LdapUtil.SearchLdapVisitor;

public class Bug50458  extends LdapUpgrade {
    
    private static final String VALUE_TO_REMOVE = "syncListener";
    
    Bug50458() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            doDomain(zlc);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

    }
    
    private void doDomain(ZimbraLdapContext modZlc) {
        String bases[] = mProv.getSearchBases(Provisioning.SA_DOMAIN_FLAG);
        String query = "(&" + LdapFilter.allDomains() + 
            "(" + Provisioning.A_zimbraPasswordChangeListener + "=" + VALUE_TO_REMOVE + ")"+ ")";
        
        upgrade(modZlc, bases, query);
    }
    
   
    private void upgrade(ZimbraLdapContext modZlc, String bases[], String query) {
        SearchLdapVisitor visitor = new Bug50458Visitor(modZlc);

        String attrs[] = new String[] {Provisioning.A_zimbraPasswordChangeListener};
        
        for (String base : bases) {
            try {
                LdapUtil.searchLdapOnMaster(base, query, attrs, visitor);
            } catch (ServiceException e) {
                // log and continue
                System.out.println("Caught ServiceException while searching " + query + " under base " + base);
                e.printStackTrace();
            }
        }
    }
    
    private static class Bug50458Visitor implements SearchLdapVisitor {
        private ZimbraLdapContext mModZlc;
        
        Bug50458Visitor(ZimbraLdapContext modZlc) {
            mModZlc = modZlc;
        }
        
        public void visit(String dn, Map<String, Object> attrs, Attributes ldapAttrs) {
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

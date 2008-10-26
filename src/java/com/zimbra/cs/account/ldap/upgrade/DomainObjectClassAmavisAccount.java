package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.account.ldap.upgrade.DomainPublicServiceProtocolAndPort.DomainPuclicServiceProtocolAndPortVisitor;

public class DomainObjectClassAmavisAccount extends LdapUpgrade {

    DomainObjectClassAmavisAccount(boolean verbose) throws ServiceException {
        super(verbose);
    }
    
    static class AddDomainObjectClassAmavisAccountVisitor extends LdapUpgrade.UpgradeVisitor implements NamedEntry.Visitor {
        int mDomainsVisited;
    
        AddDomainObjectClassAmavisAccountVisitor(LdapProvisioning prov, ZimbraLdapContext zlcForMod, boolean verbose) {
            super(prov, zlcForMod, verbose);
        }
        
        public void visit(NamedEntry entry) {
            if (!(entry instanceof Domain)) {
                // should not happen
                System.out.println("Encountered non domain object: " + entry.getName() + ", skipping");
                return;
            }
            
            mDomainsVisited++;
            
            Domain domain = (Domain)entry;
            
            Map<String, Object> attrs = new HashMap<String, Object>(); 
            attrs.put("+" + Provisioning.A_objectClass, "amavisAccount");
            
            try {
                System.out.format("Updating domain %-30s: objectClass=amavisAccount\n",
                                  domain.getName());
                LdapUpgrade.modifyAttrs(domain, mZlcForMod, attrs);
            } catch (ServiceException e) {
                // log the exception and continue
                System.out.println("Caught ServiceException while modifying domain " + domain.getName());
            } catch (NamingException e) {
                // log the exception and continue
                System.out.println("Caught NamingException while modifying domain " + domain.getName());
            }
        }
        
        void reportStat() {
            System.out.println();
            System.out.println("Number of domains modified = " + mDomainsVisited);
            System.out.println();
        }
    }
    
    /**
     * bug 32557
     * 
     * Add objectClass=amavisAccount to all existing domains
     * 
     * @throws ServiceException
     */
    void doUpgrade() throws ServiceException {
        
        String query = "(&(objectClass=zimbraDomain)(!(objectClass=amavisAccount)))";
        String bases[] = mProv.getSearchBases(Provisioning.SA_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_objectClass,
                                       Provisioning.A_zimbraId,
                                       Provisioning.A_zimbraDomainName};
                
        ZimbraLdapContext zlc = null; 
        AddDomainObjectClassAmavisAccountVisitor visitor = null;
        
        try {
            zlc = new ZimbraLdapContext(true);
            
            visitor = new AddDomainObjectClassAmavisAccountVisitor(mProv, zlc,  mVerbose);
            
            for (String base : bases) {
                // should really have one base, but iterate thought the arrya anyway
                if (mVerbose) {
                    System.out.println("LDAP search base: " + base);
                    System.out.println("LDAP search query: " + query);
                    System.out.println();
                }
                
                mProv.searchObjects(query, attrs, base,
                                    Provisioning.SO_NO_FIXUP_OBJECTCLASS | Provisioning.SO_NO_FIXUP_RETURNATTRS, // turn off fixup for objectclass and return attrs
                                    visitor, 
                                    0,      // return all entries that satisfy filter.
                                    false,  // do not use connection pool, for the OpenLdap bug (see bug 24168) might still be there
                                    true);  // use LDAP master
             
            }
        } finally {
            ZimbraLdapContext.closeContext(zlc);
            if (visitor != null)
                visitor.reportStat();
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}

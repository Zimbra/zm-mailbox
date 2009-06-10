package com.zimbra.cs.account.ldap.upgrade;

import java.io.IOException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class SetZimbraPrefFromDisplay extends LdapUpgrade {    
    
    SetZimbraPrefFromDisplay() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        // TODO Auto-generated method stub
        
        LdapDIT dit = mProv.getDIT();
        String base;
        String query;
        String returnAttrs[] = new String[] {Provisioning.A_objectClass,
                                             Provisioning.A_cn,
                                             Provisioning.A_uid,
                                             Provisioning.A_displayName,
                                             Provisioning.A_zimbraPrefFromDisplay};
        
        base = dit.mailBranchBaseDN();
        query = "(&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource)))";
        
        int maxResults = 0; // no limit
        ZimbraLdapContext zlc = null; 
        ZimbraLdapContext modZlc = null;
        int numModified = 0;
        
        try {
            zlc = new ZimbraLdapContext(true, false);  // use master, do not use connection pool
            modZlc = new ZimbraLdapContext(true);
            
            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, returnAttrs, false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
            byte[] cookie = null;

            NamingEnumeration ne = null;

            
            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);

                    ne = zlc.searchDir(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        SearchResult sr = (SearchResult) ne.nextElement();
                        String dn = sr.getNameInNamespace();

                        Attributes attrs = sr.getAttributes();
                        
                        String zpfd = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraPrefFromDisplay);
                        
                        if (zpfd == null) {
                            String displayName = LdapUtil.getAttrString(attrs, Provisioning.A_displayName);
                            String cn = LdapUtil.getAttrString(attrs, Provisioning.A_cn);
                            String uid = LdapUtil.getAttrString(attrs, Provisioning.A_uid);
                        
                            String display = displayName;
                            if (display == null)
                                display = cn;
                            // catch the case where no real name was present and so cn was defaulted to the username
                            if (display == null || display.trim().equals("") || display.equals(uid))
                                display = null;

                            if (display != null) {
                                System.out.println("Setting " + Provisioning.A_zimbraPrefFromDisplay + 
                                        " on dn [" + dn + "] to [" + display + "]");
                                
                                Attributes modAttrs = new BasicAttributes(true);
                                modAttrs.put(Provisioning.A_zimbraPrefFromDisplay, display);
                                modZlc.replaceAttributes(dn, modAttrs);
                                
                                numModified++;
                            }
                        }
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
            ZimbraLdapContext.closeContext(modZlc);
            
            System.out.println("\nModified " + numModified + " objects");
        }
    }

}

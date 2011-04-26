package com.zimbra.cs.prov.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;

/**
 * An SDK-neutral LdapHelper.  Based on Z* classes and LdapUtil in the com.zimbra.cs.ldap package.
 * 
 * @author pshao
 *
 */
public class ZLdapHelper extends LdapHelper {
    
    ZLdapHelper(LdapProv ldapProv) {
        super(ldapProv);
    }

    @Override
    public void searchLdap(ILdapContext ldapContext, SearchLdapOptions searchOptions) 
    throws ServiceException {
        
        ZLdapContext zlc = LdapClient.toZLdapContext(getProv(), ldapContext);
        zlc.searchPaged(searchOptions);
    }

}

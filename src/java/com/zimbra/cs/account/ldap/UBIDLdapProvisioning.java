package com.zimbra.cs.account.ldap;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO.SDKTODO;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapContext;


public class UBIDLdapProvisioning extends LdapProvisioning {

    @Override
    @SDKTODO
    protected void modifyLdapAttrs(Entry entry, ILdapContext initZlc, Map<String, ? extends Object> attrs)
            throws ServiceException {
        ZLdapContext zlc = LdapClient.toZLdapContext(this, initZlc);
        try {
            if (zlc == null) {
                zlc = LdapClient.getContext(LdapServerType.MASTER);
            }
            
            LdapUtil.modifyAttrs(zlc, ((LdapEntry)entry).getDN(), attrs, entry);
            refreshEntry(entry, zlc, this);
        } catch (LdapException e) {
            throw ServiceException.FAILURE("unable to modify attrs: "
                    + e.getMessage(), e);
        } finally {
            if (initZlc == null)
                LdapClient.closeContext(zlc);
        }
    }
}

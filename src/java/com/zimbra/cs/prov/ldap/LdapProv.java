package com.zimbra.cs.prov.ldap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.mime.MimeTypeInfo;

public abstract class LdapProv extends Provisioning {
    
    protected LdapDIT mDIT;
    
    protected void setDIT() {
        mDIT = new LdapDIT(this);
    }

    public LdapDIT getDIT() {
        return mDIT;
    }
    
    public abstract void searchOCsForSuperClasses(Map<String, Set<String>> ocs);
    
    public abstract List<MimeTypeInfo> getAllMimeTypesByQuery() throws ServiceException;
    public abstract List<MimeTypeInfo> getMimeTypesByQuery(String mimeType) throws ServiceException;

}

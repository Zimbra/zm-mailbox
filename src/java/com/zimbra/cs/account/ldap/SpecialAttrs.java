package com.zimbra.cs.account.ldap;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;

public class SpecialAttrs {
    
    // special Zimbra attrs
    private static final String SA_zimbraId  = Provisioning.A_zimbraId;
    
    // pseudo attrs
    private static final String PA_ldapBase    = "ldap.baseDn";
    private static final String PA_ldapRdnAttr = "ldap.rdnAttr";
    
    private String mZimbraId;
    private String mLdapBaseDn;
    private String mLdapRdnAttr;
    
    protected Map<String, Object> mAttrs;
    
    public SpecialAttrs(Map<String, Object> attrs) {
        mAttrs = attrs;
    }
    
    public String getZimbraId()     { return mZimbraId; }
    public String getLdapBaseDn()   { return mLdapBaseDn; }
    public String getLdapRdnAttr()  { return mLdapRdnAttr; }
    
    protected String getSingleValuedAttr(Map<String, Object> attrs, String attr) throws ServiceException {
        Object value = attrs.get(attr);
        if (value == null)
            return null;
        
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(attr + " is a single-valued attribute", null);
        else
            return (String)value;
    }
    
    protected void processZimbraId() throws ServiceException  {
        String zimbraId = getSingleValuedAttr(mAttrs, SA_zimbraId);
        
        if (zimbraId != null) {
            // present, validate if it is a valid uuid
            try {
                if (!LdapUtil.isValidUUID(zimbraId))
                throw ServiceException.INVALID_REQUEST(zimbraId + " is not a valid UUID", null);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(zimbraId + " is not a valid UUID", e);
            }
        
            /* check for uniqueness of the zimbraId
            * 
            * for now we go with GIGO (garbage in, garbage out) and not check, since there is a race condition 
            * that an entry is added after our check.
            * There is a way to do the uniqueness check in OpenLDAP with an overlay, we will address the uniqueness
            * when we do that.
            */
            /*
            if (getAccountById(uuid) != null)
                throw AccountServiceException.ACCOUNT_EXISTS(emailAddress);
            */
        
            // remove it from the attr list
            mAttrs.remove(Provisioning.A_zimbraId);
        }
        mZimbraId = zimbraId;
    }
        
    protected void processLdapBaseDn() throws ServiceException {
        // default is don't support it, do nothing here
        // if the pseudo attr is present, a NamingExeption will be thrown when the entry is being created
    }
    
    protected void processLdapRdnAttr() throws ServiceException {
        // default is don't support it, do nothing here
        // if the pseudo attr is present, a NamingExeption will be thrown when the entry is being created
    }
    
    public void process() throws ServiceException {
        if (mAttrs != null) {
            processZimbraId();
            processLdapBaseDn();
            processLdapRdnAttr();
        }
    }
}
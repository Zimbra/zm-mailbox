package com.zimbra.cs.account.ldap;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class AuthMechanism {
    
    public static enum AuthMechType {
        AMT_ZIMBRA,
        AMT_LDAP,
        AMT_CUSTOM
    }
    
    private String mAuthMech;  // value of the zimbraAuthMech attribute
    private AuthMechType mType;
    
    /*
     * For AMT_ZIMBRA and AMT_LDAP, the same value as mAuthMech
     * For AMT_CUSTOM, the handler name afte the : in mAuthMech
     */ 
    private String mAuthMethod; 
    
    AuthMechanism(Account acct) throws ServiceException {
        
        mAuthMech = Provisioning.AM_ZIMBRA;   
   
        Provisioning prov = Provisioning.getInstance();
        Domain d = prov.getDomain(acct);
        // see if it specifies an alternate auth
        if (d != null) {
            String am = d.getAttr(Provisioning.A_zimbraAuthMech);
            if (am != null)
                mAuthMech = am;
        }
        
        if (mAuthMech.equals(Provisioning.AM_ZIMBRA)) {
            mType = AuthMechType.AMT_ZIMBRA;
            mAuthMethod = mAuthMech;
        } else if (mAuthMech.equals(Provisioning.AM_LDAP) || mAuthMech.equals(Provisioning.AM_AD)) {
            mType = AuthMechType.AMT_LDAP;
            mAuthMethod = mAuthMech;
        } else if (mAuthMech.startsWith("custom:")) {
            mType = AuthMechType.AMT_CUSTOM;
            mAuthMethod = mAuthMech; // TODO parse for the method
        } else {
            ZimbraLog.account.warn("unknown value for "+Provisioning.A_zimbraAuthMech+": "+
                                   mAuthMech+", falling back to default mech");
            // fallback to zimbra
            mAuthMech = Provisioning.AM_ZIMBRA;
            mType = AuthMechType.AMT_ZIMBRA;
            mAuthMethod = mAuthMech;
        }
    }
    
    public AuthMechType getType() {
        return mType;
    }
    
    public String getMethod() {
        return mAuthMethod;
    }
    
    public boolean isZimbraAuth() {
        return (mType == AuthMechType.AMT_ZIMBRA);
    }
    
}

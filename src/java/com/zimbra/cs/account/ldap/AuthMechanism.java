/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

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
    private String mAuthHandler; 
    
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
            mAuthHandler = mAuthMech;
            
        } else if (mAuthMech.equals(Provisioning.AM_LDAP) || mAuthMech.equals(Provisioning.AM_AD)) {
            mType = AuthMechType.AMT_LDAP;
            mAuthHandler = mAuthMech;
            
        } else if (mAuthMech.startsWith(Provisioning.AM_CUSTOM)) {
            // the value is in the format of custom:{handler}
            int idx = mAuthMech.indexOf(':');
            if (idx != -1) {
                mType = AuthMechType.AMT_CUSTOM;
                mAuthHandler = mAuthMech.substring(idx+1);
            }
        }
     
        if (mType == null) {
            // we didn't have a valid value, fallback to zimbra default
            ZimbraLog.account.warn("unknown value for "+Provisioning.A_zimbraAuthMech+": "+
                                   mAuthMech+", falling back to default mech");

            mAuthMech = Provisioning.AM_ZIMBRA;
            mType = AuthMechType.AMT_ZIMBRA;
            mAuthHandler = mAuthMech;
        }
    }
    
    public AuthMechType getType() {
        return mType;
    }
    
    public String getHandler() {
        return mAuthHandler;
    }
    
    public boolean isZimbraAuth() {
        return (mType == AuthMechType.AMT_ZIMBRA);
    }
    
}

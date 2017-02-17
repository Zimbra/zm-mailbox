/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapUtil;

public class SpecialAttrs {
    
    // special Zimbra attrs
    public static final String SA_zimbraId  = Provisioning.A_zimbraId;
    
    // pseudo attrs
    public static final String PA_ldapBase    = "ldap.baseDN";
    
    private String mZimbraId;
    private String mLdapBaseDn;
    
    public String getZimbraId()     { return mZimbraId; }
    public String getLdapBaseDn()   { return mLdapBaseDn; }
    
    public static String getSingleValuedAttr(Map<String, Object> attrs, String attr) throws ServiceException {
        Object value = attrs.get(attr);
        if (value == null)
            return null;
        
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(attr + " is a single-valued attribute", null);
        else
            return (String)value;
    }
    
    public void handleZimbraId(Map<String, Object> attrs) throws ServiceException  {
        String zimbraId = getSingleValuedAttr(attrs, SA_zimbraId);
        
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
            attrs.remove(SA_zimbraId);
            mZimbraId = zimbraId;
        }
    }
        
    public void handleLdapBaseDn(Map<String, Object> attrs) throws ServiceException {
        String baseDn = getSingleValuedAttr(attrs, PA_ldapBase);
        if (baseDn != null) {
            attrs.remove(PA_ldapBase);
            mLdapBaseDn = baseDn;
        }
    }

}
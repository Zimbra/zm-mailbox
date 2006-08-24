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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
 class LdapAccount extends LdapNamedEntry implements Account {

    private String mName;
    private String mDomainName;
    
    LdapAccount(String dn, Attributes attrs, Map<String, Object> defaults) throws NamingException {
        super(dn, attrs, defaults);
        initNameAndDomain();
    }

    public String getId() {
        return super.getAttr(Provisioning.A_zimbraId);
    }

    public String getUid() {
        return super.getAttr(Provisioning.A_uid);
    }
    
    public String getName() {
        return mName;
    }

    void initDefaults(Map<String,Object> defaults) {
        setDefaults(defaults);
    }
    
    /**`<
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName() {
        return mDomainName;
    }

    private void initNameAndDomain() {
        String uid = getUid(); 
        mDomainName = LdapUtil.dnToDomain(mDn);
        if (!mDomainName.equals("")) {
            mName =  uid+"@"+mDomainName;
        } else {
            mName = uid;
            mDomainName = null;
        }
    }

    public String getAccountStatus() {
        return super.getAttr(Provisioning.A_zimbraAccountStatus);
    }
    
    public String[] getAliases() {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }
    
    public String getAccountCOSId() {
        return super.getAttr(Provisioning.A_zimbraCOSId);
    }

    private ICalTimeZone mTimeZone;

    public synchronized ICalTimeZone getTimeZone() throws ServiceException {
        String tzId = getAttr(Provisioning.A_zimbraPrefTimeZoneId);
        if (tzId == null) {
        	if (mTimeZone != null)
                return mTimeZone;
            mTimeZone = ICalTimeZone.getUTC();
            return mTimeZone;
        }

        if (mTimeZone != null) {
            if (mTimeZone.getID().equals(tzId))
                return mTimeZone;
            // Else the account's time zone was updated.  Discard the cached
            // ICalTimeZone object.
        }

    	WellKnownTimeZone z = Provisioning.getInstance().getTimeZoneById(tzId);
        if (z != null)
            mTimeZone = z.toTimeZone();
        if (mTimeZone == null)
            mTimeZone = ICalTimeZone.getUTC();
        return mTimeZone;
    }

    public CalendarUserType getCalendarUserType() {
        String cutype = getAttr(Provisioning.A_zimbraAccountCalendarUserType,
                                CalendarUserType.USER.toString());
        return CalendarUserType.valueOf(cutype);
    }

    public boolean saveToSent() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);
    }
}

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

package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class Account extends NamedEntry {

    private String mDomain;
    
    public Account(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, defaults);
        int index = name.indexOf('@');
        if (index != -1) mDomain = name.substring(index+1);
    }

    public static enum CalendarUserType {
        USER,       // regular person account
        RESOURCE    // calendar resource
    }

    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName() {
        return mDomain;
    }

    /**
     * Returns calendar user type
     * @return USER (default) or RESOURCE
     * @throws ServiceException
     */
    public CalendarUserType getCalendarUserType() {
        String cutype = getAttr(Provisioning.A_zimbraAccountCalendarUserType,
                CalendarUserType.USER.toString());
        return CalendarUserType.valueOf(cutype);
    }

    public String getUid() {
        return super.getAttr(Provisioning.A_uid);
    }

    public boolean saveToSent() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);
    }
    
    public String getAccountStatus() {
        return super.getAttr(Provisioning.A_zimbraAccountStatus);
    }
    
    public String[] getAliases() {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }

    /**
     * Returns the *account's* COSId, that is, returns the zimbraCOSId directly set on the account, or null if not set.
     * Use Provisioning.getCos(account) to get the actual COS object.
     * @return 
     */
    public String getAccountCOSId() {
        return getAttr(Provisioning.A_zimbraCOSId);
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
}

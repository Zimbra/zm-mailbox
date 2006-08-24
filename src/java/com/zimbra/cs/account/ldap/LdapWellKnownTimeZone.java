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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 7. 11.
 */
package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;

/**
 * @author jhahm
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class LdapWellKnownTimeZone extends LdapNamedEntry implements WellKnownTimeZone {

    private static Log mLog = LogFactory.getLog(LdapWellKnownTimeZone.class);

    LdapWellKnownTimeZone(String dn, Attributes attrs) throws NamingException {
        super(dn, attrs, null);
    }

    public String getName() {
        return getAttr(Provisioning.A_cn);
    }

    public String getId() {
        return getAttr(Provisioning.A_cn);
    }

    private ICalTimeZone mTimeZone;

    public synchronized ICalTimeZone toTimeZone() {
        if (mTimeZone != null)
            return mTimeZone;

        String tzId = getId();
        try {
        	int standardOffset = getStandardOffsetMins();
            int daylightOffset = getDaylightOffsetMins();
            mTimeZone = new ICalTimeZone(tzId,
                                         standardOffset * 60 * 1000,
                                         getStandardDtStart(),
                                         getStandardRecurrenceRule(),
                                         daylightOffset * 60 * 1000,
                                         getDaylightDtStart(),
                                         getDaylightRecurrenceRule());
        } catch (Exception e) {
            mLog.error("Invalid time zone entry: " + tzId, e);
            mTimeZone = new ICalTimeZone(tzId,
                                         0,
                                         "16010101T000000",
                                         null,
                                         0,
                                         "16010101T000000",
                                         null);
        }
        return mTimeZone;
    }

    public String getStandardDtStart() {
        return getAttr(Provisioning.A_zimbraTimeZoneStandardDtStart);
    }

    public String getStandardOffset() {
        return getAttr(Provisioning.A_zimbraTimeZoneStandardOffset);
    }

    boolean mStandardOffsetMinsCached = false;
    int mStandardOffsetMins;

    private synchronized int getStandardOffsetMins() {
        if (!mStandardOffsetMinsCached) {
            mStandardOffsetMins = offsetToInt(getStandardOffset());
            mStandardOffsetMinsCached = true;
        }
        return mStandardOffsetMins;
    }

    public String getStandardRecurrenceRule() {
        return getAttr(Provisioning.A_zimbraTimeZoneStandardRRule);
    }

    public String getDaylightDtStart() {
        return getAttr(Provisioning.A_zimbraTimeZoneDaylightDtStart);
    }

    public String getDaylightOffset() {
        return getAttr(Provisioning.A_zimbraTimeZoneDaylightOffset);
    }

    boolean mDaylightOffsetMinsCached = false;
    int mDaylightOffsetMins;

    private synchronized int getDaylightOffsetMins() {
        if (!mDaylightOffsetMinsCached) {
            mDaylightOffsetMins = offsetToInt(getDaylightOffset());
            mDaylightOffsetMinsCached = true;
        }
        return mDaylightOffsetMins;
    }

    public String getDaylightRecurrenceRule() {
        return getAttr(Provisioning.A_zimbraTimeZoneDaylightRRule);
    }

    /**
     * First sort by the offset from GMT in minutes, then alphabetically
     * by substring of the time zone name following the "(GMT+/-HHMM) " prefix.
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapWellKnownTimeZone))
            return 0;
        LdapWellKnownTimeZone other = (LdapWellKnownTimeZone) obj;

        int thisOffset = getStandardOffsetMins();
        int otherOffset = other.getStandardOffsetMins();
        if (thisOffset < otherOffset)
            return -1;
        else if (thisOffset > otherOffset)
            return 1;

        String thisId = getId();
        if (thisId.indexOf("(GMT") == 0)
            thisId = thisId.substring(thisId.indexOf(')') + 1);
        String otherId = other.getId();
        if (otherId.indexOf("(GMT") == 0)
            otherId = otherId.substring(otherId.indexOf(')') + 1);
        return thisId.compareTo(otherId);
    }

    private static int offsetToInt(String offset) {
        try {
        	boolean negative = offset.charAt(0) == '-';
            int hour = Integer.parseInt(offset.substring(1, 3));
            int min = Integer.parseInt(offset.substring(3, 5));
            int offsetMins = hour * 60 + min;
            if (negative)
                offsetMins *= -1;
            return offsetMins;
        } catch (StringIndexOutOfBoundsException se) {
        	return 0;
        } catch (NumberFormatException ne) {
        	return 0;
        }
    }
}

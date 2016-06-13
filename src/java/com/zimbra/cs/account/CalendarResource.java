/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import java.util.Map;

/**
 * @author jhahm
 */
public class CalendarResource extends ZAttrCalendarResource {

    public CalendarResource(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }
    
    @Override
    public EntryType getEntryType() {
        return EntryType.CALRESOURCE;
    }

    public String getResourceType() {
        return getAttr(Provisioning.A_zimbraCalResType, "Location");
    }

    public boolean autoAcceptDecline() {
        return getBooleanAttr(
                Provisioning.A_zimbraCalResAutoAcceptDecline, true);
    }

    public boolean autoDeclineIfBusy() {
        return getBooleanAttr(
                Provisioning.A_zimbraCalResAutoDeclineIfBusy, true);
    }

    public boolean autoDeclineRecurring() {
        return getBooleanAttr(
                Provisioning.A_zimbraCalResAutoDeclineRecurring, false);
    }

    public int getMaxNumConflictsAllowed() {
        return getIntAttr(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, 0);
    }

    public int getMaxPercentConflictsAllowed() {
        return getIntAttr(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, 0);
    }

    public String getLocationDisplayName() {
        return getAttr(Provisioning.A_zimbraCalResLocationDisplayName);
    }

    public String getSite() {
        return getAttr(Provisioning.A_zimbraCalResSite);
    }

    public String getBuilding() {
        return getAttr(Provisioning.A_zimbraCalResBuilding);
    }

    public String getFloor() {
        return getAttr(Provisioning.A_zimbraCalResFloor);
    }

    public String getRoom() {
        return getAttr(Provisioning.A_zimbraCalResRoom);
    }

    public int getCapacity() {
        return getIntAttr(Provisioning.A_zimbraCalResCapacity, 0);
    }

    public String getContactName() {
        return getAttr(Provisioning.A_zimbraCalResContactName);
    }

    public String getContactEmail(){
        return getAttr(Provisioning.A_zimbraCalResContactEmail);
    }

    public String getContactPhone(){
        return getAttr(Provisioning.A_zimbraCalResContactPhone);
    }    
    
}

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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;

/**
 * @author jhahm
 */
public class LdapCalendarResource
extends LdapAccount implements CalendarResource {

    LdapCalendarResource(LdapAccount account) {
        super(account.mDn, account.mAttrs, account.mProv);
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

    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName);
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

/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Map;

/**
 * @author jhahm
 */
public class CalendarResource extends Account {

    public CalendarResource(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, defaults);
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

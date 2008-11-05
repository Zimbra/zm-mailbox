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
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public class ZAttrCalendarResource extends Account {

    public ZAttrCalendarResource(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, defaults);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 schemers 20081104-1827 */

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @return displayName, or null unset
     */
    @ZAttr(id=-1)
    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @return zimbraAccountCalendarUserType, or null unset
     */
    @ZAttr(id=313)
    public String getAccountCalendarUserType() {
        return getAttr(Provisioning.A_zimbraAccountCalendarUserType);
    }

    /**
     * Whether this calendar resource accepts/declines meeting invites
     * automatically; default TRUE
     *
     * @return zimbraCalResAutoAcceptDecline, or false if unset
     */
    @ZAttr(id=315)
    public boolean isCalResAutoAcceptDecline() {
        return getBooleanAttr(Provisioning.A_zimbraCalResAutoAcceptDecline, false);
    }

    /**
     * Whether this calendar resource declines invite if already busy;
     * default TRUE
     *
     * @return zimbraCalResAutoDeclineIfBusy, or false if unset
     */
    @ZAttr(id=322)
    public boolean isCalResAutoDeclineIfBusy() {
        return getBooleanAttr(Provisioning.A_zimbraCalResAutoDeclineIfBusy, false);
    }

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FASE
     *
     * @return zimbraCalResAutoDeclineRecurring, or false if unset
     */
    @ZAttr(id=323)
    public boolean isCalResAutoDeclineRecurring() {
        return getBooleanAttr(Provisioning.A_zimbraCalResAutoDeclineRecurring, false);
    }

    /**
     * building number or name
     *
     * @return zimbraCalResBuilding, or null unset
     */
    @ZAttr(id=327)
    public String getCalResBuilding() {
        return getAttr(Provisioning.A_zimbraCalResBuilding);
    }

    /**
     * capacity
     *
     * @return zimbraCalResCapacity, or -1 if unset
     */
    @ZAttr(id=330)
    public int getCalResCapacity() {
        return getIntAttr(Provisioning.A_zimbraCalResCapacity, -1);
    }

    /**
     * email of contact in charge of resource
     *
     * @return zimbraCalResContactEmail, or null unset
     */
    @ZAttr(id=332)
    public String getCalResContactEmail() {
        return getAttr(Provisioning.A_zimbraCalResContactEmail);
    }

    /**
     * name of contact in charge of resource
     *
     * @return zimbraCalResContactName, or null unset
     */
    @ZAttr(id=331)
    public String getCalResContactName() {
        return getAttr(Provisioning.A_zimbraCalResContactName);
    }

    /**
     * phone number of contact in charge of resource
     *
     * @return zimbraCalResContactPhone, or null unset
     */
    @ZAttr(id=333)
    public String getCalResContactPhone() {
        return getAttr(Provisioning.A_zimbraCalResContactPhone);
    }

    /**
     * floor number or name
     *
     * @return zimbraCalResFloor, or null unset
     */
    @ZAttr(id=328)
    public String getCalResFloor() {
        return getAttr(Provisioning.A_zimbraCalResFloor);
    }

    /**
     * display name for resource location
     *
     * @return zimbraCalResLocationDisplayName, or null unset
     */
    @ZAttr(id=324)
    public String getCalResLocationDisplayName() {
        return getAttr(Provisioning.A_zimbraCalResLocationDisplayName);
    }

    /**
     * room number or name
     *
     * @return zimbraCalResRoom, or null unset
     */
    @ZAttr(id=329)
    public String getCalResRoom() {
        return getAttr(Provisioning.A_zimbraCalResRoom);
    }

    /**
     * site name
     *
     * @return zimbraCalResSite, or null unset
     */
    @ZAttr(id=326)
    public String getCalResSite() {
        return getAttr(Provisioning.A_zimbraCalResSite);
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Equipment, Location]
     *
     * @return zimbraCalResType, or null unset
     */
    @ZAttr(id=314)
    public String getCalResType() {
        return getAttr(Provisioning.A_zimbraCalResType);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @return zimbraLocale, or null unset
     */
    @ZAttr(id=345)
    public String getLocaleAsString() {
        return getAttr(Provisioning.A_zimbraLocale);
    }

    ///// END-AUTO-GEN-REPLACE
}

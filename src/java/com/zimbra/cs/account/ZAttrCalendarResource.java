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

import java.util.HashMap;
import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public class ZAttrCalendarResource extends Account {

    public ZAttrCalendarResource(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20081117-1433 */

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
     * RFC2798: preferred name to be used when displaying entries
     *
     * @param displayName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setDisplayName(String displayName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, displayName);
        return attrs;
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetDisplayName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, "");
        return attrs;
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @return zimbraAccountCalendarUserType, or null if unset and/or has invalid value
     */
    @ZAttr(id=313)
    public ZAttrProvisioning.AccountCalendarUserType getAccountCalendarUserType() {
        try { String v = getAttr(Provisioning.A_zimbraAccountCalendarUserType); return v == null ? null : ZAttrProvisioning.AccountCalendarUserType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @return zimbraAccountCalendarUserType, or null unset
     */
    @ZAttr(id=313)
    public String getAccountCalendarUserTypeAsString() {
        return getAttr(Provisioning.A_zimbraAccountCalendarUserType);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @param zimbraAccountCalendarUserType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=313)
    public Map<String,Object> setAccountCalendarUserType(ZAttrProvisioning.AccountCalendarUserType zimbraAccountCalendarUserType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, zimbraAccountCalendarUserType.toString());
        return attrs;
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @param zimbraAccountCalendarUserType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=313)
    public Map<String,Object> setAccountCalendarUserTypeAsString(String zimbraAccountCalendarUserType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, zimbraAccountCalendarUserType);
        return attrs;
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=313)
    public Map<String,Object> unsetAccountCalendarUserType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, "");
        return attrs;
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
     * Whether this calendar resource accepts/declines meeting invites
     * automatically; default TRUE
     *
     * @param zimbraCalResAutoAcceptDecline new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=315)
    public Map<String,Object> setCalResAutoAcceptDecline(boolean zimbraCalResAutoAcceptDecline, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoAcceptDecline, Boolean.toString(zimbraCalResAutoAcceptDecline));
        return attrs;
    }

    /**
     * Whether this calendar resource accepts/declines meeting invites
     * automatically; default TRUE
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=315)
    public Map<String,Object> unsetCalResAutoAcceptDecline(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoAcceptDecline, "");
        return attrs;
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
     * Whether this calendar resource declines invite if already busy;
     * default TRUE
     *
     * @param zimbraCalResAutoDeclineIfBusy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=322)
    public Map<String,Object> setCalResAutoDeclineIfBusy(boolean zimbraCalResAutoDeclineIfBusy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineIfBusy, Boolean.toString(zimbraCalResAutoDeclineIfBusy));
        return attrs;
    }

    /**
     * Whether this calendar resource declines invite if already busy;
     * default TRUE
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=322)
    public Map<String,Object> unsetCalResAutoDeclineIfBusy(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineIfBusy, "");
        return attrs;
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
     * Whether this calendar resource declines invites to recurring
     * appointments; default FASE
     *
     * @param zimbraCalResAutoDeclineRecurring new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=323)
    public Map<String,Object> setCalResAutoDeclineRecurring(boolean zimbraCalResAutoDeclineRecurring, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineRecurring, Boolean.toString(zimbraCalResAutoDeclineRecurring));
        return attrs;
    }

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FASE
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=323)
    public Map<String,Object> unsetCalResAutoDeclineRecurring(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineRecurring, "");
        return attrs;
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
     * building number or name
     *
     * @param zimbraCalResBuilding new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=327)
    public Map<String,Object> setCalResBuilding(String zimbraCalResBuilding, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResBuilding, zimbraCalResBuilding);
        return attrs;
    }

    /**
     * building number or name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=327)
    public Map<String,Object> unsetCalResBuilding(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResBuilding, "");
        return attrs;
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
     * capacity
     *
     * @param zimbraCalResCapacity new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=330)
    public Map<String,Object> setCalResCapacity(int zimbraCalResCapacity, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, Integer.toString(zimbraCalResCapacity));
        return attrs;
    }

    /**
     * capacity
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=330)
    public Map<String,Object> unsetCalResCapacity(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, "");
        return attrs;
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
     * email of contact in charge of resource
     *
     * @param zimbraCalResContactEmail new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=332)
    public Map<String,Object> setCalResContactEmail(String zimbraCalResContactEmail, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactEmail, zimbraCalResContactEmail);
        return attrs;
    }

    /**
     * email of contact in charge of resource
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=332)
    public Map<String,Object> unsetCalResContactEmail(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactEmail, "");
        return attrs;
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
     * name of contact in charge of resource
     *
     * @param zimbraCalResContactName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=331)
    public Map<String,Object> setCalResContactName(String zimbraCalResContactName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactName, zimbraCalResContactName);
        return attrs;
    }

    /**
     * name of contact in charge of resource
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=331)
    public Map<String,Object> unsetCalResContactName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactName, "");
        return attrs;
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
     * phone number of contact in charge of resource
     *
     * @param zimbraCalResContactPhone new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=333)
    public Map<String,Object> setCalResContactPhone(String zimbraCalResContactPhone, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactPhone, zimbraCalResContactPhone);
        return attrs;
    }

    /**
     * phone number of contact in charge of resource
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=333)
    public Map<String,Object> unsetCalResContactPhone(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactPhone, "");
        return attrs;
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
     * floor number or name
     *
     * @param zimbraCalResFloor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=328)
    public Map<String,Object> setCalResFloor(String zimbraCalResFloor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResFloor, zimbraCalResFloor);
        return attrs;
    }

    /**
     * floor number or name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=328)
    public Map<String,Object> unsetCalResFloor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResFloor, "");
        return attrs;
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
     * display name for resource location
     *
     * @param zimbraCalResLocationDisplayName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=324)
    public Map<String,Object> setCalResLocationDisplayName(String zimbraCalResLocationDisplayName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResLocationDisplayName, zimbraCalResLocationDisplayName);
        return attrs;
    }

    /**
     * display name for resource location
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=324)
    public Map<String,Object> unsetCalResLocationDisplayName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResLocationDisplayName, "");
        return attrs;
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
     * room number or name
     *
     * @param zimbraCalResRoom new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=329)
    public Map<String,Object> setCalResRoom(String zimbraCalResRoom, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResRoom, zimbraCalResRoom);
        return attrs;
    }

    /**
     * room number or name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=329)
    public Map<String,Object> unsetCalResRoom(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResRoom, "");
        return attrs;
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
     * site name
     *
     * @param zimbraCalResSite new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=326)
    public Map<String,Object> setCalResSite(String zimbraCalResSite, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResSite, zimbraCalResSite);
        return attrs;
    }

    /**
     * site name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=326)
    public Map<String,Object> unsetCalResSite(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResSite, "");
        return attrs;
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Equipment, Location]
     *
     * @return zimbraCalResType, or null if unset and/or has invalid value
     */
    @ZAttr(id=314)
    public ZAttrProvisioning.CalResType getCalResType() {
        try { String v = getAttr(Provisioning.A_zimbraCalResType); return v == null ? null : ZAttrProvisioning.CalResType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Equipment, Location]
     *
     * @return zimbraCalResType, or null unset
     */
    @ZAttr(id=314)
    public String getCalResTypeAsString() {
        return getAttr(Provisioning.A_zimbraCalResType);
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Equipment, Location]
     *
     * @param zimbraCalResType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=314)
    public Map<String,Object> setCalResType(ZAttrProvisioning.CalResType zimbraCalResType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResType, zimbraCalResType.toString());
        return attrs;
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Equipment, Location]
     *
     * @param zimbraCalResType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=314)
    public Map<String,Object> setCalResTypeAsString(String zimbraCalResType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResType, zimbraCalResType);
        return attrs;
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Equipment, Location]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=314)
    public Map<String,Object> unsetCalResType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResType, "");
        return attrs;
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
     * Zimbra Systems Unique ID
     *
     * @param zimbraId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=1)
    public Map<String,Object> setId(String zimbraId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, zimbraId);
        return attrs;
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=1)
    public Map<String,Object> unsetId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "");
        return attrs;
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

    /**
     * locale of entry, e.g. en_US
     *
     * @param zimbraLocale new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=345)
    public Map<String,Object> setLocale(String zimbraLocale, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, zimbraLocale);
        return attrs;
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=345)
    public Map<String,Object> unsetLocale(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, "");
        return attrs;
    }

    ///// END-AUTO-GEN-REPLACE
}

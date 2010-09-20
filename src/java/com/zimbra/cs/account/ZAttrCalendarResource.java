/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.util.DateUtil;

import java.util.Date;
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

    /* build: unknown unknown unknown unknown */

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @return displayName, or null if unset
     */
    @ZAttr(id=-1)
    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName, null);
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @param displayName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setDisplayName(String displayName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, displayName);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetDisplayName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * <p>Valid values: [RESOURCE, USER]
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
     * <p>Valid values: [RESOURCE, USER]
     *
     * @return zimbraAccountCalendarUserType, or null if unset
     */
    @ZAttr(id=313)
    public String getAccountCalendarUserTypeAsString() {
        return getAttr(Provisioning.A_zimbraAccountCalendarUserType, null);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [RESOURCE, USER]
     *
     * @param zimbraAccountCalendarUserType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=313)
    public void setAccountCalendarUserType(ZAttrProvisioning.AccountCalendarUserType zimbraAccountCalendarUserType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, zimbraAccountCalendarUserType.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [RESOURCE, USER]
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
     * <p>Valid values: [RESOURCE, USER]
     *
     * @param zimbraAccountCalendarUserType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=313)
    public void setAccountCalendarUserTypeAsString(String zimbraAccountCalendarUserType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, zimbraAccountCalendarUserType);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [RESOURCE, USER]
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
     * <p>Valid values: [RESOURCE, USER]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=313)
    public void unsetAccountCalendarUserType() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [RESOURCE, USER]
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=315)
    public void setCalResAutoAcceptDecline(boolean zimbraCalResAutoAcceptDecline) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoAcceptDecline, zimbraCalResAutoAcceptDecline ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
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
        attrs.put(Provisioning.A_zimbraCalResAutoAcceptDecline, zimbraCalResAutoAcceptDecline ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether this calendar resource accepts/declines meeting invites
     * automatically; default TRUE
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=315)
    public void unsetCalResAutoAcceptDecline() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoAcceptDecline, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=322)
    public void setCalResAutoDeclineIfBusy(boolean zimbraCalResAutoDeclineIfBusy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineIfBusy, zimbraCalResAutoDeclineIfBusy ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
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
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineIfBusy, zimbraCalResAutoDeclineIfBusy ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether this calendar resource declines invite if already busy;
     * default TRUE
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=322)
    public void unsetCalResAutoDeclineIfBusy() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineIfBusy, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * appointments; default FALSE
     *
     * @return zimbraCalResAutoDeclineRecurring, or false if unset
     */
    @ZAttr(id=323)
    public boolean isCalResAutoDeclineRecurring() {
        return getBooleanAttr(Provisioning.A_zimbraCalResAutoDeclineRecurring, false);
    }

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FALSE
     *
     * @param zimbraCalResAutoDeclineRecurring new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=323)
    public void setCalResAutoDeclineRecurring(boolean zimbraCalResAutoDeclineRecurring) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineRecurring, zimbraCalResAutoDeclineRecurring ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FALSE
     *
     * @param zimbraCalResAutoDeclineRecurring new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=323)
    public Map<String,Object> setCalResAutoDeclineRecurring(boolean zimbraCalResAutoDeclineRecurring, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineRecurring, zimbraCalResAutoDeclineRecurring ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FALSE
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=323)
    public void unsetCalResAutoDeclineRecurring() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResAutoDeclineRecurring, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FALSE
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
     * @return zimbraCalResBuilding, or null if unset
     */
    @ZAttr(id=327)
    public String getCalResBuilding() {
        return getAttr(Provisioning.A_zimbraCalResBuilding, null);
    }

    /**
     * building number or name
     *
     * @param zimbraCalResBuilding new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=327)
    public void setCalResBuilding(String zimbraCalResBuilding) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResBuilding, zimbraCalResBuilding);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=327)
    public void unsetCalResBuilding() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResBuilding, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=330)
    public void setCalResCapacity(int zimbraCalResCapacity) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, Integer.toString(zimbraCalResCapacity));
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=330)
    public void unsetCalResCapacity() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraCalResContactEmail, or null if unset
     */
    @ZAttr(id=332)
    public String getCalResContactEmail() {
        return getAttr(Provisioning.A_zimbraCalResContactEmail, null);
    }

    /**
     * email of contact in charge of resource
     *
     * @param zimbraCalResContactEmail new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=332)
    public void setCalResContactEmail(String zimbraCalResContactEmail) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactEmail, zimbraCalResContactEmail);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=332)
    public void unsetCalResContactEmail() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactEmail, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraCalResContactName, or null if unset
     */
    @ZAttr(id=331)
    public String getCalResContactName() {
        return getAttr(Provisioning.A_zimbraCalResContactName, null);
    }

    /**
     * name of contact in charge of resource
     *
     * @param zimbraCalResContactName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=331)
    public void setCalResContactName(String zimbraCalResContactName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactName, zimbraCalResContactName);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=331)
    public void unsetCalResContactName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactName, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraCalResContactPhone, or null if unset
     */
    @ZAttr(id=333)
    public String getCalResContactPhone() {
        return getAttr(Provisioning.A_zimbraCalResContactPhone, null);
    }

    /**
     * phone number of contact in charge of resource
     *
     * @param zimbraCalResContactPhone new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=333)
    public void setCalResContactPhone(String zimbraCalResContactPhone) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactPhone, zimbraCalResContactPhone);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=333)
    public void unsetCalResContactPhone() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResContactPhone, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraCalResFloor, or null if unset
     */
    @ZAttr(id=328)
    public String getCalResFloor() {
        return getAttr(Provisioning.A_zimbraCalResFloor, null);
    }

    /**
     * floor number or name
     *
     * @param zimbraCalResFloor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=328)
    public void setCalResFloor(String zimbraCalResFloor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResFloor, zimbraCalResFloor);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=328)
    public void unsetCalResFloor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResFloor, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraCalResLocationDisplayName, or null if unset
     */
    @ZAttr(id=324)
    public String getCalResLocationDisplayName() {
        return getAttr(Provisioning.A_zimbraCalResLocationDisplayName, null);
    }

    /**
     * display name for resource location
     *
     * @param zimbraCalResLocationDisplayName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=324)
    public void setCalResLocationDisplayName(String zimbraCalResLocationDisplayName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResLocationDisplayName, zimbraCalResLocationDisplayName);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=324)
    public void unsetCalResLocationDisplayName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResLocationDisplayName, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * Maximum number of conflicting instances allowed before declining
     * schedule request for a recurring appointments; default 0 (means
     * decline on any conflict)
     *
     * @return zimbraCalResMaxNumConflictsAllowed, or -1 if unset
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=808)
    public int getCalResMaxNumConflictsAllowed() {
        return getIntAttr(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, -1);
    }

    /**
     * Maximum number of conflicting instances allowed before declining
     * schedule request for a recurring appointments; default 0 (means
     * decline on any conflict)
     *
     * @param zimbraCalResMaxNumConflictsAllowed new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=808)
    public void setCalResMaxNumConflictsAllowed(int zimbraCalResMaxNumConflictsAllowed) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, Integer.toString(zimbraCalResMaxNumConflictsAllowed));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of conflicting instances allowed before declining
     * schedule request for a recurring appointments; default 0 (means
     * decline on any conflict)
     *
     * @param zimbraCalResMaxNumConflictsAllowed new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=808)
    public Map<String,Object> setCalResMaxNumConflictsAllowed(int zimbraCalResMaxNumConflictsAllowed, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, Integer.toString(zimbraCalResMaxNumConflictsAllowed));
        return attrs;
    }

    /**
     * Maximum number of conflicting instances allowed before declining
     * schedule request for a recurring appointments; default 0 (means
     * decline on any conflict)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=808)
    public void unsetCalResMaxNumConflictsAllowed() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of conflicting instances allowed before declining
     * schedule request for a recurring appointments; default 0 (means
     * decline on any conflict)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=808)
    public Map<String,Object> unsetCalResMaxNumConflictsAllowed(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, "");
        return attrs;
    }

    /**
     * Maximum percent of conflicting instances allowed before declining
     * schedule request for a recurring appointment; default 0 (means decline
     * on any conflict)
     *
     * @return zimbraCalResMaxPercentConflictsAllowed, or -1 if unset
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=809)
    public int getCalResMaxPercentConflictsAllowed() {
        return getIntAttr(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, -1);
    }

    /**
     * Maximum percent of conflicting instances allowed before declining
     * schedule request for a recurring appointment; default 0 (means decline
     * on any conflict)
     *
     * @param zimbraCalResMaxPercentConflictsAllowed new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=809)
    public void setCalResMaxPercentConflictsAllowed(int zimbraCalResMaxPercentConflictsAllowed) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, Integer.toString(zimbraCalResMaxPercentConflictsAllowed));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum percent of conflicting instances allowed before declining
     * schedule request for a recurring appointment; default 0 (means decline
     * on any conflict)
     *
     * @param zimbraCalResMaxPercentConflictsAllowed new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=809)
    public Map<String,Object> setCalResMaxPercentConflictsAllowed(int zimbraCalResMaxPercentConflictsAllowed, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, Integer.toString(zimbraCalResMaxPercentConflictsAllowed));
        return attrs;
    }

    /**
     * Maximum percent of conflicting instances allowed before declining
     * schedule request for a recurring appointment; default 0 (means decline
     * on any conflict)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=809)
    public void unsetCalResMaxPercentConflictsAllowed() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum percent of conflicting instances allowed before declining
     * schedule request for a recurring appointment; default 0 (means decline
     * on any conflict)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=809)
    public Map<String,Object> unsetCalResMaxPercentConflictsAllowed(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, "");
        return attrs;
    }

    /**
     * room number or name
     *
     * @return zimbraCalResRoom, or null if unset
     */
    @ZAttr(id=329)
    public String getCalResRoom() {
        return getAttr(Provisioning.A_zimbraCalResRoom, null);
    }

    /**
     * room number or name
     *
     * @param zimbraCalResRoom new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=329)
    public void setCalResRoom(String zimbraCalResRoom) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResRoom, zimbraCalResRoom);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=329)
    public void unsetCalResRoom() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResRoom, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraCalResSite, or null if unset
     */
    @ZAttr(id=326)
    public String getCalResSite() {
        return getAttr(Provisioning.A_zimbraCalResSite, null);
    }

    /**
     * site name
     *
     * @param zimbraCalResSite new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=326)
    public void setCalResSite(String zimbraCalResSite) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResSite, zimbraCalResSite);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=326)
    public void unsetCalResSite() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResSite, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * <p>Valid values: [Location, Equipment]
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
     * <p>Valid values: [Location, Equipment]
     *
     * @return zimbraCalResType, or null if unset
     */
    @ZAttr(id=314)
    public String getCalResTypeAsString() {
        return getAttr(Provisioning.A_zimbraCalResType, null);
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Location, Equipment]
     *
     * @param zimbraCalResType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=314)
    public void setCalResType(ZAttrProvisioning.CalResType zimbraCalResType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResType, zimbraCalResType.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Location, Equipment]
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
     * <p>Valid values: [Location, Equipment]
     *
     * @param zimbraCalResType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=314)
    public void setCalResTypeAsString(String zimbraCalResType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResType, zimbraCalResType);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Location, Equipment]
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
     * <p>Valid values: [Location, Equipment]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=314)
    public void unsetCalResType() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalResType, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * calendar resource type - Location or Equipment
     *
     * <p>Valid values: [Location, Equipment]
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
     * time object was created
     *
     * <p>Use getCreateTimestampAsString to access value as a string.
     *
     * @see #getCreateTimestampAsString()
     *
     * @return zimbraCreateTimestamp as Date, null if unset or unable to parse
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Date getCreateTimestamp() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraCreateTimestamp, null);
    }

    /**
     * time object was created
     *
     * @return zimbraCreateTimestamp, or null if unset
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public String getCreateTimestampAsString() {
        return getAttr(Provisioning.A_zimbraCreateTimestamp, null);
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public void setCreateTimestamp(Date zimbraCreateTimestamp) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(zimbraCreateTimestamp));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Map<String,Object> setCreateTimestamp(Date zimbraCreateTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(zimbraCreateTimestamp));
        return attrs;
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public void setCreateTimestampAsString(String zimbraCreateTimestamp) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Map<String,Object> setCreateTimestampAsString(String zimbraCreateTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp);
        return attrs;
    }

    /**
     * time object was created
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public void unsetCreateTimestamp() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time object was created
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Map<String,Object> unsetCreateTimestamp(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, "");
        return attrs;
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null if unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId, null);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @param zimbraId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=1)
    public void setId(String zimbraId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, zimbraId);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=1)
    public void unsetId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "");
        getProvisioning().modifyAttrs(this, attrs);
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
     * @return zimbraLocale, or null if unset
     */
    @ZAttr(id=345)
    public String getLocaleAsString() {
        return getAttr(Provisioning.A_zimbraLocale, null);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @param zimbraLocale new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=345)
    public void setLocale(String zimbraLocale) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, zimbraLocale);
        getProvisioning().modifyAttrs(this, attrs);
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
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=345)
    public void unsetLocale() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, "");
        getProvisioning().modifyAttrs(this, attrs);
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

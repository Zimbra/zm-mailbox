/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.account.ZAttr;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public abstract class ZAttrUCService extends NamedEntry {

    public ZAttrUCService(String name, String id, Map<String,Object> attrs, Provisioning prov) {
        super(name, id, attrs, null, prov);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 8.0.0_BETA1_1111 pshao 20120414-1503 */

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @return cn, or null if unset
     */
    @ZAttr(id=-1)
    public String getCn() {
        return getAttr(Provisioning.A_cn, null);
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @param cn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setCn(String cn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, cn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @param cn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setCn(String cn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, cn);
        return attrs;
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetCn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetCn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, "");
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @return description, or empty array if unset
     */
    @ZAttr(id=-1)
    public String[] getDescription() {
        return getMultiAttr(Provisioning.A_description);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setDescription(String[] description) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, description);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setDescription(String[] description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, description);
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void addDescription(String description) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_description, description);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> addDescription(String description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_description, description);
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void removeDescription(String description) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_description, description);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> removeDescription(String description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_description, description);
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetDescription() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetDescription(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, "");
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @return zimbraACE, or empty array if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public String[] getACE() {
        return getMultiAttr(Provisioning.A_zimbraACE);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void setACE(String[] zimbraACE) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, zimbraACE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> setACE(String[] zimbraACE, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, zimbraACE);
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void addACE(String zimbraACE) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraACE, zimbraACE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> addACE(String zimbraACE, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraACE, zimbraACE);
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void removeACE(String zimbraACE) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraACE, zimbraACE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> removeACE(String zimbraACE, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraACE, zimbraACE);
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void unsetACE() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> unsetACE(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, "");
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
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp==null ? "" : DateUtil.toGeneralizedTime(zimbraCreateTimestamp));
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
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp==null ? "" : DateUtil.toGeneralizedTime(zimbraCreateTimestamp));
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
     * admin password for UC service
     *
     * @return zimbraUCAdminPassword, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1404)
    public String getUCAdminPassword() {
        return getAttr(Provisioning.A_zimbraUCAdminPassword, null);
    }

    /**
     * admin password for UC service
     *
     * @param zimbraUCAdminPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1404)
    public void setUCAdminPassword(String zimbraUCAdminPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminPassword, zimbraUCAdminPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin password for UC service
     *
     * @param zimbraUCAdminPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1404)
    public Map<String,Object> setUCAdminPassword(String zimbraUCAdminPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminPassword, zimbraUCAdminPassword);
        return attrs;
    }

    /**
     * admin password for UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1404)
    public void unsetUCAdminPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin password for UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1404)
    public Map<String,Object> unsetUCAdminPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminPassword, "");
        return attrs;
    }

    /**
     * admin service URL for the UC service
     *
     * @return zimbraUCAdminURL, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1405)
    public String getUCAdminURL() {
        return getAttr(Provisioning.A_zimbraUCAdminURL, null);
    }

    /**
     * admin service URL for the UC service
     *
     * @param zimbraUCAdminURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1405)
    public void setUCAdminURL(String zimbraUCAdminURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminURL, zimbraUCAdminURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin service URL for the UC service
     *
     * @param zimbraUCAdminURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1405)
    public Map<String,Object> setUCAdminURL(String zimbraUCAdminURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminURL, zimbraUCAdminURL);
        return attrs;
    }

    /**
     * admin service URL for the UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1405)
    public void unsetUCAdminURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin service URL for the UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1405)
    public Map<String,Object> unsetUCAdminURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminURL, "");
        return attrs;
    }

    /**
     * admin user for UC service
     *
     * @return zimbraUCAdminUser, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1403)
    public String getUCAdminUser() {
        return getAttr(Provisioning.A_zimbraUCAdminUser, null);
    }

    /**
     * admin user for UC service
     *
     * @param zimbraUCAdminUser new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1403)
    public void setUCAdminUser(String zimbraUCAdminUser) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminUser, zimbraUCAdminUser);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin user for UC service
     *
     * @param zimbraUCAdminUser new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1403)
    public Map<String,Object> setUCAdminUser(String zimbraUCAdminUser, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminUser, zimbraUCAdminUser);
        return attrs;
    }

    /**
     * admin user for UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1403)
    public void unsetUCAdminUser() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminUser, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin user for UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1403)
    public Map<String,Object> unsetUCAdminUser(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCAdminUser, "");
        return attrs;
    }

    /**
     * call control service URL for the UC service
     *
     * @return zimbraUCCallControlURL, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1409)
    public String getUCCallControlURL() {
        return getAttr(Provisioning.A_zimbraUCCallControlURL, null);
    }

    /**
     * call control service URL for the UC service
     *
     * @param zimbraUCCallControlURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1409)
    public void setUCCallControlURL(String zimbraUCCallControlURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallControlURL, zimbraUCCallControlURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * call control service URL for the UC service
     *
     * @param zimbraUCCallControlURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1409)
    public Map<String,Object> setUCCallControlURL(String zimbraUCCallControlURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallControlURL, zimbraUCCallControlURL);
        return attrs;
    }

    /**
     * call control service URL for the UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1409)
    public void unsetUCCallControlURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallControlURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * call control service URL for the UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1409)
    public Map<String,Object> unsetUCCallControlURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallControlURL, "");
        return attrs;
    }

    /**
     * call log service URL for the UC service
     *
     * @return zimbraUCCallLogURL, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1408)
    public String getUCCallLogURL() {
        return getAttr(Provisioning.A_zimbraUCCallLogURL, null);
    }

    /**
     * call log service URL for the UC service
     *
     * @param zimbraUCCallLogURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1408)
    public void setUCCallLogURL(String zimbraUCCallLogURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallLogURL, zimbraUCCallLogURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * call log service URL for the UC service
     *
     * @param zimbraUCCallLogURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1408)
    public Map<String,Object> setUCCallLogURL(String zimbraUCCallLogURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallLogURL, zimbraUCCallLogURL);
        return attrs;
    }

    /**
     * call log service URL for the UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1408)
    public void unsetUCCallLogURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallLogURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * call log service URL for the UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1408)
    public Map<String,Object> unsetUCCallLogURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCCallLogURL, "");
        return attrs;
    }

    /**
     * handler class for the UC service
     *
     * @return zimbraUCHandlerClass, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1402)
    public String getUCHandlerClass() {
        return getAttr(Provisioning.A_zimbraUCHandlerClass, null);
    }

    /**
     * handler class for the UC service
     *
     * @param zimbraUCHandlerClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1402)
    public void setUCHandlerClass(String zimbraUCHandlerClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCHandlerClass, zimbraUCHandlerClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * handler class for the UC service
     *
     * @param zimbraUCHandlerClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1402)
    public Map<String,Object> setUCHandlerClass(String zimbraUCHandlerClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCHandlerClass, zimbraUCHandlerClass);
        return attrs;
    }

    /**
     * handler class for the UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1402)
    public void unsetUCHandlerClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCHandlerClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * handler class for the UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1402)
    public Map<String,Object> unsetUCHandlerClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCHandlerClass, "");
        return attrs;
    }

    /**
     * video service URL for the UC service
     *
     * @return zimbraUCVideoURL, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1407)
    public String getUCVideoURL() {
        return getAttr(Provisioning.A_zimbraUCVideoURL, null);
    }

    /**
     * video service URL for the UC service
     *
     * @param zimbraUCVideoURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1407)
    public void setUCVideoURL(String zimbraUCVideoURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVideoURL, zimbraUCVideoURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * video service URL for the UC service
     *
     * @param zimbraUCVideoURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1407)
    public Map<String,Object> setUCVideoURL(String zimbraUCVideoURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVideoURL, zimbraUCVideoURL);
        return attrs;
    }

    /**
     * video service URL for the UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1407)
    public void unsetUCVideoURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVideoURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * video service URL for the UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1407)
    public Map<String,Object> unsetUCVideoURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVideoURL, "");
        return attrs;
    }

    /**
     * voicemail service URL for the UC service
     *
     * @return zimbraUCVoicemailURL, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1406)
    public String getUCVoicemailURL() {
        return getAttr(Provisioning.A_zimbraUCVoicemailURL, null);
    }

    /**
     * voicemail service URL for the UC service
     *
     * @param zimbraUCVoicemailURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1406)
    public void setUCVoicemailURL(String zimbraUCVoicemailURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVoicemailURL, zimbraUCVoicemailURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * voicemail service URL for the UC service
     *
     * @param zimbraUCVoicemailURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1406)
    public Map<String,Object> setUCVoicemailURL(String zimbraUCVoicemailURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVoicemailURL, zimbraUCVoicemailURL);
        return attrs;
    }

    /**
     * voicemail service URL for the UC service
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1406)
    public void unsetUCVoicemailURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVoicemailURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * voicemail service URL for the UC service
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1406)
    public Map<String,Object> unsetUCVoicemailURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUCVoicemailURL, "");
        return attrs;
    }

    ///// END-AUTO-GEN-REPLACE

}

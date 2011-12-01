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

import com.zimbra.common.account.ZAttr;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public abstract class ZAttrDomain extends NamedEntry {

    public ZAttrDomain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);

    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 8.0.0_BETA1_1111 pshao 20111201-1051 */

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
     * whether to show catchall addresses in admin console
     *
     * @return zimbraAdminConsoleCatchAllAddressEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public boolean isAdminConsoleCatchAllAddressEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, false);
    }

    /**
     * whether to show catchall addresses in admin console
     *
     * @param zimbraAdminConsoleCatchAllAddressEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public void setAdminConsoleCatchAllAddressEnabled(boolean zimbraAdminConsoleCatchAllAddressEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, zimbraAdminConsoleCatchAllAddressEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to show catchall addresses in admin console
     *
     * @param zimbraAdminConsoleCatchAllAddressEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public Map<String,Object> setAdminConsoleCatchAllAddressEnabled(boolean zimbraAdminConsoleCatchAllAddressEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, zimbraAdminConsoleCatchAllAddressEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether to show catchall addresses in admin console
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public void unsetAdminConsoleCatchAllAddressEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to show catchall addresses in admin console
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public Map<String,Object> unsetAdminConsoleCatchAllAddressEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, "");
        return attrs;
    }

    /**
     * enable MX check feature for domain
     *
     * @return zimbraAdminConsoleDNSCheckEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public boolean isAdminConsoleDNSCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, false);
    }

    /**
     * enable MX check feature for domain
     *
     * @param zimbraAdminConsoleDNSCheckEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public void setAdminConsoleDNSCheckEnabled(boolean zimbraAdminConsoleDNSCheckEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, zimbraAdminConsoleDNSCheckEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable MX check feature for domain
     *
     * @param zimbraAdminConsoleDNSCheckEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public Map<String,Object> setAdminConsoleDNSCheckEnabled(boolean zimbraAdminConsoleDNSCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, zimbraAdminConsoleDNSCheckEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * enable MX check feature for domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public void unsetAdminConsoleDNSCheckEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable MX check feature for domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public Map<String,Object> unsetAdminConsoleDNSCheckEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, "");
        return attrs;
    }

    /**
     * whether configuring external LDAP auth is enabled in admin console
     *
     * @return zimbraAdminConsoleLDAPAuthEnabled, or false if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=774)
    public boolean isAdminConsoleLDAPAuthEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleLDAPAuthEnabled, false);
    }

    /**
     * whether configuring external LDAP auth is enabled in admin console
     *
     * @param zimbraAdminConsoleLDAPAuthEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=774)
    public void setAdminConsoleLDAPAuthEnabled(boolean zimbraAdminConsoleLDAPAuthEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLDAPAuthEnabled, zimbraAdminConsoleLDAPAuthEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether configuring external LDAP auth is enabled in admin console
     *
     * @param zimbraAdminConsoleLDAPAuthEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=774)
    public Map<String,Object> setAdminConsoleLDAPAuthEnabled(boolean zimbraAdminConsoleLDAPAuthEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLDAPAuthEnabled, zimbraAdminConsoleLDAPAuthEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether configuring external LDAP auth is enabled in admin console
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=774)
    public void unsetAdminConsoleLDAPAuthEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLDAPAuthEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether configuring external LDAP auth is enabled in admin console
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=774)
    public Map<String,Object> unsetAdminConsoleLDAPAuthEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLDAPAuthEnabled, "");
        return attrs;
    }

    /**
     * admin console login message
     *
     * @return zimbraAdminConsoleLoginMessage, or empty array if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public String[] getAdminConsoleLoginMessage() {
        return getMultiAttr(Provisioning.A_zimbraAdminConsoleLoginMessage);
    }

    /**
     * admin console login message
     *
     * @param zimbraAdminConsoleLoginMessage new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public void setAdminConsoleLoginMessage(String[] zimbraAdminConsoleLoginMessage) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginMessage, zimbraAdminConsoleLoginMessage);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin console login message
     *
     * @param zimbraAdminConsoleLoginMessage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public Map<String,Object> setAdminConsoleLoginMessage(String[] zimbraAdminConsoleLoginMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginMessage, zimbraAdminConsoleLoginMessage);
        return attrs;
    }

    /**
     * admin console login message
     *
     * @param zimbraAdminConsoleLoginMessage new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public void addAdminConsoleLoginMessage(String zimbraAdminConsoleLoginMessage) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAdminConsoleLoginMessage, zimbraAdminConsoleLoginMessage);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin console login message
     *
     * @param zimbraAdminConsoleLoginMessage new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public Map<String,Object> addAdminConsoleLoginMessage(String zimbraAdminConsoleLoginMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAdminConsoleLoginMessage, zimbraAdminConsoleLoginMessage);
        return attrs;
    }

    /**
     * admin console login message
     *
     * @param zimbraAdminConsoleLoginMessage existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public void removeAdminConsoleLoginMessage(String zimbraAdminConsoleLoginMessage) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAdminConsoleLoginMessage, zimbraAdminConsoleLoginMessage);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin console login message
     *
     * @param zimbraAdminConsoleLoginMessage existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public Map<String,Object> removeAdminConsoleLoginMessage(String zimbraAdminConsoleLoginMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAdminConsoleLoginMessage, zimbraAdminConsoleLoginMessage);
        return attrs;
    }

    /**
     * admin console login message
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public void unsetAdminConsoleLoginMessage() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginMessage, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * admin console login message
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public Map<String,Object> unsetAdminConsoleLoginMessage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginMessage, "");
        return attrs;
    }

    /**
     * login URL for admin console to send the user to upon explicit logging
     * in
     *
     * @return zimbraAdminConsoleLoginURL, or null if unset
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=696)
    public String getAdminConsoleLoginURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLoginURL, null);
    }

    /**
     * login URL for admin console to send the user to upon explicit logging
     * in
     *
     * @param zimbraAdminConsoleLoginURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=696)
    public void setAdminConsoleLoginURL(String zimbraAdminConsoleLoginURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, zimbraAdminConsoleLoginURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * login URL for admin console to send the user to upon explicit logging
     * in
     *
     * @param zimbraAdminConsoleLoginURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=696)
    public Map<String,Object> setAdminConsoleLoginURL(String zimbraAdminConsoleLoginURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, zimbraAdminConsoleLoginURL);
        return attrs;
    }

    /**
     * login URL for admin console to send the user to upon explicit logging
     * in
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=696)
    public void unsetAdminConsoleLoginURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * login URL for admin console to send the user to upon explicit logging
     * in
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=696)
    public Map<String,Object> unsetAdminConsoleLoginURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, "");
        return attrs;
    }

    /**
     * logout URL for admin console to send the user to upon explicit logging
     * out
     *
     * @return zimbraAdminConsoleLogoutURL, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=684)
    public String getAdminConsoleLogoutURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLogoutURL, null);
    }

    /**
     * logout URL for admin console to send the user to upon explicit logging
     * out
     *
     * @param zimbraAdminConsoleLogoutURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=684)
    public void setAdminConsoleLogoutURL(String zimbraAdminConsoleLogoutURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, zimbraAdminConsoleLogoutURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for admin console to send the user to upon explicit logging
     * out
     *
     * @param zimbraAdminConsoleLogoutURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=684)
    public Map<String,Object> setAdminConsoleLogoutURL(String zimbraAdminConsoleLogoutURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, zimbraAdminConsoleLogoutURL);
        return attrs;
    }

    /**
     * logout URL for admin console to send the user to upon explicit logging
     * out
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=684)
    public void unsetAdminConsoleLogoutURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for admin console to send the user to upon explicit logging
     * out
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=684)
    public Map<String,Object> unsetAdminConsoleLogoutURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, "");
        return attrs;
    }

    /**
     * whether to allow skin management in admin console
     *
     * @return zimbraAdminConsoleSkinEnabled, or false if unset
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public boolean isAdminConsoleSkinEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleSkinEnabled, false);
    }

    /**
     * whether to allow skin management in admin console
     *
     * @param zimbraAdminConsoleSkinEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public void setAdminConsoleSkinEnabled(boolean zimbraAdminConsoleSkinEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleSkinEnabled, zimbraAdminConsoleSkinEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to allow skin management in admin console
     *
     * @param zimbraAdminConsoleSkinEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public Map<String,Object> setAdminConsoleSkinEnabled(boolean zimbraAdminConsoleSkinEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleSkinEnabled, zimbraAdminConsoleSkinEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether to allow skin management in admin console
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public void unsetAdminConsoleSkinEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleSkinEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to allow skin management in admin console
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public Map<String,Object> unsetAdminConsoleSkinEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleSkinEnabled, "");
        return attrs;
    }

    /**
     * last calculated aggregate quota usage for the domain in bytes
     *
     * @return zimbraAggregateQuotaLastUsage, or -1 if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1328)
    public long getAggregateQuotaLastUsage() {
        return getLongAttr(Provisioning.A_zimbraAggregateQuotaLastUsage, -1L);
    }

    /**
     * last calculated aggregate quota usage for the domain in bytes
     *
     * @param zimbraAggregateQuotaLastUsage new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1328)
    public void setAggregateQuotaLastUsage(long zimbraAggregateQuotaLastUsage) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAggregateQuotaLastUsage, Long.toString(zimbraAggregateQuotaLastUsage));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * last calculated aggregate quota usage for the domain in bytes
     *
     * @param zimbraAggregateQuotaLastUsage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1328)
    public Map<String,Object> setAggregateQuotaLastUsage(long zimbraAggregateQuotaLastUsage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAggregateQuotaLastUsage, Long.toString(zimbraAggregateQuotaLastUsage));
        return attrs;
    }

    /**
     * last calculated aggregate quota usage for the domain in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1328)
    public void unsetAggregateQuotaLastUsage() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAggregateQuotaLastUsage, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * last calculated aggregate quota usage for the domain in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1328)
    public Map<String,Object> unsetAggregateQuotaLastUsage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAggregateQuotaLastUsage, "");
        return attrs;
    }

    /**
     * fallback to local auth if external mech fails
     *
     * @return zimbraAuthFallbackToLocal, or false if unset
     */
    @ZAttr(id=257)
    public boolean isAuthFallbackToLocal() {
        return getBooleanAttr(Provisioning.A_zimbraAuthFallbackToLocal, false);
    }

    /**
     * fallback to local auth if external mech fails
     *
     * @param zimbraAuthFallbackToLocal new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=257)
    public void setAuthFallbackToLocal(boolean zimbraAuthFallbackToLocal) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthFallbackToLocal, zimbraAuthFallbackToLocal ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * fallback to local auth if external mech fails
     *
     * @param zimbraAuthFallbackToLocal new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=257)
    public Map<String,Object> setAuthFallbackToLocal(boolean zimbraAuthFallbackToLocal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthFallbackToLocal, zimbraAuthFallbackToLocal ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * fallback to local auth if external mech fails
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=257)
    public void unsetAuthFallbackToLocal() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthFallbackToLocal, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * fallback to local auth if external mech fails
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=257)
    public Map<String,Object> unsetAuthFallbackToLocal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthFallbackToLocal, "");
        return attrs;
    }

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @return zimbraAuthKerberos5Realm, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=548)
    public String getAuthKerberos5Realm() {
        return getAttr(Provisioning.A_zimbraAuthKerberos5Realm, null);
    }

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @param zimbraAuthKerberos5Realm new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=548)
    public void setAuthKerberos5Realm(String zimbraAuthKerberos5Realm) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, zimbraAuthKerberos5Realm);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @param zimbraAuthKerberos5Realm new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=548)
    public Map<String,Object> setAuthKerberos5Realm(String zimbraAuthKerberos5Realm, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, zimbraAuthKerberos5Realm);
        return attrs;
    }

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=548)
    public void unsetAuthKerberos5Realm() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=548)
    public Map<String,Object> unsetAuthKerberos5Realm(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "");
        return attrs;
    }

    /**
     * LDAP bind dn for ldap auth mech
     *
     * @return zimbraAuthLdapBindDn, or null if unset
     */
    @ZAttr(id=44)
    public String getAuthLdapBindDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapBindDn, null);
    }

    /**
     * LDAP bind dn for ldap auth mech
     *
     * @param zimbraAuthLdapBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=44)
    public void setAuthLdapBindDn(String zimbraAuthLdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapBindDn, zimbraAuthLdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind dn for ldap auth mech
     *
     * @param zimbraAuthLdapBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=44)
    public Map<String,Object> setAuthLdapBindDn(String zimbraAuthLdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapBindDn, zimbraAuthLdapBindDn);
        return attrs;
    }

    /**
     * LDAP bind dn for ldap auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=44)
    public void unsetAuthLdapBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind dn for ldap auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=44)
    public Map<String,Object> unsetAuthLdapBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapBindDn, "");
        return attrs;
    }

    /**
     * LDAP search base for ldap auth mech
     *
     * @return zimbraAuthLdapSearchBase, or null if unset
     */
    @ZAttr(id=252)
    public String getAuthLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBase, null);
    }

    /**
     * LDAP search base for ldap auth mech
     *
     * @param zimbraAuthLdapSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=252)
    public void setAuthLdapSearchBase(String zimbraAuthLdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBase, zimbraAuthLdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for ldap auth mech
     *
     * @param zimbraAuthLdapSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=252)
    public Map<String,Object> setAuthLdapSearchBase(String zimbraAuthLdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBase, zimbraAuthLdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for ldap auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=252)
    public void unsetAuthLdapSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for ldap auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=252)
    public Map<String,Object> unsetAuthLdapSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBase, "");
        return attrs;
    }

    /**
     * LDAP search bind dn for ldap auth mech
     *
     * @return zimbraAuthLdapSearchBindDn, or null if unset
     */
    @ZAttr(id=253)
    public String getAuthLdapSearchBindDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBindDn, null);
    }

    /**
     * LDAP search bind dn for ldap auth mech
     *
     * @param zimbraAuthLdapSearchBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=253)
    public void setAuthLdapSearchBindDn(String zimbraAuthLdapSearchBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, zimbraAuthLdapSearchBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search bind dn for ldap auth mech
     *
     * @param zimbraAuthLdapSearchBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=253)
    public Map<String,Object> setAuthLdapSearchBindDn(String zimbraAuthLdapSearchBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, zimbraAuthLdapSearchBindDn);
        return attrs;
    }

    /**
     * LDAP search bind dn for ldap auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=253)
    public void unsetAuthLdapSearchBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search bind dn for ldap auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=253)
    public Map<String,Object> unsetAuthLdapSearchBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, "");
        return attrs;
    }

    /**
     * LDAP search bind password for ldap auth mech
     *
     * @return zimbraAuthLdapSearchBindPassword, or null if unset
     */
    @ZAttr(id=254)
    public String getAuthLdapSearchBindPassword() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBindPassword, null);
    }

    /**
     * LDAP search bind password for ldap auth mech
     *
     * @param zimbraAuthLdapSearchBindPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=254)
    public void setAuthLdapSearchBindPassword(String zimbraAuthLdapSearchBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, zimbraAuthLdapSearchBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search bind password for ldap auth mech
     *
     * @param zimbraAuthLdapSearchBindPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=254)
    public Map<String,Object> setAuthLdapSearchBindPassword(String zimbraAuthLdapSearchBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, zimbraAuthLdapSearchBindPassword);
        return attrs;
    }

    /**
     * LDAP search bind password for ldap auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=254)
    public void unsetAuthLdapSearchBindPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search bind password for ldap auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=254)
    public Map<String,Object> unsetAuthLdapSearchBindPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, "");
        return attrs;
    }

    /**
     * LDAP search filter for ldap auth mech
     *
     * @return zimbraAuthLdapSearchFilter, or null if unset
     */
    @ZAttr(id=255)
    public String getAuthLdapSearchFilter() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchFilter, null);
    }

    /**
     * LDAP search filter for ldap auth mech
     *
     * @param zimbraAuthLdapSearchFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=255)
    public void setAuthLdapSearchFilter(String zimbraAuthLdapSearchFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, zimbraAuthLdapSearchFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for ldap auth mech
     *
     * @param zimbraAuthLdapSearchFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=255)
    public Map<String,Object> setAuthLdapSearchFilter(String zimbraAuthLdapSearchFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, zimbraAuthLdapSearchFilter);
        return attrs;
    }

    /**
     * LDAP search filter for ldap auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=255)
    public void unsetAuthLdapSearchFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for ldap auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=255)
    public Map<String,Object> unsetAuthLdapSearchFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "");
        return attrs;
    }

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @return zimbraAuthLdapStartTlsEnabled, or false if unset
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=654)
    public boolean isAuthLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAuthLdapStartTlsEnabled, false);
    }

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @param zimbraAuthLdapStartTlsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=654)
    public void setAuthLdapStartTlsEnabled(boolean zimbraAuthLdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, zimbraAuthLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @param zimbraAuthLdapStartTlsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=654)
    public Map<String,Object> setAuthLdapStartTlsEnabled(boolean zimbraAuthLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, zimbraAuthLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=654)
    public void unsetAuthLdapStartTlsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=654)
    public Map<String,Object> unsetAuthLdapStartTlsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, "");
        return attrs;
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @return zimbraAuthLdapURL, or empty array if unset
     */
    @ZAttr(id=43)
    public String[] getAuthLdapURL() {
        return getMultiAttr(Provisioning.A_zimbraAuthLdapURL);
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param zimbraAuthLdapURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=43)
    public void setAuthLdapURL(String[] zimbraAuthLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapURL, zimbraAuthLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param zimbraAuthLdapURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=43)
    public Map<String,Object> setAuthLdapURL(String[] zimbraAuthLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapURL, zimbraAuthLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param zimbraAuthLdapURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=43)
    public void addAuthLdapURL(String zimbraAuthLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAuthLdapURL, zimbraAuthLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param zimbraAuthLdapURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=43)
    public Map<String,Object> addAuthLdapURL(String zimbraAuthLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAuthLdapURL, zimbraAuthLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param zimbraAuthLdapURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=43)
    public void removeAuthLdapURL(String zimbraAuthLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAuthLdapURL, zimbraAuthLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param zimbraAuthLdapURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=43)
    public Map<String,Object> removeAuthLdapURL(String zimbraAuthLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAuthLdapURL, zimbraAuthLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=43)
    public void unsetAuthLdapURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=43)
    public Map<String,Object> unsetAuthLdapURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapURL, "");
        return attrs;
    }

    /**
     * mechanism to use for verifying password. Valid values are zimbra,
     * ldap, ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @return zimbraAuthMech, or null if unset
     */
    @ZAttr(id=42)
    public String getAuthMech() {
        return getAttr(Provisioning.A_zimbraAuthMech, null);
    }

    /**
     * mechanism to use for verifying password. Valid values are zimbra,
     * ldap, ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @param zimbraAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=42)
    public void setAuthMech(String zimbraAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMech, zimbraAuthMech);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mechanism to use for verifying password. Valid values are zimbra,
     * ldap, ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @param zimbraAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=42)
    public Map<String,Object> setAuthMech(String zimbraAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMech, zimbraAuthMech);
        return attrs;
    }

    /**
     * mechanism to use for verifying password. Valid values are zimbra,
     * ldap, ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=42)
    public void unsetAuthMech() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMech, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mechanism to use for verifying password. Valid values are zimbra,
     * ldap, ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=42)
    public Map<String,Object> unsetAuthMech(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMech, "");
        return attrs;
    }

    /**
     * mechanism to use for verifying password for admin. See zimbraAuthMech
     *
     * @return zimbraAuthMechAdmin, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1252)
    public String getAuthMechAdmin() {
        return getAttr(Provisioning.A_zimbraAuthMechAdmin, null);
    }

    /**
     * mechanism to use for verifying password for admin. See zimbraAuthMech
     *
     * @param zimbraAuthMechAdmin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1252)
    public void setAuthMechAdmin(String zimbraAuthMechAdmin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMechAdmin, zimbraAuthMechAdmin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mechanism to use for verifying password for admin. See zimbraAuthMech
     *
     * @param zimbraAuthMechAdmin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1252)
    public Map<String,Object> setAuthMechAdmin(String zimbraAuthMechAdmin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMechAdmin, zimbraAuthMechAdmin);
        return attrs;
    }

    /**
     * mechanism to use for verifying password for admin. See zimbraAuthMech
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1252)
    public void unsetAuthMechAdmin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMechAdmin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mechanism to use for verifying password for admin. See zimbraAuthMech
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1252)
    public Map<String,Object> unsetAuthMechAdmin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthMechAdmin, "");
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute name in the external directory that contains localpart of
     * the account name. If not specified, localpart of the account name is
     * the principal user used to authenticated to Zimbra.
     *
     * @return zimbraAutoProvAccountNameMap, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1230)
    public String getAutoProvAccountNameMap() {
        return getAttr(Provisioning.A_zimbraAutoProvAccountNameMap, null);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute name in the external directory that contains localpart of
     * the account name. If not specified, localpart of the account name is
     * the principal user used to authenticated to Zimbra.
     *
     * @param zimbraAutoProvAccountNameMap new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1230)
    public void setAutoProvAccountNameMap(String zimbraAutoProvAccountNameMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, zimbraAutoProvAccountNameMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute name in the external directory that contains localpart of
     * the account name. If not specified, localpart of the account name is
     * the principal user used to authenticated to Zimbra.
     *
     * @param zimbraAutoProvAccountNameMap new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1230)
    public Map<String,Object> setAutoProvAccountNameMap(String zimbraAutoProvAccountNameMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, zimbraAutoProvAccountNameMap);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute name in the external directory that contains localpart of
     * the account name. If not specified, localpart of the account name is
     * the principal user used to authenticated to Zimbra.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1230)
    public void unsetAutoProvAccountNameMap() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute name in the external directory that contains localpart of
     * the account name. If not specified, localpart of the account name is
     * the principal user used to authenticated to Zimbra.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1230)
    public Map<String,Object> unsetAutoProvAccountNameMap(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, "");
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @return zimbraAutoProvAttrMap, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public String[] getAutoProvAttrMap() {
        return getMultiAttr(Provisioning.A_zimbraAutoProvAttrMap);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param zimbraAutoProvAttrMap new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public void setAutoProvAttrMap(String[] zimbraAutoProvAttrMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAttrMap, zimbraAutoProvAttrMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param zimbraAutoProvAttrMap new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public Map<String,Object> setAutoProvAttrMap(String[] zimbraAutoProvAttrMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAttrMap, zimbraAutoProvAttrMap);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param zimbraAutoProvAttrMap new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public void addAutoProvAttrMap(String zimbraAutoProvAttrMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAutoProvAttrMap, zimbraAutoProvAttrMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param zimbraAutoProvAttrMap new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public Map<String,Object> addAutoProvAttrMap(String zimbraAutoProvAttrMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAutoProvAttrMap, zimbraAutoProvAttrMap);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param zimbraAutoProvAttrMap existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public void removeAutoProvAttrMap(String zimbraAutoProvAttrMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAutoProvAttrMap, zimbraAutoProvAttrMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param zimbraAutoProvAttrMap existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public Map<String,Object> removeAutoProvAttrMap(String zimbraAutoProvAttrMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAutoProvAttrMap, zimbraAutoProvAttrMap);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public void unsetAutoProvAttrMap() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAttrMap, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public Map<String,Object> unsetAutoProvAttrMap(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAttrMap, "");
        return attrs;
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @return zimbraAutoProvAuthMech, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public String[] getAutoProvAuthMechAsString() {
        return getMultiAttr(Provisioning.A_zimbraAutoProvAuthMech);
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @param zimbraAutoProvAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public void setAutoProvAuthMech(ZAttrProvisioning.AutoProvAuthMech zimbraAutoProvAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAuthMech, zimbraAutoProvAuthMech.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @param zimbraAutoProvAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public Map<String,Object> setAutoProvAuthMech(ZAttrProvisioning.AutoProvAuthMech zimbraAutoProvAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAuthMech, zimbraAutoProvAuthMech.toString());
        return attrs;
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @param zimbraAutoProvAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public void setAutoProvAuthMechAsString(String[] zimbraAutoProvAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAuthMech, zimbraAutoProvAuthMech);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @param zimbraAutoProvAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public Map<String,Object> setAutoProvAuthMechAsString(String[] zimbraAutoProvAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAuthMech, zimbraAutoProvAuthMech);
        return attrs;
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public void unsetAutoProvAuthMech() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAuthMech, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * <p>Valid values: [KRB5, LDAP, PREAUTH, SPNEGO]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public Map<String,Object> unsetAutoProvAuthMech(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvAuthMech, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Max number of
     * accounts to process in each interval for EAGER auto provision.
     *
     * @return zimbraAutoProvBatchSize, or 20 if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1234)
    public int getAutoProvBatchSize() {
        return getIntAttr(Provisioning.A_zimbraAutoProvBatchSize, 20);
    }

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Max number of
     * accounts to process in each interval for EAGER auto provision.
     *
     * @param zimbraAutoProvBatchSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1234)
    public void setAutoProvBatchSize(int zimbraAutoProvBatchSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvBatchSize, Integer.toString(zimbraAutoProvBatchSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Max number of
     * accounts to process in each interval for EAGER auto provision.
     *
     * @param zimbraAutoProvBatchSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1234)
    public Map<String,Object> setAutoProvBatchSize(int zimbraAutoProvBatchSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvBatchSize, Integer.toString(zimbraAutoProvBatchSize));
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Max number of
     * accounts to process in each interval for EAGER auto provision.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1234)
    public void unsetAutoProvBatchSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvBatchSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Max number of
     * accounts to process in each interval for EAGER auto provision.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1234)
    public Map<String,Object> unsetAutoProvBatchSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvBatchSize, "");
        return attrs;
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * <p>Use getAutoProvLastPolledTimestampAsString to access value as a string.
     *
     * @see #getAutoProvLastPolledTimestampAsString()
     *
     * @return zimbraAutoProvLastPolledTimestamp as Date, null if unset or unable to parse
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public Date getAutoProvLastPolledTimestamp() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraAutoProvLastPolledTimestamp, null);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @return zimbraAutoProvLastPolledTimestamp, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public String getAutoProvLastPolledTimestampAsString() {
        return getAttr(Provisioning.A_zimbraAutoProvLastPolledTimestamp, null);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @param zimbraAutoProvLastPolledTimestamp new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public void setAutoProvLastPolledTimestamp(Date zimbraAutoProvLastPolledTimestamp) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, zimbraAutoProvLastPolledTimestamp==null ? "" : DateUtil.toGeneralizedTime(zimbraAutoProvLastPolledTimestamp));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @param zimbraAutoProvLastPolledTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public Map<String,Object> setAutoProvLastPolledTimestamp(Date zimbraAutoProvLastPolledTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, zimbraAutoProvLastPolledTimestamp==null ? "" : DateUtil.toGeneralizedTime(zimbraAutoProvLastPolledTimestamp));
        return attrs;
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @param zimbraAutoProvLastPolledTimestamp new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public void setAutoProvLastPolledTimestampAsString(String zimbraAutoProvLastPolledTimestamp) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, zimbraAutoProvLastPolledTimestamp);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @param zimbraAutoProvLastPolledTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public Map<String,Object> setAutoProvLastPolledTimestampAsString(String zimbraAutoProvLastPolledTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, zimbraAutoProvLastPolledTimestamp);
        return attrs;
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public void unsetAutoProvLastPolledTimestamp() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public Map<String,Object> unsetAutoProvLastPolledTimestamp(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: required LDAP search bind
     * DN for auto provision.
     *
     * @return zimbraAutoProvLdapAdminBindDn, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1225)
    public String getAutoProvLdapAdminBindDn() {
        return getAttr(Provisioning.A_zimbraAutoProvLdapAdminBindDn, null);
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: required LDAP search bind
     * DN for auto provision.
     *
     * @param zimbraAutoProvLdapAdminBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1225)
    public void setAutoProvLdapAdminBindDn(String zimbraAutoProvLdapAdminBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindDn, zimbraAutoProvLdapAdminBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: required LDAP search bind
     * DN for auto provision.
     *
     * @param zimbraAutoProvLdapAdminBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1225)
    public Map<String,Object> setAutoProvLdapAdminBindDn(String zimbraAutoProvLdapAdminBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindDn, zimbraAutoProvLdapAdminBindDn);
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: required LDAP search bind
     * DN for auto provision.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1225)
    public void unsetAutoProvLdapAdminBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: required LDAP search bind
     * DN for auto provision.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1225)
    public Map<String,Object> unsetAutoProvLdapAdminBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindDn, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * search bind password for auto provision.
     *
     * @return zimbraAutoProvLdapAdminBindPassword, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1226)
    public String getAutoProvLdapAdminBindPassword() {
        return getAttr(Provisioning.A_zimbraAutoProvLdapAdminBindPassword, null);
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * search bind password for auto provision.
     *
     * @param zimbraAutoProvLdapAdminBindPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1226)
    public void setAutoProvLdapAdminBindPassword(String zimbraAutoProvLdapAdminBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindPassword, zimbraAutoProvLdapAdminBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * search bind password for auto provision.
     *
     * @param zimbraAutoProvLdapAdminBindPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1226)
    public Map<String,Object> setAutoProvLdapAdminBindPassword(String zimbraAutoProvLdapAdminBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindPassword, zimbraAutoProvLdapAdminBindPassword);
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * search bind password for auto provision.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1226)
    public void unsetAutoProvLdapAdminBindPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * search bind password for auto provision.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1226)
    public Map<String,Object> unsetAutoProvLdapAdminBindPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindPassword, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) LDAP external DN template for account
     * auto provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @return zimbraAutoProvLdapBindDn, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1229)
    public String getAutoProvLdapBindDn() {
        return getAttr(Provisioning.A_zimbraAutoProvLdapBindDn, null);
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) LDAP external DN template for account
     * auto provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @param zimbraAutoProvLdapBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1229)
    public void setAutoProvLdapBindDn(String zimbraAutoProvLdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapBindDn, zimbraAutoProvLdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) LDAP external DN template for account
     * auto provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @param zimbraAutoProvLdapBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1229)
    public Map<String,Object> setAutoProvLdapBindDn(String zimbraAutoProvLdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapBindDn, zimbraAutoProvLdapBindDn);
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) LDAP external DN template for account
     * auto provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1229)
    public void unsetAutoProvLdapBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) LDAP external DN template for account
     * auto provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1229)
    public Map<String,Object> unsetAutoProvLdapBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapBindDn, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter), MANUAL mode: required LDAP search
     * base for auto provision, used in conjunction with
     * zimbraAutoProvLdapSearchFilter. If not set, LDAP root DSE will be
     * used.
     *
     * @return zimbraAutoProvLdapSearchBase, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1227)
    public String getAutoProvLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraAutoProvLdapSearchBase, null);
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter), MANUAL mode: required LDAP search
     * base for auto provision, used in conjunction with
     * zimbraAutoProvLdapSearchFilter. If not set, LDAP root DSE will be
     * used.
     *
     * @param zimbraAutoProvLdapSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1227)
    public void setAutoProvLdapSearchBase(String zimbraAutoProvLdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, zimbraAutoProvLdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter), MANUAL mode: required LDAP search
     * base for auto provision, used in conjunction with
     * zimbraAutoProvLdapSearchFilter. If not set, LDAP root DSE will be
     * used.
     *
     * @param zimbraAutoProvLdapSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1227)
    public Map<String,Object> setAutoProvLdapSearchBase(String zimbraAutoProvLdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, zimbraAutoProvLdapSearchBase);
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter), MANUAL mode: required LDAP search
     * base for auto provision, used in conjunction with
     * zimbraAutoProvLdapSearchFilter. If not set, LDAP root DSE will be
     * used.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1227)
    public void unsetAutoProvLdapSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter), MANUAL mode: required LDAP search
     * base for auto provision, used in conjunction with
     * zimbraAutoProvLdapSearchFilter. If not set, LDAP root DSE will be
     * used.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1227)
    public Map<String,Object> unsetAutoProvLdapSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapBindDn) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapBindDn) LDAP search filter template for account auto
     * provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @return zimbraAutoProvLdapSearchFilter, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1228)
    public String getAutoProvLdapSearchFilter() {
        return getAttr(Provisioning.A_zimbraAutoProvLdapSearchFilter, null);
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapBindDn) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapBindDn) LDAP search filter template for account auto
     * provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @param zimbraAutoProvLdapSearchFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1228)
    public void setAutoProvLdapSearchFilter(String zimbraAutoProvLdapSearchFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, zimbraAutoProvLdapSearchFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapBindDn) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapBindDn) LDAP search filter template for account auto
     * provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @param zimbraAutoProvLdapSearchFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1228)
    public Map<String,Object> setAutoProvLdapSearchFilter(String zimbraAutoProvLdapSearchFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, zimbraAutoProvLdapSearchFilter);
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapBindDn) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapBindDn) LDAP search filter template for account auto
     * provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1228)
    public void unsetAutoProvLdapSearchFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapBindDn) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapBindDn) LDAP search filter template for account auto
     * provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1228)
    public Map<String,Object> unsetAutoProvLdapSearchFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "");
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Default
     * is FALSE. Whether to use startTLS when accessing the external LDAP
     * server for auto provision.
     *
     * @return zimbraAutoProvLdapStartTlsEnabled, or false if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1224)
    public boolean isAutoProvLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAutoProvLdapStartTlsEnabled, false);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Default
     * is FALSE. Whether to use startTLS when accessing the external LDAP
     * server for auto provision.
     *
     * @param zimbraAutoProvLdapStartTlsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1224)
    public void setAutoProvLdapStartTlsEnabled(boolean zimbraAutoProvLdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapStartTlsEnabled, zimbraAutoProvLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Default
     * is FALSE. Whether to use startTLS when accessing the external LDAP
     * server for auto provision.
     *
     * @param zimbraAutoProvLdapStartTlsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1224)
    public Map<String,Object> setAutoProvLdapStartTlsEnabled(boolean zimbraAutoProvLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapStartTlsEnabled, zimbraAutoProvLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Default
     * is FALSE. Whether to use startTLS when accessing the external LDAP
     * server for auto provision.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1224)
    public void unsetAutoProvLdapStartTlsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapStartTlsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Default
     * is FALSE. Whether to use startTLS when accessing the external LDAP
     * server for auto provision.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1224)
    public Map<String,Object> unsetAutoProvLdapStartTlsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapStartTlsEnabled, "");
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * URL of the external LDAP source for auto provision.
     *
     * @return zimbraAutoProvLdapURL, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1223)
    public String getAutoProvLdapURL() {
        return getAttr(Provisioning.A_zimbraAutoProvLdapURL, null);
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * URL of the external LDAP source for auto provision.
     *
     * @param zimbraAutoProvLdapURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1223)
    public void setAutoProvLdapURL(String zimbraAutoProvLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapURL, zimbraAutoProvLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * URL of the external LDAP source for auto provision.
     *
     * @param zimbraAutoProvLdapURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1223)
    public Map<String,Object> setAutoProvLdapURL(String zimbraAutoProvLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapURL, zimbraAutoProvLdapURL);
        return attrs;
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * URL of the external LDAP source for auto provision.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1223)
    public void unsetAutoProvLdapURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * URL of the external LDAP source for auto provision.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1223)
    public Map<String,Object> unsetAutoProvLdapURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLdapURL, "");
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Class
     * name of auto provision listener. The class must implement the
     * com.zimbra.cs.account.Account.AutoProvisionListener interface. The
     * singleton listener instance is invoked after each account is auto
     * created in Zimbra. Listener can be plugged in as a server extension to
     * handle tasks like updating the account auto provision status in the
     * external LDAP directory.
     *
     * @return zimbraAutoProvListenerClass, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1233)
    public String getAutoProvListenerClass() {
        return getAttr(Provisioning.A_zimbraAutoProvListenerClass, null);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Class
     * name of auto provision listener. The class must implement the
     * com.zimbra.cs.account.Account.AutoProvisionListener interface. The
     * singleton listener instance is invoked after each account is auto
     * created in Zimbra. Listener can be plugged in as a server extension to
     * handle tasks like updating the account auto provision status in the
     * external LDAP directory.
     *
     * @param zimbraAutoProvListenerClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1233)
    public void setAutoProvListenerClass(String zimbraAutoProvListenerClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvListenerClass, zimbraAutoProvListenerClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Class
     * name of auto provision listener. The class must implement the
     * com.zimbra.cs.account.Account.AutoProvisionListener interface. The
     * singleton listener instance is invoked after each account is auto
     * created in Zimbra. Listener can be plugged in as a server extension to
     * handle tasks like updating the account auto provision status in the
     * external LDAP directory.
     *
     * @param zimbraAutoProvListenerClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1233)
    public Map<String,Object> setAutoProvListenerClass(String zimbraAutoProvListenerClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvListenerClass, zimbraAutoProvListenerClass);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Class
     * name of auto provision listener. The class must implement the
     * com.zimbra.cs.account.Account.AutoProvisionListener interface. The
     * singleton listener instance is invoked after each account is auto
     * created in Zimbra. Listener can be plugged in as a server extension to
     * handle tasks like updating the account auto provision status in the
     * external LDAP directory.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1233)
    public void unsetAutoProvListenerClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvListenerClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Class
     * name of auto provision listener. The class must implement the
     * com.zimbra.cs.account.Account.AutoProvisionListener interface. The
     * singleton listener instance is invoked after each account is auto
     * created in Zimbra. Listener can be plugged in as a server extension to
     * handle tasks like updating the account auto provision status in the
     * external LDAP directory.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1233)
    public Map<String,Object> unsetAutoProvListenerClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvListenerClass, "");
        return attrs;
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A For EAGER auto provision, a domain can be
     * scheduled on multiple server. To avoid conflict, only one server can
     * perform provisioning for a domain at one time. This attribute servers
     * a lock for the test-and-set LDAP operation to synchronize EAGER auto
     * provision attempts between servers.
     *
     * @return zimbraAutoProvLock, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1236)
    public String getAutoProvLock() {
        return getAttr(Provisioning.A_zimbraAutoProvLock, null);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A For EAGER auto provision, a domain can be
     * scheduled on multiple server. To avoid conflict, only one server can
     * perform provisioning for a domain at one time. This attribute servers
     * a lock for the test-and-set LDAP operation to synchronize EAGER auto
     * provision attempts between servers.
     *
     * @param zimbraAutoProvLock new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1236)
    public void setAutoProvLock(String zimbraAutoProvLock) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, zimbraAutoProvLock);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A For EAGER auto provision, a domain can be
     * scheduled on multiple server. To avoid conflict, only one server can
     * perform provisioning for a domain at one time. This attribute servers
     * a lock for the test-and-set LDAP operation to synchronize EAGER auto
     * provision attempts between servers.
     *
     * @param zimbraAutoProvLock new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1236)
    public Map<String,Object> setAutoProvLock(String zimbraAutoProvLock, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, zimbraAutoProvLock);
        return attrs;
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A For EAGER auto provision, a domain can be
     * scheduled on multiple server. To avoid conflict, only one server can
     * perform provisioning for a domain at one time. This attribute servers
     * a lock for the test-and-set LDAP operation to synchronize EAGER auto
     * provision attempts between servers.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1236)
    public void unsetAutoProvLock() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A For EAGER auto provision, a domain can be
     * scheduled on multiple server. To avoid conflict, only one server can
     * perform provisioning for a domain at one time. This attribute servers
     * a lock for the test-and-set LDAP operation to synchronize EAGER auto
     * provision attempts between servers.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1236)
    public Map<String,Object> unsetAutoProvLock(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, "");
        return attrs;
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @return zimbraAutoProvMode, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public String[] getAutoProvModeAsString() {
        return getMultiAttr(Provisioning.A_zimbraAutoProvMode);
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @param zimbraAutoProvMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public void setAutoProvMode(ZAttrProvisioning.AutoProvMode zimbraAutoProvMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvMode, zimbraAutoProvMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @param zimbraAutoProvMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public Map<String,Object> setAutoProvMode(ZAttrProvisioning.AutoProvMode zimbraAutoProvMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvMode, zimbraAutoProvMode.toString());
        return attrs;
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @param zimbraAutoProvMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public void setAutoProvModeAsString(String[] zimbraAutoProvMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvMode, zimbraAutoProvMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @param zimbraAutoProvMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public Map<String,Object> setAutoProvModeAsString(String[] zimbraAutoProvMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvMode, zimbraAutoProvMode);
        return attrs;
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public void unsetAutoProvMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * <p>Valid values: [MANUAL, LAZY, EAGER]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public Map<String,Object> unsetAutoProvMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvMode, "");
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Email
     * address to put in the From header for the notification email to the
     * newly created account. If not set, no notification email will sent to
     * the newly created account.
     *
     * @return zimbraAutoProvNotificationFromAddress, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1232)
    public String getAutoProvNotificationFromAddress() {
        return getAttr(Provisioning.A_zimbraAutoProvNotificationFromAddress, null);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Email
     * address to put in the From header for the notification email to the
     * newly created account. If not set, no notification email will sent to
     * the newly created account.
     *
     * @param zimbraAutoProvNotificationFromAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1232)
    public void setAutoProvNotificationFromAddress(String zimbraAutoProvNotificationFromAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvNotificationFromAddress, zimbraAutoProvNotificationFromAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Email
     * address to put in the From header for the notification email to the
     * newly created account. If not set, no notification email will sent to
     * the newly created account.
     *
     * @param zimbraAutoProvNotificationFromAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1232)
    public Map<String,Object> setAutoProvNotificationFromAddress(String zimbraAutoProvNotificationFromAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvNotificationFromAddress, zimbraAutoProvNotificationFromAddress);
        return attrs;
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Email
     * address to put in the From header for the notification email to the
     * newly created account. If not set, no notification email will sent to
     * the newly created account.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1232)
    public void unsetAutoProvNotificationFromAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvNotificationFromAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Email
     * address to put in the From header for the notification email to the
     * newly created account. If not set, no notification email will sent to
     * the newly created account.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1232)
    public Map<String,Object> unsetAutoProvNotificationFromAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvNotificationFromAddress, "");
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @return zimbraAvailableSkin, or empty array if unset
     */
    @ZAttr(id=364)
    public String[] getAvailableSkin() {
        return getMultiAttr(Provisioning.A_zimbraAvailableSkin);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=364)
    public void setAvailableSkin(String[] zimbraAvailableSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> setAvailableSkin(String[] zimbraAvailableSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=364)
    public void addAvailableSkin(String zimbraAvailableSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> addAvailableSkin(String zimbraAvailableSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=364)
    public void removeAvailableSkin(String zimbraAvailableSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> removeAvailableSkin(String zimbraAvailableSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=364)
    public void unsetAvailableSkin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableSkin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> unsetAvailableSkin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableSkin, "");
        return attrs;
    }

    /**
     * Realm for the basic auth challenge (WWW-Authenticate) header
     *
     * @return zimbraBasicAuthRealm, or "Zimbra" if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1098)
    public String getBasicAuthRealm() {
        return getAttr(Provisioning.A_zimbraBasicAuthRealm, "Zimbra");
    }

    /**
     * Realm for the basic auth challenge (WWW-Authenticate) header
     *
     * @param zimbraBasicAuthRealm new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1098)
    public void setBasicAuthRealm(String zimbraBasicAuthRealm) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBasicAuthRealm, zimbraBasicAuthRealm);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Realm for the basic auth challenge (WWW-Authenticate) header
     *
     * @param zimbraBasicAuthRealm new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1098)
    public Map<String,Object> setBasicAuthRealm(String zimbraBasicAuthRealm, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBasicAuthRealm, zimbraBasicAuthRealm);
        return attrs;
    }

    /**
     * Realm for the basic auth challenge (WWW-Authenticate) header
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1098)
    public void unsetBasicAuthRealm() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBasicAuthRealm, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Realm for the basic auth challenge (WWW-Authenticate) header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1098)
    public Map<String,Object> unsetBasicAuthRealm(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBasicAuthRealm, "");
        return attrs;
    }

    /**
     * list of disabled fields in calendar location web UI
     *
     * @return zimbraCalendarLocationDisabledFields, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1218)
    public String getCalendarLocationDisabledFields() {
        return getAttr(Provisioning.A_zimbraCalendarLocationDisabledFields, null);
    }

    /**
     * list of disabled fields in calendar location web UI
     *
     * @param zimbraCalendarLocationDisabledFields new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1218)
    public void setCalendarLocationDisabledFields(String zimbraCalendarLocationDisabledFields) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarLocationDisabledFields, zimbraCalendarLocationDisabledFields);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of disabled fields in calendar location web UI
     *
     * @param zimbraCalendarLocationDisabledFields new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1218)
    public Map<String,Object> setCalendarLocationDisabledFields(String zimbraCalendarLocationDisabledFields, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarLocationDisabledFields, zimbraCalendarLocationDisabledFields);
        return attrs;
    }

    /**
     * list of disabled fields in calendar location web UI
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1218)
    public void unsetCalendarLocationDisabledFields() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarLocationDisabledFields, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of disabled fields in calendar location web UI
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1218)
    public Map<String,Object> unsetCalendarLocationDisabledFields(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarLocationDisabledFields, "");
        return attrs;
    }

    /**
     * change password URL
     *
     * @return zimbraChangePasswordURL, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=777)
    public String getChangePasswordURL() {
        return getAttr(Provisioning.A_zimbraChangePasswordURL, null);
    }

    /**
     * change password URL
     *
     * @param zimbraChangePasswordURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=777)
    public void setChangePasswordURL(String zimbraChangePasswordURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChangePasswordURL, zimbraChangePasswordURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * change password URL
     *
     * @param zimbraChangePasswordURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=777)
    public Map<String,Object> setChangePasswordURL(String zimbraChangePasswordURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChangePasswordURL, zimbraChangePasswordURL);
        return attrs;
    }

    /**
     * change password URL
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=777)
    public void unsetChangePasswordURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChangePasswordURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * change password URL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=777)
    public Map<String,Object> unsetChangePasswordURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChangePasswordURL, "");
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
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @return zimbraDNSCheckHostname, or null if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public String getDNSCheckHostname() {
        return getAttr(Provisioning.A_zimbraDNSCheckHostname, null);
    }

    /**
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @param zimbraDNSCheckHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public void setDNSCheckHostname(String zimbraDNSCheckHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDNSCheckHostname, zimbraDNSCheckHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @param zimbraDNSCheckHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public Map<String,Object> setDNSCheckHostname(String zimbraDNSCheckHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDNSCheckHostname, zimbraDNSCheckHostname);
        return attrs;
    }

    /**
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public void unsetDNSCheckHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDNSCheckHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public Map<String,Object> unsetDNSCheckHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDNSCheckHostname, "");
        return attrs;
    }

    /**
     * maximum aggregate quota for the domain in bytes
     *
     * @return zimbraDomainAggregateQuota, or 0 if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1327)
    public long getDomainAggregateQuota() {
        return getLongAttr(Provisioning.A_zimbraDomainAggregateQuota, 0L);
    }

    /**
     * maximum aggregate quota for the domain in bytes
     *
     * @param zimbraDomainAggregateQuota new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1327)
    public void setDomainAggregateQuota(long zimbraDomainAggregateQuota) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuota, Long.toString(zimbraDomainAggregateQuota));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum aggregate quota for the domain in bytes
     *
     * @param zimbraDomainAggregateQuota new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1327)
    public Map<String,Object> setDomainAggregateQuota(long zimbraDomainAggregateQuota, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuota, Long.toString(zimbraDomainAggregateQuota));
        return attrs;
    }

    /**
     * maximum aggregate quota for the domain in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1327)
    public void unsetDomainAggregateQuota() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuota, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum aggregate quota for the domain in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1327)
    public Map<String,Object> unsetDomainAggregateQuota(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuota, "");
        return attrs;
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @return zimbraDomainAggregateQuotaPolicy, or ZAttrProvisioning.DomainAggregateQuotaPolicy.ALLOWSENDRECEIVE if unset and/or has invalid value
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public ZAttrProvisioning.DomainAggregateQuotaPolicy getDomainAggregateQuotaPolicy() {
        try { String v = getAttr(Provisioning.A_zimbraDomainAggregateQuotaPolicy); return v == null ? ZAttrProvisioning.DomainAggregateQuotaPolicy.ALLOWSENDRECEIVE : ZAttrProvisioning.DomainAggregateQuotaPolicy.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.DomainAggregateQuotaPolicy.ALLOWSENDRECEIVE; }
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @return zimbraDomainAggregateQuotaPolicy, or "ALLOWSENDRECEIVE" if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public String getDomainAggregateQuotaPolicyAsString() {
        return getAttr(Provisioning.A_zimbraDomainAggregateQuotaPolicy, "ALLOWSENDRECEIVE");
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @param zimbraDomainAggregateQuotaPolicy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public void setDomainAggregateQuotaPolicy(ZAttrProvisioning.DomainAggregateQuotaPolicy zimbraDomainAggregateQuotaPolicy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaPolicy, zimbraDomainAggregateQuotaPolicy.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @param zimbraDomainAggregateQuotaPolicy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public Map<String,Object> setDomainAggregateQuotaPolicy(ZAttrProvisioning.DomainAggregateQuotaPolicy zimbraDomainAggregateQuotaPolicy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaPolicy, zimbraDomainAggregateQuotaPolicy.toString());
        return attrs;
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @param zimbraDomainAggregateQuotaPolicy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public void setDomainAggregateQuotaPolicyAsString(String zimbraDomainAggregateQuotaPolicy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaPolicy, zimbraDomainAggregateQuotaPolicy);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @param zimbraDomainAggregateQuotaPolicy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public Map<String,Object> setDomainAggregateQuotaPolicyAsString(String zimbraDomainAggregateQuotaPolicy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaPolicy, zimbraDomainAggregateQuotaPolicy);
        return attrs;
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public void unsetDomainAggregateQuotaPolicy() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaPolicy, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * <p>Valid values: [BLOCKSENDRECEIVE, BLOCKSEND, ALLOWSENDRECEIVE]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public Map<String,Object> unsetDomainAggregateQuotaPolicy(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaPolicy, "");
        return attrs;
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @return zimbraDomainAggregateQuotaWarnEmailRecipient, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public String[] getDomainAggregateQuotaWarnEmailRecipient() {
        return getMultiAttr(Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient);
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param zimbraDomainAggregateQuotaWarnEmailRecipient new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public void setDomainAggregateQuotaWarnEmailRecipient(String[] zimbraDomainAggregateQuotaWarnEmailRecipient) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, zimbraDomainAggregateQuotaWarnEmailRecipient);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param zimbraDomainAggregateQuotaWarnEmailRecipient new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public Map<String,Object> setDomainAggregateQuotaWarnEmailRecipient(String[] zimbraDomainAggregateQuotaWarnEmailRecipient, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, zimbraDomainAggregateQuotaWarnEmailRecipient);
        return attrs;
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param zimbraDomainAggregateQuotaWarnEmailRecipient new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public void addDomainAggregateQuotaWarnEmailRecipient(String zimbraDomainAggregateQuotaWarnEmailRecipient) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, zimbraDomainAggregateQuotaWarnEmailRecipient);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param zimbraDomainAggregateQuotaWarnEmailRecipient new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public Map<String,Object> addDomainAggregateQuotaWarnEmailRecipient(String zimbraDomainAggregateQuotaWarnEmailRecipient, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, zimbraDomainAggregateQuotaWarnEmailRecipient);
        return attrs;
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param zimbraDomainAggregateQuotaWarnEmailRecipient existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public void removeDomainAggregateQuotaWarnEmailRecipient(String zimbraDomainAggregateQuotaWarnEmailRecipient) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, zimbraDomainAggregateQuotaWarnEmailRecipient);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param zimbraDomainAggregateQuotaWarnEmailRecipient existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public Map<String,Object> removeDomainAggregateQuotaWarnEmailRecipient(String zimbraDomainAggregateQuotaWarnEmailRecipient, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, zimbraDomainAggregateQuotaWarnEmailRecipient);
        return attrs;
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public void unsetDomainAggregateQuotaWarnEmailRecipient() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public Map<String,Object> unsetDomainAggregateQuotaWarnEmailRecipient(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnEmailRecipient, "");
        return attrs;
    }

    /**
     * percentage threshold for domain aggregate quota warnings
     *
     * @return zimbraDomainAggregateQuotaWarnPercent, or 80 if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1330)
    public int getDomainAggregateQuotaWarnPercent() {
        return getIntAttr(Provisioning.A_zimbraDomainAggregateQuotaWarnPercent, 80);
    }

    /**
     * percentage threshold for domain aggregate quota warnings
     *
     * @param zimbraDomainAggregateQuotaWarnPercent new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1330)
    public void setDomainAggregateQuotaWarnPercent(int zimbraDomainAggregateQuotaWarnPercent) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnPercent, Integer.toString(zimbraDomainAggregateQuotaWarnPercent));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * percentage threshold for domain aggregate quota warnings
     *
     * @param zimbraDomainAggregateQuotaWarnPercent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1330)
    public Map<String,Object> setDomainAggregateQuotaWarnPercent(int zimbraDomainAggregateQuotaWarnPercent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnPercent, Integer.toString(zimbraDomainAggregateQuotaWarnPercent));
        return attrs;
    }

    /**
     * percentage threshold for domain aggregate quota warnings
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1330)
    public void unsetDomainAggregateQuotaWarnPercent() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnPercent, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * percentage threshold for domain aggregate quota warnings
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1330)
    public Map<String,Object> unsetDomainAggregateQuotaWarnPercent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAggregateQuotaWarnPercent, "");
        return attrs;
    }

    /**
     * zimbraId of domain alias target
     *
     * @return zimbraDomainAliasTargetId, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public String getDomainAliasTargetId() {
        return getAttr(Provisioning.A_zimbraDomainAliasTargetId, null);
    }

    /**
     * zimbraId of domain alias target
     *
     * @param zimbraDomainAliasTargetId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public void setDomainAliasTargetId(String zimbraDomainAliasTargetId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, zimbraDomainAliasTargetId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraId of domain alias target
     *
     * @param zimbraDomainAliasTargetId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public Map<String,Object> setDomainAliasTargetId(String zimbraDomainAliasTargetId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, zimbraDomainAliasTargetId);
        return attrs;
    }

    /**
     * zimbraId of domain alias target
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public void unsetDomainAliasTargetId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraId of domain alias target
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public Map<String,Object> unsetDomainAliasTargetId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, "");
        return attrs;
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @return zimbraDomainCOSMaxAccounts, or empty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public String[] getDomainCOSMaxAccounts() {
        return getMultiAttr(Provisioning.A_zimbraDomainCOSMaxAccounts);
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param zimbraDomainCOSMaxAccounts new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public void setDomainCOSMaxAccounts(String[] zimbraDomainCOSMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, zimbraDomainCOSMaxAccounts);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param zimbraDomainCOSMaxAccounts new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public Map<String,Object> setDomainCOSMaxAccounts(String[] zimbraDomainCOSMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, zimbraDomainCOSMaxAccounts);
        return attrs;
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param zimbraDomainCOSMaxAccounts new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public void addDomainCOSMaxAccounts(String zimbraDomainCOSMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainCOSMaxAccounts, zimbraDomainCOSMaxAccounts);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param zimbraDomainCOSMaxAccounts new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public Map<String,Object> addDomainCOSMaxAccounts(String zimbraDomainCOSMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainCOSMaxAccounts, zimbraDomainCOSMaxAccounts);
        return attrs;
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param zimbraDomainCOSMaxAccounts existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public void removeDomainCOSMaxAccounts(String zimbraDomainCOSMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainCOSMaxAccounts, zimbraDomainCOSMaxAccounts);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param zimbraDomainCOSMaxAccounts existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public Map<String,Object> removeDomainCOSMaxAccounts(String zimbraDomainCOSMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainCOSMaxAccounts, zimbraDomainCOSMaxAccounts);
        return attrs;
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public void unsetDomainCOSMaxAccounts() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public Map<String,Object> unsetDomainCOSMaxAccounts(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, "");
        return attrs;
    }

    /**
     * COS zimbraID
     *
     * @return zimbraDomainDefaultCOSId, or null if unset
     */
    @ZAttr(id=299)
    public String getDomainDefaultCOSId() {
        return getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null);
    }

    /**
     * COS zimbraID
     *
     * @param zimbraDomainDefaultCOSId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=299)
    public void setDomainDefaultCOSId(String zimbraDomainDefaultCOSId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultCOSId, zimbraDomainDefaultCOSId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * COS zimbraID
     *
     * @param zimbraDomainDefaultCOSId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=299)
    public Map<String,Object> setDomainDefaultCOSId(String zimbraDomainDefaultCOSId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultCOSId, zimbraDomainDefaultCOSId);
        return attrs;
    }

    /**
     * COS zimbraID
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=299)
    public void unsetDomainDefaultCOSId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultCOSId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * COS zimbraID
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=299)
    public Map<String,Object> unsetDomainDefaultCOSId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultCOSId, "");
        return attrs;
    }

    /**
     * id of the default COS for external user accounts
     *
     * @return zimbraDomainDefaultExternalUserCOSId, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1247)
    public String getDomainDefaultExternalUserCOSId() {
        return getAttr(Provisioning.A_zimbraDomainDefaultExternalUserCOSId, null);
    }

    /**
     * id of the default COS for external user accounts
     *
     * @param zimbraDomainDefaultExternalUserCOSId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1247)
    public void setDomainDefaultExternalUserCOSId(String zimbraDomainDefaultExternalUserCOSId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultExternalUserCOSId, zimbraDomainDefaultExternalUserCOSId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * id of the default COS for external user accounts
     *
     * @param zimbraDomainDefaultExternalUserCOSId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1247)
    public Map<String,Object> setDomainDefaultExternalUserCOSId(String zimbraDomainDefaultExternalUserCOSId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultExternalUserCOSId, zimbraDomainDefaultExternalUserCOSId);
        return attrs;
    }

    /**
     * id of the default COS for external user accounts
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1247)
    public void unsetDomainDefaultExternalUserCOSId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultExternalUserCOSId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * id of the default COS for external user accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1247)
    public Map<String,Object> unsetDomainDefaultExternalUserCOSId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainDefaultExternalUserCOSId, "");
        return attrs;
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @return zimbraDomainFeatureMaxAccounts, or empty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public String[] getDomainFeatureMaxAccounts() {
        return getMultiAttr(Provisioning.A_zimbraDomainFeatureMaxAccounts);
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param zimbraDomainFeatureMaxAccounts new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public void setDomainFeatureMaxAccounts(String[] zimbraDomainFeatureMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainFeatureMaxAccounts, zimbraDomainFeatureMaxAccounts);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param zimbraDomainFeatureMaxAccounts new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public Map<String,Object> setDomainFeatureMaxAccounts(String[] zimbraDomainFeatureMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainFeatureMaxAccounts, zimbraDomainFeatureMaxAccounts);
        return attrs;
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param zimbraDomainFeatureMaxAccounts new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public void addDomainFeatureMaxAccounts(String zimbraDomainFeatureMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainFeatureMaxAccounts, zimbraDomainFeatureMaxAccounts);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param zimbraDomainFeatureMaxAccounts new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public Map<String,Object> addDomainFeatureMaxAccounts(String zimbraDomainFeatureMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainFeatureMaxAccounts, zimbraDomainFeatureMaxAccounts);
        return attrs;
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param zimbraDomainFeatureMaxAccounts existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public void removeDomainFeatureMaxAccounts(String zimbraDomainFeatureMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainFeatureMaxAccounts, zimbraDomainFeatureMaxAccounts);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param zimbraDomainFeatureMaxAccounts existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public Map<String,Object> removeDomainFeatureMaxAccounts(String zimbraDomainFeatureMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainFeatureMaxAccounts, zimbraDomainFeatureMaxAccounts);
        return attrs;
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public void unsetDomainFeatureMaxAccounts() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainFeatureMaxAccounts, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public Map<String,Object> unsetDomainFeatureMaxAccounts(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainFeatureMaxAccounts, "");
        return attrs;
    }

    /**
     * enable domain mandatory mail signature
     *
     * @return zimbraDomainMandatoryMailSignatureEnabled, or false if unset
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1069)
    public boolean isDomainMandatoryMailSignatureEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraDomainMandatoryMailSignatureEnabled, false);
    }

    /**
     * enable domain mandatory mail signature
     *
     * @param zimbraDomainMandatoryMailSignatureEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1069)
    public void setDomainMandatoryMailSignatureEnabled(boolean zimbraDomainMandatoryMailSignatureEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureEnabled, zimbraDomainMandatoryMailSignatureEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable domain mandatory mail signature
     *
     * @param zimbraDomainMandatoryMailSignatureEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1069)
    public Map<String,Object> setDomainMandatoryMailSignatureEnabled(boolean zimbraDomainMandatoryMailSignatureEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureEnabled, zimbraDomainMandatoryMailSignatureEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * enable domain mandatory mail signature
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1069)
    public void unsetDomainMandatoryMailSignatureEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable domain mandatory mail signature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1069)
    public Map<String,Object> unsetDomainMandatoryMailSignatureEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureEnabled, "");
        return attrs;
    }

    /**
     * domain mandatory mail html signature
     *
     * @return zimbraDomainMandatoryMailSignatureHTML, or null if unset
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1071)
    public String getDomainMandatoryMailSignatureHTML() {
        return getAttr(Provisioning.A_zimbraDomainMandatoryMailSignatureHTML, null);
    }

    /**
     * domain mandatory mail html signature
     *
     * @param zimbraDomainMandatoryMailSignatureHTML new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1071)
    public void setDomainMandatoryMailSignatureHTML(String zimbraDomainMandatoryMailSignatureHTML) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureHTML, zimbraDomainMandatoryMailSignatureHTML);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain mandatory mail html signature
     *
     * @param zimbraDomainMandatoryMailSignatureHTML new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1071)
    public Map<String,Object> setDomainMandatoryMailSignatureHTML(String zimbraDomainMandatoryMailSignatureHTML, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureHTML, zimbraDomainMandatoryMailSignatureHTML);
        return attrs;
    }

    /**
     * domain mandatory mail html signature
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1071)
    public void unsetDomainMandatoryMailSignatureHTML() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureHTML, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain mandatory mail html signature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1071)
    public Map<String,Object> unsetDomainMandatoryMailSignatureHTML(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureHTML, "");
        return attrs;
    }

    /**
     * domain mandatory mail plain text signature
     *
     * @return zimbraDomainMandatoryMailSignatureText, or null if unset
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1070)
    public String getDomainMandatoryMailSignatureText() {
        return getAttr(Provisioning.A_zimbraDomainMandatoryMailSignatureText, null);
    }

    /**
     * domain mandatory mail plain text signature
     *
     * @param zimbraDomainMandatoryMailSignatureText new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1070)
    public void setDomainMandatoryMailSignatureText(String zimbraDomainMandatoryMailSignatureText) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureText, zimbraDomainMandatoryMailSignatureText);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain mandatory mail plain text signature
     *
     * @param zimbraDomainMandatoryMailSignatureText new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1070)
    public Map<String,Object> setDomainMandatoryMailSignatureText(String zimbraDomainMandatoryMailSignatureText, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureText, zimbraDomainMandatoryMailSignatureText);
        return attrs;
    }

    /**
     * domain mandatory mail plain text signature
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1070)
    public void unsetDomainMandatoryMailSignatureText() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureText, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain mandatory mail plain text signature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1070)
    public Map<String,Object> unsetDomainMandatoryMailSignatureText(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMandatoryMailSignatureText, "");
        return attrs;
    }

    /**
     * maximum number of accounts allowed in a domain
     *
     * @return zimbraDomainMaxAccounts, or -1 if unset
     */
    @ZAttr(id=400)
    public int getDomainMaxAccounts() {
        return getIntAttr(Provisioning.A_zimbraDomainMaxAccounts, -1);
    }

    /**
     * maximum number of accounts allowed in a domain
     *
     * @param zimbraDomainMaxAccounts new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=400)
    public void setDomainMaxAccounts(int zimbraDomainMaxAccounts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMaxAccounts, Integer.toString(zimbraDomainMaxAccounts));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed in a domain
     *
     * @param zimbraDomainMaxAccounts new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=400)
    public Map<String,Object> setDomainMaxAccounts(int zimbraDomainMaxAccounts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMaxAccounts, Integer.toString(zimbraDomainMaxAccounts));
        return attrs;
    }

    /**
     * maximum number of accounts allowed in a domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=400)
    public void unsetDomainMaxAccounts() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMaxAccounts, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of accounts allowed in a domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=400)
    public Map<String,Object> unsetDomainMaxAccounts(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainMaxAccounts, "");
        return attrs;
    }

    /**
     * name of the domain
     *
     * @return zimbraDomainName, or null if unset
     */
    @ZAttr(id=19)
    public String getDomainName() {
        return getAttr(Provisioning.A_zimbraDomainName, null);
    }

    /**
     * name of the domain
     *
     * @param zimbraDomainName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=19)
    public void setDomainName(String zimbraDomainName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainName, zimbraDomainName);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * name of the domain
     *
     * @param zimbraDomainName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=19)
    public Map<String,Object> setDomainName(String zimbraDomainName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainName, zimbraDomainName);
        return attrs;
    }

    /**
     * name of the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=19)
    public void unsetDomainName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainName, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * name of the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=19)
    public Map<String,Object> unsetDomainName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainName, "");
        return attrs;
    }

    /**
     * domain rename info/status
     *
     * @return zimbraDomainRenameInfo, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=536)
    public String getDomainRenameInfo() {
        return getAttr(Provisioning.A_zimbraDomainRenameInfo, null);
    }

    /**
     * domain rename info/status
     *
     * @param zimbraDomainRenameInfo new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=536)
    public void setDomainRenameInfo(String zimbraDomainRenameInfo) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, zimbraDomainRenameInfo);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain rename info/status
     *
     * @param zimbraDomainRenameInfo new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=536)
    public Map<String,Object> setDomainRenameInfo(String zimbraDomainRenameInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, zimbraDomainRenameInfo);
        return attrs;
    }

    /**
     * domain rename info/status
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=536)
    public void unsetDomainRenameInfo() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain rename info/status
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=536)
    public Map<String,Object> unsetDomainRenameInfo(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, "");
        return attrs;
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @return zimbraDomainStatus, or null if unset and/or has invalid value
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public ZAttrProvisioning.DomainStatus getDomainStatus() {
        try { String v = getAttr(Provisioning.A_zimbraDomainStatus); return v == null ? null : ZAttrProvisioning.DomainStatus.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @return zimbraDomainStatus, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public String getDomainStatusAsString() {
        return getAttr(Provisioning.A_zimbraDomainStatus, null);
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public void setDomainStatus(ZAttrProvisioning.DomainStatus zimbraDomainStatus) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, zimbraDomainStatus.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public Map<String,Object> setDomainStatus(ZAttrProvisioning.DomainStatus zimbraDomainStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, zimbraDomainStatus.toString());
        return attrs;
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public void setDomainStatusAsString(String zimbraDomainStatus) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, zimbraDomainStatus);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public Map<String,Object> setDomainStatusAsString(String zimbraDomainStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, zimbraDomainStatus);
        return attrs;
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public void unsetDomainStatus() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP entries. Modification
     * and deletion of the domain can only be done internally by the server
     * when it is safe to release the domain, they cannot be done in admin
     * console or zmprov. How zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public Map<String,Object> unsetDomainStatus(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, "");
        return attrs;
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @return zimbraDomainType, or null if unset and/or has invalid value
     */
    @ZAttr(id=212)
    public ZAttrProvisioning.DomainType getDomainType() {
        try { String v = getAttr(Provisioning.A_zimbraDomainType); return v == null ? null : ZAttrProvisioning.DomainType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @return zimbraDomainType, or null if unset
     */
    @ZAttr(id=212)
    public String getDomainTypeAsString() {
        return getAttr(Provisioning.A_zimbraDomainType, null);
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @param zimbraDomainType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=212)
    public void setDomainType(ZAttrProvisioning.DomainType zimbraDomainType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainType, zimbraDomainType.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @param zimbraDomainType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=212)
    public Map<String,Object> setDomainType(ZAttrProvisioning.DomainType zimbraDomainType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainType, zimbraDomainType.toString());
        return attrs;
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @param zimbraDomainType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=212)
    public void setDomainTypeAsString(String zimbraDomainType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainType, zimbraDomainType);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @param zimbraDomainType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=212)
    public Map<String,Object> setDomainTypeAsString(String zimbraDomainType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainType, zimbraDomainType);
        return attrs;
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=212)
    public void unsetDomainType() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainType, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * should be one of: local, alias
     *
     * <p>Valid values: [alias, local]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=212)
    public Map<String,Object> unsetDomainType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainType, "");
        return attrs;
    }

    /**
     * URL for posting error report popped up in WEB client
     *
     * @return zimbraErrorReportUrl, or null if unset
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1075)
    public String getErrorReportUrl() {
        return getAttr(Provisioning.A_zimbraErrorReportUrl, null);
    }

    /**
     * URL for posting error report popped up in WEB client
     *
     * @param zimbraErrorReportUrl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1075)
    public void setErrorReportUrl(String zimbraErrorReportUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraErrorReportUrl, zimbraErrorReportUrl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL for posting error report popped up in WEB client
     *
     * @param zimbraErrorReportUrl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1075)
    public Map<String,Object> setErrorReportUrl(String zimbraErrorReportUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraErrorReportUrl, zimbraErrorReportUrl);
        return attrs;
    }

    /**
     * URL for posting error report popped up in WEB client
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1075)
    public void unsetErrorReportUrl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraErrorReportUrl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL for posting error report popped up in WEB client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1075)
    public Map<String,Object> unsetErrorReportUrl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraErrorReportUrl, "");
        return attrs;
    }

    /**
     * the handler class for getting all groups an account belongs to in the
     * external directory
     *
     * @return zimbraExternalGroupHandlerClass, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1251)
    public String getExternalGroupHandlerClass() {
        return getAttr(Provisioning.A_zimbraExternalGroupHandlerClass, null);
    }

    /**
     * the handler class for getting all groups an account belongs to in the
     * external directory
     *
     * @param zimbraExternalGroupHandlerClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1251)
    public void setExternalGroupHandlerClass(String zimbraExternalGroupHandlerClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupHandlerClass, zimbraExternalGroupHandlerClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the handler class for getting all groups an account belongs to in the
     * external directory
     *
     * @param zimbraExternalGroupHandlerClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1251)
    public Map<String,Object> setExternalGroupHandlerClass(String zimbraExternalGroupHandlerClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupHandlerClass, zimbraExternalGroupHandlerClass);
        return attrs;
    }

    /**
     * the handler class for getting all groups an account belongs to in the
     * external directory
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1251)
    public void unsetExternalGroupHandlerClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupHandlerClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the handler class for getting all groups an account belongs to in the
     * external directory
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1251)
    public Map<String,Object> unsetExternalGroupHandlerClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupHandlerClass, "");
        return attrs;
    }

    /**
     * LDAP search base for searching external LDAP groups
     *
     * @return zimbraExternalGroupLdapSearchBase, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1249)
    public String getExternalGroupLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraExternalGroupLdapSearchBase, null);
    }

    /**
     * LDAP search base for searching external LDAP groups
     *
     * @param zimbraExternalGroupLdapSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1249)
    public void setExternalGroupLdapSearchBase(String zimbraExternalGroupLdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchBase, zimbraExternalGroupLdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for searching external LDAP groups
     *
     * @param zimbraExternalGroupLdapSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1249)
    public Map<String,Object> setExternalGroupLdapSearchBase(String zimbraExternalGroupLdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchBase, zimbraExternalGroupLdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for searching external LDAP groups
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1249)
    public void unsetExternalGroupLdapSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for searching external LDAP groups
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1249)
    public Map<String,Object> unsetExternalGroupLdapSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchBase, "");
        return attrs;
    }

    /**
     * LDAP search filter for searching external LDAP groups
     *
     * @return zimbraExternalGroupLdapSearchFilter, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1250)
    public String getExternalGroupLdapSearchFilter() {
        return getAttr(Provisioning.A_zimbraExternalGroupLdapSearchFilter, null);
    }

    /**
     * LDAP search filter for searching external LDAP groups
     *
     * @param zimbraExternalGroupLdapSearchFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1250)
    public void setExternalGroupLdapSearchFilter(String zimbraExternalGroupLdapSearchFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchFilter, zimbraExternalGroupLdapSearchFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for searching external LDAP groups
     *
     * @param zimbraExternalGroupLdapSearchFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1250)
    public Map<String,Object> setExternalGroupLdapSearchFilter(String zimbraExternalGroupLdapSearchFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchFilter, zimbraExternalGroupLdapSearchFilter);
        return attrs;
    }

    /**
     * LDAP search filter for searching external LDAP groups
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1250)
    public void unsetExternalGroupLdapSearchFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for searching external LDAP groups
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1250)
    public Map<String,Object> unsetExternalGroupLdapSearchFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalGroupLdapSearchFilter, "");
        return attrs;
    }

    /**
     * external imap hostname
     *
     * @return zimbraExternalImapHostname, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=786)
    public String getExternalImapHostname() {
        return getAttr(Provisioning.A_zimbraExternalImapHostname, null);
    }

    /**
     * external imap hostname
     *
     * @param zimbraExternalImapHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=786)
    public void setExternalImapHostname(String zimbraExternalImapHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapHostname, zimbraExternalImapHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap hostname
     *
     * @param zimbraExternalImapHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=786)
    public Map<String,Object> setExternalImapHostname(String zimbraExternalImapHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapHostname, zimbraExternalImapHostname);
        return attrs;
    }

    /**
     * external imap hostname
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=786)
    public void unsetExternalImapHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=786)
    public Map<String,Object> unsetExternalImapHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapHostname, "");
        return attrs;
    }

    /**
     * external imap port
     *
     * <p>Use getExternalImapPortAsString to access value as a string.
     *
     * @see #getExternalImapPortAsString()
     *
     * @return zimbraExternalImapPort, or -1 if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public int getExternalImapPort() {
        return getIntAttr(Provisioning.A_zimbraExternalImapPort, -1);
    }

    /**
     * external imap port
     *
     * @return zimbraExternalImapPort, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public String getExternalImapPortAsString() {
        return getAttr(Provisioning.A_zimbraExternalImapPort, null);
    }

    /**
     * external imap port
     *
     * @param zimbraExternalImapPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public void setExternalImapPort(int zimbraExternalImapPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapPort, Integer.toString(zimbraExternalImapPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap port
     *
     * @param zimbraExternalImapPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public Map<String,Object> setExternalImapPort(int zimbraExternalImapPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapPort, Integer.toString(zimbraExternalImapPort));
        return attrs;
    }

    /**
     * external imap port
     *
     * @param zimbraExternalImapPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public void setExternalImapPortAsString(String zimbraExternalImapPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapPort, zimbraExternalImapPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap port
     *
     * @param zimbraExternalImapPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public Map<String,Object> setExternalImapPortAsString(String zimbraExternalImapPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapPort, zimbraExternalImapPort);
        return attrs;
    }

    /**
     * external imap port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public void unsetExternalImapPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public Map<String,Object> unsetExternalImapPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapPort, "");
        return attrs;
    }

    /**
     * external imap SSL hostname
     *
     * @return zimbraExternalImapSSLHostname, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=787)
    public String getExternalImapSSLHostname() {
        return getAttr(Provisioning.A_zimbraExternalImapSSLHostname, null);
    }

    /**
     * external imap SSL hostname
     *
     * @param zimbraExternalImapSSLHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=787)
    public void setExternalImapSSLHostname(String zimbraExternalImapSSLHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLHostname, zimbraExternalImapSSLHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap SSL hostname
     *
     * @param zimbraExternalImapSSLHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=787)
    public Map<String,Object> setExternalImapSSLHostname(String zimbraExternalImapSSLHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLHostname, zimbraExternalImapSSLHostname);
        return attrs;
    }

    /**
     * external imap SSL hostname
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=787)
    public void unsetExternalImapSSLHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap SSL hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=787)
    public Map<String,Object> unsetExternalImapSSLHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLHostname, "");
        return attrs;
    }

    /**
     * external imap SSL port
     *
     * <p>Use getExternalImapSSLPortAsString to access value as a string.
     *
     * @see #getExternalImapSSLPortAsString()
     *
     * @return zimbraExternalImapSSLPort, or -1 if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public int getExternalImapSSLPort() {
        return getIntAttr(Provisioning.A_zimbraExternalImapSSLPort, -1);
    }

    /**
     * external imap SSL port
     *
     * @return zimbraExternalImapSSLPort, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public String getExternalImapSSLPortAsString() {
        return getAttr(Provisioning.A_zimbraExternalImapSSLPort, null);
    }

    /**
     * external imap SSL port
     *
     * @param zimbraExternalImapSSLPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public void setExternalImapSSLPort(int zimbraExternalImapSSLPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLPort, Integer.toString(zimbraExternalImapSSLPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap SSL port
     *
     * @param zimbraExternalImapSSLPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public Map<String,Object> setExternalImapSSLPort(int zimbraExternalImapSSLPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLPort, Integer.toString(zimbraExternalImapSSLPort));
        return attrs;
    }

    /**
     * external imap SSL port
     *
     * @param zimbraExternalImapSSLPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public void setExternalImapSSLPortAsString(String zimbraExternalImapSSLPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLPort, zimbraExternalImapSSLPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap SSL port
     *
     * @param zimbraExternalImapSSLPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public Map<String,Object> setExternalImapSSLPortAsString(String zimbraExternalImapSSLPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLPort, zimbraExternalImapSSLPort);
        return attrs;
    }

    /**
     * external imap SSL port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public void unsetExternalImapSSLPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external imap SSL port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public Map<String,Object> unsetExternalImapSSLPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalImapSSLPort, "");
        return attrs;
    }

    /**
     * external pop3 hostname
     *
     * @return zimbraExternalPop3Hostname, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=784)
    public String getExternalPop3Hostname() {
        return getAttr(Provisioning.A_zimbraExternalPop3Hostname, null);
    }

    /**
     * external pop3 hostname
     *
     * @param zimbraExternalPop3Hostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=784)
    public void setExternalPop3Hostname(String zimbraExternalPop3Hostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Hostname, zimbraExternalPop3Hostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 hostname
     *
     * @param zimbraExternalPop3Hostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=784)
    public Map<String,Object> setExternalPop3Hostname(String zimbraExternalPop3Hostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Hostname, zimbraExternalPop3Hostname);
        return attrs;
    }

    /**
     * external pop3 hostname
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=784)
    public void unsetExternalPop3Hostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Hostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=784)
    public Map<String,Object> unsetExternalPop3Hostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Hostname, "");
        return attrs;
    }

    /**
     * external pop3 port
     *
     * <p>Use getExternalPop3PortAsString to access value as a string.
     *
     * @see #getExternalPop3PortAsString()
     *
     * @return zimbraExternalPop3Port, or -1 if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public int getExternalPop3Port() {
        return getIntAttr(Provisioning.A_zimbraExternalPop3Port, -1);
    }

    /**
     * external pop3 port
     *
     * @return zimbraExternalPop3Port, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public String getExternalPop3PortAsString() {
        return getAttr(Provisioning.A_zimbraExternalPop3Port, null);
    }

    /**
     * external pop3 port
     *
     * @param zimbraExternalPop3Port new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public void setExternalPop3Port(int zimbraExternalPop3Port) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Port, Integer.toString(zimbraExternalPop3Port));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 port
     *
     * @param zimbraExternalPop3Port new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public Map<String,Object> setExternalPop3Port(int zimbraExternalPop3Port, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Port, Integer.toString(zimbraExternalPop3Port));
        return attrs;
    }

    /**
     * external pop3 port
     *
     * @param zimbraExternalPop3Port new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public void setExternalPop3PortAsString(String zimbraExternalPop3Port) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Port, zimbraExternalPop3Port);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 port
     *
     * @param zimbraExternalPop3Port new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public Map<String,Object> setExternalPop3PortAsString(String zimbraExternalPop3Port, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Port, zimbraExternalPop3Port);
        return attrs;
    }

    /**
     * external pop3 port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public void unsetExternalPop3Port() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Port, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public Map<String,Object> unsetExternalPop3Port(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3Port, "");
        return attrs;
    }

    /**
     * external pop3 SSL hostname
     *
     * @return zimbraExternalPop3SSLHostname, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=785)
    public String getExternalPop3SSLHostname() {
        return getAttr(Provisioning.A_zimbraExternalPop3SSLHostname, null);
    }

    /**
     * external pop3 SSL hostname
     *
     * @param zimbraExternalPop3SSLHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=785)
    public void setExternalPop3SSLHostname(String zimbraExternalPop3SSLHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLHostname, zimbraExternalPop3SSLHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 SSL hostname
     *
     * @param zimbraExternalPop3SSLHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=785)
    public Map<String,Object> setExternalPop3SSLHostname(String zimbraExternalPop3SSLHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLHostname, zimbraExternalPop3SSLHostname);
        return attrs;
    }

    /**
     * external pop3 SSL hostname
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=785)
    public void unsetExternalPop3SSLHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 SSL hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=785)
    public Map<String,Object> unsetExternalPop3SSLHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLHostname, "");
        return attrs;
    }

    /**
     * external pop3 SSL port
     *
     * <p>Use getExternalPop3SSLPortAsString to access value as a string.
     *
     * @see #getExternalPop3SSLPortAsString()
     *
     * @return zimbraExternalPop3SSLPort, or -1 if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public int getExternalPop3SSLPort() {
        return getIntAttr(Provisioning.A_zimbraExternalPop3SSLPort, -1);
    }

    /**
     * external pop3 SSL port
     *
     * @return zimbraExternalPop3SSLPort, or null if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public String getExternalPop3SSLPortAsString() {
        return getAttr(Provisioning.A_zimbraExternalPop3SSLPort, null);
    }

    /**
     * external pop3 SSL port
     *
     * @param zimbraExternalPop3SSLPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public void setExternalPop3SSLPort(int zimbraExternalPop3SSLPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLPort, Integer.toString(zimbraExternalPop3SSLPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 SSL port
     *
     * @param zimbraExternalPop3SSLPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public Map<String,Object> setExternalPop3SSLPort(int zimbraExternalPop3SSLPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLPort, Integer.toString(zimbraExternalPop3SSLPort));
        return attrs;
    }

    /**
     * external pop3 SSL port
     *
     * @param zimbraExternalPop3SSLPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public void setExternalPop3SSLPortAsString(String zimbraExternalPop3SSLPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLPort, zimbraExternalPop3SSLPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 SSL port
     *
     * @param zimbraExternalPop3SSLPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public Map<String,Object> setExternalPop3SSLPortAsString(String zimbraExternalPop3SSLPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLPort, zimbraExternalPop3SSLPort);
        return attrs;
    }

    /**
     * external pop3 SSL port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public void unsetExternalPop3SSLPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external pop3 SSL port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public Map<String,Object> unsetExternalPop3SSLPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalPop3SSLPort, "");
        return attrs;
    }

    /**
     * whether checking against zimbraExternalShareWhitelistDomain for
     * external sharing is enabled
     *
     * @return zimbraExternalShareDomainWhitelistEnabled, or false if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1264)
    public boolean isExternalShareDomainWhitelistEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraExternalShareDomainWhitelistEnabled, false);
    }

    /**
     * whether checking against zimbraExternalShareWhitelistDomain for
     * external sharing is enabled
     *
     * @param zimbraExternalShareDomainWhitelistEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1264)
    public void setExternalShareDomainWhitelistEnabled(boolean zimbraExternalShareDomainWhitelistEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareDomainWhitelistEnabled, zimbraExternalShareDomainWhitelistEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether checking against zimbraExternalShareWhitelistDomain for
     * external sharing is enabled
     *
     * @param zimbraExternalShareDomainWhitelistEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1264)
    public Map<String,Object> setExternalShareDomainWhitelistEnabled(boolean zimbraExternalShareDomainWhitelistEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareDomainWhitelistEnabled, zimbraExternalShareDomainWhitelistEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether checking against zimbraExternalShareWhitelistDomain for
     * external sharing is enabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1264)
    public void unsetExternalShareDomainWhitelistEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareDomainWhitelistEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether checking against zimbraExternalShareWhitelistDomain for
     * external sharing is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1264)
    public Map<String,Object> unsetExternalShareDomainWhitelistEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareDomainWhitelistEnabled, "");
        return attrs;
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @return zimbraExternalShareWhitelistDomain, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public String[] getExternalShareWhitelistDomain() {
        return getMultiAttr(Provisioning.A_zimbraExternalShareWhitelistDomain);
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param zimbraExternalShareWhitelistDomain new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public void setExternalShareWhitelistDomain(String[] zimbraExternalShareWhitelistDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareWhitelistDomain, zimbraExternalShareWhitelistDomain);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param zimbraExternalShareWhitelistDomain new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public Map<String,Object> setExternalShareWhitelistDomain(String[] zimbraExternalShareWhitelistDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareWhitelistDomain, zimbraExternalShareWhitelistDomain);
        return attrs;
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param zimbraExternalShareWhitelistDomain new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public void addExternalShareWhitelistDomain(String zimbraExternalShareWhitelistDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraExternalShareWhitelistDomain, zimbraExternalShareWhitelistDomain);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param zimbraExternalShareWhitelistDomain new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public Map<String,Object> addExternalShareWhitelistDomain(String zimbraExternalShareWhitelistDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraExternalShareWhitelistDomain, zimbraExternalShareWhitelistDomain);
        return attrs;
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param zimbraExternalShareWhitelistDomain existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public void removeExternalShareWhitelistDomain(String zimbraExternalShareWhitelistDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraExternalShareWhitelistDomain, zimbraExternalShareWhitelistDomain);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param zimbraExternalShareWhitelistDomain existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public Map<String,Object> removeExternalShareWhitelistDomain(String zimbraExternalShareWhitelistDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraExternalShareWhitelistDomain, zimbraExternalShareWhitelistDomain);
        return attrs;
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public void unsetExternalShareWhitelistDomain() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareWhitelistDomain, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of external domains that users can share files and folders with
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public Map<String,Object> unsetExternalShareWhitelistDomain(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalShareWhitelistDomain, "");
        return attrs;
    }

    /**
     * switch for turning external sharing on/off
     *
     * @return zimbraExternalSharingEnabled, or false if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1261)
    public boolean isExternalSharingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraExternalSharingEnabled, false);
    }

    /**
     * switch for turning external sharing on/off
     *
     * @param zimbraExternalSharingEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1261)
    public void setExternalSharingEnabled(boolean zimbraExternalSharingEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalSharingEnabled, zimbraExternalSharingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * switch for turning external sharing on/off
     *
     * @param zimbraExternalSharingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1261)
    public Map<String,Object> setExternalSharingEnabled(boolean zimbraExternalSharingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalSharingEnabled, zimbraExternalSharingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * switch for turning external sharing on/off
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1261)
    public void unsetExternalSharingEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalSharingEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * switch for turning external sharing on/off
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1261)
    public Map<String,Object> unsetExternalSharingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExternalSharingEnabled, "");
        return attrs;
    }

    /**
     * whether receiving reminders on the designated device for appointments
     * and tasks is enabled
     *
     * @return zimbraFeatureCalendarReminderDeviceEmailEnabled, or false if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1150)
    public boolean isFeatureCalendarReminderDeviceEmailEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureCalendarReminderDeviceEmailEnabled, false);
    }

    /**
     * whether receiving reminders on the designated device for appointments
     * and tasks is enabled
     *
     * @param zimbraFeatureCalendarReminderDeviceEmailEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1150)
    public void setFeatureCalendarReminderDeviceEmailEnabled(boolean zimbraFeatureCalendarReminderDeviceEmailEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarReminderDeviceEmailEnabled, zimbraFeatureCalendarReminderDeviceEmailEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether receiving reminders on the designated device for appointments
     * and tasks is enabled
     *
     * @param zimbraFeatureCalendarReminderDeviceEmailEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1150)
    public Map<String,Object> setFeatureCalendarReminderDeviceEmailEnabled(boolean zimbraFeatureCalendarReminderDeviceEmailEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarReminderDeviceEmailEnabled, zimbraFeatureCalendarReminderDeviceEmailEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether receiving reminders on the designated device for appointments
     * and tasks is enabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1150)
    public void unsetFeatureCalendarReminderDeviceEmailEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarReminderDeviceEmailEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether receiving reminders on the designated device for appointments
     * and tasks is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1150)
    public Map<String,Object> unsetFeatureCalendarReminderDeviceEmailEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarReminderDeviceEmailEnabled, "");
        return attrs;
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @return zimbraForeignName, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public String[] getForeignName() {
        return getMultiAttr(Provisioning.A_zimbraForeignName);
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param zimbraForeignName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public void setForeignName(String[] zimbraForeignName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignName, zimbraForeignName);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param zimbraForeignName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public Map<String,Object> setForeignName(String[] zimbraForeignName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignName, zimbraForeignName);
        return attrs;
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param zimbraForeignName new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public void addForeignName(String zimbraForeignName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraForeignName, zimbraForeignName);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param zimbraForeignName new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public Map<String,Object> addForeignName(String zimbraForeignName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraForeignName, zimbraForeignName);
        return attrs;
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param zimbraForeignName existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public void removeForeignName(String zimbraForeignName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraForeignName, zimbraForeignName);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param zimbraForeignName existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public Map<String,Object> removeForeignName(String zimbraForeignName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraForeignName, zimbraForeignName);
        return attrs;
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public void unsetForeignName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignName, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public Map<String,Object> unsetForeignName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignName, "");
        return attrs;
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @return zimbraForeignNameHandler, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public String[] getForeignNameHandler() {
        return getMultiAttr(Provisioning.A_zimbraForeignNameHandler);
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param zimbraForeignNameHandler new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public void setForeignNameHandler(String[] zimbraForeignNameHandler) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignNameHandler, zimbraForeignNameHandler);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param zimbraForeignNameHandler new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public Map<String,Object> setForeignNameHandler(String[] zimbraForeignNameHandler, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignNameHandler, zimbraForeignNameHandler);
        return attrs;
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param zimbraForeignNameHandler new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public void addForeignNameHandler(String zimbraForeignNameHandler) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraForeignNameHandler, zimbraForeignNameHandler);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param zimbraForeignNameHandler new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public Map<String,Object> addForeignNameHandler(String zimbraForeignNameHandler, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraForeignNameHandler, zimbraForeignNameHandler);
        return attrs;
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param zimbraForeignNameHandler existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public void removeForeignNameHandler(String zimbraForeignNameHandler) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraForeignNameHandler, zimbraForeignNameHandler);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param zimbraForeignNameHandler existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public Map<String,Object> removeForeignNameHandler(String zimbraForeignNameHandler, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraForeignNameHandler, zimbraForeignNameHandler);
        return attrs;
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public void unsetForeignNameHandler() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignNameHandler, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public Map<String,Object> unsetForeignNameHandler(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignNameHandler, "");
        return attrs;
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthPassword, or null if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=609)
    public String getFreebusyExchangeAuthPassword() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword, null);
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeAuthPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=609)
    public void setFreebusyExchangeAuthPassword(String zimbraFreebusyExchangeAuthPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthPassword, zimbraFreebusyExchangeAuthPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeAuthPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=609)
    public Map<String,Object> setFreebusyExchangeAuthPassword(String zimbraFreebusyExchangeAuthPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthPassword, zimbraFreebusyExchangeAuthPassword);
        return attrs;
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=609)
    public void unsetFreebusyExchangeAuthPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=609)
    public Map<String,Object> unsetFreebusyExchangeAuthPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthPassword, "");
        return attrs;
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null if unset and/or has invalid value
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public ZAttrProvisioning.FreebusyExchangeAuthScheme getFreebusyExchangeAuthScheme() {
        try { String v = getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme); return v == null ? null : ZAttrProvisioning.FreebusyExchangeAuthScheme.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public String getFreebusyExchangeAuthSchemeAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme, null);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public void setFreebusyExchangeAuthScheme(ZAttrProvisioning.FreebusyExchangeAuthScheme zimbraFreebusyExchangeAuthScheme) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, zimbraFreebusyExchangeAuthScheme.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public Map<String,Object> setFreebusyExchangeAuthScheme(ZAttrProvisioning.FreebusyExchangeAuthScheme zimbraFreebusyExchangeAuthScheme, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, zimbraFreebusyExchangeAuthScheme.toString());
        return attrs;
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public void setFreebusyExchangeAuthSchemeAsString(String zimbraFreebusyExchangeAuthScheme) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, zimbraFreebusyExchangeAuthScheme);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public Map<String,Object> setFreebusyExchangeAuthSchemeAsString(String zimbraFreebusyExchangeAuthScheme, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, zimbraFreebusyExchangeAuthScheme);
        return attrs;
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public void unsetFreebusyExchangeAuthScheme() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [form, basic]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public Map<String,Object> unsetFreebusyExchangeAuthScheme(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, "");
        return attrs;
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthUsername, or null if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=608)
    public String getFreebusyExchangeAuthUsername() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername, null);
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeAuthUsername new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=608)
    public void setFreebusyExchangeAuthUsername(String zimbraFreebusyExchangeAuthUsername) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, zimbraFreebusyExchangeAuthUsername);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeAuthUsername new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=608)
    public Map<String,Object> setFreebusyExchangeAuthUsername(String zimbraFreebusyExchangeAuthUsername, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, zimbraFreebusyExchangeAuthUsername);
        return attrs;
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=608)
    public void unsetFreebusyExchangeAuthUsername() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=608)
    public Map<String,Object> unsetFreebusyExchangeAuthUsername(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, "");
        return attrs;
    }

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getFreebusyExchangeCachedIntervalAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalAsString()
     *
     * @return zimbraFreebusyExchangeCachedInterval in millseconds, or 5184000000 (60d)  if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public long getFreebusyExchangeCachedInterval() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, 5184000000L);
    }

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraFreebusyExchangeCachedInterval, or "60d" if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public String getFreebusyExchangeCachedIntervalAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "60d");
    }

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraFreebusyExchangeCachedInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public void setFreebusyExchangeCachedInterval(String zimbraFreebusyExchangeCachedInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, zimbraFreebusyExchangeCachedInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraFreebusyExchangeCachedInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public Map<String,Object> setFreebusyExchangeCachedInterval(String zimbraFreebusyExchangeCachedInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, zimbraFreebusyExchangeCachedInterval);
        return attrs;
    }

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public void unsetFreebusyExchangeCachedInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public Map<String,Object> unsetFreebusyExchangeCachedInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "");
        return attrs;
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getFreebusyExchangeCachedIntervalStartAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalStartAsString()
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart in millseconds, or 604800000 (7d)  if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public long getFreebusyExchangeCachedIntervalStart() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, 604800000L);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart, or "7d" if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public String getFreebusyExchangeCachedIntervalStartAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "7d");
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraFreebusyExchangeCachedIntervalStart new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public void setFreebusyExchangeCachedIntervalStart(String zimbraFreebusyExchangeCachedIntervalStart) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, zimbraFreebusyExchangeCachedIntervalStart);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraFreebusyExchangeCachedIntervalStart new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public Map<String,Object> setFreebusyExchangeCachedIntervalStart(String zimbraFreebusyExchangeCachedIntervalStart, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, zimbraFreebusyExchangeCachedIntervalStart);
        return attrs;
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public void unsetFreebusyExchangeCachedIntervalStart() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public Map<String,Object> unsetFreebusyExchangeCachedIntervalStart(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "");
        return attrs;
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @return zimbraFreebusyExchangeServerType, or ZAttrProvisioning.FreebusyExchangeServerType.webdav if unset and/or has invalid value
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public ZAttrProvisioning.FreebusyExchangeServerType getFreebusyExchangeServerType() {
        try { String v = getAttr(Provisioning.A_zimbraFreebusyExchangeServerType); return v == null ? ZAttrProvisioning.FreebusyExchangeServerType.webdav : ZAttrProvisioning.FreebusyExchangeServerType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.FreebusyExchangeServerType.webdav; }
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @return zimbraFreebusyExchangeServerType, or "webdav" if unset
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public String getFreebusyExchangeServerTypeAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeServerType, "webdav");
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @param zimbraFreebusyExchangeServerType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public void setFreebusyExchangeServerType(ZAttrProvisioning.FreebusyExchangeServerType zimbraFreebusyExchangeServerType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeServerType, zimbraFreebusyExchangeServerType.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @param zimbraFreebusyExchangeServerType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public Map<String,Object> setFreebusyExchangeServerType(ZAttrProvisioning.FreebusyExchangeServerType zimbraFreebusyExchangeServerType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeServerType, zimbraFreebusyExchangeServerType.toString());
        return attrs;
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @param zimbraFreebusyExchangeServerType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public void setFreebusyExchangeServerTypeAsString(String zimbraFreebusyExchangeServerType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeServerType, zimbraFreebusyExchangeServerType);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @param zimbraFreebusyExchangeServerType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public Map<String,Object> setFreebusyExchangeServerTypeAsString(String zimbraFreebusyExchangeServerType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeServerType, zimbraFreebusyExchangeServerType);
        return attrs;
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public void unsetFreebusyExchangeServerType() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeServerType, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * <p>Valid values: [ews, webdav]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public Map<String,Object> unsetFreebusyExchangeServerType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeServerType, "");
        return attrs;
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeURL, or null if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=607)
    public String getFreebusyExchangeURL() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeURL, null);
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=607)
    public void setFreebusyExchangeURL(String zimbraFreebusyExchangeURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeURL, zimbraFreebusyExchangeURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=607)
    public Map<String,Object> setFreebusyExchangeURL(String zimbraFreebusyExchangeURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeURL, zimbraFreebusyExchangeURL);
        return attrs;
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=607)
    public void unsetFreebusyExchangeURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=607)
    public Map<String,Object> unsetFreebusyExchangeURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeURL, "");
        return attrs;
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @return zimbraFreebusyExchangeUserOrg, or null if unset
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=610)
    public String getFreebusyExchangeUserOrg() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeUserOrg, null);
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @param zimbraFreebusyExchangeUserOrg new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=610)
    public void setFreebusyExchangeUserOrg(String zimbraFreebusyExchangeUserOrg) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, zimbraFreebusyExchangeUserOrg);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @param zimbraFreebusyExchangeUserOrg new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=610)
    public Map<String,Object> setFreebusyExchangeUserOrg(String zimbraFreebusyExchangeUserOrg, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, zimbraFreebusyExchangeUserOrg);
        return attrs;
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=610)
    public void unsetFreebusyExchangeUserOrg() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=610)
    public Map<String,Object> unsetFreebusyExchangeUserOrg(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, "");
        return attrs;
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @return zimbraGalAccountId, or empty array if unset
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public String[] getGalAccountId() {
        return getMultiAttr(Provisioning.A_zimbraGalAccountId);
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param zimbraGalAccountId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public void setGalAccountId(String[] zimbraGalAccountId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAccountId, zimbraGalAccountId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param zimbraGalAccountId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public Map<String,Object> setGalAccountId(String[] zimbraGalAccountId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAccountId, zimbraGalAccountId);
        return attrs;
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param zimbraGalAccountId new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public void addGalAccountId(String zimbraGalAccountId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalAccountId, zimbraGalAccountId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param zimbraGalAccountId new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public Map<String,Object> addGalAccountId(String zimbraGalAccountId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalAccountId, zimbraGalAccountId);
        return attrs;
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param zimbraGalAccountId existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public void removeGalAccountId(String zimbraGalAccountId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalAccountId, zimbraGalAccountId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param zimbraGalAccountId existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public Map<String,Object> removeGalAccountId(String zimbraGalAccountId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalAccountId, zimbraGalAccountId);
        return attrs;
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public void unsetGalAccountId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAccountId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraId of GAL sync accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public Map<String,Object> unsetGalAccountId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAccountId, "");
        return attrs;
    }

    /**
     * When set to TRUE, GAL search will always include local calendar
     * resources regardless of zimbraGalMode.
     *
     * @return zimbraGalAlwaysIncludeLocalCalendarResources, or false if unset
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1093)
    public boolean isGalAlwaysIncludeLocalCalendarResources() {
        return getBooleanAttr(Provisioning.A_zimbraGalAlwaysIncludeLocalCalendarResources, false);
    }

    /**
     * When set to TRUE, GAL search will always include local calendar
     * resources regardless of zimbraGalMode.
     *
     * @param zimbraGalAlwaysIncludeLocalCalendarResources new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1093)
    public void setGalAlwaysIncludeLocalCalendarResources(boolean zimbraGalAlwaysIncludeLocalCalendarResources) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAlwaysIncludeLocalCalendarResources, zimbraGalAlwaysIncludeLocalCalendarResources ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When set to TRUE, GAL search will always include local calendar
     * resources regardless of zimbraGalMode.
     *
     * @param zimbraGalAlwaysIncludeLocalCalendarResources new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1093)
    public Map<String,Object> setGalAlwaysIncludeLocalCalendarResources(boolean zimbraGalAlwaysIncludeLocalCalendarResources, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAlwaysIncludeLocalCalendarResources, zimbraGalAlwaysIncludeLocalCalendarResources ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * When set to TRUE, GAL search will always include local calendar
     * resources regardless of zimbraGalMode.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1093)
    public void unsetGalAlwaysIncludeLocalCalendarResources() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAlwaysIncludeLocalCalendarResources, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When set to TRUE, GAL search will always include local calendar
     * resources regardless of zimbraGalMode.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1093)
    public Map<String,Object> unsetGalAlwaysIncludeLocalCalendarResources(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAlwaysIncludeLocalCalendarResources, "");
        return attrs;
    }

    /**
     * LDAP search filter for external GAL auto-complete queries
     *
     * @return zimbraGalAutoCompleteLdapFilter, or "externalLdapAutoComplete" if unset
     */
    @ZAttr(id=360)
    public String getGalAutoCompleteLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalAutoCompleteLdapFilter, "externalLdapAutoComplete");
    }

    /**
     * LDAP search filter for external GAL auto-complete queries
     *
     * @param zimbraGalAutoCompleteLdapFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=360)
    public void setGalAutoCompleteLdapFilter(String zimbraGalAutoCompleteLdapFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAutoCompleteLdapFilter, zimbraGalAutoCompleteLdapFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for external GAL auto-complete queries
     *
     * @param zimbraGalAutoCompleteLdapFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=360)
    public Map<String,Object> setGalAutoCompleteLdapFilter(String zimbraGalAutoCompleteLdapFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAutoCompleteLdapFilter, zimbraGalAutoCompleteLdapFilter);
        return attrs;
    }

    /**
     * LDAP search filter for external GAL auto-complete queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=360)
    public void unsetGalAutoCompleteLdapFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAutoCompleteLdapFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for external GAL auto-complete queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=360)
    public Map<String,Object> unsetGalAutoCompleteLdapFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalAutoCompleteLdapFilter, "");
        return attrs;
    }

    /**
     * whether to indicate if an email address on a message is a GAL group
     *
     * @return zimbraGalGroupIndicatorEnabled, or true if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1153)
    public boolean isGalGroupIndicatorEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraGalGroupIndicatorEnabled, true);
    }

    /**
     * whether to indicate if an email address on a message is a GAL group
     *
     * @param zimbraGalGroupIndicatorEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1153)
    public void setGalGroupIndicatorEnabled(boolean zimbraGalGroupIndicatorEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupIndicatorEnabled, zimbraGalGroupIndicatorEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to indicate if an email address on a message is a GAL group
     *
     * @param zimbraGalGroupIndicatorEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1153)
    public Map<String,Object> setGalGroupIndicatorEnabled(boolean zimbraGalGroupIndicatorEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupIndicatorEnabled, zimbraGalGroupIndicatorEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether to indicate if an email address on a message is a GAL group
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1153)
    public void unsetGalGroupIndicatorEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupIndicatorEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to indicate if an email address on a message is a GAL group
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1153)
    public Map<String,Object> unsetGalGroupIndicatorEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupIndicatorEnabled, "");
        return attrs;
    }

    /**
     * LDAP search base for internal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     *
     * @return zimbraGalInternalSearchBase, or "DOMAIN" if unset
     */
    @ZAttr(id=358)
    public String getGalInternalSearchBase() {
        return getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
    }

    /**
     * LDAP search base for internal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     *
     * @param zimbraGalInternalSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=358)
    public void setGalInternalSearchBase(String zimbraGalInternalSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalInternalSearchBase, zimbraGalInternalSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for internal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     *
     * @param zimbraGalInternalSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=358)
    public Map<String,Object> setGalInternalSearchBase(String zimbraGalInternalSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalInternalSearchBase, zimbraGalInternalSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for internal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=358)
    public void unsetGalInternalSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalInternalSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for internal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=358)
    public Map<String,Object> unsetGalInternalSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalInternalSearchBase, "");
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @return zimbraGalLdapAttrMap, or empty array if unset
     */
    @ZAttr(id=153)
    public String[] getGalLdapAttrMap() {
        String[] value = getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap); return value.length > 0 ? value : new String[] {"co=workCountry","company=company","zimbraPhoneticCompany,ms-DS-Phonetic-Company-Name=phoneticCompany","givenName,gn=firstName","zimbraPhoneticFirstName,ms-DS-Phonetic-First-Name=phoneticFirstName","sn=lastName","zimbraPhoneticLastName,ms-DS-Phonetic-Last-Name=phoneticLastName","displayName,cn=fullName,fullName2,fullName3,fullName4,fullName5,fullName6,fullName7,fullName8,fullName9,fullName10","initials=initials","description=notes","l=workCity","physicalDeliveryOfficeName=office","ou=department","street,streetAddress=workStreet","postalCode=workPostalCode","facsimileTelephoneNumber,fax=workFax","homeTelephoneNumber,homePhone=homePhone","mobileTelephoneNumber,mobile=mobilePhone","pagerTelephoneNumber,pager=pager","telephoneNumber=workPhone","st=workState","zimbraMailDeliveryAddress,zimbraMailAlias,mail=email,email2,email3,email4,email5,email6,email7,email8,email9,email10,email11,email12,email13,email14,email15,email16","title=jobTitle","whenChanged,modifyTimeStamp=modifyTimeStamp","whenCreated,createTimeStamp=createTimeStamp","zimbraId=zimbraId","objectClass=objectClass","zimbraMailForwardingAddress=member","zimbraCalResType,msExchResourceSearchProperties=zimbraCalResType","zimbraCalResLocationDisplayName=zimbraCalResLocationDisplayName","zimbraCalResBuilding=zimbraCalResBuilding","zimbraCalResCapacity,msExchResourceCapacity=zimbraCalResCapacity","zimbraCalResFloor=zimbraCalResFloor","zimbraCalResSite=zimbraCalResSite","zimbraCalResContactEmail=zimbraCalResContactEmail","msExchResourceSearchProperties=zimbraAccountCalendarUserType","(certificate) userCertificate=userCertificate","(binary) userSMIMECertificate=userSMIMECertificate"};
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param zimbraGalLdapAttrMap new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=153)
    public void setGalLdapAttrMap(String[] zimbraGalLdapAttrMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAttrMap, zimbraGalLdapAttrMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param zimbraGalLdapAttrMap new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=153)
    public Map<String,Object> setGalLdapAttrMap(String[] zimbraGalLdapAttrMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAttrMap, zimbraGalLdapAttrMap);
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param zimbraGalLdapAttrMap new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=153)
    public void addGalLdapAttrMap(String zimbraGalLdapAttrMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapAttrMap, zimbraGalLdapAttrMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param zimbraGalLdapAttrMap new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=153)
    public Map<String,Object> addGalLdapAttrMap(String zimbraGalLdapAttrMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapAttrMap, zimbraGalLdapAttrMap);
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param zimbraGalLdapAttrMap existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=153)
    public void removeGalLdapAttrMap(String zimbraGalLdapAttrMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapAttrMap, zimbraGalLdapAttrMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param zimbraGalLdapAttrMap existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=153)
    public Map<String,Object> removeGalLdapAttrMap(String zimbraGalLdapAttrMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapAttrMap, zimbraGalLdapAttrMap);
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=153)
    public void unsetGalLdapAttrMap() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAttrMap, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=153)
    public Map<String,Object> unsetGalLdapAttrMap(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAttrMap, "");
        return attrs;
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @return zimbraGalLdapAuthMech, or null if unset and/or has invalid value
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public ZAttrProvisioning.GalLdapAuthMech getGalLdapAuthMech() {
        try { String v = getAttr(Provisioning.A_zimbraGalLdapAuthMech); return v == null ? null : ZAttrProvisioning.GalLdapAuthMech.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @return zimbraGalLdapAuthMech, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public String getGalLdapAuthMechAsString() {
        return getAttr(Provisioning.A_zimbraGalLdapAuthMech, null);
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public void setGalLdapAuthMech(ZAttrProvisioning.GalLdapAuthMech zimbraGalLdapAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAuthMech, zimbraGalLdapAuthMech.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public Map<String,Object> setGalLdapAuthMech(ZAttrProvisioning.GalLdapAuthMech zimbraGalLdapAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAuthMech, zimbraGalLdapAuthMech.toString());
        return attrs;
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public void setGalLdapAuthMechAsString(String zimbraGalLdapAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAuthMech, zimbraGalLdapAuthMech);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public Map<String,Object> setGalLdapAuthMechAsString(String zimbraGalLdapAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAuthMech, zimbraGalLdapAuthMech);
        return attrs;
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public void unsetGalLdapAuthMech() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAuthMech, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public Map<String,Object> unsetGalLdapAuthMech(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapAuthMech, "");
        return attrs;
    }

    /**
     * LDAP bind dn for external GAL queries
     *
     * @return zimbraGalLdapBindDn, or null if unset
     */
    @ZAttr(id=49)
    public String getGalLdapBindDn() {
        return getAttr(Provisioning.A_zimbraGalLdapBindDn, null);
    }

    /**
     * LDAP bind dn for external GAL queries
     *
     * @param zimbraGalLdapBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=49)
    public void setGalLdapBindDn(String zimbraGalLdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindDn, zimbraGalLdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind dn for external GAL queries
     *
     * @param zimbraGalLdapBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=49)
    public Map<String,Object> setGalLdapBindDn(String zimbraGalLdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindDn, zimbraGalLdapBindDn);
        return attrs;
    }

    /**
     * LDAP bind dn for external GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=49)
    public void unsetGalLdapBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind dn for external GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=49)
    public Map<String,Object> unsetGalLdapBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindDn, "");
        return attrs;
    }

    /**
     * LDAP bind password for external GAL queries
     *
     * @return zimbraGalLdapBindPassword, or null if unset
     */
    @ZAttr(id=50)
    public String getGalLdapBindPassword() {
        return getAttr(Provisioning.A_zimbraGalLdapBindPassword, null);
    }

    /**
     * LDAP bind password for external GAL queries
     *
     * @param zimbraGalLdapBindPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=50)
    public void setGalLdapBindPassword(String zimbraGalLdapBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, zimbraGalLdapBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for external GAL queries
     *
     * @param zimbraGalLdapBindPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=50)
    public Map<String,Object> setGalLdapBindPassword(String zimbraGalLdapBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, zimbraGalLdapBindPassword);
        return attrs;
    }

    /**
     * LDAP bind password for external GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=50)
    public void unsetGalLdapBindPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for external GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=50)
    public Map<String,Object> unsetGalLdapBindPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, "");
        return attrs;
    }

    /**
     * LDAP search filter for external GAL search queries
     *
     * @return zimbraGalLdapFilter, or null if unset
     */
    @ZAttr(id=51)
    public String getGalLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalLdapFilter, null);
    }

    /**
     * LDAP search filter for external GAL search queries
     *
     * @param zimbraGalLdapFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=51)
    public void setGalLdapFilter(String zimbraGalLdapFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilter, zimbraGalLdapFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for external GAL search queries
     *
     * @param zimbraGalLdapFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=51)
    public Map<String,Object> setGalLdapFilter(String zimbraGalLdapFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilter, zimbraGalLdapFilter);
        return attrs;
    }

    /**
     * LDAP search filter for external GAL search queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=51)
    public void unsetGalLdapFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for external GAL search queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=51)
    public Map<String,Object> unsetGalLdapFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilter, "");
        return attrs;
    }

    /**
     * the handler class for mapping groups from GAL source to zimbra GAL
     * contacts for external GAL
     *
     * @return zimbraGalLdapGroupHandlerClass, or null if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1112)
    public String getGalLdapGroupHandlerClass() {
        return getAttr(Provisioning.A_zimbraGalLdapGroupHandlerClass, null);
    }

    /**
     * the handler class for mapping groups from GAL source to zimbra GAL
     * contacts for external GAL
     *
     * @param zimbraGalLdapGroupHandlerClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1112)
    public void setGalLdapGroupHandlerClass(String zimbraGalLdapGroupHandlerClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, zimbraGalLdapGroupHandlerClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the handler class for mapping groups from GAL source to zimbra GAL
     * contacts for external GAL
     *
     * @param zimbraGalLdapGroupHandlerClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1112)
    public Map<String,Object> setGalLdapGroupHandlerClass(String zimbraGalLdapGroupHandlerClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, zimbraGalLdapGroupHandlerClass);
        return attrs;
    }

    /**
     * the handler class for mapping groups from GAL source to zimbra GAL
     * contacts for external GAL
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1112)
    public void unsetGalLdapGroupHandlerClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the handler class for mapping groups from GAL source to zimbra GAL
     * contacts for external GAL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1112)
    public Map<String,Object> unsetGalLdapGroupHandlerClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, "");
        return attrs;
    }

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @return zimbraGalLdapKerberos5Keytab, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=551)
    public String getGalLdapKerberos5Keytab() {
        return getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab, null);
    }

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @param zimbraGalLdapKerberos5Keytab new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=551)
    public void setGalLdapKerberos5Keytab(String zimbraGalLdapKerberos5Keytab) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Keytab, zimbraGalLdapKerberos5Keytab);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @param zimbraGalLdapKerberos5Keytab new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=551)
    public Map<String,Object> setGalLdapKerberos5Keytab(String zimbraGalLdapKerberos5Keytab, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Keytab, zimbraGalLdapKerberos5Keytab);
        return attrs;
    }

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=551)
    public void unsetGalLdapKerberos5Keytab() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Keytab, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=551)
    public Map<String,Object> unsetGalLdapKerberos5Keytab(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Keytab, "");
        return attrs;
    }

    /**
     * kerberos5 principal for external GAL queries
     *
     * @return zimbraGalLdapKerberos5Principal, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=550)
    public String getGalLdapKerberos5Principal() {
        return getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal, null);
    }

    /**
     * kerberos5 principal for external GAL queries
     *
     * @param zimbraGalLdapKerberos5Principal new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=550)
    public void setGalLdapKerberos5Principal(String zimbraGalLdapKerberos5Principal) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Principal, zimbraGalLdapKerberos5Principal);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 principal for external GAL queries
     *
     * @param zimbraGalLdapKerberos5Principal new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=550)
    public Map<String,Object> setGalLdapKerberos5Principal(String zimbraGalLdapKerberos5Principal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Principal, zimbraGalLdapKerberos5Principal);
        return attrs;
    }

    /**
     * kerberos5 principal for external GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=550)
    public void unsetGalLdapKerberos5Principal() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Principal, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 principal for external GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=550)
    public Map<String,Object> unsetGalLdapKerberos5Principal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Principal, "");
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled.
     *
     * @return zimbraGalLdapPageSize, or 1000 if unset
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=583)
    public int getGalLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalLdapPageSize, 1000);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled.
     *
     * @param zimbraGalLdapPageSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=583)
    public void setGalLdapPageSize(int zimbraGalLdapPageSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, Integer.toString(zimbraGalLdapPageSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled.
     *
     * @param zimbraGalLdapPageSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=583)
    public Map<String,Object> setGalLdapPageSize(int zimbraGalLdapPageSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, Integer.toString(zimbraGalLdapPageSize));
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=583)
    public void unsetGalLdapPageSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=583)
    public Map<String,Object> unsetGalLdapPageSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, "");
        return attrs;
    }

    /**
     * LDAP search base for external GAL queries
     *
     * @return zimbraGalLdapSearchBase, or null if unset
     */
    @ZAttr(id=48)
    public String getGalLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraGalLdapSearchBase, null);
    }

    /**
     * LDAP search base for external GAL queries
     *
     * @param zimbraGalLdapSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=48)
    public void setGalLdapSearchBase(String zimbraGalLdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapSearchBase, zimbraGalLdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for external GAL queries
     *
     * @param zimbraGalLdapSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=48)
    public Map<String,Object> setGalLdapSearchBase(String zimbraGalLdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapSearchBase, zimbraGalLdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for external GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=48)
    public void unsetGalLdapSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for external GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=48)
    public Map<String,Object> unsetGalLdapSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapSearchBase, "");
        return attrs;
    }

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @return zimbraGalLdapStartTlsEnabled, or false if unset
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=655)
    public boolean isGalLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraGalLdapStartTlsEnabled, false);
    }

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @param zimbraGalLdapStartTlsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=655)
    public void setGalLdapStartTlsEnabled(boolean zimbraGalLdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapStartTlsEnabled, zimbraGalLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @param zimbraGalLdapStartTlsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=655)
    public Map<String,Object> setGalLdapStartTlsEnabled(boolean zimbraGalLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapStartTlsEnabled, zimbraGalLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=655)
    public void unsetGalLdapStartTlsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapStartTlsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=655)
    public Map<String,Object> unsetGalLdapStartTlsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapStartTlsEnabled, "");
        return attrs;
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @return zimbraGalLdapURL, or empty array if unset
     */
    @ZAttr(id=47)
    public String[] getGalLdapURL() {
        return getMultiAttr(Provisioning.A_zimbraGalLdapURL);
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param zimbraGalLdapURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=47)
    public void setGalLdapURL(String[] zimbraGalLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapURL, zimbraGalLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param zimbraGalLdapURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=47)
    public Map<String,Object> setGalLdapURL(String[] zimbraGalLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapURL, zimbraGalLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param zimbraGalLdapURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=47)
    public void addGalLdapURL(String zimbraGalLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapURL, zimbraGalLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param zimbraGalLdapURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=47)
    public Map<String,Object> addGalLdapURL(String zimbraGalLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapURL, zimbraGalLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param zimbraGalLdapURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=47)
    public void removeGalLdapURL(String zimbraGalLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapURL, zimbraGalLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param zimbraGalLdapURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=47)
    public Map<String,Object> removeGalLdapURL(String zimbraGalLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapURL, zimbraGalLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=47)
    public void unsetGalLdapURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=47)
    public Map<String,Object> unsetGalLdapURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapURL, "");
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @return zimbraGalLdapValueMap, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public String[] getGalLdapValueMap() {
        String[] value = getMultiAttr(Provisioning.A_zimbraGalLdapValueMap); return value.length > 0 ? value : new String[] {"zimbraCalResType: Room Location","zimbraAccountCalendarUserType: Room|Equipment RESOURCE"};
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param zimbraGalLdapValueMap new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public void setGalLdapValueMap(String[] zimbraGalLdapValueMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapValueMap, zimbraGalLdapValueMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param zimbraGalLdapValueMap new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public Map<String,Object> setGalLdapValueMap(String[] zimbraGalLdapValueMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapValueMap, zimbraGalLdapValueMap);
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param zimbraGalLdapValueMap new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public void addGalLdapValueMap(String zimbraGalLdapValueMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapValueMap, zimbraGalLdapValueMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param zimbraGalLdapValueMap new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public Map<String,Object> addGalLdapValueMap(String zimbraGalLdapValueMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapValueMap, zimbraGalLdapValueMap);
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param zimbraGalLdapValueMap existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public void removeGalLdapValueMap(String zimbraGalLdapValueMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapValueMap, zimbraGalLdapValueMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param zimbraGalLdapValueMap existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public Map<String,Object> removeGalLdapValueMap(String zimbraGalLdapValueMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapValueMap, zimbraGalLdapValueMap);
        return attrs;
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public void unsetGalLdapValueMap() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapValueMap, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public Map<String,Object> unsetGalLdapValueMap(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapValueMap, "");
        return attrs;
    }

    /**
     * maximum number of gal entries to return from a search
     *
     * @return zimbraGalMaxResults, or 100 if unset
     */
    @ZAttr(id=53)
    public int getGalMaxResults() {
        return getIntAttr(Provisioning.A_zimbraGalMaxResults, 100);
    }

    /**
     * maximum number of gal entries to return from a search
     *
     * @param zimbraGalMaxResults new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=53)
    public void setGalMaxResults(int zimbraGalMaxResults) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMaxResults, Integer.toString(zimbraGalMaxResults));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of gal entries to return from a search
     *
     * @param zimbraGalMaxResults new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=53)
    public Map<String,Object> setGalMaxResults(int zimbraGalMaxResults, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMaxResults, Integer.toString(zimbraGalMaxResults));
        return attrs;
    }

    /**
     * maximum number of gal entries to return from a search
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=53)
    public void unsetGalMaxResults() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMaxResults, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * maximum number of gal entries to return from a search
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=53)
    public Map<String,Object> unsetGalMaxResults(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMaxResults, "");
        return attrs;
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @return zimbraGalMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=46)
    public ZAttrProvisioning.GalMode getGalMode() {
        try { String v = getAttr(Provisioning.A_zimbraGalMode); return v == null ? null : ZAttrProvisioning.GalMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @return zimbraGalMode, or null if unset
     */
    @ZAttr(id=46)
    public String getGalModeAsString() {
        return getAttr(Provisioning.A_zimbraGalMode, null);
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @param zimbraGalMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=46)
    public void setGalMode(ZAttrProvisioning.GalMode zimbraGalMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMode, zimbraGalMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @param zimbraGalMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=46)
    public Map<String,Object> setGalMode(ZAttrProvisioning.GalMode zimbraGalMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMode, zimbraGalMode.toString());
        return attrs;
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @param zimbraGalMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=46)
    public void setGalModeAsString(String zimbraGalMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMode, zimbraGalMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @param zimbraGalMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=46)
    public Map<String,Object> setGalModeAsString(String zimbraGalMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMode, zimbraGalMode);
        return attrs;
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=46)
    public void unsetGalMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     *
     * <p>Valid values: [ldap, both, zimbra]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=46)
    public Map<String,Object> unsetGalMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalMode, "");
        return attrs;
    }

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @return zimbraGalSyncInternalSearchBase, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=598)
    public String getGalSyncInternalSearchBase() {
        return getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase, null);
    }

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @param zimbraGalSyncInternalSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=598)
    public void setGalSyncInternalSearchBase(String zimbraGalSyncInternalSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncInternalSearchBase, zimbraGalSyncInternalSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @param zimbraGalSyncInternalSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=598)
    public Map<String,Object> setGalSyncInternalSearchBase(String zimbraGalSyncInternalSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncInternalSearchBase, zimbraGalSyncInternalSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=598)
    public void unsetGalSyncInternalSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncInternalSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=598)
    public Map<String,Object> unsetGalSyncInternalSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncInternalSearchBase, "");
        return attrs;
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @return zimbraGalSyncLdapAuthMech, or null if unset and/or has invalid value
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public ZAttrProvisioning.GalSyncLdapAuthMech getGalSyncLdapAuthMech() {
        try { String v = getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech); return v == null ? null : ZAttrProvisioning.GalSyncLdapAuthMech.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @return zimbraGalSyncLdapAuthMech, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public String getGalSyncLdapAuthMechAsString() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech, null);
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public void setGalSyncLdapAuthMech(ZAttrProvisioning.GalSyncLdapAuthMech zimbraGalSyncLdapAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapAuthMech, zimbraGalSyncLdapAuthMech.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public Map<String,Object> setGalSyncLdapAuthMech(ZAttrProvisioning.GalSyncLdapAuthMech zimbraGalSyncLdapAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapAuthMech, zimbraGalSyncLdapAuthMech.toString());
        return attrs;
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public void setGalSyncLdapAuthMechAsString(String zimbraGalSyncLdapAuthMech) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapAuthMech, zimbraGalSyncLdapAuthMech);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public Map<String,Object> setGalSyncLdapAuthMechAsString(String zimbraGalSyncLdapAuthMech, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapAuthMech, zimbraGalSyncLdapAuthMech);
        return attrs;
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public void unsetGalSyncLdapAuthMech() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapAuthMech, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [none, kerberos5, simple]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public Map<String,Object> unsetGalSyncLdapAuthMech(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapAuthMech, "");
        return attrs;
    }

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @return zimbraGalSyncLdapBindDn, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=593)
    public String getGalSyncLdapBindDn() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapBindDn, null);
    }

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @param zimbraGalSyncLdapBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=593)
    public void setGalSyncLdapBindDn(String zimbraGalSyncLdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindDn, zimbraGalSyncLdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @param zimbraGalSyncLdapBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=593)
    public Map<String,Object> setGalSyncLdapBindDn(String zimbraGalSyncLdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindDn, zimbraGalSyncLdapBindDn);
        return attrs;
    }

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=593)
    public void unsetGalSyncLdapBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=593)
    public Map<String,Object> unsetGalSyncLdapBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindDn, "");
        return attrs;
    }

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @return zimbraGalSyncLdapBindPassword, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=594)
    public String getGalSyncLdapBindPassword() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword, null);
    }

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @param zimbraGalSyncLdapBindPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=594)
    public void setGalSyncLdapBindPassword(String zimbraGalSyncLdapBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindPassword, zimbraGalSyncLdapBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @param zimbraGalSyncLdapBindPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=594)
    public Map<String,Object> setGalSyncLdapBindPassword(String zimbraGalSyncLdapBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindPassword, zimbraGalSyncLdapBindPassword);
        return attrs;
    }

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=594)
    public void unsetGalSyncLdapBindPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=594)
    public Map<String,Object> unsetGalSyncLdapBindPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapBindPassword, "");
        return attrs;
    }

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @return zimbraGalSyncLdapFilter, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=591)
    public String getGalSyncLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapFilter, null);
    }

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @param zimbraGalSyncLdapFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=591)
    public void setGalSyncLdapFilter(String zimbraGalSyncLdapFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapFilter, zimbraGalSyncLdapFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @param zimbraGalSyncLdapFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=591)
    public Map<String,Object> setGalSyncLdapFilter(String zimbraGalSyncLdapFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapFilter, zimbraGalSyncLdapFilter);
        return attrs;
    }

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=591)
    public void unsetGalSyncLdapFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=591)
    public Map<String,Object> unsetGalSyncLdapFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapFilter, "");
        return attrs;
    }

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @return zimbraGalSyncLdapKerberos5Keytab, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=596)
    public String getGalSyncLdapKerberos5Keytab() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab, null);
    }

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @param zimbraGalSyncLdapKerberos5Keytab new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=596)
    public void setGalSyncLdapKerberos5Keytab(String zimbraGalSyncLdapKerberos5Keytab) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab, zimbraGalSyncLdapKerberos5Keytab);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @param zimbraGalSyncLdapKerberos5Keytab new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=596)
    public Map<String,Object> setGalSyncLdapKerberos5Keytab(String zimbraGalSyncLdapKerberos5Keytab, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab, zimbraGalSyncLdapKerberos5Keytab);
        return attrs;
    }

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=596)
    public void unsetGalSyncLdapKerberos5Keytab() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=596)
    public Map<String,Object> unsetGalSyncLdapKerberos5Keytab(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab, "");
        return attrs;
    }

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @return zimbraGalSyncLdapKerberos5Principal, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=595)
    public String getGalSyncLdapKerberos5Principal() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, null);
    }

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @param zimbraGalSyncLdapKerberos5Principal new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=595)
    public void setGalSyncLdapKerberos5Principal(String zimbraGalSyncLdapKerberos5Principal) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, zimbraGalSyncLdapKerberos5Principal);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @param zimbraGalSyncLdapKerberos5Principal new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=595)
    public Map<String,Object> setGalSyncLdapKerberos5Principal(String zimbraGalSyncLdapKerberos5Principal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, zimbraGalSyncLdapKerberos5Principal);
        return attrs;
    }

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=595)
    public void unsetGalSyncLdapKerberos5Principal() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=595)
    public Map<String,Object> unsetGalSyncLdapKerberos5Principal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, "");
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @return zimbraGalSyncLdapPageSize, or 1000 if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=597)
    public int getGalSyncLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalSyncLdapPageSize, 1000);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @param zimbraGalSyncLdapPageSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=597)
    public void setGalSyncLdapPageSize(int zimbraGalSyncLdapPageSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, Integer.toString(zimbraGalSyncLdapPageSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @param zimbraGalSyncLdapPageSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=597)
    public Map<String,Object> setGalSyncLdapPageSize(int zimbraGalSyncLdapPageSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, Integer.toString(zimbraGalSyncLdapPageSize));
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=597)
    public void unsetGalSyncLdapPageSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=597)
    public Map<String,Object> unsetGalSyncLdapPageSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, "");
        return attrs;
    }

    /**
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @return zimbraGalSyncLdapSearchBase, or null if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=590)
    public String getGalSyncLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase, null);
    }

    /**
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @param zimbraGalSyncLdapSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=590)
    public void setGalSyncLdapSearchBase(String zimbraGalSyncLdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapSearchBase, zimbraGalSyncLdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @param zimbraGalSyncLdapSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=590)
    public Map<String,Object> setGalSyncLdapSearchBase(String zimbraGalSyncLdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapSearchBase, zimbraGalSyncLdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=590)
    public void unsetGalSyncLdapSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=590)
    public Map<String,Object> unsetGalSyncLdapSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapSearchBase, "");
        return attrs;
    }

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @return zimbraGalSyncLdapStartTlsEnabled, or false if unset
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=656)
    public boolean isGalSyncLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, false);
    }

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @param zimbraGalSyncLdapStartTlsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=656)
    public void setGalSyncLdapStartTlsEnabled(boolean zimbraGalSyncLdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, zimbraGalSyncLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @param zimbraGalSyncLdapStartTlsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=656)
    public Map<String,Object> setGalSyncLdapStartTlsEnabled(boolean zimbraGalSyncLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, zimbraGalSyncLdapStartTlsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=656)
    public void unsetGalSyncLdapStartTlsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=656)
    public Map<String,Object> unsetGalSyncLdapStartTlsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, "");
        return attrs;
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @return zimbraGalSyncLdapURL, or empty array if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public String[] getGalSyncLdapURL() {
        return getMultiAttr(Provisioning.A_zimbraGalSyncLdapURL);
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param zimbraGalSyncLdapURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public void setGalSyncLdapURL(String[] zimbraGalSyncLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapURL, zimbraGalSyncLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param zimbraGalSyncLdapURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public Map<String,Object> setGalSyncLdapURL(String[] zimbraGalSyncLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapURL, zimbraGalSyncLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param zimbraGalSyncLdapURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public void addGalSyncLdapURL(String zimbraGalSyncLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalSyncLdapURL, zimbraGalSyncLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param zimbraGalSyncLdapURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public Map<String,Object> addGalSyncLdapURL(String zimbraGalSyncLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalSyncLdapURL, zimbraGalSyncLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param zimbraGalSyncLdapURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public void removeGalSyncLdapURL(String zimbraGalSyncLdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalSyncLdapURL, zimbraGalSyncLdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param zimbraGalSyncLdapURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public Map<String,Object> removeGalSyncLdapURL(String zimbraGalSyncLdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalSyncLdapURL, zimbraGalSyncLdapURL);
        return attrs;
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public void unsetGalSyncLdapURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public Map<String,Object> unsetGalSyncLdapURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapURL, "");
        return attrs;
    }

    /**
     * Maximum number of concurrent GAL sync requests allowed on the system /
     * domain.
     *
     * @return zimbraGalSyncMaxConcurrentClients, or 2 if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1154)
    public int getGalSyncMaxConcurrentClients() {
        return getIntAttr(Provisioning.A_zimbraGalSyncMaxConcurrentClients, 2);
    }

    /**
     * Maximum number of concurrent GAL sync requests allowed on the system /
     * domain.
     *
     * @param zimbraGalSyncMaxConcurrentClients new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1154)
    public void setGalSyncMaxConcurrentClients(int zimbraGalSyncMaxConcurrentClients) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMaxConcurrentClients, Integer.toString(zimbraGalSyncMaxConcurrentClients));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of concurrent GAL sync requests allowed on the system /
     * domain.
     *
     * @param zimbraGalSyncMaxConcurrentClients new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1154)
    public Map<String,Object> setGalSyncMaxConcurrentClients(int zimbraGalSyncMaxConcurrentClients, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMaxConcurrentClients, Integer.toString(zimbraGalSyncMaxConcurrentClients));
        return attrs;
    }

    /**
     * Maximum number of concurrent GAL sync requests allowed on the system /
     * domain.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1154)
    public void unsetGalSyncMaxConcurrentClients() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMaxConcurrentClients, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of concurrent GAL sync requests allowed on the system /
     * domain.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1154)
    public Map<String,Object> unsetGalSyncMaxConcurrentClients(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMaxConcurrentClients, "");
        return attrs;
    }

    /**
     * LDAP generalized time format for external GAL sync
     *
     * @return zimbraGalSyncTimestampFormat, or "yyyyMMddHHmmss'Z'" if unset
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1019)
    public String getGalSyncTimestampFormat() {
        return getAttr(Provisioning.A_zimbraGalSyncTimestampFormat, "yyyyMMddHHmmss'Z'");
    }

    /**
     * LDAP generalized time format for external GAL sync
     *
     * @param zimbraGalSyncTimestampFormat new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1019)
    public void setGalSyncTimestampFormat(String zimbraGalSyncTimestampFormat) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncTimestampFormat, zimbraGalSyncTimestampFormat);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP generalized time format for external GAL sync
     *
     * @param zimbraGalSyncTimestampFormat new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1019)
    public Map<String,Object> setGalSyncTimestampFormat(String zimbraGalSyncTimestampFormat, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncTimestampFormat, zimbraGalSyncTimestampFormat);
        return attrs;
    }

    /**
     * LDAP generalized time format for external GAL sync
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1019)
    public void unsetGalSyncTimestampFormat() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncTimestampFormat, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP generalized time format for external GAL sync
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1019)
    public Map<String,Object> unsetGalSyncTimestampFormat(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncTimestampFormat, "");
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @return zimbraGalTokenizeAutoCompleteKey, or ZAttrProvisioning.GalTokenizeAutoCompleteKey.and if unset and/or has invalid value
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public ZAttrProvisioning.GalTokenizeAutoCompleteKey getGalTokenizeAutoCompleteKey() {
        try { String v = getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey); return v == null ? ZAttrProvisioning.GalTokenizeAutoCompleteKey.and : ZAttrProvisioning.GalTokenizeAutoCompleteKey.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.GalTokenizeAutoCompleteKey.and; }
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @return zimbraGalTokenizeAutoCompleteKey, or "and" if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public String getGalTokenizeAutoCompleteKeyAsString() {
        return getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, "and");
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public void setGalTokenizeAutoCompleteKey(ZAttrProvisioning.GalTokenizeAutoCompleteKey zimbraGalTokenizeAutoCompleteKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, zimbraGalTokenizeAutoCompleteKey.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public Map<String,Object> setGalTokenizeAutoCompleteKey(ZAttrProvisioning.GalTokenizeAutoCompleteKey zimbraGalTokenizeAutoCompleteKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, zimbraGalTokenizeAutoCompleteKey.toString());
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public void setGalTokenizeAutoCompleteKeyAsString(String zimbraGalTokenizeAutoCompleteKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, zimbraGalTokenizeAutoCompleteKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public Map<String,Object> setGalTokenizeAutoCompleteKeyAsString(String zimbraGalTokenizeAutoCompleteKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, zimbraGalTokenizeAutoCompleteKey);
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public void unsetGalTokenizeAutoCompleteKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public Map<String,Object> unsetGalTokenizeAutoCompleteKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, "");
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @return zimbraGalTokenizeSearchKey, or ZAttrProvisioning.GalTokenizeSearchKey.and if unset and/or has invalid value
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public ZAttrProvisioning.GalTokenizeSearchKey getGalTokenizeSearchKey() {
        try { String v = getAttr(Provisioning.A_zimbraGalTokenizeSearchKey); return v == null ? ZAttrProvisioning.GalTokenizeSearchKey.and : ZAttrProvisioning.GalTokenizeSearchKey.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.GalTokenizeSearchKey.and; }
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @return zimbraGalTokenizeSearchKey, or "and" if unset
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public String getGalTokenizeSearchKeyAsString() {
        return getAttr(Provisioning.A_zimbraGalTokenizeSearchKey, "and");
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public void setGalTokenizeSearchKey(ZAttrProvisioning.GalTokenizeSearchKey zimbraGalTokenizeSearchKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeSearchKey, zimbraGalTokenizeSearchKey.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public Map<String,Object> setGalTokenizeSearchKey(ZAttrProvisioning.GalTokenizeSearchKey zimbraGalTokenizeSearchKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeSearchKey, zimbraGalTokenizeSearchKey.toString());
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public void setGalTokenizeSearchKeyAsString(String zimbraGalTokenizeSearchKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeSearchKey, zimbraGalTokenizeSearchKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public Map<String,Object> setGalTokenizeSearchKeyAsString(String zimbraGalTokenizeSearchKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeSearchKey, zimbraGalTokenizeSearchKey);
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public void unsetGalTokenizeSearchKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeSearchKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [or, and]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public Map<String,Object> unsetGalTokenizeSearchKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalTokenizeSearchKey, "");
        return attrs;
    }

    /**
     * help URL for admin
     *
     * @return zimbraHelpAdminURL, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=674)
    public String getHelpAdminURL() {
        return getAttr(Provisioning.A_zimbraHelpAdminURL, null);
    }

    /**
     * help URL for admin
     *
     * @param zimbraHelpAdminURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=674)
    public void setHelpAdminURL(String zimbraHelpAdminURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdminURL, zimbraHelpAdminURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for admin
     *
     * @param zimbraHelpAdminURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=674)
    public Map<String,Object> setHelpAdminURL(String zimbraHelpAdminURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdminURL, zimbraHelpAdminURL);
        return attrs;
    }

    /**
     * help URL for admin
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=674)
    public void unsetHelpAdminURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdminURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for admin
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=674)
    public Map<String,Object> unsetHelpAdminURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdminURL, "");
        return attrs;
    }

    /**
     * help URL for advanced client
     *
     * @return zimbraHelpAdvancedURL, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=676)
    public String getHelpAdvancedURL() {
        return getAttr(Provisioning.A_zimbraHelpAdvancedURL, null);
    }

    /**
     * help URL for advanced client
     *
     * @param zimbraHelpAdvancedURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=676)
    public void setHelpAdvancedURL(String zimbraHelpAdvancedURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdvancedURL, zimbraHelpAdvancedURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for advanced client
     *
     * @param zimbraHelpAdvancedURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=676)
    public Map<String,Object> setHelpAdvancedURL(String zimbraHelpAdvancedURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdvancedURL, zimbraHelpAdvancedURL);
        return attrs;
    }

    /**
     * help URL for advanced client
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=676)
    public void unsetHelpAdvancedURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdvancedURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for advanced client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=676)
    public Map<String,Object> unsetHelpAdvancedURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpAdvancedURL, "");
        return attrs;
    }

    /**
     * help URL for delegated admin
     *
     * @return zimbraHelpDelegatedURL, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=675)
    public String getHelpDelegatedURL() {
        return getAttr(Provisioning.A_zimbraHelpDelegatedURL, null);
    }

    /**
     * help URL for delegated admin
     *
     * @param zimbraHelpDelegatedURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=675)
    public void setHelpDelegatedURL(String zimbraHelpDelegatedURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpDelegatedURL, zimbraHelpDelegatedURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for delegated admin
     *
     * @param zimbraHelpDelegatedURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=675)
    public Map<String,Object> setHelpDelegatedURL(String zimbraHelpDelegatedURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpDelegatedURL, zimbraHelpDelegatedURL);
        return attrs;
    }

    /**
     * help URL for delegated admin
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=675)
    public void unsetHelpDelegatedURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpDelegatedURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for delegated admin
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=675)
    public Map<String,Object> unsetHelpDelegatedURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpDelegatedURL, "");
        return attrs;
    }

    /**
     * help URL for standard client
     *
     * @return zimbraHelpStandardURL, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=677)
    public String getHelpStandardURL() {
        return getAttr(Provisioning.A_zimbraHelpStandardURL, null);
    }

    /**
     * help URL for standard client
     *
     * @param zimbraHelpStandardURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=677)
    public void setHelpStandardURL(String zimbraHelpStandardURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpStandardURL, zimbraHelpStandardURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for standard client
     *
     * @param zimbraHelpStandardURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=677)
    public Map<String,Object> setHelpStandardURL(String zimbraHelpStandardURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpStandardURL, zimbraHelpStandardURL);
        return attrs;
    }

    /**
     * help URL for standard client
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=677)
    public void unsetHelpStandardURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpStandardURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * help URL for standard client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=677)
    public Map<String,Object> unsetHelpStandardURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHelpStandardURL, "");
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
     * additional domains considered as internal w.r.t. recipient
     *
     * @return zimbraInternalSendersDomain, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public String[] getInternalSendersDomain() {
        return getMultiAttr(Provisioning.A_zimbraInternalSendersDomain);
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param zimbraInternalSendersDomain new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public void setInternalSendersDomain(String[] zimbraInternalSendersDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInternalSendersDomain, zimbraInternalSendersDomain);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param zimbraInternalSendersDomain new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public Map<String,Object> setInternalSendersDomain(String[] zimbraInternalSendersDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInternalSendersDomain, zimbraInternalSendersDomain);
        return attrs;
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param zimbraInternalSendersDomain new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public void addInternalSendersDomain(String zimbraInternalSendersDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraInternalSendersDomain, zimbraInternalSendersDomain);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param zimbraInternalSendersDomain new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public Map<String,Object> addInternalSendersDomain(String zimbraInternalSendersDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraInternalSendersDomain, zimbraInternalSendersDomain);
        return attrs;
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param zimbraInternalSendersDomain existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public void removeInternalSendersDomain(String zimbraInternalSendersDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraInternalSendersDomain, zimbraInternalSendersDomain);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param zimbraInternalSendersDomain existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public Map<String,Object> removeInternalSendersDomain(String zimbraInternalSendersDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraInternalSendersDomain, zimbraInternalSendersDomain);
        return attrs;
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public void unsetInternalSendersDomain() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInternalSendersDomain, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public Map<String,Object> unsetInternalSendersDomain(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInternalSendersDomain, "");
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

    /**
     * optional regex used by web client to validate email address
     *
     * @return zimbraMailAddressValidationRegex, or empty array if unset
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public String[] getMailAddressValidationRegex() {
        return getMultiAttr(Provisioning.A_zimbraMailAddressValidationRegex);
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param zimbraMailAddressValidationRegex new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public void setMailAddressValidationRegex(String[] zimbraMailAddressValidationRegex) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAddressValidationRegex, zimbraMailAddressValidationRegex);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param zimbraMailAddressValidationRegex new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public Map<String,Object> setMailAddressValidationRegex(String[] zimbraMailAddressValidationRegex, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAddressValidationRegex, zimbraMailAddressValidationRegex);
        return attrs;
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param zimbraMailAddressValidationRegex new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public void addMailAddressValidationRegex(String zimbraMailAddressValidationRegex) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailAddressValidationRegex, zimbraMailAddressValidationRegex);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param zimbraMailAddressValidationRegex new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public Map<String,Object> addMailAddressValidationRegex(String zimbraMailAddressValidationRegex, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailAddressValidationRegex, zimbraMailAddressValidationRegex);
        return attrs;
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param zimbraMailAddressValidationRegex existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public void removeMailAddressValidationRegex(String zimbraMailAddressValidationRegex) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailAddressValidationRegex, zimbraMailAddressValidationRegex);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param zimbraMailAddressValidationRegex existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public Map<String,Object> removeMailAddressValidationRegex(String zimbraMailAddressValidationRegex, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailAddressValidationRegex, zimbraMailAddressValidationRegex);
        return attrs;
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public void unsetMailAddressValidationRegex() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAddressValidationRegex, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * optional regex used by web client to validate email address
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public Map<String,Object> unsetMailAddressValidationRegex(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAddressValidationRegex, "");
        return attrs;
    }

    /**
     * Maximum mailbox quota for the domain in bytes. The effective quota for
     * a mailbox would be the minimum of this and zimbraMailQuota.
     *
     * @return zimbraMailDomainQuota, or 0 if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1326)
    public long getMailDomainQuota() {
        return getLongAttr(Provisioning.A_zimbraMailDomainQuota, 0L);
    }

    /**
     * Maximum mailbox quota for the domain in bytes. The effective quota for
     * a mailbox would be the minimum of this and zimbraMailQuota.
     *
     * @param zimbraMailDomainQuota new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1326)
    public void setMailDomainQuota(long zimbraMailDomainQuota) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDomainQuota, Long.toString(zimbraMailDomainQuota));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum mailbox quota for the domain in bytes. The effective quota for
     * a mailbox would be the minimum of this and zimbraMailQuota.
     *
     * @param zimbraMailDomainQuota new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1326)
    public Map<String,Object> setMailDomainQuota(long zimbraMailDomainQuota, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDomainQuota, Long.toString(zimbraMailDomainQuota));
        return attrs;
    }

    /**
     * Maximum mailbox quota for the domain in bytes. The effective quota for
     * a mailbox would be the minimum of this and zimbraMailQuota.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1326)
    public void unsetMailDomainQuota() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDomainQuota, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum mailbox quota for the domain in bytes. The effective quota for
     * a mailbox would be the minimum of this and zimbraMailQuota.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1326)
    public Map<String,Object> unsetMailDomainQuota(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDomainQuota, "");
        return attrs;
    }

    /**
     * Map from a certificate field to a Zimbra account key that can uniquely
     * identify a Zimbra account for client certificate authentication. Value
     * is a comma-separated list of mapping rules, each mapping maps a
     * certificate field to a Zimbra account key. Each is attempted in
     * sequence until a unique account can be resolved. e.g. a value can be:
     * SUBJECTALTNAME_OTHERNAME_UPN=zimbraForeignPrincipal,(uid=%{SUBJECT_CN})
     * value: comma-separated mapping-rule mapping-rule:
     * {cert-field-to-zimbra-key-map} | {LDAP-filter}
     * cert-field-to-zimbra-key-map: {certificate-field}={Zimbra-account-key}
     * certificate-field: SUBJECT_{an RDN attr, e.g. CN}: a RND in DN of
     * Subject SUBJECT_DN: entire DN of Subject SUBJECTALTNAME_OTHERNAME_UPN:
     * UPN(aka Principal Name) in otherName in subjectAltName extension
     * SUBJECTALTNAME_RFC822NAME: rfc822Name in subjectAltName extension
     * Zimbra-account-key: name: primary name or any of the aliases of an
     * account zimbraId: zimbraId of an account zimbraForeignPrincipal:
     * zimbraForeignPrincipal of an account. The matching value on the
     * zimbraForeignPrincipal must be prefixed with &quot;cert
     * {supported-certificate-filed}:&quot; e.g. cert
     * SUBJECTALTNAME_OTHERNAME_UPN:123456@mydomain LDAP-filter: An LDAP
     * filter template with placeholders to be substituted by certificate
     * field values. (objectClass=zimbraAccount) is internally ANDed with the
     * supplied filter. e.g.
     * (|(uid=%{SUBJECT_CN})(mail=%{SUBJECTALTNAME_RFC822NAME})) Note: it is
     * recommended not to use LDAP-filter rule, as it will trigger an LDAP
     * search for each cert auth request. LDAP-filter is disabled by default.
     * To enable it globally, set
     * zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled on global config
     * to TRUE. If LDAP-filter is not enabled, all client certificate
     * authentication will fail on domains configured with LDAP-filter.
     *
     * @return zimbraMailSSLClientCertPrincipalMap, or "SUBJECT_EMAILADDRESS=name" if unset
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1215)
    public String getMailSSLClientCertPrincipalMap() {
        return getAttr(Provisioning.A_zimbraMailSSLClientCertPrincipalMap, "SUBJECT_EMAILADDRESS=name");
    }

    /**
     * Map from a certificate field to a Zimbra account key that can uniquely
     * identify a Zimbra account for client certificate authentication. Value
     * is a comma-separated list of mapping rules, each mapping maps a
     * certificate field to a Zimbra account key. Each is attempted in
     * sequence until a unique account can be resolved. e.g. a value can be:
     * SUBJECTALTNAME_OTHERNAME_UPN=zimbraForeignPrincipal,(uid=%{SUBJECT_CN})
     * value: comma-separated mapping-rule mapping-rule:
     * {cert-field-to-zimbra-key-map} | {LDAP-filter}
     * cert-field-to-zimbra-key-map: {certificate-field}={Zimbra-account-key}
     * certificate-field: SUBJECT_{an RDN attr, e.g. CN}: a RND in DN of
     * Subject SUBJECT_DN: entire DN of Subject SUBJECTALTNAME_OTHERNAME_UPN:
     * UPN(aka Principal Name) in otherName in subjectAltName extension
     * SUBJECTALTNAME_RFC822NAME: rfc822Name in subjectAltName extension
     * Zimbra-account-key: name: primary name or any of the aliases of an
     * account zimbraId: zimbraId of an account zimbraForeignPrincipal:
     * zimbraForeignPrincipal of an account. The matching value on the
     * zimbraForeignPrincipal must be prefixed with &quot;cert
     * {supported-certificate-filed}:&quot; e.g. cert
     * SUBJECTALTNAME_OTHERNAME_UPN:123456@mydomain LDAP-filter: An LDAP
     * filter template with placeholders to be substituted by certificate
     * field values. (objectClass=zimbraAccount) is internally ANDed with the
     * supplied filter. e.g.
     * (|(uid=%{SUBJECT_CN})(mail=%{SUBJECTALTNAME_RFC822NAME})) Note: it is
     * recommended not to use LDAP-filter rule, as it will trigger an LDAP
     * search for each cert auth request. LDAP-filter is disabled by default.
     * To enable it globally, set
     * zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled on global config
     * to TRUE. If LDAP-filter is not enabled, all client certificate
     * authentication will fail on domains configured with LDAP-filter.
     *
     * @param zimbraMailSSLClientCertPrincipalMap new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1215)
    public void setMailSSLClientCertPrincipalMap(String zimbraMailSSLClientCertPrincipalMap) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLClientCertPrincipalMap, zimbraMailSSLClientCertPrincipalMap);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Map from a certificate field to a Zimbra account key that can uniquely
     * identify a Zimbra account for client certificate authentication. Value
     * is a comma-separated list of mapping rules, each mapping maps a
     * certificate field to a Zimbra account key. Each is attempted in
     * sequence until a unique account can be resolved. e.g. a value can be:
     * SUBJECTALTNAME_OTHERNAME_UPN=zimbraForeignPrincipal,(uid=%{SUBJECT_CN})
     * value: comma-separated mapping-rule mapping-rule:
     * {cert-field-to-zimbra-key-map} | {LDAP-filter}
     * cert-field-to-zimbra-key-map: {certificate-field}={Zimbra-account-key}
     * certificate-field: SUBJECT_{an RDN attr, e.g. CN}: a RND in DN of
     * Subject SUBJECT_DN: entire DN of Subject SUBJECTALTNAME_OTHERNAME_UPN:
     * UPN(aka Principal Name) in otherName in subjectAltName extension
     * SUBJECTALTNAME_RFC822NAME: rfc822Name in subjectAltName extension
     * Zimbra-account-key: name: primary name or any of the aliases of an
     * account zimbraId: zimbraId of an account zimbraForeignPrincipal:
     * zimbraForeignPrincipal of an account. The matching value on the
     * zimbraForeignPrincipal must be prefixed with &quot;cert
     * {supported-certificate-filed}:&quot; e.g. cert
     * SUBJECTALTNAME_OTHERNAME_UPN:123456@mydomain LDAP-filter: An LDAP
     * filter template with placeholders to be substituted by certificate
     * field values. (objectClass=zimbraAccount) is internally ANDed with the
     * supplied filter. e.g.
     * (|(uid=%{SUBJECT_CN})(mail=%{SUBJECTALTNAME_RFC822NAME})) Note: it is
     * recommended not to use LDAP-filter rule, as it will trigger an LDAP
     * search for each cert auth request. LDAP-filter is disabled by default.
     * To enable it globally, set
     * zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled on global config
     * to TRUE. If LDAP-filter is not enabled, all client certificate
     * authentication will fail on domains configured with LDAP-filter.
     *
     * @param zimbraMailSSLClientCertPrincipalMap new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1215)
    public Map<String,Object> setMailSSLClientCertPrincipalMap(String zimbraMailSSLClientCertPrincipalMap, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLClientCertPrincipalMap, zimbraMailSSLClientCertPrincipalMap);
        return attrs;
    }

    /**
     * Map from a certificate field to a Zimbra account key that can uniquely
     * identify a Zimbra account for client certificate authentication. Value
     * is a comma-separated list of mapping rules, each mapping maps a
     * certificate field to a Zimbra account key. Each is attempted in
     * sequence until a unique account can be resolved. e.g. a value can be:
     * SUBJECTALTNAME_OTHERNAME_UPN=zimbraForeignPrincipal,(uid=%{SUBJECT_CN})
     * value: comma-separated mapping-rule mapping-rule:
     * {cert-field-to-zimbra-key-map} | {LDAP-filter}
     * cert-field-to-zimbra-key-map: {certificate-field}={Zimbra-account-key}
     * certificate-field: SUBJECT_{an RDN attr, e.g. CN}: a RND in DN of
     * Subject SUBJECT_DN: entire DN of Subject SUBJECTALTNAME_OTHERNAME_UPN:
     * UPN(aka Principal Name) in otherName in subjectAltName extension
     * SUBJECTALTNAME_RFC822NAME: rfc822Name in subjectAltName extension
     * Zimbra-account-key: name: primary name or any of the aliases of an
     * account zimbraId: zimbraId of an account zimbraForeignPrincipal:
     * zimbraForeignPrincipal of an account. The matching value on the
     * zimbraForeignPrincipal must be prefixed with &quot;cert
     * {supported-certificate-filed}:&quot; e.g. cert
     * SUBJECTALTNAME_OTHERNAME_UPN:123456@mydomain LDAP-filter: An LDAP
     * filter template with placeholders to be substituted by certificate
     * field values. (objectClass=zimbraAccount) is internally ANDed with the
     * supplied filter. e.g.
     * (|(uid=%{SUBJECT_CN})(mail=%{SUBJECTALTNAME_RFC822NAME})) Note: it is
     * recommended not to use LDAP-filter rule, as it will trigger an LDAP
     * search for each cert auth request. LDAP-filter is disabled by default.
     * To enable it globally, set
     * zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled on global config
     * to TRUE. If LDAP-filter is not enabled, all client certificate
     * authentication will fail on domains configured with LDAP-filter.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1215)
    public void unsetMailSSLClientCertPrincipalMap() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLClientCertPrincipalMap, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Map from a certificate field to a Zimbra account key that can uniquely
     * identify a Zimbra account for client certificate authentication. Value
     * is a comma-separated list of mapping rules, each mapping maps a
     * certificate field to a Zimbra account key. Each is attempted in
     * sequence until a unique account can be resolved. e.g. a value can be:
     * SUBJECTALTNAME_OTHERNAME_UPN=zimbraForeignPrincipal,(uid=%{SUBJECT_CN})
     * value: comma-separated mapping-rule mapping-rule:
     * {cert-field-to-zimbra-key-map} | {LDAP-filter}
     * cert-field-to-zimbra-key-map: {certificate-field}={Zimbra-account-key}
     * certificate-field: SUBJECT_{an RDN attr, e.g. CN}: a RND in DN of
     * Subject SUBJECT_DN: entire DN of Subject SUBJECTALTNAME_OTHERNAME_UPN:
     * UPN(aka Principal Name) in otherName in subjectAltName extension
     * SUBJECTALTNAME_RFC822NAME: rfc822Name in subjectAltName extension
     * Zimbra-account-key: name: primary name or any of the aliases of an
     * account zimbraId: zimbraId of an account zimbraForeignPrincipal:
     * zimbraForeignPrincipal of an account. The matching value on the
     * zimbraForeignPrincipal must be prefixed with &quot;cert
     * {supported-certificate-filed}:&quot; e.g. cert
     * SUBJECTALTNAME_OTHERNAME_UPN:123456@mydomain LDAP-filter: An LDAP
     * filter template with placeholders to be substituted by certificate
     * field values. (objectClass=zimbraAccount) is internally ANDed with the
     * supplied filter. e.g.
     * (|(uid=%{SUBJECT_CN})(mail=%{SUBJECTALTNAME_RFC822NAME})) Note: it is
     * recommended not to use LDAP-filter rule, as it will trigger an LDAP
     * search for each cert auth request. LDAP-filter is disabled by default.
     * To enable it globally, set
     * zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled on global config
     * to TRUE. If LDAP-filter is not enabled, all client certificate
     * authentication will fail on domains configured with LDAP-filter.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1215)
    public Map<String,Object> unsetMailSSLClientCertPrincipalMap(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLClientCertPrincipalMap, "");
        return attrs;
    }

    /**
     * Maximum number of entries for zimbraPrefMailTrustedSenderList.
     *
     * @return zimbraMailTrustedSenderListMaxNumEntries, or -1 if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1139)
    public int getMailTrustedSenderListMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraMailTrustedSenderListMaxNumEntries, -1);
    }

    /**
     * Maximum number of entries for zimbraPrefMailTrustedSenderList.
     *
     * @param zimbraMailTrustedSenderListMaxNumEntries new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1139)
    public void setMailTrustedSenderListMaxNumEntries(int zimbraMailTrustedSenderListMaxNumEntries) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTrustedSenderListMaxNumEntries, Integer.toString(zimbraMailTrustedSenderListMaxNumEntries));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of entries for zimbraPrefMailTrustedSenderList.
     *
     * @param zimbraMailTrustedSenderListMaxNumEntries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1139)
    public Map<String,Object> setMailTrustedSenderListMaxNumEntries(int zimbraMailTrustedSenderListMaxNumEntries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTrustedSenderListMaxNumEntries, Integer.toString(zimbraMailTrustedSenderListMaxNumEntries));
        return attrs;
    }

    /**
     * Maximum number of entries for zimbraPrefMailTrustedSenderList.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1139)
    public void unsetMailTrustedSenderListMaxNumEntries() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTrustedSenderListMaxNumEntries, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of entries for zimbraPrefMailTrustedSenderList.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1139)
    public Map<String,Object> unsetMailTrustedSenderListMaxNumEntries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTrustedSenderListMaxNumEntries, "");
        return attrs;
    }

    /**
     * certificate to be used for validating the SAML assertions received
     * from myonelogin (tricipher)
     *
     * @return zimbraMyoneloginSamlSigningCert, or null if unset
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1169)
    public String getMyoneloginSamlSigningCert() {
        return getAttr(Provisioning.A_zimbraMyoneloginSamlSigningCert, null);
    }

    /**
     * certificate to be used for validating the SAML assertions received
     * from myonelogin (tricipher)
     *
     * @param zimbraMyoneloginSamlSigningCert new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1169)
    public void setMyoneloginSamlSigningCert(String zimbraMyoneloginSamlSigningCert) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMyoneloginSamlSigningCert, zimbraMyoneloginSamlSigningCert);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * certificate to be used for validating the SAML assertions received
     * from myonelogin (tricipher)
     *
     * @param zimbraMyoneloginSamlSigningCert new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1169)
    public Map<String,Object> setMyoneloginSamlSigningCert(String zimbraMyoneloginSamlSigningCert, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMyoneloginSamlSigningCert, zimbraMyoneloginSamlSigningCert);
        return attrs;
    }

    /**
     * certificate to be used for validating the SAML assertions received
     * from myonelogin (tricipher)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1169)
    public void unsetMyoneloginSamlSigningCert() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMyoneloginSamlSigningCert, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * certificate to be used for validating the SAML assertions received
     * from myonelogin (tricipher)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1169)
    public Map<String,Object> unsetMyoneloginSamlSigningCert(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMyoneloginSamlSigningCert, "");
        return attrs;
    }

    /**
     * Deprecated since: 7.0.0. See bug 39647. Orig desc: Account for storing
     * templates and providing space for public wiki
     *
     * @return zimbraNotebookAccount, or null if unset
     */
    @ZAttr(id=363)
    public String getNotebookAccount() {
        return getAttr(Provisioning.A_zimbraNotebookAccount, null);
    }

    /**
     * Deprecated since: 7.0.0. See bug 39647. Orig desc: Account for storing
     * templates and providing space for public wiki
     *
     * @param zimbraNotebookAccount new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=363)
    public void setNotebookAccount(String zimbraNotebookAccount) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookAccount, zimbraNotebookAccount);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 7.0.0. See bug 39647. Orig desc: Account for storing
     * templates and providing space for public wiki
     *
     * @param zimbraNotebookAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=363)
    public Map<String,Object> setNotebookAccount(String zimbraNotebookAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookAccount, zimbraNotebookAccount);
        return attrs;
    }

    /**
     * Deprecated since: 7.0.0. See bug 39647. Orig desc: Account for storing
     * templates and providing space for public wiki
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=363)
    public void unsetNotebookAccount() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookAccount, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 7.0.0. See bug 39647. Orig desc: Account for storing
     * templates and providing space for public wiki
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=363)
    public Map<String,Object> unsetNotebookAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookAccount, "");
        return attrs;
    }

    /**
     * administrative notes
     *
     * @return zimbraNotes, or null if unset
     */
    @ZAttr(id=9)
    public String getNotes() {
        return getAttr(Provisioning.A_zimbraNotes, null);
    }

    /**
     * administrative notes
     *
     * @param zimbraNotes new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=9)
    public void setNotes(String zimbraNotes) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotes, zimbraNotes);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * administrative notes
     *
     * @param zimbraNotes new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=9)
    public Map<String,Object> setNotes(String zimbraNotes, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotes, zimbraNotes);
        return attrs;
    }

    /**
     * administrative notes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=9)
    public void unsetNotes() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotes, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * administrative notes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=9)
    public Map<String,Object> unsetNotes(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotes, "");
        return attrs;
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @return zimbraOAuthConsumerCredentials, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public String[] getOAuthConsumerCredentials() {
        return getMultiAttr(Provisioning.A_zimbraOAuthConsumerCredentials);
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param zimbraOAuthConsumerCredentials new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public void setOAuthConsumerCredentials(String[] zimbraOAuthConsumerCredentials) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOAuthConsumerCredentials, zimbraOAuthConsumerCredentials);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param zimbraOAuthConsumerCredentials new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public Map<String,Object> setOAuthConsumerCredentials(String[] zimbraOAuthConsumerCredentials, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOAuthConsumerCredentials, zimbraOAuthConsumerCredentials);
        return attrs;
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param zimbraOAuthConsumerCredentials new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public void addOAuthConsumerCredentials(String zimbraOAuthConsumerCredentials) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraOAuthConsumerCredentials, zimbraOAuthConsumerCredentials);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param zimbraOAuthConsumerCredentials new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public Map<String,Object> addOAuthConsumerCredentials(String zimbraOAuthConsumerCredentials, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraOAuthConsumerCredentials, zimbraOAuthConsumerCredentials);
        return attrs;
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param zimbraOAuthConsumerCredentials existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public void removeOAuthConsumerCredentials(String zimbraOAuthConsumerCredentials) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraOAuthConsumerCredentials, zimbraOAuthConsumerCredentials);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param zimbraOAuthConsumerCredentials existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public Map<String,Object> removeOAuthConsumerCredentials(String zimbraOAuthConsumerCredentials, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraOAuthConsumerCredentials, zimbraOAuthConsumerCredentials);
        return attrs;
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public void unsetOAuthConsumerCredentials() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOAuthConsumerCredentials, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public Map<String,Object> unsetOAuthConsumerCredentials(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOAuthConsumerCredentials, "");
        return attrs;
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @return zimbraOpenidConsumerAllowedOPEndpointURL, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public String[] getOpenidConsumerAllowedOPEndpointURL() {
        return getMultiAttr(Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL);
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param zimbraOpenidConsumerAllowedOPEndpointURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public void setOpenidConsumerAllowedOPEndpointURL(String[] zimbraOpenidConsumerAllowedOPEndpointURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, zimbraOpenidConsumerAllowedOPEndpointURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param zimbraOpenidConsumerAllowedOPEndpointURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public Map<String,Object> setOpenidConsumerAllowedOPEndpointURL(String[] zimbraOpenidConsumerAllowedOPEndpointURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, zimbraOpenidConsumerAllowedOPEndpointURL);
        return attrs;
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param zimbraOpenidConsumerAllowedOPEndpointURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public void addOpenidConsumerAllowedOPEndpointURL(String zimbraOpenidConsumerAllowedOPEndpointURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, zimbraOpenidConsumerAllowedOPEndpointURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param zimbraOpenidConsumerAllowedOPEndpointURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public Map<String,Object> addOpenidConsumerAllowedOPEndpointURL(String zimbraOpenidConsumerAllowedOPEndpointURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, zimbraOpenidConsumerAllowedOPEndpointURL);
        return attrs;
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param zimbraOpenidConsumerAllowedOPEndpointURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public void removeOpenidConsumerAllowedOPEndpointURL(String zimbraOpenidConsumerAllowedOPEndpointURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, zimbraOpenidConsumerAllowedOPEndpointURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param zimbraOpenidConsumerAllowedOPEndpointURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public Map<String,Object> removeOpenidConsumerAllowedOPEndpointURL(String zimbraOpenidConsumerAllowedOPEndpointURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, zimbraOpenidConsumerAllowedOPEndpointURL);
        return attrs;
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public void unsetOpenidConsumerAllowedOPEndpointURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public Map<String,Object> unsetOpenidConsumerAllowedOPEndpointURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraOpenidConsumerAllowedOPEndpointURL, "");
        return attrs;
    }

    /**
     * registered change password listener name
     *
     * @return zimbraPasswordChangeListener, or null if unset
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=586)
    public String getPasswordChangeListener() {
        return getAttr(Provisioning.A_zimbraPasswordChangeListener, null);
    }

    /**
     * registered change password listener name
     *
     * @param zimbraPasswordChangeListener new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=586)
    public void setPasswordChangeListener(String zimbraPasswordChangeListener) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordChangeListener, zimbraPasswordChangeListener);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * registered change password listener name
     *
     * @param zimbraPasswordChangeListener new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=586)
    public Map<String,Object> setPasswordChangeListener(String zimbraPasswordChangeListener, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordChangeListener, zimbraPasswordChangeListener);
        return attrs;
    }

    /**
     * registered change password listener name
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=586)
    public void unsetPasswordChangeListener() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordChangeListener, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * registered change password listener name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=586)
    public Map<String,Object> unsetPasswordChangeListener(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordChangeListener, "");
        return attrs;
    }

    /**
     * preauth secret key
     *
     * @return zimbraPreAuthKey, or null if unset
     */
    @ZAttr(id=307)
    public String getPreAuthKey() {
        return getAttr(Provisioning.A_zimbraPreAuthKey, null);
    }

    /**
     * preauth secret key
     *
     * @param zimbraPreAuthKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=307)
    public void setPreAuthKey(String zimbraPreAuthKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, zimbraPreAuthKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * preauth secret key
     *
     * @param zimbraPreAuthKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=307)
    public Map<String,Object> setPreAuthKey(String zimbraPreAuthKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, zimbraPreAuthKey);
        return attrs;
    }

    /**
     * preauth secret key
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=307)
    public void unsetPreAuthKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * preauth secret key
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=307)
    public Map<String,Object> unsetPreAuthKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, "");
        return attrs;
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @return zimbraPrefMailTrustedSenderList, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public String[] getPrefMailTrustedSenderList() {
        return getMultiAttr(Provisioning.A_zimbraPrefMailTrustedSenderList);
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param zimbraPrefMailTrustedSenderList new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public void setPrefMailTrustedSenderList(String[] zimbraPrefMailTrustedSenderList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailTrustedSenderList, zimbraPrefMailTrustedSenderList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param zimbraPrefMailTrustedSenderList new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public Map<String,Object> setPrefMailTrustedSenderList(String[] zimbraPrefMailTrustedSenderList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailTrustedSenderList, zimbraPrefMailTrustedSenderList);
        return attrs;
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param zimbraPrefMailTrustedSenderList new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public void addPrefMailTrustedSenderList(String zimbraPrefMailTrustedSenderList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefMailTrustedSenderList, zimbraPrefMailTrustedSenderList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param zimbraPrefMailTrustedSenderList new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public Map<String,Object> addPrefMailTrustedSenderList(String zimbraPrefMailTrustedSenderList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefMailTrustedSenderList, zimbraPrefMailTrustedSenderList);
        return attrs;
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param zimbraPrefMailTrustedSenderList existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public void removePrefMailTrustedSenderList(String zimbraPrefMailTrustedSenderList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefMailTrustedSenderList, zimbraPrefMailTrustedSenderList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param zimbraPrefMailTrustedSenderList existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public Map<String,Object> removePrefMailTrustedSenderList(String zimbraPrefMailTrustedSenderList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefMailTrustedSenderList, zimbraPrefMailTrustedSenderList);
        return attrs;
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public void unsetPrefMailTrustedSenderList() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailTrustedSenderList, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public Map<String,Object> unsetPrefMailTrustedSenderList(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailTrustedSenderList, "");
        return attrs;
    }

    /**
     * Skin to use for this account
     *
     * @return zimbraPrefSkin, or null if unset
     */
    @ZAttr(id=355)
    public String getPrefSkin() {
        return getAttr(Provisioning.A_zimbraPrefSkin, null);
    }

    /**
     * Skin to use for this account
     *
     * @param zimbraPrefSkin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=355)
    public void setPrefSkin(String zimbraPrefSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSkin, zimbraPrefSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Skin to use for this account
     *
     * @param zimbraPrefSkin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=355)
    public Map<String,Object> setPrefSkin(String zimbraPrefSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSkin, zimbraPrefSkin);
        return attrs;
    }

    /**
     * Skin to use for this account
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=355)
    public void unsetPrefSkin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSkin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Skin to use for this account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=355)
    public Map<String,Object> unsetPrefSkin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSkin, "");
        return attrs;
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @return zimbraPrefSpellIgnoreWord, or empty array if unset
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public String[] getPrefSpellIgnoreWord() {
        return getMultiAttr(Provisioning.A_zimbraPrefSpellIgnoreWord);
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param zimbraPrefSpellIgnoreWord new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public void setPrefSpellIgnoreWord(String[] zimbraPrefSpellIgnoreWord) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSpellIgnoreWord, zimbraPrefSpellIgnoreWord);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param zimbraPrefSpellIgnoreWord new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public Map<String,Object> setPrefSpellIgnoreWord(String[] zimbraPrefSpellIgnoreWord, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSpellIgnoreWord, zimbraPrefSpellIgnoreWord);
        return attrs;
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param zimbraPrefSpellIgnoreWord new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public void addPrefSpellIgnoreWord(String zimbraPrefSpellIgnoreWord) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefSpellIgnoreWord, zimbraPrefSpellIgnoreWord);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param zimbraPrefSpellIgnoreWord new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public Map<String,Object> addPrefSpellIgnoreWord(String zimbraPrefSpellIgnoreWord, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefSpellIgnoreWord, zimbraPrefSpellIgnoreWord);
        return attrs;
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param zimbraPrefSpellIgnoreWord existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public void removePrefSpellIgnoreWord(String zimbraPrefSpellIgnoreWord) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefSpellIgnoreWord, zimbraPrefSpellIgnoreWord);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param zimbraPrefSpellIgnoreWord existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public Map<String,Object> removePrefSpellIgnoreWord(String zimbraPrefSpellIgnoreWord, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefSpellIgnoreWord, zimbraPrefSpellIgnoreWord);
        return attrs;
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public void unsetPrefSpellIgnoreWord() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSpellIgnoreWord, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public Map<String,Object> unsetPrefSpellIgnoreWord(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSpellIgnoreWord, "");
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @return zimbraPrefTimeZoneId, or empty array if unset
     */
    @ZAttr(id=235)
    public String[] getPrefTimeZoneId() {
        return getMultiAttr(Provisioning.A_zimbraPrefTimeZoneId);
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=235)
    public void setPrefTimeZoneId(String[] zimbraPrefTimeZoneId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> setPrefTimeZoneId(String[] zimbraPrefTimeZoneId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=235)
    public void addPrefTimeZoneId(String zimbraPrefTimeZoneId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> addPrefTimeZoneId(String zimbraPrefTimeZoneId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=235)
    public void removePrefTimeZoneId(String zimbraPrefTimeZoneId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> removePrefTimeZoneId(String zimbraPrefTimeZoneId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=235)
    public void unsetPrefTimeZoneId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time zone of user or COS
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> unsetPrefTimeZoneId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, "");
        return attrs;
    }

    /**
     * Name to be used in public API such as REST or SOAP proxy.
     *
     * @return zimbraPublicServiceHostname, or null if unset
     */
    @ZAttr(id=377)
    public String getPublicServiceHostname() {
        return getAttr(Provisioning.A_zimbraPublicServiceHostname, null);
    }

    /**
     * Name to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServiceHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=377)
    public void setPublicServiceHostname(String zimbraPublicServiceHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceHostname, zimbraPublicServiceHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Name to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServiceHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=377)
    public Map<String,Object> setPublicServiceHostname(String zimbraPublicServiceHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceHostname, zimbraPublicServiceHostname);
        return attrs;
    }

    /**
     * Name to be used in public API such as REST or SOAP proxy.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=377)
    public void unsetPublicServiceHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Name to be used in public API such as REST or SOAP proxy.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=377)
    public Map<String,Object> unsetPublicServiceHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceHostname, "");
        return attrs;
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * <p>Use getPublicServicePortAsString to access value as a string.
     *
     * @see #getPublicServicePortAsString()
     *
     * @return zimbraPublicServicePort, or -1 if unset
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public int getPublicServicePort() {
        return getIntAttr(Provisioning.A_zimbraPublicServicePort, -1);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @return zimbraPublicServicePort, or null if unset
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public String getPublicServicePortAsString() {
        return getAttr(Provisioning.A_zimbraPublicServicePort, null);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServicePort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public void setPublicServicePort(int zimbraPublicServicePort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, Integer.toString(zimbraPublicServicePort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServicePort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public Map<String,Object> setPublicServicePort(int zimbraPublicServicePort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, Integer.toString(zimbraPublicServicePort));
        return attrs;
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServicePort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public void setPublicServicePortAsString(String zimbraPublicServicePort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, zimbraPublicServicePort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServicePort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public Map<String,Object> setPublicServicePortAsString(String zimbraPublicServicePort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, zimbraPublicServicePort);
        return attrs;
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public void unsetPublicServicePort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public Map<String,Object> unsetPublicServicePort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, "");
        return attrs;
    }

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @return zimbraPublicServiceProtocol, or null if unset
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=698)
    public String getPublicServiceProtocol() {
        return getAttr(Provisioning.A_zimbraPublicServiceProtocol, null);
    }

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServiceProtocol new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=698)
    public void setPublicServiceProtocol(String zimbraPublicServiceProtocol) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceProtocol, zimbraPublicServiceProtocol);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServiceProtocol new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=698)
    public Map<String,Object> setPublicServiceProtocol(String zimbraPublicServiceProtocol, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceProtocol, zimbraPublicServiceProtocol);
        return attrs;
    }

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=698)
    public void unsetPublicServiceProtocol() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceProtocol, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=698)
    public Map<String,Object> unsetPublicServiceProtocol(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceProtocol, "");
        return attrs;
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @return zimbraResponseHeader, or empty array if unset
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public String[] getResponseHeader() {
        return getMultiAttr(Provisioning.A_zimbraResponseHeader);
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param zimbraResponseHeader new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public void setResponseHeader(String[] zimbraResponseHeader) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraResponseHeader, zimbraResponseHeader);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param zimbraResponseHeader new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public Map<String,Object> setResponseHeader(String[] zimbraResponseHeader, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraResponseHeader, zimbraResponseHeader);
        return attrs;
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param zimbraResponseHeader new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public void addResponseHeader(String zimbraResponseHeader) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraResponseHeader, zimbraResponseHeader);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param zimbraResponseHeader new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public Map<String,Object> addResponseHeader(String zimbraResponseHeader, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraResponseHeader, zimbraResponseHeader);
        return attrs;
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param zimbraResponseHeader existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public void removeResponseHeader(String zimbraResponseHeader) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraResponseHeader, zimbraResponseHeader);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param zimbraResponseHeader existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public Map<String,Object> removeResponseHeader(String zimbraResponseHeader, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraResponseHeader, zimbraResponseHeader);
        return attrs;
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public void unsetResponseHeader() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraResponseHeader, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public Map<String,Object> unsetResponseHeader(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraResponseHeader, "");
        return attrs;
    }

    /**
     * CA certificate for authenticating client certificates in nginx proxy
     * (https only)
     *
     * @return zimbraReverseProxyClientCertCA, or null if unset
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1201)
    public String getReverseProxyClientCertCA() {
        return getAttr(Provisioning.A_zimbraReverseProxyClientCertCA, null);
    }

    /**
     * CA certificate for authenticating client certificates in nginx proxy
     * (https only)
     *
     * @param zimbraReverseProxyClientCertCA new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1201)
    public void setReverseProxyClientCertCA(String zimbraReverseProxyClientCertCA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertCA, zimbraReverseProxyClientCertCA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * CA certificate for authenticating client certificates in nginx proxy
     * (https only)
     *
     * @param zimbraReverseProxyClientCertCA new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1201)
    public Map<String,Object> setReverseProxyClientCertCA(String zimbraReverseProxyClientCertCA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertCA, zimbraReverseProxyClientCertCA);
        return attrs;
    }

    /**
     * CA certificate for authenticating client certificates in nginx proxy
     * (https only)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1201)
    public void unsetReverseProxyClientCertCA() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertCA, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * CA certificate for authenticating client certificates in nginx proxy
     * (https only)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1201)
    public Map<String,Object> unsetReverseProxyClientCertCA(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertCA, "");
        return attrs;
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @return zimbraReverseProxyClientCertMode, or ZAttrProvisioning.ReverseProxyClientCertMode.off if unset and/or has invalid value
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public ZAttrProvisioning.ReverseProxyClientCertMode getReverseProxyClientCertMode() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyClientCertMode); return v == null ? ZAttrProvisioning.ReverseProxyClientCertMode.off : ZAttrProvisioning.ReverseProxyClientCertMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.ReverseProxyClientCertMode.off; }
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @return zimbraReverseProxyClientCertMode, or "off" if unset
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public String getReverseProxyClientCertModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyClientCertMode, "off");
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @param zimbraReverseProxyClientCertMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public void setReverseProxyClientCertMode(ZAttrProvisioning.ReverseProxyClientCertMode zimbraReverseProxyClientCertMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertMode, zimbraReverseProxyClientCertMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @param zimbraReverseProxyClientCertMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public Map<String,Object> setReverseProxyClientCertMode(ZAttrProvisioning.ReverseProxyClientCertMode zimbraReverseProxyClientCertMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertMode, zimbraReverseProxyClientCertMode.toString());
        return attrs;
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @param zimbraReverseProxyClientCertMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public void setReverseProxyClientCertModeAsString(String zimbraReverseProxyClientCertMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertMode, zimbraReverseProxyClientCertMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @param zimbraReverseProxyClientCertMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public Map<String,Object> setReverseProxyClientCertModeAsString(String zimbraReverseProxyClientCertMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertMode, zimbraReverseProxyClientCertMode);
        return attrs;
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public void unsetReverseProxyClientCertMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * <p>Valid values: [off, on, optional]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public Map<String,Object> unsetReverseProxyClientCertMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyClientCertMode, "");
        return attrs;
    }

    /**
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup servlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @return zimbraReverseProxyUseExternalRoute, or false if unset
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public boolean isReverseProxyUseExternalRoute() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyUseExternalRoute, false);
    }

    /**
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup servlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @param zimbraReverseProxyUseExternalRoute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public void setReverseProxyUseExternalRoute(boolean zimbraReverseProxyUseExternalRoute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRoute, zimbraReverseProxyUseExternalRoute ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup servlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @param zimbraReverseProxyUseExternalRoute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public Map<String,Object> setReverseProxyUseExternalRoute(boolean zimbraReverseProxyUseExternalRoute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRoute, zimbraReverseProxyUseExternalRoute ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup servlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public void unsetReverseProxyUseExternalRoute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRoute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup servlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public Map<String,Object> unsetReverseProxyUseExternalRoute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRoute, "");
        return attrs;
    }

    /**
     * Use external route configured on domain if account cannot be found.
     * Also see zimbraReverseProxyUseExternalRoute.
     *
     * @return zimbraReverseProxyUseExternalRouteIfAccountNotExist, or false if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1132)
    public boolean isReverseProxyUseExternalRouteIfAccountNotExist() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyUseExternalRouteIfAccountNotExist, false);
    }

    /**
     * Use external route configured on domain if account cannot be found.
     * Also see zimbraReverseProxyUseExternalRoute.
     *
     * @param zimbraReverseProxyUseExternalRouteIfAccountNotExist new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1132)
    public void setReverseProxyUseExternalRouteIfAccountNotExist(boolean zimbraReverseProxyUseExternalRouteIfAccountNotExist) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRouteIfAccountNotExist, zimbraReverseProxyUseExternalRouteIfAccountNotExist ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Use external route configured on domain if account cannot be found.
     * Also see zimbraReverseProxyUseExternalRoute.
     *
     * @param zimbraReverseProxyUseExternalRouteIfAccountNotExist new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1132)
    public Map<String,Object> setReverseProxyUseExternalRouteIfAccountNotExist(boolean zimbraReverseProxyUseExternalRouteIfAccountNotExist, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRouteIfAccountNotExist, zimbraReverseProxyUseExternalRouteIfAccountNotExist ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Use external route configured on domain if account cannot be found.
     * Also see zimbraReverseProxyUseExternalRoute.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1132)
    public void unsetReverseProxyUseExternalRouteIfAccountNotExist() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRouteIfAccountNotExist, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Use external route configured on domain if account cannot be found.
     * Also see zimbraReverseProxyUseExternalRoute.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1132)
    public Map<String,Object> unsetReverseProxyUseExternalRouteIfAccountNotExist(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUseExternalRouteIfAccountNotExist, "");
        return attrs;
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @return zimbraSMIMELdapAttribute, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public String[] getSMIMELdapAttribute() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapAttribute);
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public void setSMIMELdapAttribute(String[] zimbraSMIMELdapAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, zimbraSMIMELdapAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public Map<String,Object> setSMIMELdapAttribute(String[] zimbraSMIMELdapAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, zimbraSMIMELdapAttribute);
        return attrs;
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapAttribute new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public void addSMIMELdapAttribute(String zimbraSMIMELdapAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapAttribute, zimbraSMIMELdapAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapAttribute new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public Map<String,Object> addSMIMELdapAttribute(String zimbraSMIMELdapAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapAttribute, zimbraSMIMELdapAttribute);
        return attrs;
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapAttribute existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public void removeSMIMELdapAttribute(String zimbraSMIMELdapAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapAttribute, zimbraSMIMELdapAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapAttribute existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public Map<String,Object> removeSMIMELdapAttribute(String zimbraSMIMELdapAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapAttribute, zimbraSMIMELdapAttribute);
        return attrs;
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public void unsetSMIMELdapAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public Map<String,Object> unsetSMIMELdapAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapAttribute, "");
        return attrs;
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @return zimbraSMIMELdapBindDn, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public String[] getSMIMELdapBindDn() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapBindDn);
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindDn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public void setSMIMELdapBindDn(String[] zimbraSMIMELdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, zimbraSMIMELdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public Map<String,Object> setSMIMELdapBindDn(String[] zimbraSMIMELdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, zimbraSMIMELdapBindDn);
        return attrs;
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindDn new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public void addSMIMELdapBindDn(String zimbraSMIMELdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapBindDn, zimbraSMIMELdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindDn new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public Map<String,Object> addSMIMELdapBindDn(String zimbraSMIMELdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapBindDn, zimbraSMIMELdapBindDn);
        return attrs;
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindDn existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public void removeSMIMELdapBindDn(String zimbraSMIMELdapBindDn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapBindDn, zimbraSMIMELdapBindDn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindDn existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public Map<String,Object> removeSMIMELdapBindDn(String zimbraSMIMELdapBindDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapBindDn, zimbraSMIMELdapBindDn);
        return attrs;
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public void unsetSMIMELdapBindDn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public Map<String,Object> unsetSMIMELdapBindDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindDn, "");
        return attrs;
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @return zimbraSMIMELdapBindPassword, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public String[] getSMIMELdapBindPassword() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapBindPassword);
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindPassword new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public void setSMIMELdapBindPassword(String[] zimbraSMIMELdapBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, zimbraSMIMELdapBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public Map<String,Object> setSMIMELdapBindPassword(String[] zimbraSMIMELdapBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, zimbraSMIMELdapBindPassword);
        return attrs;
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindPassword new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public void addSMIMELdapBindPassword(String zimbraSMIMELdapBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapBindPassword, zimbraSMIMELdapBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindPassword new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public Map<String,Object> addSMIMELdapBindPassword(String zimbraSMIMELdapBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapBindPassword, zimbraSMIMELdapBindPassword);
        return attrs;
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindPassword existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public void removeSMIMELdapBindPassword(String zimbraSMIMELdapBindPassword) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapBindPassword, zimbraSMIMELdapBindPassword);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapBindPassword existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public Map<String,Object> removeSMIMELdapBindPassword(String zimbraSMIMELdapBindPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapBindPassword, zimbraSMIMELdapBindPassword);
        return attrs;
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public void unsetSMIMELdapBindPassword() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public Map<String,Object> unsetSMIMELdapBindPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapBindPassword, "");
        return attrs;
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @return zimbraSMIMELdapDiscoverSearchBaseEnabled, or empty array if unset
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public String[] getSMIMELdapDiscoverSearchBaseEnabled() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled);
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapDiscoverSearchBaseEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public void setSMIMELdapDiscoverSearchBaseEnabled(String[] zimbraSMIMELdapDiscoverSearchBaseEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, zimbraSMIMELdapDiscoverSearchBaseEnabled);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapDiscoverSearchBaseEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public Map<String,Object> setSMIMELdapDiscoverSearchBaseEnabled(String[] zimbraSMIMELdapDiscoverSearchBaseEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, zimbraSMIMELdapDiscoverSearchBaseEnabled);
        return attrs;
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapDiscoverSearchBaseEnabled new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public void addSMIMELdapDiscoverSearchBaseEnabled(String zimbraSMIMELdapDiscoverSearchBaseEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, zimbraSMIMELdapDiscoverSearchBaseEnabled);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapDiscoverSearchBaseEnabled new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public Map<String,Object> addSMIMELdapDiscoverSearchBaseEnabled(String zimbraSMIMELdapDiscoverSearchBaseEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, zimbraSMIMELdapDiscoverSearchBaseEnabled);
        return attrs;
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapDiscoverSearchBaseEnabled existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public void removeSMIMELdapDiscoverSearchBaseEnabled(String zimbraSMIMELdapDiscoverSearchBaseEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, zimbraSMIMELdapDiscoverSearchBaseEnabled);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapDiscoverSearchBaseEnabled existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public Map<String,Object> removeSMIMELdapDiscoverSearchBaseEnabled(String zimbraSMIMELdapDiscoverSearchBaseEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, zimbraSMIMELdapDiscoverSearchBaseEnabled);
        return attrs;
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public void unsetSMIMELdapDiscoverSearchBaseEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public Map<String,Object> unsetSMIMELdapDiscoverSearchBaseEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapDiscoverSearchBaseEnabled, "");
        return attrs;
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @return zimbraSMIMELdapFilter, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public String[] getSMIMELdapFilter() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapFilter);
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapFilter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public void setSMIMELdapFilter(String[] zimbraSMIMELdapFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapFilter, zimbraSMIMELdapFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapFilter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public Map<String,Object> setSMIMELdapFilter(String[] zimbraSMIMELdapFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapFilter, zimbraSMIMELdapFilter);
        return attrs;
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapFilter new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public void addSMIMELdapFilter(String zimbraSMIMELdapFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapFilter, zimbraSMIMELdapFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapFilter new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public Map<String,Object> addSMIMELdapFilter(String zimbraSMIMELdapFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapFilter, zimbraSMIMELdapFilter);
        return attrs;
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapFilter existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public void removeSMIMELdapFilter(String zimbraSMIMELdapFilter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapFilter, zimbraSMIMELdapFilter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param zimbraSMIMELdapFilter existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public Map<String,Object> removeSMIMELdapFilter(String zimbraSMIMELdapFilter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapFilter, zimbraSMIMELdapFilter);
        return attrs;
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public void unsetSMIMELdapFilter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public Map<String,Object> unsetSMIMELdapFilter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapFilter, "");
        return attrs;
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @return zimbraSMIMELdapSearchBase, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public String[] getSMIMELdapSearchBase() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapSearchBase);
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public void setSMIMELdapSearchBase(String[] zimbraSMIMELdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, zimbraSMIMELdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public Map<String,Object> setSMIMELdapSearchBase(String[] zimbraSMIMELdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, zimbraSMIMELdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapSearchBase new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public void addSMIMELdapSearchBase(String zimbraSMIMELdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapSearchBase, zimbraSMIMELdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapSearchBase new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public Map<String,Object> addSMIMELdapSearchBase(String zimbraSMIMELdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapSearchBase, zimbraSMIMELdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapSearchBase existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public void removeSMIMELdapSearchBase(String zimbraSMIMELdapSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapSearchBase, zimbraSMIMELdapSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapSearchBase existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public Map<String,Object> removeSMIMELdapSearchBase(String zimbraSMIMELdapSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapSearchBase, zimbraSMIMELdapSearchBase);
        return attrs;
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public void unsetSMIMELdapSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public Map<String,Object> unsetSMIMELdapSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapSearchBase, "");
        return attrs;
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @return zimbraSMIMELdapStartTlsEnabled, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public String[] getSMIMELdapStartTlsEnabled() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapStartTlsEnabled);
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapStartTlsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public void setSMIMELdapStartTlsEnabled(String[] zimbraSMIMELdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapStartTlsEnabled, zimbraSMIMELdapStartTlsEnabled);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapStartTlsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public Map<String,Object> setSMIMELdapStartTlsEnabled(String[] zimbraSMIMELdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapStartTlsEnabled, zimbraSMIMELdapStartTlsEnabled);
        return attrs;
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapStartTlsEnabled new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public void addSMIMELdapStartTlsEnabled(String zimbraSMIMELdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapStartTlsEnabled, zimbraSMIMELdapStartTlsEnabled);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapStartTlsEnabled new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public Map<String,Object> addSMIMELdapStartTlsEnabled(String zimbraSMIMELdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapStartTlsEnabled, zimbraSMIMELdapStartTlsEnabled);
        return attrs;
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapStartTlsEnabled existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public void removeSMIMELdapStartTlsEnabled(String zimbraSMIMELdapStartTlsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapStartTlsEnabled, zimbraSMIMELdapStartTlsEnabled);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapStartTlsEnabled existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public Map<String,Object> removeSMIMELdapStartTlsEnabled(String zimbraSMIMELdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapStartTlsEnabled, zimbraSMIMELdapStartTlsEnabled);
        return attrs;
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public void unsetSMIMELdapStartTlsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapStartTlsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public Map<String,Object> unsetSMIMELdapStartTlsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapStartTlsEnabled, "");
        return attrs;
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @return zimbraSMIMELdapURL, or empty array if unset
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public String[] getSMIMELdapURL() {
        return getMultiAttr(Provisioning.A_zimbraSMIMELdapURL);
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public void setSMIMELdapURL(String[] zimbraSMIMELdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapURL, zimbraSMIMELdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public Map<String,Object> setSMIMELdapURL(String[] zimbraSMIMELdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapURL, zimbraSMIMELdapURL);
        return attrs;
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public void addSMIMELdapURL(String zimbraSMIMELdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapURL, zimbraSMIMELdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public Map<String,Object> addSMIMELdapURL(String zimbraSMIMELdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSMIMELdapURL, zimbraSMIMELdapURL);
        return attrs;
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public void removeSMIMELdapURL(String zimbraSMIMELdapURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapURL, zimbraSMIMELdapURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param zimbraSMIMELdapURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public Map<String,Object> removeSMIMELdapURL(String zimbraSMIMELdapURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSMIMELdapURL, zimbraSMIMELdapURL);
        return attrs;
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public void unsetSMIMELdapURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public Map<String,Object> unsetSMIMELdapURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSMIMELdapURL, "");
        return attrs;
    }

    /**
     * SSL certificate
     *
     * @return zimbraSSLCertificate, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=563)
    public String getSSLCertificate() {
        return getAttr(Provisioning.A_zimbraSSLCertificate, null);
    }

    /**
     * SSL certificate
     *
     * @param zimbraSSLCertificate new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=563)
    public void setSSLCertificate(String zimbraSSLCertificate) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLCertificate, zimbraSSLCertificate);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL certificate
     *
     * @param zimbraSSLCertificate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=563)
    public Map<String,Object> setSSLCertificate(String zimbraSSLCertificate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLCertificate, zimbraSSLCertificate);
        return attrs;
    }

    /**
     * SSL certificate
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=563)
    public void unsetSSLCertificate() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLCertificate, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL certificate
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=563)
    public Map<String,Object> unsetSSLCertificate(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLCertificate, "");
        return attrs;
    }

    /**
     * SSL private key
     *
     * @return zimbraSSLPrivateKey, or null if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=564)
    public String getSSLPrivateKey() {
        return getAttr(Provisioning.A_zimbraSSLPrivateKey, null);
    }

    /**
     * SSL private key
     *
     * @param zimbraSSLPrivateKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=564)
    public void setSSLPrivateKey(String zimbraSSLPrivateKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLPrivateKey, zimbraSSLPrivateKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL private key
     *
     * @param zimbraSSLPrivateKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=564)
    public Map<String,Object> setSSLPrivateKey(String zimbraSSLPrivateKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLPrivateKey, zimbraSSLPrivateKey);
        return attrs;
    }

    /**
     * SSL private key
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=564)
    public void unsetSSLPrivateKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLPrivateKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL private key
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=564)
    public Map<String,Object> unsetSSLPrivateKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLPrivateKey, "");
        return attrs;
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @return zimbraSkinBackgroundColor, or null if unset
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=648)
    public String getSkinBackgroundColor() {
        return getAttr(Provisioning.A_zimbraSkinBackgroundColor, null);
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @param zimbraSkinBackgroundColor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=648)
    public void setSkinBackgroundColor(String zimbraSkinBackgroundColor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinBackgroundColor, zimbraSkinBackgroundColor);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @param zimbraSkinBackgroundColor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=648)
    public Map<String,Object> setSkinBackgroundColor(String zimbraSkinBackgroundColor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinBackgroundColor, zimbraSkinBackgroundColor);
        return attrs;
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=648)
    public void unsetSkinBackgroundColor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinBackgroundColor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=648)
    public Map<String,Object> unsetSkinBackgroundColor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinBackgroundColor, "");
        return attrs;
    }

    /**
     * favicon for chameleon skin for the domain
     *
     * @return zimbraSkinFavicon, or null if unset
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=800)
    public String getSkinFavicon() {
        return getAttr(Provisioning.A_zimbraSkinFavicon, null);
    }

    /**
     * favicon for chameleon skin for the domain
     *
     * @param zimbraSkinFavicon new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=800)
    public void setSkinFavicon(String zimbraSkinFavicon) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinFavicon, zimbraSkinFavicon);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * favicon for chameleon skin for the domain
     *
     * @param zimbraSkinFavicon new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=800)
    public Map<String,Object> setSkinFavicon(String zimbraSkinFavicon, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinFavicon, zimbraSkinFavicon);
        return attrs;
    }

    /**
     * favicon for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=800)
    public void unsetSkinFavicon() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinFavicon, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * favicon for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=800)
    public Map<String,Object> unsetSkinFavicon(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinFavicon, "");
        return attrs;
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @return zimbraSkinForegroundColor, or null if unset
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=647)
    public String getSkinForegroundColor() {
        return getAttr(Provisioning.A_zimbraSkinForegroundColor, null);
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @param zimbraSkinForegroundColor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=647)
    public void setSkinForegroundColor(String zimbraSkinForegroundColor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinForegroundColor, zimbraSkinForegroundColor);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @param zimbraSkinForegroundColor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=647)
    public Map<String,Object> setSkinForegroundColor(String zimbraSkinForegroundColor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinForegroundColor, zimbraSkinForegroundColor);
        return attrs;
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=647)
    public void unsetSkinForegroundColor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinForegroundColor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=647)
    public Map<String,Object> unsetSkinForegroundColor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinForegroundColor, "");
        return attrs;
    }

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @return zimbraSkinLogoAppBanner, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=671)
    public String getSkinLogoAppBanner() {
        return getAttr(Provisioning.A_zimbraSkinLogoAppBanner, null);
    }

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @param zimbraSkinLogoAppBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=671)
    public void setSkinLogoAppBanner(String zimbraSkinLogoAppBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoAppBanner, zimbraSkinLogoAppBanner);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @param zimbraSkinLogoAppBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=671)
    public Map<String,Object> setSkinLogoAppBanner(String zimbraSkinLogoAppBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoAppBanner, zimbraSkinLogoAppBanner);
        return attrs;
    }

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=671)
    public void unsetSkinLogoAppBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoAppBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=671)
    public Map<String,Object> unsetSkinLogoAppBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoAppBanner, "");
        return attrs;
    }

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @return zimbraSkinLogoLoginBanner, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=670)
    public String getSkinLogoLoginBanner() {
        return getAttr(Provisioning.A_zimbraSkinLogoLoginBanner, null);
    }

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @param zimbraSkinLogoLoginBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=670)
    public void setSkinLogoLoginBanner(String zimbraSkinLogoLoginBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoLoginBanner, zimbraSkinLogoLoginBanner);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @param zimbraSkinLogoLoginBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=670)
    public Map<String,Object> setSkinLogoLoginBanner(String zimbraSkinLogoLoginBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoLoginBanner, zimbraSkinLogoLoginBanner);
        return attrs;
    }

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=670)
    public void unsetSkinLogoLoginBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoLoginBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=670)
    public Map<String,Object> unsetSkinLogoLoginBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoLoginBanner, "");
        return attrs;
    }

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @return zimbraSkinLogoURL, or null if unset
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=649)
    public String getSkinLogoURL() {
        return getAttr(Provisioning.A_zimbraSkinLogoURL, null);
    }

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @param zimbraSkinLogoURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=649)
    public void setSkinLogoURL(String zimbraSkinLogoURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoURL, zimbraSkinLogoURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @param zimbraSkinLogoURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=649)
    public Map<String,Object> setSkinLogoURL(String zimbraSkinLogoURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoURL, zimbraSkinLogoURL);
        return attrs;
    }

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=649)
    public void unsetSkinLogoURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=649)
    public Map<String,Object> unsetSkinLogoURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinLogoURL, "");
        return attrs;
    }

    /**
     * secondary color for chameleon skin for the domain
     *
     * @return zimbraSkinSecondaryColor, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=668)
    public String getSkinSecondaryColor() {
        return getAttr(Provisioning.A_zimbraSkinSecondaryColor, null);
    }

    /**
     * secondary color for chameleon skin for the domain
     *
     * @param zimbraSkinSecondaryColor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=668)
    public void setSkinSecondaryColor(String zimbraSkinSecondaryColor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSecondaryColor, zimbraSkinSecondaryColor);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * secondary color for chameleon skin for the domain
     *
     * @param zimbraSkinSecondaryColor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=668)
    public Map<String,Object> setSkinSecondaryColor(String zimbraSkinSecondaryColor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSecondaryColor, zimbraSkinSecondaryColor);
        return attrs;
    }

    /**
     * secondary color for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=668)
    public void unsetSkinSecondaryColor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSecondaryColor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * secondary color for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=668)
    public Map<String,Object> unsetSkinSecondaryColor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSecondaryColor, "");
        return attrs;
    }

    /**
     * selection color for chameleon skin for the domain
     *
     * @return zimbraSkinSelectionColor, or null if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=669)
    public String getSkinSelectionColor() {
        return getAttr(Provisioning.A_zimbraSkinSelectionColor, null);
    }

    /**
     * selection color for chameleon skin for the domain
     *
     * @param zimbraSkinSelectionColor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=669)
    public void setSkinSelectionColor(String zimbraSkinSelectionColor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSelectionColor, zimbraSkinSelectionColor);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * selection color for chameleon skin for the domain
     *
     * @param zimbraSkinSelectionColor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=669)
    public Map<String,Object> setSkinSelectionColor(String zimbraSkinSelectionColor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSelectionColor, zimbraSkinSelectionColor);
        return attrs;
    }

    /**
     * selection color for chameleon skin for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=669)
    public void unsetSkinSelectionColor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSelectionColor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * selection color for chameleon skin for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=669)
    public Map<String,Object> unsetSkinSelectionColor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinSelectionColor, "");
        return attrs;
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @return zimbraSmtpHostname, or empty array if unset
     */
    @ZAttr(id=97)
    public String[] getSmtpHostname() {
        return getMultiAttr(Provisioning.A_zimbraSmtpHostname);
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param zimbraSmtpHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=97)
    public void setSmtpHostname(String[] zimbraSmtpHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpHostname, zimbraSmtpHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param zimbraSmtpHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=97)
    public Map<String,Object> setSmtpHostname(String[] zimbraSmtpHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpHostname, zimbraSmtpHostname);
        return attrs;
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param zimbraSmtpHostname new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=97)
    public void addSmtpHostname(String zimbraSmtpHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSmtpHostname, zimbraSmtpHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param zimbraSmtpHostname new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=97)
    public Map<String,Object> addSmtpHostname(String zimbraSmtpHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSmtpHostname, zimbraSmtpHostname);
        return attrs;
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param zimbraSmtpHostname existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=97)
    public void removeSmtpHostname(String zimbraSmtpHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSmtpHostname, zimbraSmtpHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param zimbraSmtpHostname existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=97)
    public Map<String,Object> removeSmtpHostname(String zimbraSmtpHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSmtpHostname, zimbraSmtpHostname);
        return attrs;
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=97)
    public void unsetSmtpHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=97)
    public Map<String,Object> unsetSmtpHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpHostname, "");
        return attrs;
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * <p>Use getSmtpPortAsString to access value as a string.
     *
     * @see #getSmtpPortAsString()
     *
     * @return zimbraSmtpPort, or -1 if unset
     */
    @ZAttr(id=98)
    public int getSmtpPort() {
        return getIntAttr(Provisioning.A_zimbraSmtpPort, -1);
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @return zimbraSmtpPort, or null if unset
     */
    @ZAttr(id=98)
    public String getSmtpPortAsString() {
        return getAttr(Provisioning.A_zimbraSmtpPort, null);
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @param zimbraSmtpPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=98)
    public void setSmtpPort(int zimbraSmtpPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpPort, Integer.toString(zimbraSmtpPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @param zimbraSmtpPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=98)
    public Map<String,Object> setSmtpPort(int zimbraSmtpPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpPort, Integer.toString(zimbraSmtpPort));
        return attrs;
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @param zimbraSmtpPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=98)
    public void setSmtpPortAsString(String zimbraSmtpPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpPort, zimbraSmtpPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @param zimbraSmtpPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=98)
    public Map<String,Object> setSmtpPortAsString(String zimbraSmtpPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpPort, zimbraSmtpPort);
        return attrs;
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=98)
    public void unsetSmtpPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=98)
    public Map<String,Object> unsetSmtpPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpPort, "");
        return attrs;
    }

    /**
     * Value of the mail.smtp.sendpartial property
     *
     * @return zimbraSmtpSendPartial, or false if unset
     */
    @ZAttr(id=249)
    public boolean isSmtpSendPartial() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendPartial, false);
    }

    /**
     * Value of the mail.smtp.sendpartial property
     *
     * @param zimbraSmtpSendPartial new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=249)
    public void setSmtpSendPartial(boolean zimbraSmtpSendPartial) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendPartial, zimbraSmtpSendPartial ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value of the mail.smtp.sendpartial property
     *
     * @param zimbraSmtpSendPartial new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=249)
    public Map<String,Object> setSmtpSendPartial(boolean zimbraSmtpSendPartial, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendPartial, zimbraSmtpSendPartial ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Value of the mail.smtp.sendpartial property
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=249)
    public void unsetSmtpSendPartial() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendPartial, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value of the mail.smtp.sendpartial property
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=249)
    public Map<String,Object> unsetSmtpSendPartial(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendPartial, "");
        return attrs;
    }

    /**
     * timeout value in seconds
     *
     * @return zimbraSmtpTimeout, or -1 if unset
     */
    @ZAttr(id=99)
    public int getSmtpTimeout() {
        return getIntAttr(Provisioning.A_zimbraSmtpTimeout, -1);
    }

    /**
     * timeout value in seconds
     *
     * @param zimbraSmtpTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=99)
    public void setSmtpTimeout(int zimbraSmtpTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpTimeout, Integer.toString(zimbraSmtpTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * timeout value in seconds
     *
     * @param zimbraSmtpTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=99)
    public Map<String,Object> setSmtpTimeout(int zimbraSmtpTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpTimeout, Integer.toString(zimbraSmtpTimeout));
        return attrs;
    }

    /**
     * timeout value in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=99)
    public void unsetSmtpTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * timeout value in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=99)
    public Map<String,Object> unsetSmtpTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpTimeout, "");
        return attrs;
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @return zimbraSpamTrashAlias, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public String[] getSpamTrashAlias() {
        return getMultiAttr(Provisioning.A_zimbraSpamTrashAlias);
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param zimbraSpamTrashAlias new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public void setSpamTrashAlias(String[] zimbraSpamTrashAlias) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTrashAlias, zimbraSpamTrashAlias);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param zimbraSpamTrashAlias new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public Map<String,Object> setSpamTrashAlias(String[] zimbraSpamTrashAlias, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTrashAlias, zimbraSpamTrashAlias);
        return attrs;
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param zimbraSpamTrashAlias new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public void addSpamTrashAlias(String zimbraSpamTrashAlias) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSpamTrashAlias, zimbraSpamTrashAlias);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param zimbraSpamTrashAlias new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public Map<String,Object> addSpamTrashAlias(String zimbraSpamTrashAlias, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSpamTrashAlias, zimbraSpamTrashAlias);
        return attrs;
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param zimbraSpamTrashAlias existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public void removeSpamTrashAlias(String zimbraSpamTrashAlias) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSpamTrashAlias, zimbraSpamTrashAlias);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param zimbraSpamTrashAlias existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public Map<String,Object> removeSpamTrashAlias(String zimbraSpamTrashAlias, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSpamTrashAlias, zimbraSpamTrashAlias);
        return attrs;
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public void unsetSpamTrashAlias() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTrashAlias, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public Map<String,Object> unsetSpamTrashAlias(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTrashAlias, "");
        return attrs;
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @return zimbraStandardClientCustomPrefTab, or empty array if unset
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public String[] getStandardClientCustomPrefTab() {
        return getMultiAttr(Provisioning.A_zimbraStandardClientCustomPrefTab);
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param zimbraStandardClientCustomPrefTab new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public void setStandardClientCustomPrefTab(String[] zimbraStandardClientCustomPrefTab) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTab, zimbraStandardClientCustomPrefTab);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param zimbraStandardClientCustomPrefTab new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public Map<String,Object> setStandardClientCustomPrefTab(String[] zimbraStandardClientCustomPrefTab, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTab, zimbraStandardClientCustomPrefTab);
        return attrs;
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param zimbraStandardClientCustomPrefTab new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public void addStandardClientCustomPrefTab(String zimbraStandardClientCustomPrefTab) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraStandardClientCustomPrefTab, zimbraStandardClientCustomPrefTab);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param zimbraStandardClientCustomPrefTab new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public Map<String,Object> addStandardClientCustomPrefTab(String zimbraStandardClientCustomPrefTab, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraStandardClientCustomPrefTab, zimbraStandardClientCustomPrefTab);
        return attrs;
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param zimbraStandardClientCustomPrefTab existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public void removeStandardClientCustomPrefTab(String zimbraStandardClientCustomPrefTab) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraStandardClientCustomPrefTab, zimbraStandardClientCustomPrefTab);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param zimbraStandardClientCustomPrefTab existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public Map<String,Object> removeStandardClientCustomPrefTab(String zimbraStandardClientCustomPrefTab, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraStandardClientCustomPrefTab, zimbraStandardClientCustomPrefTab);
        return attrs;
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public void unsetStandardClientCustomPrefTab() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTab, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public Map<String,Object> unsetStandardClientCustomPrefTab(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTab, "");
        return attrs;
    }

    /**
     * whether extra custom tabs in the Preferences page in HTML client are
     * enabled
     *
     * @return zimbraStandardClientCustomPrefTabsEnabled, or false if unset
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1266)
    public boolean isStandardClientCustomPrefTabsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraStandardClientCustomPrefTabsEnabled, false);
    }

    /**
     * whether extra custom tabs in the Preferences page in HTML client are
     * enabled
     *
     * @param zimbraStandardClientCustomPrefTabsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1266)
    public void setStandardClientCustomPrefTabsEnabled(boolean zimbraStandardClientCustomPrefTabsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTabsEnabled, zimbraStandardClientCustomPrefTabsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether extra custom tabs in the Preferences page in HTML client are
     * enabled
     *
     * @param zimbraStandardClientCustomPrefTabsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1266)
    public Map<String,Object> setStandardClientCustomPrefTabsEnabled(boolean zimbraStandardClientCustomPrefTabsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTabsEnabled, zimbraStandardClientCustomPrefTabsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether extra custom tabs in the Preferences page in HTML client are
     * enabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1266)
    public void unsetStandardClientCustomPrefTabsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTabsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether extra custom tabs in the Preferences page in HTML client are
     * enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1266)
    public Map<String,Object> unsetStandardClientCustomPrefTabsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStandardClientCustomPrefTabsEnabled, "");
        return attrs;
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @return zimbraVirtualHostname, or empty array if unset
     */
    @ZAttr(id=352)
    public String[] getVirtualHostname() {
        return getMultiAttr(Provisioning.A_zimbraVirtualHostname);
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param zimbraVirtualHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=352)
    public void setVirtualHostname(String[] zimbraVirtualHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, zimbraVirtualHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param zimbraVirtualHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=352)
    public Map<String,Object> setVirtualHostname(String[] zimbraVirtualHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, zimbraVirtualHostname);
        return attrs;
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param zimbraVirtualHostname new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=352)
    public void addVirtualHostname(String zimbraVirtualHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraVirtualHostname, zimbraVirtualHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param zimbraVirtualHostname new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=352)
    public Map<String,Object> addVirtualHostname(String zimbraVirtualHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraVirtualHostname, zimbraVirtualHostname);
        return attrs;
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param zimbraVirtualHostname existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=352)
    public void removeVirtualHostname(String zimbraVirtualHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraVirtualHostname, zimbraVirtualHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param zimbraVirtualHostname existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=352)
    public Map<String,Object> removeVirtualHostname(String zimbraVirtualHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraVirtualHostname, zimbraVirtualHostname);
        return attrs;
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=352)
    public void unsetVirtualHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=352)
    public Map<String,Object> unsetVirtualHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, "");
        return attrs;
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @return zimbraVirtualIPAddress, or empty array if unset
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public String[] getVirtualIPAddress() {
        return getMultiAttr(Provisioning.A_zimbraVirtualIPAddress);
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param zimbraVirtualIPAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public void setVirtualIPAddress(String[] zimbraVirtualIPAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualIPAddress, zimbraVirtualIPAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param zimbraVirtualIPAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public Map<String,Object> setVirtualIPAddress(String[] zimbraVirtualIPAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualIPAddress, zimbraVirtualIPAddress);
        return attrs;
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param zimbraVirtualIPAddress new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public void addVirtualIPAddress(String zimbraVirtualIPAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraVirtualIPAddress, zimbraVirtualIPAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param zimbraVirtualIPAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public Map<String,Object> addVirtualIPAddress(String zimbraVirtualIPAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraVirtualIPAddress, zimbraVirtualIPAddress);
        return attrs;
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param zimbraVirtualIPAddress existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public void removeVirtualIPAddress(String zimbraVirtualIPAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraVirtualIPAddress, zimbraVirtualIPAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param zimbraVirtualIPAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public Map<String,Object> removeVirtualIPAddress(String zimbraVirtualIPAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraVirtualIPAddress, zimbraVirtualIPAddress);
        return attrs;
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public void unsetVirtualIPAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualIPAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public Map<String,Object> unsetVirtualIPAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirtualIPAddress, "");
        return attrs;
    }

    /**
     * link for admin users in the web client
     *
     * @return zimbraWebClientAdminReference, or null if unset
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=701)
    public String getWebClientAdminReference() {
        return getAttr(Provisioning.A_zimbraWebClientAdminReference, null);
    }

    /**
     * link for admin users in the web client
     *
     * @param zimbraWebClientAdminReference new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=701)
    public void setWebClientAdminReference(String zimbraWebClientAdminReference) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientAdminReference, zimbraWebClientAdminReference);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * link for admin users in the web client
     *
     * @param zimbraWebClientAdminReference new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=701)
    public Map<String,Object> setWebClientAdminReference(String zimbraWebClientAdminReference, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientAdminReference, zimbraWebClientAdminReference);
        return attrs;
    }

    /**
     * link for admin users in the web client
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=701)
    public void unsetWebClientAdminReference() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientAdminReference, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * link for admin users in the web client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=701)
    public Map<String,Object> unsetWebClientAdminReference(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientAdminReference, "");
        return attrs;
    }

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     *
     * @return zimbraWebClientLoginURL, or null if unset
     */
    @ZAttr(id=506)
    public String getWebClientLoginURL() {
        return getAttr(Provisioning.A_zimbraWebClientLoginURL, null);
    }

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     *
     * @param zimbraWebClientLoginURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=506)
    public void setWebClientLoginURL(String zimbraWebClientLoginURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURL, zimbraWebClientLoginURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     *
     * @param zimbraWebClientLoginURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=506)
    public Map<String,Object> setWebClientLoginURL(String zimbraWebClientLoginURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURL, zimbraWebClientLoginURL);
        return attrs;
    }

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=506)
    public void unsetWebClientLoginURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=506)
    public Map<String,Object> unsetWebClientLoginURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURL, "");
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @return zimbraWebClientLoginURLAllowedUA, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public String[] getWebClientLoginURLAllowedUA() {
        return getMultiAttr(Provisioning.A_zimbraWebClientLoginURLAllowedUA);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLoginURLAllowedUA new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public void setWebClientLoginURLAllowedUA(String[] zimbraWebClientLoginURLAllowedUA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURLAllowedUA, zimbraWebClientLoginURLAllowedUA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLoginURLAllowedUA new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public Map<String,Object> setWebClientLoginURLAllowedUA(String[] zimbraWebClientLoginURLAllowedUA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURLAllowedUA, zimbraWebClientLoginURLAllowedUA);
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLoginURLAllowedUA new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public void addWebClientLoginURLAllowedUA(String zimbraWebClientLoginURLAllowedUA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraWebClientLoginURLAllowedUA, zimbraWebClientLoginURLAllowedUA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLoginURLAllowedUA new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public Map<String,Object> addWebClientLoginURLAllowedUA(String zimbraWebClientLoginURLAllowedUA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraWebClientLoginURLAllowedUA, zimbraWebClientLoginURLAllowedUA);
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLoginURLAllowedUA existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public void removeWebClientLoginURLAllowedUA(String zimbraWebClientLoginURLAllowedUA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraWebClientLoginURLAllowedUA, zimbraWebClientLoginURLAllowedUA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLoginURLAllowedUA existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public Map<String,Object> removeWebClientLoginURLAllowedUA(String zimbraWebClientLoginURLAllowedUA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraWebClientLoginURLAllowedUA, zimbraWebClientLoginURLAllowedUA);
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public void unsetWebClientLoginURLAllowedUA() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURLAllowedUA, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public Map<String,Object> unsetWebClientLoginURLAllowedUA(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURLAllowedUA, "");
        return attrs;
    }

    /**
     * logout URL for web client to send the user to upon explicit logging
     * out
     *
     * @return zimbraWebClientLogoutURL, or null if unset
     */
    @ZAttr(id=507)
    public String getWebClientLogoutURL() {
        return getAttr(Provisioning.A_zimbraWebClientLogoutURL, null);
    }

    /**
     * logout URL for web client to send the user to upon explicit logging
     * out
     *
     * @param zimbraWebClientLogoutURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=507)
    public void setWebClientLogoutURL(String zimbraWebClientLogoutURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURL, zimbraWebClientLogoutURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for web client to send the user to upon explicit logging
     * out
     *
     * @param zimbraWebClientLogoutURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=507)
    public Map<String,Object> setWebClientLogoutURL(String zimbraWebClientLogoutURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURL, zimbraWebClientLogoutURL);
        return attrs;
    }

    /**
     * logout URL for web client to send the user to upon explicit logging
     * out
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=507)
    public void unsetWebClientLogoutURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for web client to send the user to upon explicit logging
     * out
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=507)
    public Map<String,Object> unsetWebClientLogoutURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURL, "");
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @return zimbraWebClientLogoutURLAllowedUA, or empty array if unset
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public String[] getWebClientLogoutURLAllowedUA() {
        return getMultiAttr(Provisioning.A_zimbraWebClientLogoutURLAllowedUA);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLogoutURLAllowedUA new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public void setWebClientLogoutURLAllowedUA(String[] zimbraWebClientLogoutURLAllowedUA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURLAllowedUA, zimbraWebClientLogoutURLAllowedUA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLogoutURLAllowedUA new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public Map<String,Object> setWebClientLogoutURLAllowedUA(String[] zimbraWebClientLogoutURLAllowedUA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURLAllowedUA, zimbraWebClientLogoutURLAllowedUA);
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLogoutURLAllowedUA new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public void addWebClientLogoutURLAllowedUA(String zimbraWebClientLogoutURLAllowedUA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraWebClientLogoutURLAllowedUA, zimbraWebClientLogoutURLAllowedUA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLogoutURLAllowedUA new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public Map<String,Object> addWebClientLogoutURLAllowedUA(String zimbraWebClientLogoutURLAllowedUA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraWebClientLogoutURLAllowedUA, zimbraWebClientLogoutURLAllowedUA);
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLogoutURLAllowedUA existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public void removeWebClientLogoutURLAllowedUA(String zimbraWebClientLogoutURLAllowedUA) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraWebClientLogoutURLAllowedUA, zimbraWebClientLogoutURLAllowedUA);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param zimbraWebClientLogoutURLAllowedUA existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public Map<String,Object> removeWebClientLogoutURLAllowedUA(String zimbraWebClientLogoutURLAllowedUA, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraWebClientLogoutURLAllowedUA, zimbraWebClientLogoutURLAllowedUA);
        return attrs;
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public void unsetWebClientLogoutURLAllowedUA() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURLAllowedUA, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public Map<String,Object> unsetWebClientLogoutURLAllowedUA(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientLogoutURLAllowedUA, "");
        return attrs;
    }

    /**
     * max input buffer length for web client
     *
     * @return zimbraWebClientMaxInputBufferLength, or 1024 if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1339)
    public int getWebClientMaxInputBufferLength() {
        return getIntAttr(Provisioning.A_zimbraWebClientMaxInputBufferLength, 1024);
    }

    /**
     * max input buffer length for web client
     *
     * @param zimbraWebClientMaxInputBufferLength new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1339)
    public void setWebClientMaxInputBufferLength(int zimbraWebClientMaxInputBufferLength) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientMaxInputBufferLength, Integer.toString(zimbraWebClientMaxInputBufferLength));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * max input buffer length for web client
     *
     * @param zimbraWebClientMaxInputBufferLength new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1339)
    public Map<String,Object> setWebClientMaxInputBufferLength(int zimbraWebClientMaxInputBufferLength, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientMaxInputBufferLength, Integer.toString(zimbraWebClientMaxInputBufferLength));
        return attrs;
    }

    /**
     * max input buffer length for web client
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1339)
    public void unsetWebClientMaxInputBufferLength() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientMaxInputBufferLength, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * max input buffer length for web client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1339)
    public Map<String,Object> unsetWebClientMaxInputBufferLength(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraWebClientMaxInputBufferLength, "");
        return attrs;
    }

    /**
     * whether zimlets that send sensitive data are disabled in
     * &quot;mixed&quot; zimbraMailMode
     *
     * @return zimbraZimletDataSensitiveInMixedModeDisabled, or true if unset
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1269)
    public boolean isZimletDataSensitiveInMixedModeDisabled() {
        return getBooleanAttr(Provisioning.A_zimbraZimletDataSensitiveInMixedModeDisabled, true);
    }

    /**
     * whether zimlets that send sensitive data are disabled in
     * &quot;mixed&quot; zimbraMailMode
     *
     * @param zimbraZimletDataSensitiveInMixedModeDisabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1269)
    public void setZimletDataSensitiveInMixedModeDisabled(boolean zimbraZimletDataSensitiveInMixedModeDisabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDataSensitiveInMixedModeDisabled, zimbraZimletDataSensitiveInMixedModeDisabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether zimlets that send sensitive data are disabled in
     * &quot;mixed&quot; zimbraMailMode
     *
     * @param zimbraZimletDataSensitiveInMixedModeDisabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1269)
    public Map<String,Object> setZimletDataSensitiveInMixedModeDisabled(boolean zimbraZimletDataSensitiveInMixedModeDisabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDataSensitiveInMixedModeDisabled, zimbraZimletDataSensitiveInMixedModeDisabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether zimlets that send sensitive data are disabled in
     * &quot;mixed&quot; zimbraMailMode
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1269)
    public void unsetZimletDataSensitiveInMixedModeDisabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDataSensitiveInMixedModeDisabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether zimlets that send sensitive data are disabled in
     * &quot;mixed&quot; zimbraMailMode
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1269)
    public Map<String,Object> unsetZimletDataSensitiveInMixedModeDisabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDataSensitiveInMixedModeDisabled, "");
        return attrs;
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @return zimbraZimletDomainAvailableZimlets, or empty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public String[] getZimletDomainAvailableZimlets() {
        return getMultiAttr(Provisioning.A_zimbraZimletDomainAvailableZimlets);
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param zimbraZimletDomainAvailableZimlets new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public void setZimletDomainAvailableZimlets(String[] zimbraZimletDomainAvailableZimlets) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDomainAvailableZimlets, zimbraZimletDomainAvailableZimlets);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param zimbraZimletDomainAvailableZimlets new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public Map<String,Object> setZimletDomainAvailableZimlets(String[] zimbraZimletDomainAvailableZimlets, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDomainAvailableZimlets, zimbraZimletDomainAvailableZimlets);
        return attrs;
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param zimbraZimletDomainAvailableZimlets new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public void addZimletDomainAvailableZimlets(String zimbraZimletDomainAvailableZimlets) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraZimletDomainAvailableZimlets, zimbraZimletDomainAvailableZimlets);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param zimbraZimletDomainAvailableZimlets new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public Map<String,Object> addZimletDomainAvailableZimlets(String zimbraZimletDomainAvailableZimlets, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraZimletDomainAvailableZimlets, zimbraZimletDomainAvailableZimlets);
        return attrs;
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param zimbraZimletDomainAvailableZimlets existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public void removeZimletDomainAvailableZimlets(String zimbraZimletDomainAvailableZimlets) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraZimletDomainAvailableZimlets, zimbraZimletDomainAvailableZimlets);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param zimbraZimletDomainAvailableZimlets existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public Map<String,Object> removeZimletDomainAvailableZimlets(String zimbraZimletDomainAvailableZimlets, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraZimletDomainAvailableZimlets, zimbraZimletDomainAvailableZimlets);
        return attrs;
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public void unsetZimletDomainAvailableZimlets() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDomainAvailableZimlets, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public Map<String,Object> unsetZimletDomainAvailableZimlets(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDomainAvailableZimlets, "");
        return attrs;
    }

    ///// END-AUTO-GEN-REPLACE

}

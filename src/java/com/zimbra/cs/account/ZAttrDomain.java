/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import com.zimbra.common.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public class ZAttrDomain extends NamedEntry {

    public ZAttrDomain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);

    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20081125-1427 */

    /**
     * RFC2256: descriptive information
     *
     * @return description, or null if unset
     */
    @ZAttr(id=-1)
    public String getDescription() {
        return getAttr(Provisioning.A_description, null);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setDescription(String description) throws com.zimbra.common.service.ServiceException {
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
    public Map<String,Object> setDescription(String description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, description);
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
     * @return zimbraACE, or ampty array if unset
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
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @return zimbraAdminConsoleLoginURL, or null if unset
     */
    @ZAttr(id=696)
    public String getAdminConsoleLoginURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLoginURL, null);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @param zimbraAdminConsoleLoginURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=696)
    public void setAdminConsoleLoginURL(String zimbraAdminConsoleLoginURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, zimbraAdminConsoleLoginURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @param zimbraAdminConsoleLoginURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=696)
    public Map<String,Object> setAdminConsoleLoginURL(String zimbraAdminConsoleLoginURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, zimbraAdminConsoleLoginURL);
        return attrs;
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=696)
    public void unsetAdminConsoleLoginURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=696)
    public Map<String,Object> unsetAdminConsoleLoginURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLoginURL, "");
        return attrs;
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @return zimbraAdminConsoleLogoutURL, or null if unset
     */
    @ZAttr(id=684)
    public String getAdminConsoleLogoutURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLogoutURL, null);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @param zimbraAdminConsoleLogoutURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=684)
    public void setAdminConsoleLogoutURL(String zimbraAdminConsoleLogoutURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, zimbraAdminConsoleLogoutURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @param zimbraAdminConsoleLogoutURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=684)
    public Map<String,Object> setAdminConsoleLogoutURL(String zimbraAdminConsoleLogoutURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, zimbraAdminConsoleLogoutURL);
        return attrs;
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=684)
    public void unsetAdminConsoleLogoutURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleLogoutURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * @return zimbraAuthLdapURL, or ampty array if unset
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
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @return zimbraAuthMech, or null if unset
     */
    @ZAttr(id=42)
    public String getAuthMech() {
        return getAttr(Provisioning.A_zimbraAuthMech, null);
    }

    /**
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
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
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
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
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
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
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
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
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @return zimbraAvailableSkin, or ampty array if unset
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
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain
     *
     * @return zimbraDomainCOSMaxAccounts, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public String[] getDomainCOSMaxAccounts() {
        return getMultiAttr(Provisioning.A_zimbraDomainCOSMaxAccounts);
    }

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain
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
     * in a domain
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
     * in a domain
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
     * in a domain
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
     * in a domain
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
     * in a domain
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
     * in a domain
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
     * in a domain
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
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @return zimbraDomainFeatureMaxAccounts, or ampty array if unset
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
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @return zimbraDomainStatus, or null if unset and/or has invalid value
     */
    @ZAttr(id=535)
    public ZAttrProvisioning.DomainStatus getDomainStatus() {
        try { String v = getAttr(Provisioning.A_zimbraDomainStatus); return v == null ? null : ZAttrProvisioning.DomainStatus.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @return zimbraDomainStatus, or null if unset
     */
    @ZAttr(id=535)
    public String getDomainStatusAsString() {
        return getAttr(Provisioning.A_zimbraDomainStatus, null);
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param zimbraDomainStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * <p>Valid values: [local, alias]
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
     * Exchange user password for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthPassword, or null if unset
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
     * <p>Valid values: [basic, form]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null if unset and/or has invalid value
     */
    @ZAttr(id=611)
    public ZAttrProvisioning.FreebusyExchangeAuthScheme getFreebusyExchangeAuthScheme() {
        try { String v = getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme); return v == null ? null : ZAttrProvisioning.FreebusyExchangeAuthScheme.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null if unset
     */
    @ZAttr(id=611)
    public String getFreebusyExchangeAuthSchemeAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme, null);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [basic, form]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [basic, form]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [basic, form]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [basic, form]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [basic, form]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     */
    @ZAttr(id=608)
    public Map<String,Object> unsetFreebusyExchangeAuthUsername(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, "");
        return attrs;
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * <p>Use getFreebusyExchangeCachedIntervalAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalAsString()
     *
     * @return zimbraFreebusyExchangeCachedInterval in millseconds, or 5184000000 (60d)  if unset
     */
    @ZAttr(id=621)
    public long getFreebusyExchangeCachedInterval() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, 5184000000L);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedInterval, or "60d" if unset
     */
    @ZAttr(id=621)
    public String getFreebusyExchangeCachedIntervalAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "60d");
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @param zimbraFreebusyExchangeCachedInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=621)
    public void setFreebusyExchangeCachedInterval(String zimbraFreebusyExchangeCachedInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, zimbraFreebusyExchangeCachedInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @param zimbraFreebusyExchangeCachedInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=621)
    public Map<String,Object> setFreebusyExchangeCachedInterval(String zimbraFreebusyExchangeCachedInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, zimbraFreebusyExchangeCachedInterval);
        return attrs;
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=621)
    public void unsetFreebusyExchangeCachedInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=621)
    public Map<String,Object> unsetFreebusyExchangeCachedInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "");
        return attrs;
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * <p>Use getFreebusyExchangeCachedIntervalStartAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalStartAsString()
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart in millseconds, or 604800000 (7d)  if unset
     */
    @ZAttr(id=620)
    public long getFreebusyExchangeCachedIntervalStart() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, 604800000L);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart, or "7d" if unset
     */
    @ZAttr(id=620)
    public String getFreebusyExchangeCachedIntervalStartAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "7d");
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @param zimbraFreebusyExchangeCachedIntervalStart new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=620)
    public void setFreebusyExchangeCachedIntervalStart(String zimbraFreebusyExchangeCachedIntervalStart) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, zimbraFreebusyExchangeCachedIntervalStart);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @param zimbraFreebusyExchangeCachedIntervalStart new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=620)
    public Map<String,Object> setFreebusyExchangeCachedIntervalStart(String zimbraFreebusyExchangeCachedIntervalStart, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, zimbraFreebusyExchangeCachedIntervalStart);
        return attrs;
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=620)
    public void unsetFreebusyExchangeCachedIntervalStart() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=620)
    public Map<String,Object> unsetFreebusyExchangeCachedIntervalStart(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "");
        return attrs;
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeURL, or null if unset
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
     */
    @ZAttr(id=610)
    public Map<String,Object> unsetFreebusyExchangeUserOrg(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, "");
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
     * LDAP search base for interal GAL queries (special values:
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
     * LDAP search base for interal GAL queries (special values:
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
     * LDAP search base for interal GAL queries (special values:
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
     * LDAP search base for interal GAL queries (special values:
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
     * LDAP search base for interal GAL queries (special values:
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
     * @return zimbraGalLdapAttrMap, or ampty array if unset
     */
    @ZAttr(id=153)
    public String[] getGalLdapAttrMap() {
        String[] value = getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap); return value.length > 0 ? value : new String[] {"co=workCountry","company=company","givenName,gn=firstName","sn=lastName","displayName,cn=fullName","initials=initials","description=notes","l=workCity","physicalDeliveryOfficeName=office","ou=department","street,streetAddress=workStreet","postalCode=workPostalCode","telephoneNumber=workPhone","st=workState","zimbraMailDeliveryAddress,zimbraMailAlias,mail=email,email2,email3,email4,email5,email6,email7,email8,email9,email10,email11,email12,email13,email14,email15,email16","title=jobTitle","whenChanged,modifyTimeStamp=modifyTimeStamp","whenCreated,createTimeStamp=createTimeStamp","zimbraId=zimbraId","objectClass=objectClass","zimbraMailForwardingAddress=zimbraMailForwardingAddress","zimbraCalResType=zimbraCalResType","zimbraCalResLocationDisplayName=zimbraCalResLocationDisplayName"};
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @return zimbraGalLdapAuthMech, or null if unset and/or has invalid value
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @return zimbraGalLdapAuthMech, or null if unset
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * kerberos5 keytab file path for external GAL queries
     *
     * @return zimbraGalLdapKerberos5Keytab, or null if unset
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
     */
    @ZAttr(id=550)
    public Map<String,Object> unsetGalLdapKerberos5Principal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Principal, "");
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
     *
     * @return zimbraGalLdapPageSize, or 1000 if unset
     */
    @ZAttr(id=583)
    public int getGalLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalLdapPageSize, 1000);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
     *
     * @param zimbraGalLdapPageSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=583)
    public void setGalLdapPageSize(int zimbraGalLdapPageSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, Integer.toString(zimbraGalLdapPageSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
     *
     * @param zimbraGalLdapPageSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=583)
    public Map<String,Object> setGalLdapPageSize(int zimbraGalLdapPageSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, Integer.toString(zimbraGalLdapPageSize));
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=583)
    public void unsetGalLdapPageSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * @return zimbraGalLdapURL, or ampty array if unset
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
     *
     * @return zimbraGalMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=46)
    public ZAttrProvisioning.GalMode getGalMode() {
        try { String v = getAttr(Provisioning.A_zimbraGalMode); return v == null ? null : ZAttrProvisioning.GalMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
     *
     * @return zimbraGalMode, or null if unset
     */
    @ZAttr(id=46)
    public String getGalModeAsString() {
        return getAttr(Provisioning.A_zimbraGalMode, null);
    }

    /**
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @return zimbraGalSyncLdapAuthMech, or null if unset and/or has invalid value
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @return zimbraGalSyncLdapAuthMech, or null if unset
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param zimbraGalSyncLdapAuthMech new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     */
    @ZAttr(id=595)
    public Map<String,Object> unsetGalSyncLdapKerberos5Principal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, "");
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @return zimbraGalSyncLdapPageSize, or 1000 if unset
     */
    @ZAttr(id=597)
    public int getGalSyncLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalSyncLdapPageSize, 1000);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @param zimbraGalSyncLdapPageSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=597)
    public void setGalSyncLdapPageSize(int zimbraGalSyncLdapPageSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, Integer.toString(zimbraGalSyncLdapPageSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @param zimbraGalSyncLdapPageSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=597)
    public Map<String,Object> setGalSyncLdapPageSize(int zimbraGalSyncLdapPageSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, Integer.toString(zimbraGalSyncLdapPageSize));
        return attrs;
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=597)
    public void unsetGalSyncLdapPageSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapPageSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * @return zimbraGalSyncLdapURL, or ampty array if unset
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
     */
    @ZAttr(id=589)
    public Map<String,Object> unsetGalSyncLdapURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapURL, "");
        return attrs;
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @return zimbraGalTokenizeAutoCompleteKey, or null if unset and/or has invalid value
     */
    @ZAttr(id=599)
    public ZAttrProvisioning.GalTokenizeAutoCompleteKey getGalTokenizeAutoCompleteKey() {
        try { String v = getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey); return v == null ? null : ZAttrProvisioning.GalTokenizeAutoCompleteKey.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @return zimbraGalTokenizeAutoCompleteKey, or null if unset
     */
    @ZAttr(id=599)
    public String getGalTokenizeAutoCompleteKeyAsString() {
        return getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, null);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeAutoCompleteKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [and, or]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [and, or]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [and, or]
     *
     * @return zimbraGalTokenizeSearchKey, or null if unset and/or has invalid value
     */
    @ZAttr(id=600)
    public ZAttrProvisioning.GalTokenizeSearchKey getGalTokenizeSearchKey() {
        try { String v = getAttr(Provisioning.A_zimbraGalTokenizeSearchKey); return v == null ? null : ZAttrProvisioning.GalTokenizeSearchKey.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @return zimbraGalTokenizeSearchKey, or null if unset
     */
    @ZAttr(id=600)
    public String getGalTokenizeSearchKeyAsString() {
        return getAttr(Provisioning.A_zimbraGalTokenizeSearchKey, null);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [and, or]
     *
     * @param zimbraGalTokenizeSearchKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * <p>Valid values: [and, or]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     * <p>Valid values: [and, or]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * Account for storing templates and providing space for public wiki
     *
     * @return zimbraNotebookAccount, or null if unset
     */
    @ZAttr(id=363)
    public String getNotebookAccount() {
        return getAttr(Provisioning.A_zimbraNotebookAccount, null);
    }

    /**
     * Account for storing templates and providing space for public wiki
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
     * Account for storing templates and providing space for public wiki
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
     * Account for storing templates and providing space for public wiki
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
     * Account for storing templates and providing space for public wiki
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
     * registered change password listener name
     *
     * @return zimbraPasswordChangeListener, or null if unset
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
     * time zone of user or COS
     *
     * @return zimbraPrefTimeZoneId, or ampty array if unset
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
     * @return zimbraPublicServicePort, or null if unset
     */
    @ZAttr(id=699)
    public String getPublicServicePort() {
        return getAttr(Provisioning.A_zimbraPublicServicePort, null);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @param zimbraPublicServicePort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=699)
    public void setPublicServicePort(String zimbraPublicServicePort) throws com.zimbra.common.service.ServiceException {
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
     */
    @ZAttr(id=699)
    public Map<String,Object> setPublicServicePort(String zimbraPublicServicePort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServicePort, zimbraPublicServicePort);
        return attrs;
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
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
     */
    @ZAttr(id=698)
    public Map<String,Object> unsetPublicServiceProtocol(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPublicServiceProtocol, "");
        return attrs;
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @return zimbraSkinBackgroundColor, or null if unset
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
     */
    @ZAttr(id=648)
    public Map<String,Object> unsetSkinBackgroundColor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSkinBackgroundColor, "");
        return attrs;
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @return zimbraSkinForegroundColor, or null if unset
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
     * @return zimbraSmtpHostname, or ampty array if unset
     */
    @ZAttr(id=97)
    public String[] getSmtpHostname() {
        String[] value = getMultiAttr(Provisioning.A_zimbraSmtpHostname); return value.length > 0 ? value : new String[] {"localhost"};
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
     * @return zimbraSmtpPort, or "25" if unset
     */
    @ZAttr(id=98)
    public String getSmtpPort() {
        return getAttr(Provisioning.A_zimbraSmtpPort, "25");
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @param zimbraSmtpPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=98)
    public void setSmtpPort(String zimbraSmtpPort) throws com.zimbra.common.service.ServiceException {
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
    public Map<String,Object> setSmtpPort(String zimbraSmtpPort, Map<String,Object> attrs) {
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
     * @return zimbraSmtpTimeout, or 60 if unset
     */
    @ZAttr(id=99)
    public int getSmtpTimeout() {
        return getIntAttr(Provisioning.A_zimbraSmtpTimeout, 60);
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
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @return zimbraVirtualHostname, or ampty array if unset
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
     * @return zimbraVirtualIPAddress, or ampty array if unset
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
     * logout URL for web client to send the user to upon explicit loggin out
     *
     * @return zimbraWebClientLogoutURL, or null if unset
     */
    @ZAttr(id=507)
    public String getWebClientLogoutURL() {
        return getAttr(Provisioning.A_zimbraWebClientLogoutURL, null);
    }

    /**
     * logout URL for web client to send the user to upon explicit loggin out
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
     * logout URL for web client to send the user to upon explicit loggin out
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
     * logout URL for web client to send the user to upon explicit loggin out
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
     * logout URL for web client to send the user to upon explicit loggin out
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
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute.
     *
     * @return zimbraZimletDomainAvailableZimlets, or ampty array if unset
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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
     * zimbraZimletAvailableZimlets and this attribute.
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

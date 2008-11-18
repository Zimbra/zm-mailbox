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

    /* build: 5.0 pshao 20081118-1352 */

    /**
     * RFC2256: descriptive information
     *
     * @return description, or null unset
     */
    @ZAttr(id=-1)
    public String getDescription() {
        return getAttr(Provisioning.A_description);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public Map<String,Object> setAdminConsoleCatchAllAddressEnabled(boolean zimbraAdminConsoleCatchAllAddressEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, Boolean.toString(zimbraAdminConsoleCatchAllAddressEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public Map<String,Object> setAdminConsoleDNSCheckEnabled(boolean zimbraAdminConsoleDNSCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, Boolean.toString(zimbraAdminConsoleDNSCheckEnabled));
        return attrs;
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
     * @return zimbraAdminConsoleLoginURL, or null unset
     */
    @ZAttr(id=696)
    public String getAdminConsoleLoginURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLoginURL);
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
     * @return zimbraAdminConsoleLogoutURL, or null unset
     */
    @ZAttr(id=684)
    public String getAdminConsoleLogoutURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLogoutURL);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public Map<String,Object> setAdminConsoleSkinEnabled(boolean zimbraAdminConsoleSkinEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleSkinEnabled, Boolean.toString(zimbraAdminConsoleSkinEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=257)
    public Map<String,Object> setAuthFallbackToLocal(boolean zimbraAuthFallbackToLocal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthFallbackToLocal, Boolean.toString(zimbraAuthFallbackToLocal));
        return attrs;
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
     * @return zimbraAuthKerberos5Realm, or null unset
     */
    @ZAttr(id=548)
    public String getAuthKerberos5Realm() {
        return getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
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
     * @return zimbraAuthLdapBindDn, or null unset
     */
    @ZAttr(id=44)
    public String getAuthLdapBindDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapBindDn);
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
     * @return zimbraAuthLdapSearchBase, or null unset
     */
    @ZAttr(id=252)
    public String getAuthLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBase);
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
     * @return zimbraAuthLdapSearchBindDn, or null unset
     */
    @ZAttr(id=253)
    public String getAuthLdapSearchBindDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBindDn);
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
     * @return zimbraAuthLdapSearchBindPassword, or null unset
     */
    @ZAttr(id=254)
    public String getAuthLdapSearchBindPassword() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBindPassword);
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
     * @return zimbraAuthLdapSearchFilter, or null unset
     */
    @ZAttr(id=255)
    public String getAuthLdapSearchFilter() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchFilter);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=654)
    public Map<String,Object> setAuthLdapStartTlsEnabled(boolean zimbraAuthLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, Boolean.toString(zimbraAuthLdapStartTlsEnabled));
        return attrs;
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
     * @return zimbraAuthMech, or null unset
     */
    @ZAttr(id=42)
    public String getAuthMech() {
        return getAttr(Provisioning.A_zimbraAuthMech);
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
     * @return zimbraDNSCheckHostname, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public String getDNSCheckHostname() {
        return getAttr(Provisioning.A_zimbraDNSCheckHostname);
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
     * @return zimbraDomainDefaultCOSId, or null unset
     */
    @ZAttr(id=299)
    public String getDomainDefaultCOSId() {
        return getAttr(Provisioning.A_zimbraDomainDefaultCOSId);
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
     * @return zimbraDomainName, or null unset
     */
    @ZAttr(id=19)
    public String getDomainName() {
        return getAttr(Provisioning.A_zimbraDomainName);
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
     * @return zimbraDomainRenameInfo, or null unset
     */
    @ZAttr(id=536)
    public String getDomainRenameInfo() {
        return getAttr(Provisioning.A_zimbraDomainRenameInfo);
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
     * @return zimbraDomainStatus, or null unset
     */
    @ZAttr(id=535)
    public String getDomainStatusAsString() {
        return getAttr(Provisioning.A_zimbraDomainStatus);
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
     * @return zimbraDomainType, or null unset
     */
    @ZAttr(id=212)
    public String getDomainTypeAsString() {
        return getAttr(Provisioning.A_zimbraDomainType);
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
     * @return zimbraFreebusyExchangeAuthPassword, or null unset
     */
    @ZAttr(id=609)
    public String getFreebusyExchangeAuthPassword() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword);
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
     * @return zimbraFreebusyExchangeAuthScheme, or null unset
     */
    @ZAttr(id=611)
    public String getFreebusyExchangeAuthSchemeAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme);
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
     * @return zimbraFreebusyExchangeAuthUsername, or null unset
     */
    @ZAttr(id=608)
    public String getFreebusyExchangeAuthUsername() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername);
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
     * @return zimbraFreebusyExchangeCachedInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=621)
    public long getFreebusyExchangeCachedInterval() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, -1);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedInterval, or null unset
     */
    @ZAttr(id=621)
    public String getFreebusyExchangeCachedIntervalAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedInterval);
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
     * @return zimbraFreebusyExchangeCachedIntervalStart in millseconds, or -1 if unset
     */
    @ZAttr(id=620)
    public long getFreebusyExchangeCachedIntervalStart() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, -1);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart, or null unset
     */
    @ZAttr(id=620)
    public String getFreebusyExchangeCachedIntervalStartAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart);
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
     * @return zimbraFreebusyExchangeURL, or null unset
     */
    @ZAttr(id=607)
    public String getFreebusyExchangeURL() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeURL);
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
     * @return zimbraFreebusyExchangeUserOrg, or null unset
     */
    @ZAttr(id=610)
    public String getFreebusyExchangeUserOrg() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeUserOrg);
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
     * @return zimbraGalAutoCompleteLdapFilter, or null unset
     */
    @ZAttr(id=360)
    public String getGalAutoCompleteLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalAutoCompleteLdapFilter);
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
     * @return zimbraGalInternalSearchBase, or null unset
     */
    @ZAttr(id=358)
    public String getGalInternalSearchBase() {
        return getAttr(Provisioning.A_zimbraGalInternalSearchBase);
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
        return getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
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
     * @return zimbraGalLdapAuthMech, or null unset
     */
    @ZAttr(id=549)
    public String getGalLdapAuthMechAsString() {
        return getAttr(Provisioning.A_zimbraGalLdapAuthMech);
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
     * @return zimbraGalLdapBindDn, or null unset
     */
    @ZAttr(id=49)
    public String getGalLdapBindDn() {
        return getAttr(Provisioning.A_zimbraGalLdapBindDn);
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
     * @return zimbraGalLdapBindPassword, or null unset
     */
    @ZAttr(id=50)
    public String getGalLdapBindPassword() {
        return getAttr(Provisioning.A_zimbraGalLdapBindPassword);
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
     * @return zimbraGalLdapFilter, or null unset
     */
    @ZAttr(id=51)
    public String getGalLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalLdapFilter);
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
     * @return zimbraGalLdapKerberos5Keytab, or null unset
     */
    @ZAttr(id=551)
    public String getGalLdapKerberos5Keytab() {
        return getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
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
     * @return zimbraGalLdapKerberos5Principal, or null unset
     */
    @ZAttr(id=550)
    public String getGalLdapKerberos5Principal() {
        return getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
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
     * @return zimbraGalLdapPageSize, or -1 if unset
     */
    @ZAttr(id=583)
    public int getGalLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalLdapPageSize, -1);
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
     * @return zimbraGalLdapSearchBase, or null unset
     */
    @ZAttr(id=48)
    public String getGalLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraGalLdapSearchBase);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=655)
    public Map<String,Object> setGalLdapStartTlsEnabled(boolean zimbraGalLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapStartTlsEnabled, Boolean.toString(zimbraGalLdapStartTlsEnabled));
        return attrs;
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
     * @return zimbraGalMaxResults, or -1 if unset
     */
    @ZAttr(id=53)
    public int getGalMaxResults() {
        return getIntAttr(Provisioning.A_zimbraGalMaxResults, -1);
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
     * @return zimbraGalMode, or null unset
     */
    @ZAttr(id=46)
    public String getGalModeAsString() {
        return getAttr(Provisioning.A_zimbraGalMode);
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
     * @return zimbraGalSyncInternalSearchBase, or null unset
     */
    @ZAttr(id=598)
    public String getGalSyncInternalSearchBase() {
        return getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
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
     * @return zimbraGalSyncLdapAuthMech, or null unset
     */
    @ZAttr(id=592)
    public String getGalSyncLdapAuthMechAsString() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech);
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
     * @return zimbraGalSyncLdapBindDn, or null unset
     */
    @ZAttr(id=593)
    public String getGalSyncLdapBindDn() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapBindDn);
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
     * @return zimbraGalSyncLdapBindPassword, or null unset
     */
    @ZAttr(id=594)
    public String getGalSyncLdapBindPassword() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword);
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
     * @return zimbraGalSyncLdapFilter, or null unset
     */
    @ZAttr(id=591)
    public String getGalSyncLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapFilter);
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
     * @return zimbraGalSyncLdapKerberos5Keytab, or null unset
     */
    @ZAttr(id=596)
    public String getGalSyncLdapKerberos5Keytab() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
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
     * @return zimbraGalSyncLdapKerberos5Principal, or null unset
     */
    @ZAttr(id=595)
    public String getGalSyncLdapKerberos5Principal() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
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
     * @return zimbraGalSyncLdapPageSize, or -1 if unset
     */
    @ZAttr(id=597)
    public int getGalSyncLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalSyncLdapPageSize, -1);
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
     * @return zimbraGalSyncLdapSearchBase, or null unset
     */
    @ZAttr(id=590)
    public String getGalSyncLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=656)
    public Map<String,Object> setGalSyncLdapStartTlsEnabled(boolean zimbraGalSyncLdapStartTlsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, Boolean.toString(zimbraGalSyncLdapStartTlsEnabled));
        return attrs;
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
     * @return zimbraGalTokenizeAutoCompleteKey, or null unset
     */
    @ZAttr(id=599)
    public String getGalTokenizeAutoCompleteKeyAsString() {
        return getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey);
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
     * @return zimbraGalTokenizeSearchKey, or null unset
     */
    @ZAttr(id=600)
    public String getGalTokenizeSearchKeyAsString() {
        return getAttr(Provisioning.A_zimbraGalTokenizeSearchKey);
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
     * @return zimbraHelpAdminURL, or null unset
     */
    @ZAttr(id=674)
    public String getHelpAdminURL() {
        return getAttr(Provisioning.A_zimbraHelpAdminURL);
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
     * @return zimbraHelpAdvancedURL, or null unset
     */
    @ZAttr(id=676)
    public String getHelpAdvancedURL() {
        return getAttr(Provisioning.A_zimbraHelpAdvancedURL);
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
     * @return zimbraHelpDelegatedURL, or null unset
     */
    @ZAttr(id=675)
    public String getHelpDelegatedURL() {
        return getAttr(Provisioning.A_zimbraHelpDelegatedURL);
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
     * @return zimbraHelpStandardURL, or null unset
     */
    @ZAttr(id=677)
    public String getHelpStandardURL() {
        return getAttr(Provisioning.A_zimbraHelpStandardURL);
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

    /**
     * Account for storing templates and providing space for public wiki
     *
     * @return zimbraNotebookAccount, or null unset
     */
    @ZAttr(id=363)
    public String getNotebookAccount() {
        return getAttr(Provisioning.A_zimbraNotebookAccount);
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
     * @return zimbraNotes, or null unset
     */
    @ZAttr(id=9)
    public String getNotes() {
        return getAttr(Provisioning.A_zimbraNotes);
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
     * @return zimbraPasswordChangeListener, or null unset
     */
    @ZAttr(id=586)
    public String getPasswordChangeListener() {
        return getAttr(Provisioning.A_zimbraPasswordChangeListener);
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
     * @return zimbraPreAuthKey, or null unset
     */
    @ZAttr(id=307)
    public String getPreAuthKey() {
        return getAttr(Provisioning.A_zimbraPreAuthKey);
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
     * @return zimbraPrefSkin, or null unset
     */
    @ZAttr(id=355)
    public String getPrefSkin() {
        return getAttr(Provisioning.A_zimbraPrefSkin);
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
     * @return zimbraPublicServiceHostname, or null unset
     */
    @ZAttr(id=377)
    public String getPublicServiceHostname() {
        return getAttr(Provisioning.A_zimbraPublicServiceHostname);
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
     * @return zimbraPublicServicePort, or null unset
     */
    @ZAttr(id=699)
    public String getPublicServicePort() {
        return getAttr(Provisioning.A_zimbraPublicServicePort);
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
     * @return zimbraPublicServiceProtocol, or null unset
     */
    @ZAttr(id=698)
    public String getPublicServiceProtocol() {
        return getAttr(Provisioning.A_zimbraPublicServiceProtocol);
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
     * @return zimbraSkinBackgroundColor, or null unset
     */
    @ZAttr(id=648)
    public String getSkinBackgroundColor() {
        return getAttr(Provisioning.A_zimbraSkinBackgroundColor);
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
     * @return zimbraSkinForegroundColor, or null unset
     */
    @ZAttr(id=647)
    public String getSkinForegroundColor() {
        return getAttr(Provisioning.A_zimbraSkinForegroundColor);
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
     * @return zimbraSkinLogoAppBanner, or null unset
     */
    @ZAttr(id=671)
    public String getSkinLogoAppBanner() {
        return getAttr(Provisioning.A_zimbraSkinLogoAppBanner);
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
     * @return zimbraSkinLogoLoginBanner, or null unset
     */
    @ZAttr(id=670)
    public String getSkinLogoLoginBanner() {
        return getAttr(Provisioning.A_zimbraSkinLogoLoginBanner);
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
     * @return zimbraSkinLogoURL, or null unset
     */
    @ZAttr(id=649)
    public String getSkinLogoURL() {
        return getAttr(Provisioning.A_zimbraSkinLogoURL);
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
     * @return zimbraSkinSecondaryColor, or null unset
     */
    @ZAttr(id=668)
    public String getSkinSecondaryColor() {
        return getAttr(Provisioning.A_zimbraSkinSecondaryColor);
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
     * @return zimbraSkinSelectionColor, or null unset
     */
    @ZAttr(id=669)
    public String getSkinSelectionColor() {
        return getAttr(Provisioning.A_zimbraSkinSelectionColor);
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
        return getMultiAttr(Provisioning.A_zimbraSmtpHostname);
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
     * @return zimbraSmtpPort, or null unset
     */
    @ZAttr(id=98)
    public String getSmtpPort() {
        return getAttr(Provisioning.A_zimbraSmtpPort);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=249)
    public Map<String,Object> setSmtpSendPartial(boolean zimbraSmtpSendPartial, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendPartial, Boolean.toString(zimbraSmtpSendPartial));
        return attrs;
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
     * @return zimbraWebClientAdminReference, or null unset
     */
    @ZAttr(id=701)
    public String getWebClientAdminReference() {
        return getAttr(Provisioning.A_zimbraWebClientAdminReference);
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
     * @return zimbraWebClientLoginURL, or null unset
     */
    @ZAttr(id=506)
    public String getWebClientLoginURL() {
        return getAttr(Provisioning.A_zimbraWebClientLoginURL);
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
     * @return zimbraWebClientLogoutURL, or null unset
     */
    @ZAttr(id=507)
    public String getWebClientLogoutURL() {
        return getAttr(Provisioning.A_zimbraWebClientLogoutURL);
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

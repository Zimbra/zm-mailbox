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
public class ZAttrConfig extends Entry {

    public ZAttrConfig(Map<String, Object> attrs, Provisioning provisioning) {
        super(attrs, null, provisioning);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20081125-1427 */

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
     * additional account attrs that get returned to a client
     *
     * @return zimbraAccountClientAttr, or ampty array if unset
     */
    @ZAttr(id=112)
    public String[] getAccountClientAttr() {
        return getMultiAttr(Provisioning.A_zimbraAccountClientAttr);
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param zimbraAccountClientAttr new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=112)
    public void setAccountClientAttr(String[] zimbraAccountClientAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountClientAttr, zimbraAccountClientAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param zimbraAccountClientAttr new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=112)
    public Map<String,Object> setAccountClientAttr(String[] zimbraAccountClientAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountClientAttr, zimbraAccountClientAttr);
        return attrs;
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param zimbraAccountClientAttr new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=112)
    public void addAccountClientAttr(String zimbraAccountClientAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAccountClientAttr, zimbraAccountClientAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param zimbraAccountClientAttr new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=112)
    public Map<String,Object> addAccountClientAttr(String zimbraAccountClientAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAccountClientAttr, zimbraAccountClientAttr);
        return attrs;
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param zimbraAccountClientAttr existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=112)
    public void removeAccountClientAttr(String zimbraAccountClientAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAccountClientAttr, zimbraAccountClientAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param zimbraAccountClientAttr existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=112)
    public Map<String,Object> removeAccountClientAttr(String zimbraAccountClientAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAccountClientAttr, zimbraAccountClientAttr);
        return attrs;
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=112)
    public void unsetAccountClientAttr() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountClientAttr, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * additional account attrs that get returned to a client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=112)
    public Map<String,Object> unsetAccountClientAttr(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountClientAttr, "");
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @return zimbraAccountExtraObjectClass, or ampty array if unset
     */
    @ZAttr(id=438)
    public String[] getAccountExtraObjectClass() {
        String[] value = getMultiAttr(Provisioning.A_zimbraAccountExtraObjectClass); return value.length > 0 ? value : new String[] {"amavisAccount"};
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param zimbraAccountExtraObjectClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=438)
    public void setAccountExtraObjectClass(String[] zimbraAccountExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountExtraObjectClass, zimbraAccountExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param zimbraAccountExtraObjectClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=438)
    public Map<String,Object> setAccountExtraObjectClass(String[] zimbraAccountExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountExtraObjectClass, zimbraAccountExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param zimbraAccountExtraObjectClass new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=438)
    public void addAccountExtraObjectClass(String zimbraAccountExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAccountExtraObjectClass, zimbraAccountExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param zimbraAccountExtraObjectClass new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=438)
    public Map<String,Object> addAccountExtraObjectClass(String zimbraAccountExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAccountExtraObjectClass, zimbraAccountExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param zimbraAccountExtraObjectClass existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=438)
    public void removeAccountExtraObjectClass(String zimbraAccountExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAccountExtraObjectClass, zimbraAccountExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param zimbraAccountExtraObjectClass existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=438)
    public Map<String,Object> removeAccountExtraObjectClass(String zimbraAccountExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAccountExtraObjectClass, zimbraAccountExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=438)
    public void unsetAccountExtraObjectClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountExtraObjectClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=438)
    public Map<String,Object> unsetAccountExtraObjectClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountExtraObjectClass, "");
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
     * SSL port for admin UI
     *
     * @return zimbraAdminPort, or "7071" if unset
     */
    @ZAttr(id=155)
    public String getAdminPort() {
        return getAttr(Provisioning.A_zimbraAdminPort, "7071");
    }

    /**
     * SSL port for admin UI
     *
     * @param zimbraAdminPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=155)
    public void setAdminPort(String zimbraAdminPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminPort, zimbraAdminPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL port for admin UI
     *
     * @param zimbraAdminPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=155)
    public Map<String,Object> setAdminPort(String zimbraAdminPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminPort, zimbraAdminPort);
        return attrs;
    }

    /**
     * SSL port for admin UI
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=155)
    public void unsetAdminPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL port for admin UI
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=155)
    public Map<String,Object> unsetAdminPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminPort, "");
        return attrs;
    }

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     *
     * @return zimbraAdminURL, or "/zimbraAdmin" if unset
     */
    @ZAttr(id=497)
    public String getAdminURL() {
        return getAttr(Provisioning.A_zimbraAdminURL, "/zimbraAdmin");
    }

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     *
     * @param zimbraAdminURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=497)
    public void setAdminURL(String zimbraAdminURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminURL, zimbraAdminURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     *
     * @param zimbraAdminURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=497)
    public Map<String,Object> setAdminURL(String zimbraAdminURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminURL, zimbraAdminURL);
        return attrs;
    }

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=497)
    public void unsetAdminURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=497)
    public Map<String,Object> unsetAdminURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminURL, "");
        return attrs;
    }

    /**
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     *
     * @return zimbraArchiveMailFrom, or null if unset
     */
    @ZAttr(id=430)
    public String getArchiveMailFrom() {
        return getAttr(Provisioning.A_zimbraArchiveMailFrom, null);
    }

    /**
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     *
     * @param zimbraArchiveMailFrom new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=430)
    public void setArchiveMailFrom(String zimbraArchiveMailFrom) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveMailFrom, zimbraArchiveMailFrom);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     *
     * @param zimbraArchiveMailFrom new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=430)
    public Map<String,Object> setArchiveMailFrom(String zimbraArchiveMailFrom, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveMailFrom, zimbraArchiveMailFrom);
        return attrs;
    }

    /**
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=430)
    public void unsetArchiveMailFrom() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveMailFrom, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=430)
    public Map<String,Object> unsetArchiveMailFrom(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveMailFrom, "");
        return attrs;
    }

    /**
     * block all attachment downloading
     *
     * @return zimbraAttachmentsBlocked, or false if unset
     */
    @ZAttr(id=115)
    public boolean isAttachmentsBlocked() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsBlocked, false);
    }

    /**
     * block all attachment downloading
     *
     * @param zimbraAttachmentsBlocked new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=115)
    public void setAttachmentsBlocked(boolean zimbraAttachmentsBlocked) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, zimbraAttachmentsBlocked ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * block all attachment downloading
     *
     * @param zimbraAttachmentsBlocked new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=115)
    public Map<String,Object> setAttachmentsBlocked(boolean zimbraAttachmentsBlocked, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, zimbraAttachmentsBlocked ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * block all attachment downloading
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=115)
    public void unsetAttachmentsBlocked() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * block all attachment downloading
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=115)
    public Map<String,Object> unsetAttachmentsBlocked(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, "");
        return attrs;
    }

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @return zimbraAttachmentsIndexedTextLimit, or 1048576 if unset
     */
    @ZAttr(id=582)
    public int getAttachmentsIndexedTextLimit() {
        return getIntAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, 1048576);
    }

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @param zimbraAttachmentsIndexedTextLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=582)
    public void setAttachmentsIndexedTextLimit(int zimbraAttachmentsIndexedTextLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsIndexedTextLimit, Integer.toString(zimbraAttachmentsIndexedTextLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @param zimbraAttachmentsIndexedTextLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=582)
    public Map<String,Object> setAttachmentsIndexedTextLimit(int zimbraAttachmentsIndexedTextLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsIndexedTextLimit, Integer.toString(zimbraAttachmentsIndexedTextLimit));
        return attrs;
    }

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=582)
    public void unsetAttachmentsIndexedTextLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsIndexedTextLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=582)
    public Map<String,Object> unsetAttachmentsIndexedTextLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsIndexedTextLimit, "");
        return attrs;
    }

    /**
     * Class to use to scan attachments during compose
     *
     * @return zimbraAttachmentsScanClass, or "com.zimbra.cs.scan.ClamScanner" if unset
     */
    @ZAttr(id=238)
    public String getAttachmentsScanClass() {
        return getAttr(Provisioning.A_zimbraAttachmentsScanClass, "com.zimbra.cs.scan.ClamScanner");
    }

    /**
     * Class to use to scan attachments during compose
     *
     * @param zimbraAttachmentsScanClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=238)
    public void setAttachmentsScanClass(String zimbraAttachmentsScanClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanClass, zimbraAttachmentsScanClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Class to use to scan attachments during compose
     *
     * @param zimbraAttachmentsScanClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=238)
    public Map<String,Object> setAttachmentsScanClass(String zimbraAttachmentsScanClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanClass, zimbraAttachmentsScanClass);
        return attrs;
    }

    /**
     * Class to use to scan attachments during compose
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=238)
    public void unsetAttachmentsScanClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Class to use to scan attachments during compose
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=238)
    public Map<String,Object> unsetAttachmentsScanClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanClass, "");
        return attrs;
    }

    /**
     * Whether to scan attachments during compose
     *
     * @return zimbraAttachmentsScanEnabled, or false if unset
     */
    @ZAttr(id=237)
    public boolean isAttachmentsScanEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsScanEnabled, false);
    }

    /**
     * Whether to scan attachments during compose
     *
     * @param zimbraAttachmentsScanEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=237)
    public void setAttachmentsScanEnabled(boolean zimbraAttachmentsScanEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanEnabled, zimbraAttachmentsScanEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to scan attachments during compose
     *
     * @param zimbraAttachmentsScanEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=237)
    public Map<String,Object> setAttachmentsScanEnabled(boolean zimbraAttachmentsScanEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanEnabled, zimbraAttachmentsScanEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to scan attachments during compose
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=237)
    public void unsetAttachmentsScanEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to scan attachments during compose
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=237)
    public Map<String,Object> unsetAttachmentsScanEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanEnabled, "");
        return attrs;
    }

    /**
     * Data for class that scans attachments during compose
     *
     * @return zimbraAttachmentsScanURL, or null if unset
     */
    @ZAttr(id=239)
    public String getAttachmentsScanURL() {
        return getAttr(Provisioning.A_zimbraAttachmentsScanURL, null);
    }

    /**
     * Data for class that scans attachments during compose
     *
     * @param zimbraAttachmentsScanURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=239)
    public void setAttachmentsScanURL(String zimbraAttachmentsScanURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanURL, zimbraAttachmentsScanURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Data for class that scans attachments during compose
     *
     * @param zimbraAttachmentsScanURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=239)
    public Map<String,Object> setAttachmentsScanURL(String zimbraAttachmentsScanURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanURL, zimbraAttachmentsScanURL);
        return attrs;
    }

    /**
     * Data for class that scans attachments during compose
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=239)
    public void unsetAttachmentsScanURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Data for class that scans attachments during compose
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=239)
    public Map<String,Object> unsetAttachmentsScanURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanURL, "");
        return attrs;
    }

    /**
     * view all attachments in html only
     *
     * @return zimbraAttachmentsViewInHtmlOnly, or false if unset
     */
    @ZAttr(id=116)
    public boolean isAttachmentsViewInHtmlOnly() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, false);
    }

    /**
     * view all attachments in html only
     *
     * @param zimbraAttachmentsViewInHtmlOnly new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=116)
    public void setAttachmentsViewInHtmlOnly(boolean zimbraAttachmentsViewInHtmlOnly) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, zimbraAttachmentsViewInHtmlOnly ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * view all attachments in html only
     *
     * @param zimbraAttachmentsViewInHtmlOnly new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=116)
    public Map<String,Object> setAttachmentsViewInHtmlOnly(boolean zimbraAttachmentsViewInHtmlOnly, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, zimbraAttachmentsViewInHtmlOnly ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * view all attachments in html only
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=116)
    public void unsetAttachmentsViewInHtmlOnly() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * view all attachments in html only
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=116)
    public Map<String,Object> unsetAttachmentsViewInHtmlOnly(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, "");
        return attrs;
    }

    /**
     * auth token secret key
     *
     * @return zimbraAuthTokenKey, or ampty array if unset
     */
    @ZAttr(id=100)
    public String[] getAuthTokenKey() {
        return getMultiAttr(Provisioning.A_zimbraAuthTokenKey);
    }

    /**
     * auth token secret key
     *
     * @param zimbraAuthTokenKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=100)
    public void setAuthTokenKey(String[] zimbraAuthTokenKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenKey, zimbraAuthTokenKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth token secret key
     *
     * @param zimbraAuthTokenKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=100)
    public Map<String,Object> setAuthTokenKey(String[] zimbraAuthTokenKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenKey, zimbraAuthTokenKey);
        return attrs;
    }

    /**
     * auth token secret key
     *
     * @param zimbraAuthTokenKey new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=100)
    public void addAuthTokenKey(String zimbraAuthTokenKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAuthTokenKey, zimbraAuthTokenKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth token secret key
     *
     * @param zimbraAuthTokenKey new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=100)
    public Map<String,Object> addAuthTokenKey(String zimbraAuthTokenKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAuthTokenKey, zimbraAuthTokenKey);
        return attrs;
    }

    /**
     * auth token secret key
     *
     * @param zimbraAuthTokenKey existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=100)
    public void removeAuthTokenKey(String zimbraAuthTokenKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAuthTokenKey, zimbraAuthTokenKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth token secret key
     *
     * @param zimbraAuthTokenKey existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=100)
    public Map<String,Object> removeAuthTokenKey(String zimbraAuthTokenKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAuthTokenKey, zimbraAuthTokenKey);
        return attrs;
    }

    /**
     * auth token secret key
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=100)
    public void unsetAuthTokenKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * auth token secret key
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=100)
    public Map<String,Object> unsetAuthTokenKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenKey, "");
        return attrs;
    }

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     *
     * @return zimbraAutoSubmittedNullReturnPath, or true if unset
     */
    @ZAttr(id=502)
    public boolean isAutoSubmittedNullReturnPath() {
        return getBooleanAttr(Provisioning.A_zimbraAutoSubmittedNullReturnPath, true);
    }

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     *
     * @param zimbraAutoSubmittedNullReturnPath new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=502)
    public void setAutoSubmittedNullReturnPath(boolean zimbraAutoSubmittedNullReturnPath) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoSubmittedNullReturnPath, zimbraAutoSubmittedNullReturnPath ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     *
     * @param zimbraAutoSubmittedNullReturnPath new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=502)
    public Map<String,Object> setAutoSubmittedNullReturnPath(boolean zimbraAutoSubmittedNullReturnPath, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoSubmittedNullReturnPath, zimbraAutoSubmittedNullReturnPath ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=502)
    public void unsetAutoSubmittedNullReturnPath() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoSubmittedNullReturnPath, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=502)
    public Map<String,Object> unsetAutoSubmittedNullReturnPath(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoSubmittedNullReturnPath, "");
        return attrs;
    }

    /**
     * length of each interval in auto-grouped backup
     *
     * @return zimbraBackupAutoGroupedInterval, or "1d" if unset
     */
    @ZAttr(id=513)
    public String getBackupAutoGroupedInterval() {
        return getAttr(Provisioning.A_zimbraBackupAutoGroupedInterval, "1d");
    }

    /**
     * length of each interval in auto-grouped backup
     *
     * @param zimbraBackupAutoGroupedInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=513)
    public void setBackupAutoGroupedInterval(String zimbraBackupAutoGroupedInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedInterval, zimbraBackupAutoGroupedInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * length of each interval in auto-grouped backup
     *
     * @param zimbraBackupAutoGroupedInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=513)
    public Map<String,Object> setBackupAutoGroupedInterval(String zimbraBackupAutoGroupedInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedInterval, zimbraBackupAutoGroupedInterval);
        return attrs;
    }

    /**
     * length of each interval in auto-grouped backup
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=513)
    public void unsetBackupAutoGroupedInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * length of each interval in auto-grouped backup
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=513)
    public Map<String,Object> unsetBackupAutoGroupedInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedInterval, "");
        return attrs;
    }

    /**
     * number of groups to auto-group backups over
     *
     * @return zimbraBackupAutoGroupedNumGroups, or 7 if unset
     */
    @ZAttr(id=514)
    public int getBackupAutoGroupedNumGroups() {
        return getIntAttr(Provisioning.A_zimbraBackupAutoGroupedNumGroups, 7);
    }

    /**
     * number of groups to auto-group backups over
     *
     * @param zimbraBackupAutoGroupedNumGroups new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=514)
    public void setBackupAutoGroupedNumGroups(int zimbraBackupAutoGroupedNumGroups) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedNumGroups, Integer.toString(zimbraBackupAutoGroupedNumGroups));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of groups to auto-group backups over
     *
     * @param zimbraBackupAutoGroupedNumGroups new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=514)
    public Map<String,Object> setBackupAutoGroupedNumGroups(int zimbraBackupAutoGroupedNumGroups, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedNumGroups, Integer.toString(zimbraBackupAutoGroupedNumGroups));
        return attrs;
    }

    /**
     * number of groups to auto-group backups over
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=514)
    public void unsetBackupAutoGroupedNumGroups() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedNumGroups, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of groups to auto-group backups over
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=514)
    public Map<String,Object> unsetBackupAutoGroupedNumGroups(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedNumGroups, "");
        return attrs;
    }

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     *
     * @return zimbraBackupAutoGroupedThrottled, or false if unset
     */
    @ZAttr(id=515)
    public boolean isBackupAutoGroupedThrottled() {
        return getBooleanAttr(Provisioning.A_zimbraBackupAutoGroupedThrottled, false);
    }

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     *
     * @param zimbraBackupAutoGroupedThrottled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=515)
    public void setBackupAutoGroupedThrottled(boolean zimbraBackupAutoGroupedThrottled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedThrottled, zimbraBackupAutoGroupedThrottled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     *
     * @param zimbraBackupAutoGroupedThrottled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=515)
    public Map<String,Object> setBackupAutoGroupedThrottled(boolean zimbraBackupAutoGroupedThrottled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedThrottled, zimbraBackupAutoGroupedThrottled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=515)
    public void unsetBackupAutoGroupedThrottled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedThrottled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=515)
    public Map<String,Object> unsetBackupAutoGroupedThrottled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedThrottled, "");
        return attrs;
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @return zimbraBackupMode, or ZAttrProvisioning.BackupMode.Standard if unset and/or has invalid value
     */
    @ZAttr(id=512)
    public ZAttrProvisioning.BackupMode getBackupMode() {
        try { String v = getAttr(Provisioning.A_zimbraBackupMode); return v == null ? ZAttrProvisioning.BackupMode.Standard : ZAttrProvisioning.BackupMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.BackupMode.Standard; }
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @return zimbraBackupMode, or "Standard" if unset
     */
    @ZAttr(id=512)
    public String getBackupModeAsString() {
        return getAttr(Provisioning.A_zimbraBackupMode, "Standard");
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @param zimbraBackupMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=512)
    public void setBackupMode(ZAttrProvisioning.BackupMode zimbraBackupMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupMode, zimbraBackupMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @param zimbraBackupMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=512)
    public Map<String,Object> setBackupMode(ZAttrProvisioning.BackupMode zimbraBackupMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupMode, zimbraBackupMode.toString());
        return attrs;
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @param zimbraBackupMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=512)
    public void setBackupModeAsString(String zimbraBackupMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupMode, zimbraBackupMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @param zimbraBackupMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=512)
    public Map<String,Object> setBackupModeAsString(String zimbraBackupMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupMode, zimbraBackupMode);
        return attrs;
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=512)
    public void unsetBackupMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=512)
    public Map<String,Object> unsetBackupMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupMode, "");
        return attrs;
    }

    /**
     * Backup report email recipients
     *
     * @return zimbraBackupReportEmailRecipients, or ampty array if unset
     */
    @ZAttr(id=459)
    public String[] getBackupReportEmailRecipients() {
        return getMultiAttr(Provisioning.A_zimbraBackupReportEmailRecipients);
    }

    /**
     * Backup report email recipients
     *
     * @param zimbraBackupReportEmailRecipients new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=459)
    public void setBackupReportEmailRecipients(String[] zimbraBackupReportEmailRecipients) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailRecipients, zimbraBackupReportEmailRecipients);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email recipients
     *
     * @param zimbraBackupReportEmailRecipients new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=459)
    public Map<String,Object> setBackupReportEmailRecipients(String[] zimbraBackupReportEmailRecipients, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailRecipients, zimbraBackupReportEmailRecipients);
        return attrs;
    }

    /**
     * Backup report email recipients
     *
     * @param zimbraBackupReportEmailRecipients new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=459)
    public void addBackupReportEmailRecipients(String zimbraBackupReportEmailRecipients) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraBackupReportEmailRecipients, zimbraBackupReportEmailRecipients);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email recipients
     *
     * @param zimbraBackupReportEmailRecipients new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=459)
    public Map<String,Object> addBackupReportEmailRecipients(String zimbraBackupReportEmailRecipients, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraBackupReportEmailRecipients, zimbraBackupReportEmailRecipients);
        return attrs;
    }

    /**
     * Backup report email recipients
     *
     * @param zimbraBackupReportEmailRecipients existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=459)
    public void removeBackupReportEmailRecipients(String zimbraBackupReportEmailRecipients) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraBackupReportEmailRecipients, zimbraBackupReportEmailRecipients);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email recipients
     *
     * @param zimbraBackupReportEmailRecipients existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=459)
    public Map<String,Object> removeBackupReportEmailRecipients(String zimbraBackupReportEmailRecipients, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraBackupReportEmailRecipients, zimbraBackupReportEmailRecipients);
        return attrs;
    }

    /**
     * Backup report email recipients
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=459)
    public void unsetBackupReportEmailRecipients() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailRecipients, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email recipients
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=459)
    public Map<String,Object> unsetBackupReportEmailRecipients(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailRecipients, "");
        return attrs;
    }

    /**
     * Backup report email From address
     *
     * @return zimbraBackupReportEmailSender, or null if unset
     */
    @ZAttr(id=460)
    public String getBackupReportEmailSender() {
        return getAttr(Provisioning.A_zimbraBackupReportEmailSender, null);
    }

    /**
     * Backup report email From address
     *
     * @param zimbraBackupReportEmailSender new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=460)
    public void setBackupReportEmailSender(String zimbraBackupReportEmailSender) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSender, zimbraBackupReportEmailSender);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email From address
     *
     * @param zimbraBackupReportEmailSender new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=460)
    public Map<String,Object> setBackupReportEmailSender(String zimbraBackupReportEmailSender, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSender, zimbraBackupReportEmailSender);
        return attrs;
    }

    /**
     * Backup report email From address
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=460)
    public void unsetBackupReportEmailSender() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSender, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email From address
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=460)
    public Map<String,Object> unsetBackupReportEmailSender(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSender, "");
        return attrs;
    }

    /**
     * Backup report email subject prefix
     *
     * @return zimbraBackupReportEmailSubjectPrefix, or "ZCS Backup Report" if unset
     */
    @ZAttr(id=461)
    public String getBackupReportEmailSubjectPrefix() {
        return getAttr(Provisioning.A_zimbraBackupReportEmailSubjectPrefix, "ZCS Backup Report");
    }

    /**
     * Backup report email subject prefix
     *
     * @param zimbraBackupReportEmailSubjectPrefix new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=461)
    public void setBackupReportEmailSubjectPrefix(String zimbraBackupReportEmailSubjectPrefix) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSubjectPrefix, zimbraBackupReportEmailSubjectPrefix);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email subject prefix
     *
     * @param zimbraBackupReportEmailSubjectPrefix new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=461)
    public Map<String,Object> setBackupReportEmailSubjectPrefix(String zimbraBackupReportEmailSubjectPrefix, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSubjectPrefix, zimbraBackupReportEmailSubjectPrefix);
        return attrs;
    }

    /**
     * Backup report email subject prefix
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=461)
    public void unsetBackupReportEmailSubjectPrefix() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSubjectPrefix, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Backup report email subject prefix
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=461)
    public Map<String,Object> unsetBackupReportEmailSubjectPrefix(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupReportEmailSubjectPrefix, "");
        return attrs;
    }

    /**
     * Default backup target path
     *
     * @return zimbraBackupTarget, or "/opt/zimbra/backup" if unset
     */
    @ZAttr(id=458)
    public String getBackupTarget() {
        return getAttr(Provisioning.A_zimbraBackupTarget, "/opt/zimbra/backup");
    }

    /**
     * Default backup target path
     *
     * @param zimbraBackupTarget new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=458)
    public void setBackupTarget(String zimbraBackupTarget) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupTarget, zimbraBackupTarget);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default backup target path
     *
     * @param zimbraBackupTarget new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=458)
    public Map<String,Object> setBackupTarget(String zimbraBackupTarget, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupTarget, zimbraBackupTarget);
        return attrs;
    }

    /**
     * Default backup target path
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=458)
    public void unsetBackupTarget() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupTarget, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default backup target path
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=458)
    public Map<String,Object> unsetBackupTarget(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupTarget, "");
        return attrs;
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @return zimbraCOSInheritedAttr, or ampty array if unset
     */
    @ZAttr(id=21)
    public String[] getCOSInheritedAttr() {
        return getMultiAttr(Provisioning.A_zimbraCOSInheritedAttr);
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param zimbraCOSInheritedAttr new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=21)
    public void setCOSInheritedAttr(String[] zimbraCOSInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCOSInheritedAttr, zimbraCOSInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param zimbraCOSInheritedAttr new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=21)
    public Map<String,Object> setCOSInheritedAttr(String[] zimbraCOSInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCOSInheritedAttr, zimbraCOSInheritedAttr);
        return attrs;
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param zimbraCOSInheritedAttr new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=21)
    public void addCOSInheritedAttr(String zimbraCOSInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCOSInheritedAttr, zimbraCOSInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param zimbraCOSInheritedAttr new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=21)
    public Map<String,Object> addCOSInheritedAttr(String zimbraCOSInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCOSInheritedAttr, zimbraCOSInheritedAttr);
        return attrs;
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param zimbraCOSInheritedAttr existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=21)
    public void removeCOSInheritedAttr(String zimbraCOSInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCOSInheritedAttr, zimbraCOSInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param zimbraCOSInheritedAttr existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=21)
    public Map<String,Object> removeCOSInheritedAttr(String zimbraCOSInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCOSInheritedAttr, zimbraCOSInheritedAttr);
        return attrs;
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=21)
    public void unsetCOSInheritedAttr() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCOSInheritedAttr, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=21)
    public Map<String,Object> unsetCOSInheritedAttr(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCOSInheritedAttr, "");
        return attrs;
    }

    /**
     * alternate location for calendar and task folders
     *
     * @return zimbraCalendarCalDavAlternateCalendarHomeSet, or ampty array if unset
     */
    @ZAttr(id=651)
    public String[] getCalendarCalDavAlternateCalendarHomeSet() {
        return getMultiAttr(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet);
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param zimbraCalendarCalDavAlternateCalendarHomeSet new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=651)
    public void setCalendarCalDavAlternateCalendarHomeSet(String[] zimbraCalendarCalDavAlternateCalendarHomeSet) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, zimbraCalendarCalDavAlternateCalendarHomeSet);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param zimbraCalendarCalDavAlternateCalendarHomeSet new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=651)
    public Map<String,Object> setCalendarCalDavAlternateCalendarHomeSet(String[] zimbraCalendarCalDavAlternateCalendarHomeSet, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, zimbraCalendarCalDavAlternateCalendarHomeSet);
        return attrs;
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param zimbraCalendarCalDavAlternateCalendarHomeSet new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=651)
    public void addCalendarCalDavAlternateCalendarHomeSet(String zimbraCalendarCalDavAlternateCalendarHomeSet) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, zimbraCalendarCalDavAlternateCalendarHomeSet);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param zimbraCalendarCalDavAlternateCalendarHomeSet new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=651)
    public Map<String,Object> addCalendarCalDavAlternateCalendarHomeSet(String zimbraCalendarCalDavAlternateCalendarHomeSet, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, zimbraCalendarCalDavAlternateCalendarHomeSet);
        return attrs;
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param zimbraCalendarCalDavAlternateCalendarHomeSet existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=651)
    public void removeCalendarCalDavAlternateCalendarHomeSet(String zimbraCalendarCalDavAlternateCalendarHomeSet) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, zimbraCalendarCalDavAlternateCalendarHomeSet);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param zimbraCalendarCalDavAlternateCalendarHomeSet existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=651)
    public Map<String,Object> removeCalendarCalDavAlternateCalendarHomeSet(String zimbraCalendarCalDavAlternateCalendarHomeSet, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, zimbraCalendarCalDavAlternateCalendarHomeSet);
        return attrs;
    }

    /**
     * alternate location for calendar and task folders
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=651)
    public void unsetCalendarCalDavAlternateCalendarHomeSet() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * alternate location for calendar and task folders
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=651)
    public Map<String,Object> unsetCalendarCalDavAlternateCalendarHomeSet(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet, "");
        return attrs;
    }

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @return zimbraCalendarCalDavDisableFreebusy, or false if unset
     */
    @ZAttr(id=690)
    public boolean isCalendarCalDavDisableFreebusy() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, false);
    }

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @param zimbraCalendarCalDavDisableFreebusy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=690)
    public void setCalendarCalDavDisableFreebusy(boolean zimbraCalendarCalDavDisableFreebusy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, zimbraCalendarCalDavDisableFreebusy ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @param zimbraCalendarCalDavDisableFreebusy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=690)
    public Map<String,Object> setCalendarCalDavDisableFreebusy(boolean zimbraCalendarCalDavDisableFreebusy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, zimbraCalendarCalDavDisableFreebusy ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=690)
    public void unsetCalendarCalDavDisableFreebusy() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=690)
    public Map<String,Object> unsetCalendarCalDavDisableFreebusy(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, "");
        return attrs;
    }

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @return zimbraCalendarCalDavDisableScheduling, or false if unset
     */
    @ZAttr(id=652)
    public boolean isCalendarCalDavDisableScheduling() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarCalDavDisableScheduling, false);
    }

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @param zimbraCalendarCalDavDisableScheduling new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=652)
    public void setCalendarCalDavDisableScheduling(boolean zimbraCalendarCalDavDisableScheduling) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableScheduling, zimbraCalendarCalDavDisableScheduling ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @param zimbraCalendarCalDavDisableScheduling new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=652)
    public Map<String,Object> setCalendarCalDavDisableScheduling(boolean zimbraCalendarCalDavDisableScheduling, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableScheduling, zimbraCalendarCalDavDisableScheduling ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=652)
    public void unsetCalendarCalDavDisableScheduling() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableScheduling, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=652)
    public Map<String,Object> unsetCalendarCalDavDisableScheduling(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableScheduling, "");
        return attrs;
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @return zimbraCalendarCompatibilityMode, or ZAttrProvisioning.CalendarCompatibilityMode.standard if unset and/or has invalid value
     */
    @ZAttr(id=243)
    public ZAttrProvisioning.CalendarCompatibilityMode getCalendarCompatibilityMode() {
        try { String v = getAttr(Provisioning.A_zimbraCalendarCompatibilityMode); return v == null ? ZAttrProvisioning.CalendarCompatibilityMode.standard : ZAttrProvisioning.CalendarCompatibilityMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.CalendarCompatibilityMode.standard; }
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @return zimbraCalendarCompatibilityMode, or "standard" if unset
     */
    @ZAttr(id=243)
    public String getCalendarCompatibilityModeAsString() {
        return getAttr(Provisioning.A_zimbraCalendarCompatibilityMode, "standard");
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @param zimbraCalendarCompatibilityMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=243)
    public void setCalendarCompatibilityMode(ZAttrProvisioning.CalendarCompatibilityMode zimbraCalendarCompatibilityMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCompatibilityMode, zimbraCalendarCompatibilityMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @param zimbraCalendarCompatibilityMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=243)
    public Map<String,Object> setCalendarCompatibilityMode(ZAttrProvisioning.CalendarCompatibilityMode zimbraCalendarCompatibilityMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCompatibilityMode, zimbraCalendarCompatibilityMode.toString());
        return attrs;
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @param zimbraCalendarCompatibilityMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=243)
    public void setCalendarCompatibilityModeAsString(String zimbraCalendarCompatibilityMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCompatibilityMode, zimbraCalendarCompatibilityMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @param zimbraCalendarCompatibilityMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=243)
    public Map<String,Object> setCalendarCompatibilityModeAsString(String zimbraCalendarCompatibilityMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCompatibilityMode, zimbraCalendarCompatibilityMode);
        return attrs;
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=243)
    public void unsetCalendarCompatibilityMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCompatibilityMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=243)
    public Map<String,Object> unsetCalendarCompatibilityMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCompatibilityMode, "");
        return attrs;
    }

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceDailyMaxDays, or 730 if unset
     */
    @ZAttr(id=661)
    public int getCalendarRecurrenceDailyMaxDays() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, 730);
    }

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceDailyMaxDays new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=661)
    public void setCalendarRecurrenceDailyMaxDays(int zimbraCalendarRecurrenceDailyMaxDays) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, Integer.toString(zimbraCalendarRecurrenceDailyMaxDays));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceDailyMaxDays new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=661)
    public Map<String,Object> setCalendarRecurrenceDailyMaxDays(int zimbraCalendarRecurrenceDailyMaxDays, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, Integer.toString(zimbraCalendarRecurrenceDailyMaxDays));
        return attrs;
    }

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=661)
    public void unsetCalendarRecurrenceDailyMaxDays() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=661)
    public Map<String,Object> unsetCalendarRecurrenceDailyMaxDays(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, "");
        return attrs;
    }

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceMaxInstances, or 0 if unset
     */
    @ZAttr(id=660)
    public int getCalendarRecurrenceMaxInstances() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, 0);
    }

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceMaxInstances new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=660)
    public void setCalendarRecurrenceMaxInstances(int zimbraCalendarRecurrenceMaxInstances) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, Integer.toString(zimbraCalendarRecurrenceMaxInstances));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceMaxInstances new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=660)
    public Map<String,Object> setCalendarRecurrenceMaxInstances(int zimbraCalendarRecurrenceMaxInstances, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, Integer.toString(zimbraCalendarRecurrenceMaxInstances));
        return attrs;
    }

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=660)
    public void unsetCalendarRecurrenceMaxInstances() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=660)
    public Map<String,Object> unsetCalendarRecurrenceMaxInstances(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, "");
        return attrs;
    }

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceMonthlyMaxMonths, or 360 if unset
     */
    @ZAttr(id=663)
    public int getCalendarRecurrenceMonthlyMaxMonths() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, 360);
    }

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceMonthlyMaxMonths new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=663)
    public void setCalendarRecurrenceMonthlyMaxMonths(int zimbraCalendarRecurrenceMonthlyMaxMonths) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, Integer.toString(zimbraCalendarRecurrenceMonthlyMaxMonths));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceMonthlyMaxMonths new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=663)
    public Map<String,Object> setCalendarRecurrenceMonthlyMaxMonths(int zimbraCalendarRecurrenceMonthlyMaxMonths, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, Integer.toString(zimbraCalendarRecurrenceMonthlyMaxMonths));
        return attrs;
    }

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=663)
    public void unsetCalendarRecurrenceMonthlyMaxMonths() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=663)
    public Map<String,Object> unsetCalendarRecurrenceMonthlyMaxMonths(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, "");
        return attrs;
    }

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @return zimbraCalendarRecurrenceOtherFrequencyMaxYears, or 1 if unset
     */
    @ZAttr(id=665)
    public int getCalendarRecurrenceOtherFrequencyMaxYears() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, 1);
    }

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @param zimbraCalendarRecurrenceOtherFrequencyMaxYears new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=665)
    public void setCalendarRecurrenceOtherFrequencyMaxYears(int zimbraCalendarRecurrenceOtherFrequencyMaxYears) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, Integer.toString(zimbraCalendarRecurrenceOtherFrequencyMaxYears));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @param zimbraCalendarRecurrenceOtherFrequencyMaxYears new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=665)
    public Map<String,Object> setCalendarRecurrenceOtherFrequencyMaxYears(int zimbraCalendarRecurrenceOtherFrequencyMaxYears, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, Integer.toString(zimbraCalendarRecurrenceOtherFrequencyMaxYears));
        return attrs;
    }

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=665)
    public void unsetCalendarRecurrenceOtherFrequencyMaxYears() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=665)
    public Map<String,Object> unsetCalendarRecurrenceOtherFrequencyMaxYears(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, "");
        return attrs;
    }

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceWeeklyMaxWeeks, or 520 if unset
     */
    @ZAttr(id=662)
    public int getCalendarRecurrenceWeeklyMaxWeeks() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, 520);
    }

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceWeeklyMaxWeeks new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=662)
    public void setCalendarRecurrenceWeeklyMaxWeeks(int zimbraCalendarRecurrenceWeeklyMaxWeeks) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, Integer.toString(zimbraCalendarRecurrenceWeeklyMaxWeeks));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceWeeklyMaxWeeks new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=662)
    public Map<String,Object> setCalendarRecurrenceWeeklyMaxWeeks(int zimbraCalendarRecurrenceWeeklyMaxWeeks, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, Integer.toString(zimbraCalendarRecurrenceWeeklyMaxWeeks));
        return attrs;
    }

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=662)
    public void unsetCalendarRecurrenceWeeklyMaxWeeks() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=662)
    public Map<String,Object> unsetCalendarRecurrenceWeeklyMaxWeeks(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, "");
        return attrs;
    }

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceYearlyMaxYears, or 100 if unset
     */
    @ZAttr(id=664)
    public int getCalendarRecurrenceYearlyMaxYears() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, 100);
    }

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceYearlyMaxYears new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=664)
    public void setCalendarRecurrenceYearlyMaxYears(int zimbraCalendarRecurrenceYearlyMaxYears) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, Integer.toString(zimbraCalendarRecurrenceYearlyMaxYears));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param zimbraCalendarRecurrenceYearlyMaxYears new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=664)
    public Map<String,Object> setCalendarRecurrenceYearlyMaxYears(int zimbraCalendarRecurrenceYearlyMaxYears, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, Integer.toString(zimbraCalendarRecurrenceYearlyMaxYears));
        return attrs;
    }

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=664)
    public void unsetCalendarRecurrenceYearlyMaxYears() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=664)
    public Map<String,Object> unsetCalendarRecurrenceYearlyMaxYears(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, "");
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @return zimbraCalendarResourceExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public String[] getCalendarResourceExtraObjectClass() {
        String[] value = getMultiAttr(Provisioning.A_zimbraCalendarResourceExtraObjectClass); return value.length > 0 ? value : new String[] {"amavisAccount"};
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param zimbraCalendarResourceExtraObjectClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public void setCalendarResourceExtraObjectClass(String[] zimbraCalendarResourceExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceExtraObjectClass, zimbraCalendarResourceExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param zimbraCalendarResourceExtraObjectClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public Map<String,Object> setCalendarResourceExtraObjectClass(String[] zimbraCalendarResourceExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceExtraObjectClass, zimbraCalendarResourceExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param zimbraCalendarResourceExtraObjectClass new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public void addCalendarResourceExtraObjectClass(String zimbraCalendarResourceExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCalendarResourceExtraObjectClass, zimbraCalendarResourceExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param zimbraCalendarResourceExtraObjectClass new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public Map<String,Object> addCalendarResourceExtraObjectClass(String zimbraCalendarResourceExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCalendarResourceExtraObjectClass, zimbraCalendarResourceExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param zimbraCalendarResourceExtraObjectClass existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public void removeCalendarResourceExtraObjectClass(String zimbraCalendarResourceExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCalendarResourceExtraObjectClass, zimbraCalendarResourceExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param zimbraCalendarResourceExtraObjectClass existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public Map<String,Object> removeCalendarResourceExtraObjectClass(String zimbraCalendarResourceExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCalendarResourceExtraObjectClass, zimbraCalendarResourceExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public void unsetCalendarResourceExtraObjectClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceExtraObjectClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public Map<String,Object> unsetCalendarResourceExtraObjectClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceExtraObjectClass, "");
        return attrs;
    }

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     *
     * @return zimbraCertAuthorityCertSelfSigned, or null if unset
     */
    @ZAttr(id=280)
    public String getCertAuthorityCertSelfSigned() {
        return getAttr(Provisioning.A_zimbraCertAuthorityCertSelfSigned, null);
    }

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     *
     * @param zimbraCertAuthorityCertSelfSigned new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=280)
    public void setCertAuthorityCertSelfSigned(String zimbraCertAuthorityCertSelfSigned) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityCertSelfSigned, zimbraCertAuthorityCertSelfSigned);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     *
     * @param zimbraCertAuthorityCertSelfSigned new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=280)
    public Map<String,Object> setCertAuthorityCertSelfSigned(String zimbraCertAuthorityCertSelfSigned, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityCertSelfSigned, zimbraCertAuthorityCertSelfSigned);
        return attrs;
    }

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=280)
    public void unsetCertAuthorityCertSelfSigned() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityCertSelfSigned, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=280)
    public Map<String,Object> unsetCertAuthorityCertSelfSigned(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityCertSelfSigned, "");
        return attrs;
    }

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     *
     * @return zimbraCertAuthorityKeySelfSigned, or null if unset
     */
    @ZAttr(id=279)
    public String getCertAuthorityKeySelfSigned() {
        return getAttr(Provisioning.A_zimbraCertAuthorityKeySelfSigned, null);
    }

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     *
     * @param zimbraCertAuthorityKeySelfSigned new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=279)
    public void setCertAuthorityKeySelfSigned(String zimbraCertAuthorityKeySelfSigned) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityKeySelfSigned, zimbraCertAuthorityKeySelfSigned);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     *
     * @param zimbraCertAuthorityKeySelfSigned new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=279)
    public Map<String,Object> setCertAuthorityKeySelfSigned(String zimbraCertAuthorityKeySelfSigned, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityKeySelfSigned, zimbraCertAuthorityKeySelfSigned);
        return attrs;
    }

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=279)
    public void unsetCertAuthorityKeySelfSigned() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityKeySelfSigned, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=279)
    public Map<String,Object> unsetCertAuthorityKeySelfSigned(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCertAuthorityKeySelfSigned, "");
        return attrs;
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @return zimbraClusterType, or ZAttrProvisioning.ClusterType.none if unset and/or has invalid value
     */
    @ZAttr(id=508)
    public ZAttrProvisioning.ClusterType getClusterType() {
        try { String v = getAttr(Provisioning.A_zimbraClusterType); return v == null ? ZAttrProvisioning.ClusterType.none : ZAttrProvisioning.ClusterType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.ClusterType.none; }
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @return zimbraClusterType, or "none" if unset
     */
    @ZAttr(id=508)
    public String getClusterTypeAsString() {
        return getAttr(Provisioning.A_zimbraClusterType, "none");
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @param zimbraClusterType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=508)
    public void setClusterType(ZAttrProvisioning.ClusterType zimbraClusterType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraClusterType, zimbraClusterType.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @param zimbraClusterType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=508)
    public Map<String,Object> setClusterType(ZAttrProvisioning.ClusterType zimbraClusterType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraClusterType, zimbraClusterType.toString());
        return attrs;
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @param zimbraClusterType new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=508)
    public void setClusterTypeAsString(String zimbraClusterType) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraClusterType, zimbraClusterType);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @param zimbraClusterType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=508)
    public Map<String,Object> setClusterTypeAsString(String zimbraClusterType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraClusterType, zimbraClusterType);
        return attrs;
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=508)
    public void unsetClusterType() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraClusterType, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=508)
    public Map<String,Object> unsetClusterType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraClusterType, "");
        return attrs;
    }

    /**
     * Names of additonal components that have been installed
     *
     * @return zimbraComponentAvailable, or ampty array if unset
     */
    @ZAttr(id=242)
    public String[] getComponentAvailable() {
        return getMultiAttr(Provisioning.A_zimbraComponentAvailable);
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param zimbraComponentAvailable new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=242)
    public void setComponentAvailable(String[] zimbraComponentAvailable) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraComponentAvailable, zimbraComponentAvailable);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param zimbraComponentAvailable new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=242)
    public Map<String,Object> setComponentAvailable(String[] zimbraComponentAvailable, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraComponentAvailable, zimbraComponentAvailable);
        return attrs;
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param zimbraComponentAvailable new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=242)
    public void addComponentAvailable(String zimbraComponentAvailable) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraComponentAvailable, zimbraComponentAvailable);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param zimbraComponentAvailable new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=242)
    public Map<String,Object> addComponentAvailable(String zimbraComponentAvailable, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraComponentAvailable, zimbraComponentAvailable);
        return attrs;
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param zimbraComponentAvailable existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=242)
    public void removeComponentAvailable(String zimbraComponentAvailable) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraComponentAvailable, zimbraComponentAvailable);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param zimbraComponentAvailable existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=242)
    public Map<String,Object> removeComponentAvailable(String zimbraComponentAvailable, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraComponentAvailable, zimbraComponentAvailable);
        return attrs;
    }

    /**
     * Names of additonal components that have been installed
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=242)
    public void unsetComponentAvailable() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraComponentAvailable, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Names of additonal components that have been installed
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=242)
    public Map<String,Object> unsetComponentAvailable(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraComponentAvailable, "");
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @return zimbraCosExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public String[] getCosExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraCosExtraObjectClass);
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param zimbraCosExtraObjectClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public void setCosExtraObjectClass(String[] zimbraCosExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCosExtraObjectClass, zimbraCosExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param zimbraCosExtraObjectClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public Map<String,Object> setCosExtraObjectClass(String[] zimbraCosExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCosExtraObjectClass, zimbraCosExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param zimbraCosExtraObjectClass new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public void addCosExtraObjectClass(String zimbraCosExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCosExtraObjectClass, zimbraCosExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param zimbraCosExtraObjectClass new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public Map<String,Object> addCosExtraObjectClass(String zimbraCosExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraCosExtraObjectClass, zimbraCosExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param zimbraCosExtraObjectClass existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public void removeCosExtraObjectClass(String zimbraCosExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCosExtraObjectClass, zimbraCosExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param zimbraCosExtraObjectClass existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public Map<String,Object> removeCosExtraObjectClass(String zimbraCosExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraCosExtraObjectClass, zimbraCosExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public void unsetCosExtraObjectClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCosExtraObjectClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public Map<String,Object> unsetCosExtraObjectClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCosExtraObjectClass, "");
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
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @return zimbraDefaultDomainName, or null if unset
     */
    @ZAttr(id=172)
    public String getDefaultDomainName() {
        return getAttr(Provisioning.A_zimbraDefaultDomainName, null);
    }

    /**
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @param zimbraDefaultDomainName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=172)
    public void setDefaultDomainName(String zimbraDefaultDomainName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, zimbraDefaultDomainName);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @param zimbraDefaultDomainName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=172)
    public Map<String,Object> setDefaultDomainName(String zimbraDefaultDomainName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, zimbraDefaultDomainName);
        return attrs;
    }

    /**
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=172)
    public void unsetDefaultDomainName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=172)
    public Map<String,Object> unsetDefaultDomainName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, "");
        return attrs;
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @return zimbraDomainAdminModifiableAttr, or ampty array if unset
     */
    @ZAttr(id=300)
    public String[] getDomainAdminModifiableAttr() {
        return getMultiAttr(Provisioning.A_zimbraDomainAdminModifiableAttr);
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param zimbraDomainAdminModifiableAttr new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=300)
    public void setDomainAdminModifiableAttr(String[] zimbraDomainAdminModifiableAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAdminModifiableAttr, zimbraDomainAdminModifiableAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param zimbraDomainAdminModifiableAttr new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=300)
    public Map<String,Object> setDomainAdminModifiableAttr(String[] zimbraDomainAdminModifiableAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAdminModifiableAttr, zimbraDomainAdminModifiableAttr);
        return attrs;
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param zimbraDomainAdminModifiableAttr new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=300)
    public void addDomainAdminModifiableAttr(String zimbraDomainAdminModifiableAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainAdminModifiableAttr, zimbraDomainAdminModifiableAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param zimbraDomainAdminModifiableAttr new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=300)
    public Map<String,Object> addDomainAdminModifiableAttr(String zimbraDomainAdminModifiableAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainAdminModifiableAttr, zimbraDomainAdminModifiableAttr);
        return attrs;
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param zimbraDomainAdminModifiableAttr existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=300)
    public void removeDomainAdminModifiableAttr(String zimbraDomainAdminModifiableAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainAdminModifiableAttr, zimbraDomainAdminModifiableAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param zimbraDomainAdminModifiableAttr existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=300)
    public Map<String,Object> removeDomainAdminModifiableAttr(String zimbraDomainAdminModifiableAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainAdminModifiableAttr, zimbraDomainAdminModifiableAttr);
        return attrs;
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=300)
    public void unsetDomainAdminModifiableAttr() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAdminModifiableAttr, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=300)
    public Map<String,Object> unsetDomainAdminModifiableAttr(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAdminModifiableAttr, "");
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @return zimbraDomainExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public String[] getDomainExtraObjectClass() {
        String[] value = getMultiAttr(Provisioning.A_zimbraDomainExtraObjectClass); return value.length > 0 ? value : new String[] {"amavisAccount"};
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param zimbraDomainExtraObjectClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public void setDomainExtraObjectClass(String[] zimbraDomainExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainExtraObjectClass, zimbraDomainExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param zimbraDomainExtraObjectClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public Map<String,Object> setDomainExtraObjectClass(String[] zimbraDomainExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainExtraObjectClass, zimbraDomainExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param zimbraDomainExtraObjectClass new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public void addDomainExtraObjectClass(String zimbraDomainExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainExtraObjectClass, zimbraDomainExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param zimbraDomainExtraObjectClass new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public Map<String,Object> addDomainExtraObjectClass(String zimbraDomainExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainExtraObjectClass, zimbraDomainExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param zimbraDomainExtraObjectClass existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public void removeDomainExtraObjectClass(String zimbraDomainExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainExtraObjectClass, zimbraDomainExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param zimbraDomainExtraObjectClass existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public Map<String,Object> removeDomainExtraObjectClass(String zimbraDomainExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainExtraObjectClass, zimbraDomainExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public void unsetDomainExtraObjectClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainExtraObjectClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public Map<String,Object> unsetDomainExtraObjectClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainExtraObjectClass, "");
        return attrs;
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @return zimbraDomainInheritedAttr, or ampty array if unset
     */
    @ZAttr(id=63)
    public String[] getDomainInheritedAttr() {
        return getMultiAttr(Provisioning.A_zimbraDomainInheritedAttr);
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param zimbraDomainInheritedAttr new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=63)
    public void setDomainInheritedAttr(String[] zimbraDomainInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainInheritedAttr, zimbraDomainInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param zimbraDomainInheritedAttr new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=63)
    public Map<String,Object> setDomainInheritedAttr(String[] zimbraDomainInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainInheritedAttr, zimbraDomainInheritedAttr);
        return attrs;
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param zimbraDomainInheritedAttr new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=63)
    public void addDomainInheritedAttr(String zimbraDomainInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainInheritedAttr, zimbraDomainInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param zimbraDomainInheritedAttr new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=63)
    public Map<String,Object> addDomainInheritedAttr(String zimbraDomainInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDomainInheritedAttr, zimbraDomainInheritedAttr);
        return attrs;
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param zimbraDomainInheritedAttr existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=63)
    public void removeDomainInheritedAttr(String zimbraDomainInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainInheritedAttr, zimbraDomainInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param zimbraDomainInheritedAttr existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=63)
    public Map<String,Object> removeDomainInheritedAttr(String zimbraDomainInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDomainInheritedAttr, zimbraDomainInheritedAttr);
        return attrs;
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=63)
    public void unsetDomainInheritedAttr() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainInheritedAttr, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=63)
    public Map<String,Object> unsetDomainInheritedAttr(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainInheritedAttr, "");
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
     * Maximum size in bytes for attachments
     *
     * @return zimbraFileUploadMaxSize, or 10485760 if unset
     */
    @ZAttr(id=227)
    public long getFileUploadMaxSize() {
        return getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, 10485760L);
    }

    /**
     * Maximum size in bytes for attachments
     *
     * @param zimbraFileUploadMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=227)
    public void setFileUploadMaxSize(long zimbraFileUploadMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFileUploadMaxSize, Long.toString(zimbraFileUploadMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum size in bytes for attachments
     *
     * @param zimbraFileUploadMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=227)
    public Map<String,Object> setFileUploadMaxSize(long zimbraFileUploadMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFileUploadMaxSize, Long.toString(zimbraFileUploadMaxSize));
        return attrs;
    }

    /**
     * Maximum size in bytes for attachments
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=227)
    public void unsetFileUploadMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFileUploadMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum size in bytes for attachments
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=227)
    public Map<String,Object> unsetFileUploadMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFileUploadMaxSize, "");
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
     * LDAP search filter definitions for GAL queries
     *
     * @return zimbraGalLdapFilterDef, or ampty array if unset
     */
    @ZAttr(id=52)
    public String[] getGalLdapFilterDef() {
        String[] value = getMultiAttr(Provisioning.A_zimbraGalLdapFilterDef); return value.length > 0 ? value : new String[] {"zimbraAccounts:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))","zimbraAccountAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))","zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))","zimbraResources:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraCalendarResource))","zimbraResourceAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(objectclass=zimbraCalendarResource))","zimbraResourceSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraCalendarResource))","ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))","adAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(givenName=%s*)(mail=%s*))(!(msExchHideFromAddressLists=TRUE))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))","externalLdapAutoComplete:(|(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*))"};
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param zimbraGalLdapFilterDef new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=52)
    public void setGalLdapFilterDef(String[] zimbraGalLdapFilterDef) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilterDef, zimbraGalLdapFilterDef);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param zimbraGalLdapFilterDef new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=52)
    public Map<String,Object> setGalLdapFilterDef(String[] zimbraGalLdapFilterDef, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilterDef, zimbraGalLdapFilterDef);
        return attrs;
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param zimbraGalLdapFilterDef new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=52)
    public void addGalLdapFilterDef(String zimbraGalLdapFilterDef) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapFilterDef, zimbraGalLdapFilterDef);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param zimbraGalLdapFilterDef new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=52)
    public Map<String,Object> addGalLdapFilterDef(String zimbraGalLdapFilterDef, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapFilterDef, zimbraGalLdapFilterDef);
        return attrs;
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param zimbraGalLdapFilterDef existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=52)
    public void removeGalLdapFilterDef(String zimbraGalLdapFilterDef) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapFilterDef, zimbraGalLdapFilterDef);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param zimbraGalLdapFilterDef existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=52)
    public Map<String,Object> removeGalLdapFilterDef(String zimbraGalLdapFilterDef, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapFilterDef, zimbraGalLdapFilterDef);
        return attrs;
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=52)
    public void unsetGalLdapFilterDef() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilterDef, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=52)
    public Map<String,Object> unsetGalLdapFilterDef(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalLdapFilterDef, "");
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
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * <p>Use getHsmAgeAsString to access value as a string.
     *
     * @see #getHsmAgeAsString()
     *
     * @return zimbraHsmAge in millseconds, or 2592000000 (30d)  if unset
     */
    @ZAttr(id=8)
    public long getHsmAge() {
        return getTimeInterval(Provisioning.A_zimbraHsmAge, 2592000000L);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @return zimbraHsmAge, or "30d" if unset
     */
    @ZAttr(id=8)
    public String getHsmAgeAsString() {
        return getAttr(Provisioning.A_zimbraHsmAge, "30d");
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @param zimbraHsmAge new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=8)
    public void setHsmAge(String zimbraHsmAge) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHsmAge, zimbraHsmAge);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @param zimbraHsmAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=8)
    public Map<String,Object> setHsmAge(String zimbraHsmAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHsmAge, zimbraHsmAge);
        return attrs;
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=8)
    public void unsetHsmAge() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHsmAge, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=8)
    public Map<String,Object> unsetHsmAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHsmAge, "");
        return attrs;
    }

    /**
     * number of http handler threads
     *
     * @return zimbraHttpNumThreads, or 250 if unset
     */
    @ZAttr(id=518)
    public int getHttpNumThreads() {
        return getIntAttr(Provisioning.A_zimbraHttpNumThreads, 250);
    }

    /**
     * number of http handler threads
     *
     * @param zimbraHttpNumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=518)
    public void setHttpNumThreads(int zimbraHttpNumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpNumThreads, Integer.toString(zimbraHttpNumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of http handler threads
     *
     * @param zimbraHttpNumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=518)
    public Map<String,Object> setHttpNumThreads(int zimbraHttpNumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpNumThreads, Integer.toString(zimbraHttpNumThreads));
        return attrs;
    }

    /**
     * number of http handler threads
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=518)
    public void unsetHttpNumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpNumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of http handler threads
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=518)
    public Map<String,Object> unsetHttpNumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpNumThreads, "");
        return attrs;
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @return zimbraHttpProxyURL, or ampty array if unset
     */
    @ZAttr(id=388)
    public String[] getHttpProxyURL() {
        return getMultiAttr(Provisioning.A_zimbraHttpProxyURL);
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param zimbraHttpProxyURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=388)
    public void setHttpProxyURL(String[] zimbraHttpProxyURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpProxyURL, zimbraHttpProxyURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param zimbraHttpProxyURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=388)
    public Map<String,Object> setHttpProxyURL(String[] zimbraHttpProxyURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpProxyURL, zimbraHttpProxyURL);
        return attrs;
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param zimbraHttpProxyURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=388)
    public void addHttpProxyURL(String zimbraHttpProxyURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraHttpProxyURL, zimbraHttpProxyURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param zimbraHttpProxyURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=388)
    public Map<String,Object> addHttpProxyURL(String zimbraHttpProxyURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraHttpProxyURL, zimbraHttpProxyURL);
        return attrs;
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param zimbraHttpProxyURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=388)
    public void removeHttpProxyURL(String zimbraHttpProxyURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraHttpProxyURL, zimbraHttpProxyURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param zimbraHttpProxyURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=388)
    public Map<String,Object> removeHttpProxyURL(String zimbraHttpProxyURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraHttpProxyURL, zimbraHttpProxyURL);
        return attrs;
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=388)
    public void unsetHttpProxyURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpProxyURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=388)
    public Map<String,Object> unsetHttpProxyURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpProxyURL, "");
        return attrs;
    }

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     *
     * @return zimbraHttpSSLNumThreads, or 50 if unset
     */
    @ZAttr(id=519)
    public int getHttpSSLNumThreads() {
        return getIntAttr(Provisioning.A_zimbraHttpSSLNumThreads, 50);
    }

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     *
     * @param zimbraHttpSSLNumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=519)
    public void setHttpSSLNumThreads(int zimbraHttpSSLNumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpSSLNumThreads, Integer.toString(zimbraHttpSSLNumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     *
     * @param zimbraHttpSSLNumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=519)
    public Map<String,Object> setHttpSSLNumThreads(int zimbraHttpSSLNumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpSSLNumThreads, Integer.toString(zimbraHttpSSLNumThreads));
        return attrs;
    }

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=519)
    public void unsetHttpSSLNumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpSSLNumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=519)
    public Map<String,Object> unsetHttpSSLNumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHttpSSLNumThreads, "");
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraImapBindOnStartup, or true if unset
     */
    @ZAttr(id=268)
    public boolean isImapBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraImapBindOnStartup, true);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraImapBindOnStartup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=268)
    public void setImapBindOnStartup(boolean zimbraImapBindOnStartup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindOnStartup, zimbraImapBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraImapBindOnStartup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=268)
    public Map<String,Object> setImapBindOnStartup(boolean zimbraImapBindOnStartup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindOnStartup, zimbraImapBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=268)
    public void unsetImapBindOnStartup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindOnStartup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=268)
    public Map<String,Object> unsetImapBindOnStartup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindOnStartup, "");
        return attrs;
    }

    /**
     * port number on which IMAP server should listen
     *
     * @return zimbraImapBindPort, or "7143" if unset
     */
    @ZAttr(id=180)
    public String getImapBindPort() {
        return getAttr(Provisioning.A_zimbraImapBindPort, "7143");
    }

    /**
     * port number on which IMAP server should listen
     *
     * @param zimbraImapBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=180)
    public void setImapBindPort(String zimbraImapBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindPort, zimbraImapBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAP server should listen
     *
     * @param zimbraImapBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=180)
    public Map<String,Object> setImapBindPort(String zimbraImapBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindPort, zimbraImapBindPort);
        return attrs;
    }

    /**
     * port number on which IMAP server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=180)
    public void unsetImapBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAP server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=180)
    public Map<String,Object> unsetImapBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindPort, "");
        return attrs;
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @return zimbraImapCleartextLoginEnabled, or false if unset
     */
    @ZAttr(id=185)
    public boolean isImapCleartextLoginEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapCleartextLoginEnabled, false);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @param zimbraImapCleartextLoginEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=185)
    public void setImapCleartextLoginEnabled(boolean zimbraImapCleartextLoginEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapCleartextLoginEnabled, zimbraImapCleartextLoginEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @param zimbraImapCleartextLoginEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=185)
    public Map<String,Object> setImapCleartextLoginEnabled(boolean zimbraImapCleartextLoginEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapCleartextLoginEnabled, zimbraImapCleartextLoginEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=185)
    public void unsetImapCleartextLoginEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapCleartextLoginEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=185)
    public Map<String,Object> unsetImapCleartextLoginEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapCleartextLoginEnabled, "");
        return attrs;
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @return zimbraImapDisabledCapability, or ampty array if unset
     */
    @ZAttr(id=443)
    public String[] getImapDisabledCapability() {
        return getMultiAttr(Provisioning.A_zimbraImapDisabledCapability);
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param zimbraImapDisabledCapability new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=443)
    public void setImapDisabledCapability(String[] zimbraImapDisabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapDisabledCapability, zimbraImapDisabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param zimbraImapDisabledCapability new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=443)
    public Map<String,Object> setImapDisabledCapability(String[] zimbraImapDisabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapDisabledCapability, zimbraImapDisabledCapability);
        return attrs;
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param zimbraImapDisabledCapability new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=443)
    public void addImapDisabledCapability(String zimbraImapDisabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraImapDisabledCapability, zimbraImapDisabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param zimbraImapDisabledCapability new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=443)
    public Map<String,Object> addImapDisabledCapability(String zimbraImapDisabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraImapDisabledCapability, zimbraImapDisabledCapability);
        return attrs;
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param zimbraImapDisabledCapability existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=443)
    public void removeImapDisabledCapability(String zimbraImapDisabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraImapDisabledCapability, zimbraImapDisabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param zimbraImapDisabledCapability existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=443)
    public Map<String,Object> removeImapDisabledCapability(String zimbraImapDisabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraImapDisabledCapability, zimbraImapDisabledCapability);
        return attrs;
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=443)
    public void unsetImapDisabledCapability() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapDisabledCapability, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=443)
    public Map<String,Object> unsetImapDisabledCapability(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapDisabledCapability, "");
        return attrs;
    }

    /**
     * Whether to expose version on IMAP banner
     *
     * @return zimbraImapExposeVersionOnBanner, or false if unset
     */
    @ZAttr(id=693)
    public boolean isImapExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraImapExposeVersionOnBanner, false);
    }

    /**
     * Whether to expose version on IMAP banner
     *
     * @param zimbraImapExposeVersionOnBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=693)
    public void setImapExposeVersionOnBanner(boolean zimbraImapExposeVersionOnBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapExposeVersionOnBanner, zimbraImapExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on IMAP banner
     *
     * @param zimbraImapExposeVersionOnBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=693)
    public Map<String,Object> setImapExposeVersionOnBanner(boolean zimbraImapExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapExposeVersionOnBanner, zimbraImapExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to expose version on IMAP banner
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=693)
    public void unsetImapExposeVersionOnBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapExposeVersionOnBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on IMAP banner
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=693)
    public Map<String,Object> unsetImapExposeVersionOnBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapExposeVersionOnBanner, "");
        return attrs;
    }

    /**
     * number of handler threads
     *
     * @return zimbraImapNumThreads, or 200 if unset
     */
    @ZAttr(id=181)
    public int getImapNumThreads() {
        return getIntAttr(Provisioning.A_zimbraImapNumThreads, 200);
    }

    /**
     * number of handler threads
     *
     * @param zimbraImapNumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=181)
    public void setImapNumThreads(int zimbraImapNumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNumThreads, Integer.toString(zimbraImapNumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of handler threads
     *
     * @param zimbraImapNumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=181)
    public Map<String,Object> setImapNumThreads(int zimbraImapNumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNumThreads, Integer.toString(zimbraImapNumThreads));
        return attrs;
    }

    /**
     * number of handler threads
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=181)
    public void unsetImapNumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of handler threads
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=181)
    public Map<String,Object> unsetImapNumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNumThreads, "");
        return attrs;
    }

    /**
     * port number on which IMAP proxy server should listen
     *
     * @return zimbraImapProxyBindPort, or "143" if unset
     */
    @ZAttr(id=348)
    public String getImapProxyBindPort() {
        return getAttr(Provisioning.A_zimbraImapProxyBindPort, "143");
    }

    /**
     * port number on which IMAP proxy server should listen
     *
     * @param zimbraImapProxyBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=348)
    public void setImapProxyBindPort(String zimbraImapProxyBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapProxyBindPort, zimbraImapProxyBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAP proxy server should listen
     *
     * @param zimbraImapProxyBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=348)
    public Map<String,Object> setImapProxyBindPort(String zimbraImapProxyBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapProxyBindPort, zimbraImapProxyBindPort);
        return attrs;
    }

    /**
     * port number on which IMAP proxy server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=348)
    public void unsetImapProxyBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapProxyBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAP proxy server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=348)
    public Map<String,Object> unsetImapProxyBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapProxyBindPort, "");
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraImapSSLBindOnStartup, or true if unset
     */
    @ZAttr(id=269)
    public boolean isImapSSLBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraImapSSLBindOnStartup, true);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraImapSSLBindOnStartup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=269)
    public void setImapSSLBindOnStartup(boolean zimbraImapSSLBindOnStartup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindOnStartup, zimbraImapSSLBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraImapSSLBindOnStartup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=269)
    public Map<String,Object> setImapSSLBindOnStartup(boolean zimbraImapSSLBindOnStartup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindOnStartup, zimbraImapSSLBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=269)
    public void unsetImapSSLBindOnStartup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindOnStartup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=269)
    public Map<String,Object> unsetImapSSLBindOnStartup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindOnStartup, "");
        return attrs;
    }

    /**
     * port number on which IMAP SSL server should listen on
     *
     * @return zimbraImapSSLBindPort, or "7993" if unset
     */
    @ZAttr(id=183)
    public String getImapSSLBindPort() {
        return getAttr(Provisioning.A_zimbraImapSSLBindPort, "7993");
    }

    /**
     * port number on which IMAP SSL server should listen on
     *
     * @param zimbraImapSSLBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=183)
    public void setImapSSLBindPort(String zimbraImapSSLBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindPort, zimbraImapSSLBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAP SSL server should listen on
     *
     * @param zimbraImapSSLBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=183)
    public Map<String,Object> setImapSSLBindPort(String zimbraImapSSLBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindPort, zimbraImapSSLBindPort);
        return attrs;
    }

    /**
     * port number on which IMAP SSL server should listen on
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=183)
    public void unsetImapSSLBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAP SSL server should listen on
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=183)
    public Map<String,Object> unsetImapSSLBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindPort, "");
        return attrs;
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @return zimbraImapSSLDisabledCapability, or ampty array if unset
     */
    @ZAttr(id=444)
    public String[] getImapSSLDisabledCapability() {
        return getMultiAttr(Provisioning.A_zimbraImapSSLDisabledCapability);
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param zimbraImapSSLDisabledCapability new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=444)
    public void setImapSSLDisabledCapability(String[] zimbraImapSSLDisabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLDisabledCapability, zimbraImapSSLDisabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param zimbraImapSSLDisabledCapability new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=444)
    public Map<String,Object> setImapSSLDisabledCapability(String[] zimbraImapSSLDisabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLDisabledCapability, zimbraImapSSLDisabledCapability);
        return attrs;
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param zimbraImapSSLDisabledCapability new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=444)
    public void addImapSSLDisabledCapability(String zimbraImapSSLDisabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraImapSSLDisabledCapability, zimbraImapSSLDisabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param zimbraImapSSLDisabledCapability new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=444)
    public Map<String,Object> addImapSSLDisabledCapability(String zimbraImapSSLDisabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraImapSSLDisabledCapability, zimbraImapSSLDisabledCapability);
        return attrs;
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param zimbraImapSSLDisabledCapability existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=444)
    public void removeImapSSLDisabledCapability(String zimbraImapSSLDisabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraImapSSLDisabledCapability, zimbraImapSSLDisabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param zimbraImapSSLDisabledCapability existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=444)
    public Map<String,Object> removeImapSSLDisabledCapability(String zimbraImapSSLDisabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraImapSSLDisabledCapability, zimbraImapSSLDisabledCapability);
        return attrs;
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=444)
    public void unsetImapSSLDisabledCapability() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLDisabledCapability, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=444)
    public Map<String,Object> unsetImapSSLDisabledCapability(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLDisabledCapability, "");
        return attrs;
    }

    /**
     * port number on which IMAPS proxy server should listen
     *
     * @return zimbraImapSSLProxyBindPort, or "993" if unset
     */
    @ZAttr(id=349)
    public String getImapSSLProxyBindPort() {
        return getAttr(Provisioning.A_zimbraImapSSLProxyBindPort, "993");
    }

    /**
     * port number on which IMAPS proxy server should listen
     *
     * @param zimbraImapSSLProxyBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=349)
    public void setImapSSLProxyBindPort(String zimbraImapSSLProxyBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLProxyBindPort, zimbraImapSSLProxyBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAPS proxy server should listen
     *
     * @param zimbraImapSSLProxyBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=349)
    public Map<String,Object> setImapSSLProxyBindPort(String zimbraImapSSLProxyBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLProxyBindPort, zimbraImapSSLProxyBindPort);
        return attrs;
    }

    /**
     * port number on which IMAPS proxy server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=349)
    public void unsetImapSSLProxyBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLProxyBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which IMAPS proxy server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=349)
    public Map<String,Object> unsetImapSSLProxyBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLProxyBindPort, "");
        return attrs;
    }

    /**
     * whether IMAP SSL server is enabled for a given server
     *
     * @return zimbraImapSSLServerEnabled, or true if unset
     */
    @ZAttr(id=184)
    public boolean isImapSSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapSSLServerEnabled, true);
    }

    /**
     * whether IMAP SSL server is enabled for a given server
     *
     * @param zimbraImapSSLServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=184)
    public void setImapSSLServerEnabled(boolean zimbraImapSSLServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLServerEnabled, zimbraImapSSLServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP SSL server is enabled for a given server
     *
     * @param zimbraImapSSLServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=184)
    public Map<String,Object> setImapSSLServerEnabled(boolean zimbraImapSSLServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLServerEnabled, zimbraImapSSLServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether IMAP SSL server is enabled for a given server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=184)
    public void unsetImapSSLServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP SSL server is enabled for a given server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=184)
    public Map<String,Object> unsetImapSSLServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLServerEnabled, "");
        return attrs;
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @return zimbraImapSaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=555)
    public boolean isImapSaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapSaslGssapiEnabled, false);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @param zimbraImapSaslGssapiEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=555)
    public void setImapSaslGssapiEnabled(boolean zimbraImapSaslGssapiEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSaslGssapiEnabled, zimbraImapSaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @param zimbraImapSaslGssapiEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=555)
    public Map<String,Object> setImapSaslGssapiEnabled(boolean zimbraImapSaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSaslGssapiEnabled, zimbraImapSaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=555)
    public void unsetImapSaslGssapiEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSaslGssapiEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=555)
    public Map<String,Object> unsetImapSaslGssapiEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSaslGssapiEnabled, "");
        return attrs;
    }

    /**
     * whether IMAP server is enabled for a given server
     *
     * @return zimbraImapServerEnabled, or true if unset
     */
    @ZAttr(id=176)
    public boolean isImapServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapServerEnabled, true);
    }

    /**
     * whether IMAP server is enabled for a given server
     *
     * @param zimbraImapServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=176)
    public void setImapServerEnabled(boolean zimbraImapServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapServerEnabled, zimbraImapServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP server is enabled for a given server
     *
     * @param zimbraImapServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=176)
    public Map<String,Object> setImapServerEnabled(boolean zimbraImapServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapServerEnabled, zimbraImapServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether IMAP server is enabled for a given server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=176)
    public void unsetImapServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP server is enabled for a given server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=176)
    public Map<String,Object> unsetImapServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapServerEnabled, "");
        return attrs;
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @return zimbraInstalledSkin, or ampty array if unset
     */
    @ZAttr(id=368)
    public String[] getInstalledSkin() {
        return getMultiAttr(Provisioning.A_zimbraInstalledSkin);
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param zimbraInstalledSkin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=368)
    public void setInstalledSkin(String[] zimbraInstalledSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInstalledSkin, zimbraInstalledSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param zimbraInstalledSkin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=368)
    public Map<String,Object> setInstalledSkin(String[] zimbraInstalledSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInstalledSkin, zimbraInstalledSkin);
        return attrs;
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param zimbraInstalledSkin new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=368)
    public void addInstalledSkin(String zimbraInstalledSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraInstalledSkin, zimbraInstalledSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param zimbraInstalledSkin new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=368)
    public Map<String,Object> addInstalledSkin(String zimbraInstalledSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraInstalledSkin, zimbraInstalledSkin);
        return attrs;
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param zimbraInstalledSkin existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=368)
    public void removeInstalledSkin(String zimbraInstalledSkin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraInstalledSkin, zimbraInstalledSkin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param zimbraInstalledSkin existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=368)
    public Map<String,Object> removeInstalledSkin(String zimbraInstalledSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraInstalledSkin, zimbraInstalledSkin);
        return attrs;
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=368)
    public void unsetInstalledSkin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInstalledSkin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=368)
    public Map<String,Object> unsetInstalledSkin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInstalledSkin, "");
        return attrs;
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * <p>Use getLastLogonTimestampFrequencyAsString to access value as a string.
     *
     * @see #getLastLogonTimestampFrequencyAsString()
     *
     * @return zimbraLastLogonTimestampFrequency in millseconds, or 604800000 (7d)  if unset
     */
    @ZAttr(id=114)
    public long getLastLogonTimestampFrequency() {
        return getTimeInterval(Provisioning.A_zimbraLastLogonTimestampFrequency, 604800000L);
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @return zimbraLastLogonTimestampFrequency, or "7d" if unset
     */
    @ZAttr(id=114)
    public String getLastLogonTimestampFrequencyAsString() {
        return getAttr(Provisioning.A_zimbraLastLogonTimestampFrequency, "7d");
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @param zimbraLastLogonTimestampFrequency new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=114)
    public void setLastLogonTimestampFrequency(String zimbraLastLogonTimestampFrequency) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestampFrequency, zimbraLastLogonTimestampFrequency);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @param zimbraLastLogonTimestampFrequency new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=114)
    public Map<String,Object> setLastLogonTimestampFrequency(String zimbraLastLogonTimestampFrequency, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestampFrequency, zimbraLastLogonTimestampFrequency);
        return attrs;
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=114)
    public void unsetLastLogonTimestampFrequency() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestampFrequency, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=114)
    public Map<String,Object> unsetLastLogonTimestampFrequency(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestampFrequency, "");
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraLmtpBindOnStartup, or false if unset
     */
    @ZAttr(id=270)
    public boolean isLmtpBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpBindOnStartup, false);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraLmtpBindOnStartup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=270)
    public void setLmtpBindOnStartup(boolean zimbraLmtpBindOnStartup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindOnStartup, zimbraLmtpBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraLmtpBindOnStartup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=270)
    public Map<String,Object> setLmtpBindOnStartup(boolean zimbraLmtpBindOnStartup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindOnStartup, zimbraLmtpBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=270)
    public void unsetLmtpBindOnStartup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindOnStartup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=270)
    public Map<String,Object> unsetLmtpBindOnStartup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindOnStartup, "");
        return attrs;
    }

    /**
     * port number on which LMTP server should listen
     *
     * @return zimbraLmtpBindPort, or "7025" if unset
     */
    @ZAttr(id=24)
    public String getLmtpBindPort() {
        return getAttr(Provisioning.A_zimbraLmtpBindPort, "7025");
    }

    /**
     * port number on which LMTP server should listen
     *
     * @param zimbraLmtpBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=24)
    public void setLmtpBindPort(String zimbraLmtpBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindPort, zimbraLmtpBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which LMTP server should listen
     *
     * @param zimbraLmtpBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=24)
    public Map<String,Object> setLmtpBindPort(String zimbraLmtpBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindPort, zimbraLmtpBindPort);
        return attrs;
    }

    /**
     * port number on which LMTP server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=24)
    public void unsetLmtpBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which LMTP server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=24)
    public Map<String,Object> unsetLmtpBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindPort, "");
        return attrs;
    }

    /**
     * Whether to expose version on LMTP banner
     *
     * @return zimbraLmtpExposeVersionOnBanner, or false if unset
     */
    @ZAttr(id=691)
    public boolean isLmtpExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpExposeVersionOnBanner, false);
    }

    /**
     * Whether to expose version on LMTP banner
     *
     * @param zimbraLmtpExposeVersionOnBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=691)
    public void setLmtpExposeVersionOnBanner(boolean zimbraLmtpExposeVersionOnBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpExposeVersionOnBanner, zimbraLmtpExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on LMTP banner
     *
     * @param zimbraLmtpExposeVersionOnBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=691)
    public Map<String,Object> setLmtpExposeVersionOnBanner(boolean zimbraLmtpExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpExposeVersionOnBanner, zimbraLmtpExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to expose version on LMTP banner
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=691)
    public void unsetLmtpExposeVersionOnBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpExposeVersionOnBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on LMTP banner
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=691)
    public Map<String,Object> unsetLmtpExposeVersionOnBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpExposeVersionOnBanner, "");
        return attrs;
    }

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     *
     * @return zimbraLmtpNumThreads, or 20 if unset
     */
    @ZAttr(id=26)
    public int getLmtpNumThreads() {
        return getIntAttr(Provisioning.A_zimbraLmtpNumThreads, 20);
    }

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     *
     * @param zimbraLmtpNumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=26)
    public void setLmtpNumThreads(int zimbraLmtpNumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpNumThreads, Integer.toString(zimbraLmtpNumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     *
     * @param zimbraLmtpNumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=26)
    public Map<String,Object> setLmtpNumThreads(int zimbraLmtpNumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpNumThreads, Integer.toString(zimbraLmtpNumThreads));
        return attrs;
    }

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=26)
    public void unsetLmtpNumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpNumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=26)
    public Map<String,Object> unsetLmtpNumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpNumThreads, "");
        return attrs;
    }

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @return zimbraLmtpPermanentFailureWhenOverQuota, or false if unset
     */
    @ZAttr(id=657)
    public boolean isLmtpPermanentFailureWhenOverQuota() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, false);
    }

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @param zimbraLmtpPermanentFailureWhenOverQuota new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=657)
    public void setLmtpPermanentFailureWhenOverQuota(boolean zimbraLmtpPermanentFailureWhenOverQuota) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, zimbraLmtpPermanentFailureWhenOverQuota ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @param zimbraLmtpPermanentFailureWhenOverQuota new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=657)
    public Map<String,Object> setLmtpPermanentFailureWhenOverQuota(boolean zimbraLmtpPermanentFailureWhenOverQuota, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, zimbraLmtpPermanentFailureWhenOverQuota ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=657)
    public void unsetLmtpPermanentFailureWhenOverQuota() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=657)
    public Map<String,Object> unsetLmtpPermanentFailureWhenOverQuota(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, "");
        return attrs;
    }

    /**
     * whether LMTP server is enabled for a given server
     *
     * @return zimbraLmtpServerEnabled, or true if unset
     */
    @ZAttr(id=630)
    public boolean isLmtpServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpServerEnabled, true);
    }

    /**
     * whether LMTP server is enabled for a given server
     *
     * @param zimbraLmtpServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=630)
    public void setLmtpServerEnabled(boolean zimbraLmtpServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpServerEnabled, zimbraLmtpServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether LMTP server is enabled for a given server
     *
     * @param zimbraLmtpServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=630)
    public Map<String,Object> setLmtpServerEnabled(boolean zimbraLmtpServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpServerEnabled, zimbraLmtpServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether LMTP server is enabled for a given server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=630)
    public void unsetLmtpServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether LMTP server is enabled for a given server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=630)
    public Map<String,Object> unsetLmtpServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpServerEnabled, "");
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
     * destination for syslog messages
     *
     * @return zimbraLogHostname, or ampty array if unset
     */
    @ZAttr(id=250)
    public String[] getLogHostname() {
        return getMultiAttr(Provisioning.A_zimbraLogHostname);
    }

    /**
     * destination for syslog messages
     *
     * @param zimbraLogHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=250)
    public void setLogHostname(String[] zimbraLogHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogHostname, zimbraLogHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * destination for syslog messages
     *
     * @param zimbraLogHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=250)
    public Map<String,Object> setLogHostname(String[] zimbraLogHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogHostname, zimbraLogHostname);
        return attrs;
    }

    /**
     * destination for syslog messages
     *
     * @param zimbraLogHostname new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=250)
    public void addLogHostname(String zimbraLogHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraLogHostname, zimbraLogHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * destination for syslog messages
     *
     * @param zimbraLogHostname new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=250)
    public Map<String,Object> addLogHostname(String zimbraLogHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraLogHostname, zimbraLogHostname);
        return attrs;
    }

    /**
     * destination for syslog messages
     *
     * @param zimbraLogHostname existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=250)
    public void removeLogHostname(String zimbraLogHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraLogHostname, zimbraLogHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * destination for syslog messages
     *
     * @param zimbraLogHostname existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=250)
    public Map<String,Object> removeLogHostname(String zimbraLogHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraLogHostname, zimbraLogHostname);
        return attrs;
    }

    /**
     * destination for syslog messages
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=250)
    public void unsetLogHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * destination for syslog messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=250)
    public Map<String,Object> unsetLogHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogHostname, "");
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * <p>Use getLogRawLifetimeAsString to access value as a string.
     *
     * @see #getLogRawLifetimeAsString()
     *
     * @return zimbraLogRawLifetime in millseconds, or 2678400000 (31d)  if unset
     */
    @ZAttr(id=263)
    public long getLogRawLifetime() {
        return getTimeInterval(Provisioning.A_zimbraLogRawLifetime, 2678400000L);
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @return zimbraLogRawLifetime, or "31d" if unset
     */
    @ZAttr(id=263)
    public String getLogRawLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraLogRawLifetime, "31d");
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @param zimbraLogRawLifetime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=263)
    public void setLogRawLifetime(String zimbraLogRawLifetime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogRawLifetime, zimbraLogRawLifetime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @param zimbraLogRawLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=263)
    public Map<String,Object> setLogRawLifetime(String zimbraLogRawLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogRawLifetime, zimbraLogRawLifetime);
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=263)
    public void unsetLogRawLifetime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogRawLifetime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=263)
    public Map<String,Object> unsetLogRawLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogRawLifetime, "");
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * <p>Use getLogSummaryLifetimeAsString to access value as a string.
     *
     * @see #getLogSummaryLifetimeAsString()
     *
     * @return zimbraLogSummaryLifetime in millseconds, or 63072000000 (730d)  if unset
     */
    @ZAttr(id=264)
    public long getLogSummaryLifetime() {
        return getTimeInterval(Provisioning.A_zimbraLogSummaryLifetime, 63072000000L);
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @return zimbraLogSummaryLifetime, or "730d" if unset
     */
    @ZAttr(id=264)
    public String getLogSummaryLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraLogSummaryLifetime, "730d");
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @param zimbraLogSummaryLifetime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=264)
    public void setLogSummaryLifetime(String zimbraLogSummaryLifetime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogSummaryLifetime, zimbraLogSummaryLifetime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @param zimbraLogSummaryLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=264)
    public Map<String,Object> setLogSummaryLifetime(String zimbraLogSummaryLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogSummaryLifetime, zimbraLogSummaryLifetime);
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=264)
    public void unsetLogSummaryLifetime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogSummaryLifetime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=264)
    public Map<String,Object> unsetLogSummaryLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogSummaryLifetime, "");
        return attrs;
    }

    /**
     * whether mailbox server should log to syslog
     *
     * @return zimbraLogToSyslog, or false if unset
     */
    @ZAttr(id=520)
    public boolean isLogToSyslog() {
        return getBooleanAttr(Provisioning.A_zimbraLogToSyslog, false);
    }

    /**
     * whether mailbox server should log to syslog
     *
     * @param zimbraLogToSyslog new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=520)
    public void setLogToSyslog(boolean zimbraLogToSyslog) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogToSyslog, zimbraLogToSyslog ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether mailbox server should log to syslog
     *
     * @param zimbraLogToSyslog new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=520)
    public Map<String,Object> setLogToSyslog(boolean zimbraLogToSyslog, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogToSyslog, zimbraLogToSyslog ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether mailbox server should log to syslog
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=520)
    public void unsetLogToSyslog() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogToSyslog, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether mailbox server should log to syslog
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=520)
    public Map<String,Object> unsetLogToSyslog(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogToSyslog, "");
        return attrs;
    }

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @return zimbraMailDiskStreamingThreshold, or 1048576 if unset
     */
    @ZAttr(id=565)
    public int getMailDiskStreamingThreshold() {
        return getIntAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, 1048576);
    }

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @param zimbraMailDiskStreamingThreshold new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=565)
    public void setMailDiskStreamingThreshold(int zimbraMailDiskStreamingThreshold) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDiskStreamingThreshold, Integer.toString(zimbraMailDiskStreamingThreshold));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @param zimbraMailDiskStreamingThreshold new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=565)
    public Map<String,Object> setMailDiskStreamingThreshold(int zimbraMailDiskStreamingThreshold, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDiskStreamingThreshold, Integer.toString(zimbraMailDiskStreamingThreshold));
        return attrs;
    }

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=565)
    public void unsetMailDiskStreamingThreshold() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDiskStreamingThreshold, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=565)
    public Map<String,Object> unsetMailDiskStreamingThreshold(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDiskStreamingThreshold, "");
        return attrs;
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @return zimbraMailMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=308)
    public ZAttrProvisioning.MailMode getMailMode() {
        try { String v = getAttr(Provisioning.A_zimbraMailMode); return v == null ? null : ZAttrProvisioning.MailMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @return zimbraMailMode, or null if unset
     */
    @ZAttr(id=308)
    public String getMailModeAsString() {
        return getAttr(Provisioning.A_zimbraMailMode, null);
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraMailMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=308)
    public void setMailMode(ZAttrProvisioning.MailMode zimbraMailMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMode, zimbraMailMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraMailMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=308)
    public Map<String,Object> setMailMode(ZAttrProvisioning.MailMode zimbraMailMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMode, zimbraMailMode.toString());
        return attrs;
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraMailMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=308)
    public void setMailModeAsString(String zimbraMailMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMode, zimbraMailMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraMailMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=308)
    public Map<String,Object> setMailModeAsString(String zimbraMailMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMode, zimbraMailMode);
        return attrs;
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=308)
    public void unsetMailMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=308)
    public Map<String,Object> unsetMailMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMode, "");
        return attrs;
    }

    /**
     * HTTP port for end-user UI
     *
     * @return zimbraMailPort, or "80" if unset
     */
    @ZAttr(id=154)
    public String getMailPort() {
        return getAttr(Provisioning.A_zimbraMailPort, "80");
    }

    /**
     * HTTP port for end-user UI
     *
     * @param zimbraMailPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=154)
    public void setMailPort(String zimbraMailPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPort, zimbraMailPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTP port for end-user UI
     *
     * @param zimbraMailPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=154)
    public Map<String,Object> setMailPort(String zimbraMailPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPort, zimbraMailPort);
        return attrs;
    }

    /**
     * HTTP port for end-user UI
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=154)
    public void unsetMailPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTP port for end-user UI
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=154)
    public Map<String,Object> unsetMailPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPort, "");
        return attrs;
    }

    /**
     * HTTP proxy port
     *
     * @return zimbraMailProxyPort, or "0" if unset
     */
    @ZAttr(id=626)
    public String getMailProxyPort() {
        return getAttr(Provisioning.A_zimbraMailProxyPort, "0");
    }

    /**
     * HTTP proxy port
     *
     * @param zimbraMailProxyPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=626)
    public void setMailProxyPort(String zimbraMailProxyPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailProxyPort, zimbraMailProxyPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTP proxy port
     *
     * @param zimbraMailProxyPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=626)
    public Map<String,Object> setMailProxyPort(String zimbraMailProxyPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailProxyPort, zimbraMailProxyPort);
        return attrs;
    }

    /**
     * HTTP proxy port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=626)
    public void unsetMailProxyPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailProxyPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTP proxy port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=626)
    public Map<String,Object> unsetMailProxyPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailProxyPort, "");
        return attrs;
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * <p>Use getMailPurgeSleepIntervalAsString to access value as a string.
     *
     * @see #getMailPurgeSleepIntervalAsString()
     *
     * @return zimbraMailPurgeSleepInterval in millseconds, or 60000 (1m)  if unset
     */
    @ZAttr(id=542)
    public long getMailPurgeSleepInterval() {
        return getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, 60000L);
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @return zimbraMailPurgeSleepInterval, or "1m" if unset
     */
    @ZAttr(id=542)
    public String getMailPurgeSleepIntervalAsString() {
        return getAttr(Provisioning.A_zimbraMailPurgeSleepInterval, "1m");
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @param zimbraMailPurgeSleepInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=542)
    public void setMailPurgeSleepInterval(String zimbraMailPurgeSleepInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPurgeSleepInterval, zimbraMailPurgeSleepInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @param zimbraMailPurgeSleepInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=542)
    public Map<String,Object> setMailPurgeSleepInterval(String zimbraMailPurgeSleepInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPurgeSleepInterval, zimbraMailPurgeSleepInterval);
        return attrs;
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=542)
    public void unsetMailPurgeSleepInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPurgeSleepInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=542)
    public Map<String,Object> unsetMailPurgeSleepInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPurgeSleepInterval, "");
        return attrs;
    }

    /**
     * If TRUE, the envelope sender of a message redirected by mail filters
     * will be set to the users address. If FALSE, the envelope sender will
     * be set to the From address of the redirected message.
     *
     * @return zimbraMailRedirectSetEnvelopeSender, or true if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public boolean isMailRedirectSetEnvelopeSender() {
        return getBooleanAttr(Provisioning.A_zimbraMailRedirectSetEnvelopeSender, true);
    }

    /**
     * If TRUE, the envelope sender of a message redirected by mail filters
     * will be set to the users address. If FALSE, the envelope sender will
     * be set to the From address of the redirected message.
     *
     * @param zimbraMailRedirectSetEnvelopeSender new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public void setMailRedirectSetEnvelopeSender(boolean zimbraMailRedirectSetEnvelopeSender) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailRedirectSetEnvelopeSender, zimbraMailRedirectSetEnvelopeSender ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If TRUE, the envelope sender of a message redirected by mail filters
     * will be set to the users address. If FALSE, the envelope sender will
     * be set to the From address of the redirected message.
     *
     * @param zimbraMailRedirectSetEnvelopeSender new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public Map<String,Object> setMailRedirectSetEnvelopeSender(boolean zimbraMailRedirectSetEnvelopeSender, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailRedirectSetEnvelopeSender, zimbraMailRedirectSetEnvelopeSender ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * If TRUE, the envelope sender of a message redirected by mail filters
     * will be set to the users address. If FALSE, the envelope sender will
     * be set to the From address of the redirected message.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public void unsetMailRedirectSetEnvelopeSender() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailRedirectSetEnvelopeSender, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If TRUE, the envelope sender of a message redirected by mail filters
     * will be set to the users address. If FALSE, the envelope sender will
     * be set to the From address of the redirected message.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public Map<String,Object> unsetMailRedirectSetEnvelopeSender(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailRedirectSetEnvelopeSender, "");
        return attrs;
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @return zimbraMailReferMode, or ZAttrProvisioning.MailReferMode.wronghost if unset and/or has invalid value
     */
    @ZAttr(id=613)
    public ZAttrProvisioning.MailReferMode getMailReferMode() {
        try { String v = getAttr(Provisioning.A_zimbraMailReferMode); return v == null ? ZAttrProvisioning.MailReferMode.wronghost : ZAttrProvisioning.MailReferMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.MailReferMode.wronghost; }
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @return zimbraMailReferMode, or "wronghost" if unset
     */
    @ZAttr(id=613)
    public String getMailReferModeAsString() {
        return getAttr(Provisioning.A_zimbraMailReferMode, "wronghost");
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @param zimbraMailReferMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=613)
    public void setMailReferMode(ZAttrProvisioning.MailReferMode zimbraMailReferMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, zimbraMailReferMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @param zimbraMailReferMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=613)
    public Map<String,Object> setMailReferMode(ZAttrProvisioning.MailReferMode zimbraMailReferMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, zimbraMailReferMode.toString());
        return attrs;
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @param zimbraMailReferMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=613)
    public void setMailReferModeAsString(String zimbraMailReferMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, zimbraMailReferMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @param zimbraMailReferMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=613)
    public Map<String,Object> setMailReferModeAsString(String zimbraMailReferMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, zimbraMailReferMode);
        return attrs;
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=613)
    public void unsetMailReferMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=613)
    public Map<String,Object> unsetMailReferMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, "");
        return attrs;
    }

    /**
     * SSL port for end-user UI
     *
     * @return zimbraMailSSLPort, or "0" if unset
     */
    @ZAttr(id=166)
    public String getMailSSLPort() {
        return getAttr(Provisioning.A_zimbraMailSSLPort, "0");
    }

    /**
     * SSL port for end-user UI
     *
     * @param zimbraMailSSLPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=166)
    public void setMailSSLPort(String zimbraMailSSLPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLPort, zimbraMailSSLPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL port for end-user UI
     *
     * @param zimbraMailSSLPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=166)
    public Map<String,Object> setMailSSLPort(String zimbraMailSSLPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLPort, zimbraMailSSLPort);
        return attrs;
    }

    /**
     * SSL port for end-user UI
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=166)
    public void unsetMailSSLPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL port for end-user UI
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=166)
    public Map<String,Object> unsetMailSSLPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLPort, "");
        return attrs;
    }

    /**
     * SSL port HTTP proxy
     *
     * @return zimbraMailSSLProxyPort, or "0" if unset
     */
    @ZAttr(id=627)
    public String getMailSSLProxyPort() {
        return getAttr(Provisioning.A_zimbraMailSSLProxyPort, "0");
    }

    /**
     * SSL port HTTP proxy
     *
     * @param zimbraMailSSLProxyPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=627)
    public void setMailSSLProxyPort(String zimbraMailSSLProxyPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLProxyPort, zimbraMailSSLProxyPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL port HTTP proxy
     *
     * @param zimbraMailSSLProxyPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=627)
    public Map<String,Object> setMailSSLProxyPort(String zimbraMailSSLProxyPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLProxyPort, zimbraMailSSLProxyPort);
        return attrs;
    }

    /**
     * SSL port HTTP proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=627)
    public void unsetMailSSLProxyPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLProxyPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SSL port HTTP proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=627)
    public Map<String,Object> unsetMailSSLProxyPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSSLProxyPort, "");
        return attrs;
    }

    /**
     * URL prefix for where the zimbra app resides on this server
     *
     * @return zimbraMailURL, or "/zimbra" if unset
     */
    @ZAttr(id=340)
    public String getMailURL() {
        return getAttr(Provisioning.A_zimbraMailURL, "/zimbra");
    }

    /**
     * URL prefix for where the zimbra app resides on this server
     *
     * @param zimbraMailURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=340)
    public void setMailURL(String zimbraMailURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailURL, zimbraMailURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL prefix for where the zimbra app resides on this server
     *
     * @param zimbraMailURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=340)
    public Map<String,Object> setMailURL(String zimbraMailURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailURL, zimbraMailURL);
        return attrs;
    }

    /**
     * URL prefix for where the zimbra app resides on this server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=340)
    public void unsetMailURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL prefix for where the zimbra app resides on this server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=340)
    public Map<String,Object> unsetMailURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailURL, "");
        return attrs;
    }

    /**
     * port number on which memcached server should listen
     *
     * @return zimbraMemcachedBindPort, or "11211" if unset
     */
    @ZAttr(id=580)
    public String getMemcachedBindPort() {
        return getAttr(Provisioning.A_zimbraMemcachedBindPort, "11211");
    }

    /**
     * port number on which memcached server should listen
     *
     * @param zimbraMemcachedBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=580)
    public void setMemcachedBindPort(String zimbraMemcachedBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedBindPort, zimbraMemcachedBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which memcached server should listen
     *
     * @param zimbraMemcachedBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=580)
    public Map<String,Object> setMemcachedBindPort(String zimbraMemcachedBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedBindPort, zimbraMemcachedBindPort);
        return attrs;
    }

    /**
     * port number on which memcached server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=580)
    public void unsetMemcachedBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which memcached server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=580)
    public Map<String,Object> unsetMemcachedBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedBindPort, "");
        return attrs;
    }

    /**
     * Size limit in number of bytes on the message cache.
     *
     * @return zimbraMessageCacheSize, or 1671168 if unset
     */
    @ZAttr(id=297)
    public int getMessageCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMessageCacheSize, 1671168);
    }

    /**
     * Size limit in number of bytes on the message cache.
     *
     * @param zimbraMessageCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=297)
    public void setMessageCacheSize(int zimbraMessageCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageCacheSize, Integer.toString(zimbraMessageCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size limit in number of bytes on the message cache.
     *
     * @param zimbraMessageCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=297)
    public Map<String,Object> setMessageCacheSize(int zimbraMessageCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageCacheSize, Integer.toString(zimbraMessageCacheSize));
        return attrs;
    }

    /**
     * Size limit in number of bytes on the message cache.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=297)
    public void unsetMessageCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size limit in number of bytes on the message cache.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=297)
    public Map<String,Object> unsetMessageCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageCacheSize, "");
        return attrs;
    }

    /**
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     *
     * @return zimbraMessageIdDedupeCacheSize, or 3000 if unset
     */
    @ZAttr(id=334)
    public int getMessageIdDedupeCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMessageIdDedupeCacheSize, 3000);
    }

    /**
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     *
     * @param zimbraMessageIdDedupeCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=334)
    public void setMessageIdDedupeCacheSize(int zimbraMessageIdDedupeCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageIdDedupeCacheSize, Integer.toString(zimbraMessageIdDedupeCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     *
     * @param zimbraMessageIdDedupeCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=334)
    public Map<String,Object> setMessageIdDedupeCacheSize(int zimbraMessageIdDedupeCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageIdDedupeCacheSize, Integer.toString(zimbraMessageIdDedupeCacheSize));
        return attrs;
    }

    /**
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=334)
    public void unsetMessageIdDedupeCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageIdDedupeCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=334)
    public Map<String,Object> unsetMessageIdDedupeCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMessageIdDedupeCacheSize, "");
        return attrs;
    }

    /**
     * mta anti spam lock method.
     *
     * @return zimbraMtaAntiSpamLockMethod, or "flock" if unset
     */
    @ZAttr(id=612)
    public String getMtaAntiSpamLockMethod() {
        return getAttr(Provisioning.A_zimbraMtaAntiSpamLockMethod, "flock");
    }

    /**
     * mta anti spam lock method.
     *
     * @param zimbraMtaAntiSpamLockMethod new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=612)
    public void setMtaAntiSpamLockMethod(String zimbraMtaAntiSpamLockMethod) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAntiSpamLockMethod, zimbraMtaAntiSpamLockMethod);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mta anti spam lock method.
     *
     * @param zimbraMtaAntiSpamLockMethod new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=612)
    public Map<String,Object> setMtaAntiSpamLockMethod(String zimbraMtaAntiSpamLockMethod, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAntiSpamLockMethod, zimbraMtaAntiSpamLockMethod);
        return attrs;
    }

    /**
     * mta anti spam lock method.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=612)
    public void unsetMtaAntiSpamLockMethod() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAntiSpamLockMethod, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mta anti spam lock method.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=612)
    public Map<String,Object> unsetMtaAntiSpamLockMethod(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAntiSpamLockMethod, "");
        return attrs;
    }

    /**
     * Value for postconf smtpd_use_tls
     *
     * @return zimbraMtaAuthEnabled, or true if unset
     */
    @ZAttr(id=194)
    public boolean isMtaAuthEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMtaAuthEnabled, true);
    }

    /**
     * Value for postconf smtpd_use_tls
     *
     * @param zimbraMtaAuthEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=194)
    public void setMtaAuthEnabled(boolean zimbraMtaAuthEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthEnabled, zimbraMtaAuthEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf smtpd_use_tls
     *
     * @param zimbraMtaAuthEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=194)
    public Map<String,Object> setMtaAuthEnabled(boolean zimbraMtaAuthEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthEnabled, zimbraMtaAuthEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Value for postconf smtpd_use_tls
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=194)
    public void unsetMtaAuthEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf smtpd_use_tls
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=194)
    public Map<String,Object> unsetMtaAuthEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthEnabled, "");
        return attrs;
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @return zimbraMtaAuthHost, or ampty array if unset
     */
    @ZAttr(id=309)
    public String[] getMtaAuthHost() {
        return getMultiAttr(Provisioning.A_zimbraMtaAuthHost);
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param zimbraMtaAuthHost new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=309)
    public void setMtaAuthHost(String[] zimbraMtaAuthHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthHost, zimbraMtaAuthHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param zimbraMtaAuthHost new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=309)
    public Map<String,Object> setMtaAuthHost(String[] zimbraMtaAuthHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthHost, zimbraMtaAuthHost);
        return attrs;
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param zimbraMtaAuthHost new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=309)
    public void addMtaAuthHost(String zimbraMtaAuthHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaAuthHost, zimbraMtaAuthHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param zimbraMtaAuthHost new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=309)
    public Map<String,Object> addMtaAuthHost(String zimbraMtaAuthHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaAuthHost, zimbraMtaAuthHost);
        return attrs;
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param zimbraMtaAuthHost existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=309)
    public void removeMtaAuthHost(String zimbraMtaAuthHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaAuthHost, zimbraMtaAuthHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param zimbraMtaAuthHost existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=309)
    public Map<String,Object> removeMtaAuthHost(String zimbraMtaAuthHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaAuthHost, zimbraMtaAuthHost);
        return attrs;
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=309)
    public void unsetMtaAuthHost() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthHost, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=309)
    public Map<String,Object> unsetMtaAuthHost(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthHost, "");
        return attrs;
    }

    /**
     * whether this server is a mta auth target
     *
     * @return zimbraMtaAuthTarget, or false if unset
     */
    @ZAttr(id=505)
    public boolean isMtaAuthTarget() {
        return getBooleanAttr(Provisioning.A_zimbraMtaAuthTarget, false);
    }

    /**
     * whether this server is a mta auth target
     *
     * @param zimbraMtaAuthTarget new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=505)
    public void setMtaAuthTarget(boolean zimbraMtaAuthTarget) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthTarget, zimbraMtaAuthTarget ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether this server is a mta auth target
     *
     * @param zimbraMtaAuthTarget new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=505)
    public Map<String,Object> setMtaAuthTarget(boolean zimbraMtaAuthTarget, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthTarget, zimbraMtaAuthTarget ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether this server is a mta auth target
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=505)
    public void unsetMtaAuthTarget() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthTarget, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether this server is a mta auth target
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=505)
    public Map<String,Object> unsetMtaAuthTarget(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthTarget, "");
        return attrs;
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @return zimbraMtaAuthURL, or ampty array if unset
     */
    @ZAttr(id=310)
    public String[] getMtaAuthURL() {
        return getMultiAttr(Provisioning.A_zimbraMtaAuthURL);
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param zimbraMtaAuthURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=310)
    public void setMtaAuthURL(String[] zimbraMtaAuthURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthURL, zimbraMtaAuthURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param zimbraMtaAuthURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=310)
    public Map<String,Object> setMtaAuthURL(String[] zimbraMtaAuthURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthURL, zimbraMtaAuthURL);
        return attrs;
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param zimbraMtaAuthURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=310)
    public void addMtaAuthURL(String zimbraMtaAuthURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaAuthURL, zimbraMtaAuthURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param zimbraMtaAuthURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=310)
    public Map<String,Object> addMtaAuthURL(String zimbraMtaAuthURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaAuthURL, zimbraMtaAuthURL);
        return attrs;
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param zimbraMtaAuthURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=310)
    public void removeMtaAuthURL(String zimbraMtaAuthURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaAuthURL, zimbraMtaAuthURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param zimbraMtaAuthURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=310)
    public Map<String,Object> removeMtaAuthURL(String zimbraMtaAuthURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaAuthURL, zimbraMtaAuthURL);
        return attrs;
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=310)
    public void unsetMtaAuthURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=310)
    public Map<String,Object> unsetMtaAuthURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthURL, "");
        return attrs;
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @return zimbraMtaBlockedExtension, or ampty array if unset
     */
    @ZAttr(id=195)
    public String[] getMtaBlockedExtension() {
        return getMultiAttr(Provisioning.A_zimbraMtaBlockedExtension);
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param zimbraMtaBlockedExtension new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=195)
    public void setMtaBlockedExtension(String[] zimbraMtaBlockedExtension) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaBlockedExtension, zimbraMtaBlockedExtension);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param zimbraMtaBlockedExtension new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=195)
    public Map<String,Object> setMtaBlockedExtension(String[] zimbraMtaBlockedExtension, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaBlockedExtension, zimbraMtaBlockedExtension);
        return attrs;
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param zimbraMtaBlockedExtension new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=195)
    public void addMtaBlockedExtension(String zimbraMtaBlockedExtension) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaBlockedExtension, zimbraMtaBlockedExtension);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param zimbraMtaBlockedExtension new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=195)
    public Map<String,Object> addMtaBlockedExtension(String zimbraMtaBlockedExtension, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaBlockedExtension, zimbraMtaBlockedExtension);
        return attrs;
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param zimbraMtaBlockedExtension existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=195)
    public void removeMtaBlockedExtension(String zimbraMtaBlockedExtension) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaBlockedExtension, zimbraMtaBlockedExtension);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param zimbraMtaBlockedExtension existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=195)
    public Map<String,Object> removeMtaBlockedExtension(String zimbraMtaBlockedExtension, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaBlockedExtension, zimbraMtaBlockedExtension);
        return attrs;
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=195)
    public void unsetMtaBlockedExtension() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaBlockedExtension, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Attachment file extensions that are blocked
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=195)
    public Map<String,Object> unsetMtaBlockedExtension(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaBlockedExtension, "");
        return attrs;
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @return zimbraMtaCommonBlockedExtension, or ampty array if unset
     */
    @ZAttr(id=196)
    public String[] getMtaCommonBlockedExtension() {
        String[] value = getMultiAttr(Provisioning.A_zimbraMtaCommonBlockedExtension); return value.length > 0 ? value : new String[] {"asd","bat","chm","cmd","com","dll","do","exe","hlp","hta","js","jse","lnk","mov","ocx","pif","reg","rm","scr","shb","shm","shs","vbe","vbs","vbx","vxd","wav","wmf","wsf","wsh","xl"};
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param zimbraMtaCommonBlockedExtension new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=196)
    public void setMtaCommonBlockedExtension(String[] zimbraMtaCommonBlockedExtension) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaCommonBlockedExtension, zimbraMtaCommonBlockedExtension);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param zimbraMtaCommonBlockedExtension new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=196)
    public Map<String,Object> setMtaCommonBlockedExtension(String[] zimbraMtaCommonBlockedExtension, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaCommonBlockedExtension, zimbraMtaCommonBlockedExtension);
        return attrs;
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param zimbraMtaCommonBlockedExtension new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=196)
    public void addMtaCommonBlockedExtension(String zimbraMtaCommonBlockedExtension) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaCommonBlockedExtension, zimbraMtaCommonBlockedExtension);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param zimbraMtaCommonBlockedExtension new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=196)
    public Map<String,Object> addMtaCommonBlockedExtension(String zimbraMtaCommonBlockedExtension, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaCommonBlockedExtension, zimbraMtaCommonBlockedExtension);
        return attrs;
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param zimbraMtaCommonBlockedExtension existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=196)
    public void removeMtaCommonBlockedExtension(String zimbraMtaCommonBlockedExtension) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaCommonBlockedExtension, zimbraMtaCommonBlockedExtension);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param zimbraMtaCommonBlockedExtension existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=196)
    public Map<String,Object> removeMtaCommonBlockedExtension(String zimbraMtaCommonBlockedExtension, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaCommonBlockedExtension, zimbraMtaCommonBlockedExtension);
        return attrs;
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=196)
    public void unsetMtaCommonBlockedExtension() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaCommonBlockedExtension, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=196)
    public Map<String,Object> unsetMtaCommonBlockedExtension(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaCommonBlockedExtension, "");
        return attrs;
    }

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     *
     * @return zimbraMtaDnsLookupsEnabled, or true if unset
     */
    @ZAttr(id=197)
    public boolean isMtaDnsLookupsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMtaDnsLookupsEnabled, true);
    }

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     *
     * @param zimbraMtaDnsLookupsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=197)
    public void setMtaDnsLookupsEnabled(boolean zimbraMtaDnsLookupsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaDnsLookupsEnabled, zimbraMtaDnsLookupsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     *
     * @param zimbraMtaDnsLookupsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=197)
    public Map<String,Object> setMtaDnsLookupsEnabled(boolean zimbraMtaDnsLookupsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaDnsLookupsEnabled, zimbraMtaDnsLookupsEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=197)
    public void unsetMtaDnsLookupsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaDnsLookupsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=197)
    public Map<String,Object> unsetMtaDnsLookupsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaDnsLookupsEnabled, "");
        return attrs;
    }

    /**
     * Value for postconf message_size_limit
     *
     * @return zimbraMtaMaxMessageSize, or 10240000 if unset
     */
    @ZAttr(id=198)
    public int getMtaMaxMessageSize() {
        return getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, 10240000);
    }

    /**
     * Value for postconf message_size_limit
     *
     * @param zimbraMtaMaxMessageSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=198)
    public void setMtaMaxMessageSize(int zimbraMtaMaxMessageSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMaxMessageSize, Integer.toString(zimbraMtaMaxMessageSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf message_size_limit
     *
     * @param zimbraMtaMaxMessageSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=198)
    public Map<String,Object> setMtaMaxMessageSize(int zimbraMtaMaxMessageSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMaxMessageSize, Integer.toString(zimbraMtaMaxMessageSize));
        return attrs;
    }

    /**
     * Value for postconf message_size_limit
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=198)
    public void unsetMtaMaxMessageSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMaxMessageSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf message_size_limit
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=198)
    public Map<String,Object> unsetMtaMaxMessageSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMaxMessageSize, "");
        return attrs;
    }

    /**
     * value of postfix mydestination
     *
     * @return zimbraMtaMyDestination, or "localhost" if unset
     */
    @ZAttr(id=524)
    public String getMtaMyDestination() {
        return getAttr(Provisioning.A_zimbraMtaMyDestination, "localhost");
    }

    /**
     * value of postfix mydestination
     *
     * @param zimbraMtaMyDestination new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=524)
    public void setMtaMyDestination(String zimbraMtaMyDestination) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyDestination, zimbraMtaMyDestination);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix mydestination
     *
     * @param zimbraMtaMyDestination new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=524)
    public Map<String,Object> setMtaMyDestination(String zimbraMtaMyDestination, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyDestination, zimbraMtaMyDestination);
        return attrs;
    }

    /**
     * value of postfix mydestination
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=524)
    public void unsetMtaMyDestination() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyDestination, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix mydestination
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=524)
    public Map<String,Object> unsetMtaMyDestination(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyDestination, "");
        return attrs;
    }

    /**
     * value of postfix myhostname
     *
     * @return zimbraMtaMyHostname, or null if unset
     */
    @ZAttr(id=509)
    public String getMtaMyHostname() {
        return getAttr(Provisioning.A_zimbraMtaMyHostname, null);
    }

    /**
     * value of postfix myhostname
     *
     * @param zimbraMtaMyHostname new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=509)
    public void setMtaMyHostname(String zimbraMtaMyHostname) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyHostname, zimbraMtaMyHostname);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix myhostname
     *
     * @param zimbraMtaMyHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=509)
    public Map<String,Object> setMtaMyHostname(String zimbraMtaMyHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyHostname, zimbraMtaMyHostname);
        return attrs;
    }

    /**
     * value of postfix myhostname
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=509)
    public void unsetMtaMyHostname() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyHostname, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix myhostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=509)
    public Map<String,Object> unsetMtaMyHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyHostname, "");
        return attrs;
    }

    /**
     * value of postfix mynetworks
     *
     * @return zimbraMtaMyNetworks, or ampty array if unset
     */
    @ZAttr(id=311)
    public String[] getMtaMyNetworks() {
        return getMultiAttr(Provisioning.A_zimbraMtaMyNetworks);
    }

    /**
     * value of postfix mynetworks
     *
     * @param zimbraMtaMyNetworks new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=311)
    public void setMtaMyNetworks(String[] zimbraMtaMyNetworks) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyNetworks, zimbraMtaMyNetworks);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix mynetworks
     *
     * @param zimbraMtaMyNetworks new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=311)
    public Map<String,Object> setMtaMyNetworks(String[] zimbraMtaMyNetworks, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyNetworks, zimbraMtaMyNetworks);
        return attrs;
    }

    /**
     * value of postfix mynetworks
     *
     * @param zimbraMtaMyNetworks new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=311)
    public void addMtaMyNetworks(String zimbraMtaMyNetworks) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaMyNetworks, zimbraMtaMyNetworks);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix mynetworks
     *
     * @param zimbraMtaMyNetworks new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=311)
    public Map<String,Object> addMtaMyNetworks(String zimbraMtaMyNetworks, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaMyNetworks, zimbraMtaMyNetworks);
        return attrs;
    }

    /**
     * value of postfix mynetworks
     *
     * @param zimbraMtaMyNetworks existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=311)
    public void removeMtaMyNetworks(String zimbraMtaMyNetworks) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaMyNetworks, zimbraMtaMyNetworks);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix mynetworks
     *
     * @param zimbraMtaMyNetworks existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=311)
    public Map<String,Object> removeMtaMyNetworks(String zimbraMtaMyNetworks, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaMyNetworks, zimbraMtaMyNetworks);
        return attrs;
    }

    /**
     * value of postfix mynetworks
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=311)
    public void unsetMtaMyNetworks() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyNetworks, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix mynetworks
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=311)
    public Map<String,Object> unsetMtaMyNetworks(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyNetworks, "");
        return attrs;
    }

    /**
     * value of postfix myorigin
     *
     * @return zimbraMtaMyOrigin, or null if unset
     */
    @ZAttr(id=510)
    public String getMtaMyOrigin() {
        return getAttr(Provisioning.A_zimbraMtaMyOrigin, null);
    }

    /**
     * value of postfix myorigin
     *
     * @param zimbraMtaMyOrigin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=510)
    public void setMtaMyOrigin(String zimbraMtaMyOrigin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyOrigin, zimbraMtaMyOrigin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix myorigin
     *
     * @param zimbraMtaMyOrigin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=510)
    public Map<String,Object> setMtaMyOrigin(String zimbraMtaMyOrigin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyOrigin, zimbraMtaMyOrigin);
        return attrs;
    }

    /**
     * value of postfix myorigin
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=510)
    public void unsetMtaMyOrigin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyOrigin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value of postfix myorigin
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=510)
    public Map<String,Object> unsetMtaMyOrigin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaMyOrigin, "");
        return attrs;
    }

    /**
     * value for postfix non_smtpd_milters
     *
     * @return zimbraMtaNonSmtpdMilters, or null if unset
     */
    @ZAttr(id=673)
    public String getMtaNonSmtpdMilters() {
        return getAttr(Provisioning.A_zimbraMtaNonSmtpdMilters, null);
    }

    /**
     * value for postfix non_smtpd_milters
     *
     * @param zimbraMtaNonSmtpdMilters new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=673)
    public void setMtaNonSmtpdMilters(String zimbraMtaNonSmtpdMilters) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaNonSmtpdMilters, zimbraMtaNonSmtpdMilters);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value for postfix non_smtpd_milters
     *
     * @param zimbraMtaNonSmtpdMilters new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=673)
    public Map<String,Object> setMtaNonSmtpdMilters(String zimbraMtaNonSmtpdMilters, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaNonSmtpdMilters, zimbraMtaNonSmtpdMilters);
        return attrs;
    }

    /**
     * value for postfix non_smtpd_milters
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=673)
    public void unsetMtaNonSmtpdMilters() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaNonSmtpdMilters, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value for postfix non_smtpd_milters
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=673)
    public Map<String,Object> unsetMtaNonSmtpdMilters(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaNonSmtpdMilters, "");
        return attrs;
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @return zimbraMtaRecipientDelimiter, or ampty array if unset
     */
    @ZAttr(id=306)
    public String[] getMtaRecipientDelimiter() {
        return getMultiAttr(Provisioning.A_zimbraMtaRecipientDelimiter);
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param zimbraMtaRecipientDelimiter new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=306)
    public void setMtaRecipientDelimiter(String[] zimbraMtaRecipientDelimiter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRecipientDelimiter, zimbraMtaRecipientDelimiter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param zimbraMtaRecipientDelimiter new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=306)
    public Map<String,Object> setMtaRecipientDelimiter(String[] zimbraMtaRecipientDelimiter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRecipientDelimiter, zimbraMtaRecipientDelimiter);
        return attrs;
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param zimbraMtaRecipientDelimiter new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=306)
    public void addMtaRecipientDelimiter(String zimbraMtaRecipientDelimiter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaRecipientDelimiter, zimbraMtaRecipientDelimiter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param zimbraMtaRecipientDelimiter new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=306)
    public Map<String,Object> addMtaRecipientDelimiter(String zimbraMtaRecipientDelimiter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaRecipientDelimiter, zimbraMtaRecipientDelimiter);
        return attrs;
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param zimbraMtaRecipientDelimiter existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=306)
    public void removeMtaRecipientDelimiter(String zimbraMtaRecipientDelimiter) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaRecipientDelimiter, zimbraMtaRecipientDelimiter);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param zimbraMtaRecipientDelimiter existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=306)
    public Map<String,Object> removeMtaRecipientDelimiter(String zimbraMtaRecipientDelimiter, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaRecipientDelimiter, zimbraMtaRecipientDelimiter);
        return attrs;
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=306)
    public void unsetMtaRecipientDelimiter() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRecipientDelimiter, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=306)
    public Map<String,Object> unsetMtaRecipientDelimiter(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRecipientDelimiter, "");
        return attrs;
    }

    /**
     * Value for postconf relayhost
     *
     * @return zimbraMtaRelayHost, or ampty array if unset
     */
    @ZAttr(id=199)
    public String[] getMtaRelayHost() {
        return getMultiAttr(Provisioning.A_zimbraMtaRelayHost);
    }

    /**
     * Value for postconf relayhost
     *
     * @param zimbraMtaRelayHost new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=199)
    public void setMtaRelayHost(String[] zimbraMtaRelayHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRelayHost, zimbraMtaRelayHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf relayhost
     *
     * @param zimbraMtaRelayHost new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=199)
    public Map<String,Object> setMtaRelayHost(String[] zimbraMtaRelayHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRelayHost, zimbraMtaRelayHost);
        return attrs;
    }

    /**
     * Value for postconf relayhost
     *
     * @param zimbraMtaRelayHost new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=199)
    public void addMtaRelayHost(String zimbraMtaRelayHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaRelayHost, zimbraMtaRelayHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf relayhost
     *
     * @param zimbraMtaRelayHost new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=199)
    public Map<String,Object> addMtaRelayHost(String zimbraMtaRelayHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaRelayHost, zimbraMtaRelayHost);
        return attrs;
    }

    /**
     * Value for postconf relayhost
     *
     * @param zimbraMtaRelayHost existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=199)
    public void removeMtaRelayHost(String zimbraMtaRelayHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaRelayHost, zimbraMtaRelayHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf relayhost
     *
     * @param zimbraMtaRelayHost existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=199)
    public Map<String,Object> removeMtaRelayHost(String zimbraMtaRelayHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaRelayHost, zimbraMtaRelayHost);
        return attrs;
    }

    /**
     * Value for postconf relayhost
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=199)
    public void unsetMtaRelayHost() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRelayHost, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf relayhost
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=199)
    public Map<String,Object> unsetMtaRelayHost(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRelayHost, "");
        return attrs;
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @return zimbraMtaRestriction, or ampty array if unset
     */
    @ZAttr(id=226)
    public String[] getMtaRestriction() {
        String[] value = getMultiAttr(Provisioning.A_zimbraMtaRestriction); return value.length > 0 ? value : new String[] {"reject_invalid_hostname","reject_non_fqdn_sender"};
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param zimbraMtaRestriction new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=226)
    public void setMtaRestriction(String[] zimbraMtaRestriction) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRestriction, zimbraMtaRestriction);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param zimbraMtaRestriction new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=226)
    public Map<String,Object> setMtaRestriction(String[] zimbraMtaRestriction, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRestriction, zimbraMtaRestriction);
        return attrs;
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param zimbraMtaRestriction new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=226)
    public void addMtaRestriction(String zimbraMtaRestriction) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaRestriction, zimbraMtaRestriction);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param zimbraMtaRestriction new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=226)
    public Map<String,Object> addMtaRestriction(String zimbraMtaRestriction, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMtaRestriction, zimbraMtaRestriction);
        return attrs;
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param zimbraMtaRestriction existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=226)
    public void removeMtaRestriction(String zimbraMtaRestriction) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaRestriction, zimbraMtaRestriction);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param zimbraMtaRestriction existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=226)
    public Map<String,Object> removeMtaRestriction(String zimbraMtaRestriction, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMtaRestriction, zimbraMtaRestriction);
        return attrs;
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=226)
    public void unsetMtaRestriction() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRestriction, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * restrictions to reject some suspect SMTP clients
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=226)
    public Map<String,Object> unsetMtaRestriction(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaRestriction, "");
        return attrs;
    }

    /**
     * value for postfix smtpd_milters
     *
     * @return zimbraMtaSmtpdMilters, or null if unset
     */
    @ZAttr(id=672)
    public String getMtaSmtpdMilters() {
        return getAttr(Provisioning.A_zimbraMtaSmtpdMilters, null);
    }

    /**
     * value for postfix smtpd_milters
     *
     * @param zimbraMtaSmtpdMilters new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=672)
    public void setMtaSmtpdMilters(String zimbraMtaSmtpdMilters) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaSmtpdMilters, zimbraMtaSmtpdMilters);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value for postfix smtpd_milters
     *
     * @param zimbraMtaSmtpdMilters new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=672)
    public Map<String,Object> setMtaSmtpdMilters(String zimbraMtaSmtpdMilters, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaSmtpdMilters, zimbraMtaSmtpdMilters);
        return attrs;
    }

    /**
     * value for postfix smtpd_milters
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=672)
    public void unsetMtaSmtpdMilters() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaSmtpdMilters, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * value for postfix smtpd_milters
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=672)
    public Map<String,Object> unsetMtaSmtpdMilters(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaSmtpdMilters, "");
        return attrs;
    }

    /**
     * Value for postconf smtpd_tls_auth_only
     *
     * @return zimbraMtaTlsAuthOnly, or true if unset
     */
    @ZAttr(id=200)
    public boolean isMtaTlsAuthOnly() {
        return getBooleanAttr(Provisioning.A_zimbraMtaTlsAuthOnly, true);
    }

    /**
     * Value for postconf smtpd_tls_auth_only
     *
     * @param zimbraMtaTlsAuthOnly new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=200)
    public void setMtaTlsAuthOnly(boolean zimbraMtaTlsAuthOnly) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, zimbraMtaTlsAuthOnly ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf smtpd_tls_auth_only
     *
     * @param zimbraMtaTlsAuthOnly new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=200)
    public Map<String,Object> setMtaTlsAuthOnly(boolean zimbraMtaTlsAuthOnly, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, zimbraMtaTlsAuthOnly ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Value for postconf smtpd_tls_auth_only
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=200)
    public void unsetMtaTlsAuthOnly() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Value for postconf smtpd_tls_auth_only
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=200)
    public Map<String,Object> unsetMtaTlsAuthOnly(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, "");
        return attrs;
    }

    /**
     * A signed activation key that authorizes this installation.
     *
     * @return zimbraNetworkActivation, or null if unset
     */
    @ZAttr(id=375)
    public String getNetworkActivation() {
        return getAttr(Provisioning.A_zimbraNetworkActivation, null);
    }

    /**
     * A signed activation key that authorizes this installation.
     *
     * @param zimbraNetworkActivation new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=375)
    public void setNetworkActivation(String zimbraNetworkActivation) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkActivation, zimbraNetworkActivation);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * A signed activation key that authorizes this installation.
     *
     * @param zimbraNetworkActivation new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=375)
    public Map<String,Object> setNetworkActivation(String zimbraNetworkActivation, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkActivation, zimbraNetworkActivation);
        return attrs;
    }

    /**
     * A signed activation key that authorizes this installation.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=375)
    public void unsetNetworkActivation() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkActivation, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * A signed activation key that authorizes this installation.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=375)
    public Map<String,Object> unsetNetworkActivation(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkActivation, "");
        return attrs;
    }

    /**
     * Contents of a signed Zimbra license key - an XML string.
     *
     * @return zimbraNetworkLicense, or null if unset
     */
    @ZAttr(id=374)
    public String getNetworkLicense() {
        return getAttr(Provisioning.A_zimbraNetworkLicense, null);
    }

    /**
     * Contents of a signed Zimbra license key - an XML string.
     *
     * @param zimbraNetworkLicense new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=374)
    public void setNetworkLicense(String zimbraNetworkLicense) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkLicense, zimbraNetworkLicense);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Contents of a signed Zimbra license key - an XML string.
     *
     * @param zimbraNetworkLicense new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=374)
    public Map<String,Object> setNetworkLicense(String zimbraNetworkLicense, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkLicense, zimbraNetworkLicense);
        return attrs;
    }

    /**
     * Contents of a signed Zimbra license key - an XML string.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=374)
    public void unsetNetworkLicense() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkLicense, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Contents of a signed Zimbra license key - an XML string.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=374)
    public Map<String,Object> unsetNetworkLicense(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNetworkLicense, "");
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
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @return zimbraNotebookFolderCacheSize, or 1024 if unset
     */
    @ZAttr(id=370)
    public int getNotebookFolderCacheSize() {
        return getIntAttr(Provisioning.A_zimbraNotebookFolderCacheSize, 1024);
    }

    /**
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @param zimbraNotebookFolderCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=370)
    public void setNotebookFolderCacheSize(int zimbraNotebookFolderCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookFolderCacheSize, Integer.toString(zimbraNotebookFolderCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @param zimbraNotebookFolderCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=370)
    public Map<String,Object> setNotebookFolderCacheSize(int zimbraNotebookFolderCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookFolderCacheSize, Integer.toString(zimbraNotebookFolderCacheSize));
        return attrs;
    }

    /**
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=370)
    public void unsetNotebookFolderCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookFolderCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=370)
    public Map<String,Object> unsetNotebookFolderCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookFolderCacheSize, "");
        return attrs;
    }

    /**
     * The maximum number of cached templates in each Wiki / Notebook folder
     * cache.
     *
     * @return zimbraNotebookMaxCachedTemplatesPerFolder, or 256 if unset
     */
    @ZAttr(id=371)
    public int getNotebookMaxCachedTemplatesPerFolder() {
        return getIntAttr(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, 256);
    }

    /**
     * The maximum number of cached templates in each Wiki / Notebook folder
     * cache.
     *
     * @param zimbraNotebookMaxCachedTemplatesPerFolder new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=371)
    public void setNotebookMaxCachedTemplatesPerFolder(int zimbraNotebookMaxCachedTemplatesPerFolder) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, Integer.toString(zimbraNotebookMaxCachedTemplatesPerFolder));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of cached templates in each Wiki / Notebook folder
     * cache.
     *
     * @param zimbraNotebookMaxCachedTemplatesPerFolder new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=371)
    public Map<String,Object> setNotebookMaxCachedTemplatesPerFolder(int zimbraNotebookMaxCachedTemplatesPerFolder, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, Integer.toString(zimbraNotebookMaxCachedTemplatesPerFolder));
        return attrs;
    }

    /**
     * The maximum number of cached templates in each Wiki / Notebook folder
     * cache.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=371)
    public void unsetNotebookMaxCachedTemplatesPerFolder() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of cached templates in each Wiki / Notebook folder
     * cache.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=371)
    public Map<String,Object> unsetNotebookMaxCachedTemplatesPerFolder(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, "");
        return attrs;
    }

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     *
     * @return zimbraNotebookPageCacheSize, or 10240 if unset
     */
    @ZAttr(id=369)
    public int getNotebookPageCacheSize() {
        return getIntAttr(Provisioning.A_zimbraNotebookPageCacheSize, 10240);
    }

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     *
     * @param zimbraNotebookPageCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=369)
    public void setNotebookPageCacheSize(int zimbraNotebookPageCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookPageCacheSize, Integer.toString(zimbraNotebookPageCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     *
     * @param zimbraNotebookPageCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=369)
    public Map<String,Object> setNotebookPageCacheSize(int zimbraNotebookPageCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookPageCacheSize, Integer.toString(zimbraNotebookPageCacheSize));
        return attrs;
    }

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=369)
    public void unsetNotebookPageCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookPageCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=369)
    public Map<String,Object> unsetNotebookPageCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookPageCacheSize, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @return zimbraNotifyBindAddress, or ampty array if unset
     */
    @ZAttr(id=317)
    public String[] getNotifyBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraNotifyBindAddress);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param zimbraNotifyBindAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=317)
    public void setNotifyBindAddress(String[] zimbraNotifyBindAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindAddress, zimbraNotifyBindAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param zimbraNotifyBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=317)
    public Map<String,Object> setNotifyBindAddress(String[] zimbraNotifyBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindAddress, zimbraNotifyBindAddress);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param zimbraNotifyBindAddress new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=317)
    public void addNotifyBindAddress(String zimbraNotifyBindAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraNotifyBindAddress, zimbraNotifyBindAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param zimbraNotifyBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=317)
    public Map<String,Object> addNotifyBindAddress(String zimbraNotifyBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraNotifyBindAddress, zimbraNotifyBindAddress);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param zimbraNotifyBindAddress existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=317)
    public void removeNotifyBindAddress(String zimbraNotifyBindAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraNotifyBindAddress, zimbraNotifyBindAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param zimbraNotifyBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=317)
    public Map<String,Object> removeNotifyBindAddress(String zimbraNotifyBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraNotifyBindAddress, zimbraNotifyBindAddress);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=317)
    public void unsetNotifyBindAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=317)
    public Map<String,Object> unsetNotifyBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindAddress, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @return zimbraNotifyBindPort, or 7035 if unset
     */
    @ZAttr(id=318)
    public int getNotifyBindPort() {
        return getIntAttr(Provisioning.A_zimbraNotifyBindPort, 7035);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @param zimbraNotifyBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=318)
    public void setNotifyBindPort(int zimbraNotifyBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindPort, Integer.toString(zimbraNotifyBindPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @param zimbraNotifyBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=318)
    public Map<String,Object> setNotifyBindPort(int zimbraNotifyBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindPort, Integer.toString(zimbraNotifyBindPort));
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=318)
    public void unsetNotifyBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=318)
    public Map<String,Object> unsetNotifyBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyBindPort, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @return zimbraNotifySSLBindAddress, or ampty array if unset
     */
    @ZAttr(id=320)
    public String[] getNotifySSLBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraNotifySSLBindAddress);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param zimbraNotifySSLBindAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=320)
    public void setNotifySSLBindAddress(String[] zimbraNotifySSLBindAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindAddress, zimbraNotifySSLBindAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param zimbraNotifySSLBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=320)
    public Map<String,Object> setNotifySSLBindAddress(String[] zimbraNotifySSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindAddress, zimbraNotifySSLBindAddress);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param zimbraNotifySSLBindAddress new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=320)
    public void addNotifySSLBindAddress(String zimbraNotifySSLBindAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraNotifySSLBindAddress, zimbraNotifySSLBindAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param zimbraNotifySSLBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=320)
    public Map<String,Object> addNotifySSLBindAddress(String zimbraNotifySSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraNotifySSLBindAddress, zimbraNotifySSLBindAddress);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param zimbraNotifySSLBindAddress existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=320)
    public void removeNotifySSLBindAddress(String zimbraNotifySSLBindAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraNotifySSLBindAddress, zimbraNotifySSLBindAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param zimbraNotifySSLBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=320)
    public Map<String,Object> removeNotifySSLBindAddress(String zimbraNotifySSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraNotifySSLBindAddress, zimbraNotifySSLBindAddress);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=320)
    public void unsetNotifySSLBindAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=320)
    public Map<String,Object> unsetNotifySSLBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindAddress, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @return zimbraNotifySSLBindPort, or 7036 if unset
     */
    @ZAttr(id=321)
    public int getNotifySSLBindPort() {
        return getIntAttr(Provisioning.A_zimbraNotifySSLBindPort, 7036);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @param zimbraNotifySSLBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=321)
    public void setNotifySSLBindPort(int zimbraNotifySSLBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindPort, Integer.toString(zimbraNotifySSLBindPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @param zimbraNotifySSLBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=321)
    public Map<String,Object> setNotifySSLBindPort(int zimbraNotifySSLBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindPort, Integer.toString(zimbraNotifySSLBindPort));
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=321)
    public void unsetNotifySSLBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=321)
    public Map<String,Object> unsetNotifySSLBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLBindPort, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     *
     * @return zimbraNotifySSLServerEnabled, or true if unset
     */
    @ZAttr(id=319)
    public boolean isNotifySSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraNotifySSLServerEnabled, true);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     *
     * @param zimbraNotifySSLServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=319)
    public void setNotifySSLServerEnabled(boolean zimbraNotifySSLServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLServerEnabled, zimbraNotifySSLServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     *
     * @param zimbraNotifySSLServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=319)
    public Map<String,Object> setNotifySSLServerEnabled(boolean zimbraNotifySSLServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLServerEnabled, zimbraNotifySSLServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=319)
    public void unsetNotifySSLServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=319)
    public Map<String,Object> unsetNotifySSLServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifySSLServerEnabled, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     *
     * @return zimbraNotifyServerEnabled, or true if unset
     */
    @ZAttr(id=316)
    public boolean isNotifyServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraNotifyServerEnabled, true);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     *
     * @param zimbraNotifyServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=316)
    public void setNotifyServerEnabled(boolean zimbraNotifyServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyServerEnabled, zimbraNotifyServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     *
     * @param zimbraNotifyServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=316)
    public Map<String,Object> setNotifyServerEnabled(boolean zimbraNotifyServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyServerEnabled, zimbraNotifyServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=316)
    public void unsetNotifyServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=316)
    public Map<String,Object> unsetNotifyServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotifyServerEnabled, "");
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
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraPop3BindOnStartup, or true if unset
     */
    @ZAttr(id=271)
    public boolean isPop3BindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraPop3BindOnStartup, true);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraPop3BindOnStartup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=271)
    public void setPop3BindOnStartup(boolean zimbraPop3BindOnStartup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindOnStartup, zimbraPop3BindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraPop3BindOnStartup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=271)
    public Map<String,Object> setPop3BindOnStartup(boolean zimbraPop3BindOnStartup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindOnStartup, zimbraPop3BindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=271)
    public void unsetPop3BindOnStartup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindOnStartup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=271)
    public Map<String,Object> unsetPop3BindOnStartup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindOnStartup, "");
        return attrs;
    }

    /**
     * port number on which POP3 server should listen
     *
     * @return zimbraPop3BindPort, or "7110" if unset
     */
    @ZAttr(id=94)
    public String getPop3BindPort() {
        return getAttr(Provisioning.A_zimbraPop3BindPort, "7110");
    }

    /**
     * port number on which POP3 server should listen
     *
     * @param zimbraPop3BindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=94)
    public void setPop3BindPort(String zimbraPop3BindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindPort, zimbraPop3BindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3 server should listen
     *
     * @param zimbraPop3BindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=94)
    public Map<String,Object> setPop3BindPort(String zimbraPop3BindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindPort, zimbraPop3BindPort);
        return attrs;
    }

    /**
     * port number on which POP3 server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=94)
    public void unsetPop3BindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3 server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=94)
    public Map<String,Object> unsetPop3BindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindPort, "");
        return attrs;
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @return zimbraPop3CleartextLoginEnabled, or false if unset
     */
    @ZAttr(id=189)
    public boolean isPop3CleartextLoginEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3CleartextLoginEnabled, false);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @param zimbraPop3CleartextLoginEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=189)
    public void setPop3CleartextLoginEnabled(boolean zimbraPop3CleartextLoginEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3CleartextLoginEnabled, zimbraPop3CleartextLoginEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @param zimbraPop3CleartextLoginEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=189)
    public Map<String,Object> setPop3CleartextLoginEnabled(boolean zimbraPop3CleartextLoginEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3CleartextLoginEnabled, zimbraPop3CleartextLoginEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=189)
    public void unsetPop3CleartextLoginEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3CleartextLoginEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=189)
    public Map<String,Object> unsetPop3CleartextLoginEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3CleartextLoginEnabled, "");
        return attrs;
    }

    /**
     * Whether to expose version on POP3 banner
     *
     * @return zimbraPop3ExposeVersionOnBanner, or false if unset
     */
    @ZAttr(id=692)
    public boolean isPop3ExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraPop3ExposeVersionOnBanner, false);
    }

    /**
     * Whether to expose version on POP3 banner
     *
     * @param zimbraPop3ExposeVersionOnBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=692)
    public void setPop3ExposeVersionOnBanner(boolean zimbraPop3ExposeVersionOnBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ExposeVersionOnBanner, zimbraPop3ExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on POP3 banner
     *
     * @param zimbraPop3ExposeVersionOnBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=692)
    public Map<String,Object> setPop3ExposeVersionOnBanner(boolean zimbraPop3ExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ExposeVersionOnBanner, zimbraPop3ExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to expose version on POP3 banner
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=692)
    public void unsetPop3ExposeVersionOnBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ExposeVersionOnBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on POP3 banner
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=692)
    public Map<String,Object> unsetPop3ExposeVersionOnBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ExposeVersionOnBanner, "");
        return attrs;
    }

    /**
     * number of handler threads
     *
     * @return zimbraPop3NumThreads, or 100 if unset
     */
    @ZAttr(id=96)
    public int getPop3NumThreads() {
        return getIntAttr(Provisioning.A_zimbraPop3NumThreads, 100);
    }

    /**
     * number of handler threads
     *
     * @param zimbraPop3NumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=96)
    public void setPop3NumThreads(int zimbraPop3NumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NumThreads, Integer.toString(zimbraPop3NumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of handler threads
     *
     * @param zimbraPop3NumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=96)
    public Map<String,Object> setPop3NumThreads(int zimbraPop3NumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NumThreads, Integer.toString(zimbraPop3NumThreads));
        return attrs;
    }

    /**
     * number of handler threads
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=96)
    public void unsetPop3NumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * number of handler threads
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=96)
    public Map<String,Object> unsetPop3NumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NumThreads, "");
        return attrs;
    }

    /**
     * port number on which POP3 proxy server should listen
     *
     * @return zimbraPop3ProxyBindPort, or "110" if unset
     */
    @ZAttr(id=350)
    public String getPop3ProxyBindPort() {
        return getAttr(Provisioning.A_zimbraPop3ProxyBindPort, "110");
    }

    /**
     * port number on which POP3 proxy server should listen
     *
     * @param zimbraPop3ProxyBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=350)
    public void setPop3ProxyBindPort(String zimbraPop3ProxyBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ProxyBindPort, zimbraPop3ProxyBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3 proxy server should listen
     *
     * @param zimbraPop3ProxyBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=350)
    public Map<String,Object> setPop3ProxyBindPort(String zimbraPop3ProxyBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ProxyBindPort, zimbraPop3ProxyBindPort);
        return attrs;
    }

    /**
     * port number on which POP3 proxy server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=350)
    public void unsetPop3ProxyBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ProxyBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3 proxy server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=350)
    public Map<String,Object> unsetPop3ProxyBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ProxyBindPort, "");
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraPop3SSLBindOnStartup, or true if unset
     */
    @ZAttr(id=272)
    public boolean isPop3SSLBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SSLBindOnStartup, true);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraPop3SSLBindOnStartup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=272)
    public void setPop3SSLBindOnStartup(boolean zimbraPop3SSLBindOnStartup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindOnStartup, zimbraPop3SSLBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param zimbraPop3SSLBindOnStartup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=272)
    public Map<String,Object> setPop3SSLBindOnStartup(boolean zimbraPop3SSLBindOnStartup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindOnStartup, zimbraPop3SSLBindOnStartup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=272)
    public void unsetPop3SSLBindOnStartup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindOnStartup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=272)
    public Map<String,Object> unsetPop3SSLBindOnStartup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindOnStartup, "");
        return attrs;
    }

    /**
     * port number on which POP3 server should listen
     *
     * @return zimbraPop3SSLBindPort, or "7995" if unset
     */
    @ZAttr(id=187)
    public String getPop3SSLBindPort() {
        return getAttr(Provisioning.A_zimbraPop3SSLBindPort, "7995");
    }

    /**
     * port number on which POP3 server should listen
     *
     * @param zimbraPop3SSLBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=187)
    public void setPop3SSLBindPort(String zimbraPop3SSLBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindPort, zimbraPop3SSLBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3 server should listen
     *
     * @param zimbraPop3SSLBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=187)
    public Map<String,Object> setPop3SSLBindPort(String zimbraPop3SSLBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindPort, zimbraPop3SSLBindPort);
        return attrs;
    }

    /**
     * port number on which POP3 server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=187)
    public void unsetPop3SSLBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3 server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=187)
    public Map<String,Object> unsetPop3SSLBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindPort, "");
        return attrs;
    }

    /**
     * port number on which POP3S proxy server should listen
     *
     * @return zimbraPop3SSLProxyBindPort, or "995" if unset
     */
    @ZAttr(id=351)
    public String getPop3SSLProxyBindPort() {
        return getAttr(Provisioning.A_zimbraPop3SSLProxyBindPort, "995");
    }

    /**
     * port number on which POP3S proxy server should listen
     *
     * @param zimbraPop3SSLProxyBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=351)
    public void setPop3SSLProxyBindPort(String zimbraPop3SSLProxyBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLProxyBindPort, zimbraPop3SSLProxyBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3S proxy server should listen
     *
     * @param zimbraPop3SSLProxyBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=351)
    public Map<String,Object> setPop3SSLProxyBindPort(String zimbraPop3SSLProxyBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLProxyBindPort, zimbraPop3SSLProxyBindPort);
        return attrs;
    }

    /**
     * port number on which POP3S proxy server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=351)
    public void unsetPop3SSLProxyBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLProxyBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * port number on which POP3S proxy server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=351)
    public Map<String,Object> unsetPop3SSLProxyBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLProxyBindPort, "");
        return attrs;
    }

    /**
     * whether POP3 SSL server is enabled for a server
     *
     * @return zimbraPop3SSLServerEnabled, or true if unset
     */
    @ZAttr(id=188)
    public boolean isPop3SSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SSLServerEnabled, true);
    }

    /**
     * whether POP3 SSL server is enabled for a server
     *
     * @param zimbraPop3SSLServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=188)
    public void setPop3SSLServerEnabled(boolean zimbraPop3SSLServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLServerEnabled, zimbraPop3SSLServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SSL server is enabled for a server
     *
     * @param zimbraPop3SSLServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=188)
    public Map<String,Object> setPop3SSLServerEnabled(boolean zimbraPop3SSLServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLServerEnabled, zimbraPop3SSLServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether POP3 SSL server is enabled for a server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=188)
    public void unsetPop3SSLServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SSL server is enabled for a server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=188)
    public Map<String,Object> unsetPop3SSLServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLServerEnabled, "");
        return attrs;
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @return zimbraPop3SaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=554)
    public boolean isPop3SaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SaslGssapiEnabled, false);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @param zimbraPop3SaslGssapiEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=554)
    public void setPop3SaslGssapiEnabled(boolean zimbraPop3SaslGssapiEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SaslGssapiEnabled, zimbraPop3SaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @param zimbraPop3SaslGssapiEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=554)
    public Map<String,Object> setPop3SaslGssapiEnabled(boolean zimbraPop3SaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SaslGssapiEnabled, zimbraPop3SaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=554)
    public void unsetPop3SaslGssapiEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SaslGssapiEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=554)
    public Map<String,Object> unsetPop3SaslGssapiEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SaslGssapiEnabled, "");
        return attrs;
    }

    /**
     * whether IMAP is enabled for a server
     *
     * @return zimbraPop3ServerEnabled, or true if unset
     */
    @ZAttr(id=177)
    public boolean isPop3ServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3ServerEnabled, true);
    }

    /**
     * whether IMAP is enabled for a server
     *
     * @param zimbraPop3ServerEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=177)
    public void setPop3ServerEnabled(boolean zimbraPop3ServerEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ServerEnabled, zimbraPop3ServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP is enabled for a server
     *
     * @param zimbraPop3ServerEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=177)
    public Map<String,Object> setPop3ServerEnabled(boolean zimbraPop3ServerEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ServerEnabled, zimbraPop3ServerEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether IMAP is enabled for a server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=177)
    public void unsetPop3ServerEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ServerEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP is enabled for a server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=177)
    public Map<String,Object> unsetPop3ServerEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ServerEnabled, "");
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
     * redolog rollover destination
     *
     * @return zimbraRedoLogArchiveDir, or "redolog/archive" if unset
     */
    @ZAttr(id=76)
    public String getRedoLogArchiveDir() {
        return getAttr(Provisioning.A_zimbraRedoLogArchiveDir, "redolog/archive");
    }

    /**
     * redolog rollover destination
     *
     * @param zimbraRedoLogArchiveDir new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=76)
    public void setRedoLogArchiveDir(String zimbraRedoLogArchiveDir) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogArchiveDir, zimbraRedoLogArchiveDir);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * redolog rollover destination
     *
     * @param zimbraRedoLogArchiveDir new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=76)
    public Map<String,Object> setRedoLogArchiveDir(String zimbraRedoLogArchiveDir, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogArchiveDir, zimbraRedoLogArchiveDir);
        return attrs;
    }

    /**
     * redolog rollover destination
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=76)
    public void unsetRedoLogArchiveDir() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogArchiveDir, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * redolog rollover destination
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=76)
    public Map<String,Object> unsetRedoLogArchiveDir(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogArchiveDir, "");
        return attrs;
    }

    /**
     * whether logs are delete on rollover or archived
     *
     * @return zimbraRedoLogDeleteOnRollover, or true if unset
     */
    @ZAttr(id=251)
    public boolean isRedoLogDeleteOnRollover() {
        return getBooleanAttr(Provisioning.A_zimbraRedoLogDeleteOnRollover, true);
    }

    /**
     * whether logs are delete on rollover or archived
     *
     * @param zimbraRedoLogDeleteOnRollover new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=251)
    public void setRedoLogDeleteOnRollover(boolean zimbraRedoLogDeleteOnRollover) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogDeleteOnRollover, zimbraRedoLogDeleteOnRollover ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether logs are delete on rollover or archived
     *
     * @param zimbraRedoLogDeleteOnRollover new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=251)
    public Map<String,Object> setRedoLogDeleteOnRollover(boolean zimbraRedoLogDeleteOnRollover, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogDeleteOnRollover, zimbraRedoLogDeleteOnRollover ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether logs are delete on rollover or archived
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=251)
    public void unsetRedoLogDeleteOnRollover() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogDeleteOnRollover, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether logs are delete on rollover or archived
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=251)
    public Map<String,Object> unsetRedoLogDeleteOnRollover(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogDeleteOnRollover, "");
        return attrs;
    }

    /**
     * whether redo logging is enabled
     *
     * @return zimbraRedoLogEnabled, or true if unset
     */
    @ZAttr(id=74)
    public boolean isRedoLogEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraRedoLogEnabled, true);
    }

    /**
     * whether redo logging is enabled
     *
     * @param zimbraRedoLogEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=74)
    public void setRedoLogEnabled(boolean zimbraRedoLogEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogEnabled, zimbraRedoLogEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether redo logging is enabled
     *
     * @param zimbraRedoLogEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=74)
    public Map<String,Object> setRedoLogEnabled(boolean zimbraRedoLogEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogEnabled, zimbraRedoLogEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether redo logging is enabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=74)
    public void unsetRedoLogEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether redo logging is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=74)
    public Map<String,Object> unsetRedoLogEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogEnabled, "");
        return attrs;
    }

    /**
     * how frequently writes to redo log get fsynced to disk
     *
     * @return zimbraRedoLogFsyncIntervalMS, or 10 if unset
     */
    @ZAttr(id=79)
    public int getRedoLogFsyncIntervalMS() {
        return getIntAttr(Provisioning.A_zimbraRedoLogFsyncIntervalMS, 10);
    }

    /**
     * how frequently writes to redo log get fsynced to disk
     *
     * @param zimbraRedoLogFsyncIntervalMS new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=79)
    public void setRedoLogFsyncIntervalMS(int zimbraRedoLogFsyncIntervalMS) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogFsyncIntervalMS, Integer.toString(zimbraRedoLogFsyncIntervalMS));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * how frequently writes to redo log get fsynced to disk
     *
     * @param zimbraRedoLogFsyncIntervalMS new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=79)
    public Map<String,Object> setRedoLogFsyncIntervalMS(int zimbraRedoLogFsyncIntervalMS, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogFsyncIntervalMS, Integer.toString(zimbraRedoLogFsyncIntervalMS));
        return attrs;
    }

    /**
     * how frequently writes to redo log get fsynced to disk
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=79)
    public void unsetRedoLogFsyncIntervalMS() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogFsyncIntervalMS, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * how frequently writes to redo log get fsynced to disk
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=79)
    public Map<String,Object> unsetRedoLogFsyncIntervalMS(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogFsyncIntervalMS, "");
        return attrs;
    }

    /**
     * name and location of the redolog file
     *
     * @return zimbraRedoLogLogPath, or "redolog/redo.log" if unset
     */
    @ZAttr(id=75)
    public String getRedoLogLogPath() {
        return getAttr(Provisioning.A_zimbraRedoLogLogPath, "redolog/redo.log");
    }

    /**
     * name and location of the redolog file
     *
     * @param zimbraRedoLogLogPath new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=75)
    public void setRedoLogLogPath(String zimbraRedoLogLogPath) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogLogPath, zimbraRedoLogLogPath);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * name and location of the redolog file
     *
     * @param zimbraRedoLogLogPath new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=75)
    public Map<String,Object> setRedoLogLogPath(String zimbraRedoLogLogPath, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogLogPath, zimbraRedoLogLogPath);
        return attrs;
    }

    /**
     * name and location of the redolog file
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=75)
    public void unsetRedoLogLogPath() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogLogPath, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * name and location of the redolog file
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=75)
    public Map<String,Object> unsetRedoLogLogPath(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogLogPath, "");
        return attrs;
    }

    /**
     * provider class name for redo logging
     *
     * @return zimbraRedoLogProvider, or ampty array if unset
     */
    @ZAttr(id=225)
    public String[] getRedoLogProvider() {
        return getMultiAttr(Provisioning.A_zimbraRedoLogProvider);
    }

    /**
     * provider class name for redo logging
     *
     * @param zimbraRedoLogProvider new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=225)
    public void setRedoLogProvider(String[] zimbraRedoLogProvider) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogProvider, zimbraRedoLogProvider);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * provider class name for redo logging
     *
     * @param zimbraRedoLogProvider new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=225)
    public Map<String,Object> setRedoLogProvider(String[] zimbraRedoLogProvider, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogProvider, zimbraRedoLogProvider);
        return attrs;
    }

    /**
     * provider class name for redo logging
     *
     * @param zimbraRedoLogProvider new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=225)
    public void addRedoLogProvider(String zimbraRedoLogProvider) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraRedoLogProvider, zimbraRedoLogProvider);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * provider class name for redo logging
     *
     * @param zimbraRedoLogProvider new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=225)
    public Map<String,Object> addRedoLogProvider(String zimbraRedoLogProvider, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraRedoLogProvider, zimbraRedoLogProvider);
        return attrs;
    }

    /**
     * provider class name for redo logging
     *
     * @param zimbraRedoLogProvider existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=225)
    public void removeRedoLogProvider(String zimbraRedoLogProvider) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraRedoLogProvider, zimbraRedoLogProvider);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * provider class name for redo logging
     *
     * @param zimbraRedoLogProvider existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=225)
    public Map<String,Object> removeRedoLogProvider(String zimbraRedoLogProvider, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraRedoLogProvider, zimbraRedoLogProvider);
        return attrs;
    }

    /**
     * provider class name for redo logging
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=225)
    public void unsetRedoLogProvider() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogProvider, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * provider class name for redo logging
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=225)
    public Map<String,Object> unsetRedoLogProvider(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogProvider, "");
        return attrs;
    }

    /**
     * redo.log file rolls over when it gets to this size
     *
     * @return zimbraRedoLogRolloverFileSizeKB, or 102400 if unset
     */
    @ZAttr(id=78)
    public int getRedoLogRolloverFileSizeKB() {
        return getIntAttr(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, 102400);
    }

    /**
     * redo.log file rolls over when it gets to this size
     *
     * @param zimbraRedoLogRolloverFileSizeKB new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=78)
    public void setRedoLogRolloverFileSizeKB(int zimbraRedoLogRolloverFileSizeKB) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, Integer.toString(zimbraRedoLogRolloverFileSizeKB));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * redo.log file rolls over when it gets to this size
     *
     * @param zimbraRedoLogRolloverFileSizeKB new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=78)
    public Map<String,Object> setRedoLogRolloverFileSizeKB(int zimbraRedoLogRolloverFileSizeKB, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, Integer.toString(zimbraRedoLogRolloverFileSizeKB));
        return attrs;
    }

    /**
     * redo.log file rolls over when it gets to this size
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=78)
    public void unsetRedoLogRolloverFileSizeKB() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * redo.log file rolls over when it gets to this size
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=78)
    public Map<String,Object> unsetRedoLogRolloverFileSizeKB(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, "");
        return attrs;
    }

    /**
     * Path to remote management command to execute on this server
     *
     * @return zimbraRemoteManagementCommand, or "/opt/zimbra/libexec/zmrcd" if unset
     */
    @ZAttr(id=336)
    public String getRemoteManagementCommand() {
        return getAttr(Provisioning.A_zimbraRemoteManagementCommand, "/opt/zimbra/libexec/zmrcd");
    }

    /**
     * Path to remote management command to execute on this server
     *
     * @param zimbraRemoteManagementCommand new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=336)
    public void setRemoteManagementCommand(String zimbraRemoteManagementCommand) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementCommand, zimbraRemoteManagementCommand);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Path to remote management command to execute on this server
     *
     * @param zimbraRemoteManagementCommand new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=336)
    public Map<String,Object> setRemoteManagementCommand(String zimbraRemoteManagementCommand, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementCommand, zimbraRemoteManagementCommand);
        return attrs;
    }

    /**
     * Path to remote management command to execute on this server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=336)
    public void unsetRemoteManagementCommand() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementCommand, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Path to remote management command to execute on this server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=336)
    public Map<String,Object> unsetRemoteManagementCommand(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementCommand, "");
        return attrs;
    }

    /**
     * Port on which remote management sshd listening on this server.
     *
     * @return zimbraRemoteManagementPort, or 22 if unset
     */
    @ZAttr(id=339)
    public int getRemoteManagementPort() {
        return getIntAttr(Provisioning.A_zimbraRemoteManagementPort, 22);
    }

    /**
     * Port on which remote management sshd listening on this server.
     *
     * @param zimbraRemoteManagementPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=339)
    public void setRemoteManagementPort(int zimbraRemoteManagementPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPort, Integer.toString(zimbraRemoteManagementPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Port on which remote management sshd listening on this server.
     *
     * @param zimbraRemoteManagementPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=339)
    public Map<String,Object> setRemoteManagementPort(int zimbraRemoteManagementPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPort, Integer.toString(zimbraRemoteManagementPort));
        return attrs;
    }

    /**
     * Port on which remote management sshd listening on this server.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=339)
    public void unsetRemoteManagementPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Port on which remote management sshd listening on this server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=339)
    public Map<String,Object> unsetRemoteManagementPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPort, "");
        return attrs;
    }

    /**
     * Private key this server should use to access another server
     *
     * @return zimbraRemoteManagementPrivateKeyPath, or "/opt/zimbra/.ssh/zimbra_identity" if unset
     */
    @ZAttr(id=338)
    public String getRemoteManagementPrivateKeyPath() {
        return getAttr(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, "/opt/zimbra/.ssh/zimbra_identity");
    }

    /**
     * Private key this server should use to access another server
     *
     * @param zimbraRemoteManagementPrivateKeyPath new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=338)
    public void setRemoteManagementPrivateKeyPath(String zimbraRemoteManagementPrivateKeyPath) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, zimbraRemoteManagementPrivateKeyPath);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Private key this server should use to access another server
     *
     * @param zimbraRemoteManagementPrivateKeyPath new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=338)
    public Map<String,Object> setRemoteManagementPrivateKeyPath(String zimbraRemoteManagementPrivateKeyPath, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, zimbraRemoteManagementPrivateKeyPath);
        return attrs;
    }

    /**
     * Private key this server should use to access another server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=338)
    public void unsetRemoteManagementPrivateKeyPath() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Private key this server should use to access another server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=338)
    public Map<String,Object> unsetRemoteManagementPrivateKeyPath(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, "");
        return attrs;
    }

    /**
     * Login name of user allowed to execute remote management command
     *
     * @return zimbraRemoteManagementUser, or "zimbra" if unset
     */
    @ZAttr(id=337)
    public String getRemoteManagementUser() {
        return getAttr(Provisioning.A_zimbraRemoteManagementUser, "zimbra");
    }

    /**
     * Login name of user allowed to execute remote management command
     *
     * @param zimbraRemoteManagementUser new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=337)
    public void setRemoteManagementUser(String zimbraRemoteManagementUser) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementUser, zimbraRemoteManagementUser);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Login name of user allowed to execute remote management command
     *
     * @param zimbraRemoteManagementUser new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=337)
    public Map<String,Object> setRemoteManagementUser(String zimbraRemoteManagementUser, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementUser, zimbraRemoteManagementUser);
        return attrs;
    }

    /**
     * Login name of user allowed to execute remote management command
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=337)
    public void unsetRemoteManagementUser() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementUser, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Login name of user allowed to execute remote management command
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=337)
    public Map<String,Object> unsetRemoteManagementUser(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRemoteManagementUser, "");
        return attrs;
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @return zimbraReverseProxyAdminIPAddress, or ampty array if unset
     */
    @ZAttr(id=697)
    public String[] getReverseProxyAdminIPAddress() {
        return getMultiAttr(Provisioning.A_zimbraReverseProxyAdminIPAddress);
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param zimbraReverseProxyAdminIPAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=697)
    public void setReverseProxyAdminIPAddress(String[] zimbraReverseProxyAdminIPAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminIPAddress, zimbraReverseProxyAdminIPAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param zimbraReverseProxyAdminIPAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=697)
    public Map<String,Object> setReverseProxyAdminIPAddress(String[] zimbraReverseProxyAdminIPAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminIPAddress, zimbraReverseProxyAdminIPAddress);
        return attrs;
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param zimbraReverseProxyAdminIPAddress new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=697)
    public void addReverseProxyAdminIPAddress(String zimbraReverseProxyAdminIPAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraReverseProxyAdminIPAddress, zimbraReverseProxyAdminIPAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param zimbraReverseProxyAdminIPAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=697)
    public Map<String,Object> addReverseProxyAdminIPAddress(String zimbraReverseProxyAdminIPAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraReverseProxyAdminIPAddress, zimbraReverseProxyAdminIPAddress);
        return attrs;
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param zimbraReverseProxyAdminIPAddress existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=697)
    public void removeReverseProxyAdminIPAddress(String zimbraReverseProxyAdminIPAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraReverseProxyAdminIPAddress, zimbraReverseProxyAdminIPAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param zimbraReverseProxyAdminIPAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=697)
    public Map<String,Object> removeReverseProxyAdminIPAddress(String zimbraReverseProxyAdminIPAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraReverseProxyAdminIPAddress, zimbraReverseProxyAdminIPAddress);
        return attrs;
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=697)
    public void unsetReverseProxyAdminIPAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminIPAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=697)
    public Map<String,Object> unsetReverseProxyAdminIPAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminIPAddress, "");
        return attrs;
    }

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @return zimbraReverseProxyAdminPortAttribute, or "zimbraAdminPort" if unset
     */
    @ZAttr(id=700)
    public String getReverseProxyAdminPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyAdminPortAttribute, "zimbraAdminPort");
    }

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @param zimbraReverseProxyAdminPortAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=700)
    public void setReverseProxyAdminPortAttribute(String zimbraReverseProxyAdminPortAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminPortAttribute, zimbraReverseProxyAdminPortAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @param zimbraReverseProxyAdminPortAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=700)
    public Map<String,Object> setReverseProxyAdminPortAttribute(String zimbraReverseProxyAdminPortAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminPortAttribute, zimbraReverseProxyAdminPortAttribute);
        return attrs;
    }

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=700)
    public void unsetReverseProxyAdminPortAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminPortAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=700)
    public Map<String,Object> unsetReverseProxyAdminPortAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAdminPortAttribute, "");
        return attrs;
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * <p>Use getReverseProxyAuthWaitIntervalAsString to access value as a string.
     *
     * @see #getReverseProxyAuthWaitIntervalAsString()
     *
     * @return zimbraReverseProxyAuthWaitInterval in millseconds, or 10000 (10s)  if unset
     */
    @ZAttr(id=569)
    public long getReverseProxyAuthWaitInterval() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyAuthWaitInterval, 10000L);
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @return zimbraReverseProxyAuthWaitInterval, or "10s" if unset
     */
    @ZAttr(id=569)
    public String getReverseProxyAuthWaitIntervalAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyAuthWaitInterval, "10s");
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @param zimbraReverseProxyAuthWaitInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=569)
    public void setReverseProxyAuthWaitInterval(String zimbraReverseProxyAuthWaitInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAuthWaitInterval, zimbraReverseProxyAuthWaitInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @param zimbraReverseProxyAuthWaitInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=569)
    public Map<String,Object> setReverseProxyAuthWaitInterval(String zimbraReverseProxyAuthWaitInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAuthWaitInterval, zimbraReverseProxyAuthWaitInterval);
        return attrs;
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=569)
    public void unsetReverseProxyAuthWaitInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAuthWaitInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=569)
    public Map<String,Object> unsetReverseProxyAuthWaitInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyAuthWaitInterval, "");
        return attrs;
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * <p>Use getReverseProxyCacheEntryTTLAsString to access value as a string.
     *
     * @see #getReverseProxyCacheEntryTTLAsString()
     *
     * @return zimbraReverseProxyCacheEntryTTL in millseconds, or 3600000 (1h)  if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public long getReverseProxyCacheEntryTTL() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheEntryTTL, 3600000L);
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @return zimbraReverseProxyCacheEntryTTL, or "1h" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public String getReverseProxyCacheEntryTTLAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheEntryTTL, "1h");
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @param zimbraReverseProxyCacheEntryTTL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public void setReverseProxyCacheEntryTTL(String zimbraReverseProxyCacheEntryTTL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheEntryTTL, zimbraReverseProxyCacheEntryTTL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @param zimbraReverseProxyCacheEntryTTL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public Map<String,Object> setReverseProxyCacheEntryTTL(String zimbraReverseProxyCacheEntryTTL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheEntryTTL, zimbraReverseProxyCacheEntryTTL);
        return attrs;
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public void unsetReverseProxyCacheEntryTTL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheEntryTTL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public Map<String,Object> unsetReverseProxyCacheEntryTTL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheEntryTTL, "");
        return attrs;
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * <p>Use getReverseProxyCacheFetchTimeoutAsString to access value as a string.
     *
     * @see #getReverseProxyCacheFetchTimeoutAsString()
     *
     * @return zimbraReverseProxyCacheFetchTimeout in millseconds, or 3000 (3s)  if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public long getReverseProxyCacheFetchTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, 3000L);
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @return zimbraReverseProxyCacheFetchTimeout, or "3s" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public String getReverseProxyCacheFetchTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, "3s");
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @param zimbraReverseProxyCacheFetchTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public void setReverseProxyCacheFetchTimeout(String zimbraReverseProxyCacheFetchTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, zimbraReverseProxyCacheFetchTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @param zimbraReverseProxyCacheFetchTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public Map<String,Object> setReverseProxyCacheFetchTimeout(String zimbraReverseProxyCacheFetchTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, zimbraReverseProxyCacheFetchTimeout);
        return attrs;
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public void unsetReverseProxyCacheFetchTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public Map<String,Object> unsetReverseProxyCacheFetchTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, "");
        return attrs;
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * <p>Use getReverseProxyCacheReconnectIntervalAsString to access value as a string.
     *
     * @see #getReverseProxyCacheReconnectIntervalAsString()
     *
     * @return zimbraReverseProxyCacheReconnectInterval in millseconds, or 60000 (1m)  if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public long getReverseProxyCacheReconnectInterval() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, 60000L);
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @return zimbraReverseProxyCacheReconnectInterval, or "1m" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public String getReverseProxyCacheReconnectIntervalAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, "1m");
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @param zimbraReverseProxyCacheReconnectInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public void setReverseProxyCacheReconnectInterval(String zimbraReverseProxyCacheReconnectInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, zimbraReverseProxyCacheReconnectInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @param zimbraReverseProxyCacheReconnectInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public Map<String,Object> setReverseProxyCacheReconnectInterval(String zimbraReverseProxyCacheReconnectInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, zimbraReverseProxyCacheReconnectInterval);
        return attrs;
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public void unsetReverseProxyCacheReconnectInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public Map<String,Object> unsetReverseProxyCacheReconnectInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, "");
        return attrs;
    }

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @return zimbraReverseProxyDefaultRealm, or null if unset
     */
    @ZAttr(id=703)
    public String getReverseProxyDefaultRealm() {
        return getAttr(Provisioning.A_zimbraReverseProxyDefaultRealm, null);
    }

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @param zimbraReverseProxyDefaultRealm new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=703)
    public void setReverseProxyDefaultRealm(String zimbraReverseProxyDefaultRealm) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDefaultRealm, zimbraReverseProxyDefaultRealm);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @param zimbraReverseProxyDefaultRealm new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=703)
    public Map<String,Object> setReverseProxyDefaultRealm(String zimbraReverseProxyDefaultRealm, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDefaultRealm, zimbraReverseProxyDefaultRealm);
        return attrs;
    }

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=703)
    public void unsetReverseProxyDefaultRealm() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDefaultRealm, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=703)
    public Map<String,Object> unsetReverseProxyDefaultRealm(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDefaultRealm, "");
        return attrs;
    }

    /**
     * LDAP attribute that contains domain name for the domain
     *
     * @return zimbraReverseProxyDomainNameAttribute, or "zimbraDomainName" if unset
     */
    @ZAttr(id=547)
    public String getReverseProxyDomainNameAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameAttribute, "zimbraDomainName");
    }

    /**
     * LDAP attribute that contains domain name for the domain
     *
     * @param zimbraReverseProxyDomainNameAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=547)
    public void setReverseProxyDomainNameAttribute(String zimbraReverseProxyDomainNameAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameAttribute, zimbraReverseProxyDomainNameAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute that contains domain name for the domain
     *
     * @param zimbraReverseProxyDomainNameAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=547)
    public Map<String,Object> setReverseProxyDomainNameAttribute(String zimbraReverseProxyDomainNameAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameAttribute, zimbraReverseProxyDomainNameAttribute);
        return attrs;
    }

    /**
     * LDAP attribute that contains domain name for the domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=547)
    public void unsetReverseProxyDomainNameAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute that contains domain name for the domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=547)
    public Map<String,Object> unsetReverseProxyDomainNameAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameAttribute, "");
        return attrs;
    }

    /**
     * LDAP query to find a domain
     *
     * @return zimbraReverseProxyDomainNameQuery, or "(&(zimbraVirtualIPAddress=${IPADDR})(objectClass=zimbraDomain))" if unset
     */
    @ZAttr(id=545)
    public String getReverseProxyDomainNameQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameQuery, "(&(zimbraVirtualIPAddress=${IPADDR})(objectClass=zimbraDomain))");
    }

    /**
     * LDAP query to find a domain
     *
     * @param zimbraReverseProxyDomainNameQuery new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=545)
    public void setReverseProxyDomainNameQuery(String zimbraReverseProxyDomainNameQuery) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameQuery, zimbraReverseProxyDomainNameQuery);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP query to find a domain
     *
     * @param zimbraReverseProxyDomainNameQuery new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=545)
    public Map<String,Object> setReverseProxyDomainNameQuery(String zimbraReverseProxyDomainNameQuery, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameQuery, zimbraReverseProxyDomainNameQuery);
        return attrs;
    }

    /**
     * LDAP query to find a domain
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=545)
    public void unsetReverseProxyDomainNameQuery() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameQuery, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP query to find a domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=545)
    public Map<String,Object> unsetReverseProxyDomainNameQuery(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameQuery, "");
        return attrs;
    }

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @return zimbraReverseProxyDomainNameSearchBase, or null if unset
     */
    @ZAttr(id=546)
    public String getReverseProxyDomainNameSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameSearchBase, null);
    }

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @param zimbraReverseProxyDomainNameSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=546)
    public void setReverseProxyDomainNameSearchBase(String zimbraReverseProxyDomainNameSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameSearchBase, zimbraReverseProxyDomainNameSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @param zimbraReverseProxyDomainNameSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=546)
    public Map<String,Object> setReverseProxyDomainNameSearchBase(String zimbraReverseProxyDomainNameSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameSearchBase, zimbraReverseProxyDomainNameSearchBase);
        return attrs;
    }

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=546)
    public void unsetReverseProxyDomainNameSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=546)
    public Map<String,Object> unsetReverseProxyDomainNameSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyDomainNameSearchBase, "");
        return attrs;
    }

    /**
     * Whether to enable HTTP proxy
     *
     * @return zimbraReverseProxyHttpEnabled, or false if unset
     */
    @ZAttr(id=628)
    public boolean isReverseProxyHttpEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyHttpEnabled, false);
    }

    /**
     * Whether to enable HTTP proxy
     *
     * @param zimbraReverseProxyHttpEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=628)
    public void setReverseProxyHttpEnabled(boolean zimbraReverseProxyHttpEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpEnabled, zimbraReverseProxyHttpEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to enable HTTP proxy
     *
     * @param zimbraReverseProxyHttpEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=628)
    public Map<String,Object> setReverseProxyHttpEnabled(boolean zimbraReverseProxyHttpEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpEnabled, zimbraReverseProxyHttpEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to enable HTTP proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=628)
    public void unsetReverseProxyHttpEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to enable HTTP proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=628)
    public Map<String,Object> unsetReverseProxyHttpEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpEnabled, "");
        return attrs;
    }

    /**
     * attribute that contains http bind port
     *
     * @return zimbraReverseProxyHttpPortAttribute, or "zimbraMailPort" if unset
     */
    @ZAttr(id=632)
    public String getReverseProxyHttpPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "zimbraMailPort");
    }

    /**
     * attribute that contains http bind port
     *
     * @param zimbraReverseProxyHttpPortAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=632)
    public void setReverseProxyHttpPortAttribute(String zimbraReverseProxyHttpPortAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpPortAttribute, zimbraReverseProxyHttpPortAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains http bind port
     *
     * @param zimbraReverseProxyHttpPortAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=632)
    public Map<String,Object> setReverseProxyHttpPortAttribute(String zimbraReverseProxyHttpPortAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpPortAttribute, zimbraReverseProxyHttpPortAttribute);
        return attrs;
    }

    /**
     * attribute that contains http bind port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=632)
    public void unsetReverseProxyHttpPortAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains http bind port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=632)
    public Map<String,Object> unsetReverseProxyHttpPortAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "");
        return attrs;
    }

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @return zimbraReverseProxyIPLoginLimit, or 0 if unset
     */
    @ZAttr(id=622)
    public int getReverseProxyIPLoginLimit() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyIPLoginLimit, 0);
    }

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @param zimbraReverseProxyIPLoginLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=622)
    public void setReverseProxyIPLoginLimit(int zimbraReverseProxyIPLoginLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimit, Integer.toString(zimbraReverseProxyIPLoginLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @param zimbraReverseProxyIPLoginLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=622)
    public Map<String,Object> setReverseProxyIPLoginLimit(int zimbraReverseProxyIPLoginLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimit, Integer.toString(zimbraReverseProxyIPLoginLimit));
        return attrs;
    }

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=622)
    public void unsetReverseProxyIPLoginLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=622)
    public Map<String,Object> unsetReverseProxyIPLoginLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimit, "");
        return attrs;
    }

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @return zimbraReverseProxyIPLoginLimitTime, or 3600 if unset
     */
    @ZAttr(id=623)
    public int getReverseProxyIPLoginLimitTime() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, 3600);
    }

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @param zimbraReverseProxyIPLoginLimitTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=623)
    public void setReverseProxyIPLoginLimitTime(int zimbraReverseProxyIPLoginLimitTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, Integer.toString(zimbraReverseProxyIPLoginLimitTime));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @param zimbraReverseProxyIPLoginLimitTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=623)
    public Map<String,Object> setReverseProxyIPLoginLimitTime(int zimbraReverseProxyIPLoginLimitTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, Integer.toString(zimbraReverseProxyIPLoginLimitTime));
        return attrs;
    }

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=623)
    public void unsetReverseProxyIPLoginLimitTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=623)
    public Map<String,Object> unsetReverseProxyIPLoginLimitTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, "");
        return attrs;
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @return zimbraReverseProxyImapEnabledCapability, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public String[] getReverseProxyImapEnabledCapability() {
        String[] value = getMultiAttr(Provisioning.A_zimbraReverseProxyImapEnabledCapability); return value.length > 0 ? value : new String[] {"IMAP4rev1","ACL","BINARY","CATENATE","CHILDREN","CONDSTORE","ENABLE","ESEARCH","ESORT","I18NLEVEL=1","ID","IDLE","LIST-EXTENDED","LITERAL+","MULTIAPPEND","NAMESPACE","QRESYNC","QUOTA","RIGHTS=ektx","SASL-IR","SEARCHRES","SORT","THREAD=ORDEREDSUBJECT","UIDPLUS","UNSELECT","WITHIN"};
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param zimbraReverseProxyImapEnabledCapability new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public void setReverseProxyImapEnabledCapability(String[] zimbraReverseProxyImapEnabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapEnabledCapability, zimbraReverseProxyImapEnabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param zimbraReverseProxyImapEnabledCapability new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public Map<String,Object> setReverseProxyImapEnabledCapability(String[] zimbraReverseProxyImapEnabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapEnabledCapability, zimbraReverseProxyImapEnabledCapability);
        return attrs;
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param zimbraReverseProxyImapEnabledCapability new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public void addReverseProxyImapEnabledCapability(String zimbraReverseProxyImapEnabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraReverseProxyImapEnabledCapability, zimbraReverseProxyImapEnabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param zimbraReverseProxyImapEnabledCapability new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public Map<String,Object> addReverseProxyImapEnabledCapability(String zimbraReverseProxyImapEnabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraReverseProxyImapEnabledCapability, zimbraReverseProxyImapEnabledCapability);
        return attrs;
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param zimbraReverseProxyImapEnabledCapability existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public void removeReverseProxyImapEnabledCapability(String zimbraReverseProxyImapEnabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraReverseProxyImapEnabledCapability, zimbraReverseProxyImapEnabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param zimbraReverseProxyImapEnabledCapability existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public Map<String,Object> removeReverseProxyImapEnabledCapability(String zimbraReverseProxyImapEnabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraReverseProxyImapEnabledCapability, zimbraReverseProxyImapEnabledCapability);
        return attrs;
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public void unsetReverseProxyImapEnabledCapability() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapEnabledCapability, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public Map<String,Object> unsetReverseProxyImapEnabledCapability(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapEnabledCapability, "");
        return attrs;
    }

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @return zimbraReverseProxyImapExposeVersionOnBanner, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public boolean isReverseProxyImapExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, false);
    }

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @param zimbraReverseProxyImapExposeVersionOnBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public void setReverseProxyImapExposeVersionOnBanner(boolean zimbraReverseProxyImapExposeVersionOnBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, zimbraReverseProxyImapExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @param zimbraReverseProxyImapExposeVersionOnBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public Map<String,Object> setReverseProxyImapExposeVersionOnBanner(boolean zimbraReverseProxyImapExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, zimbraReverseProxyImapExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public void unsetReverseProxyImapExposeVersionOnBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public Map<String,Object> unsetReverseProxyImapExposeVersionOnBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, "");
        return attrs;
    }

    /**
     * attribute that contains imap bind port
     *
     * @return zimbraReverseProxyImapPortAttribute, or "zimbraImapBindPort" if unset
     */
    @ZAttr(id=479)
    public String getReverseProxyImapPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapPortAttribute, "zimbraImapBindPort");
    }

    /**
     * attribute that contains imap bind port
     *
     * @param zimbraReverseProxyImapPortAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=479)
    public void setReverseProxyImapPortAttribute(String zimbraReverseProxyImapPortAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapPortAttribute, zimbraReverseProxyImapPortAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains imap bind port
     *
     * @param zimbraReverseProxyImapPortAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=479)
    public Map<String,Object> setReverseProxyImapPortAttribute(String zimbraReverseProxyImapPortAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapPortAttribute, zimbraReverseProxyImapPortAttribute);
        return attrs;
    }

    /**
     * attribute that contains imap bind port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=479)
    public void unsetReverseProxyImapPortAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapPortAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains imap bind port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=479)
    public Map<String,Object> unsetReverseProxyImapPortAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapPortAttribute, "");
        return attrs;
    }

    /**
     * attribute that contains imap bind port for SSL
     *
     * @return zimbraReverseProxyImapSSLPortAttribute, or "zimbraImapSSLBindPort" if unset
     */
    @ZAttr(id=480)
    public String getReverseProxyImapSSLPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute, "zimbraImapSSLBindPort");
    }

    /**
     * attribute that contains imap bind port for SSL
     *
     * @param zimbraReverseProxyImapSSLPortAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=480)
    public void setReverseProxyImapSSLPortAttribute(String zimbraReverseProxyImapSSLPortAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute, zimbraReverseProxyImapSSLPortAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains imap bind port for SSL
     *
     * @param zimbraReverseProxyImapSSLPortAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=480)
    public Map<String,Object> setReverseProxyImapSSLPortAttribute(String zimbraReverseProxyImapSSLPortAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute, zimbraReverseProxyImapSSLPortAttribute);
        return attrs;
    }

    /**
     * attribute that contains imap bind port for SSL
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=480)
    public void unsetReverseProxyImapSSLPortAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains imap bind port for SSL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=480)
    public Map<String,Object> unsetReverseProxyImapSSLPortAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute, "");
        return attrs;
    }

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @return zimbraReverseProxyImapSaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=643)
    public boolean isReverseProxyImapSaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, false);
    }

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @param zimbraReverseProxyImapSaslGssapiEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=643)
    public void setReverseProxyImapSaslGssapiEnabled(boolean zimbraReverseProxyImapSaslGssapiEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, zimbraReverseProxyImapSaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @param zimbraReverseProxyImapSaslGssapiEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=643)
    public Map<String,Object> setReverseProxyImapSaslGssapiEnabled(boolean zimbraReverseProxyImapSaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, zimbraReverseProxyImapSaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=643)
    public void unsetReverseProxyImapSaslGssapiEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=643)
    public Map<String,Object> unsetReverseProxyImapSaslGssapiEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, "");
        return attrs;
    }

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @return zimbraReverseProxyImapSaslPlainEnabled, or true if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public boolean isReverseProxyImapSaslPlainEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, true);
    }

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @param zimbraReverseProxyImapSaslPlainEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public void setReverseProxyImapSaslPlainEnabled(boolean zimbraReverseProxyImapSaslPlainEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, zimbraReverseProxyImapSaslPlainEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @param zimbraReverseProxyImapSaslPlainEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public Map<String,Object> setReverseProxyImapSaslPlainEnabled(boolean zimbraReverseProxyImapSaslPlainEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, zimbraReverseProxyImapSaslPlainEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public void unsetReverseProxyImapSaslPlainEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public Map<String,Object> unsetReverseProxyImapSaslPlainEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, "");
        return attrs;
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyImapStartTlsMode, or ZAttrProvisioning.ReverseProxyImapStartTlsMode.only if unset and/or has invalid value
     */
    @ZAttr(id=641)
    public ZAttrProvisioning.ReverseProxyImapStartTlsMode getReverseProxyImapStartTlsMode() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyImapStartTlsMode); return v == null ? ZAttrProvisioning.ReverseProxyImapStartTlsMode.only : ZAttrProvisioning.ReverseProxyImapStartTlsMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.ReverseProxyImapStartTlsMode.only; }
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyImapStartTlsMode, or "only" if unset
     */
    @ZAttr(id=641)
    public String getReverseProxyImapStartTlsModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapStartTlsMode, "only");
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyImapStartTlsMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=641)
    public void setReverseProxyImapStartTlsMode(ZAttrProvisioning.ReverseProxyImapStartTlsMode zimbraReverseProxyImapStartTlsMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapStartTlsMode, zimbraReverseProxyImapStartTlsMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyImapStartTlsMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=641)
    public Map<String,Object> setReverseProxyImapStartTlsMode(ZAttrProvisioning.ReverseProxyImapStartTlsMode zimbraReverseProxyImapStartTlsMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapStartTlsMode, zimbraReverseProxyImapStartTlsMode.toString());
        return attrs;
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyImapStartTlsMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=641)
    public void setReverseProxyImapStartTlsModeAsString(String zimbraReverseProxyImapStartTlsMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapStartTlsMode, zimbraReverseProxyImapStartTlsMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyImapStartTlsMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=641)
    public Map<String,Object> setReverseProxyImapStartTlsModeAsString(String zimbraReverseProxyImapStartTlsMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapStartTlsMode, zimbraReverseProxyImapStartTlsMode);
        return attrs;
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=641)
    public void unsetReverseProxyImapStartTlsMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapStartTlsMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=641)
    public Map<String,Object> unsetReverseProxyImapStartTlsMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapStartTlsMode, "");
        return attrs;
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * <p>Use getReverseProxyInactivityTimeoutAsString to access value as a string.
     *
     * @see #getReverseProxyInactivityTimeoutAsString()
     *
     * @return zimbraReverseProxyInactivityTimeout in millseconds, or 3600000 (1h)  if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public long getReverseProxyInactivityTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyInactivityTimeout, 3600000L);
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @return zimbraReverseProxyInactivityTimeout, or "1h" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public String getReverseProxyInactivityTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyInactivityTimeout, "1h");
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @param zimbraReverseProxyInactivityTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public void setReverseProxyInactivityTimeout(String zimbraReverseProxyInactivityTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyInactivityTimeout, zimbraReverseProxyInactivityTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @param zimbraReverseProxyInactivityTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public Map<String,Object> setReverseProxyInactivityTimeout(String zimbraReverseProxyInactivityTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyInactivityTimeout, zimbraReverseProxyInactivityTimeout);
        return attrs;
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public void unsetReverseProxyInactivityTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyInactivityTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public Map<String,Object> unsetReverseProxyInactivityTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyInactivityTimeout, "");
        return attrs;
    }

    /**
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @return zimbraReverseProxyIpThrottleMsg, or "Login rejected from this IP" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public String getReverseProxyIpThrottleMsg() {
        return getAttr(Provisioning.A_zimbraReverseProxyIpThrottleMsg, "Login rejected from this IP");
    }

    /**
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @param zimbraReverseProxyIpThrottleMsg new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public void setReverseProxyIpThrottleMsg(String zimbraReverseProxyIpThrottleMsg) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIpThrottleMsg, zimbraReverseProxyIpThrottleMsg);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @param zimbraReverseProxyIpThrottleMsg new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public Map<String,Object> setReverseProxyIpThrottleMsg(String zimbraReverseProxyIpThrottleMsg, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIpThrottleMsg, zimbraReverseProxyIpThrottleMsg);
        return attrs;
    }

    /**
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public void unsetReverseProxyIpThrottleMsg() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIpThrottleMsg, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public Map<String,Object> unsetReverseProxyIpThrottleMsg(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyIpThrottleMsg, "");
        return attrs;
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @return zimbraReverseProxyLogLevel, or ZAttrProvisioning.ReverseProxyLogLevel.info if unset and/or has invalid value
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public ZAttrProvisioning.ReverseProxyLogLevel getReverseProxyLogLevel() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyLogLevel); return v == null ? ZAttrProvisioning.ReverseProxyLogLevel.info : ZAttrProvisioning.ReverseProxyLogLevel.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.ReverseProxyLogLevel.info; }
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @return zimbraReverseProxyLogLevel, or "info" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public String getReverseProxyLogLevelAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyLogLevel, "info");
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @param zimbraReverseProxyLogLevel new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public void setReverseProxyLogLevel(ZAttrProvisioning.ReverseProxyLogLevel zimbraReverseProxyLogLevel) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLogLevel, zimbraReverseProxyLogLevel.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @param zimbraReverseProxyLogLevel new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public Map<String,Object> setReverseProxyLogLevel(ZAttrProvisioning.ReverseProxyLogLevel zimbraReverseProxyLogLevel, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLogLevel, zimbraReverseProxyLogLevel.toString());
        return attrs;
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @param zimbraReverseProxyLogLevel new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public void setReverseProxyLogLevelAsString(String zimbraReverseProxyLogLevel) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLogLevel, zimbraReverseProxyLogLevel);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @param zimbraReverseProxyLogLevel new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public Map<String,Object> setReverseProxyLogLevelAsString(String zimbraReverseProxyLogLevel, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLogLevel, zimbraReverseProxyLogLevel);
        return attrs;
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public void unsetReverseProxyLogLevel() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLogLevel, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public Map<String,Object> unsetReverseProxyLogLevel(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLogLevel, "");
        return attrs;
    }

    /**
     * whether this server is a reverse proxy lookup target
     *
     * @return zimbraReverseProxyLookupTarget, or false if unset
     */
    @ZAttr(id=504)
    public boolean isReverseProxyLookupTarget() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
    }

    /**
     * whether this server is a reverse proxy lookup target
     *
     * @param zimbraReverseProxyLookupTarget new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=504)
    public void setReverseProxyLookupTarget(boolean zimbraReverseProxyLookupTarget) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLookupTarget, zimbraReverseProxyLookupTarget ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether this server is a reverse proxy lookup target
     *
     * @param zimbraReverseProxyLookupTarget new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=504)
    public Map<String,Object> setReverseProxyLookupTarget(boolean zimbraReverseProxyLookupTarget, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLookupTarget, zimbraReverseProxyLookupTarget ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether this server is a reverse proxy lookup target
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=504)
    public void unsetReverseProxyLookupTarget() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLookupTarget, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether this server is a reverse proxy lookup target
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=504)
    public Map<String,Object> unsetReverseProxyLookupTarget(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLookupTarget, "");
        return attrs;
    }

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @return zimbraReverseProxyMailEnabled, or true if unset
     */
    @ZAttr(id=629)
    public boolean isReverseProxyMailEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyMailEnabled, true);
    }

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @param zimbraReverseProxyMailEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=629)
    public void setReverseProxyMailEnabled(boolean zimbraReverseProxyMailEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailEnabled, zimbraReverseProxyMailEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @param zimbraReverseProxyMailEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=629)
    public Map<String,Object> setReverseProxyMailEnabled(boolean zimbraReverseProxyMailEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailEnabled, zimbraReverseProxyMailEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=629)
    public void unsetReverseProxyMailEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=629)
    public Map<String,Object> unsetReverseProxyMailEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailEnabled, "");
        return attrs;
    }

    /**
     * LDAP attribute that contains mailhost for the user
     *
     * @return zimbraReverseProxyMailHostAttribute, or "zimbraMailHost" if unset
     */
    @ZAttr(id=474)
    public String getReverseProxyMailHostAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostAttribute, "zimbraMailHost");
    }

    /**
     * LDAP attribute that contains mailhost for the user
     *
     * @param zimbraReverseProxyMailHostAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=474)
    public void setReverseProxyMailHostAttribute(String zimbraReverseProxyMailHostAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostAttribute, zimbraReverseProxyMailHostAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute that contains mailhost for the user
     *
     * @param zimbraReverseProxyMailHostAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=474)
    public Map<String,Object> setReverseProxyMailHostAttribute(String zimbraReverseProxyMailHostAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostAttribute, zimbraReverseProxyMailHostAttribute);
        return attrs;
    }

    /**
     * LDAP attribute that contains mailhost for the user
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=474)
    public void unsetReverseProxyMailHostAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute that contains mailhost for the user
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=474)
    public Map<String,Object> unsetReverseProxyMailHostAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostAttribute, "");
        return attrs;
    }

    /**
     * LDAP query to find a user
     *
     * @return zimbraReverseProxyMailHostQuery, or "(|(zimbraMailDeliveryAddress=${USER})(zimbraMailAlias=${USER})(zimbraId=${USER}))" if unset
     */
    @ZAttr(id=472)
    public String getReverseProxyMailHostQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostQuery, "(|(zimbraMailDeliveryAddress=${USER})(zimbraMailAlias=${USER})(zimbraId=${USER}))");
    }

    /**
     * LDAP query to find a user
     *
     * @param zimbraReverseProxyMailHostQuery new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=472)
    public void setReverseProxyMailHostQuery(String zimbraReverseProxyMailHostQuery) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostQuery, zimbraReverseProxyMailHostQuery);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP query to find a user
     *
     * @param zimbraReverseProxyMailHostQuery new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=472)
    public Map<String,Object> setReverseProxyMailHostQuery(String zimbraReverseProxyMailHostQuery, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostQuery, zimbraReverseProxyMailHostQuery);
        return attrs;
    }

    /**
     * LDAP query to find a user
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=472)
    public void unsetReverseProxyMailHostQuery() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostQuery, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP query to find a user
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=472)
    public Map<String,Object> unsetReverseProxyMailHostQuery(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostQuery, "");
        return attrs;
    }

    /**
     * search base for zimbraReverseProxyMailHostQuery
     *
     * @return zimbraReverseProxyMailHostSearchBase, or null if unset
     */
    @ZAttr(id=473)
    public String getReverseProxyMailHostSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostSearchBase, null);
    }

    /**
     * search base for zimbraReverseProxyMailHostQuery
     *
     * @param zimbraReverseProxyMailHostSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=473)
    public void setReverseProxyMailHostSearchBase(String zimbraReverseProxyMailHostSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostSearchBase, zimbraReverseProxyMailHostSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * search base for zimbraReverseProxyMailHostQuery
     *
     * @param zimbraReverseProxyMailHostSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=473)
    public Map<String,Object> setReverseProxyMailHostSearchBase(String zimbraReverseProxyMailHostSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostSearchBase, zimbraReverseProxyMailHostSearchBase);
        return attrs;
    }

    /**
     * search base for zimbraReverseProxyMailHostQuery
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=473)
    public void unsetReverseProxyMailHostSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * search base for zimbraReverseProxyMailHostQuery
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=473)
    public Map<String,Object> unsetReverseProxyMailHostSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailHostSearchBase, "");
        return attrs;
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @return zimbraReverseProxyMailMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=685)
    public ZAttrProvisioning.ReverseProxyMailMode getReverseProxyMailMode() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyMailMode); return v == null ? null : ZAttrProvisioning.ReverseProxyMailMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @return zimbraReverseProxyMailMode, or null if unset
     */
    @ZAttr(id=685)
    public String getReverseProxyMailModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailMode, null);
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraReverseProxyMailMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=685)
    public void setReverseProxyMailMode(ZAttrProvisioning.ReverseProxyMailMode zimbraReverseProxyMailMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailMode, zimbraReverseProxyMailMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraReverseProxyMailMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=685)
    public Map<String,Object> setReverseProxyMailMode(ZAttrProvisioning.ReverseProxyMailMode zimbraReverseProxyMailMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailMode, zimbraReverseProxyMailMode.toString());
        return attrs;
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraReverseProxyMailMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=685)
    public void setReverseProxyMailModeAsString(String zimbraReverseProxyMailMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailMode, zimbraReverseProxyMailMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param zimbraReverseProxyMailMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=685)
    public Map<String,Object> setReverseProxyMailModeAsString(String zimbraReverseProxyMailMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailMode, zimbraReverseProxyMailMode);
        return attrs;
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=685)
    public void unsetReverseProxyMailMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=685)
    public Map<String,Object> unsetReverseProxyMailMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyMailMode, "");
        return attrs;
    }

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @return zimbraReverseProxyPassErrors, or true if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public boolean isReverseProxyPassErrors() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPassErrors, true);
    }

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @param zimbraReverseProxyPassErrors new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public void setReverseProxyPassErrors(boolean zimbraReverseProxyPassErrors) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPassErrors, zimbraReverseProxyPassErrors ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @param zimbraReverseProxyPassErrors new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public Map<String,Object> setReverseProxyPassErrors(boolean zimbraReverseProxyPassErrors, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPassErrors, zimbraReverseProxyPassErrors ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public void unsetReverseProxyPassErrors() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPassErrors, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public Map<String,Object> unsetReverseProxyPassErrors(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPassErrors, "");
        return attrs;
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @return zimbraReverseProxyPop3EnabledCapability, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public String[] getReverseProxyPop3EnabledCapability() {
        String[] value = getMultiAttr(Provisioning.A_zimbraReverseProxyPop3EnabledCapability); return value.length > 0 ? value : new String[] {"TOP","USER","UIDL","EXPIRE 31 USER","XOIP"};
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param zimbraReverseProxyPop3EnabledCapability new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public void setReverseProxyPop3EnabledCapability(String[] zimbraReverseProxyPop3EnabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3EnabledCapability, zimbraReverseProxyPop3EnabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param zimbraReverseProxyPop3EnabledCapability new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public Map<String,Object> setReverseProxyPop3EnabledCapability(String[] zimbraReverseProxyPop3EnabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3EnabledCapability, zimbraReverseProxyPop3EnabledCapability);
        return attrs;
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param zimbraReverseProxyPop3EnabledCapability new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public void addReverseProxyPop3EnabledCapability(String zimbraReverseProxyPop3EnabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraReverseProxyPop3EnabledCapability, zimbraReverseProxyPop3EnabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param zimbraReverseProxyPop3EnabledCapability new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public Map<String,Object> addReverseProxyPop3EnabledCapability(String zimbraReverseProxyPop3EnabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraReverseProxyPop3EnabledCapability, zimbraReverseProxyPop3EnabledCapability);
        return attrs;
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param zimbraReverseProxyPop3EnabledCapability existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public void removeReverseProxyPop3EnabledCapability(String zimbraReverseProxyPop3EnabledCapability) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraReverseProxyPop3EnabledCapability, zimbraReverseProxyPop3EnabledCapability);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param zimbraReverseProxyPop3EnabledCapability existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public Map<String,Object> removeReverseProxyPop3EnabledCapability(String zimbraReverseProxyPop3EnabledCapability, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraReverseProxyPop3EnabledCapability, zimbraReverseProxyPop3EnabledCapability);
        return attrs;
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public void unsetReverseProxyPop3EnabledCapability() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3EnabledCapability, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public Map<String,Object> unsetReverseProxyPop3EnabledCapability(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3EnabledCapability, "");
        return attrs;
    }

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @return zimbraReverseProxyPop3ExposeVersionOnBanner, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public boolean isReverseProxyPop3ExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, false);
    }

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @param zimbraReverseProxyPop3ExposeVersionOnBanner new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public void setReverseProxyPop3ExposeVersionOnBanner(boolean zimbraReverseProxyPop3ExposeVersionOnBanner) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, zimbraReverseProxyPop3ExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @param zimbraReverseProxyPop3ExposeVersionOnBanner new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public Map<String,Object> setReverseProxyPop3ExposeVersionOnBanner(boolean zimbraReverseProxyPop3ExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, zimbraReverseProxyPop3ExposeVersionOnBanner ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public void unsetReverseProxyPop3ExposeVersionOnBanner() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public Map<String,Object> unsetReverseProxyPop3ExposeVersionOnBanner(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, "");
        return attrs;
    }

    /**
     * attribute that contains pop3 bind port
     *
     * @return zimbraReverseProxyPop3PortAttribute, or "zimbraPop3BindPort" if unset
     */
    @ZAttr(id=477)
    public String getReverseProxyPop3PortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3PortAttribute, "zimbraPop3BindPort");
    }

    /**
     * attribute that contains pop3 bind port
     *
     * @param zimbraReverseProxyPop3PortAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=477)
    public void setReverseProxyPop3PortAttribute(String zimbraReverseProxyPop3PortAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3PortAttribute, zimbraReverseProxyPop3PortAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains pop3 bind port
     *
     * @param zimbraReverseProxyPop3PortAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=477)
    public Map<String,Object> setReverseProxyPop3PortAttribute(String zimbraReverseProxyPop3PortAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3PortAttribute, zimbraReverseProxyPop3PortAttribute);
        return attrs;
    }

    /**
     * attribute that contains pop3 bind port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=477)
    public void unsetReverseProxyPop3PortAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3PortAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains pop3 bind port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=477)
    public Map<String,Object> unsetReverseProxyPop3PortAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3PortAttribute, "");
        return attrs;
    }

    /**
     * attribute that contains pop3 bind port for SSL
     *
     * @return zimbraReverseProxyPop3SSLPortAttribute, or "zimbraPop3SSLBindPort" if unset
     */
    @ZAttr(id=478)
    public String getReverseProxyPop3SSLPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute, "zimbraPop3SSLBindPort");
    }

    /**
     * attribute that contains pop3 bind port for SSL
     *
     * @param zimbraReverseProxyPop3SSLPortAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=478)
    public void setReverseProxyPop3SSLPortAttribute(String zimbraReverseProxyPop3SSLPortAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute, zimbraReverseProxyPop3SSLPortAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains pop3 bind port for SSL
     *
     * @param zimbraReverseProxyPop3SSLPortAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=478)
    public Map<String,Object> setReverseProxyPop3SSLPortAttribute(String zimbraReverseProxyPop3SSLPortAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute, zimbraReverseProxyPop3SSLPortAttribute);
        return attrs;
    }

    /**
     * attribute that contains pop3 bind port for SSL
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=478)
    public void unsetReverseProxyPop3SSLPortAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * attribute that contains pop3 bind port for SSL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=478)
    public Map<String,Object> unsetReverseProxyPop3SSLPortAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute, "");
        return attrs;
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @return zimbraReverseProxyPop3SaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=644)
    public boolean isReverseProxyPop3SaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, false);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @param zimbraReverseProxyPop3SaslGssapiEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=644)
    public void setReverseProxyPop3SaslGssapiEnabled(boolean zimbraReverseProxyPop3SaslGssapiEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, zimbraReverseProxyPop3SaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @param zimbraReverseProxyPop3SaslGssapiEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=644)
    public Map<String,Object> setReverseProxyPop3SaslGssapiEnabled(boolean zimbraReverseProxyPop3SaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, zimbraReverseProxyPop3SaslGssapiEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=644)
    public void unsetReverseProxyPop3SaslGssapiEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=644)
    public Map<String,Object> unsetReverseProxyPop3SaslGssapiEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, "");
        return attrs;
    }

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @return zimbraReverseProxyPop3SaslPlainEnabled, or true if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public boolean isReverseProxyPop3SaslPlainEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, true);
    }

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @param zimbraReverseProxyPop3SaslPlainEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public void setReverseProxyPop3SaslPlainEnabled(boolean zimbraReverseProxyPop3SaslPlainEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, zimbraReverseProxyPop3SaslPlainEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @param zimbraReverseProxyPop3SaslPlainEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public Map<String,Object> setReverseProxyPop3SaslPlainEnabled(boolean zimbraReverseProxyPop3SaslPlainEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, zimbraReverseProxyPop3SaslPlainEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public void unsetReverseProxyPop3SaslPlainEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public Map<String,Object> unsetReverseProxyPop3SaslPlainEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, "");
        return attrs;
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyPop3StartTlsMode, or ZAttrProvisioning.ReverseProxyPop3StartTlsMode.only if unset and/or has invalid value
     */
    @ZAttr(id=642)
    public ZAttrProvisioning.ReverseProxyPop3StartTlsMode getReverseProxyPop3StartTlsMode() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyPop3StartTlsMode); return v == null ? ZAttrProvisioning.ReverseProxyPop3StartTlsMode.only : ZAttrProvisioning.ReverseProxyPop3StartTlsMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.ReverseProxyPop3StartTlsMode.only; }
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyPop3StartTlsMode, or "only" if unset
     */
    @ZAttr(id=642)
    public String getReverseProxyPop3StartTlsModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, "only");
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyPop3StartTlsMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=642)
    public void setReverseProxyPop3StartTlsMode(ZAttrProvisioning.ReverseProxyPop3StartTlsMode zimbraReverseProxyPop3StartTlsMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, zimbraReverseProxyPop3StartTlsMode.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyPop3StartTlsMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=642)
    public Map<String,Object> setReverseProxyPop3StartTlsMode(ZAttrProvisioning.ReverseProxyPop3StartTlsMode zimbraReverseProxyPop3StartTlsMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, zimbraReverseProxyPop3StartTlsMode.toString());
        return attrs;
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyPop3StartTlsMode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=642)
    public void setReverseProxyPop3StartTlsModeAsString(String zimbraReverseProxyPop3StartTlsMode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, zimbraReverseProxyPop3StartTlsMode);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param zimbraReverseProxyPop3StartTlsMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=642)
    public Map<String,Object> setReverseProxyPop3StartTlsModeAsString(String zimbraReverseProxyPop3StartTlsMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, zimbraReverseProxyPop3StartTlsMode);
        return attrs;
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=642)
    public void unsetReverseProxyPop3StartTlsMode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=642)
    public Map<String,Object> unsetReverseProxyPop3StartTlsMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3StartTlsMode, "");
        return attrs;
    }

    /**
     * LDAP query to find server object
     *
     * @return zimbraReverseProxyPortQuery, or "(&(zimbraServiceHostname=${MAILHOST})(objectClass=zimbraServer))" if unset
     */
    @ZAttr(id=475)
    public String getReverseProxyPortQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyPortQuery, "(&(zimbraServiceHostname=${MAILHOST})(objectClass=zimbraServer))");
    }

    /**
     * LDAP query to find server object
     *
     * @param zimbraReverseProxyPortQuery new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=475)
    public void setReverseProxyPortQuery(String zimbraReverseProxyPortQuery) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortQuery, zimbraReverseProxyPortQuery);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP query to find server object
     *
     * @param zimbraReverseProxyPortQuery new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=475)
    public Map<String,Object> setReverseProxyPortQuery(String zimbraReverseProxyPortQuery, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortQuery, zimbraReverseProxyPortQuery);
        return attrs;
    }

    /**
     * LDAP query to find server object
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=475)
    public void unsetReverseProxyPortQuery() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortQuery, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP query to find server object
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=475)
    public Map<String,Object> unsetReverseProxyPortQuery(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortQuery, "");
        return attrs;
    }

    /**
     * search base for zimbraReverseProxyPortQuery
     *
     * @return zimbraReverseProxyPortSearchBase, or null if unset
     */
    @ZAttr(id=476)
    public String getReverseProxyPortSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyPortSearchBase, null);
    }

    /**
     * search base for zimbraReverseProxyPortQuery
     *
     * @param zimbraReverseProxyPortSearchBase new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=476)
    public void setReverseProxyPortSearchBase(String zimbraReverseProxyPortSearchBase) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortSearchBase, zimbraReverseProxyPortSearchBase);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * search base for zimbraReverseProxyPortQuery
     *
     * @param zimbraReverseProxyPortSearchBase new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=476)
    public Map<String,Object> setReverseProxyPortSearchBase(String zimbraReverseProxyPortSearchBase, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortSearchBase, zimbraReverseProxyPortSearchBase);
        return attrs;
    }

    /**
     * search base for zimbraReverseProxyPortQuery
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=476)
    public void unsetReverseProxyPortSearchBase() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortSearchBase, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * search base for zimbraReverseProxyPortQuery
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=476)
    public Map<String,Object> unsetReverseProxyPortSearchBase(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPortSearchBase, "");
        return attrs;
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * <p>Use getReverseProxyRouteLookupTimeoutAsString to access value as a string.
     *
     * @see #getReverseProxyRouteLookupTimeoutAsString()
     *
     * @return zimbraReverseProxyRouteLookupTimeout in millseconds, or 15000 (15s)  if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public long getReverseProxyRouteLookupTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, 15000L);
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @return zimbraReverseProxyRouteLookupTimeout, or "15s" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public String getReverseProxyRouteLookupTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, "15s");
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @param zimbraReverseProxyRouteLookupTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public void setReverseProxyRouteLookupTimeout(String zimbraReverseProxyRouteLookupTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, zimbraReverseProxyRouteLookupTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @param zimbraReverseProxyRouteLookupTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public Map<String,Object> setReverseProxyRouteLookupTimeout(String zimbraReverseProxyRouteLookupTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, zimbraReverseProxyRouteLookupTimeout);
        return attrs;
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public void unsetReverseProxyRouteLookupTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public Map<String,Object> unsetReverseProxyRouteLookupTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, "");
        return attrs;
    }

    /**
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @return zimbraReverseProxySSLCiphers, or "!SSLv2:!MD5:HIGH" if unset
     */
    @ZAttr(id=640)
    public String getReverseProxySSLCiphers() {
        return getAttr(Provisioning.A_zimbraReverseProxySSLCiphers, "!SSLv2:!MD5:HIGH");
    }

    /**
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @param zimbraReverseProxySSLCiphers new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=640)
    public void setReverseProxySSLCiphers(String zimbraReverseProxySSLCiphers) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySSLCiphers, zimbraReverseProxySSLCiphers);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @param zimbraReverseProxySSLCiphers new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=640)
    public Map<String,Object> setReverseProxySSLCiphers(String zimbraReverseProxySSLCiphers, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySSLCiphers, zimbraReverseProxySSLCiphers);
        return attrs;
    }

    /**
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=640)
    public void unsetReverseProxySSLCiphers() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySSLCiphers, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=640)
    public Map<String,Object> unsetReverseProxySSLCiphers(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySSLCiphers, "");
        return attrs;
    }

    /**
     * whether nginx should send ID command for imap
     *
     * @return zimbraReverseProxySendImapId, or true if unset
     */
    @ZAttr(id=588)
    public boolean isReverseProxySendImapId() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySendImapId, true);
    }

    /**
     * whether nginx should send ID command for imap
     *
     * @param zimbraReverseProxySendImapId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=588)
    public void setReverseProxySendImapId(boolean zimbraReverseProxySendImapId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendImapId, zimbraReverseProxySendImapId ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether nginx should send ID command for imap
     *
     * @param zimbraReverseProxySendImapId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=588)
    public Map<String,Object> setReverseProxySendImapId(boolean zimbraReverseProxySendImapId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendImapId, zimbraReverseProxySendImapId ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether nginx should send ID command for imap
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=588)
    public void unsetReverseProxySendImapId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendImapId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether nginx should send ID command for imap
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=588)
    public Map<String,Object> unsetReverseProxySendImapId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendImapId, "");
        return attrs;
    }

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @return zimbraReverseProxySendPop3Xoip, or true if unset
     */
    @ZAttr(id=587)
    public boolean isReverseProxySendPop3Xoip() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySendPop3Xoip, true);
    }

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @param zimbraReverseProxySendPop3Xoip new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=587)
    public void setReverseProxySendPop3Xoip(boolean zimbraReverseProxySendPop3Xoip) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendPop3Xoip, zimbraReverseProxySendPop3Xoip ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @param zimbraReverseProxySendPop3Xoip new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=587)
    public Map<String,Object> setReverseProxySendPop3Xoip(boolean zimbraReverseProxySendPop3Xoip, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendPop3Xoip, zimbraReverseProxySendPop3Xoip ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=587)
    public void unsetReverseProxySendPop3Xoip() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendPop3Xoip, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=587)
    public Map<String,Object> unsetReverseProxySendPop3Xoip(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySendPop3Xoip, "");
        return attrs;
    }

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @return zimbraReverseProxyUserLoginLimit, or 0 if unset
     */
    @ZAttr(id=624)
    public int getReverseProxyUserLoginLimit() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyUserLoginLimit, 0);
    }

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @param zimbraReverseProxyUserLoginLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=624)
    public void setReverseProxyUserLoginLimit(int zimbraReverseProxyUserLoginLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimit, Integer.toString(zimbraReverseProxyUserLoginLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @param zimbraReverseProxyUserLoginLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=624)
    public Map<String,Object> setReverseProxyUserLoginLimit(int zimbraReverseProxyUserLoginLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimit, Integer.toString(zimbraReverseProxyUserLoginLimit));
        return attrs;
    }

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=624)
    public void unsetReverseProxyUserLoginLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=624)
    public Map<String,Object> unsetReverseProxyUserLoginLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimit, "");
        return attrs;
    }

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @return zimbraReverseProxyUserLoginLimitTime, or 3600 if unset
     */
    @ZAttr(id=625)
    public int getReverseProxyUserLoginLimitTime() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, 3600);
    }

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @param zimbraReverseProxyUserLoginLimitTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=625)
    public void setReverseProxyUserLoginLimitTime(int zimbraReverseProxyUserLoginLimitTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, Integer.toString(zimbraReverseProxyUserLoginLimitTime));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @param zimbraReverseProxyUserLoginLimitTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=625)
    public Map<String,Object> setReverseProxyUserLoginLimitTime(int zimbraReverseProxyUserLoginLimitTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, Integer.toString(zimbraReverseProxyUserLoginLimitTime));
        return attrs;
    }

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=625)
    public void unsetReverseProxyUserLoginLimitTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=625)
    public Map<String,Object> unsetReverseProxyUserLoginLimitTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, "");
        return attrs;
    }

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @return zimbraReverseProxyUserNameAttribute, or null if unset
     */
    @ZAttr(id=572)
    public String getReverseProxyUserNameAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyUserNameAttribute, null);
    }

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @param zimbraReverseProxyUserNameAttribute new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=572)
    public void setReverseProxyUserNameAttribute(String zimbraReverseProxyUserNameAttribute) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserNameAttribute, zimbraReverseProxyUserNameAttribute);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @param zimbraReverseProxyUserNameAttribute new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=572)
    public Map<String,Object> setReverseProxyUserNameAttribute(String zimbraReverseProxyUserNameAttribute, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserNameAttribute, zimbraReverseProxyUserNameAttribute);
        return attrs;
    }

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=572)
    public void unsetReverseProxyUserNameAttribute() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserNameAttribute, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=572)
    public Map<String,Object> unsetReverseProxyUserNameAttribute(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserNameAttribute, "");
        return attrs;
    }

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @return zimbraReverseProxyUserThrottleMsg, or "Login rejected for this user" if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public String getReverseProxyUserThrottleMsg() {
        return getAttr(Provisioning.A_zimbraReverseProxyUserThrottleMsg, "Login rejected for this user");
    }

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @param zimbraReverseProxyUserThrottleMsg new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public void setReverseProxyUserThrottleMsg(String zimbraReverseProxyUserThrottleMsg) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserThrottleMsg, zimbraReverseProxyUserThrottleMsg);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @param zimbraReverseProxyUserThrottleMsg new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public Map<String,Object> setReverseProxyUserThrottleMsg(String zimbraReverseProxyUserThrottleMsg, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserThrottleMsg, zimbraReverseProxyUserThrottleMsg);
        return attrs;
    }

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public void unsetReverseProxyUserThrottleMsg() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserThrottleMsg, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public Map<String,Object> unsetReverseProxyUserThrottleMsg(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyUserThrottleMsg, "");
        return attrs;
    }

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @return zimbraReverseProxyWorkerConnections, or 10240 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public int getReverseProxyWorkerConnections() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyWorkerConnections, 10240);
    }

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @param zimbraReverseProxyWorkerConnections new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public void setReverseProxyWorkerConnections(int zimbraReverseProxyWorkerConnections) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerConnections, Integer.toString(zimbraReverseProxyWorkerConnections));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @param zimbraReverseProxyWorkerConnections new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public Map<String,Object> setReverseProxyWorkerConnections(int zimbraReverseProxyWorkerConnections, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerConnections, Integer.toString(zimbraReverseProxyWorkerConnections));
        return attrs;
    }

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public void unsetReverseProxyWorkerConnections() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerConnections, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public Map<String,Object> unsetReverseProxyWorkerConnections(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerConnections, "");
        return attrs;
    }

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @return zimbraReverseProxyWorkerProcesses, or 4 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public int getReverseProxyWorkerProcesses() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyWorkerProcesses, 4);
    }

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @param zimbraReverseProxyWorkerProcesses new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public void setReverseProxyWorkerProcesses(int zimbraReverseProxyWorkerProcesses) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerProcesses, Integer.toString(zimbraReverseProxyWorkerProcesses));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @param zimbraReverseProxyWorkerProcesses new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public Map<String,Object> setReverseProxyWorkerProcesses(int zimbraReverseProxyWorkerProcesses, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerProcesses, Integer.toString(zimbraReverseProxyWorkerProcesses));
        return attrs;
    }

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public void unsetReverseProxyWorkerProcesses() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerProcesses, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public Map<String,Object> unsetReverseProxyWorkerProcesses(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyWorkerProcesses, "");
        return attrs;
    }

    /**
     * SSL certificate
     *
     * @return zimbraSSLCertificate, or null if unset
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
     */
    @ZAttr(id=563)
    public Map<String,Object> unsetSSLCertificate(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLCertificate, "");
        return attrs;
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @return zimbraSSLExcludeCipherSuites, or ampty array if unset
     */
    @ZAttr(id=639)
    public String[] getSSLExcludeCipherSuites() {
        String[] value = getMultiAttr(Provisioning.A_zimbraSSLExcludeCipherSuites); return value.length > 0 ? value : new String[] {"SSL_RSA_WITH_DES_CBC_SHA","SSL_DHE_RSA_WITH_DES_CBC_SHA","SSL_DHE_DSS_WITH_DES_CBC_SHA","SSL_RSA_EXPORT_WITH_RC4_40_MD5","SSL_RSA_EXPORT_WITH_DES40_CBC_SHA","SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA","SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"};
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param zimbraSSLExcludeCipherSuites new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=639)
    public void setSSLExcludeCipherSuites(String[] zimbraSSLExcludeCipherSuites) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLExcludeCipherSuites, zimbraSSLExcludeCipherSuites);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param zimbraSSLExcludeCipherSuites new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=639)
    public Map<String,Object> setSSLExcludeCipherSuites(String[] zimbraSSLExcludeCipherSuites, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLExcludeCipherSuites, zimbraSSLExcludeCipherSuites);
        return attrs;
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param zimbraSSLExcludeCipherSuites new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=639)
    public void addSSLExcludeCipherSuites(String zimbraSSLExcludeCipherSuites) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSSLExcludeCipherSuites, zimbraSSLExcludeCipherSuites);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param zimbraSSLExcludeCipherSuites new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=639)
    public Map<String,Object> addSSLExcludeCipherSuites(String zimbraSSLExcludeCipherSuites, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSSLExcludeCipherSuites, zimbraSSLExcludeCipherSuites);
        return attrs;
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param zimbraSSLExcludeCipherSuites existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=639)
    public void removeSSLExcludeCipherSuites(String zimbraSSLExcludeCipherSuites) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSSLExcludeCipherSuites, zimbraSSLExcludeCipherSuites);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param zimbraSSLExcludeCipherSuites existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=639)
    public Map<String,Object> removeSSLExcludeCipherSuites(String zimbraSSLExcludeCipherSuites, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSSLExcludeCipherSuites, zimbraSSLExcludeCipherSuites);
        return attrs;
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=639)
    public void unsetSSLExcludeCipherSuites() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLExcludeCipherSuites, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * space separated list of excluded cipher suites
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=639)
    public Map<String,Object> unsetSSLExcludeCipherSuites(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLExcludeCipherSuites, "");
        return attrs;
    }

    /**
     * SSL private key
     *
     * @return zimbraSSLPrivateKey, or null if unset
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
     */
    @ZAttr(id=564)
    public Map<String,Object> unsetSSLPrivateKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSSLPrivateKey, "");
        return attrs;
    }

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     *
     * @return zimbraScheduledTaskNumThreads, or 20 if unset
     */
    @ZAttr(id=522)
    public int getScheduledTaskNumThreads() {
        return getIntAttr(Provisioning.A_zimbraScheduledTaskNumThreads, 20);
    }

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     *
     * @param zimbraScheduledTaskNumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=522)
    public void setScheduledTaskNumThreads(int zimbraScheduledTaskNumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraScheduledTaskNumThreads, Integer.toString(zimbraScheduledTaskNumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     *
     * @param zimbraScheduledTaskNumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=522)
    public Map<String,Object> setScheduledTaskNumThreads(int zimbraScheduledTaskNumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraScheduledTaskNumThreads, Integer.toString(zimbraScheduledTaskNumThreads));
        return attrs;
    }

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=522)
    public void unsetScheduledTaskNumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraScheduledTaskNumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=522)
    public Map<String,Object> unsetScheduledTaskNumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraScheduledTaskNumThreads, "");
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @return zimbraServerExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public String[] getServerExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraServerExtraObjectClass);
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param zimbraServerExtraObjectClass new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public void setServerExtraObjectClass(String[] zimbraServerExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerExtraObjectClass, zimbraServerExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param zimbraServerExtraObjectClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public Map<String,Object> setServerExtraObjectClass(String[] zimbraServerExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerExtraObjectClass, zimbraServerExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param zimbraServerExtraObjectClass new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public void addServerExtraObjectClass(String zimbraServerExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraServerExtraObjectClass, zimbraServerExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param zimbraServerExtraObjectClass new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public Map<String,Object> addServerExtraObjectClass(String zimbraServerExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraServerExtraObjectClass, zimbraServerExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param zimbraServerExtraObjectClass existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public void removeServerExtraObjectClass(String zimbraServerExtraObjectClass) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraServerExtraObjectClass, zimbraServerExtraObjectClass);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param zimbraServerExtraObjectClass existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public Map<String,Object> removeServerExtraObjectClass(String zimbraServerExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraServerExtraObjectClass, zimbraServerExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public void unsetServerExtraObjectClass() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerExtraObjectClass, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public Map<String,Object> unsetServerExtraObjectClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerExtraObjectClass, "");
        return attrs;
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @return zimbraServerInheritedAttr, or ampty array if unset
     */
    @ZAttr(id=62)
    public String[] getServerInheritedAttr() {
        return getMultiAttr(Provisioning.A_zimbraServerInheritedAttr);
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param zimbraServerInheritedAttr new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=62)
    public void setServerInheritedAttr(String[] zimbraServerInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerInheritedAttr, zimbraServerInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param zimbraServerInheritedAttr new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=62)
    public Map<String,Object> setServerInheritedAttr(String[] zimbraServerInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerInheritedAttr, zimbraServerInheritedAttr);
        return attrs;
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param zimbraServerInheritedAttr new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=62)
    public void addServerInheritedAttr(String zimbraServerInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraServerInheritedAttr, zimbraServerInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param zimbraServerInheritedAttr new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=62)
    public Map<String,Object> addServerInheritedAttr(String zimbraServerInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraServerInheritedAttr, zimbraServerInheritedAttr);
        return attrs;
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param zimbraServerInheritedAttr existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=62)
    public void removeServerInheritedAttr(String zimbraServerInheritedAttr) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraServerInheritedAttr, zimbraServerInheritedAttr);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param zimbraServerInheritedAttr existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=62)
    public Map<String,Object> removeServerInheritedAttr(String zimbraServerInheritedAttr, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraServerInheritedAttr, zimbraServerInheritedAttr);
        return attrs;
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=62)
    public void unsetServerInheritedAttr() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerInheritedAttr, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=62)
    public Map<String,Object> unsetServerInheritedAttr(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServerInheritedAttr, "");
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
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @return zimbraSmtpSendAddAuthenticatedUser, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public boolean isSmtpSendAddAuthenticatedUser() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, false);
    }

    /**
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @param zimbraSmtpSendAddAuthenticatedUser new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public void setSmtpSendAddAuthenticatedUser(boolean zimbraSmtpSendAddAuthenticatedUser) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, zimbraSmtpSendAddAuthenticatedUser ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @param zimbraSmtpSendAddAuthenticatedUser new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public Map<String,Object> setSmtpSendAddAuthenticatedUser(boolean zimbraSmtpSendAddAuthenticatedUser, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, zimbraSmtpSendAddAuthenticatedUser ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public void unsetSmtpSendAddAuthenticatedUser() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public Map<String,Object> unsetSmtpSendAddAuthenticatedUser(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, "");
        return attrs;
    }

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @return zimbraSmtpSendAddMailer, or true if unset
     */
    @ZAttr(id=636)
    public boolean isSmtpSendAddMailer() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddMailer, true);
    }

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @param zimbraSmtpSendAddMailer new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=636)
    public void setSmtpSendAddMailer(boolean zimbraSmtpSendAddMailer) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddMailer, zimbraSmtpSendAddMailer ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @param zimbraSmtpSendAddMailer new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=636)
    public Map<String,Object> setSmtpSendAddMailer(boolean zimbraSmtpSendAddMailer, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddMailer, zimbraSmtpSendAddMailer ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=636)
    public void unsetSmtpSendAddMailer() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddMailer, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=636)
    public Map<String,Object> unsetSmtpSendAddMailer(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddMailer, "");
        return attrs;
    }

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     *
     * @return zimbraSmtpSendAddOriginatingIP, or true if unset
     */
    @ZAttr(id=435)
    public boolean isSmtpSendAddOriginatingIP() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddOriginatingIP, true);
    }

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     *
     * @param zimbraSmtpSendAddOriginatingIP new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=435)
    public void setSmtpSendAddOriginatingIP(boolean zimbraSmtpSendAddOriginatingIP) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddOriginatingIP, zimbraSmtpSendAddOriginatingIP ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     *
     * @param zimbraSmtpSendAddOriginatingIP new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=435)
    public Map<String,Object> setSmtpSendAddOriginatingIP(boolean zimbraSmtpSendAddOriginatingIP, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddOriginatingIP, zimbraSmtpSendAddOriginatingIP ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=435)
    public void unsetSmtpSendAddOriginatingIP() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddOriginatingIP, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=435)
    public Map<String,Object> unsetSmtpSendAddOriginatingIP(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddOriginatingIP, "");
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
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @return zimbraSoapExposeVersion, or false if unset
     */
    @ZAttr(id=708)
    public boolean isSoapExposeVersion() {
        return getBooleanAttr(Provisioning.A_zimbraSoapExposeVersion, false);
    }

    /**
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @param zimbraSoapExposeVersion new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=708)
    public void setSoapExposeVersion(boolean zimbraSoapExposeVersion) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapExposeVersion, zimbraSoapExposeVersion ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @param zimbraSoapExposeVersion new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=708)
    public Map<String,Object> setSoapExposeVersion(boolean zimbraSoapExposeVersion, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapExposeVersion, zimbraSoapExposeVersion ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=708)
    public void unsetSoapExposeVersion() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapExposeVersion, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=708)
    public Map<String,Object> unsetSoapExposeVersion(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapExposeVersion, "");
        return attrs;
    }

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @return zimbraSoapRequestMaxSize, or 15360000 if unset
     */
    @ZAttr(id=557)
    public int getSoapRequestMaxSize() {
        return getIntAttr(Provisioning.A_zimbraSoapRequestMaxSize, 15360000);
    }

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @param zimbraSoapRequestMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=557)
    public void setSoapRequestMaxSize(int zimbraSoapRequestMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapRequestMaxSize, Integer.toString(zimbraSoapRequestMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @param zimbraSoapRequestMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=557)
    public Map<String,Object> setSoapRequestMaxSize(int zimbraSoapRequestMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapRequestMaxSize, Integer.toString(zimbraSoapRequestMaxSize));
        return attrs;
    }

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=557)
    public void unsetSoapRequestMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapRequestMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=557)
    public Map<String,Object> unsetSoapRequestMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapRequestMaxSize, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable spam checking
     *
     * @return zimbraSpamCheckEnabled, or false if unset
     */
    @ZAttr(id=201)
    public boolean isSpamCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraSpamCheckEnabled, false);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable spam checking
     *
     * @param zimbraSpamCheckEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=201)
    public void setSpamCheckEnabled(boolean zimbraSpamCheckEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamCheckEnabled, zimbraSpamCheckEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable spam checking
     *
     * @param zimbraSpamCheckEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=201)
    public Map<String,Object> setSpamCheckEnabled(boolean zimbraSpamCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamCheckEnabled, zimbraSpamCheckEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable spam checking
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=201)
    public void unsetSpamCheckEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamCheckEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable spam checking
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=201)
    public Map<String,Object> unsetSpamCheckEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamCheckEnabled, "");
        return attrs;
    }

    /**
     * mail header name for flagging spam
     *
     * @return zimbraSpamHeader, or "X-Spam-Flag" if unset
     */
    @ZAttr(id=210)
    public String getSpamHeader() {
        return getAttr(Provisioning.A_zimbraSpamHeader, "X-Spam-Flag");
    }

    /**
     * mail header name for flagging spam
     *
     * @param zimbraSpamHeader new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=210)
    public void setSpamHeader(String zimbraSpamHeader) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeader, zimbraSpamHeader);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail header name for flagging spam
     *
     * @param zimbraSpamHeader new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=210)
    public Map<String,Object> setSpamHeader(String zimbraSpamHeader, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeader, zimbraSpamHeader);
        return attrs;
    }

    /**
     * mail header name for flagging spam
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=210)
    public void unsetSpamHeader() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeader, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail header name for flagging spam
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=210)
    public Map<String,Object> unsetSpamHeader(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeader, "");
        return attrs;
    }

    /**
     * regular expression for matching the spam header
     *
     * @return zimbraSpamHeaderValue, or "YES" if unset
     */
    @ZAttr(id=211)
    public String getSpamHeaderValue() {
        return getAttr(Provisioning.A_zimbraSpamHeaderValue, "YES");
    }

    /**
     * regular expression for matching the spam header
     *
     * @param zimbraSpamHeaderValue new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=211)
    public void setSpamHeaderValue(String zimbraSpamHeaderValue) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeaderValue, zimbraSpamHeaderValue);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regular expression for matching the spam header
     *
     * @param zimbraSpamHeaderValue new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=211)
    public Map<String,Object> setSpamHeaderValue(String zimbraSpamHeaderValue, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeaderValue, zimbraSpamHeaderValue);
        return attrs;
    }

    /**
     * regular expression for matching the spam header
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=211)
    public void unsetSpamHeaderValue() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeaderValue, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * regular expression for matching the spam header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=211)
    public Map<String,Object> unsetSpamHeaderValue(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamHeaderValue, "");
        return attrs;
    }

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     *
     * @return zimbraSpamIsNotSpamAccount, or null if unset
     */
    @ZAttr(id=245)
    public String getSpamIsNotSpamAccount() {
        return getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount, null);
    }

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     *
     * @param zimbraSpamIsNotSpamAccount new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=245)
    public void setSpamIsNotSpamAccount(String zimbraSpamIsNotSpamAccount) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsNotSpamAccount, zimbraSpamIsNotSpamAccount);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     *
     * @param zimbraSpamIsNotSpamAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=245)
    public Map<String,Object> setSpamIsNotSpamAccount(String zimbraSpamIsNotSpamAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsNotSpamAccount, zimbraSpamIsNotSpamAccount);
        return attrs;
    }

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=245)
    public void unsetSpamIsNotSpamAccount() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsNotSpamAccount, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=245)
    public Map<String,Object> unsetSpamIsNotSpamAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsNotSpamAccount, "");
        return attrs;
    }

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     *
     * @return zimbraSpamIsSpamAccount, or null if unset
     */
    @ZAttr(id=244)
    public String getSpamIsSpamAccount() {
        return getAttr(Provisioning.A_zimbraSpamIsSpamAccount, null);
    }

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     *
     * @param zimbraSpamIsSpamAccount new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=244)
    public void setSpamIsSpamAccount(String zimbraSpamIsSpamAccount) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsSpamAccount, zimbraSpamIsSpamAccount);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     *
     * @param zimbraSpamIsSpamAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=244)
    public Map<String,Object> setSpamIsSpamAccount(String zimbraSpamIsSpamAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsSpamAccount, zimbraSpamIsSpamAccount);
        return attrs;
    }

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=244)
    public void unsetSpamIsSpamAccount() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsSpamAccount, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=244)
    public Map<String,Object> unsetSpamIsSpamAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamIsSpamAccount, "");
        return attrs;
    }

    /**
     * Spaminess percentage beyond which a message is dropped
     *
     * @return zimbraSpamKillPercent, or 75 if unset
     */
    @ZAttr(id=202)
    public int getSpamKillPercent() {
        return getIntAttr(Provisioning.A_zimbraSpamKillPercent, 75);
    }

    /**
     * Spaminess percentage beyond which a message is dropped
     *
     * @param zimbraSpamKillPercent new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=202)
    public void setSpamKillPercent(int zimbraSpamKillPercent) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamKillPercent, Integer.toString(zimbraSpamKillPercent));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Spaminess percentage beyond which a message is dropped
     *
     * @param zimbraSpamKillPercent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=202)
    public Map<String,Object> setSpamKillPercent(int zimbraSpamKillPercent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamKillPercent, Integer.toString(zimbraSpamKillPercent));
        return attrs;
    }

    /**
     * Spaminess percentage beyond which a message is dropped
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=202)
    public void unsetSpamKillPercent() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamKillPercent, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Spaminess percentage beyond which a message is dropped
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=202)
    public Map<String,Object> unsetSpamKillPercent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamKillPercent, "");
        return attrs;
    }

    /**
     * mail header name for sender in spam report
     *
     * @return zimbraSpamReportSenderHeader, or "X-Zimbra-Spam-Report-Sender" if unset
     */
    @ZAttr(id=465)
    public String getSpamReportSenderHeader() {
        return getAttr(Provisioning.A_zimbraSpamReportSenderHeader, "X-Zimbra-Spam-Report-Sender");
    }

    /**
     * mail header name for sender in spam report
     *
     * @param zimbraSpamReportSenderHeader new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=465)
    public void setSpamReportSenderHeader(String zimbraSpamReportSenderHeader) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportSenderHeader, zimbraSpamReportSenderHeader);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail header name for sender in spam report
     *
     * @param zimbraSpamReportSenderHeader new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=465)
    public Map<String,Object> setSpamReportSenderHeader(String zimbraSpamReportSenderHeader, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportSenderHeader, zimbraSpamReportSenderHeader);
        return attrs;
    }

    /**
     * mail header name for sender in spam report
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=465)
    public void unsetSpamReportSenderHeader() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportSenderHeader, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail header name for sender in spam report
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=465)
    public Map<String,Object> unsetSpamReportSenderHeader(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportSenderHeader, "");
        return attrs;
    }

    /**
     * spam report type value for ham
     *
     * @return zimbraSpamReportTypeHam, or "ham" if unset
     */
    @ZAttr(id=468)
    public String getSpamReportTypeHam() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeHam, "ham");
    }

    /**
     * spam report type value for ham
     *
     * @param zimbraSpamReportTypeHam new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=468)
    public void setSpamReportTypeHam(String zimbraSpamReportTypeHam) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHam, zimbraSpamReportTypeHam);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * spam report type value for ham
     *
     * @param zimbraSpamReportTypeHam new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=468)
    public Map<String,Object> setSpamReportTypeHam(String zimbraSpamReportTypeHam, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHam, zimbraSpamReportTypeHam);
        return attrs;
    }

    /**
     * spam report type value for ham
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=468)
    public void unsetSpamReportTypeHam() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHam, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * spam report type value for ham
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=468)
    public Map<String,Object> unsetSpamReportTypeHam(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHam, "");
        return attrs;
    }

    /**
     * mail header name for report type in spam report
     *
     * @return zimbraSpamReportTypeHeader, or "X-Zimbra-Spam-Report-Type" if unset
     */
    @ZAttr(id=466)
    public String getSpamReportTypeHeader() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeHeader, "X-Zimbra-Spam-Report-Type");
    }

    /**
     * mail header name for report type in spam report
     *
     * @param zimbraSpamReportTypeHeader new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=466)
    public void setSpamReportTypeHeader(String zimbraSpamReportTypeHeader) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHeader, zimbraSpamReportTypeHeader);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail header name for report type in spam report
     *
     * @param zimbraSpamReportTypeHeader new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=466)
    public Map<String,Object> setSpamReportTypeHeader(String zimbraSpamReportTypeHeader, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHeader, zimbraSpamReportTypeHeader);
        return attrs;
    }

    /**
     * mail header name for report type in spam report
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=466)
    public void unsetSpamReportTypeHeader() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHeader, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail header name for report type in spam report
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=466)
    public Map<String,Object> unsetSpamReportTypeHeader(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeHeader, "");
        return attrs;
    }

    /**
     * spam report type value for spam
     *
     * @return zimbraSpamReportTypeSpam, or "spam" if unset
     */
    @ZAttr(id=467)
    public String getSpamReportTypeSpam() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeSpam, "spam");
    }

    /**
     * spam report type value for spam
     *
     * @param zimbraSpamReportTypeSpam new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=467)
    public void setSpamReportTypeSpam(String zimbraSpamReportTypeSpam) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeSpam, zimbraSpamReportTypeSpam);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * spam report type value for spam
     *
     * @param zimbraSpamReportTypeSpam new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=467)
    public Map<String,Object> setSpamReportTypeSpam(String zimbraSpamReportTypeSpam, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeSpam, zimbraSpamReportTypeSpam);
        return attrs;
    }

    /**
     * spam report type value for spam
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=467)
    public void unsetSpamReportTypeSpam() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeSpam, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * spam report type value for spam
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=467)
    public Map<String,Object> unsetSpamReportTypeSpam(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamReportTypeSpam, "");
        return attrs;
    }

    /**
     * Subject prefix for spam messages
     *
     * @return zimbraSpamSubjectTag, or null if unset
     */
    @ZAttr(id=203)
    public String getSpamSubjectTag() {
        return getAttr(Provisioning.A_zimbraSpamSubjectTag, null);
    }

    /**
     * Subject prefix for spam messages
     *
     * @param zimbraSpamSubjectTag new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=203)
    public void setSpamSubjectTag(String zimbraSpamSubjectTag) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamSubjectTag, zimbraSpamSubjectTag);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Subject prefix for spam messages
     *
     * @param zimbraSpamSubjectTag new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=203)
    public Map<String,Object> setSpamSubjectTag(String zimbraSpamSubjectTag, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamSubjectTag, zimbraSpamSubjectTag);
        return attrs;
    }

    /**
     * Subject prefix for spam messages
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=203)
    public void unsetSpamSubjectTag() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamSubjectTag, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Subject prefix for spam messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=203)
    public Map<String,Object> unsetSpamSubjectTag(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamSubjectTag, "");
        return attrs;
    }

    /**
     * Spaminess percentage beyound which a message is marked as spam
     *
     * @return zimbraSpamTagPercent, or 33 if unset
     */
    @ZAttr(id=204)
    public int getSpamTagPercent() {
        return getIntAttr(Provisioning.A_zimbraSpamTagPercent, 33);
    }

    /**
     * Spaminess percentage beyound which a message is marked as spam
     *
     * @param zimbraSpamTagPercent new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=204)
    public void setSpamTagPercent(int zimbraSpamTagPercent) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTagPercent, Integer.toString(zimbraSpamTagPercent));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Spaminess percentage beyound which a message is marked as spam
     *
     * @param zimbraSpamTagPercent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=204)
    public Map<String,Object> setSpamTagPercent(int zimbraSpamTagPercent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTagPercent, Integer.toString(zimbraSpamTagPercent));
        return attrs;
    }

    /**
     * Spaminess percentage beyound which a message is marked as spam
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=204)
    public void unsetSpamTagPercent() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTagPercent, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Spaminess percentage beyound which a message is marked as spam
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=204)
    public Map<String,Object> unsetSpamTagPercent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamTagPercent, "");
        return attrs;
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @return zimbraSpellCheckURL, or ampty array if unset
     */
    @ZAttr(id=267)
    public String[] getSpellCheckURL() {
        return getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param zimbraSpellCheckURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=267)
    public void setSpellCheckURL(String[] zimbraSpellCheckURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpellCheckURL, zimbraSpellCheckURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param zimbraSpellCheckURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=267)
    public Map<String,Object> setSpellCheckURL(String[] zimbraSpellCheckURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpellCheckURL, zimbraSpellCheckURL);
        return attrs;
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param zimbraSpellCheckURL new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=267)
    public void addSpellCheckURL(String zimbraSpellCheckURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSpellCheckURL, zimbraSpellCheckURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param zimbraSpellCheckURL new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=267)
    public Map<String,Object> addSpellCheckURL(String zimbraSpellCheckURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraSpellCheckURL, zimbraSpellCheckURL);
        return attrs;
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param zimbraSpellCheckURL existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=267)
    public void removeSpellCheckURL(String zimbraSpellCheckURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSpellCheckURL, zimbraSpellCheckURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param zimbraSpellCheckURL existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=267)
    public Map<String,Object> removeSpellCheckURL(String zimbraSpellCheckURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraSpellCheckURL, zimbraSpellCheckURL);
        return attrs;
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=267)
    public void unsetSpellCheckURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpellCheckURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=267)
    public Map<String,Object> unsetSpellCheckURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpellCheckURL, "");
        return attrs;
    }

    /**
     * CA Cert used to sign all self signed certs
     *
     * @return zimbraSslCaCert, or null if unset
     */
    @ZAttr(id=277)
    public String getSslCaCert() {
        return getAttr(Provisioning.A_zimbraSslCaCert, null);
    }

    /**
     * CA Cert used to sign all self signed certs
     *
     * @param zimbraSslCaCert new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=277)
    public void setSslCaCert(String zimbraSslCaCert) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaCert, zimbraSslCaCert);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * CA Cert used to sign all self signed certs
     *
     * @param zimbraSslCaCert new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=277)
    public Map<String,Object> setSslCaCert(String zimbraSslCaCert, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaCert, zimbraSslCaCert);
        return attrs;
    }

    /**
     * CA Cert used to sign all self signed certs
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=277)
    public void unsetSslCaCert() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaCert, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * CA Cert used to sign all self signed certs
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=277)
    public Map<String,Object> unsetSslCaCert(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaCert, "");
        return attrs;
    }

    /**
     * CA Key used to sign all self signed certs
     *
     * @return zimbraSslCaKey, or null if unset
     */
    @ZAttr(id=278)
    public String getSslCaKey() {
        return getAttr(Provisioning.A_zimbraSslCaKey, null);
    }

    /**
     * CA Key used to sign all self signed certs
     *
     * @param zimbraSslCaKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=278)
    public void setSslCaKey(String zimbraSslCaKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaKey, zimbraSslCaKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * CA Key used to sign all self signed certs
     *
     * @param zimbraSslCaKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=278)
    public Map<String,Object> setSslCaKey(String zimbraSslCaKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaKey, zimbraSslCaKey);
        return attrs;
    }

    /**
     * CA Key used to sign all self signed certs
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=278)
    public void unsetSslCaKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * CA Key used to sign all self signed certs
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=278)
    public Map<String,Object> unsetSslCaKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSslCaKey, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     *
     * @return zimbraTableMaintenanceGrowthFactor, or 10 if unset
     */
    @ZAttr(id=171)
    public int getTableMaintenanceGrowthFactor() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceGrowthFactor, 10);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     *
     * @param zimbraTableMaintenanceGrowthFactor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=171)
    public void setTableMaintenanceGrowthFactor(int zimbraTableMaintenanceGrowthFactor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceGrowthFactor, Integer.toString(zimbraTableMaintenanceGrowthFactor));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     *
     * @param zimbraTableMaintenanceGrowthFactor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=171)
    public Map<String,Object> setTableMaintenanceGrowthFactor(int zimbraTableMaintenanceGrowthFactor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceGrowthFactor, Integer.toString(zimbraTableMaintenanceGrowthFactor));
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=171)
    public void unsetTableMaintenanceGrowthFactor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceGrowthFactor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=171)
    public Map<String,Object> unsetTableMaintenanceGrowthFactor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceGrowthFactor, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     *
     * @return zimbraTableMaintenanceMaxRows, or 1000000 if unset
     */
    @ZAttr(id=169)
    public int getTableMaintenanceMaxRows() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceMaxRows, 1000000);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     *
     * @param zimbraTableMaintenanceMaxRows new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=169)
    public void setTableMaintenanceMaxRows(int zimbraTableMaintenanceMaxRows) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMaxRows, Integer.toString(zimbraTableMaintenanceMaxRows));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     *
     * @param zimbraTableMaintenanceMaxRows new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=169)
    public Map<String,Object> setTableMaintenanceMaxRows(int zimbraTableMaintenanceMaxRows, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMaxRows, Integer.toString(zimbraTableMaintenanceMaxRows));
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=169)
    public void unsetTableMaintenanceMaxRows() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMaxRows, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=169)
    public Map<String,Object> unsetTableMaintenanceMaxRows(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMaxRows, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     *
     * @return zimbraTableMaintenanceMinRows, or 10000 if unset
     */
    @ZAttr(id=168)
    public int getTableMaintenanceMinRows() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceMinRows, 10000);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     *
     * @param zimbraTableMaintenanceMinRows new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=168)
    public void setTableMaintenanceMinRows(int zimbraTableMaintenanceMinRows) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMinRows, Integer.toString(zimbraTableMaintenanceMinRows));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     *
     * @param zimbraTableMaintenanceMinRows new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=168)
    public Map<String,Object> setTableMaintenanceMinRows(int zimbraTableMaintenanceMinRows, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMinRows, Integer.toString(zimbraTableMaintenanceMinRows));
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=168)
    public void unsetTableMaintenanceMinRows() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMinRows, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=168)
    public Map<String,Object> unsetTableMaintenanceMinRows(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceMinRows, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @return zimbraTableMaintenanceOperation, or ZAttrProvisioning.TableMaintenanceOperation.ANALYZE if unset and/or has invalid value
     */
    @ZAttr(id=170)
    public ZAttrProvisioning.TableMaintenanceOperation getTableMaintenanceOperation() {
        try { String v = getAttr(Provisioning.A_zimbraTableMaintenanceOperation); return v == null ? ZAttrProvisioning.TableMaintenanceOperation.ANALYZE : ZAttrProvisioning.TableMaintenanceOperation.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return ZAttrProvisioning.TableMaintenanceOperation.ANALYZE; }
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @return zimbraTableMaintenanceOperation, or "ANALYZE" if unset
     */
    @ZAttr(id=170)
    public String getTableMaintenanceOperationAsString() {
        return getAttr(Provisioning.A_zimbraTableMaintenanceOperation, "ANALYZE");
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @param zimbraTableMaintenanceOperation new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=170)
    public void setTableMaintenanceOperation(ZAttrProvisioning.TableMaintenanceOperation zimbraTableMaintenanceOperation) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceOperation, zimbraTableMaintenanceOperation.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @param zimbraTableMaintenanceOperation new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=170)
    public Map<String,Object> setTableMaintenanceOperation(ZAttrProvisioning.TableMaintenanceOperation zimbraTableMaintenanceOperation, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceOperation, zimbraTableMaintenanceOperation.toString());
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @param zimbraTableMaintenanceOperation new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=170)
    public void setTableMaintenanceOperationAsString(String zimbraTableMaintenanceOperation) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceOperation, zimbraTableMaintenanceOperation);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @param zimbraTableMaintenanceOperation new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=170)
    public Map<String,Object> setTableMaintenanceOperationAsString(String zimbraTableMaintenanceOperation, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceOperation, zimbraTableMaintenanceOperation);
        return attrs;
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=170)
    public void unsetTableMaintenanceOperation() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceOperation, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=170)
    public Map<String,Object> unsetTableMaintenanceOperation(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTableMaintenanceOperation, "");
        return attrs;
    }

    /**
     * Whether to block archive files that are password protected or
     * encrypted
     *
     * @return zimbraVirusBlockEncryptedArchive, or true if unset
     */
    @ZAttr(id=205)
    public boolean isVirusBlockEncryptedArchive() {
        return getBooleanAttr(Provisioning.A_zimbraVirusBlockEncryptedArchive, true);
    }

    /**
     * Whether to block archive files that are password protected or
     * encrypted
     *
     * @param zimbraVirusBlockEncryptedArchive new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=205)
    public void setVirusBlockEncryptedArchive(boolean zimbraVirusBlockEncryptedArchive) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusBlockEncryptedArchive, zimbraVirusBlockEncryptedArchive ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to block archive files that are password protected or
     * encrypted
     *
     * @param zimbraVirusBlockEncryptedArchive new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=205)
    public Map<String,Object> setVirusBlockEncryptedArchive(boolean zimbraVirusBlockEncryptedArchive, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusBlockEncryptedArchive, zimbraVirusBlockEncryptedArchive ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to block archive files that are password protected or
     * encrypted
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=205)
    public void unsetVirusBlockEncryptedArchive() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusBlockEncryptedArchive, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to block archive files that are password protected or
     * encrypted
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=205)
    public Map<String,Object> unsetVirusBlockEncryptedArchive(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusBlockEncryptedArchive, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable virus checking
     *
     * @return zimbraVirusCheckEnabled, or false if unset
     */
    @ZAttr(id=206)
    public boolean isVirusCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraVirusCheckEnabled, false);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable virus checking
     *
     * @param zimbraVirusCheckEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=206)
    public void setVirusCheckEnabled(boolean zimbraVirusCheckEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusCheckEnabled, zimbraVirusCheckEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable virus checking
     *
     * @param zimbraVirusCheckEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=206)
    public Map<String,Object> setVirusCheckEnabled(boolean zimbraVirusCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusCheckEnabled, zimbraVirusCheckEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable virus checking
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=206)
    public void unsetVirusCheckEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusCheckEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable virus checking
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=206)
    public Map<String,Object> unsetVirusCheckEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusCheckEnabled, "");
        return attrs;
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * <p>Use getVirusDefinitionsUpdateFrequencyAsString to access value as a string.
     *
     * @see #getVirusDefinitionsUpdateFrequencyAsString()
     *
     * @return zimbraVirusDefinitionsUpdateFrequency in millseconds, or 7200000 (2h)  if unset
     */
    @ZAttr(id=191)
    public long getVirusDefinitionsUpdateFrequency() {
        return getTimeInterval(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, 7200000L);
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @return zimbraVirusDefinitionsUpdateFrequency, or "2h" if unset
     */
    @ZAttr(id=191)
    public String getVirusDefinitionsUpdateFrequencyAsString() {
        return getAttr(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, "2h");
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @param zimbraVirusDefinitionsUpdateFrequency new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=191)
    public void setVirusDefinitionsUpdateFrequency(String zimbraVirusDefinitionsUpdateFrequency) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, zimbraVirusDefinitionsUpdateFrequency);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @param zimbraVirusDefinitionsUpdateFrequency new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=191)
    public Map<String,Object> setVirusDefinitionsUpdateFrequency(String zimbraVirusDefinitionsUpdateFrequency, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, zimbraVirusDefinitionsUpdateFrequency);
        return attrs;
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=191)
    public void unsetVirusDefinitionsUpdateFrequency() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=191)
    public Map<String,Object> unsetVirusDefinitionsUpdateFrequency(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, "");
        return attrs;
    }

    /**
     * Whether to email admin on virus detection
     *
     * @return zimbraVirusWarnAdmin, or true if unset
     */
    @ZAttr(id=207)
    public boolean isVirusWarnAdmin() {
        return getBooleanAttr(Provisioning.A_zimbraVirusWarnAdmin, true);
    }

    /**
     * Whether to email admin on virus detection
     *
     * @param zimbraVirusWarnAdmin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=207)
    public void setVirusWarnAdmin(boolean zimbraVirusWarnAdmin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnAdmin, zimbraVirusWarnAdmin ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to email admin on virus detection
     *
     * @param zimbraVirusWarnAdmin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=207)
    public Map<String,Object> setVirusWarnAdmin(boolean zimbraVirusWarnAdmin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnAdmin, zimbraVirusWarnAdmin ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to email admin on virus detection
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=207)
    public void unsetVirusWarnAdmin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnAdmin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to email admin on virus detection
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=207)
    public Map<String,Object> unsetVirusWarnAdmin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnAdmin, "");
        return attrs;
    }

    /**
     * Whether to email recipient on virus detection
     *
     * @return zimbraVirusWarnRecipient, or true if unset
     */
    @ZAttr(id=208)
    public boolean isVirusWarnRecipient() {
        return getBooleanAttr(Provisioning.A_zimbraVirusWarnRecipient, true);
    }

    /**
     * Whether to email recipient on virus detection
     *
     * @param zimbraVirusWarnRecipient new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=208)
    public void setVirusWarnRecipient(boolean zimbraVirusWarnRecipient) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnRecipient, zimbraVirusWarnRecipient ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to email recipient on virus detection
     *
     * @param zimbraVirusWarnRecipient new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=208)
    public Map<String,Object> setVirusWarnRecipient(boolean zimbraVirusWarnRecipient, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnRecipient, zimbraVirusWarnRecipient ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether to email recipient on virus detection
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=208)
    public void unsetVirusWarnRecipient() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnRecipient, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to email recipient on virus detection
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=208)
    public Map<String,Object> unsetVirusWarnRecipient(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusWarnRecipient, "");
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
     * Enable XMPP support for IM
     *
     * @return zimbraXMPPEnabled, or true if unset
     */
    @ZAttr(id=397)
    public boolean isXMPPEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraXMPPEnabled, true);
    }

    /**
     * Enable XMPP support for IM
     *
     * @param zimbraXMPPEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=397)
    public void setXMPPEnabled(boolean zimbraXMPPEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPEnabled, zimbraXMPPEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable XMPP support for IM
     *
     * @param zimbraXMPPEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=397)
    public Map<String,Object> setXMPPEnabled(boolean zimbraXMPPEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPEnabled, zimbraXMPPEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable XMPP support for IM
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=397)
    public void unsetXMPPEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable XMPP support for IM
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=397)
    public Map<String,Object> unsetXMPPEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPEnabled, "");
        return attrs;
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @return zimbraXMPPServerDialbackKey, or ampty array if unset
     */
    @ZAttr(id=695)
    public String[] getXMPPServerDialbackKey() {
        return getMultiAttr(Provisioning.A_zimbraXMPPServerDialbackKey);
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param zimbraXMPPServerDialbackKey new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=695)
    public void setXMPPServerDialbackKey(String[] zimbraXMPPServerDialbackKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPServerDialbackKey, zimbraXMPPServerDialbackKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param zimbraXMPPServerDialbackKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=695)
    public Map<String,Object> setXMPPServerDialbackKey(String[] zimbraXMPPServerDialbackKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPServerDialbackKey, zimbraXMPPServerDialbackKey);
        return attrs;
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param zimbraXMPPServerDialbackKey new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=695)
    public void addXMPPServerDialbackKey(String zimbraXMPPServerDialbackKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraXMPPServerDialbackKey, zimbraXMPPServerDialbackKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param zimbraXMPPServerDialbackKey new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=695)
    public Map<String,Object> addXMPPServerDialbackKey(String zimbraXMPPServerDialbackKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraXMPPServerDialbackKey, zimbraXMPPServerDialbackKey);
        return attrs;
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param zimbraXMPPServerDialbackKey existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=695)
    public void removeXMPPServerDialbackKey(String zimbraXMPPServerDialbackKey) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraXMPPServerDialbackKey, zimbraXMPPServerDialbackKey);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param zimbraXMPPServerDialbackKey existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=695)
    public Map<String,Object> removeXMPPServerDialbackKey(String zimbraXMPPServerDialbackKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraXMPPServerDialbackKey, zimbraXMPPServerDialbackKey);
        return attrs;
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=695)
    public void unsetXMPPServerDialbackKey() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPServerDialbackKey, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=695)
    public Map<String,Object> unsetXMPPServerDialbackKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraXMPPServerDialbackKey, "");
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

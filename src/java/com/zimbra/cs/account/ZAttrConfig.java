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

    /* build: 5.0 pshao 20081118-1208 */

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
        return getMultiAttr(Provisioning.A_zimbraAccountExtraObjectClass);
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
     * SSL port for admin UI
     *
     * @return zimbraAdminPort, or null unset
     */
    @ZAttr(id=155)
    public String getAdminPort() {
        return getAttr(Provisioning.A_zimbraAdminPort);
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
     * @return zimbraAdminURL, or null unset
     */
    @ZAttr(id=497)
    public String getAdminURL() {
        return getAttr(Provisioning.A_zimbraAdminURL);
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
     * @return zimbraArchiveMailFrom, or null unset
     */
    @ZAttr(id=430)
    public String getArchiveMailFrom() {
        return getAttr(Provisioning.A_zimbraArchiveMailFrom);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=115)
    public Map<String,Object> setAttachmentsBlocked(boolean zimbraAttachmentsBlocked, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, Boolean.toString(zimbraAttachmentsBlocked));
        return attrs;
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
     * @return zimbraAttachmentsIndexedTextLimit, or -1 if unset
     */
    @ZAttr(id=582)
    public int getAttachmentsIndexedTextLimit() {
        return getIntAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, -1);
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
     * @return zimbraAttachmentsScanClass, or null unset
     */
    @ZAttr(id=238)
    public String getAttachmentsScanClass() {
        return getAttr(Provisioning.A_zimbraAttachmentsScanClass);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=237)
    public Map<String,Object> setAttachmentsScanEnabled(boolean zimbraAttachmentsScanEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsScanEnabled, Boolean.toString(zimbraAttachmentsScanEnabled));
        return attrs;
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
     * @return zimbraAttachmentsScanURL, or null unset
     */
    @ZAttr(id=239)
    public String getAttachmentsScanURL() {
        return getAttr(Provisioning.A_zimbraAttachmentsScanURL);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=116)
    public Map<String,Object> setAttachmentsViewInHtmlOnly(boolean zimbraAttachmentsViewInHtmlOnly, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, Boolean.toString(zimbraAttachmentsViewInHtmlOnly));
        return attrs;
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
     * @return zimbraAutoSubmittedNullReturnPath, or false if unset
     */
    @ZAttr(id=502)
    public boolean isAutoSubmittedNullReturnPath() {
        return getBooleanAttr(Provisioning.A_zimbraAutoSubmittedNullReturnPath, false);
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
        attrs.put(Provisioning.A_zimbraAutoSubmittedNullReturnPath, Boolean.toString(zimbraAutoSubmittedNullReturnPath));
        return attrs;
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
     * @return zimbraBackupAutoGroupedInterval, or null unset
     */
    @ZAttr(id=513)
    public String getBackupAutoGroupedInterval() {
        return getAttr(Provisioning.A_zimbraBackupAutoGroupedInterval);
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
     * @return zimbraBackupAutoGroupedNumGroups, or -1 if unset
     */
    @ZAttr(id=514)
    public int getBackupAutoGroupedNumGroups() {
        return getIntAttr(Provisioning.A_zimbraBackupAutoGroupedNumGroups, -1);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=515)
    public Map<String,Object> setBackupAutoGroupedThrottled(boolean zimbraBackupAutoGroupedThrottled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBackupAutoGroupedThrottled, Boolean.toString(zimbraBackupAutoGroupedThrottled));
        return attrs;
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
     * @return zimbraBackupMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=512)
    public ZAttrProvisioning.BackupMode getBackupMode() {
        try { String v = getAttr(Provisioning.A_zimbraBackupMode); return v == null ? null : ZAttrProvisioning.BackupMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @return zimbraBackupMode, or null unset
     */
    @ZAttr(id=512)
    public String getBackupModeAsString() {
        return getAttr(Provisioning.A_zimbraBackupMode);
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
     * @return zimbraBackupReportEmailSender, or null unset
     */
    @ZAttr(id=460)
    public String getBackupReportEmailSender() {
        return getAttr(Provisioning.A_zimbraBackupReportEmailSender);
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
     * @return zimbraBackupReportEmailSubjectPrefix, or null unset
     */
    @ZAttr(id=461)
    public String getBackupReportEmailSubjectPrefix() {
        return getAttr(Provisioning.A_zimbraBackupReportEmailSubjectPrefix);
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
     * @return zimbraBackupTarget, or null unset
     */
    @ZAttr(id=458)
    public String getBackupTarget() {
        return getAttr(Provisioning.A_zimbraBackupTarget);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=690)
    public Map<String,Object> setCalendarCalDavDisableFreebusy(boolean zimbraCalendarCalDavDisableFreebusy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, Boolean.toString(zimbraCalendarCalDavDisableFreebusy));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=652)
    public Map<String,Object> setCalendarCalDavDisableScheduling(boolean zimbraCalendarCalDavDisableScheduling, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCalDavDisableScheduling, Boolean.toString(zimbraCalendarCalDavDisableScheduling));
        return attrs;
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
     * @return zimbraCalendarCompatibilityMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=243)
    public ZAttrProvisioning.CalendarCompatibilityMode getCalendarCompatibilityMode() {
        try { String v = getAttr(Provisioning.A_zimbraCalendarCompatibilityMode); return v == null ? null : ZAttrProvisioning.CalendarCompatibilityMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @return zimbraCalendarCompatibilityMode, or null unset
     */
    @ZAttr(id=243)
    public String getCalendarCompatibilityModeAsString() {
        return getAttr(Provisioning.A_zimbraCalendarCompatibilityMode);
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
     * @return zimbraCalendarRecurrenceDailyMaxDays, or -1 if unset
     */
    @ZAttr(id=661)
    public int getCalendarRecurrenceDailyMaxDays() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, -1);
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
     * @return zimbraCalendarRecurrenceMaxInstances, or -1 if unset
     */
    @ZAttr(id=660)
    public int getCalendarRecurrenceMaxInstances() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, -1);
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
     * @return zimbraCalendarRecurrenceMonthlyMaxMonths, or -1 if unset
     */
    @ZAttr(id=663)
    public int getCalendarRecurrenceMonthlyMaxMonths() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, -1);
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
     * @return zimbraCalendarRecurrenceOtherFrequencyMaxYears, or -1 if unset
     */
    @ZAttr(id=665)
    public int getCalendarRecurrenceOtherFrequencyMaxYears() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, -1);
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
     * @return zimbraCalendarRecurrenceWeeklyMaxWeeks, or -1 if unset
     */
    @ZAttr(id=662)
    public int getCalendarRecurrenceWeeklyMaxWeeks() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, -1);
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
     * @return zimbraCalendarRecurrenceYearlyMaxYears, or -1 if unset
     */
    @ZAttr(id=664)
    public int getCalendarRecurrenceYearlyMaxYears() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, -1);
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
        return getMultiAttr(Provisioning.A_zimbraCalendarResourceExtraObjectClass);
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
     * @return zimbraCertAuthorityCertSelfSigned, or null unset
     */
    @ZAttr(id=280)
    public String getCertAuthorityCertSelfSigned() {
        return getAttr(Provisioning.A_zimbraCertAuthorityCertSelfSigned);
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
     * @return zimbraCertAuthorityKeySelfSigned, or null unset
     */
    @ZAttr(id=279)
    public String getCertAuthorityKeySelfSigned() {
        return getAttr(Provisioning.A_zimbraCertAuthorityKeySelfSigned);
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
     * @return zimbraClusterType, or null if unset and/or has invalid value
     */
    @ZAttr(id=508)
    public ZAttrProvisioning.ClusterType getClusterType() {
        try { String v = getAttr(Provisioning.A_zimbraClusterType); return v == null ? null : ZAttrProvisioning.ClusterType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @return zimbraClusterType, or null unset
     */
    @ZAttr(id=508)
    public String getClusterTypeAsString() {
        return getAttr(Provisioning.A_zimbraClusterType);
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
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @return zimbraDefaultDomainName, or null unset
     */
    @ZAttr(id=172)
    public String getDefaultDomainName() {
        return getAttr(Provisioning.A_zimbraDefaultDomainName);
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
        return getMultiAttr(Provisioning.A_zimbraDomainExtraObjectClass);
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
     * Maximum size in bytes for attachments
     *
     * @return zimbraFileUploadMaxSize, or -1 if unset
     */
    @ZAttr(id=227)
    public long getFileUploadMaxSize() {
        return getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, -1);
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
     * LDAP search filter definitions for GAL queries
     *
     * @return zimbraGalLdapFilterDef, or ampty array if unset
     */
    @ZAttr(id=52)
    public String[] getGalLdapFilterDef() {
        return getMultiAttr(Provisioning.A_zimbraGalLdapFilterDef);
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
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * <p>Use getHsmAgeAsString to access value as a string.
     *
     * @see #getHsmAgeAsString()
     *
     * @return zimbraHsmAge in millseconds, or -1 if unset
     */
    @ZAttr(id=8)
    public long getHsmAge() {
        return getTimeInterval(Provisioning.A_zimbraHsmAge, -1);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @return zimbraHsmAge, or null unset
     */
    @ZAttr(id=8)
    public String getHsmAgeAsString() {
        return getAttr(Provisioning.A_zimbraHsmAge);
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
     * @return zimbraHttpNumThreads, or -1 if unset
     */
    @ZAttr(id=518)
    public int getHttpNumThreads() {
        return getIntAttr(Provisioning.A_zimbraHttpNumThreads, -1);
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
     * @return zimbraHttpSSLNumThreads, or -1 if unset
     */
    @ZAttr(id=519)
    public int getHttpSSLNumThreads() {
        return getIntAttr(Provisioning.A_zimbraHttpSSLNumThreads, -1);
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
     * @return zimbraImapBindOnStartup, or false if unset
     */
    @ZAttr(id=268)
    public boolean isImapBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraImapBindOnStartup, false);
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
        attrs.put(Provisioning.A_zimbraImapBindOnStartup, Boolean.toString(zimbraImapBindOnStartup));
        return attrs;
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
     * @return zimbraImapBindPort, or null unset
     */
    @ZAttr(id=180)
    public String getImapBindPort() {
        return getAttr(Provisioning.A_zimbraImapBindPort);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=185)
    public Map<String,Object> setImapCleartextLoginEnabled(boolean zimbraImapCleartextLoginEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapCleartextLoginEnabled, Boolean.toString(zimbraImapCleartextLoginEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=693)
    public Map<String,Object> setImapExposeVersionOnBanner(boolean zimbraImapExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapExposeVersionOnBanner, Boolean.toString(zimbraImapExposeVersionOnBanner));
        return attrs;
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
     * @return zimbraImapNumThreads, or -1 if unset
     */
    @ZAttr(id=181)
    public int getImapNumThreads() {
        return getIntAttr(Provisioning.A_zimbraImapNumThreads, -1);
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
     * @return zimbraImapProxyBindPort, or null unset
     */
    @ZAttr(id=348)
    public String getImapProxyBindPort() {
        return getAttr(Provisioning.A_zimbraImapProxyBindPort);
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
     * @return zimbraImapSSLBindOnStartup, or false if unset
     */
    @ZAttr(id=269)
    public boolean isImapSSLBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraImapSSLBindOnStartup, false);
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
        attrs.put(Provisioning.A_zimbraImapSSLBindOnStartup, Boolean.toString(zimbraImapSSLBindOnStartup));
        return attrs;
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
     * @return zimbraImapSSLBindPort, or null unset
     */
    @ZAttr(id=183)
    public String getImapSSLBindPort() {
        return getAttr(Provisioning.A_zimbraImapSSLBindPort);
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
     * @return zimbraImapSSLProxyBindPort, or null unset
     */
    @ZAttr(id=349)
    public String getImapSSLProxyBindPort() {
        return getAttr(Provisioning.A_zimbraImapSSLProxyBindPort);
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
     * @return zimbraImapSSLServerEnabled, or false if unset
     */
    @ZAttr(id=184)
    public boolean isImapSSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapSSLServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraImapSSLServerEnabled, Boolean.toString(zimbraImapSSLServerEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=555)
    public Map<String,Object> setImapSaslGssapiEnabled(boolean zimbraImapSaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSaslGssapiEnabled, Boolean.toString(zimbraImapSaslGssapiEnabled));
        return attrs;
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
     * @return zimbraImapServerEnabled, or false if unset
     */
    @ZAttr(id=176)
    public boolean isImapServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraImapServerEnabled, Boolean.toString(zimbraImapServerEnabled));
        return attrs;
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
     * @return zimbraLastLogonTimestampFrequency in millseconds, or -1 if unset
     */
    @ZAttr(id=114)
    public long getLastLogonTimestampFrequency() {
        return getTimeInterval(Provisioning.A_zimbraLastLogonTimestampFrequency, -1);
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @return zimbraLastLogonTimestampFrequency, or null unset
     */
    @ZAttr(id=114)
    public String getLastLogonTimestampFrequencyAsString() {
        return getAttr(Provisioning.A_zimbraLastLogonTimestampFrequency);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=270)
    public Map<String,Object> setLmtpBindOnStartup(boolean zimbraLmtpBindOnStartup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindOnStartup, Boolean.toString(zimbraLmtpBindOnStartup));
        return attrs;
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
     * @return zimbraLmtpBindPort, or null unset
     */
    @ZAttr(id=24)
    public String getLmtpBindPort() {
        return getAttr(Provisioning.A_zimbraLmtpBindPort);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=691)
    public Map<String,Object> setLmtpExposeVersionOnBanner(boolean zimbraLmtpExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpExposeVersionOnBanner, Boolean.toString(zimbraLmtpExposeVersionOnBanner));
        return attrs;
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
     * @return zimbraLmtpNumThreads, or -1 if unset
     */
    @ZAttr(id=26)
    public int getLmtpNumThreads() {
        return getIntAttr(Provisioning.A_zimbraLmtpNumThreads, -1);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=657)
    public Map<String,Object> setLmtpPermanentFailureWhenOverQuota(boolean zimbraLmtpPermanentFailureWhenOverQuota, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, Boolean.toString(zimbraLmtpPermanentFailureWhenOverQuota));
        return attrs;
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
     * @return zimbraLmtpServerEnabled, or false if unset
     */
    @ZAttr(id=630)
    public boolean isLmtpServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraLmtpServerEnabled, Boolean.toString(zimbraLmtpServerEnabled));
        return attrs;
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
     * @return zimbraLogRawLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=263)
    public long getLogRawLifetime() {
        return getTimeInterval(Provisioning.A_zimbraLogRawLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @return zimbraLogRawLifetime, or null unset
     */
    @ZAttr(id=263)
    public String getLogRawLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraLogRawLifetime);
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
     * @return zimbraLogSummaryLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=264)
    public long getLogSummaryLifetime() {
        return getTimeInterval(Provisioning.A_zimbraLogSummaryLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @return zimbraLogSummaryLifetime, or null unset
     */
    @ZAttr(id=264)
    public String getLogSummaryLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraLogSummaryLifetime);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=520)
    public Map<String,Object> setLogToSyslog(boolean zimbraLogToSyslog, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLogToSyslog, Boolean.toString(zimbraLogToSyslog));
        return attrs;
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
     * @return zimbraMailDiskStreamingThreshold, or -1 if unset
     */
    @ZAttr(id=565)
    public int getMailDiskStreamingThreshold() {
        return getIntAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, -1);
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
     * @return zimbraMailMode, or null unset
     */
    @ZAttr(id=308)
    public String getMailModeAsString() {
        return getAttr(Provisioning.A_zimbraMailMode);
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
     * @return zimbraMailPort, or null unset
     */
    @ZAttr(id=154)
    public String getMailPort() {
        return getAttr(Provisioning.A_zimbraMailPort);
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
     * @return zimbraMailProxyPort, or null unset
     */
    @ZAttr(id=626)
    public String getMailProxyPort() {
        return getAttr(Provisioning.A_zimbraMailProxyPort);
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
     * @return zimbraMailPurgeSleepInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=542)
    public long getMailPurgeSleepInterval() {
        return getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, -1);
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @return zimbraMailPurgeSleepInterval, or null unset
     */
    @ZAttr(id=542)
    public String getMailPurgeSleepIntervalAsString() {
        return getAttr(Provisioning.A_zimbraMailPurgeSleepInterval);
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
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @return zimbraMailReferMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=613)
    public ZAttrProvisioning.MailReferMode getMailReferMode() {
        try { String v = getAttr(Provisioning.A_zimbraMailReferMode); return v == null ? null : ZAttrProvisioning.MailReferMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
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
     * @return zimbraMailReferMode, or null unset
     */
    @ZAttr(id=613)
    public String getMailReferModeAsString() {
        return getAttr(Provisioning.A_zimbraMailReferMode);
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
     * @return zimbraMailSSLPort, or null unset
     */
    @ZAttr(id=166)
    public String getMailSSLPort() {
        return getAttr(Provisioning.A_zimbraMailSSLPort);
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
     * @return zimbraMailSSLProxyPort, or null unset
     */
    @ZAttr(id=627)
    public String getMailSSLProxyPort() {
        return getAttr(Provisioning.A_zimbraMailSSLProxyPort);
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
     * @return zimbraMailURL, or null unset
     */
    @ZAttr(id=340)
    public String getMailURL() {
        return getAttr(Provisioning.A_zimbraMailURL);
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
     * @return zimbraMemcachedBindPort, or null unset
     */
    @ZAttr(id=580)
    public String getMemcachedBindPort() {
        return getAttr(Provisioning.A_zimbraMemcachedBindPort);
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
     * @return zimbraMessageCacheSize, or -1 if unset
     */
    @ZAttr(id=297)
    public int getMessageCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMessageCacheSize, -1);
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
     * @return zimbraMessageIdDedupeCacheSize, or -1 if unset
     */
    @ZAttr(id=334)
    public int getMessageIdDedupeCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMessageIdDedupeCacheSize, -1);
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
     * @return zimbraMtaAntiSpamLockMethod, or null unset
     */
    @ZAttr(id=612)
    public String getMtaAntiSpamLockMethod() {
        return getAttr(Provisioning.A_zimbraMtaAntiSpamLockMethod);
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
     * @return zimbraMtaAuthEnabled, or false if unset
     */
    @ZAttr(id=194)
    public boolean isMtaAuthEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
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
        attrs.put(Provisioning.A_zimbraMtaAuthEnabled, Boolean.toString(zimbraMtaAuthEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=505)
    public Map<String,Object> setMtaAuthTarget(boolean zimbraMtaAuthTarget, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMtaAuthTarget, Boolean.toString(zimbraMtaAuthTarget));
        return attrs;
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
        return getMultiAttr(Provisioning.A_zimbraMtaCommonBlockedExtension);
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
     * @return zimbraMtaDnsLookupsEnabled, or false if unset
     */
    @ZAttr(id=197)
    public boolean isMtaDnsLookupsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMtaDnsLookupsEnabled, false);
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
        attrs.put(Provisioning.A_zimbraMtaDnsLookupsEnabled, Boolean.toString(zimbraMtaDnsLookupsEnabled));
        return attrs;
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
     * @return zimbraMtaMaxMessageSize, or -1 if unset
     */
    @ZAttr(id=198)
    public int getMtaMaxMessageSize() {
        return getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, -1);
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
     * @return zimbraMtaMyDestination, or null unset
     */
    @ZAttr(id=524)
    public String getMtaMyDestination() {
        return getAttr(Provisioning.A_zimbraMtaMyDestination);
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
     * @return zimbraMtaMyHostname, or null unset
     */
    @ZAttr(id=509)
    public String getMtaMyHostname() {
        return getAttr(Provisioning.A_zimbraMtaMyHostname);
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
     * @return zimbraMtaMyOrigin, or null unset
     */
    @ZAttr(id=510)
    public String getMtaMyOrigin() {
        return getAttr(Provisioning.A_zimbraMtaMyOrigin);
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
     * @return zimbraMtaNonSmtpdMilters, or null unset
     */
    @ZAttr(id=673)
    public String getMtaNonSmtpdMilters() {
        return getAttr(Provisioning.A_zimbraMtaNonSmtpdMilters);
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
        return getMultiAttr(Provisioning.A_zimbraMtaRestriction);
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
     * @return zimbraMtaSmtpdMilters, or null unset
     */
    @ZAttr(id=672)
    public String getMtaSmtpdMilters() {
        return getAttr(Provisioning.A_zimbraMtaSmtpdMilters);
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
     * @return zimbraMtaTlsAuthOnly, or false if unset
     */
    @ZAttr(id=200)
    public boolean isMtaTlsAuthOnly() {
        return getBooleanAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);
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
        attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, Boolean.toString(zimbraMtaTlsAuthOnly));
        return attrs;
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
     * @return zimbraNetworkActivation, or null unset
     */
    @ZAttr(id=375)
    public String getNetworkActivation() {
        return getAttr(Provisioning.A_zimbraNetworkActivation);
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
     * @return zimbraNetworkLicense, or null unset
     */
    @ZAttr(id=374)
    public String getNetworkLicense() {
        return getAttr(Provisioning.A_zimbraNetworkLicense);
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
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @return zimbraNotebookFolderCacheSize, or -1 if unset
     */
    @ZAttr(id=370)
    public int getNotebookFolderCacheSize() {
        return getIntAttr(Provisioning.A_zimbraNotebookFolderCacheSize, -1);
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
     * @return zimbraNotebookMaxCachedTemplatesPerFolder, or -1 if unset
     */
    @ZAttr(id=371)
    public int getNotebookMaxCachedTemplatesPerFolder() {
        return getIntAttr(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, -1);
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
     * @return zimbraNotebookPageCacheSize, or -1 if unset
     */
    @ZAttr(id=369)
    public int getNotebookPageCacheSize() {
        return getIntAttr(Provisioning.A_zimbraNotebookPageCacheSize, -1);
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
     * @return zimbraNotifyBindPort, or -1 if unset
     */
    @ZAttr(id=318)
    public int getNotifyBindPort() {
        return getIntAttr(Provisioning.A_zimbraNotifyBindPort, -1);
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
     * @return zimbraNotifySSLBindPort, or -1 if unset
     */
    @ZAttr(id=321)
    public int getNotifySSLBindPort() {
        return getIntAttr(Provisioning.A_zimbraNotifySSLBindPort, -1);
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
     * @return zimbraNotifySSLServerEnabled, or false if unset
     */
    @ZAttr(id=319)
    public boolean isNotifySSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraNotifySSLServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraNotifySSLServerEnabled, Boolean.toString(zimbraNotifySSLServerEnabled));
        return attrs;
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
     * @return zimbraNotifyServerEnabled, or false if unset
     */
    @ZAttr(id=316)
    public boolean isNotifyServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraNotifyServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraNotifyServerEnabled, Boolean.toString(zimbraNotifyServerEnabled));
        return attrs;
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
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraPop3BindOnStartup, or false if unset
     */
    @ZAttr(id=271)
    public boolean isPop3BindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraPop3BindOnStartup, false);
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
        attrs.put(Provisioning.A_zimbraPop3BindOnStartup, Boolean.toString(zimbraPop3BindOnStartup));
        return attrs;
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
     * @return zimbraPop3BindPort, or null unset
     */
    @ZAttr(id=94)
    public String getPop3BindPort() {
        return getAttr(Provisioning.A_zimbraPop3BindPort);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=189)
    public Map<String,Object> setPop3CleartextLoginEnabled(boolean zimbraPop3CleartextLoginEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3CleartextLoginEnabled, Boolean.toString(zimbraPop3CleartextLoginEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=692)
    public Map<String,Object> setPop3ExposeVersionOnBanner(boolean zimbraPop3ExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ExposeVersionOnBanner, Boolean.toString(zimbraPop3ExposeVersionOnBanner));
        return attrs;
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
     * @return zimbraPop3NumThreads, or -1 if unset
     */
    @ZAttr(id=96)
    public int getPop3NumThreads() {
        return getIntAttr(Provisioning.A_zimbraPop3NumThreads, -1);
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
     * @return zimbraPop3ProxyBindPort, or null unset
     */
    @ZAttr(id=350)
    public String getPop3ProxyBindPort() {
        return getAttr(Provisioning.A_zimbraPop3ProxyBindPort);
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
     * @return zimbraPop3SSLBindOnStartup, or false if unset
     */
    @ZAttr(id=272)
    public boolean isPop3SSLBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SSLBindOnStartup, false);
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
        attrs.put(Provisioning.A_zimbraPop3SSLBindOnStartup, Boolean.toString(zimbraPop3SSLBindOnStartup));
        return attrs;
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
     * @return zimbraPop3SSLBindPort, or null unset
     */
    @ZAttr(id=187)
    public String getPop3SSLBindPort() {
        return getAttr(Provisioning.A_zimbraPop3SSLBindPort);
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
     * @return zimbraPop3SSLProxyBindPort, or null unset
     */
    @ZAttr(id=351)
    public String getPop3SSLProxyBindPort() {
        return getAttr(Provisioning.A_zimbraPop3SSLProxyBindPort);
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
     * @return zimbraPop3SSLServerEnabled, or false if unset
     */
    @ZAttr(id=188)
    public boolean isPop3SSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SSLServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraPop3SSLServerEnabled, Boolean.toString(zimbraPop3SSLServerEnabled));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=554)
    public Map<String,Object> setPop3SaslGssapiEnabled(boolean zimbraPop3SaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SaslGssapiEnabled, Boolean.toString(zimbraPop3SaslGssapiEnabled));
        return attrs;
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
     * @return zimbraPop3ServerEnabled, or false if unset
     */
    @ZAttr(id=177)
    public boolean isPop3ServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3ServerEnabled, false);
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
        attrs.put(Provisioning.A_zimbraPop3ServerEnabled, Boolean.toString(zimbraPop3ServerEnabled));
        return attrs;
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
     * redolog rollover destination
     *
     * @return zimbraRedoLogArchiveDir, or null unset
     */
    @ZAttr(id=76)
    public String getRedoLogArchiveDir() {
        return getAttr(Provisioning.A_zimbraRedoLogArchiveDir);
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
     * @return zimbraRedoLogDeleteOnRollover, or false if unset
     */
    @ZAttr(id=251)
    public boolean isRedoLogDeleteOnRollover() {
        return getBooleanAttr(Provisioning.A_zimbraRedoLogDeleteOnRollover, false);
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
        attrs.put(Provisioning.A_zimbraRedoLogDeleteOnRollover, Boolean.toString(zimbraRedoLogDeleteOnRollover));
        return attrs;
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
     * @return zimbraRedoLogEnabled, or false if unset
     */
    @ZAttr(id=74)
    public boolean isRedoLogEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraRedoLogEnabled, false);
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
        attrs.put(Provisioning.A_zimbraRedoLogEnabled, Boolean.toString(zimbraRedoLogEnabled));
        return attrs;
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
     * @return zimbraRedoLogFsyncIntervalMS, or -1 if unset
     */
    @ZAttr(id=79)
    public int getRedoLogFsyncIntervalMS() {
        return getIntAttr(Provisioning.A_zimbraRedoLogFsyncIntervalMS, -1);
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
     * @return zimbraRedoLogLogPath, or null unset
     */
    @ZAttr(id=75)
    public String getRedoLogLogPath() {
        return getAttr(Provisioning.A_zimbraRedoLogLogPath);
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
     * @return zimbraRedoLogRolloverFileSizeKB, or -1 if unset
     */
    @ZAttr(id=78)
    public int getRedoLogRolloverFileSizeKB() {
        return getIntAttr(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, -1);
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
     * @return zimbraRemoteManagementCommand, or null unset
     */
    @ZAttr(id=336)
    public String getRemoteManagementCommand() {
        return getAttr(Provisioning.A_zimbraRemoteManagementCommand);
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
     * @return zimbraRemoteManagementPort, or -1 if unset
     */
    @ZAttr(id=339)
    public int getRemoteManagementPort() {
        return getIntAttr(Provisioning.A_zimbraRemoteManagementPort, -1);
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
     * @return zimbraRemoteManagementPrivateKeyPath, or null unset
     */
    @ZAttr(id=338)
    public String getRemoteManagementPrivateKeyPath() {
        return getAttr(Provisioning.A_zimbraRemoteManagementPrivateKeyPath);
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
     * @return zimbraRemoteManagementUser, or null unset
     */
    @ZAttr(id=337)
    public String getRemoteManagementUser() {
        return getAttr(Provisioning.A_zimbraRemoteManagementUser);
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
     * @return zimbraReverseProxyAdminPortAttribute, or null unset
     */
    @ZAttr(id=700)
    public String getReverseProxyAdminPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyAdminPortAttribute);
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
     * @return zimbraReverseProxyAuthWaitInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=569)
    public long getReverseProxyAuthWaitInterval() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyAuthWaitInterval, -1);
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @return zimbraReverseProxyAuthWaitInterval, or null unset
     */
    @ZAttr(id=569)
    public String getReverseProxyAuthWaitIntervalAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyAuthWaitInterval);
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
     * @return zimbraReverseProxyCacheEntryTTL in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public long getReverseProxyCacheEntryTTL() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheEntryTTL, -1);
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @return zimbraReverseProxyCacheEntryTTL, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public String getReverseProxyCacheEntryTTLAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheEntryTTL);
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
     * @return zimbraReverseProxyCacheFetchTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public long getReverseProxyCacheFetchTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, -1);
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @return zimbraReverseProxyCacheFetchTimeout, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public String getReverseProxyCacheFetchTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheFetchTimeout);
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
     * @return zimbraReverseProxyCacheReconnectInterval in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public long getReverseProxyCacheReconnectInterval() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, -1);
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @return zimbraReverseProxyCacheReconnectInterval, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public String getReverseProxyCacheReconnectIntervalAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheReconnectInterval);
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
     * @return zimbraReverseProxyDefaultRealm, or null unset
     */
    @ZAttr(id=703)
    public String getReverseProxyDefaultRealm() {
        return getAttr(Provisioning.A_zimbraReverseProxyDefaultRealm);
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
     * @return zimbraReverseProxyDomainNameAttribute, or null unset
     */
    @ZAttr(id=547)
    public String getReverseProxyDomainNameAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameAttribute);
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
     * @return zimbraReverseProxyDomainNameQuery, or null unset
     */
    @ZAttr(id=545)
    public String getReverseProxyDomainNameQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameQuery);
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
     * @return zimbraReverseProxyDomainNameSearchBase, or null unset
     */
    @ZAttr(id=546)
    public String getReverseProxyDomainNameSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameSearchBase);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=628)
    public Map<String,Object> setReverseProxyHttpEnabled(boolean zimbraReverseProxyHttpEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyHttpEnabled, Boolean.toString(zimbraReverseProxyHttpEnabled));
        return attrs;
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
     * @return zimbraReverseProxyHttpPortAttribute, or null unset
     */
    @ZAttr(id=632)
    public String getReverseProxyHttpPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute);
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
     * @return zimbraReverseProxyIPLoginLimit, or -1 if unset
     */
    @ZAttr(id=622)
    public int getReverseProxyIPLoginLimit() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyIPLoginLimit, -1);
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
     * @return zimbraReverseProxyIPLoginLimitTime, or -1 if unset
     */
    @ZAttr(id=623)
    public int getReverseProxyIPLoginLimitTime() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, -1);
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
        return getMultiAttr(Provisioning.A_zimbraReverseProxyImapEnabledCapability);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public Map<String,Object> setReverseProxyImapExposeVersionOnBanner(boolean zimbraReverseProxyImapExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, Boolean.toString(zimbraReverseProxyImapExposeVersionOnBanner));
        return attrs;
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
     * @return zimbraReverseProxyImapPortAttribute, or null unset
     */
    @ZAttr(id=479)
    public String getReverseProxyImapPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapPortAttribute);
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
     * @return zimbraReverseProxyImapSSLPortAttribute, or null unset
     */
    @ZAttr(id=480)
    public String getReverseProxyImapSSLPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=643)
    public Map<String,Object> setReverseProxyImapSaslGssapiEnabled(boolean zimbraReverseProxyImapSaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, Boolean.toString(zimbraReverseProxyImapSaslGssapiEnabled));
        return attrs;
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
     * @return zimbraReverseProxyImapSaslPlainEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public boolean isReverseProxyImapSaslPlainEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, false);
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
        attrs.put(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, Boolean.toString(zimbraReverseProxyImapSaslPlainEnabled));
        return attrs;
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
     * @return zimbraReverseProxyImapStartTlsMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=641)
    public ZAttrProvisioning.ReverseProxyImapStartTlsMode getReverseProxyImapStartTlsMode() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyImapStartTlsMode); return v == null ? null : ZAttrProvisioning.ReverseProxyImapStartTlsMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyImapStartTlsMode, or null unset
     */
    @ZAttr(id=641)
    public String getReverseProxyImapStartTlsModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapStartTlsMode);
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
     * @return zimbraReverseProxyInactivityTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public long getReverseProxyInactivityTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyInactivityTimeout, -1);
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @return zimbraReverseProxyInactivityTimeout, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public String getReverseProxyInactivityTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyInactivityTimeout);
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
     * @return zimbraReverseProxyIpThrottleMsg, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public String getReverseProxyIpThrottleMsg() {
        return getAttr(Provisioning.A_zimbraReverseProxyIpThrottleMsg);
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
     * @return zimbraReverseProxyLogLevel, or null if unset and/or has invalid value
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public ZAttrProvisioning.ReverseProxyLogLevel getReverseProxyLogLevel() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyLogLevel); return v == null ? null : ZAttrProvisioning.ReverseProxyLogLevel.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @return zimbraReverseProxyLogLevel, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public String getReverseProxyLogLevelAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyLogLevel);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=504)
    public Map<String,Object> setReverseProxyLookupTarget(boolean zimbraReverseProxyLookupTarget, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyLookupTarget, Boolean.toString(zimbraReverseProxyLookupTarget));
        return attrs;
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
     * @return zimbraReverseProxyMailEnabled, or false if unset
     */
    @ZAttr(id=629)
    public boolean isReverseProxyMailEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyMailEnabled, false);
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
        attrs.put(Provisioning.A_zimbraReverseProxyMailEnabled, Boolean.toString(zimbraReverseProxyMailEnabled));
        return attrs;
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
     * @return zimbraReverseProxyMailHostAttribute, or null unset
     */
    @ZAttr(id=474)
    public String getReverseProxyMailHostAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostAttribute);
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
     * @return zimbraReverseProxyMailHostQuery, or null unset
     */
    @ZAttr(id=472)
    public String getReverseProxyMailHostQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostQuery);
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
     * @return zimbraReverseProxyMailHostSearchBase, or null unset
     */
    @ZAttr(id=473)
    public String getReverseProxyMailHostSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostSearchBase);
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
     * @return zimbraReverseProxyMailMode, or null unset
     */
    @ZAttr(id=685)
    public String getReverseProxyMailModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailMode);
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
     * @return zimbraReverseProxyPassErrors, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public boolean isReverseProxyPassErrors() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPassErrors, false);
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
        attrs.put(Provisioning.A_zimbraReverseProxyPassErrors, Boolean.toString(zimbraReverseProxyPassErrors));
        return attrs;
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
        return getMultiAttr(Provisioning.A_zimbraReverseProxyPop3EnabledCapability);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public Map<String,Object> setReverseProxyPop3ExposeVersionOnBanner(boolean zimbraReverseProxyPop3ExposeVersionOnBanner, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, Boolean.toString(zimbraReverseProxyPop3ExposeVersionOnBanner));
        return attrs;
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
     * @return zimbraReverseProxyPop3PortAttribute, or null unset
     */
    @ZAttr(id=477)
    public String getReverseProxyPop3PortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3PortAttribute);
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
     * @return zimbraReverseProxyPop3SSLPortAttribute, or null unset
     */
    @ZAttr(id=478)
    public String getReverseProxyPop3SSLPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=644)
    public Map<String,Object> setReverseProxyPop3SaslGssapiEnabled(boolean zimbraReverseProxyPop3SaslGssapiEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, Boolean.toString(zimbraReverseProxyPop3SaslGssapiEnabled));
        return attrs;
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
     * @return zimbraReverseProxyPop3SaslPlainEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public boolean isReverseProxyPop3SaslPlainEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, false);
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
        attrs.put(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, Boolean.toString(zimbraReverseProxyPop3SaslPlainEnabled));
        return attrs;
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
     * @return zimbraReverseProxyPop3StartTlsMode, or null if unset and/or has invalid value
     */
    @ZAttr(id=642)
    public ZAttrProvisioning.ReverseProxyPop3StartTlsMode getReverseProxyPop3StartTlsMode() {
        try { String v = getAttr(Provisioning.A_zimbraReverseProxyPop3StartTlsMode); return v == null ? null : ZAttrProvisioning.ReverseProxyPop3StartTlsMode.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyPop3StartTlsMode, or null unset
     */
    @ZAttr(id=642)
    public String getReverseProxyPop3StartTlsModeAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3StartTlsMode);
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
     * @return zimbraReverseProxyPortQuery, or null unset
     */
    @ZAttr(id=475)
    public String getReverseProxyPortQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyPortQuery);
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
     * @return zimbraReverseProxyPortSearchBase, or null unset
     */
    @ZAttr(id=476)
    public String getReverseProxyPortSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyPortSearchBase);
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
     * @return zimbraReverseProxyRouteLookupTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public long getReverseProxyRouteLookupTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, -1);
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @return zimbraReverseProxyRouteLookupTimeout, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public String getReverseProxyRouteLookupTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyRouteLookupTimeout);
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
     * @return zimbraReverseProxySSLCiphers, or null unset
     */
    @ZAttr(id=640)
    public String getReverseProxySSLCiphers() {
        return getAttr(Provisioning.A_zimbraReverseProxySSLCiphers);
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
     * @return zimbraReverseProxySendImapId, or false if unset
     */
    @ZAttr(id=588)
    public boolean isReverseProxySendImapId() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySendImapId, false);
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
        attrs.put(Provisioning.A_zimbraReverseProxySendImapId, Boolean.toString(zimbraReverseProxySendImapId));
        return attrs;
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
     * @return zimbraReverseProxySendPop3Xoip, or false if unset
     */
    @ZAttr(id=587)
    public boolean isReverseProxySendPop3Xoip() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySendPop3Xoip, false);
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
        attrs.put(Provisioning.A_zimbraReverseProxySendPop3Xoip, Boolean.toString(zimbraReverseProxySendPop3Xoip));
        return attrs;
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
     * @return zimbraReverseProxyUserLoginLimit, or -1 if unset
     */
    @ZAttr(id=624)
    public int getReverseProxyUserLoginLimit() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyUserLoginLimit, -1);
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
     * @return zimbraReverseProxyUserLoginLimitTime, or -1 if unset
     */
    @ZAttr(id=625)
    public int getReverseProxyUserLoginLimitTime() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, -1);
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
     * @return zimbraReverseProxyUserNameAttribute, or null unset
     */
    @ZAttr(id=572)
    public String getReverseProxyUserNameAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyUserNameAttribute);
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
     * @return zimbraReverseProxyUserThrottleMsg, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public String getReverseProxyUserThrottleMsg() {
        return getAttr(Provisioning.A_zimbraReverseProxyUserThrottleMsg);
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
     * @return zimbraReverseProxyWorkerConnections, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public int getReverseProxyWorkerConnections() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyWorkerConnections, -1);
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
     * @return zimbraReverseProxyWorkerProcesses, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public int getReverseProxyWorkerProcesses() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyWorkerProcesses, -1);
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
     * Object classes to add when creating a zimbra right object.
     *
     * @return zimbraRightExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public String[] getRightExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraRightExtraObjectClass);
    }

    /**
     * Object classes to add when creating a zimbra right object.
     *
     * @param zimbraRightExtraObjectClass new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public Map<String,Object> setRightExtraObjectClass(String[] zimbraRightExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRightExtraObjectClass, zimbraRightExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra right object.
     *
     * @param zimbraRightExtraObjectClass new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public Map<String,Object> addRightExtraObjectClass(String zimbraRightExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraRightExtraObjectClass, zimbraRightExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra right object.
     *
     * @param zimbraRightExtraObjectClass existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public Map<String,Object> removeRightExtraObjectClass(String zimbraRightExtraObjectClass, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraRightExtraObjectClass, zimbraRightExtraObjectClass);
        return attrs;
    }

    /**
     * Object classes to add when creating a zimbra right object.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=764)
    public Map<String,Object> unsetRightExtraObjectClass(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRightExtraObjectClass, "");
        return attrs;
    }

    /**
     * SSL certificate
     *
     * @return zimbraSSLCertificate, or null unset
     */
    @ZAttr(id=563)
    public String getSSLCertificate() {
        return getAttr(Provisioning.A_zimbraSSLCertificate);
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
        return getMultiAttr(Provisioning.A_zimbraSSLExcludeCipherSuites);
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
     * @return zimbraSSLPrivateKey, or null unset
     */
    @ZAttr(id=564)
    public String getSSLPrivateKey() {
        return getAttr(Provisioning.A_zimbraSSLPrivateKey);
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
     * @return zimbraScheduledTaskNumThreads, or -1 if unset
     */
    @ZAttr(id=522)
    public int getScheduledTaskNumThreads() {
        return getIntAttr(Provisioning.A_zimbraScheduledTaskNumThreads, -1);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public Map<String,Object> setSmtpSendAddAuthenticatedUser(boolean zimbraSmtpSendAddAuthenticatedUser, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, Boolean.toString(zimbraSmtpSendAddAuthenticatedUser));
        return attrs;
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
     * @return zimbraSmtpSendAddMailer, or false if unset
     */
    @ZAttr(id=636)
    public boolean isSmtpSendAddMailer() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddMailer, false);
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
        attrs.put(Provisioning.A_zimbraSmtpSendAddMailer, Boolean.toString(zimbraSmtpSendAddMailer));
        return attrs;
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
     * @return zimbraSmtpSendAddOriginatingIP, or false if unset
     */
    @ZAttr(id=435)
    public boolean isSmtpSendAddOriginatingIP() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddOriginatingIP, false);
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
        attrs.put(Provisioning.A_zimbraSmtpSendAddOriginatingIP, Boolean.toString(zimbraSmtpSendAddOriginatingIP));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=708)
    public Map<String,Object> setSoapExposeVersion(boolean zimbraSoapExposeVersion, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapExposeVersion, Boolean.toString(zimbraSoapExposeVersion));
        return attrs;
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
     * @return zimbraSoapRequestMaxSize, or -1 if unset
     */
    @ZAttr(id=557)
    public int getSoapRequestMaxSize() {
        return getIntAttr(Provisioning.A_zimbraSoapRequestMaxSize, -1);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=201)
    public Map<String,Object> setSpamCheckEnabled(boolean zimbraSpamCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamCheckEnabled, Boolean.toString(zimbraSpamCheckEnabled));
        return attrs;
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
     * @return zimbraSpamHeader, or null unset
     */
    @ZAttr(id=210)
    public String getSpamHeader() {
        return getAttr(Provisioning.A_zimbraSpamHeader);
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
     * @return zimbraSpamHeaderValue, or null unset
     */
    @ZAttr(id=211)
    public String getSpamHeaderValue() {
        return getAttr(Provisioning.A_zimbraSpamHeaderValue);
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
     * @return zimbraSpamIsNotSpamAccount, or null unset
     */
    @ZAttr(id=245)
    public String getSpamIsNotSpamAccount() {
        return getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount);
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
     * @return zimbraSpamIsSpamAccount, or null unset
     */
    @ZAttr(id=244)
    public String getSpamIsSpamAccount() {
        return getAttr(Provisioning.A_zimbraSpamIsSpamAccount);
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
     * @return zimbraSpamKillPercent, or -1 if unset
     */
    @ZAttr(id=202)
    public int getSpamKillPercent() {
        return getIntAttr(Provisioning.A_zimbraSpamKillPercent, -1);
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
     * @return zimbraSpamReportSenderHeader, or null unset
     */
    @ZAttr(id=465)
    public String getSpamReportSenderHeader() {
        return getAttr(Provisioning.A_zimbraSpamReportSenderHeader);
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
     * @return zimbraSpamReportTypeHam, or null unset
     */
    @ZAttr(id=468)
    public String getSpamReportTypeHam() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeHam);
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
     * @return zimbraSpamReportTypeHeader, or null unset
     */
    @ZAttr(id=466)
    public String getSpamReportTypeHeader() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeHeader);
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
     * @return zimbraSpamReportTypeSpam, or null unset
     */
    @ZAttr(id=467)
    public String getSpamReportTypeSpam() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeSpam);
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
     * @return zimbraSpamSubjectTag, or null unset
     */
    @ZAttr(id=203)
    public String getSpamSubjectTag() {
        return getAttr(Provisioning.A_zimbraSpamSubjectTag);
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
     * @return zimbraSpamTagPercent, or -1 if unset
     */
    @ZAttr(id=204)
    public int getSpamTagPercent() {
        return getIntAttr(Provisioning.A_zimbraSpamTagPercent, -1);
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
     * @return zimbraSslCaCert, or null unset
     */
    @ZAttr(id=277)
    public String getSslCaCert() {
        return getAttr(Provisioning.A_zimbraSslCaCert);
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
     * @return zimbraSslCaKey, or null unset
     */
    @ZAttr(id=278)
    public String getSslCaKey() {
        return getAttr(Provisioning.A_zimbraSslCaKey);
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
     * @return zimbraTableMaintenanceGrowthFactor, or -1 if unset
     */
    @ZAttr(id=171)
    public int getTableMaintenanceGrowthFactor() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceGrowthFactor, -1);
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
     * @return zimbraTableMaintenanceMaxRows, or -1 if unset
     */
    @ZAttr(id=169)
    public int getTableMaintenanceMaxRows() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceMaxRows, -1);
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
     * @return zimbraTableMaintenanceMinRows, or -1 if unset
     */
    @ZAttr(id=168)
    public int getTableMaintenanceMinRows() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceMinRows, -1);
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
     * @return zimbraTableMaintenanceOperation, or null if unset and/or has invalid value
     */
    @ZAttr(id=170)
    public ZAttrProvisioning.TableMaintenanceOperation getTableMaintenanceOperation() {
        try { String v = getAttr(Provisioning.A_zimbraTableMaintenanceOperation); return v == null ? null : ZAttrProvisioning.TableMaintenanceOperation.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @return zimbraTableMaintenanceOperation, or ampty array if unset
     */
    @ZAttr(id=170)
    public String[] getTableMaintenanceOperationAsString() {
        return getMultiAttr(Provisioning.A_zimbraTableMaintenanceOperation);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=170)
    public Map<String,Object> setTableMaintenanceOperationAsString(String[] zimbraTableMaintenanceOperation, Map<String,Object> attrs) {
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
     * @return zimbraVirusBlockEncryptedArchive, or false if unset
     */
    @ZAttr(id=205)
    public boolean isVirusBlockEncryptedArchive() {
        return getBooleanAttr(Provisioning.A_zimbraVirusBlockEncryptedArchive, false);
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
        attrs.put(Provisioning.A_zimbraVirusBlockEncryptedArchive, Boolean.toString(zimbraVirusBlockEncryptedArchive));
        return attrs;
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=206)
    public Map<String,Object> setVirusCheckEnabled(boolean zimbraVirusCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVirusCheckEnabled, Boolean.toString(zimbraVirusCheckEnabled));
        return attrs;
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
     * @return zimbraVirusDefinitionsUpdateFrequency in millseconds, or -1 if unset
     */
    @ZAttr(id=191)
    public long getVirusDefinitionsUpdateFrequency() {
        return getTimeInterval(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, -1);
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @return zimbraVirusDefinitionsUpdateFrequency, or null unset
     */
    @ZAttr(id=191)
    public String getVirusDefinitionsUpdateFrequencyAsString() {
        return getAttr(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency);
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
     * @return zimbraVirusWarnAdmin, or false if unset
     */
    @ZAttr(id=207)
    public boolean isVirusWarnAdmin() {
        return getBooleanAttr(Provisioning.A_zimbraVirusWarnAdmin, false);
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
        attrs.put(Provisioning.A_zimbraVirusWarnAdmin, Boolean.toString(zimbraVirusWarnAdmin));
        return attrs;
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
     * @return zimbraVirusWarnRecipient, or false if unset
     */
    @ZAttr(id=208)
    public boolean isVirusWarnRecipient() {
        return getBooleanAttr(Provisioning.A_zimbraVirusWarnRecipient, false);
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
        attrs.put(Provisioning.A_zimbraVirusWarnRecipient, Boolean.toString(zimbraVirusWarnRecipient));
        return attrs;
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
     * Enable XMPP support for IM
     *
     * @return zimbraXMPPEnabled, or false if unset
     */
    @ZAttr(id=397)
    public boolean isXMPPEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraXMPPEnabled, false);
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
        attrs.put(Provisioning.A_zimbraXMPPEnabled, Boolean.toString(zimbraXMPPEnabled));
        return attrs;
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

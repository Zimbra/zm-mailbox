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
public class ZAttrServer extends NamedEntry {

    public ZAttrServer(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20081118-1352 */

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @return cn, or null unset
     */
    @ZAttr(id=-1)
    public String getCn() {
        return getAttr(Provisioning.A_cn);
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
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraIMBindAddress, or ampty array if unset
     */
    @ZAttr(id=567)
    public String[] getIMBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraIMBindAddress);
    }

    /**
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraIMBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=567)
    public Map<String,Object> setIMBindAddress(String[] zimbraIMBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMBindAddress, zimbraIMBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraIMBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=567)
    public Map<String,Object> addIMBindAddress(String zimbraIMBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraIMBindAddress, zimbraIMBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraIMBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=567)
    public Map<String,Object> removeIMBindAddress(String zimbraIMBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraIMBindAddress, zimbraIMBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=567)
    public Map<String,Object> unsetIMBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMBindAddress, "");
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
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @return zimbraImapAdvertisedName, or null unset
     */
    @ZAttr(id=178)
    public String getImapAdvertisedName() {
        return getAttr(Provisioning.A_zimbraImapAdvertisedName);
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @param zimbraImapAdvertisedName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=178)
    public Map<String,Object> setImapAdvertisedName(String zimbraImapAdvertisedName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapAdvertisedName, zimbraImapAdvertisedName);
        return attrs;
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=178)
    public Map<String,Object> unsetImapAdvertisedName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapAdvertisedName, "");
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraImapBindAddress, or ampty array if unset
     */
    @ZAttr(id=179)
    public String[] getImapBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraImapBindAddress);
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraImapBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=179)
    public Map<String,Object> setImapBindAddress(String[] zimbraImapBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindAddress, zimbraImapBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraImapBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=179)
    public Map<String,Object> addImapBindAddress(String zimbraImapBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraImapBindAddress, zimbraImapBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraImapBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=179)
    public Map<String,Object> removeImapBindAddress(String zimbraImapBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraImapBindAddress, zimbraImapBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=179)
    public Map<String,Object> unsetImapBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapBindAddress, "");
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
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraImapSSLBindAddress, or ampty array if unset
     */
    @ZAttr(id=182)
    public String[] getImapSSLBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraImapSSLBindAddress);
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraImapSSLBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=182)
    public Map<String,Object> setImapSSLBindAddress(String[] zimbraImapSSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindAddress, zimbraImapSSLBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraImapSSLBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=182)
    public Map<String,Object> addImapSSLBindAddress(String zimbraImapSSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraImapSSLBindAddress, zimbraImapSSLBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraImapSSLBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=182)
    public Map<String,Object> removeImapSSLBindAddress(String zimbraImapSSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraImapSSLBindAddress, zimbraImapSSLBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=182)
    public Map<String,Object> unsetImapSSLBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSSLBindAddress, "");
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
     * true if this server is the monitor host
     *
     * @return zimbraIsMonitorHost, or false if unset
     */
    @ZAttr(id=132)
    public boolean isIsMonitorHost() {
        return getBooleanAttr(Provisioning.A_zimbraIsMonitorHost, false);
    }

    /**
     * true if this server is the monitor host
     *
     * @param zimbraIsMonitorHost new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=132)
    public Map<String,Object> setIsMonitorHost(boolean zimbraIsMonitorHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsMonitorHost, Boolean.toString(zimbraIsMonitorHost));
        return attrs;
    }

    /**
     * true if this server is the monitor host
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=132)
    public Map<String,Object> unsetIsMonitorHost(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsMonitorHost, "");
        return attrs;
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @return zimbraLmtpAdvertisedName, or null unset
     */
    @ZAttr(id=23)
    public String getLmtpAdvertisedName() {
        return getAttr(Provisioning.A_zimbraLmtpAdvertisedName);
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @param zimbraLmtpAdvertisedName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=23)
    public Map<String,Object> setLmtpAdvertisedName(String zimbraLmtpAdvertisedName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpAdvertisedName, zimbraLmtpAdvertisedName);
        return attrs;
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=23)
    public Map<String,Object> unsetLmtpAdvertisedName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpAdvertisedName, "");
        return attrs;
    }

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraLmtpBindAddress, or ampty array if unset
     */
    @ZAttr(id=25)
    public String[] getLmtpBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraLmtpBindAddress);
    }

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraLmtpBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=25)
    public Map<String,Object> setLmtpBindAddress(String[] zimbraLmtpBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindAddress, zimbraLmtpBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraLmtpBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=25)
    public Map<String,Object> addLmtpBindAddress(String zimbraLmtpBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraLmtpBindAddress, zimbraLmtpBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraLmtpBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=25)
    public Map<String,Object> removeLmtpBindAddress(String zimbraLmtpBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraLmtpBindAddress, zimbraLmtpBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=25)
    public Map<String,Object> unsetLmtpBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpBindAddress, "");
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
     * The id of the last purged mailbox.
     *
     * @return zimbraMailLastPurgedMailboxId, or -1 if unset
     */
    @ZAttr(id=543)
    public int getMailLastPurgedMailboxId() {
        return getIntAttr(Provisioning.A_zimbraMailLastPurgedMailboxId, -1);
    }

    /**
     * The id of the last purged mailbox.
     *
     * @param zimbraMailLastPurgedMailboxId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=543)
    public Map<String,Object> setMailLastPurgedMailboxId(int zimbraMailLastPurgedMailboxId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailLastPurgedMailboxId, Integer.toString(zimbraMailLastPurgedMailboxId));
        return attrs;
    }

    /**
     * The id of the last purged mailbox.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=543)
    public Map<String,Object> unsetMailLastPurgedMailboxId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailLastPurgedMailboxId, "");
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
     * interface address(es) on which memcached server
     *
     * @return zimbraMemcachedBindAddress, or ampty array if unset
     */
    @ZAttr(id=581)
    public String[] getMemcachedBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraMemcachedBindAddress);
    }

    /**
     * interface address(es) on which memcached server
     *
     * @param zimbraMemcachedBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=581)
    public Map<String,Object> setMemcachedBindAddress(String[] zimbraMemcachedBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedBindAddress, zimbraMemcachedBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which memcached server
     *
     * @param zimbraMemcachedBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=581)
    public Map<String,Object> addMemcachedBindAddress(String zimbraMemcachedBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMemcachedBindAddress, zimbraMemcachedBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which memcached server
     *
     * @param zimbraMemcachedBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=581)
    public Map<String,Object> removeMemcachedBindAddress(String zimbraMemcachedBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMemcachedBindAddress, zimbraMemcachedBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which memcached server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=581)
    public Map<String,Object> unsetMemcachedBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedBindAddress, "");
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
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @return zimbraPop3AdvertisedName, or null unset
     */
    @ZAttr(id=93)
    public String getPop3AdvertisedName() {
        return getAttr(Provisioning.A_zimbraPop3AdvertisedName);
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @param zimbraPop3AdvertisedName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=93)
    public Map<String,Object> setPop3AdvertisedName(String zimbraPop3AdvertisedName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3AdvertisedName, zimbraPop3AdvertisedName);
        return attrs;
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=93)
    public Map<String,Object> unsetPop3AdvertisedName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3AdvertisedName, "");
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraPop3BindAddress, or ampty array if unset
     */
    @ZAttr(id=95)
    public String[] getPop3BindAddress() {
        return getMultiAttr(Provisioning.A_zimbraPop3BindAddress);
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraPop3BindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=95)
    public Map<String,Object> setPop3BindAddress(String[] zimbraPop3BindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindAddress, zimbraPop3BindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraPop3BindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=95)
    public Map<String,Object> addPop3BindAddress(String zimbraPop3BindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPop3BindAddress, zimbraPop3BindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraPop3BindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=95)
    public Map<String,Object> removePop3BindAddress(String zimbraPop3BindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPop3BindAddress, zimbraPop3BindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=95)
    public Map<String,Object> unsetPop3BindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3BindAddress, "");
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
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraPop3SSLBindAddress, or ampty array if unset
     */
    @ZAttr(id=186)
    public String[] getPop3SSLBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraPop3SSLBindAddress);
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraPop3SSLBindAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=186)
    public Map<String,Object> setPop3SSLBindAddress(String[] zimbraPop3SSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindAddress, zimbraPop3SSLBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraPop3SSLBindAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=186)
    public Map<String,Object> addPop3SSLBindAddress(String zimbraPop3SSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPop3SSLBindAddress, zimbraPop3SSLBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param zimbraPop3SSLBindAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=186)
    public Map<String,Object> removePop3SSLBindAddress(String zimbraPop3SSLBindAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPop3SSLBindAddress, zimbraPop3SSLBindAddress);
        return attrs;
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=186)
    public Map<String,Object> unsetPop3SSLBindAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3SSLBindAddress, "");
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
     * services that are enabled on this server
     *
     * @return zimbraServiceEnabled, or ampty array if unset
     */
    @ZAttr(id=220)
    public String[] getServiceEnabled() {
        return getMultiAttr(Provisioning.A_zimbraServiceEnabled);
    }

    /**
     * services that are enabled on this server
     *
     * @param zimbraServiceEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=220)
    public Map<String,Object> setServiceEnabled(String[] zimbraServiceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, zimbraServiceEnabled);
        return attrs;
    }

    /**
     * services that are enabled on this server
     *
     * @param zimbraServiceEnabled new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=220)
    public Map<String,Object> addServiceEnabled(String zimbraServiceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraServiceEnabled, zimbraServiceEnabled);
        return attrs;
    }

    /**
     * services that are enabled on this server
     *
     * @param zimbraServiceEnabled existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=220)
    public Map<String,Object> removeServiceEnabled(String zimbraServiceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraServiceEnabled, zimbraServiceEnabled);
        return attrs;
    }

    /**
     * services that are enabled on this server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=220)
    public Map<String,Object> unsetServiceEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, "");
        return attrs;
    }

    /**
     * public hostname of the host
     *
     * @return zimbraServiceHostname, or null unset
     */
    @ZAttr(id=65)
    public String getServiceHostname() {
        return getAttr(Provisioning.A_zimbraServiceHostname);
    }

    /**
     * public hostname of the host
     *
     * @param zimbraServiceHostname new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=65)
    public Map<String,Object> setServiceHostname(String zimbraServiceHostname, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, zimbraServiceHostname);
        return attrs;
    }

    /**
     * public hostname of the host
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=65)
    public Map<String,Object> unsetServiceHostname(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "");
        return attrs;
    }

    /**
     * services that are installed on this server
     *
     * @return zimbraServiceInstalled, or ampty array if unset
     */
    @ZAttr(id=221)
    public String[] getServiceInstalled() {
        return getMultiAttr(Provisioning.A_zimbraServiceInstalled);
    }

    /**
     * services that are installed on this server
     *
     * @param zimbraServiceInstalled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=221)
    public Map<String,Object> setServiceInstalled(String[] zimbraServiceInstalled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServiceInstalled, zimbraServiceInstalled);
        return attrs;
    }

    /**
     * services that are installed on this server
     *
     * @param zimbraServiceInstalled new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=221)
    public Map<String,Object> addServiceInstalled(String zimbraServiceInstalled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraServiceInstalled, zimbraServiceInstalled);
        return attrs;
    }

    /**
     * services that are installed on this server
     *
     * @param zimbraServiceInstalled existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=221)
    public Map<String,Object> removeServiceInstalled(String zimbraServiceInstalled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraServiceInstalled, zimbraServiceInstalled);
        return attrs;
    }

    /**
     * services that are installed on this server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=221)
    public Map<String,Object> unsetServiceInstalled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraServiceInstalled, "");
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
     * Public key of this server, used by other hosts to authorize this
     * server to login.
     *
     * @return zimbraSshPublicKey, or null unset
     */
    @ZAttr(id=262)
    public String getSshPublicKey() {
        return getAttr(Provisioning.A_zimbraSshPublicKey);
    }

    /**
     * Public key of this server, used by other hosts to authorize this
     * server to login.
     *
     * @param zimbraSshPublicKey new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=262)
    public Map<String,Object> setSshPublicKey(String zimbraSshPublicKey, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSshPublicKey, zimbraSshPublicKey);
        return attrs;
    }

    /**
     * Public key of this server, used by other hosts to authorize this
     * server to login.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=262)
    public Map<String,Object> unsetSshPublicKey(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSshPublicKey, "");
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
     * whether end-user services on SOAP and LMTP interfaces are enabled
     *
     * @return zimbraUserServicesEnabled, or false if unset
     */
    @ZAttr(id=146)
    public boolean isUserServicesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraUserServicesEnabled, false);
    }

    /**
     * whether end-user services on SOAP and LMTP interfaces are enabled
     *
     * @param zimbraUserServicesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=146)
    public Map<String,Object> setUserServicesEnabled(boolean zimbraUserServicesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUserServicesEnabled, Boolean.toString(zimbraUserServicesEnabled));
        return attrs;
    }

    /**
     * whether end-user services on SOAP and LMTP interfaces are enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=146)
    public Map<String,Object> unsetUserServicesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraUserServicesEnabled, "");
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

    ///// END-AUTO-GEN-REPLACE

}

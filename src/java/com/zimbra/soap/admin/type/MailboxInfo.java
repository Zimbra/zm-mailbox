/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class MailboxInfo {

    /**
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AdminConstants.A_MT_ID /* id */, required=true)
    private final int id;
    /**
     * @zm-api-field-description Group ID
     */
    @XmlAttribute(name=AdminConstants.A_MT_GROUPID /* groupId */, required=true)
    private final int groupId;
    /**
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_MT_ACCOUNTID /* accountId */, required=true)
    private final String accountId;
    /**
     * @zm-api-field-description Index volume ID
     */
    @XmlAttribute(name=AdminConstants.A_MT_INDEXVOLUMEID /*  indexVolumeId */, required=true)
    private final short indexVolumeId;
    /**
     * @zm-api-field-description Item ID checkpoint
     */
    @XmlAttribute(name=AdminConstants.A_MT_ITEMIDCHECKPOINT /* itemIdCheckPoint */, required=true)
    private final int itemIdCheckPoint;
    /**
     * @zm-api-field-description Contact count
     */
    @XmlAttribute(name=AdminConstants.A_MT_CONTACTCOUNT /* contactCount */, required=true)
    private final int contactCount;
    /**
     * @zm-api-field-description Size checkpoint
     */
    @XmlAttribute(name=AdminConstants.A_MT_SIZECHECKPOINT /* sizeCheckPoint */, required=true)
    private final long sizeCheckPoint;
    /**
     * @zm-api-field-description Change checkpoint
     */
    @XmlAttribute(name=AdminConstants.A_MT_CHANGECHECKPOINT /* changeCheckPoint */, required=true)
    private final int changeCheckPoint;
    /**
     * @zm-api-field-description Tracking Sync
     */
    @XmlAttribute(name=AdminConstants.A_MT_TRACKINGSYNC /* trackingSync */, required=true)
    private final int trackingSync;
    /**
     * @zm-api-field-description Tracking IMAP
     */
    @XmlAttribute(name=AdminConstants.A_MT_TRACKINGIMAP /* trackingImap */, required=true)
    private final ZmBoolean trackingImap;
    /**
     * @zm-api-field-description Last Backup At
     */
    @XmlAttribute(name=AdminConstants.A_MT_LASTBACKUPAT /* lastbackupat */, required=false)
    private final Integer lastBackupAt;
    /**
     * @zm-api-field-description Last SOAP access
     */
    @XmlAttribute(name=AdminConstants.A_MT_LASTSOAPACCESS /* lastSoapAccess */, required=true)
    private final int lastSoapAccess;
    /**
     * @zm-api-field-description New Messages
     */
    @XmlAttribute(name=AdminConstants.A_MT_NEWNESSAGES /* newMessages */, required=true)
    private final int newMessages;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailboxInfo() {
        this((short)0, 0, null, (short)0, 0, 0, 0, 0, 0, true, null, 0, 0);
    }

    public MailboxInfo(int id, int groupId, String accountId,
            short indexVolumeId, int itemIdCheckPoint, int contactCount,
            long sizeCheckPoint, int changeCheckPoint, int trackingSync,
            boolean trackingImap, Integer lastBackupAt,
            int lastSoapAccess, int newMessages) {
        this.id = id;
        this.groupId = groupId;
        this.accountId = accountId;
        this.indexVolumeId = indexVolumeId;
        this.itemIdCheckPoint = itemIdCheckPoint;
        this.contactCount = contactCount;
        this.sizeCheckPoint = sizeCheckPoint;
        this.changeCheckPoint = changeCheckPoint;
        this.trackingSync = trackingSync;
        this.trackingImap = ZmBoolean.fromBool(trackingImap);
        this.lastBackupAt = lastBackupAt;
        this.lastSoapAccess = lastSoapAccess;
        this.newMessages = newMessages;
    }

    public int getId() { return id; }
    public int getGroupId() { return groupId; }
    public String getAccountId() { return accountId; }
    public short getIndexVolumeId() { return indexVolumeId; }
    public int getItemIdCheckPoint() { return itemIdCheckPoint; }
    public int getContactCount() { return contactCount; }
    public long getSizeCheckPoint() { return sizeCheckPoint; }
    public int getChangeCheckPoint() { return changeCheckPoint; }
    public int getTrackingSync() { return trackingSync; }
    public boolean isTrackingImap() { return ZmBoolean.toBool(trackingImap); }
    public Integer getLastBackupAt() { return lastBackupAt; }
    public int getLastSoapAccess() { return lastSoapAccess; }
    public int getNewMessages() { return newMessages; }
}

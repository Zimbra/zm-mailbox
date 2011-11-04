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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class MailboxInfo {

    @XmlAttribute(name=AdminConstants.A_MT_ID, required=true)
    private final int id;
    @XmlAttribute(name=AdminConstants.A_MT_GROUPID, required=true)
    private final int groupId;
    @XmlAttribute(name=AdminConstants.A_MT_ACCOUNTID, required=true)
    private final String accountId;
    @XmlAttribute(name=AdminConstants.A_MT_INDEXVOLUMEID, required=true)
    private final short indexVolumeId;
    @XmlAttribute(name=AdminConstants.A_MT_ITEMIDCHECKPOINT, required=true)
    private final int itemIdCheckPoint;
    @XmlAttribute(name=AdminConstants.A_MT_CONTACTCOUNT, required=true)
    private final int contactCount;
    @XmlAttribute(name=AdminConstants.A_MT_SIZECHECKPOINT, required=true)
    private final long sizeCheckPoint;
    @XmlAttribute(name=AdminConstants.A_MT_CHANGECHECKPOINT, required=true)
    private final int changeCheckPoint;
    @XmlAttribute(name=AdminConstants.A_MT_TRACKINGSYNC, required=true)
    private final int trackingSync;
    @XmlAttribute(name=AdminConstants.A_MT_TRACKINGIMAP, required=true)
    private final ZmBoolean trackingImap;
    @XmlAttribute(name=AdminConstants.A_MT_LASTBACKUPAT, required=false)
    private final Integer lastBackupAt;
    @XmlAttribute(name=AdminConstants.A_MT_LASTSOAPACCESS, required=true)
    private final int lastSoapAccess;
    @XmlAttribute(name=AdminConstants.A_MT_NEWNESSAGES, required=true)
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

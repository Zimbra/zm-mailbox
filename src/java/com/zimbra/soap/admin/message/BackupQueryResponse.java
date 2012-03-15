/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.BackupQueryInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_BACKUP_QUERY_RESPONSE)
@XmlType(propOrder = {})
public class BackupQueryResponse {

    /**
     * @zm-api-field-tag total-space-bytes
     * @zm-api-field-description total space on the backup target volume in bytes
     */
    @XmlAttribute(name=BackupConstants.A_TOTAL_SPACE /* totalSpace */, required=true)
    private final long totalSpace;

    /**
     * @zm-api-field-tag free-space-bytes
     * @zm-api-field-description Free space on the backup target volume in bytes
     */
    @XmlAttribute(name=BackupConstants.A_FREE_SPACE /* freeSpace */, required=true)
    private final long freeSpace;

    /**
     * @zm-api-field-tag more-backups
     * @zm-api-field-description Present with value <b>1 (true)</b> there are more backups to page through
     */
    @XmlAttribute(name=BackupConstants.A_MORE /* more */, required=false)
    private ZmBoolean more;

    /**
     * @zm-api-field-description Backup information
     */
    @XmlElement(name=BackupConstants.E_BACKUP /* backup */, required=false)
    private List<BackupQueryInfo> backups = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BackupQueryResponse() {
        this(-1L, -1L);
    }

    public BackupQueryResponse(long totalSpace, long freeSpace) {
        this.totalSpace = totalSpace;
        this.freeSpace = freeSpace;
    }

    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }

    public void setBackups(Iterable <BackupQueryInfo> backups) {
        this.backups.clear();
        if (backups != null) {
            Iterables.addAll(this.backups,backups);
        }
    }

    public void addBackup(BackupQueryInfo backup) {
        this.backups.add(backup);
    }

    public long getTotalSpace() { return totalSpace; }
    public long getFreeSpace() { return freeSpace; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public List<BackupQueryInfo> getBackups() {
        return backups;
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("totalSpace", totalSpace)
            .add("freeSpace", freeSpace)
            .add("more", more)
            .add("backups", backups);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

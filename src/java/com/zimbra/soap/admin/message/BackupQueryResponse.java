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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.BackupQueryInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=BackupConstants.E_BACKUP_QUERY_RESPONSE)
@XmlType(propOrder = {})
public class BackupQueryResponse {

    @XmlAttribute(name=BackupConstants.A_TOTAL_SPACE /* totalSpace */,
                    required=true)
    private final long totalSpace;

    @XmlAttribute(name=BackupConstants.A_FREE_SPACE /* freeSpace */,
                    required=true)
    private final long freeSpace;

    @XmlAttribute(name=BackupConstants.A_MORE /* more */, required=false)
    private ZmBoolean more;

    @XmlElement(name=BackupConstants.E_BACKUP /* backup */, required=false)
    private BackupQueryInfo backups;

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
    public void setBackups(BackupQueryInfo backups) { this.backups = backups; }
    public long getTotalSpace() { return totalSpace; }
    public long getFreeSpace() { return freeSpace; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public BackupQueryInfo getBackups() { return backups; }

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

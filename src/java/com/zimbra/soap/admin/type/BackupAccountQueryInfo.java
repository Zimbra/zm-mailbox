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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class BackupAccountQueryInfo {

    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private final String name;

    @XmlAttribute(name=BackupConstants.A_MORE /* more */, required=false)
    private ZmBoolean more;

    @XmlElement(name=BackupConstants.E_BACKUP /* backup */, required=false)
    private BackupAccountQueryBackupInfo backup;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BackupAccountQueryInfo() {
        this((String) null);
    }

    public BackupAccountQueryInfo(String name) {
        this.name = name;
    }

    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setBackup(BackupAccountQueryBackupInfo backup) {
        this.backup = backup;
    }
    public String getName() { return name; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public BackupAccountQueryBackupInfo getBackup() { return backup; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("more", more)
            .add("backup", backup);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

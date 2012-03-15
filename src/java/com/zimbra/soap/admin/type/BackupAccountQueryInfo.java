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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class BackupAccountQueryInfo {

    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description Present if there are more backups to page through
     */
    @XmlAttribute(name=BackupConstants.A_MORE /* more */, required=false)
    private ZmBoolean more;

    /**
     * @zm-api-field-description Information about backup
     */
    @XmlElement(name=BackupConstants.E_BACKUP /* backup */, required=false)
    private List<BackupAccountQueryBackupInfo> backups = Lists.newArrayList();

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

    public void setBackups(Iterable <BackupAccountQueryBackupInfo> backups) {
        this.backups.clear();
        if (backups != null) {
            Iterables.addAll(this.backups,backups);
        }
    }

    public void addBackup(BackupAccountQueryBackupInfo backup) {
        this.backups.add(backup);
    }


    public String getName() { return name; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }

    public List<BackupAccountQueryBackupInfo> getBackups() {
        return backups;
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("more", more)
            .add("backups", backups);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

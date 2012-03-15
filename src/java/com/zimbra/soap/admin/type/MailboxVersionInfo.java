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

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MailboxVersionInfo {

    /**
     * @zm-api-field-tag mailbox-id
     * @zm-api-field-description Mailbox ID
     */
    @XmlAttribute(name=BackupConstants.A_MAILBOXID /* mbxid */, required=true)
    private int mailboxId;

    /**
     * @zm-api-field-tag mailbox-major-version
     * @zm-api-field-description Major version of mailbox
     */
    @XmlAttribute(name=BackupConstants.A_MAJOR_VERSION /* majorVer */, required=true)
    private short majorVersion;

    /**
     * @zm-api-field-tag mailbox-minor-version
     * @zm-api-field-description Minor version of mailbox
     */
    @XmlAttribute(name=BackupConstants.A_MINOR_VERSION /* minorVer */, required=true)
    private short minorVersion;

    /**
     * @zm-api-field-tag db-schema-version
     * @zm-api-field-description Database schema version
     */
    @XmlAttribute(name=BackupConstants.A_DB_VERSION /* dbVer */, required=true)
    private int dbVersion;

    /**
     * @zm-api-field-tag search-index-version
     * @zm-api-field-description Search index version
     */
    @XmlAttribute(name=BackupConstants.A_INDEX_VERSION /* indexVer */, required=true)
    private int indexVersion;

    public MailboxVersionInfo() {
    }

    private MailboxVersionInfo(int mailboxId,
            short majorVersion, short minorVersion, int dbVersion, int indexVersion) {
        setMailboxId(mailboxId);
        setMajorVersion(majorVersion);
        setMinorVersion(minorVersion);
        setDbVersion(dbVersion);
        setIndexVersion(indexVersion);
    }

    public static MailboxVersionInfo createFromMailboxIdMajorMinorDbIndexVersions(int mailboxId,
            short majorVersion, short minorVersion, int dbVersion, int indexVersion) {
        return new MailboxVersionInfo(mailboxId, majorVersion, minorVersion, dbVersion, indexVersion);
    }

    public void setMailboxId(int mailboxId) { this.mailboxId = mailboxId; }
    public void setMajorVersion(short majorVersion) { this.majorVersion = majorVersion; }
    public void setMinorVersion(short minorVersion) { this.minorVersion = minorVersion; }
    public void setDbVersion(int dbVersion) { this.dbVersion = dbVersion; }
    public void setIndexVersion(int indexVersion) { this.indexVersion = indexVersion; }
    public int getMailboxId() { return mailboxId; }
    public short getMajorVersion() { return majorVersion; }
    public short getMinorVersion() { return minorVersion; }
    public int getDbVersion() { return dbVersion; }
    public int getIndexVersion() { return indexVersion; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("mailboxId", mailboxId)
            .add("majorVersion", majorVersion)
            .add("minorVersion", minorVersion)
            .add("dbVersion", dbVersion)
            .add("indexVersion", indexVersion);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

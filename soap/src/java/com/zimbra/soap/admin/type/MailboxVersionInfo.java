/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("mailboxId", mailboxId)
            .add("majorVersion", majorVersion)
            .add("minorVersion", minorVersion)
            .add("dbVersion", dbVersion)
            .add("indexVersion", indexVersion);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

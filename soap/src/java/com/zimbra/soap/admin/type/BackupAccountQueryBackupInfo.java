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
public class BackupAccountQueryBackupInfo {

    /**
     * @zm-api-field-tag backup-label
     * @zm-api-field-description Backup label
     */
    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-tag backup-type
     * @zm-api-field-description Backup type - <b>full|incremental</b>
     */
    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=false)
    private String type;

    /**
     * @zm-api-field-tag start-time-in-millis
     * @zm-api-field-description Start time in milliseconds
     */
    @XmlAttribute(name=BackupConstants.A_START /* start */, required=false)
    private Long start;

    /**
     * @zm-api-field-tag end-time-in-millis
     * @zm-api-field-description End time in milliseconds
     */
    @XmlAttribute(name=BackupConstants.A_END /* end */, required=false)
    private Long end;

    /**
     * @zm-api-field-tag account-uid
     * @zm-api-field-description Account UID
     */
    @XmlAttribute(name=BackupConstants.A_ACCOUNT_ID /* accountId */, required=false)
    private String accountId;

    public BackupAccountQueryBackupInfo() {
    }

    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    public void setStart(Long start) { this.start = start; }
    public void setEnd(Long end) { this.end = end; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getLabel() { return label; }
    public String getType() { return type; }
    public Long getStart() { return start; }
    public Long getEnd() { return end; }
    public String getAccountId() { return accountId; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("label", label)
            .add("type", type)
            .add("start", start)
            .add("end", end)
            .add("accountId", accountId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

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

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.BackupSpec;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Do a backup
 * <br />
 * &lt;account> elements are required when method=full and server is running in standard backup mode.
 * If server is running in auto-grouped backup mode, omit the account list in full backup request to trigger
 * auto-grouped backup.  If account list is specified, only those accounts will be backed up.
 * <br />
 * <br />
 * When sync is 1, the full backup is run synchronously; otherwise full backup is started and run in a separate thread
 * and the soap call can return with the backup label.
 * <br />
 * <br />
 * or
 * <pre>
 * &lt;BackupRequest>
 *   &lt;backup method="abort" 
 *   [target="{path to backup target}"] label="{full backup label to abort}"/>
 * &lt;/BackupRequest>
 * </pre>
 * label is only meaningful and required in the request when method is abort.
 * <br />
 * <br />
 * or
 * <pre>
 * &lt;BackupRequest>
 *    &lt;backup method="delete" 
 *    [target="{path to backup target}"] before="{full backup label}|{date}"]/>
 * &lt;/BackupRequest>
 * </pre>
 * date is in YYYY/MM/DD[-hh:mm:ss] or nn{d|m|y} where d = day, m = month, y = year
 * <br />
 * <br />
 * E.g., before="7d" means delete backups older than 7 days
 * <br />
 * before="1y" means delete backups older than a year.
 * <br />
 * <pre>
 * &lt;BackupResponse>
 *    [&lt;backup label="{full backup set label}" incr-label="{incremental backup label}"/>]
 * &lt;/BackupResponse>
 * </pre>
 * <br />
 * label attr is only meaningful upon return from full backup.
 * <br />
 * During incremental backup, a full backup may be performed if one has never been done for some of the accounts.
 * In that case, both label and incr-label attrs are set in the response.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_BACKUP_REQUEST)
public class BackupRequest {

    /**
     * @zm-api-field-description Backup specification
     */
    @XmlElement(name=BackupConstants.E_BACKUP /* backup */, required=true)
    private final BackupSpec backup;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BackupRequest() {
        this((BackupSpec) null);
    }

    public BackupRequest(BackupSpec backup) {
        this.backup = backup;
    }

    public BackupSpec getBackup() { return backup; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("backup", backup);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

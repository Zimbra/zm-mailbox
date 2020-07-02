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
import com.zimbra.soap.admin.type.RestoreSpec;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Perform an action related to a Restore from backup
 * <ul>
 * <li> When includeIncrementals is 1 (true), any incremental backups from the last full backup are also restored.
 *      Default to 1 (true).
 * <li> when sysData is 1 (true), restore system tables and local config.
 * <li> if label is not specified, restore from the latest full backup.
 * <li> prefix is used to produce new account names if the name is reused or a new account is to be created
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_RESTORE_REQUEST)
public class RestoreRequest {

    /**
     * @zm-api-field-description Restore specification
     */
    @XmlElement(name=BackupConstants.E_RESTORE /* restore */, required=true)
    private final RestoreSpec restore;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RestoreRequest() {
        this((RestoreSpec) null);
    }

    public RestoreRequest(RestoreSpec restore) {
        this.restore = restore;
    }

    public RestoreSpec getRestore() { return restore; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("restore", restore);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

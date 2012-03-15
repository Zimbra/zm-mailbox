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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.RestoreSpec;

/**
 * @zm-api-command-network-edition
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("restore", restore);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

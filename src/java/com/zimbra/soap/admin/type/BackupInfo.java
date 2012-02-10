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
public class BackupInfo {

    /**
     * @zm-api-field-tag full-backup-set-label
     * @zm-api-field-description Full backup set label
     */
    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-tag incremental-backup-label
     * @zm-api-field-description Incremental backup label
     */
    @XmlAttribute(name=BackupConstants.A_INCR_LABEL /* incr-label */, required=false)
    private String incrementalLabel;

    public BackupInfo() {
    }

    public void setLabel(String label) { this.label = label; }
    public void setIncrementalLabel(String incrementalLabel) {
        this.incrementalLabel = incrementalLabel;
    }
    public String getLabel() { return label; }
    public String getIncrementalLabel() { return incrementalLabel; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("label", label)
            .add("incrementalLabel", incrementalLabel);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

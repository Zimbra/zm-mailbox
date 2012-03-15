/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.replication.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.ReplicationConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ReplicationMasterCatchupStatus {

    /**
     * @zm-api-field-tag num-remaining-files
     * @zm-api-field-description Number of remaining files
     */
    @XmlAttribute(name=ReplicationConstants.A_REMAINING_FILES /* remainingFiles */, required=true)
    private final int remaingingFiles;

    /**
     * @zm-api-field-tag num-remaining-bytes
     * @zm-api-field-description Number of remaining bytes
     */
    @XmlAttribute(name=ReplicationConstants.A_REMAINING_BYTES /* remainingBytes */, required=true)
    private final long remaingingBytes;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReplicationMasterCatchupStatus() {
        this(-1, -1L);
    }

    public ReplicationMasterCatchupStatus(int remaingingFiles, long remaingingBytes) {
        this.remaingingFiles = remaingingFiles;
        this.remaingingBytes = remaingingBytes;
    }

    public int getRemaingingFiles() { return remaingingFiles; }
    public long getRemaingingBytes() { return remaingingBytes; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("remaingingFiles", remaingingFiles)
            .add("remaingingBytes", remaingingBytes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}

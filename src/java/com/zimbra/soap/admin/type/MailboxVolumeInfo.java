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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MailboxVolumeInfo {

    /**
     * @zm-api-field-tag volume-id
     * @zm-api-field-description Volume ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private short id;

    /**
     * @zm-api-field-tag volume-type
     * @zm-api-field-description Volume type
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_TYPE /* type */, required=true)
    private short volumeType;

    /**
     * @zm-api-field-tag volume-rootpath
     * @zm-api-field-description Root of the mailbox data
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_ROOTPATH /* rootpath */, required=true)
    private String volumeRootpath;

    private MailboxVolumeInfo() {
    }

    private MailboxVolumeInfo(short id, short volumeType, String volumeRootpath) {
        setId(id);
        setVolumeType(volumeType);
        setVolumeRootpath(volumeRootpath);
    }

    public static MailboxVolumeInfo createForIdTypeAndRootpath(short id, short volumeType, String volumeRootpath) {
        return new MailboxVolumeInfo(id, volumeType, volumeRootpath);
    }

    public void setId(short id) { this.id = id; }
    public void setVolumeType(short volumeType) { this.volumeType = volumeType; }
    public void setVolumeRootpath(String volumeRootpath) { this.volumeRootpath = volumeRootpath; }
    public short getId() { return id; }
    public short getVolumeType() { return volumeType; }
    public String getVolumeRootpath() { return volumeRootpath; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("volumeType", volumeType)
            .add("volumeRootpath", volumeRootpath);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

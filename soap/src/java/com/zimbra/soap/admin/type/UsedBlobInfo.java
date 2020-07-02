/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class UsedBlobInfo {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description Blob ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private final int id;

    /**
     * @zm-api-field-tag rev
     * @zm-api-field-description Revision
     */
    @XmlAttribute(name=AdminConstants.A_REVISION /* rev */, required=true)
    private final int revision;

    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Size
     */
    @XmlAttribute(name=AdminConstants.A_SIZE /* s */, required=true)
    private final long size;

    /**
     * @zm-api-field-tag volume-id
     * @zm-api-field-description Volume ID
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_ID /* volumeId */, required=true)
    private final short volumeId;

    /**
     * @zm-api-field-description Blob size information
     */
    @XmlElement(name=AdminConstants.E_BLOB /* blob */, required=true)
    private BlobSizeInfo blob;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private UsedBlobInfo() {
        this(-1, -1, -1L, (short)-1);
    }

    public UsedBlobInfo(int id, int revision, long size, short volumeId) {
        this.id = id;
        this.revision = revision;
        this.size = size;
        this.volumeId = volumeId;
    }

    public void setBlob(BlobSizeInfo blob) { this.blob = blob; }
    public int getId() { return id; }
    public int getRevision() { return revision; }
    public long getSize() { return size; }
    public short getVolumeId() { return volumeId; }
    public BlobSizeInfo getBlob() { return blob; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("revision", revision)
            .add("size", size)
            .add("volumeId", volumeId)
            .add("blob", blob);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}

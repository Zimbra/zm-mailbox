/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016, 2022 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public final class VolumeInfo {

    /**
     * @zm-api-field-tag volume-id
     * @zm-api-field-description Volume ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=false)
    private short id = -1;

    /**
     * @zm-api-field-tag volume-name
     * @zm-api-field-description Name or description of volume
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag volume-root-path
     * @zm-api-field-description Absolute path to root of volume, e.g. /opt/zimbra/store
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_ROOTPATH /* rootpath */, required=true)
    private String rootPath;

    /**
     * @zm-api-field-tag volume-type
     * @zm-api-field-description Volume type
     * <table>
     * <tr> <td> <b>1</b> </td> <td> Primary message volume </td> </tr>
     * <tr> <td> <b>2</b> </td> <td> Secondary message volume </td> </tr>
     * <tr> <td> <b>10</b> </td> <td> index volume </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_TYPE /* type */, required=true)
    private short type = -1;

    /**
     * @zm-api-field-tag compress-blobs
     * @zm-api-field-description Specifies whether blobs in this volume are compressed
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_COMPRESS_BLOBS /* compressBlobs */, required=false)
    private boolean compressBlobs = false;

    /**
     * @zm-api-field-tag compression-threshold
     * @zm-api-field-description Long value that specifies the maximum uncompressed file size, in bytes, of blobs
     * that will not be compressed (in other words blobs larger than this threshold are compressed)
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD /* compressionThreshold */, required=false)
    private long compressionThreshold = 4096;

    /**
     * @zm-api-field-description mgbits
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_MGBITS /* mgbits */, required=false)
    private short mgbits;

    /**
     * @zm-api-field-description mbits
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_MBITS /* mbits */, required=false)
    private short mbits;

    /**
     * @zm-api-field-description fgbits
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_FGBITS /* fgbits */, required=false)
    private short fgbits;

    /**
     * @zm-api-field-description fbits
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_FBITS /* fbits */, required=false)
    private short fbits;

    /**
     * @zm-api-field-description Set if the volume is current.
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_CURRENT /* current */, required=false)
    private boolean current = false;

    /**
     * @zm-api-field-description Set to 1 for internal volumes and 2 for external volumes
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_STORE_TYPE /* storeType */, required=false)
    private short storeType = 1;

    @XmlAttribute(name=AdminConstants.A_VOLUME_STORE_MANAGER_CLASS /* storeManagerClass*/, required=false)
    private String storeManagerClass;

    /**
     * @zm-api-field-description Volume external information for S3
     */
    @XmlElement(name=AdminConstants.E_VOLUME_EXT /* volumeExternalInfo */, required=false)
    private VolumeExternalInfo volumeExternalInfo;

    /**
     * @zm-api-field-description Volume external information for OpenIO
     */
    @XmlElement(name=AdminConstants.E_VOLUME_OPENIO_EXT /* volumeExternalOpenIOInfo */, required=false)
    private VolumeExternalOpenIOInfo volumeExternalOpenIOInfo;

    /**
     * @zm-api-field-description Specifies requestType
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_REQUEST_TYPE /* requestType */, required=false)
    private String requestType = "";

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public void setId(short value) {
        id = value;
    }

    public short getId() {
        return id;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setRootPath(String value) {
        rootPath = value;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setType(short value) {
        type = value;
    }

    public short getType() {
        return type;
    }

    public void setCompressBlobs(Boolean value) {
        compressBlobs = value;
    }

    public boolean isCompressBlobs() {
        return compressBlobs;
    }

    public void setCompressionThreshold(long value) {
        compressionThreshold = value;
    }

    public long getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setMgbits(short value) {
        mgbits = value;
    }

    public short getMgbits() {
        return mgbits;
    }

    public void setMbits(short value) {
        mbits = value;
    }

    public short getMbits() {
        return mbits;
    }

    public void setFgbits(short value) {
        fgbits = value;
    }

    public short getFgbits() {
        return fgbits;
    }

    public void setFbits(short value) {
        fbits = value;
    }

    public short getFbits() {
        return fbits;
    }

    public void setCurrent(boolean value) {
        current = value;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setVolumeExternalInfo(VolumeExternalInfo value) {
        volumeExternalInfo = value;
    }

    public VolumeExternalInfo getVolumeExternalInfo() {
        return volumeExternalInfo;
    }

    public void setStoreType(short value) {
        storeType = value;
    }

    public short getStoreType() {
        return storeType;
    }

    public VolumeExternalOpenIOInfo getVolumeExternalOpenIOInfo() {
        return volumeExternalOpenIOInfo;
    }

    public void setVolumeExternalOpenIOInfo(VolumeExternalOpenIOInfo volumeExternalOpenIOInfo) {
        this.volumeExternalOpenIOInfo = volumeExternalOpenIOInfo;
    }

    public String getStoreManagerClass() {
        return storeManagerClass;
    }

    public void setStoreManagerClass(String storeManagerClass) {
        this.storeManagerClass = storeManagerClass;
    }
}

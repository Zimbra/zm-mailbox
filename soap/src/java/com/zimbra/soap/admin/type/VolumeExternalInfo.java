/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VolumeExternalInfo {

    /**
     * @zm-api-field-description Set to 1 for Internal and 2 for External.
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_STORAGE_TYPE /* storageType */, required=false)
    private String storageType = "S3";

    /**
     * @zm-api-field-description Prefix for bucket location e.g. server1_primary
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_VOLUME_PREFIX /* volumePrefix */, required=false)
    private String volumePrefix = "";

    /**
     * @zm-api-field-description Specifies global bucket configuration Id
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID /* globalBucketConfigId */, required=true)
    private String globalBucketConfigId = "";

    /**
     * @zm-api-field-description Specifies frequent access enabled or not
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS /* useInFrequentAccess */, required=false)
    private boolean useInFrequentAccess = false;

    /**
     * @zm-api-field-description Specifies threshold value of useInFrequentAccess
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD /* useInFrequentAccessThreshold */, required=false)
    private int useInFrequentAccessThreshold = 65536;

    /**
     * @zm-api-field-description Specifies intelligent tiering enabled or not
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING /* useIntelligentTiering */, required=false)
    private boolean useIntelligentTiering = false;

    public void setStorageType(String value) {
        storageType = value;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setVolumePrefix(String value) {
        volumePrefix = value;
    }

    public String getVolumePrefix() {
        return volumePrefix;
    }

    public void setGlobalBucketConfigurationId(String value) {
        globalBucketConfigId = value;
    }

    public String getGlobalBucketConfigurationId() {
        return globalBucketConfigId;
    }

    public void setUseInFrequentAccess(boolean value) {
        useInFrequentAccess = value;
    }

    public boolean isUseInFrequentAccess() {
        return useInFrequentAccess;
    }

    public void setUseIntelligentTiering(boolean value) {
        useIntelligentTiering = value;
    }

    public boolean isUseIntelligentTiering() {
        return useIntelligentTiering;
    }

    public void setUseInFrequentAccessThreshold(int value) {
        useInFrequentAccessThreshold = value;
    }

    public int getUseInFrequentAccessThreshold() {
        return useInFrequentAccessThreshold;
    }
}

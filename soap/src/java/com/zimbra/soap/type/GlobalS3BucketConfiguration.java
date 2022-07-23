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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.GlobalExternalStoreConfigConstants;

@XmlAccessorType(XmlAccessType.NONE)
public final class GlobalS3BucketConfiguration {

    /**
     * @zm-api-field-tag globalBucketUUID
     * @zm-api-field-description Global Bucket UUID
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID, /* globalBucketUUID */ required=false)
    private String globalBucketUUID;

    /**
     * @zm-api-field-tag bucketName
     * @zm-api-field-description Global Bucket Name
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_BUCKET_NAME, /* bucketName */ required=false)
    private String bucketName;

    /**
     * @zm-api-field-tag storeProvider
     * @zm-api-field-description Global Store Provider
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_STORE_PROVIDER, /* storeProvider */ required=false)
    private String storeProvider;

    /**
     * @zm-api-field-tag protocol
     * @zm-api-field-description Global Protocol
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_PROTOCOL, /* protocol */ required=false)
    private String protocol;

    /**
     * @zm-api-field-tag accessKey
     * @zm-api-field-description Global Access Key
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_ACCESS_KEY, /* accessKey */ required=false)
    private String accessKey;

    /**
     * @zm-api-field-tag secret
     * @zm-api-field-description Global Secret
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_SECRET_KEY, /* secret */ required=false)
    private String secretKey;

    /**
     * @zm-api-field-tag region
     * @zm-api-field-description Global Region
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_REGION, /* region */ required=false)
    private String region;

    /**
     * @zm-api-field-tag destinationPath
     * @zm-api-field-description Global Destination Path
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_DESTINATION_PATH, /* destinationPath */ required=false)
    private String destinationPath;

    /**
     * @zm-api-field-tag url
     * @zm-api-field-description Global Url
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_URL, /* url */ required=false)
    private String url;

    /**
     * @zm-api-field-tag bucketStatus
     * @zm-api-field-description Global Bucket Status
     */
    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_BUCKET_STATUS, /* bucketStatus */ required=false)
    private String bucketStatus;

    public String getGlobalBucketUUID() {
        return globalBucketUUID;
    }

    public void setGlobalBucketUUID(String globalBucketUUID) {
        this.globalBucketUUID = globalBucketUUID;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getStoreProvider() {
        return storeProvider;
    }

    public void setStoreProvider(String storeProvider) {
        this.storeProvider = storeProvider;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBucketStatus() {
        return bucketStatus;
    }

    public void setBucketStatus(String bucketStatus) {
        this.bucketStatus = bucketStatus;
    }

}
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

package com.zimbra.soap.admin.message;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.GlobalExternalStoreConfigConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get S3 Bucket Config
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_VALIDATE_S3_BUCKET_REACHABLE_REQUEST)
public class ValidateS3BucketReachableRequest extends AdminAttrsImpl {

    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_URL /* url */, required=true)
    private String url;

    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_BUCKET_NAME /* bucketName */, required=true)
    private String bucketName;

    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_REGION /* region */, required=false)
    private String region;

    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_ACCESS_KEY /* accessKey */, required=true)
    private String accessKey;

    @XmlAttribute(name=GlobalExternalStoreConfigConstants.A_S3_SECRET_KEY /* secretKey */, required=true)
    private String secretKey;

    @Override
    public String toString() {
        return "ValidateS3BucketReachableRequest [url=" + url + ", bucketName=" + bucketName + ", region=" + region
                + ", accessKey=" + accessKey + ", secretKey=" + secretKey + "]";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
       return region;
    }

    public void setRegion(String region) {
       this.region = region;
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

    public ValidateS3BucketReachableRequest() {
    }

}

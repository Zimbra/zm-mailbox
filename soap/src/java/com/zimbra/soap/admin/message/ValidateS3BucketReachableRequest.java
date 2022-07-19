/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Synacor, Inc.
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
import com.zimbra.soap.admin.type.AdminAttrsImpl;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get S3 Bucket Config
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_VALIDATE_S3_BUCKET_REACHABLE_REQUEST)
public class ValidateS3BucketReachableRequest {
	
	 
    @XmlAttribute(name=AccountConstants.URL /* name */, required=true)
    private String url;
    
   
    @XmlAttribute(name=AccountConstants.BUCKET_NAME /* name */, required=true)
    private String bucketName;
    
   
    @XmlAttribute(name=AccountConstants.REGION /* name */, required=true)
    private String region;
    
   
    @XmlAttribute(name=AccountConstants.ACCESS_KEY /* name */, required=true)
    private String accessKey;
    
    
    @XmlAttribute(name=AccountConstants.SECRATE_KEY /* name */, required=true)
    private String secrateKey;
    
    

    @Override
    public String toString() {
        return "ValidateS3BucketReachableRequest [url=" + url + ", bucketName=" + bucketName + ", region=" + region
                + ", accessKey=" + accessKey + ", secrateKey=" + secrateKey + "]";
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



    public String getSecrateKey() {
      return secrateKey;
    }



    public void setSecrateKey(String secrateKey) {
       this.secrateKey = secrateKey;
    }



    public ValidateS3BucketReachableRequest() {
    }

}
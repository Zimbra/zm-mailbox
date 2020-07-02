/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_SAVE_SMIME_CERTIFICATE_REQUEST)
public class SaveSmimeCertificateRequest {

	/**
     * @zm-api-field-description Upload specification
     */
    @XmlElement(name=MailConstants.E_UPLOAD /* upload */, required=true)
    private Id upload;
    
    /**
     * @zm-api-field-tag smime-certificate-password
     * @zm-api-field-description password for smime certificate
     */
    @XmlAttribute(name=SmimeConstants.A_CERTIFICATE_PASSWORD /* password */, required=false)
    private String password;
    
    /**
     * @zm-api-field-tag smime-certificate-replaceId
     * @zm-api-field-description Id of the certificate to be replaced
     */
    @XmlAttribute(name=SmimeConstants.A_REPLACE_ID /* replaceId */, required=false)
    private String replaceId;

	public SaveSmimeCertificateRequest() {
		this(null);
	}

    /**
	 * @param upload
	 */
	public SaveSmimeCertificateRequest(Id upload) {
		this.upload = upload;
	}

	public void setUpload(Id upload) { this.upload = upload; }
    public Id getUpload() { return upload; }
    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }
    public void setReplaceId(String replaceId) { this.replaceId = replaceId; }
    public String getReplaceId() { return replaceId; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("upload", upload.getId())
            .add("password", password)
            .add("replaceId", replaceId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}

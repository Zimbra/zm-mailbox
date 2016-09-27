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
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SmimeConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_SAVE_SMIME_CERTIFICATE_RESPONSE)
public class SaveSmimeCertificateResponse {

	/**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Smime Certificate Id
     */
    @XmlAttribute(name=SmimeConstants.A_PUB_CERT_ID /* smime certificate id */, required=false)
    private String pubCertId;
    
	/**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Smime Certificate Key Id
     */
    @XmlAttribute(name=SmimeConstants.A_PVT_KEY_ID /* smime certificate key id */, required=false)
    private String pvtKeyId;

	public SaveSmimeCertificateResponse() {
	}

	public void setPubCertId(String pubCertId) { this.pubCertId = pubCertId; }
    public String getPubCertId() { return pubCertId; }
    public void setPvtKeyId(String pvtKeyId) { this.pvtKeyId = pvtKeyId; }
    public String getPvtKeyId() { return pvtKeyId; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("pubCertId", pubCertId)
            .add("pvtKeyId", pvtKeyId);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
    
}

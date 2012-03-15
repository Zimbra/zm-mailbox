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

package com.zimbra.soap.appblast.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AppBlastConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AppBlastConstants.E_FINISH_EDIT_RESPONSE)
public class FinishEditDocumentResponse {

    /**
     * @zm-api-field-tag edit-url
     * @zm-api-field-description Edit URL
     */
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=true)
    private String url;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FinishEditDocumentResponse() {
        this((String) null);
    }

    private FinishEditDocumentResponse(String url) {
        this.url = url;
    }

    public static FinishEditDocumentResponse createForUrl(String url) {
        return new FinishEditDocumentResponse(url);
    }

    public void setUrl(String url) { this.url = url; }
    public String getUrl() { return url; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("url", url);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}

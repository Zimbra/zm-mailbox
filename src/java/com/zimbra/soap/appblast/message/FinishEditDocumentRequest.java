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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AppBlastConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;

/**
 * @zm-api-command-description Finish Edit
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AppBlastConstants.E_FINISH_EDIT_REQUEST)
public class FinishEditDocumentRequest {

    /**
     * @zm-api-field-description Document specification
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=true)
    private Id doc;

    private FinishEditDocumentRequest() {
    }

    private FinishEditDocumentRequest(Id doc) {
        setDoc(doc);
    }

    public static FinishEditDocumentRequest create(Id doc) {
        return new FinishEditDocumentRequest(doc);
    }

    public static FinishEditDocumentRequest create(String id) {
        return new FinishEditDocumentRequest(new Id(id));
    }

    public void setDoc(Id doc) { this.doc = doc; }
    public Id getDoc() { return doc; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}

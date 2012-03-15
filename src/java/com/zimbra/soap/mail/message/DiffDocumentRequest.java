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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.DiffDocumentVersionSpec;

/**
 * @zm-api-command-description Performs line by line diff of two revisions of a Document then returns a list of
 * <b>&lt;chunk/></b> containing the result.  Sections of text that are identical to both versions are indicated with
 * disp="common".  For each conflict the chunk will show disp="first", disp="second" or both.
 * <pre>
 *     v3:
 *     line 1
 *     line 2
 *     line 3
 *     line 4
 *     line 5
 *
 *     v4:
 *     line 1
 *     line 2
 *     line 3.6
 *     line 4
 *     line 5
 *
 *     &lt;DiffDocumentRequest xmlns:ns0="urn:zimbraMail">
 *       &lt;doc v1="3" v2="4" id="641"/>
 *     &lt;/DiffDocumentRequest>
 *
 *     &lt;DiffDocumentResponse xmlns:ns0="urn:zimbraMail">
 *       &lt;chunk disp="common">line 1
 *     line 2&lt;/chunk>
 *       &lt;chunk disp="first">line 3&lt;/chunk>
 *       &lt;chunk disp="second">line 3.6&lt;/chunk>
 *       &lt;chunk disp="common">line 4
 *     line 5&lt;/chunk>
 *     &lt;/DiffDocumentResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_DIFF_DOCUMENT_REQUEST)
public class DiffDocumentRequest {

    /**
     * @zm-api-field-description Diff document version specification
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=false)
    private DiffDocumentVersionSpec doc;

    public DiffDocumentRequest() {
    }

    public void setDoc(DiffDocumentVersionSpec doc) { this.doc = doc; }
    public DiffDocumentVersionSpec getDoc() { return doc; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.DiffDocumentVersionSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}

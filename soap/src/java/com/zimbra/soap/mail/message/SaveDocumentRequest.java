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
import com.zimbra.soap.mail.type.DocumentSpec;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Save Document
 * <br />
 * <br />
 * One mechanism for Creating and updating a Document is:
 * <ol>
 * <li> Use FileUploadServlet to upload the document
 * <li> Call SaveDocumentRequest using the upload-id returned from FileUploadServlet.
 * </ol>
 * A Document represents a file.  A file can be created by uploading to FileUploadServlet.  Or it can refer to an
 * attachment of an existing message.
 * <br />
 * <br />
 * Documents are versioned.  The server maintains the metadata of each version, such as by who and when the version
 * was edited, and the fragment.
 * <br />
 * <br />
 * When updating an existing Document, the client must supply the id of Document, and the last known version of the
 * document in the 'ver' attribute.  This is used to prevent blindly overwriting someone else's change made after
 * the version this update was based upon.  The update will succeed only when the last known version supplied by the
 * client matches the current version of the item identified by item-id.
 * <br />
 * <br />
 * Saving a new document, as opposed to adding a revision of existing document, should leave the id and ver fields
 * empty in the request.  Then the server checks and see if the named document already exists, if so returns an error.
 * <br />
 * <br />
 * The request should contain either an <b>&lt;upload></b> element or a <b>&lt;msg></b> element, but not both.
 * When <b>&lt;upload></b> is used, the document should first be uploaded to FileUploadServlet, and then use the
 * upload-id from the FileUploadResponse.
 * <br />
 * When <b>&lt;m></b> is used, the document is retrieved from an existing message in the mailbox, identified by the
 * msg-id and part-id.  The content of the document can be inlined in the <b>&lt;content></b> element.
 * The content can come from another document / revision specified in the <b>&lt;doc></b> sub element.
 * <br />
 * Examples:
 * <br />
 * <br />
 * Saving a new document:
 * <pre>
 *     &lt;SaveDocumentRequest xmlns:ns0="urn:zimbraMail">
 *       &lt;doc>
 *         &lt;upload id="18baa043-394f-42ae-be8a-110b279cb696:cc2f2fdf-7957-4412-aa83-6433662ce5d0"/>
 *       &lt;/doc>
 *     &lt;/SaveDocumentRequest>
 *
 *     &lt;SaveDocumentResponse xmlns:ns0="urn:zimbraMail">
 *       &lt;doc ver="1" id="574" name="PICT0370.JPG"/>
 *     &lt;/SaveDocumentResponse>
 * </pre>
 * Updating an existing document
 * <pre>
 *     &lt;SaveDocumentRequest xmlns:ns0="urn:zimbraMail">
 *       &lt;doc ver="1" id="574" desc="rev 2.0">
 *         &lt;upload id="18baa043-394f-42ae-be8a-110b279cb696:fcb572ce-2a81-4ad3-b55b-cb998c47b416"/>
 *       &lt;/doc>
 *     &lt;/SaveDocumentRequest>
 *
 *     &lt;SaveDocumentResponse xmlns:ns0="urn:zimbraMail">
 *       &lt;doc ver="2" id="574" name="PICT0370.JPG"/>
 *     &lt;/SaveDocumentResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SAVE_DOCUMENT_REQUEST)
public class SaveDocumentRequest {

    /**
     * @zm-api-field-description Document specification
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=true)
    private DocumentSpec doc;

    public SaveDocumentRequest() {
    }

    public void setDoc(DocumentSpec doc) { this.doc = doc; }
    public DocumentSpec getDoc() { return doc; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.mail.type.ModifyContactSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Modify Contact
 * <br />
 * When modifying tags, all specified tags are set and all others are unset.  If tn="{tag-names}" is NOT specified
 * then any existing tags will remain set.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MODIFY_CONTACT_REQUEST)
public class ModifyContactRequest {

    /**
     * @zm-api-field-tag replace-mode
     * @zm-api-field-description If set, all attrs and group members in the specified contact are replaced with
     * specified attrs and group members, otherwise the attrs and group members are merged with the existing contact.
     * Unset by default.
     */
    @XmlAttribute(name=MailConstants.A_REPLACE /* replace */, required=false)
    private ZmBoolean replace;

    /**
     * @zm-api-field-tag verbose
     * @zm-api-field-description If unset, the returned <b>&lt;cn></b> is just a placeholder containing the contact ID
     * (i.e. <b>&lt;cn id="{id}"/></b>).  <b>{verbose}</b> is set by default.
     */
    @XmlAttribute(name=MailConstants.A_VERBOSE /* verbose */, required=false)
    private ZmBoolean verbose;

    /**
     * @zm-api-field-tag want-imap-uid
     * @zm-api-field-description Set to return IMAP UID.  (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_WANT_IMAP_UID /* wantImapUid */, required=false)
    private ZmBoolean wantImapUid;

    /**
     * @zm-api-field-tag want-modified-sequence
     * @zm-api-field-description Set to return Modified Sequence.  (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_WANT_MODIFIED_SEQUENCE /* wantModSeq */, required=false)
    private ZmBoolean wantModifiedSequence;

    /**
     * @zm-api-field-description Specification of contact modifications
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=true)
    private final ModifyContactSpec contact;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifyContactRequest() {
        this((ModifyContactSpec) null);
    }

    public ModifyContactRequest(ModifyContactSpec contact) {
        this.contact = contact;
    }

    public void setReplace(Boolean replace) { this.replace = ZmBoolean.fromBool(replace); }
    public void setVerbose(Boolean verbose) { this.verbose = ZmBoolean.fromBool(verbose); }
    public Boolean getReplace() { return ZmBoolean.toBool(replace); }
    public Boolean getVerbose() { return ZmBoolean.toBool(verbose); }
    public ModifyContactSpec getContact() { return contact; }

    public void setWantImapUid(Boolean wantImapUid) { this.wantImapUid = ZmBoolean.fromBool(wantImapUid); }
    public boolean getWantImapUid() { return ZmBoolean.toBool(wantImapUid, false); }
    public void setWantModifiedSequence(Boolean wantModSeq) { this.wantModifiedSequence = ZmBoolean.fromBool(wantModSeq); }
    public boolean getWantModifiedSequence() { return ZmBoolean.toBool(wantModifiedSequence, false); }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("replace", replace)
            .add("verbose", verbose)
            .add("wantImapUid", wantImapUid)
            .add("wantModSeq", wantModifiedSequence)
            .add("contact", contact);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}

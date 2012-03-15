/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ContactSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
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
     * @zm-api-field-description Specification of contact modifications
     */
    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=true)
    private final ContactSpec contact;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifyContactRequest() {
        this((ContactSpec) null);
    }

    public ModifyContactRequest(ContactSpec contact) {
        this.contact = contact;
    }

    public void setReplace(Boolean replace) { this.replace = ZmBoolean.fromBool(replace); }
    public void setVerbose(Boolean verbose) { this.verbose = ZmBoolean.fromBool(verbose); }
    public Boolean getReplace() { return ZmBoolean.toBool(replace); }
    public Boolean getVerbose() { return ZmBoolean.toBool(verbose); }
    public ContactSpec getContact() { return contact; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("replace", replace)
            .add("verbose", verbose)
            .add("contact", contact);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}

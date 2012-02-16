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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ContactSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Create a contact
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_CONTACT_REQUEST)
public class CreateContactRequest {

    /**
     * @zm-api-field-tag verbose
     * @zm-api-field-description If set (defaults to unset) The returned <b>&lt;cn></b> is just a placeholder
     * containing the new contact ID (i.e. <b>&lt;cn id="{id}"/></b>)
     */
    @XmlAttribute(name=MailConstants.A_VERBOSE, required=false)
    private ZmBoolean verbose;

    /**
     * @zm-api-field-description Contact specification
     */
    @XmlElement(name=MailConstants.E_CONTACT, required=true)
    private final ContactSpec contact;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateContactRequest() {
        this((ContactSpec) null);
    }

    public CreateContactRequest(ContactSpec contact) {
        this.contact = contact;
    }

    public void setVerbose(Boolean verbose) { this.verbose = ZmBoolean.fromBool(verbose); }
    public Boolean getVerbose() { return ZmBoolean.toBool(verbose); }
    public ContactSpec getContact() { return contact; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("verbose", verbose)
            .add("contact", contact)
            .toString();
    }
}

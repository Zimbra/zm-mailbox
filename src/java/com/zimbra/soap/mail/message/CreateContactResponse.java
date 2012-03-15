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
import com.zimbra.soap.mail.type.ContactInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_CONTACT_RESPONSE)
public class CreateContactResponse {

    /**
     * @zm-api-field-description Details of the contact.  Note that if verbose was not set in the request,
     * the returned <b>&lt;cn></b> is just a placeholder containing the new contact ID (i.e. <b>&lt;cn id="{id}"/></b>)
     */
    @XmlElement(name=MailConstants.E_CONTACT, required=false)
    private final ContactInfo contact;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateContactResponse() {
        this((ContactInfo) null);
    }

    public CreateContactResponse(ContactInfo contact) {
        this.contact = contact;
    }

    public ContactInfo getContact() { return contact; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("contact", contact)
            .toString();
    }
}

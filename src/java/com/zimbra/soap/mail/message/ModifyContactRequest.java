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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_MODIFY_CONTACT_REQUEST)
public class ModifyContactRequest {

    @XmlAttribute(name=MailConstants.A_REPLACE, required=false)
    private Boolean replace;

    @XmlAttribute(name=MailConstants.A_VERBOSE, required=false)
    private Boolean verbose;

    @XmlElement(name=MailConstants.E_CONTACT, required=true)
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

    public void setReplace(Boolean replace) { this.replace = replace; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
    public Boolean getReplace() { return replace; }
    public Boolean getVerbose() { return verbose; }
    public ContactSpec getContact() { return contact; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("replace", replace)
            .add("verbose", verbose)
            .add("contact", contact)
            .toString();
    }
}

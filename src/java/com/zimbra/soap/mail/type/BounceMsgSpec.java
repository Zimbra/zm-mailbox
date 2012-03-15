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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class BounceMsgSpec {

    /**
     * @zm-api-field-tag id-of-msg-to-resend
     * @zm-api-field-description ID of message to resend
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-description Email addresses
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private List<EmailAddrInfo> emailAddresses = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BounceMsgSpec() {
        this((String) null);
    }

    public BounceMsgSpec(String id) {
        this.id = id;
    }

    public void setEmailAddresses(Iterable <EmailAddrInfo> emailAddresses) {
        this.emailAddresses.clear();
        if (emailAddresses != null) {
            Iterables.addAll(this.emailAddresses,emailAddresses);
        }
    }

    public void addEmailAddresse(EmailAddrInfo emailAddresse) {
        this.emailAddresses.add(emailAddresse);
    }

    public String getId() { return id; }
    public List<EmailAddrInfo> getEmailAddresses() {
        return Collections.unmodifiableList(emailAddresses);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("emailAddresses", emailAddresses);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}

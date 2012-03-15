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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GENERATE_UUID_RESPONSE)
public class GenerateUUIDResponse {

    /**
     * @zm-api-field-tag generated-uuid
     * @zm-api-field-description Generated globally unique UUID
     */
    @XmlValue
    private final String uuid;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GenerateUUIDResponse() {
        this((String) null);
    }

    public GenerateUUIDResponse(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() { return uuid; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("uuid", uuid)
            .toString();
    }
}

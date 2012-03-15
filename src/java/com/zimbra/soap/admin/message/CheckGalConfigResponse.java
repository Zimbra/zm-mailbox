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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.GalContactInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_GAL_CONFIG_RESPONSE)
@XmlType(propOrder = {"code", "message", "galContacts"})
public class CheckGalConfigResponse {

    /**
     * @zm-api-field-description Code
     */
    @XmlElement(name=AdminConstants.E_CODE, required=true)
    private String code;

    /**
     * @zm-api-field-description Message
     */
    @XmlElement(name=AdminConstants.E_MESSAGE, required=false)
    private String message;

    /**
     * @zm-api-field-description Information for GAL contacts
     */
    @XmlElement(name=MailConstants.E_CONTACT, required=false)
    private List <GalContactInfo> galContacts = Lists.newArrayList();

    public CheckGalConfigResponse() {
        this((String)null, (String) null);
    }

    public CheckGalConfigResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public CheckGalConfigResponse setGalContacts(Collection<GalContactInfo> galContacts) {
        this.galContacts.clear();
        if (galContacts != null) {
            this.galContacts.addAll(galContacts);
        }
        return this;
    }

    public CheckGalConfigResponse addGalContact(GalContactInfo galContact) {
        galContacts.add(galContact);
        return this;
    }

    public List<GalContactInfo> getGalContacts() {
        return Collections.unmodifiableList(galContacts);
    }

    public void setCode(String code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}

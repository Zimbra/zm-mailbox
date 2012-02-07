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

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_DOMAIN_MX_RECORD_RESPONSE)
@XmlType(propOrder = {"entries", "code", "message"})
public class CheckDomainMXRecordResponse {

    /**
     * @zm-api-field-description MX Record entries
     */
    @XmlElement(name=AdminConstants.E_ENTRY, required=false)
    private List <String> entries = Lists.newArrayList();

    /**
     * @zm-api-field-description Code - <b>Ok</b> or <b>Failed</b>
     */
    @XmlElement(name=AdminConstants.E_CODE, required=true)
    private String code;

    /**
     * @zm-api-field-description Message associated with <b>code="Failed"</b>
     */
    @XmlElement(name=AdminConstants.E_MESSAGE, required=false)
    private String message;

    public CheckDomainMXRecordResponse() {
    }

    public CheckDomainMXRecordResponse setEntries(Collection<String> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        return this;
    }

    public CheckDomainMXRecordResponse addEntry(String entry) {
        entries.add(entry);
        return this;
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setCode(String code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}

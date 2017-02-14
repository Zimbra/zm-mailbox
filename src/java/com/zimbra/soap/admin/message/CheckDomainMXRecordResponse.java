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

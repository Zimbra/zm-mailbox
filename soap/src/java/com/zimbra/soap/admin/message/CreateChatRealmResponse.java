/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_CHAT_REALM_RESPONSE)
public class CreateChatRealmResponse {
    /**
     * @zm-api-field-tag chatDomainId
     * @zm-api-field-description chat domain id
     */
    @XmlElement(name=AdminConstants.A_ID /* domain id */, required=true)
    private String chatDomainId;

    /**
     * @zm-api-field-tag domainName
     * @zm-api-field-description zimbra domain name
     */
    @XmlElement(name=AdminConstants.A_DOMAIN /* domain name */, required=true)
    private String domainName;

    /**
     * @return the chat domainId
     */
    public String getChatDomainId() {
        return chatDomainId;
    }

    /**
     * @param chatDomainId the chat domainId to set
     */
    public void setChatDomainId(String chatDomainId) {
        this.chatDomainId = chatDomainId;
    }

    /**
     * @return the zimbra domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName the zimbra domainName to set
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
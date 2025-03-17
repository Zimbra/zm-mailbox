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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.DomainSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Create a Chat realm
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_CHAT_REALM_REQUEST)
public class CreateChatRealmRequest  extends AdminAttrsImpl {
    /**
     * @zm-api-field-tag chat-domainId
     * @zm-api-field-description chat domain id, id of sub domain
     */
    @XmlAttribute(name=AdminConstants.A_ID /* domain Id(sub domain) */, required=true)
    private String chatDomainId;

    /**
     * @zm-api-field-description Zimbra domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* zimbra domin */, required=true)
    private final DomainSelector domain;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateChatRealmRequest() {
        this((DomainSelector) null);
    }

    public CreateChatRealmRequest(DomainSelector domain) {
        this.domain = domain;
    }

    public DomainSelector getDomain() {
        return domain;
    }

    /**
     * @return the chat domainId
     */
    public String getChatDomainId() {
        return chatDomainId;
    }

    /**
     * @param chatDomainId the chatDomainId to set
     */
    public void setChatDomainId(String chatDomainId) {
        this.chatDomainId = chatDomainId;
    }
}
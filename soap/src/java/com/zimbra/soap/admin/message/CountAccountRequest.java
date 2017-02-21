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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Count number of accounts by cos in a domain,
 * <br />
 * Note, it doesn't include any account with zimbraIsSystemResource=TRUE, nor does it include any calendar resources.
 * <br />
 * e.g.
 * <pre>
 *     &lt;CountAccountResponse>
 *        &lt;cos id="044382d5-f2c5-4265-9a1a-ac255fce729f" name="basic">123&lt;/cos>
 *        &lt;cos id="723d7cf6-8d76-443a-b5f3-c56563a8b4c7" name="standard">456&lt;/cos>
 *        &lt;cos id="35c6e603-123a-4a08-b485-b00e1f6d1663" name="premium">789&lt;/cos>
 *        &lt;cos id="35c6e603-123a-4a08-b485-b00e1f6d1663" name="default">55&lt;/cos>
 *     &lt;/CountAccountResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_COUNT_ACCOUNT_REQUEST)
@XmlType(propOrder = {})
public class CountAccountRequest {

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private final DomainSelector domain;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public CountAccountRequest() {
        this(null);
    }

    public CountAccountRequest(DomainSelector domain) {
        this.domain = domain;
    }

    public DomainSelector getDomain() { return domain; }
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Clear the MFA session for a specific domain.
 * <br />
 * This operation is restricted to global admins only.
 * <br />
 * e.g. Clear for a specific domain:
 * <pre>
 *     &lt;ClearMFASessionForDomainRequest>
 *        &lt;domain by="name">example.com&lt;/domain>
 *     &lt;/ClearMFASessionForDomainRequest>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_CLEAR_MFA_SESSION_FOR_DOMAIN_REQUEST)
@XmlType(propOrder = {})
public class ClearMFASessionForDomainRequest {

    /**
     * @zm-api-field-tag domain
     * @zm-api-field-description Domain to clear MFA session for.
     */
    @XmlElement(name = AdminConstants.E_DOMAIN, required = true)
    private DomainSelector domain;

    /**
     * @zm-api-field-tag createdBeforeTimestamp
     * @zm-api-field-description Timestamp before which MFA sessions should be cleared (in milliseconds).
     * If not specified, clears all sessions.
     */
    @XmlElement(name = AdminConstants.E_CREATED_BEFORE_TIMESTAMP, required = false)
    private Long createdBeforeTimestamp;

    /**
     * no-argument constructor wanted by JAXB.
     */
    public ClearMFASessionForDomainRequest() {
        this(null, null);
    }

    public ClearMFASessionForDomainRequest(DomainSelector domain) {
        this(domain, null);
    }

    public ClearMFASessionForDomainRequest(DomainSelector domain, Long createdBeforeTimestamp) {
        this.domain = domain;
        this.createdBeforeTimestamp = createdBeforeTimestamp;
    }

    public DomainSelector getDomain() {
        return domain;
    }

    public Long getCreatedBeforeTimestamp() {
        return createdBeforeTimestamp;
    }
}

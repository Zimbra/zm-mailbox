/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DomainSelector {
    @XmlEnum
    public enum DomainBy {
        // case must match protocol
        id, name, virtualHostname, krb5Realm, foreignName;

        public static DomainBy fromString(String s) throws ServiceException {
            try {
                return DomainBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }

        public com.zimbra.common.account.Key.DomainBy toKeyDomainBy()
        throws ServiceException {
            return com.zimbra.common.account.Key.DomainBy.fromString(this.name());
        }
    }

    /**
     * @zm-api-field-tag domain-selector-by
     * @zm-api-field-description Select the meaning of <b>{domain-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY) private final DomainBy domainBy;

    /**
     * @zm-api-field-tag domain-selector-key
     * @zm-api-field-description The key used to identify the domain. Meaning determined by <b>{domain-selector-by}</b>
     */
    @XmlValue private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DomainSelector() {
        this(null, null);
    }

    public DomainSelector(DomainBy by, String key) {
        this.domainBy = by;
        this.key = key;
    }

    public String getKey() { return key; }
    public DomainBy getBy() { return domainBy; }

    public static DomainSelector fromId(String id) {
        return new DomainSelector(DomainBy.id, id);
    }

    public static DomainSelector fromName(String name) {
        return new DomainSelector(DomainBy.name, name);
    }
}

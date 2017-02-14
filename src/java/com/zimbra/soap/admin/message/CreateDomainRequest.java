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

package com.zimbra.soap.admin.message;

import java.util.Collection;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.Attr;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Create a domain
 * <br />
 * Notes:
 * <br />
 * Extra attrs: <b>description</b>, <b>zimbraNotes</b>
 */
@XmlRootElement(name=AdminConstants.E_CREATE_DOMAIN_REQUEST)
public class CreateDomainRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag new-domain-name
     * @zm-api-field-description Name of new domain
     */
    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
    private String name;

    public CreateDomainRequest() {
        this(null, (Collection<Attr>) null);
    }

    public CreateDomainRequest(String name) {
        this(name, (Collection<Attr>) null);

    }
    public CreateDomainRequest(String name, Collection<Attr> attrs) {
        super(attrs);
        this.name = name;
    }
    public CreateDomainRequest(String name, Map<String, ? extends Object> attrs)
    throws ServiceException {
        super(attrs);
        this.name = name;
    }

    public void setName(String name) { this.name = name; }

    public String getName() { return name; }
}

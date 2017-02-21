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

package com.zimbra.soap.admin.type;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class LimitedQuery {

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description Query
     */
    @XmlValue private final String text;
    /**
     * @zm-api-field-tag query-limit
     * @zm-api-field-description Limit.  Default value 10
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private final Long limit;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private LimitedQuery() {
        this((String) null, (Long) null);
    }

    public LimitedQuery(String text, Long limit) {
        this.text = text;
        this.limit = limit;
    }

    public String getText() { return text; }
    public Long getLimit() { return limit; }
}

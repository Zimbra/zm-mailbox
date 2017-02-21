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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

// XmlRootElement is needed for classes referenced via @XmlElementRef
@XmlRootElement(name=AdminConstants.E_QUERY)
@XmlAccessorType(XmlAccessType.NONE)
public class QueueQuery {

    /**
     * @zm-api-field-tag offset
     * @zm-api-field-description Offset
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private final Integer offset;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description Limit the number of queue items to return in the response
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private final Integer limit;

    /**
     * @zm-api-field-description Queue query field
     */
    @XmlElement(name=AdminConstants.E_FIELD, required=false)
    private List<QueueQueryField> fields = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueueQuery() {
        this((Integer) null, (Integer) null);
    }

    public QueueQuery(Integer offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public void setFields(Iterable <QueueQueryField> fields) {
        this.fields.clear();
        if (fields != null) {
            Iterables.addAll(this.fields,fields);
        }
    }

    public QueueQuery addField(QueueQueryField field) {
        this.fields.add(field);
        return this;
    }

    public Integer getOffset() { return offset; }
    public Integer getLimit() { return limit; }
    public List<QueueQueryField> getFields() {
        return Collections.unmodifiableList(fields);
    }
}

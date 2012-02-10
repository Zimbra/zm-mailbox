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

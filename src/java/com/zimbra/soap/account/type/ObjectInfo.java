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

package com.zimbra.soap.account.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.WrappedAttrListSerializer;
import com.zimbra.soap.type.KeyValuePair;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class ObjectInfo {

    /**
     * @zm-api-field-tag object-name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-tag object-id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AccountConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-description Attributes
     */
    @XmlElement(name=AccountConstants.E_A)
    @JsonSerialize(using=WrappedAttrListSerializer.class)
    private final List<KeyValuePair> attrList;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ObjectInfo() {
        this(null, null, null);
    }

    public ObjectInfo(String id, String name, Collection <KeyValuePair> attrs) {
        this.name = name;
        this.id = id;
        this.attrList = new ArrayList<KeyValuePair>();
        if (attrs != null) {
            this.attrList.addAll(attrs);
        }
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public List<? extends KeyValuePair> getAttrList() {
        return Collections.unmodifiableList(attrList);
    }
}

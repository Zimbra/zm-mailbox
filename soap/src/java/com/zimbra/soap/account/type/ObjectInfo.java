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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;

/**
 * Information for an Object - attributes are encoded as Key/Value pairs in JSON - i.e. using "_attrs"
 */
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
    @ZimbraKeyValuePairs
    @XmlElement(name=AccountConstants.E_A /* a */, required=false)
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

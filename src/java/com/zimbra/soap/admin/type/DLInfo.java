/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_COS)
@XmlType(propOrder = {})
public class DLInfo extends AdminObjectInfo {

    /**
     * @zm-api-field-tag group-is-dynamic
     * @zm-api-field-description Flags whether a group is dynamic or not
     */
    @XmlAttribute(name=AdminConstants.A_DYNAMIC, required=false)
    private ZmBoolean dynamic;

    /**
     * @zm-api-field-tag via-dl-name
     * @zm-api-field-description Present if the account is a member of the returned list because they are either a
     * direct or indirect member of another list that is a member of the returned list.
     * For example, if a user is a member of engineering@domain.com, and engineering@domain.com is a member of
     * all@domain.com, then 
     * <pre>
     *     &lt;dl name="all@domain.com" ... via="engineering@domain.com"/>
     * </pre>
     * would be returned.
     */
    @XmlAttribute(name=AdminConstants.A_VIA, required=true)
    private final String via;
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DLInfo() {
        this(null, null, null, null);
    }

    public DLInfo(String id, String name) {
        this(id, name, null, null);
    }

    public DLInfo(String id, String name, Collection <Attr> attrs) {
        this(id, name, null, attrs);
    }

    public DLInfo(String id, String name, String via, Collection <Attr> attrs) {
        super(id, name, attrs);
        this.via = via;
    }

    public String getVia() {
        return via;
    }

    public Boolean isDynamic() {
        return ZmBoolean.toBool(dynamic, false);
    }
}

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

package com.zimbra.soap.admin.message;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.Attr;

/**
 * @zm-api-command-description Modify a calendar resource
 * <br />
 * Notes:
 * <ul>
 * <li> an empty attribute value removes the specified attr
 * <li> this request is by default proxied to the resources's home server
 * </ul>
 * <b>Access</b>: domain admin sufficient. limited set of attributes that can be updated by a domain admin.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_CALENDAR_RESOURCE_REQUEST)
public class ModifyCalendarResourceRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag value-of-zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private String id;

    public ModifyCalendarResourceRequest() {
        this((String)null);
    }

    public ModifyCalendarResourceRequest(String id) {
        this(id, (Collection<Attr>) null);
    }

    public ModifyCalendarResourceRequest(String id, Collection<Attr> attrs) {
        super(attrs);
        setId(id);
    }

    public void setId(String id) { this.id = id; }

    public String getId() { return id; }
}

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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.Attr;

 /*
  * Note: soap-admin.txt said:
  *       A calendar resource does not have a password (you can't login as a resource)
  * Seems to be incorrect - the API has room for a password and can login using ZWC using that password.
  */
/**
 * @zm-api-command-description Create a calendar resource
 * <br />
 * Notes:
 * <ul>
 * <li> A calendar resource is a special type of Account.  The Create, Delete, Modify, Rename, Get, GetAll, and Search
 *      operations are very similar to those of Account.
 * <li> Must specify the <b>displayName</b> and <b>zimbraCalResType</b> attributes
 * </ul>
 * <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_CALENDAR_RESOURCE_REQUEST)
public class CreateCalendarResourceRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag calendar-resource-name
     * @zm-api-field-description Name or calendar resource
     * <br />
     * Must include domain (uid@domain), and domain specified after @ must exist
     */
    @XmlAttribute(name=AdminConstants.E_NAME, required=false)
    private String name;

    /**
     * @zm-api-field-tag calendar-resource-password
     * @zm-api-field-description Password for calendar resource
     */
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=false)
    private String password;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private CreateCalendarResourceRequest() {
        this((String) null, (String) null);
    }

    public CreateCalendarResourceRequest(String name, String password) {
        this(name, password, (Collection<Attr>) null);
    }
    public CreateCalendarResourceRequest(
            String name, String password, Collection<Attr> attrs) {
        setName(name);
        setPassword(password);
        setAttrs(attrs);
    }

    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public String getPassword() { return password; }
}

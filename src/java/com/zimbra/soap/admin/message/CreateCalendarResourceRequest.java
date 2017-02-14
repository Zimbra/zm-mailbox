/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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
    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
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

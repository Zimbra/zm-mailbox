/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
 * @zm-api-command-description Create account
 * <br />
 * Notes:
 * <ul>
 * <li> accounts without passwords can't be logged into
 * <li> name must include domain (uid@name), and domain specified in name must exist
 * <li> default value for <b>zimbraAccountStatus</b> is "active"
 * </ul>
 * <b>Access</b>: domain admin sufficient
 */
@XmlRootElement(name=AdminConstants.E_CREATE_ACCOUNT_REQUEST)
public class CreateAccountRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description New account's name
     * <br />
     * Must include domain (uid@name), and domain specified in name must exist
     */
    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
    private String name;

    /**
     * @zm-api-field-tag account-password
     * @zm-api-field-description New account's password
     */
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=false)
    private String password;

    public CreateAccountRequest() {
        this(null, null, (Collection <Attr>) null);
    }

    public CreateAccountRequest(String name, String password) {
        this(name, password, (Collection <Attr>) null);
    }

    public CreateAccountRequest(String name, String password, Collection<Attr> attrs) {
        setName(name);
        setPassword(password);
        super.setAttrs(attrs);
    }

    public CreateAccountRequest(String name, String password,
            Map<String, ? extends Object> attrs)
    throws ServiceException {
        setName(name);
        setPassword(password);
        super.setAttrs(attrs);
    }

    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public String getPassword() { return password; }
}

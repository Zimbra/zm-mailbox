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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DataSourceSpecifier;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Creates a data source that imports mail items into the specified folder.
 * <br />
 * Notes:
 * <ul>
 * <li> Currently the only type supported is <b>pop3</b>.
 * <li> every attribute value is returned except password.
 * <li> this request is by default proxied to the account's home server
 * </ul>
 * Example:
 * <pre>
 *     &lt;CreateDataSourceRequest/>
 *         &lt;id>{existing-account-id}&lt;/id>
 *         &lt;dataSource type="pop3" name="{data-source-name}">
 *             &lt;a n="zimbraDataSourceName">My POP3 Account&lt;/a>
 *             &lt;a n="zimbraDataSourceIsEnabled">TRUE&lt;/a>
 *             &lt;a n="zimbraDataSourceHost">pop.myisp.com&lt;/a>
 *             &lt;a n="zimbraDataSourcePort">110&lt;/a>
 *             &lt;a n="zimbraDataSourceUsername">mylogin&lt;/a>
 *             &lt;a n="zimbraDataSourcePassword">mypassword&lt;/a>
 *             &lt;a n="zimbraDataSourceFolderId">{folder-id}&lt;/a>
 *        &lt;/dataSource>
 *     &lt;/CreateDataSourceRequest>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_DATA_SOURCE_REQUEST)
public class CreateDataSourceRequest {

    /**
     * @zm-api-field-tag existing-account-id
     * @zm-api-field-description Id for an existing Account
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-description Details of data source
     */
    @XmlElement(name=AccountConstants.E_DATA_SOURCE, required=true)
    private final DataSourceSpecifier dataSource;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateDataSourceRequest() {
        this((String) null, (DataSourceSpecifier) null);
    }

    public CreateDataSourceRequest(String id, DataSourceSpecifier dataSource) {
        this.id = id;
        this.dataSource = dataSource;
    }

    public String getId() { return id; }
    public DataSourceSpecifier getDataSource() { return dataSource; }
}

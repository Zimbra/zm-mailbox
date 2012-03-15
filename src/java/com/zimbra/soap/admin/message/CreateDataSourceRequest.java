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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DataSourceSpecifier;

/**
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

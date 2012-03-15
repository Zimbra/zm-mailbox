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
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.type.Id;

/**
 * @zm-api-command-description Changes attributes of the given data source.
 * Only the attributes specified in the request are modified. To change the name, specify 
 * <b>"zimbraDataSourceName"</b> as an attribute.
 * <br />
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_DATA_SOURCE_REQUEST)
public class ModifyDataSourceRequest extends AdminAttrsImpl {

    // Id for existing Account
    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Existing account ID
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-description Data source specification
     */
    @XmlElement(name=AccountConstants.E_DATA_SOURCE, required=true)
    private final Id dataSource;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifyDataSourceRequest() {
        this((String) null, (Id) null);
    }

    public ModifyDataSourceRequest(String id, Id dataSource) {
        this.id = id;
        this.dataSource = dataSource;
    }

    public String getId() { return id; }
    public Id getDataSource() { return dataSource; }
}

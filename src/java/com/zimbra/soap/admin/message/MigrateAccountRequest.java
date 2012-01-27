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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.IdAndAction;

/**
 * @zm-api-command-description Migrate an account
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MIGRATE_ACCOUNT_REQUEST)
public class MigrateAccountRequest {

    /**
     * @zm-api-field-description Specification for the migration
     */
    @XmlElement(name=AdminConstants.E_MIGRATE, required=true)
    private final IdAndAction migrate;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MigrateAccountRequest() {
        this((IdAndAction) null);
    }

    public MigrateAccountRequest(IdAndAction migrate) {
        this.migrate = migrate;
    }

    public IdAndAction getMigrate() { return migrate; }
}

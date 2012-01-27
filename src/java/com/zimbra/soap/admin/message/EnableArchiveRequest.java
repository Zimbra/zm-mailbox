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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.ArchiveConstants;
import com.zimbra.soap.admin.type.ArchiveSpec;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-description Enable Archive
 * <ul>
 * <li> Archive account is created by default based on name templates.  You can suppress this by sending create=0.
 *      This is useful if you are going to use a third party system to do the archiving and ZCS is just a mail forker.
 * <li> - Recommended that password not be specified so only admins can login.
 * <li> A newly created archive account is always defaulted with the following attributes.  You can override these
 *      attributes (or set additional ones) by specifying <b>&lt;a></b> elements in <b>&lt;archive></b>.
 *      <pre>
 *          amavisBypassSpamChecks: TRUE
 *          amavisBypassVirusChecks: TRUE
 *          zimbraHideInGal: TRUE
 *          zimbraIsSystemResource: TRUE
 *          zimbraMailQuota: 0
 *      </pre>
 * <li> <b>&lt;password></b>, <b>&lt;cos></b> and <b>&lt;a></b> (attr) elements matter only if create=1.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ArchiveConstants.E_ENABLE_ARCHIVE_REQUEST)
public class EnableArchiveRequest {

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=true)
    private final AccountSelector account;

    /**
     * @zm-api-field-description Archive details
     */
    @XmlElement(name=ArchiveConstants.E_ARCHIVE /* archive */, required=false)
    private ArchiveSpec archive;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EnableArchiveRequest() {
        this((AccountSelector) null);
    }

    public EnableArchiveRequest(AccountSelector account) {
        this.account = account;
    }

    public void setArchive(ArchiveSpec archive) { this.archive = archive; }
    public AccountSelector getAccount() { return account; }
    public ArchiveSpec getArchive() { return archive; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("account", account)
            .add("archive", archive);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

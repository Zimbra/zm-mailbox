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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.ArchiveConstants;
import com.zimbra.soap.admin.type.ArchiveSpec;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Create an archive
 * <ul>
 * <li> If <b>&lt;name></b> if not specified, archive account name is computed based on name templates.
 * <li> Recommended that password not be specified so only admins can login.
 * <li> A newly created archive account is always defaulted with the following attributes.  You can override these
 *      attributes (or set additional ones) by specifying <b>&lt;a></b> elements in <b>&lt;archive></b>.
 *      <pre>
 *          amavisBypassSpamChecks: TRUE
 *          amavisBypassVirusChecks: TRUE
 *          zimbraHideInGal: TRUE
 *          zimbraIsSystemResource: TRUE
 *          zimbraMailQuota: 0
 *      </pre>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ArchiveConstants.E_CREATE_ARCHIVE_REQUEST)
public class CreateArchiveRequest {

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
    private CreateArchiveRequest() {
        this((AccountSelector) null);
    }

    public CreateArchiveRequest(AccountSelector account) {
        this.account = account;
    }

    public void setArchive(ArchiveSpec archive) { this.archive = archive; }
    public AccountSelector getAccount() { return account; }
    public ArchiveSpec getArchive() { return archive; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("account", account)
            .add("archive", archive);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

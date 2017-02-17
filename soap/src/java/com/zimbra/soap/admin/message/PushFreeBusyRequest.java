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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Names;
import com.zimbra.soap.type.Id;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Push Free/Busy.
 * <br />
 * The request must include either <b>&lt;domain/></b> or <b>&lt;account/></b>. When <b>&lt;domain/></b> is specified
 * in the request, the server will push the free/busy for all the accounts in the domain to the configured
 * free/busy providers.  When <b>&lt;account/></b> list is specified, the server will push the free/busy for the
 * listed accounts to the providers.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_PUSH_FREE_BUSY_REQUEST)
public class PushFreeBusyRequest {

    /**
     * @zm-api-field-description Domain names specification
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private Names domains;

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private List<Id> accounts = Lists.newArrayList();

    public PushFreeBusyRequest() {
    }

    public void setDomains(Names domains) { this.domains = domains; }
    public void setAccounts(Iterable <Id> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public PushFreeBusyRequest addAccount(Id account) {
        this.accounts.add(account);
        return this;
    }

    public Names getDomains() { return domains; }
    public List<Id> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }
}

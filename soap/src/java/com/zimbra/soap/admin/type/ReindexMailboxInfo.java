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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ReindexMailboxInfo {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ACCOUNTID /* id */, required=true)
    private final String accountId;

    /**
     * @zm-api-field-tag types-comma-sep
     * @zm-api-field-description Comma separated list of types.  Legal values are:
     * <br />
     * <b>conversation|message|contact|appointment|task|note|wiki|document</b>
     */
    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    private String types;

    /**
     * @zm-api-field-tag ids-comma-sep
     * @zm-api-field-description Comma separated list of IDs to re-index
     */
    @XmlAttribute(name=MailConstants.A_IDS /* ids */, required=false)
    private String ids;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReindexMailboxInfo() {
        this((String) null);
    }

    public ReindexMailboxInfo(String accountId) {
        this.accountId = accountId;
        this.setTypes(null);
        this.ids = null;
    }

    public String getAccountId() { return accountId; }
    public String getTypes() { return types; }
    public String getIds() { return ids; }

    public void setTypes(String types) { this.types = types; }
    public void setIds(String ids) { this.ids = ids; }
}

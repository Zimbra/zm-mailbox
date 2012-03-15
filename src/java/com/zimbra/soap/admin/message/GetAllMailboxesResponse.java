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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_MAILBOXES_RESPONSE)
@XmlType(propOrder = {})
public class GetAllMailboxesResponse {

    /**
     * @zm-api-field-description Mailboxes
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=false)
    private List <MailboxInfo> mboxes = Lists.newArrayList();

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description <b>1 (true)</b> if more mailboxes left to return
     */
    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private ZmBoolean more;

    /**
     * @zm-api-field-tag search-total
     * @zm-api-field-description Total number of mailboxes that matched search (not affected by limit/offset)
     */
    @XmlAttribute(name=AdminConstants.A_SEARCH_TOTAL, required=true)
    private int searchTotal;

    public GetAllMailboxesResponse() {
    }

    public GetAllMailboxesResponse setMboxes(Collection <MailboxInfo> mboxes) {
        this.mboxes.clear();
        if (mboxes != null) {
            this.mboxes.addAll(mboxes);
        }
        return this;
    }

    public GetAllMailboxesResponse addMbox(MailboxInfo mbox) {
        mboxes.add(mbox);
        return this;
    }

    public List<MailboxInfo> getMboxes() {
        return Collections.unmodifiableList(mboxes);
    }

    public void setMore(boolean more) {
        this.more = ZmBoolean.fromBool(more);
    }

    public void setSearchTotal(int searchTotal) {
        this.searchTotal = searchTotal;
    }

    public int getSearchTotal() { return searchTotal; }
    public boolean isMore() { return ZmBoolean.toBool(more); }
}

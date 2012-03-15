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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.MessageInfo;

// soap-network-admin.txt implies that this is very similar to SearchResponse (presumably in urn:zimbraMail) but it looks
// like a cut down version with just E_MSG children.  See CrossMailboxSearch.LocalTask.search(mbox, params, writer).
// Also, there are no summary attributes which probably makes sense as we may not know all the results at the start
// of writing the reply.

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_MULTIPLE_MAILBOXES_RESPONSE)
public class SearchMultiMailboxResponse {

    /**
     * @zm-api-field-description Search hits
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private List<MessageInfo> msgs = Lists.newArrayList();

    public SearchMultiMailboxResponse() {
    }

    public void setMsgs(Iterable <MessageInfo> msgs) {
        this.msgs.clear();
        if (msgs != null) {
            Iterables.addAll(this.msgs,msgs);
        }
    }

    public void addMsg(MessageInfo msg) {
        this.msgs.add(msg);
    }

    public List<MessageInfo> getMsgs() {
        return Collections.unmodifiableList(msgs);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("msgs", msgs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

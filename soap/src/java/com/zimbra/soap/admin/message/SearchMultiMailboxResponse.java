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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("msgs", msgs);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

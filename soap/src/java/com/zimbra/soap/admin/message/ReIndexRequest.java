/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ReindexMailboxInfo;
import com.zimbra.soap.admin.type.ServerSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description ReIndex
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REINDEX_REQUEST)
public class ReIndexRequest {

    /**
     * @zm-api-field-tag "start|status|cancel"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>start</b> </td> <td> start reindexing </td> </tr>
     * <tr> <td> <b>status</b> </td> <td> show reindexing progress </td> </tr>
     * <tr> <td> <b>cancel</b> </td> <td> cancel reindexing </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=false)
    private final String action;

    /**
     * @zm-api-field-description Specify mailboxes that will be re-indexed
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=false)
    private List<ReindexMailboxInfo> mbox = Lists.newArrayList();

    /**
     * @zm-api-field-description Server
     * Note: either 'server' or 'mbox' is required to indicate which mailboxes are to be re-indexed
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=false)
    private ServerSelector server;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ReIndexRequest() {
        this((String)null, Lists.newArrayList(), (ServerSelector)null);
    }

    public ReIndexRequest(String action, List<ReindexMailboxInfo> mbox) {
        this(action, mbox, (ServerSelector)null);
    }

    public ReIndexRequest(String action, List<ReindexMailboxInfo> mbox, ServerSelector server) {
        this.action = action;
        this.mbox = mbox;
        this.server = server;
    }

    public String getAction() { return action; }
    public List<ReindexMailboxInfo> getMbox() { return mbox; }
    public void setMbox(Iterable<ReindexMailboxInfo> mailboxes) {
        this.mbox.clear();
        if(mailboxes != null) {
            Iterables.addAll(this.mbox, mailboxes);
        }
    }

    public void addMbox(ReindexMailboxInfo mailbox) {
        this.mbox.add(mailbox);
    }

    public void setServer(ServerSelector server) {
        this.server = server;
    }

    public ServerSelector getServer() {
        return this.server;
    }
}

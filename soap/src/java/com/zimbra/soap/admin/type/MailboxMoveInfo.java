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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.MailboxMoveType;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class MailboxMoveInfo {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account email address
     */
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag move-start-millis
     * @zm-api-field-description Move start time in milliseconds
     */
    @XmlAttribute(name=BackupConstants.A_START /* start */, required=true)
    private long start;

    /**
     * @zm-api-field-description Mailbox move type.  Whether this is a move-out to destination server or move-in
     * from source server
     */
    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=true)
    private MailboxMoveType moveType;

    /**
     * @zm-api-field-tag src-hostname
     * @zm-api-field-description Hostname of source server (this server's howtname if type="out")
     */
    @XmlAttribute(name=BackupConstants.A_SOURCE /* src */, required=true)
    private String source;

    /**
     * @zm-api-field-tag dest-hostname
     * @zm-api-field-description Hostname of destination server (this server's howtname if type="in")
     */
    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private String target;

    /**
     * @zm-api-field-tag no-peer
     * @zm-api-field-description Set if move is NOT in progress on destination server; only used when checkPeer
     * was set in the request.
     * <br />
     * If <b>noPeer</b> is set, it can mean one of:
     * <ol>
     * <li> The peer server is not reachable
     * <li> The peer server doesn't think account is being moved (possible if peer was restarted during a move)
     * <li> Race condition (because move status on multiple servers are not updated/queried transactionally)
     * </ol>
     */
    @XmlAttribute(name=BackupConstants.A_NO_PEER /* noPeer */, required=false)
    private ZmBoolean noPeer;

    public MailboxMoveInfo() {
    }

    public void setName(String name) { this.name = name; }
    public void setStart(long start) { this.start = start; }
    public void setMoveType(MailboxMoveType moveType) { this.moveType = moveType; }
    public void setSource(String source) { this.source = source; }
    public void setTarget(String target) { this.target = target; }
    public void setNoPeer(Boolean noPeer) { this.noPeer = ZmBoolean.fromBool(noPeer); }
    public String getName() { return name; }
    public long getStart() { return start; }
    public MailboxMoveType getMoveType() { return moveType; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
    public Boolean getNoPeer() { return ZmBoolean.toBool(noPeer); }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("start", start)
            .add("moveType", moveType)
            .add("source", source)
            .add("target", target)
            .add("noPeer", noPeer);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}

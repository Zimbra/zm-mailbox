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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

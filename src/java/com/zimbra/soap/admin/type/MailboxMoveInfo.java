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

    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private String name;

    @XmlAttribute(name=BackupConstants.A_START /* start */, required=true)
    private long start;

    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=true)
    private MailboxMoveType moveType;

    @XmlAttribute(name=BackupConstants.A_SOURCE /* src */, required=true)
    private String source;

    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private String target;

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

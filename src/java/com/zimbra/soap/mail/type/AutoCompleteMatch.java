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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class AutoCompleteMatch {

    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=false)
    private String email;

    @XmlAttribute(name=MailConstants.A_MATCH_TYPE /* type */, required=false)
    private String matchType;

    @XmlAttribute(name=MailConstants.A_RANKING /* ranking */, required=false)
    private Integer ranking;

    @XmlAttribute(name=MailConstants.A_IS_GROUP /* isGroup */, required=false)
    private ZmBoolean group;

    @XmlAttribute(name=MailConstants.A_EXP /* exp */, required=false)
    private ZmBoolean canExpandGroupMembers;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private Integer folder;

    @XmlAttribute(name=MailConstants.A_DISPLAYNAME /* display */, required=false)
    private String displayName;

    public AutoCompleteMatch() {
    }

    public void setEmail(String email) { this.email = email; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }
    public void setGroup(Boolean group) { this.group = ZmBoolean.fromBool(group); }
    public void setCanExpandGroupMembers(Boolean canExpandGroupMembers) {
        this.canExpandGroupMembers = ZmBoolean.fromBool(canExpandGroupMembers);
    }
    public void setId(String id) { this.id = id; }
    public void setFolder(Integer folder) { this.folder = folder; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getEmail() { return email; }
    public String getMatchType() { return matchType; }
    public Integer getRanking() { return ranking; }
    public Boolean getGroup() { return ZmBoolean.toBool(group); }
    public Boolean getCanExpandGroupMembers() { return ZmBoolean.toBool(canExpandGroupMembers); }
    public String getId() { return id; }
    public Integer getFolder() { return folder; }
    public String getDisplayName() { return displayName; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("email", email)
            .add("matchType", matchType)
            .add("ranking", ranking)
            .add("group", group)
            .add("canExpandGroupMembers", canExpandGroupMembers)
            .add("id", id)
            .add("folder", folder)
            .add("displayName", displayName);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

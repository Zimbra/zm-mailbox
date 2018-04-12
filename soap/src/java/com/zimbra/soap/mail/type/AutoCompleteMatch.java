/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AutoCompleteMatch {

    /**
     * @zm-api-field-tag email-addresses-for-group
     * @zm-api-field-description Comma-separated email addresses in case of group
     */
    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=false)
    private String email;

    /**
     * @zm-api-field-tag match-type-gal|contact|rankingTable
     * @zm-api-field-description Match type - <b>gal|contact|rankingTable</b>
     */
    @XmlAttribute(name=MailConstants.A_MATCH_TYPE /* type */, required=false)
    private String matchType;

    /**
     * @zm-api-field-tag ranking
     * @zm-api-field-description Ranking
     */
    @XmlAttribute(name=MailConstants.A_RANKING /* ranking */, required=false)
    private Integer ranking;

    /**
     * @zm-api-field-tag is-group
     * @zm-api-field-description Set if the entry is a group
     */
    @XmlAttribute(name=MailConstants.A_IS_GROUP /* isGroup */, required=false)
    private ZmBoolean group;

    /**
     * @zm-api-field-tag can-expand-group-members
     * @zm-api-field-description Set if the user has the right to expand group members.  Returned only if
     * needExp is set in the request and only on group entries (isGroup is set).
     */
    @XmlAttribute(name=MailConstants.A_EXP /* exp */, required=false)
    private ZmBoolean canExpandGroupMembers;

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description String that should be displayed by the client
     */
    @XmlAttribute(name=MailConstants.A_DISPLAYNAME /* display */, required=false)
    private String displayName;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description First Name
     */
    @XmlAttribute(name=MailConstants.A_FIRSTNAME /* first */, required=false)
    private String firstName;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description Middle Name
     */
    @XmlAttribute(name=MailConstants.A_MIDDLENAME /* middle */, required=false)
    private String middleName;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description Last Name
     */
    @XmlAttribute(name=MailConstants.A_LASTNAME /* last */, required=false)
    private String lastName;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description Full Name
     */
    @XmlAttribute(name=MailConstants.A_FULLNAME /* full */, required=false)
    private String fullName;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description Nick Name
     */
    @XmlAttribute(name=MailConstants.A_NICKNAME /* nick */, required=false)
    private String nickname;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description Company Name
     */
    @XmlAttribute(name=MailConstants.A_COMPANY /* company */, required=false)
    private String company;

    /**
     * @zm-api-field-tag display-string
     * @zm-api-field-description FileAs
     */
    @XmlAttribute(name=MailConstants.A_FILEAS /* fileas */, required=false)
    private String fileAs;

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
    public void setFolder(String folder) { this.folder = folder; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setCompany(String company) { this.company = company; }
    public void setFileAs(String fileAs) { this.fileAs = fileAs; }
    public String getEmail() { return email; }
    public String getMatchType() { return matchType; }
    public Integer getRanking() { return ranking; }
    public Boolean getGroup() { return ZmBoolean.toBool(group); }
    public Boolean getCanExpandGroupMembers() { return ZmBoolean.toBool(canExpandGroupMembers); }
    public String getId() { return id; }
    public String getFolder() { return folder; }
    public String getDisplayName() { return displayName; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getNickname() { return nickname; }
    public String getCompany() { return company; }
    public String getFileAs() { return fileAs; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("email", email)
            .add("matchType", matchType)
            .add("ranking", ranking)
            .add("group", group)
            .add("canExpandGroupMembers", canExpandGroupMembers)
            .add("id", id)
            .add("folder", folder)
            .add("displayName", displayName)
            .add("first", firstName)
            .add("middle", middleName)
            .add("last", lastName)
            .add("full", fullName)
            .add("nick", nickname)
            .add("company", company)
            .add("fileas", fileAs);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}

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

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"metadatas", "subject", "fragment", "emails"})
@GraphQLType(name=GqlConstants.CLASS_CONVERSATION_SUMMARY, description="Conversation search result information")
public class ConversationSummary {

    /**
     * @zm-api-field-tag conv-id
     * @zm-api-field-description Conversation ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag num-msgs
     * @zm-api-field-description Number of messages in conversation without IMAP \Deleted flag set
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=false)
    private Integer num;

    /**
     * @zm-api-field-tag num-unread-msgs
     * @zm-api-field-description Number of unread messages in conversation
     */
    @XmlAttribute(name=MailConstants.A_UNREAD /* u */, required=false)
    private Integer numUnread;

    /**
     * @zm-api-field-tag all-msgs
     * @zm-api-field-description Total number of messages in conversation including those with the IMAP \Deleted flag
     * set
     */
    @XmlAttribute(name=MailConstants.A_TOTAL_SIZE /* total */, required=false)
    private Integer totalSize;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Same flags as on <b>&lt;m></b> ("sarwfdxnu!?"), aggregated from all the
     * conversation's messages
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-tag date-of-most-recent
     * @zm-api-field-description Date (secs since epoch) of most recent message in the converstation
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    /**
     * @zm-api-field-tag elided
     * @zm-api-field-description If elided is set, some participants are missing before the first returned
     * <b>&lt;e></b> element
     */
    @XmlAttribute(name=MailConstants.A_ELIDED /* elided */, required=false)
    private ZmBoolean elided;

    /**
     * @zm-api-field-tag change-date
     * @zm-api-field-description Modified date in seconds
     */
    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    /**
     * @zm-api-field-tag modified-sequence
     * @zm-api-field-description Modified sequence
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-description Custom metadata
     */
    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private final List<MailCustomMetadata> metadatas = Lists.newArrayList();

    /**
     * @zm-api-field-tag conversation-subject
     * @zm-api-field-description Subject of conversation
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    /**
     * @zm-api-field-description Email information for conversation participants if available (elided will be set
     * if information for some participants is missing)
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private final List<EmailInfo> emails = Lists.newArrayList();

    public ConversationSummary() {
        this((String) null);
    }

    public ConversationSummary(String id) {
        this.setId(id);
    }

    public void setId(String id) { this.id = id; }
    public void setNum(Integer num) { this.num = num; }
    public void setNumUnread(Integer numUnread) { this.numUnread = numUnread; }
    public void setTotalSize(Integer totalSize) { this.totalSize = totalSize; }
    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setDate(Long date) { this.date = date; }
    public void setElided(Boolean elided) { this.elided = ZmBoolean.fromBool(elided); }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setMetadatas(Iterable <MailCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(MailCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setEmails(Iterable <EmailInfo> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public void addEmail(EmailInfo email) {
        this.emails.add(email);
    }

    @GraphQLQuery(name=GqlConstants.ID, description="Conversation ID")
    public String getId() { return id; }
    @GraphQLQuery(name=GqlConstants.NUM, description="Number of messages in conversation without IMAP \\Deleted flag set")
    public Integer getNum() { return num; }
    @GraphQLQuery(name=GqlConstants.NUM_UNREAD, description="Number of unread messages in conversation")
    public Integer getNumUnread() { return numUnread; }
    @GraphQLQuery(name=GqlConstants.TOTAL_SIZE, description="Total number of messages in conversation including those with the IMAP \\Deleted flag")
    public Integer getTotalSize() { return totalSize; }
    @GraphQLQuery(name=GqlConstants.FLAGS, description="Flags set on the conversation. (u)nread, (f)lagged, has (a)ttachment, (r)eplied, (s)ent by me, for(w)arded, calendar in(v)ite, (d)raft, IMAP-\\Deleted (x), (n)otification sent, urgent (!), low-priority (?), priority (+)")
    public String getFlags() { return flags; }
    @GraphQLQuery(name=GqlConstants.TAGS, description="Tags - Comma separated list of integers.  DEPRECATED - use \"tagNames\" instead")
    public String getTags() { return tags; }
    @GraphQLQuery(name=GqlConstants.TAG_NAMES, description="Comma-separated list of tag names")
    public String getTagNames() { return tagNames; }
    @GraphQLQuery(name=GqlConstants.DATE, description="Date (secs since epoch) of most recent message in the converstation")
    public Long getDate() { return date; }
    @GraphQLQuery(name=GqlConstants.ELIDED, description="If elided is set, some participants are missing before the first returned")
    public Boolean getElided() { return ZmBoolean.toBool(elided); }
    @GraphQLQuery(name=GqlConstants.CHANGE_DATE, description="Date metadata changed")
    public Long getChangeDate() { return changeDate; }
    @GraphQLQuery(name=GqlConstants.MODIFIED_FIELD_SEQUENCE, description="Modified sequence")
    public Integer getModifiedSequence() { return modifiedSequence; }
    @GraphQLQuery(name=GqlConstants.METADATAS, description="Custom metadata information")
    public List<MailCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }
    @GraphQLQuery(name=GqlConstants.SUBJECT, description="Subject of conversation")
    public String getSubject() { return subject; }
    @GraphQLQuery(name=GqlConstants.FRAGMENT, description="First few bytes of the message (probably between 40 and 100 bytes)")
    public String getFragment() { return fragment; }
    @GraphQLQuery(name=GqlConstants.EMAILS, description="Email information for conversation participants, if available")
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("num", num)
            .add("numUnread", numUnread)
            .add("totalSize", totalSize)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("date", date)
            .add("elided", elided)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("metadatas", metadatas)
            .add("subject", subject)
            .add("fragment", fragment)
            .add("emails", emails);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}

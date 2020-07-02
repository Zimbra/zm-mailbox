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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder={"condition", "tests"})
public final class FilterTests {

    /**
     * @zm-api-field-tag condition-allof|anyof
     * @zm-api-field-description Condition - <b>allof|anyof</b>
     */
    @XmlAttribute(name=MailConstants.A_CONDITION, required=true)
    private String condition;

    /**
     * @zm-api-field-description Tests
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_ADDRESS_BOOK_TEST, type=FilterTest.AddressBookTest.class),
        @XmlElement(name=MailConstants.E_ADDRESS_TEST, type=FilterTest.AddressTest.class),
        @XmlElement(name=MailConstants.E_ENVELOPE_TEST, type=FilterTest.EnvelopeTest.class),
        @XmlElement(name=MailConstants.E_ATTACHMENT_TEST, type=FilterTest.AttachmentTest.class),
        @XmlElement(name=MailConstants.E_BODY_TEST, type=FilterTest.BodyTest.class),
        @XmlElement(name=MailConstants.E_BULK_TEST, type=FilterTest.BulkTest.class),
        @XmlElement(name=MailConstants.E_CONTACT_RANKING_TEST, type=FilterTest.ContactRankingTest.class),
        @XmlElement(name=MailConstants.E_CONVERSATION_TEST, type=FilterTest.ConversationTest.class),
        @XmlElement(name=MailConstants.E_CURRENT_DAY_OF_WEEK_TEST, type=FilterTest.CurrentDayOfWeekTest.class),
        @XmlElement(name=MailConstants.E_CURRENT_TIME_TEST, type=FilterTest.CurrentTimeTest.class),
        @XmlElement(name=MailConstants.E_DATE_TEST, type=FilterTest.DateTest.class),
        @XmlElement(name=MailConstants.E_FACEBOOK_TEST, type=FilterTest.FacebookTest.class),
        @XmlElement(name=MailConstants.E_FLAGGED_TEST, type=FilterTest.FlaggedTest.class),
        @XmlElement(name=MailConstants.E_HEADER_EXISTS_TEST, type=FilterTest.HeaderExistsTest.class),
        @XmlElement(name=MailConstants.E_HEADER_TEST, type=FilterTest.HeaderTest.class),
        @XmlElement(name=MailConstants.E_IMPORTANCE_TEST, type=FilterTest.ImportanceTest.class),
        @XmlElement(name=MailConstants.E_INVITE_TEST, type=FilterTest.InviteTest.class),
        @XmlElement(name=MailConstants.E_LINKEDIN_TEST, type=FilterTest.LinkedInTest.class),
        @XmlElement(name=MailConstants.E_LIST_TEST, type=FilterTest.ListTest.class),
        @XmlElement(name=MailConstants.E_ME_TEST, type=FilterTest.MeTest.class),
        @XmlElement(name=MailConstants.E_MIME_HEADER_TEST, type=FilterTest.MimeHeaderTest.class),
        @XmlElement(name=MailConstants.E_SIZE_TEST, type=FilterTest.SizeTest.class),
        @XmlElement(name=MailConstants.E_SOCIALCAST_TEST, type=FilterTest.SocialcastTest.class),
        @XmlElement(name=MailConstants.E_TRUE_TEST, type=FilterTest.TrueTest.class),
        @XmlElement(name=MailConstants.E_TWITTER_TEST, type=FilterTest.TwitterTest.class),
        @XmlElement(name=MailConstants.E_COMMUNITY_REQUESTS_TEST, type=FilterTest.CommunityRequestsTest.class),
        @XmlElement(name=MailConstants.E_COMMUNITY_CONTENT_TEST, type=FilterTest.CommunityContentTest.class),
        @XmlElement(name=MailConstants.E_COMMUNITY_CONNECTIONS_TEST, type=FilterTest.CommunityConnectionsTest.class),
    })
    private final List<FilterTest> tests = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FilterTests() {
        this(null);
    }

    public FilterTests(String condition) {
        this.condition = condition;
    }

    public static FilterTests createForCondition(String condition) {
        return new FilterTests(condition);
    }

    public void setTests(Collection<FilterTest> list) {
        tests.clear();
        if (list != null) {
            tests.addAll(list);
        }
    }

    public void addTest(FilterTest test) {
        tests.add(test);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<FilterTest> getTests() {
        return Collections.unmodifiableList(tests);
    }

    public int size() {
        return tests.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("condition", condition).add("tests", tests).toString();
    }
}

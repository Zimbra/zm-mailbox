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
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public final class FilterTests {

    @XmlAttribute(name=MailConstants.A_CONDITION, required=true)
    private final String condition;

    @XmlElements({
        @XmlElement(name=MailConstants.E_ADDRESS_BOOK_TEST, type=FilterTest.AddressBookTest.class),
        @XmlElement(name=MailConstants.E_ADDRESS_TEST, type=FilterTest.AddressTest.class),
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
        @XmlElement(name=MailConstants.E_TWITTER_TEST, type=FilterTest.TwitterTest.class)
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

    public List<FilterTest> getTests() {
        return Collections.unmodifiableList(tests);
    }

    public int size() {
        return tests.size();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("condition", condition).add("tests", tests).toString();
    }
}

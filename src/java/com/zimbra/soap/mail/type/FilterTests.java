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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class FilterTests {

    @XmlAttribute(name=MailConstants.A_CONDITION, required=true)
    private final String condition;

    @XmlElements({
        @XmlElement(name=MailConstants.E_HEADER_TEST,
            type=FilterTestHeader.class),
        @XmlElement(name=MailConstants.E_MIME_HEADER_TEST,
            type=FilterTestMimeHeader.class),
        @XmlElement(name=MailConstants.E_HEADER_EXISTS_TEST,
            type=FilterTestHeaderExists.class),
        @XmlElement(name=MailConstants.E_SIZE_TEST,
            type=FilterTestSize.class),
        @XmlElement(name=MailConstants.E_DATE_TEST,
            type=FilterTestDate.class),
        @XmlElement(name=MailConstants.E_BODY_TEST,
            type=FilterTestBody.class),
        @XmlElement(name=MailConstants.E_ATTACHMENT_TEST,
            type=FilterTestAttachment.class),
        @XmlElement(name=MailConstants.E_ADDRESS_BOOK_TEST,
            type=FilterTestAddressBook.class),
        @XmlElement(name=MailConstants.E_INVITE_TEST,
            type=FilterTestInvite.class),
        @XmlElement(name=MailConstants.E_CURRENT_TIME_TEST,
            type=FilterTestCurrentTime.class),
        @XmlElement(name=MailConstants.E_CURRENT_DAY_OF_WEEK_TEST,
            type=FilterTestCurrentDayOfWeek.class),
        @XmlElement(name=MailConstants.E_IMPORTANCE_TEST,
            type=FilterTestImportance.class),
        @XmlElement(name=MailConstants.E_TRUE_TEST,
            type=FilterTestTrue.class)
    })
    private List<FilterTestInfo> tests = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FilterTests() {
        this((String) null);
    }

    public FilterTests(String condition) {
        this.condition = condition;
    }

    public void setTests(Iterable <FilterTestInfo> tests) {
        this.tests.clear();
        if (tests != null) {
            Iterables.addAll(this.tests,tests);
        }
    }

    public FilterTests addTest(FilterTestInfo test) {
        this.tests.add(test);
        return this;
    }

    public String getCondition() { return condition; }
    public List<FilterTestInfo> getTests() {
        return Collections.unmodifiableList(tests);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("condition", condition)
            .add("tests", tests)
            .toString();
    }
}

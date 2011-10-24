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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.BooleanSerializer;
import com.zimbra.soap.json.jackson.ContentListSerializer;
import com.zimbra.soap.util.BooleanAdapter;

@XmlAccessorType(XmlAccessType.NONE)
public class FilterTest {

    @XmlAttribute(name=MailConstants.A_INDEX, required=false)
    private int index = 0;

    @XmlAttribute(name=MailConstants.A_NEGATIVE, required=false)
    @XmlJavaTypeAdapter(BooleanAdapter.class)
    @JsonSerialize(using=BooleanSerializer.class)
    private Boolean negative;

    protected FilterTest() {
    }

    protected FilterTest(int index, Boolean negative) {
        setIndex(index);
        setNegative(negative);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Boolean getNegative() {
        return negative;
    }

    public boolean isNegative() {
        return negative != null ? negative : false;
    }

    public void setNegative(Boolean negative) {
        this.negative = negative;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("index", index).add("negative", negative).toString();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class AddressTest extends FilterTest {

        @XmlAttribute(name = MailConstants.A_HEADER, required = true)
        private String header;

        @XmlAttribute(name = MailConstants.A_PART, required = true)
        private String part;

        @XmlAttribute(name = MailConstants.A_STRING_COMPARISON, required = true)
        private String comparison;

        @XmlAttribute(name = MailConstants.A_CASE_SENSITIVE, required = false)
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @JsonSerialize(using=BooleanSerializer.class)
        private Boolean caseSensitive;

        @XmlAttribute(name = MailConstants.A_VALUE, required = true)
        private String value;


        public String getHeader() {
            return header;
        }

        public void setHeader(String val) {
            header = val;
        }

        public String getPart() {
            return part;
        }

        public void setPart(String val) {
            part = val;
        }

        public String getStringComparison() {
            return comparison;
        }

        public void setStringComparison(String val) {
            comparison = val;
        }

        public Boolean getCaseSensitive() {
            return caseSensitive;
        }

        public boolean isCaseSensitive() {
            return caseSensitive != null ? caseSensitive : false;
        }

        public void setCaseSensitive(Boolean val) {
            caseSensitive = val;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String val) {
            value = val;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("header", header)
                .add("part", part)
                .add("comparison", comparison)
                .add("caseSensitive", caseSensitive)
                .add("value", value)
                .toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class AddressBookTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_HEADER, required=true)
        private final String header;

        @SuppressWarnings("unused")
        private AddressBookTest() {
            this(null);
        }

        public AddressBookTest(String header) {
            this.header = header;
        }

        public String getHeader() {
            return header;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("header", header).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class AttachmentTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class BodyTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_VALUE, required=false)
        private String value;

        @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE, required=false)
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        private Boolean caseSensitive;

        public Boolean getCaseSensitive() {
            return caseSensitive;
        }

        public boolean isCaseSensitive() {
            return caseSensitive != null ? caseSensitive : false;
        }

        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("value", value).add("caseSensitive", caseSensitive).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class BulkTest extends FilterTest {
    }


    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ContactRankingTest extends FilterTest {

        @XmlAttribute(name = MailConstants.A_HEADER, required = true)
        private final String header;

        @SuppressWarnings("unused")
        private ContactRankingTest() {
            this(null);
        }

        public ContactRankingTest(String header) {
            this.header = header;
        }

        public String getHeader() {
            return header;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("header", header).toString();
        }

    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ConversationTest extends FilterTest {

        @XmlAttribute(name = MailConstants.A_WHERE, required = false)
        private String where;

        public String getWhere() {
            return where;
        }

        public void setWhere(String value) {
            where = value;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("where", where).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CurrentDayOfWeekTest extends FilterTest {

        // Comma separated list
        @XmlAttribute(name=MailConstants.A_VALUE, required=false)
        private String values;

        public void setValues(String values) {
            this.values = values;
        }

        public String getValues() {
            return values;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("values", values).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CurrentTimeTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_DATE_COMPARISON, required=false)
        private String dateComparison;

        @XmlAttribute(name=MailConstants.A_TIME, required=false)
        private String time;

        public void setDateComparison(String dateComparison) {
            this.dateComparison = dateComparison;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getDateComparison() {
            return dateComparison;
        }

        public String getTime() {
            return time;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("dateComparison", dateComparison).add("time", time).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class DateTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_DATE_COMPARISON, required=false)
        private String dateComparison;

        @XmlAttribute(name=MailConstants.A_DATE, required=false)
        private Long date;

        public void setDateComparison(String dateComparison) {
            this.dateComparison = dateComparison;
        }

        public void setDate(Long date) {
            this.date = date;
        }

        public String getDateComparison() {
            return dateComparison;
        }

        public Long getDate() {
            return date;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("dateComparison", dateComparison).add("date", date).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class FacebookTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class FlaggedTest extends FilterTest {

        @XmlAttribute(name = MailConstants.A_FLAG_NAME, required = true)
        private final String flag;

        @SuppressWarnings("unused")
        private FlaggedTest() {
            this(null);
        }

        public FlaggedTest(String flag) {
            this.flag = flag;
        }

        public String getFlag() {
            return flag;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("flag", flag).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class HeaderExistsTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_HEADER, required=true)
        private final String header;

        @SuppressWarnings("unused")
        private HeaderExistsTest() {
            this(null);
        }

        public HeaderExistsTest(String header) {
            this.header = header;
        }

        public String getHeader() {
            return header;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("header", header).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    @JsonPropertyOrder({ "index", "negative", "header", "caseSensitive", "stringComparison", "value" })
    public static final class HeaderTest extends FilterTest {

        // Comma separated list
        @XmlAttribute(name=MailConstants.A_HEADER, required=false)
        private String headers;

        @XmlAttribute(name=MailConstants.A_STRING_COMPARISON, required=false)
        private String stringComparison;

        @XmlAttribute(name=MailConstants.A_VALUE, required=false)
        private String value;

        @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE, required=false)
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @JsonSerialize(using=BooleanSerializer.class)
        private Boolean caseSensitive;

        public HeaderTest() {
        }

        public HeaderTest(int index, Boolean negative) {
            super(index, negative);
        }

        public static HeaderTest createForIndexNegative(int index, Boolean negative) {
            return new HeaderTest(index, negative);
        }

        public String getHeaders() {
            return headers;
        }

        public void setHeaders(String headers) {
            this.headers = headers;
        }

        public String getStringComparison() {
            return stringComparison;
        }

        public void setStringComparison(String stringComparison) {
            this.stringComparison = stringComparison;
        }

        public Boolean getCaseSensitive() {
            return caseSensitive;
        }

        public boolean isCaseSensitive() {
            return caseSensitive != null ? caseSensitive : false;
        }

        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("headers", headers)
                .add("stringComparison", stringComparison)
                .add("value", value)
                .add("caseSensitive", caseSensitive)
                .toString();
        }
    }

    @XmlEnum
    public enum Importance {
        high, normal, low;

        public static Importance fromString(String value) throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Importance.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid value: " + value + ", valid values: " + Arrays.asList(Importance.values()), null);
            }
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ImportanceTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_IMP, required=true)
        private Importance importance;

        /**
         * no-argument constructor wanted by JAXB
         */
        @SuppressWarnings("unused")
        private ImportanceTest() {
        }

        public ImportanceTest(Importance importance) {
            this.importance = importance;
        }

        public Importance getImportance() {
            return importance;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("importance", importance).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class InviteTest extends FilterTest {

        @JsonSerialize(using=ContentListSerializer.class)
        @XmlElement(name=MailConstants.E_METHOD, required=false)
        private final List<String> methods = Lists.newArrayList();

        public void setMethods(Collection<String> list) {
            methods.clear();
            if (list != null) {
                methods.addAll(list);
            }
        }

        public void addMethod(String method) {
            methods.add(method);
        }

        public void addMethod(Collection<String> list) {
            methods.addAll(list);
        }

        public List<String> getMethods() {
            return Collections.unmodifiableList(methods);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("methods", methods).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class LinkedInTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ListTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class MeTest extends FilterTest {

        @XmlAttribute(name = MailConstants.A_HEADER, required = true)
        private final String header;

        @SuppressWarnings("unused")
        private MeTest() {
            this(null);
        }

        public MeTest(String header) {
            this.header = header;
        }

        public String getHeader() {
            return header;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("header", header).toString();
        }

    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class MimeHeaderTest extends FilterTest {

        // Comma separated list
        @XmlAttribute(name=MailConstants.A_HEADER, required=false)
        private String headers;

        @XmlAttribute(name=MailConstants.A_STRING_COMPARISON, required=false)
        private String stringComparison;

        @XmlAttribute(name=MailConstants.A_VALUE, required=false)
        private String value;

        @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE, required=false)
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @JsonSerialize(using=BooleanSerializer.class)
        private Boolean caseSensitive;

        public String getHeaders() {
            return headers;
        }

        public void setHeaders(String headers) {
            this.headers = headers;
        }

        public String getStringComparison() {
            return stringComparison;
        }

        public void setStringComparison(String stringComparison) {
            this.stringComparison = stringComparison;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Boolean getCaseSensitive() {
            return caseSensitive;
        }

        public boolean isCaseSensitive() {
            return caseSensitive != null ? caseSensitive : false;
        }

        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("headers", headers)
                .add("stringComparison", stringComparison)
                .add("value", value)
                .add("caseSensitive", caseSensitive)
                .toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class SizeTest extends FilterTest {

        @XmlAttribute(name=MailConstants.A_NUMBER_COMPARISON, required=false)
        private String numberComparison;

        @XmlAttribute(name=MailConstants.A_SIZE, required=false)
        private String size;

        public void setNumberComparison(String numberComparison) {
            this.numberComparison = numberComparison;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getNumberComparison() {
            return numberComparison;
        }

        public String getSize() {
            return size;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("numberComparison", numberComparison).add("size", size).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class SocialcastTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class TrueTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class TwitterTest extends FilterTest {
    }

}

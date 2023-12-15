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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public class FilterTest {

    /**
     * @zm-api-field-tag index
     * @zm-api-field-description Index - specifies a guaranteed order for the test elements
     */
    @XmlAttribute(name=MailConstants.A_INDEX /* index */, required=false)
    private int index = 0;

    /**
     * @zm-api-field-tag not-condition
     * @zm-api-field-description Specifies a "not" condition for the test
     */
    @XmlAttribute(name=MailConstants.A_NEGATIVE /* negative */, required=false)
    private ZmBoolean negative;

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
        return ZmBoolean.toBool(negative);
    }

    public boolean isNegative() {
        return ZmBoolean.toBool(negative, false);
    }

    public void setNegative(Boolean negative) {
        this.negative = ZmBoolean.fromBool(negative);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("index", index).add("negative", negative).toString();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class AddressTest extends FilterTest {

        /**
         * @zm-api-field-tag comma-sep-header-names
         * @zm-api-field-description Comma separated list of header names
         */
        @XmlAttribute(name = MailConstants.A_HEADER /* header */, required = true)
        private String header;

        /**
         * @zm-api-field-tag part-all|localpart|domain
         * @zm-api-field-description Part of address to affect - <b>all|localpart|domain</b>
         */
        @XmlAttribute(name = MailConstants.A_PART /* part */, required = true)
        private String part;

        /**
         * @zm-api-field-tag string-comparison-type
         * @zm-api-field-description String comparison type - <b>is|contains|matches</b>
         */
        @XmlAttribute(name = MailConstants.A_STRING_COMPARISON /* stringComparison */, required = true)
        private String comparison;

        /**
         * @zm-api-field-tag case-sensitive-setting
         * @zm-api-field-description Case sensitive setting
         */
        @XmlAttribute(name = MailConstants.A_CASE_SENSITIVE /* caseSensitive */, required = false)
        private ZmBoolean caseSensitive;

        /**
         * @zm-api-field-tag value
         * @zm-api-field-description Value
         */
        @XmlAttribute(name = MailConstants.A_VALUE /* value */, required = true)
        private String value;

        /**
         * @zm-api-field-tag value-comparison-type
         * @zm-api-field-description Value comparison type - <b>gt|ge|lt|le|eq|ne</b>
         */
        @XmlAttribute(name=MailConstants.A_VALUE_COMPARISON /* valueComparison */, required=false)
        private String valueComparison;

        /**
         * @zm-api-field-tag count-comparison-type
         * @zm-api-field-description count comparison type - <b>gt|ge|lt|le|eq|ne</b>
         */
        @XmlAttribute(name=MailConstants.A_COUNT_COMPARISON /* countComparison */, required=false)
        private String countComparison;

        /**
         * @zm-api-field-tag value-comparison-comparator
         * @zm-api-field-description value comparison comparator - <b>i;ascii-numeric|i;ascii-casemap|i;octet</b>
         */
        @XmlAttribute(name=MailConstants.A_VALUE_COMPARISON_COMPARATOR /* valueComparisonComparator */, required=false)
        private String valueComparisonComparator;

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
            return ZmBoolean.toBool(caseSensitive);
        }

        public boolean isCaseSensitive() {
            return ZmBoolean.toBool(caseSensitive, false);
        }

        public void setCaseSensitive(Boolean val) {
            caseSensitive = ZmBoolean.fromBool(val);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String val) {
            value = val;
        }
        public String getValueComparison() {
            return valueComparison;
        }

        public void setValueComparison(String valueComparison) {
            this.valueComparison = valueComparison;
        }

        public String getCountComparison() {
            return countComparison;
        }

        public void setCountComparison(String countComparison) {
            this.countComparison = countComparison;
        }

        public String getValueComparisonComparator() {
            return valueComparisonComparator;
        }

        public void setValueComparisonComparator(String valueComparisonComparator) {
            this.valueComparisonComparator = valueComparisonComparator;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("header", header)
                .add("part", part)
                .add("comparison", comparison)
                .add("valueComparison", valueComparison)
                .add("valueComparisonComparator", valueComparisonComparator)
                .add("countComparison", countComparison)
                .add("caseSensitive", caseSensitive)
                .add("value", value)
                .toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class EnvelopeTest extends AddressTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class AddressBookTest extends FilterTest {

        /**
         * @zm-api-field-tag header-name
         * @zm-api-field-description Header name
         */
        @XmlAttribute(name=MailConstants.A_HEADER /* header */, required=true)
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
            return MoreObjects.toStringHelper(this).add("header", header).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class AttachmentTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class BodyTest extends FilterTest {

        /**
         * @zm-api-field-tag value
         * @zm-api-field-description Value
         */
        @XmlAttribute(name=MailConstants.A_VALUE /* value */, required=false)
        private String value;

        /**
         * @zm-api-field-tag case-sensitive-setting
         * @zm-api-field-description Case sensitive setting
         */
        @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE /* caseSensitive */, required=false)
        private ZmBoolean caseSensitive;

        public Boolean getCaseSensitive() {
            return ZmBoolean.toBool(caseSensitive);
        }

        public boolean isCaseSensitive() {
            return ZmBoolean.toBool(caseSensitive, false);
        }

        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = ZmBoolean.fromBool(caseSensitive);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("value", value).add("caseSensitive", caseSensitive).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class BulkTest extends FilterTest {
    }


    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ContactRankingTest extends FilterTest {

        /**
         * @zm-api-field-tag header-name
         * @zm-api-field-description Header name
         */
        @XmlAttribute(name = MailConstants.A_HEADER /* header */, required = true)
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
            return MoreObjects.toStringHelper(this).add("header", header).toString();
        }

    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ConversationTest extends FilterTest {

        /**
         * @zm-api-field-tag where-setting
         * @zm-api-field-description Where setting - <b>started|participated</b>
         */
        @XmlAttribute(name = MailConstants.A_WHERE /* where */, required = false)
        private String where;

        public String getWhere() {
            return where;
        }

        public void setWhere(String value) {
            where = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("where", where).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CurrentDayOfWeekTest extends FilterTest {

        /**
         * @zm-api-field-tag comma-separated-day-of-week-indices
         * @zm-api-field-description Comma separated day of week indices
         */
        @XmlAttribute(name=MailConstants.A_VALUE /* value */, required=false)
        private String values;

        public void setValues(String values) {
            this.values = values;
        }

        public String getValues() {
            return values;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("values", values).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CurrentTimeTest extends FilterTest {

        /**
         * @zm-api-field-tag date-comparison-setting
         * @zm-api-field-description Date comparison setting - <b>before|after</b>
         */
        @XmlAttribute(name=MailConstants.A_DATE_COMPARISON /* dateComparison */, required=false)
        private String dateComparison;

        /**
         * @zm-api-field-tag time-in-HHmm-format
         * @zm-api-field-description Time in HHmm format
         */
        @XmlAttribute(name=MailConstants.A_TIME /* time */, required=false)
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
            return MoreObjects.toStringHelper(this).add("dateComparison", dateComparison).add("time", time).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class DateTest extends FilterTest {

        /**
         * @zm-api-field-tag date-comparison
         * @zm-api-field-description Date comparison - <b>before|after</b>
         */
        @XmlAttribute(name=MailConstants.A_DATE_COMPARISON /* dateComparison */, required=false)
        private String dateComparison;

        /**
         * @zm-api-field-tag date
         * @zm-api-field-description Date
         */
        @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
        private String date;

        public void setDateComparison(String dateComparison) {
            this.dateComparison = dateComparison;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getDateComparison() {
            return dateComparison;
        }

        public String getDate() {
            return date;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("dateComparison", dateComparison).add("date", date).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class FacebookTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class FlaggedTest extends FilterTest {

        /**
         * @zm-api-field-tag flagged|read|priority
         * @zm-api-field-description <b>flagged|read|priority</b>
         */
        @XmlAttribute(name = MailConstants.A_FLAG_NAME /* flagName */, required = true)
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
            return MoreObjects.toStringHelper(this).add("flag", flag).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class HeaderExistsTest extends FilterTest {

        /**
         * @zm-api-field-tag header-name
         * @zm-api-field-description Header name
         */
        @XmlAttribute(name=MailConstants.A_HEADER /* header */, required=true)
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
            return MoreObjects.toStringHelper(this).add("header", header).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    @JsonPropertyOrder({ "index", "negative", "header", "caseSensitive", "stringComparison", "valueComparison","valueComparisonComparator", "countComparison", "value" })
    public static final class HeaderTest extends FilterTest {

        // Comma separated list
        /**
         * @zm-api-field-tag comma-sep-header-names
         * @zm-api-field-description Comma separated list of header names
         */
        @XmlAttribute(name=MailConstants.A_HEADER /* header */, required=false)
        private String headers;

        /**
         * @zm-api-field-tag string-comparison-type
         * @zm-api-field-description String comparison type - <b>is|contains|matches</b>
         */
        @XmlAttribute(name=MailConstants.A_STRING_COMPARISON /* stringComparison */, required=false)
        private String stringComparison;

        /**
         * @zm-api-field-tag value-comparison-type
         * @zm-api-field-description Value comparison type - <b>gt|ge|lt|le|eq|ne</b>
         */
        @XmlAttribute(name=MailConstants.A_VALUE_COMPARISON /* valueComparison */, required=false)
        private String valueComparison;

        /**
         * @zm-api-field-tag count-comparison-type
         * @zm-api-field-description count comparison type - <b>gt|ge|lt|le|eq|ne</b>
         */
        @XmlAttribute(name=MailConstants.A_COUNT_COMPARISON /* countComparison */, required=false)
        private String countComparison;

        /**
         * @zm-api-field-tag value-comparison-comparator
         * @zm-api-field-description value comparison comparator - <b>i;ascii-numeric|i;ascii-casemap|i;octet</b>
         */
        @XmlAttribute(name=MailConstants.A_VALUE_COMPARISON_COMPARATOR /* valueComparisonComparator */, required=false)
        private String valueComparisonComparator;

        /**
         * @zm-api-field-tag value
         * @zm-api-field-description Value
         */
        @XmlAttribute(name=MailConstants.A_VALUE /* value */, required=false)
        private String value;

        /**
         * @zm-api-field-tag case-sensitive-setting
         * @zm-api-field-description Case sensitive setting
         */
        @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE /* caseSensitive */, required=false)
        private ZmBoolean caseSensitive;

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

        public String getValueComparison() {
            return valueComparison;
        }

        public void setValueComparison(String valueComparison) {
            this.valueComparison = valueComparison;
        }

        public String getCountComparison() {
            return countComparison;
        }

        public void setCountComparison(String countComparison) {
            this.countComparison = countComparison;
        }

        public Boolean getCaseSensitive() {
            return ZmBoolean.toBool(caseSensitive);
        }

        public boolean isCaseSensitive() {
            return ZmBoolean.toBool(caseSensitive, false);
        }

        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = ZmBoolean.fromBool(caseSensitive);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValueComparisonComparator() {
            return valueComparisonComparator;
        }

        public void setValueComparisonComparator(String valueComparisonComparator) {
            this.valueComparisonComparator = valueComparisonComparator;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("headers", headers)
                .add("stringComparison", stringComparison)
                .add("valueComparison", valueComparison)
                .add("valueComparisonComparator", valueComparisonComparator)
                .add("countComparison", countComparison)
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

        /**
         * @zm-api-field-tag importance-high|normal|low
         * @zm-api-field-description Importance - <b>high|normal|low</b>
         */
        @XmlAttribute(name=MailConstants.A_IMP /* imp */, required=true)
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
            return MoreObjects.toStringHelper(this).add("importance", importance).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class InviteTest extends FilterTest {

        /**
         * @zm-api-field-tag methods
         * @zm-api-field-description Methods
         */
        @XmlElement(name=MailConstants.E_METHOD /* method */, required=false)
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
            return MoreObjects.toStringHelper(this).add("methods", methods).toString();
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

        /**
         * @zm-api-field-tag header-name
         * @zm-api-field-description Header name
         */
        @XmlAttribute(name = MailConstants.A_HEADER /* header */, required = true)
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
            return MoreObjects.toStringHelper(this).add("header", header).toString();
        }

    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class MimeHeaderTest extends FilterTest {

        /**
         * @zm-api-field-tag comma-sep-header-names
         * @zm-api-field-description Comma separated list of header names
         */
        @XmlAttribute(name=MailConstants.A_HEADER /* header */, required=false)
        private String headers;

        /**
         * @zm-api-field-tag string-comparison-type
         * @zm-api-field-description String comparison type - <b>is|contains|matches</b>
         */
        @XmlAttribute(name=MailConstants.A_STRING_COMPARISON /* stringComparison */, required=false)
        private String stringComparison;

        /**
         * @zm-api-field-tag value
         * @zm-api-field-description Value
         */
        @XmlAttribute(name=MailConstants.A_VALUE /* value */, required=false)
        private String value;

        /**
         * @zm-api-field-tag case-sensitive-setting
         * @zm-api-field-description Case sensitive setting
         */
        @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE /* caseSensitive */, required=false)
        private ZmBoolean caseSensitive;

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
            return ZmBoolean.toBool(caseSensitive);
        }

        public boolean isCaseSensitive() {
            return ZmBoolean.toBool(caseSensitive, false);
        }

        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = ZmBoolean.fromBool(caseSensitive);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("headers", headers)
                .add("stringComparison", stringComparison)
                .add("value", value)
                .add("caseSensitive", caseSensitive)
                .toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class SizeTest extends FilterTest {

        /**
         * @zm-api-field-tag number-comparison-setting
         * @zm-api-field-description Number comparison setting - <b>over|under</b>
         */
        @XmlAttribute(name=MailConstants.A_NUMBER_COMPARISON /* numberComparison */, required=false)
        private String numberComparison;

        /**
         * @zm-api-field-tag size
         * @zm-api-field-description size value.  Value can be specified in bytes (no suffix), kilobytes (50K),
         * megabytes (50M) or gigabytes (2G)
         */
        @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
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
            return MoreObjects.toStringHelper(this).add("numberComparison", numberComparison).add("size", size).toString();
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

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CommunityRequestsTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CommunityContentTest extends FilterTest {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class CommunityConnectionsTest extends FilterTest {
    }
}

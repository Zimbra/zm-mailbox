/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.client;

import com.google.common.base.Joiner;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.mail.type.FilterTest;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class ZFilterCondition implements ToZJSONObject {

    public static final String C_ADDRESS = "address";
    public static final String C_ADDRESSBOOK = "addressbook";
    public static final String C_ATTACHMENT = "attachment";
    public static final String C_BODY = "body";
    public static final String C_CASE_SENSITIVE = "case_sensitive";
    public static final String C_CONTACT_RANKING = "contact_ranking";
    public static final String C_CURRENT_DAY = "current_day_of_week";
    public static final String C_CURRENT_TIME = "current_time";
    public static final String C_DATE = "date";
    public static final String C_EXISTS = "exists";
    public static final String C_HEADER = "header";
    public static final String C_INVITE = "invite";
    public static final String C_ME = "me";
    public static final String C_MIME_HEADER = "mime_header";
    public static final String C_NOT_ATTACHMENT = "not attachment";
    public static final String C_NOT_EXISTS = "not exists";
    public static final String C_SIZE = "size";
    public static final String C_TRUE = "true";

    static ZFilterCondition of(FilterTest test) throws ServiceException {
        if (test instanceof FilterTest.AddressBookTest) {
            return new ZAddressBookCondition((FilterTest.AddressBookTest) test);
        } else if (test instanceof FilterTest.AddressTest) {
            return new ZAddressCondition((FilterTest.AddressTest) test);
        } else if (test instanceof FilterTest.AttachmentTest) {
            return new ZAttachmentExistsCondition((FilterTest.AttachmentTest) test);
        } else if (test instanceof FilterTest.BodyTest) {
            return new ZBodyCondition((FilterTest.BodyTest) test);
        } else if (test instanceof FilterTest.BulkTest) {
            return new ZBulkCondition((FilterTest.BulkTest) test);
        } else if (test instanceof FilterTest.ContactRankingTest) {
            return new ZContactRankingCondition((FilterTest.ContactRankingTest) test);
        } else if (test instanceof FilterTest.ConversationTest) {
            return new ZConversationCondition((FilterTest.ConversationTest) test);
        } else if (test instanceof FilterTest.CurrentDayOfWeekTest) {
            return new ZCurrentDayOfWeekCondition((FilterTest.CurrentDayOfWeekTest) test);
        } else if (test instanceof FilterTest.CurrentTimeTest) {
            return new ZCurrentTimeCondition((FilterTest.CurrentTimeTest) test);
        } else if (test instanceof FilterTest.DateTest) {
            return new ZDateCondition((FilterTest.DateTest) test);
        } else if (test instanceof FilterTest.FacebookTest) {
            return new ZFacebookCondition((FilterTest.FacebookTest) test);
        } else if (test instanceof FilterTest.FlaggedTest) {
            return new ZFlaggedCondition((FilterTest.FlaggedTest) test);
        } else if (test instanceof FilterTest.HeaderTest) {
            return new ZHeaderCondition((FilterTest.HeaderTest) test);
        } else if (test instanceof FilterTest.HeaderExistsTest) {
            return new ZHeaderExistsCondition((FilterTest.HeaderExistsTest) test);
        } else if (test instanceof FilterTest.ImportanceTest) {
            return new ZImportanceCondition((FilterTest.ImportanceTest) test);
        } else if (test instanceof FilterTest.InviteTest) {
            return new ZInviteCondition((FilterTest.InviteTest) test);
        } else if (test instanceof FilterTest.LinkedInTest) {
            return new ZLinkedInCondition((FilterTest.LinkedInTest) test);
        } else if (test instanceof FilterTest.ListTest) {
            return new ZListCondition((FilterTest.ListTest) test);
        } else if (test instanceof FilterTest.MeTest) {
            return new ZMeCondition((FilterTest.MeTest) test);
        } else if (test instanceof FilterTest.MimeHeaderTest) {
            return new ZMimeHeaderCondition((FilterTest.MimeHeaderTest) test);
        } else if (test instanceof FilterTest.SizeTest) {
            return new ZSizeCondition((FilterTest.SizeTest) test);
        } else if (test instanceof FilterTest.SocialcastTest) {
            return new ZSocialcastCondition((FilterTest.SocialcastTest) test);
        } else if (test instanceof FilterTest.TrueTest) {
            return new ZTrueCondition();
        } else if (test instanceof FilterTest.TwitterTest) {
            return new ZTwitterCondition((FilterTest.TwitterTest) test);
        }
        throw new IllegalArgumentException(test.getClass().getName());
    }

    public enum HeaderOp {
        IS, NOT_IS, CONTAINS, NOT_CONTAINS, MATCHES, NOT_MATCHES;

        public static HeaderOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }

        public static HeaderOp of(Sieve.StringComparison comparison, boolean isNegative) {
            if (comparison == Sieve.StringComparison.is) {
                return (isNegative ? NOT_IS : IS);
            }
            if (comparison == Sieve.StringComparison.contains) {
                return (isNegative ? NOT_CONTAINS : CONTAINS);
            }
            return (isNegative ? NOT_MATCHES : MATCHES);
        }

        public Sieve.StringComparison toStringComparison() {
            if (this == IS || this == NOT_IS) {
                return Sieve.StringComparison.is;
            }
            if (this == CONTAINS || this == NOT_CONTAINS) {
                return Sieve.StringComparison.contains;
            }
            return Sieve.StringComparison.matches;
        }

        public boolean isNegative() {
            return (this == NOT_IS || this == NOT_CONTAINS || this == NOT_MATCHES);
        }
    }

    public enum DateOp {
        BEFORE, NOT_BEFORE, AFTER, NOT_AFTER;

        public static DateOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }

        public static DateOp of(Sieve.DateComparison comparison, boolean isNegative) {
            if (comparison == Sieve.DateComparison.before) {
                return (isNegative ? NOT_BEFORE : BEFORE);
            }
            return (isNegative ? NOT_AFTER : AFTER);
        }

        public Sieve.DateComparison toDateComparison() {
            if (this == BEFORE || this == NOT_BEFORE) {
                return Sieve.DateComparison.before;
            }
            return Sieve.DateComparison.after;
        }

        public boolean isNegative() {
            return (this == NOT_BEFORE || this == NOT_AFTER);
        }
    }

    public enum SizeOp {
        UNDER, NOT_UNDER, OVER, NOT_OVER;

        public static SizeOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }

        public static SizeOp of(Sieve.NumberComparison comparison, boolean isNegative) {
            if (comparison == Sieve.NumberComparison.over) {
                return (isNegative ? NOT_OVER : OVER);
            }
            return (isNegative ? NOT_UNDER : UNDER);
        }

        public Sieve.NumberComparison toNumberComparison() {
            if (this == UNDER || this == NOT_UNDER) {
                return Sieve.NumberComparison.under;
            } else {
                return Sieve.NumberComparison.over;
            }
        }

        public boolean isNegative() {
            return (this == NOT_UNDER || this == NOT_OVER);
        }
    }

    public enum BodyOp {
        CONTAINS, NOT_CONTAINS;

        public static BodyOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }
    }

    public enum SimpleOp {
        IS, NOT_IS;

        public static SimpleOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }
    }

    public enum AddressBookOp {
        IN, NOT_IN;

        public static AddressBookOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }
    }

    public enum ContactRankingOp {
        IN, NOT_IN;

        public static ContactRankingOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }
    }

    public enum MeOp {
        IN, NOT_IN;

        public static MeOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }
    }

    public enum ConversationOp {
        WHERE, NOT_WHERE;

        public static ConversationOp fromString(String src) throws ServiceException {
            try {
                return valueOf(src.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + src +
                        ", possible values: " + Arrays.asList(values()), null);
            }
        }
    }

    abstract FilterTest toJAXB();
    public abstract String getName();

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        // TODO: Implement in subclasses?
        ZJSONObject jo = new ZJSONObject();
        jo.put("name", getName());
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZFilterCondition %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public abstract String toConditionString();

    public static final class ZCommunityRequestsCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZCommunityRequestsCondition(SimpleOp op) {
            this.op = op;
        }

        ZCommunityRequestsCondition(FilterTest.CommunityRequestsTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "community_requests";
        }

        @Override
        FilterTest.CommunityRequestsTest toJAXB() {
            FilterTest.CommunityRequestsTest test = new FilterTest.CommunityRequestsTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "community_requests" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZCommunityContentCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZCommunityContentCondition(SimpleOp op) {
            this.op = op;
        }

        ZCommunityContentCondition(FilterTest.CommunityContentTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "community_content";
        }

        @Override
        FilterTest.CommunityContentTest toJAXB() {
            FilterTest.CommunityContentTest test = new FilterTest.CommunityContentTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "community_content" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZCommunityConnectionsCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZCommunityConnectionsCondition(SimpleOp op) {
            this.op = op;
        }

        ZCommunityConnectionsCondition(FilterTest.CommunityConnectionsTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "community_connections";
        }

        @Override
        FilterTest.CommunityConnectionsTest toJAXB() {
            FilterTest.CommunityConnectionsTest test = new FilterTest.CommunityConnectionsTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "community_connections" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZFacebookCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZFacebookCondition(SimpleOp op) {
            this.op = op;
        }

        ZFacebookCondition(FilterTest.FacebookTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "facebook";
        }

        @Override
        FilterTest.FacebookTest toJAXB() {
            FilterTest.FacebookTest test = new FilterTest.FacebookTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "facebook" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZLinkedInCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZLinkedInCondition(SimpleOp op) {
            this.op = op;
        }

        ZLinkedInCondition(FilterTest.LinkedInTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "linkedin";
        }

        @Override
        FilterTest.LinkedInTest toJAXB() {
            FilterTest.LinkedInTest test = new FilterTest.LinkedInTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "linkedin" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZSocialcastCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZSocialcastCondition(SimpleOp op) {
            this.op = op;
        }

        ZSocialcastCondition(FilterTest.SocialcastTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "socialcast";
        }

        @Override
        FilterTest.SocialcastTest toJAXB() {
            FilterTest.SocialcastTest test = new FilterTest.SocialcastTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "socialcast" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZTwitterCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZTwitterCondition(SimpleOp op) {
            this.op = op;
        }

        ZTwitterCondition(FilterTest.TwitterTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "twitter";
        }

        @Override
        FilterTest.TwitterTest toJAXB() {
            FilterTest.TwitterTest test = new FilterTest.TwitterTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "twitter" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZBulkCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZBulkCondition(SimpleOp op) {
            this.op = op;
        }

        ZBulkCondition(FilterTest.BulkTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "bulk";
        }

        @Override
        FilterTest.BulkTest toJAXB() {
            FilterTest.BulkTest test = new FilterTest.BulkTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "bulk" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZFlaggedCondition extends ZFilterCondition {
        private final SimpleOp op;
        private final Sieve.Flag flag;

        public ZFlaggedCondition(SimpleOp op, Sieve.Flag flag) {
            this.op = op;
            this.flag = flag;
        }

        ZFlaggedCondition(FilterTest.FlaggedTest test) throws ServiceException {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
            this.flag = Sieve.Flag.fromString(test.getFlag());
        }

        @Override
        public String getName() {
            return "flagged";
        }

        @Override
        FilterTest.FlaggedTest toJAXB() {
            FilterTest.FlaggedTest test = new FilterTest.FlaggedTest(flag.toString());
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "flagged " + (op == SimpleOp.IS ? "" : "not ") + flag;
        }
    }

    public static final class ZImportanceCondition extends ZFilterCondition {
        private final SimpleOp op;
        private final FilterTest.Importance importance;

        public ZImportanceCondition(SimpleOp op, FilterTest.Importance importance) {
            this.op = op;
            this.importance = importance;
        }

        ZImportanceCondition(FilterTest.ImportanceTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
            this.importance = test.getImportance();
        }

        @Override
        public String getName() {
            return "importance";
        }

        @Override
        FilterTest.ImportanceTest toJAXB() {
            FilterTest.ImportanceTest test = new FilterTest.ImportanceTest(importance);
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "importance " +  (op == SimpleOp.IS ? "is " : "not_is ") + importance;
        }
    }

    public static final class ZListCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZListCondition(SimpleOp op) {
            this.op = op;
        }

        ZListCondition(FilterTest.ListTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
        }

        @Override
        public String getName() {
            return "list";
        }

        @Override
        FilterTest.ListTest toJAXB() {
            FilterTest.ListTest test = new FilterTest.ListTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            return test;
        }

        @Override
        public String toConditionString() {
            return "list" + (op == SimpleOp.IS ? "" : " not");
        }
    }

    public static final class ZConversationCondition extends ZFilterCondition {
        private final ConversationOp op;
        private final String where;

        public ZConversationCondition(ConversationOp op, String where) {
            this.op = op;
            this.where = where;
        }

        ZConversationCondition(FilterTest.ConversationTest test) {
            this.op = test.isNegative() ? ConversationOp.NOT_WHERE : ConversationOp.WHERE;
            this.where = test.getWhere();
        }

        @Override
        public String getName() {
            return "conversation";
        }

        @Override
        FilterTest.ConversationTest toJAXB() {
            FilterTest.ConversationTest test = new FilterTest.ConversationTest();
            test.setWhere(where);
            test.setNegative(op == ConversationOp.NOT_WHERE);
            return test;
        }

        @Override
        public String toConditionString() {
            return "conversation " + (op == ConversationOp.WHERE ? "where " : "not_where ") +
                ZFilterRule.quotedString(where);
        }
    }

    public static final class ZAddressBookCondition extends ZFilterCondition {
        private final AddressBookOp op;
        private final String header;

        public ZAddressBookCondition(AddressBookOp op, String header) {
            this.op = op;
            this.header = header;
        }

        ZAddressBookCondition(FilterTest.AddressBookTest test) {
            this.op = test.isNegative() ? AddressBookOp.NOT_IN : AddressBookOp.IN;
            this.header = test.getHeader();
        }

        @Override
        public String getName() {
            return C_ADDRESSBOOK;
        }

        public AddressBookOp getAddressBookOp() {
            return op;
        }

        public String getHeader() {
            return header;
        }

        @Override
        FilterTest.AddressBookTest toJAXB() {
            FilterTest.AddressBookTest test = new FilterTest.AddressBookTest(header);
            test.setNegative(op == AddressBookOp.NOT_IN);
            return test;
        }

        @Override
        public String toConditionString() {
            return (op == AddressBookOp.IN ? "addressbook in " : "addressbook not_in ") +
                ZFilterRule.quotedString(header);
        }
    }

    public static final class ZContactRankingCondition extends ZFilterCondition {
        private final ContactRankingOp op;
        private final String header;

        public ZContactRankingCondition(ContactRankingOp op, String header) {
            this.op = op;
            this.header = header;
        }

        ZContactRankingCondition(FilterTest.ContactRankingTest test) {
            this.op = test.isNegative() ? ContactRankingOp.NOT_IN : ContactRankingOp.IN;
            this.header = test.getHeader();
        }

        @Override
        public String getName() {
            return C_CONTACT_RANKING;
        }

        @Override
        FilterTest.ContactRankingTest toJAXB() {
            FilterTest.ContactRankingTest test = new FilterTest.ContactRankingTest(header);
            test.setNegative(op == ContactRankingOp.NOT_IN);
            return test;
        }

        @Override
        public String toConditionString() {
            return (op == ContactRankingOp.IN ? "contact_ranking in " : "contact_ranking not_in ") +
                ZFilterRule.quotedString(header);
        }
    }

    public static final class ZMeCondition extends ZFilterCondition {
        private final MeOp op;
        private final String header;

        public ZMeCondition(MeOp op, String header) {
            this.op = op;
            this.header = header;
        }

        ZMeCondition(FilterTest.MeTest test) {
            this.op = test.isNegative() ? MeOp.NOT_IN : MeOp.IN;
            this.header = test.getHeader();
        }

        @Override
        public String getName() {
            return C_ME;
        }

        @Override
        FilterTest.MeTest toJAXB() {
            FilterTest.MeTest test = new FilterTest.MeTest(header);
            test.setNegative(op == MeOp.NOT_IN);
            return test;
        }

        @Override
        public String toConditionString() {
            return (op == MeOp.IN ? "me in " : "me not_in ") + ZFilterRule.quotedString(header);
        }
    }

    public static final class ZBodyCondition extends ZFilterCondition {
        private final BodyOp op;
        private final boolean caseSensitive;
        private final String text;

        public ZBodyCondition(BodyOp op, String text) {
            this(op, false, text);
        }

        public ZBodyCondition(BodyOp op, boolean caseSensitive, String text) {
            this.op = op;
            this.caseSensitive = caseSensitive;
            this.text = text;
        }

        ZBodyCondition(FilterTest.BodyTest test) {
            this.op = test.isNegative() ? BodyOp.NOT_CONTAINS : BodyOp.CONTAINS;
            this.caseSensitive = test.isCaseSensitive();
            this.text = test.getValue();
        }

        @Override
        public String getName() {
            return C_BODY;
        }

        public BodyOp getBodyOp() {
            return op;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toConditionString() {
            return (op == BodyOp.CONTAINS ? "body contains " : "body not_contains ") +
                    (caseSensitive ? "case_sensitive " : "") + ZFilterRule.quotedString(getText());
        }

        @Override
        FilterTest.BodyTest toJAXB() {
            FilterTest.BodyTest test = new FilterTest.BodyTest();
            test.setCaseSensitive(caseSensitive);
            test.setValue(text);
            test.setNegative(op == BodyOp.NOT_CONTAINS);
            return test;
        }

    }

    public static final class ZCurrentDayOfWeekCondition extends ZFilterCondition {
        private final SimpleOp op;
        private final String days;

        public ZCurrentDayOfWeekCondition(SimpleOp op, String days) {
            this.op = op;
            this.days = days;
        }

        ZCurrentDayOfWeekCondition(FilterTest.CurrentDayOfWeekTest test) {
            this.op = test.isNegative() ? SimpleOp.NOT_IS : SimpleOp.IS;
            this.days = test.getValues();
        }

        @Override
        public String getName() {
            return C_CURRENT_DAY;
        }

        public SimpleOp getOp() {
            return op;
        }

        public String getDays() {
            return days;
        }

        @Override
        public String toConditionString() {
            return "current_day_of_week " + (op == SimpleOp.IS ? "is " : "not_is ") + days;
        }

        @Override
        FilterTest.CurrentDayOfWeekTest toJAXB() {
            FilterTest.CurrentDayOfWeekTest test = new FilterTest.CurrentDayOfWeekTest();
            test.setNegative(op == SimpleOp.NOT_IS);
            test.setValues(days);
            return test;
        }
    }

    public static final class ZSizeCondition extends ZFilterCondition {
        private final SizeOp op;
        private final String size;

        public ZSizeCondition(SizeOp op, String size) {
            this.op = op;
            this.size = size;
        }

        ZSizeCondition(FilterTest.SizeTest test) throws ServiceException {
            this.op = SizeOp.of(Sieve.NumberComparison.fromString(test.getNumberComparison()), test.isNegative());
            this.size = test.getSize();
        }

        public SizeOp getSizeOp() {
            return op;
        }

        public String getSize() {
            return size;
        }

        public String getUnits() {
            String val = getSize();
            if (val != null) {
                if (val.endsWith("M")) {
                    return "M";
                } else if (val.endsWith("K")) {
                    return "K";
                } else if (val.endsWith("G")) {
                    return "G";
                }
            }
            return "B";
        }

        public String getSizeNoUnits() {
            String val = getSize();
            if (val != null) {
                if (val.endsWith("M")) {
                    return val.substring(0, val.length()-1);
                } else if (val.endsWith("K")) {
                    return val.substring(0, val.length()-1);
                } else if (val.endsWith("G")) {
                    return val.substring(0, val.length()-1);
                }
            }
            return val;
        }

        @Override
        public String toConditionString() {
            return "size " + op.name().toLowerCase() + " " + ZFilterRule.quotedString(getSize());
        }

        @Override
        public String getName() {
            return C_SIZE;
        }

        @Override
        FilterTest.SizeTest toJAXB() {
            FilterTest.SizeTest test = new FilterTest.SizeTest();
            test.setNumberComparison(op.toNumberComparison().toString());
            test.setNegative(op.isNegative());
            test.setSize(size);
            return test;
        }
    }

    public static final class ZDateCondition extends ZFilterCondition {
        private final DateOp op;
        private final Date date;

        public ZDateCondition(DateOp op, Date date) {
            this.op = op;
            this.date = date;
        }

        public ZDateCondition(DateOp op, String dateStr) throws ServiceException {
            this.op = op;
            try {
                this.date = new SimpleDateFormat("yyyyMMdd").parse(dateStr);
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("invalid date: " + dateStr + ", should be: YYYYMMDD", e);
            }
        }

        ZDateCondition(FilterTest.DateTest test) throws ServiceException {
            this.op = DateOp.of(Sieve.DateComparison.fromString(test.getDateComparison()), test.isNegative());
            //this.date = new Date(test.getDate() * 1000);
            try {
                this.date = new SimpleDateFormat("yyyyMMdd").parse(test.getDate());
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("invalid date: " + test.getDate() + ", should be: YYYYMMDD", e);
            }
        }

        public DateOp getDateOp() {
            return op;
        }

        public Date getDate() {
            return date;
        }

        public String getDateString() {
            return Sieve.DATE_PARSER.format(date);
        }

        @Override
        public String toConditionString() {
            return "date " + op.name().toLowerCase() + " " + ZFilterRule.quotedString(getDateString());
        }

        @Override
        public String getName() {
            return C_DATE;
        }

        @Override
        FilterTest.DateTest toJAXB() {
            FilterTest.DateTest test = new FilterTest.DateTest();
            test.setDateComparison(op.toDateComparison().toString());
            test.setNegative(op.isNegative());
            //test.setDate(date.getTime() / 1000);
            test.setDate(Sieve.DATE_PARSER.format(date));
            return test;
        }
    }

    public static final class ZCurrentTimeCondition extends ZFilterCondition {
        private final DateOp op;
        private final String time;

        public ZCurrentTimeCondition(DateOp op, String time) {
            this.op = op;
            this.time = time;
        }

        ZCurrentTimeCondition(FilterTest.CurrentTimeTest test) throws ServiceException {
            this.op = DateOp.of(Sieve.DateComparison.fromString(test.getDateComparison()), test.isNegative());
            this.time = test.getTime();
        }

        public DateOp getDateOp() {
            return op;
        }

        public String getTimeStr() {
            return time;
        }

        @Override
        public String toConditionString() {
            return "current_time " + op.name().toLowerCase() + " " + time;
        }

        @Override
        public String getName() {
            return C_CURRENT_TIME;
        }

        @Override
        FilterTest.CurrentTimeTest toJAXB() {
            FilterTest.CurrentTimeTest test = new FilterTest.CurrentTimeTest();
            test.setDateComparison(op.toDateComparison().toString());
            test.setNegative(op.isNegative());
            test.setTime(time);
            return test;
        }
    }

    public static final class ZTrueCondition extends ZFilterCondition {
        @Override
        FilterTest.TrueTest toJAXB() {
            return new FilterTest.TrueTest();
        }

        @Override
        public String getName() {
            return C_TRUE;
        }

        @Override
        public String toConditionString() {
            return "true";
        }
    }

    public static final class ZHeaderCondition extends ZFilterCondition {
        private final HeaderOp op;
        private final boolean caseSensitive;
        private final String headerName;
        private final String value;

        public ZHeaderCondition(String headerName, HeaderOp op, String value) {
            this(headerName, op, false, value);
        }

        public ZHeaderCondition(String headerName, HeaderOp op, boolean caseSensitive, String value) {
            this.headerName = headerName;
            this.op = op;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        ZHeaderCondition(FilterTest.HeaderTest test) throws ServiceException {
            this.headerName = test.getHeaders();
            this.op = HeaderOp.of(Sieve.StringComparison.fromString(test.getStringComparison()), test.isNegative());
            this.caseSensitive = test.isCaseSensitive();
            this.value = test.getValue();
        }

        public HeaderOp getHeaderOp() {
            return op;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public String getHeaderName() {
            return headerName;
        }

        public String getHeaderValue() {
            return value;
        }

        @Override
        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + " " + op.name().toLowerCase() +
                    " " + (caseSensitive ? "case_sensitive " : "") + ZFilterRule.quotedString(getHeaderValue());
        }

        @Override
        public String getName() {
            return C_HEADER;
        }

        @Override
        FilterTest.HeaderTest toJAXB() {
            FilterTest.HeaderTest test = new FilterTest.HeaderTest();
            test.setHeaders(headerName);
            test.setStringComparison(op.toStringComparison().toString());
            test.setCaseSensitive(caseSensitive);
            test.setNegative(op.isNegative());
            test.setValue(value);
            return test;
        }
    }

    public static final class ZMimeHeaderCondition extends ZFilterCondition {
        private final HeaderOp op;
        private final boolean caseSensitive;
        private final String headerName;
        private final String value;

        public ZMimeHeaderCondition(String headerName, HeaderOp op, String value) {
            this(headerName, op, false, value);
        }

        public ZMimeHeaderCondition(String headerName, HeaderOp op, boolean caseSensitive, String value) {
            this.headerName = headerName;
            this.op = op;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        ZMimeHeaderCondition(FilterTest.MimeHeaderTest test) throws ServiceException {
            this.headerName = test.getHeaders();
            this.op = HeaderOp.of(Sieve.StringComparison.fromString(test.getStringComparison()), test.isNegative());
            this.caseSensitive = test.isCaseSensitive();
            this.value = test.getValue();
        }

        public HeaderOp getHeaderOp() {
            return op;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public String getHeaderName() {
            return headerName;
        }

        public String getHeaderValue() {
            return value;
        }

        @Override
        public String toConditionString() {
            return "mime_header " + ZFilterRule.quotedString(getHeaderName()) + " " + op.name().toLowerCase() +
                    " " + (caseSensitive ? "case_sensitive " : "") + ZFilterRule.quotedString(getHeaderValue());
        }

        @Override
        public String getName() {
            return C_MIME_HEADER;
        }

        @Override
        FilterTest.MimeHeaderTest toJAXB() {
            FilterTest.MimeHeaderTest test = new FilterTest.MimeHeaderTest();
            test.setHeaders(headerName);
            test.setStringComparison(op.toStringComparison().toString());
            test.setCaseSensitive(caseSensitive);
            test.setNegative(op.isNegative());
            test.setValue(value);
            return test;
        }
    }

    public static final class ZAddressCondition extends ZFilterCondition {
        private final String headerName;
        private final Sieve.AddressPart part;
        private final HeaderOp op;
        private final boolean caseSensitive;
        private final String value;

        public ZAddressCondition(String headerName, Sieve.AddressPart part, HeaderOp op, boolean caseSensitive,
                String value) {
            this.headerName = headerName;
            this.part = part;
            this.op = op;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        public ZAddressCondition(String headerName, Sieve.AddressPart part, HeaderOp op, String value) {
            this.headerName = headerName;
            this.part = part;
            this.op = op;
            this.caseSensitive = false;
            this.value = value;
        }

        ZAddressCondition(FilterTest.AddressTest test) throws ServiceException{
            this.headerName = test.getHeader();
            this.part = Sieve.AddressPart.fromString(test.getPart());
            this.op = HeaderOp.of(Sieve.StringComparison.fromString(test.getStringComparison()), test.isNegative());
            this.caseSensitive = test.isCaseSensitive();
            this.value = test.getValue();
        }

        @Override
        public String toConditionString() {
            return "address " + ZFilterRule.quotedString(headerName) + " " + part + " " +
                    op.name().toLowerCase() + " " + (caseSensitive ? "case_sensitive " : "") +
                    ZFilterRule.quotedString(value);
        }

        @Override
        public String getName() {
            return C_ADDRESS;
        }

        public HeaderOp getHeaderOp() {
            return op;
        }

        public String getHeaderName() {
            return headerName;
        }

        public String getHeaderValue() {
            return value;
        }

        @Override
        FilterTest.AddressTest toJAXB() {
            FilterTest.AddressTest test = new FilterTest.AddressTest();
            test.setHeader(headerName);
            test.setPart(part.toString());
            test.setStringComparison(op.toStringComparison().toString());
            test.setCaseSensitive(caseSensitive);
            test.setNegative(op.isNegative());
            test.setValue(value);
            return test;
        }
    }

    public static final class ZHeaderExistsCondition extends ZFilterCondition {
        private final boolean exists;
        private final String headerName;

        public ZHeaderExistsCondition(String headerName, boolean exists) {
            this.exists = exists;
            this.headerName = headerName;
        }

        ZHeaderExistsCondition(FilterTest.HeaderExistsTest test) {
            this.exists = !test.isNegative();
            this.headerName = test.getHeader();
        }

        public boolean getExists() {
            return exists;
        }

        public String getHeaderName() {
            return headerName;
        }

        @Override
        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + (exists ? " exists" : " not_exists");
        }

        @Override
        public String getName() {
            return C_EXISTS;
        }

        @Override
        FilterTest.HeaderExistsTest toJAXB() {
            FilterTest.HeaderExistsTest test = new FilterTest.HeaderExistsTest(headerName);
            test.setNegative(!exists);
            return test;
        }
    }

    public static final class ZAttachmentExistsCondition extends ZFilterCondition {
        private final boolean exists;

        public ZAttachmentExistsCondition(boolean exists) {
            this.exists = exists;
        }

        ZAttachmentExistsCondition(FilterTest.AttachmentTest test) {
            this.exists = !test.isNegative();
        }

        public boolean getExists() {
            return exists;
        }

        @Override
        public String toConditionString() {
            return exists ? "attachment exists" : "attachment not_exists";
        }

        @Override
        public String getName() {
            return C_ATTACHMENT;
        }

        @Override
        FilterTest.AttachmentTest toJAXB() {
            FilterTest.AttachmentTest test = new FilterTest.AttachmentTest();
            test.setNegative(!exists);
            return test;
        }
    }

    public static final class ZInviteCondition extends ZFilterCondition {
        public static final String METHOD_ANYREQUEST = "anyrequest";
        public static final String METHOD_ANYREPLY = "anyreply";
        public static final String METHOD_PUBLISH = "publish";
        public static final String METHOD_REQUEST = "request";
        public static final String METHOD_REPLY = "reply";
        public static final String METHOD_ADD = "add";
        public static final String METHOD_CANCEL = "cancel";
        public static final String METHOD_REFRESH = "refresh";
        public static final String METHOD_DECLINECOUNTER = "declinecounter";

        private final boolean invite;
        private final List<String> methods = new ArrayList<String>();

        public ZInviteCondition(boolean isInvite) {
            this.invite = isInvite;
        }

        public ZInviteCondition(boolean invite, String method) {
            this(invite, Collections.singletonList(method));
        }

        public ZInviteCondition(boolean invite, List<String> methods) {
            this.invite = invite;
            if (methods != null) {
                this.methods.addAll(methods);
            }
        }

        ZInviteCondition(FilterTest.InviteTest test) {
            this.invite = !test.isNegative();
            this.methods.addAll(test.getMethods());
        }

        public void setMethods(String ... args) {
            methods.clear();
            Collections.addAll(methods, args);
        }

        public boolean isInvite() {
            return invite;
        }

        @Override
        public String toConditionString() {
            StringBuilder buf = new StringBuilder("invite ");
            if (!invite) {
                buf.append("not_");
            }
            buf.append("exists");
            if (!methods.isEmpty()) {
                buf.append(" method ").append(Joiner.on(',').join(methods));
            }
            return buf.toString();
        }

        @Override
        public String getName() {
            return C_INVITE;
        }

        @Override
        FilterTest.InviteTest toJAXB() {
            FilterTest.InviteTest test = new FilterTest.InviteTest();
            test.setNegative(!invite);
            test.addMethod(methods);
            return test;
        }
    }
}

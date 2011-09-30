/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.client;

import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.mail.type.FilterTestImportance;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class ZFilterCondition implements ToZJSONObject {

    public static final String C_ADDRESSBOOK = "addressbook";
    public static final String C_CONTACT_RANKING = "contact_ranking";
    public static final String C_ME = "me";
    public static final String C_ATTACHMENT = "attachment";
    public static final String C_BODY = "body";
    public static final String C_DATE = "date";
    public static final String C_CURRENT_TIME = "current_time";
    public static final String C_CURRENT_DAY = "current_day_of_week";
    public static final String C_EXISTS = "exists";
    public static final String C_NOT_EXISTS = "not exists";
    public static final String C_HEADER = "header";
    public static final String C_TRUE = "true";
    public static final String C_MIME_HEADER = "mime_header";
    public static final String C_ADDRESS = "address";
    public static final String C_NOT_ATTACHMENT = "not attachment";
    public static final String C_SIZE = "size";
    public static final String C_INVITE = "invite";
    public static final String C_CASE_SENSITIVE = "case_sensitive";

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

    public static ZFilterCondition getCondition(Element condEl) throws ServiceException {
        String name = condEl.getName();
        boolean isNegative = condEl.getAttributeBool(MailConstants.A_NEGATIVE, false);

        if (name.equals(MailConstants.E_HEADER_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            Sieve.StringComparison comparison =
                    Sieve.StringComparison.fromString(condEl.getAttribute(MailConstants.A_STRING_COMPARISON).toLowerCase());
            boolean caseSensitive = condEl.getAttributeBool(MailConstants.A_CASE_SENSITIVE, false);
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            return new ZHeaderCondition(header, HeaderOp.of(comparison, isNegative), caseSensitive, value);
        } else if (name.equals(MailConstants.E_MIME_HEADER_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            Sieve.StringComparison comparison =
                    Sieve.StringComparison.fromString(condEl.getAttribute(MailConstants.A_STRING_COMPARISON).toLowerCase());
            boolean caseSensitive = condEl.getAttributeBool(MailConstants.A_CASE_SENSITIVE, false);
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            return new ZMimeHeaderCondition(header, HeaderOp.of(comparison, isNegative), caseSensitive, value);
        } else if (name.equals(MailConstants.E_ADDRESS_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            Sieve.AddressPart part =
                    Sieve.AddressPart.fromString(condEl.getAttribute(MailConstants.A_PART, Sieve.AddressPart.all.toString()));
            Sieve.StringComparison comparison =
                    Sieve.StringComparison.fromString(condEl.getAttribute(MailConstants.A_STRING_COMPARISON).toLowerCase());
            boolean caseSensitive = condEl.getAttributeBool(MailConstants.A_CASE_SENSITIVE, false);
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            return new ZAddressCondition(header, part, HeaderOp.of(comparison, isNegative), caseSensitive, value);
        } else if (name.equals(MailConstants.E_HEADER_EXISTS_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            return new ZHeaderExistsCondition(header, !isNegative);
        } else if (name.equals(MailConstants.E_SIZE_TEST)) {
            Sieve.NumberComparison comparison =
                    Sieve.NumberComparison.fromString(condEl.getAttribute(MailConstants.A_NUMBER_COMPARISON).toLowerCase());
            String size = condEl.getAttribute(MailConstants.A_SIZE);
            return new ZSizeCondition(SizeOp.of(comparison, isNegative), size);
        } else if (name.equals(MailConstants.E_DATE_TEST)) {
            String s = condEl.getAttribute(MailConstants.A_DATE_COMPARISON);
            s = s.toLowerCase();
            Sieve.DateComparison comparison = Sieve.DateComparison.fromString(s);
            Date date = new Date(condEl.getAttributeLong(MailConstants.A_DATE) * 1000);
            return new ZDateCondition(DateOp.of(comparison, isNegative), date);
        } else if (name.equals(MailConstants.E_CURRENT_TIME_TEST)) {
            Sieve.DateComparison comparison =
                    Sieve.DateComparison.fromString(condEl.getAttribute(MailConstants.A_DATE_COMPARISON).toLowerCase());
            String timeStr = condEl.getAttribute(MailConstants.A_TIME);
            return new ZCurrentTimeCondition(DateOp.of(comparison, isNegative), timeStr);
        } else if (name.equals(MailConstants.E_BODY_TEST)) {
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            BodyOp op = (isNegative ? BodyOp.NOT_CONTAINS : BodyOp.CONTAINS);
            boolean caseSensitive = condEl.getAttributeBool(MailConstants.A_CASE_SENSITIVE, false);
            return new ZBodyCondition(op, caseSensitive, value);
        } else if (name.equals(MailConstants.E_CURRENT_DAY_OF_WEEK_TEST)) {
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            SimpleOp op = (isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
            return new ZCurrentDayOfWeekCondition(op, value);
        } else if (name.equals(MailConstants.E_ADDRESS_BOOK_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            return new ZAddressBookCondition(isNegative ? AddressBookOp.NOT_IN : AddressBookOp.IN, header);
        } else if (name.equals(MailConstants.E_CONTACT_RANKING_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            return new ZContactRankingCondition(isNegative ? ContactRankingOp.NOT_IN : ContactRankingOp.IN, header);
        } else if (name.equals(MailConstants.E_ME_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            return new ZMeCondition(isNegative ? MeOp.NOT_IN : MeOp.IN, header);
        } else if (name.equals(MailConstants.E_ATTACHMENT_TEST)) {
            return new ZAttachmentExistsCondition(!isNegative);
        } else if (name.equals(MailConstants.E_INVITE_TEST)) {
            List<Element> eMethods = condEl.listElements(MailConstants.E_METHOD);
            if (eMethods.isEmpty()) {
                return new ZInviteCondition(!isNegative);
            } else {
                List<String> methods = new ArrayList<String>();
                for (Element eMethod : eMethods) {
                    methods.add(eMethod.getText());
                }
                return new ZInviteCondition(!isNegative, methods);
            }
        } else if (name.equals(MailConstants.E_CONVERSATION_TEST)) {
            return new ZConversationCondition(isNegative ? ConversationOp.NOT_WHERE : ConversationOp.WHERE,
                    condEl.getAttribute(MailConstants.A_WHERE));
        } else if (name.equals(MailConstants.E_FACEBOOK_TEST)) {
            return new ZFacebookCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
        } else if (name.equals(MailConstants.E_LINKEDIN_TEST)) {
            return new ZLinkedInCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
        } else if (name.equals(MailConstants.E_SOCIALCAST_TEST)) {
            return new ZSocialcastCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
        } else if (name.equals(MailConstants.E_TWITTER_TEST)) {
            return new ZTwitterCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
        } else if (name.equals(MailConstants.E_LIST_TEST)) {
            return new ZListCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
        } else if (name.equals(MailConstants.E_BULK_TEST)) {
            return new ZBulkCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS);
        } else if (name.equals(MailConstants.E_IMPORTANCE_TEST)) {
            return new ZImportanceCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS,
                    FilterTestImportance.Importance.fromString(condEl.getAttribute(MailConstants.A_IMP)));
        } else if (name.equals(MailConstants.E_FLAGGED_TEST)) {
            return new ZFlaggedCondition(isNegative ? SimpleOp.NOT_IS : SimpleOp.IS,
                    Sieve.Flag.fromString(condEl.getAttribute(MailConstants.A_FLAG_NAME)));
        } else if (name.equals(MailConstants.E_TRUE_TEST)) {
            return new ZTrueCondition();
        } else {
             throw ZClientException.CLIENT_ERROR("unknown filter condition: "+name, null);
        }
    }

    /**
     * Adds a new test element to the given parent.
     * @return the new element
     */
    abstract Element toElement(Element parent);

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

    public static final class ZFacebookCondition extends ZFilterCondition {
        private final SimpleOp op;

        public ZFacebookCondition(SimpleOp op) {
            this.op = op;
        }

        @Override
        public String getName() {
            return "facebook";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_FACEBOOK_TEST);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "linkedin";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_LINKEDIN_TEST);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "socialcast";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_SOCIALCAST_TEST);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "twitter";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_TWITTER_TEST);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "bulk";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_BULK_TEST);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "flagged";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_FLAGGED_TEST);
            test.addAttribute(MailConstants.A_FLAG_NAME, flag.toString());
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }

        @Override
        public String toConditionString() {
            return "flagged " + (op == SimpleOp.IS ? "" : "not ") + flag;
        }
    }

    public static final class ZImportanceCondition extends ZFilterCondition {
        private final SimpleOp op;
        private final FilterTestImportance.Importance importance;

        public ZImportanceCondition(SimpleOp op, FilterTestImportance.Importance importance) {
            this.op = op;
            this.importance = importance;
        }

        @Override
        public String getName() {
            return "importance";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_IMPORTANCE_TEST);
            test.addAttribute(MailConstants.A_IMP, importance.toString());
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "list";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_LIST_TEST);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return "conversation";
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_CONVERSATION_TEST);
            test.addAttribute(MailConstants.A_WHERE, where);
            if (op == ConversationOp.NOT_WHERE) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_ADDRESS_BOOK_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
            if (op == AddressBookOp.NOT_IN) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return C_CONTACT_RANKING;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_CONTACT_RANKING_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
            if (op == ContactRankingOp.NOT_IN) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        @Override
        public String getName() {
            return C_ME;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_CONTACT_RANKING_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
            if (op == MeOp.NOT_IN) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }

        @Override
        public String toConditionString() {
            return (op == MeOp.IN ? "me in " : "me not_in ") + ZFilterRule.quotedString(header);
        }
    }

    public static final class ZBodyCondition extends ZFilterCondition {
        private BodyOp bodyOp;
        private boolean caseSensitive;
        private String text;

        public ZBodyCondition(BodyOp op, String text) {
            this(op, false, text);
        }

        public ZBodyCondition(BodyOp op, boolean caseSensitive, String text) {
            this.bodyOp = op;
            this.caseSensitive = caseSensitive;
            this.text = text;
        }

        @Override
        public String getName() {
            return C_BODY;
        }

        public BodyOp getBodyOp() { return bodyOp; }
        public boolean isCaseSensitive() { return caseSensitive; }
        public String getText() { return text; }

        @Override
        public String toConditionString() {
            return (bodyOp == BodyOp.CONTAINS ? "body contains " : "body not_contains ") +
                    (caseSensitive ? "case_sensitive " : "") + ZFilterRule.quotedString(getText());
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_BODY_TEST);
            if (caseSensitive)
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            if (!StringUtil.isNullOrEmpty(text)) {
                test.addAttribute(MailConstants.A_VALUE, text);
            }
            if (bodyOp == BodyOp.NOT_CONTAINS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }

    }

    public static final class ZCurrentDayOfWeekCondition extends ZFilterCondition {
        private SimpleOp op;
        private String days;

        public ZCurrentDayOfWeekCondition(SimpleOp op, String days) {
            this.op = op;
            this.days = days;
        }

        @Override
        public String getName() {
            return C_CURRENT_DAY;
        }

        public SimpleOp getOp() { return op; }
        public String getDays() { return days; }

        @Override
        public String toConditionString() {
            return "current_day_of_week " + (op == SimpleOp.IS ? "is " : "not_is ") + days;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_CURRENT_DAY_OF_WEEK_TEST);
            test.addAttribute(MailConstants.A_VALUE, days);
            if (op == SimpleOp.NOT_IS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }
    }

    public static final class ZSizeCondition extends ZFilterCondition {
        private SizeOp mSizeOp;
        private String mSize;

        public ZSizeCondition(SizeOp op, String size) {
            mSizeOp = op;
            mSize = size;
        }

        public SizeOp getSizeOp() { return mSizeOp; }
        public String getSize() { return mSize; }

        public String getUnits() {
            String val = getSize();
            if (val != null) {
                if (val.endsWith("M")) return "M";
                else if (val.endsWith("K")) return "K";
                else if (val.endsWith("G")) return "G";
            }
            return "B";
        }

        public String getSizeNoUnits() {
            String val = getSize();
            if (val != null) {
                if (val.endsWith("M")) return val.substring(0, val.length()-1);
                else if (val.endsWith("K")) return val.substring(0, val.length()-1);
                else if (val.endsWith("G")) return val.substring(0, val.length()-1);
            }
            return val;
        }

        @Override
        public String toConditionString() {
            return "size " + mSizeOp.name().toLowerCase() + " " + ZFilterRule.quotedString(getSize());
        }

        @Override
        public String getName() {
            return C_SIZE;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_SIZE_TEST);
            test.addAttribute(MailConstants.A_NUMBER_COMPARISON, mSizeOp.toNumberComparison().toString());
            if (mSizeOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            test.addAttribute(MailConstants.A_SIZE, mSize);
            return test;
        }
    }

    public static final class ZDateCondition extends ZFilterCondition {
        private DateOp mDateOp;
        private Date mDate;

        public ZDateCondition(DateOp op, Date date) {
            mDateOp = op;
            mDate = date;
        }

        public ZDateCondition(DateOp op, String dateStr) throws ServiceException {
            mDateOp = op;
            try {
                mDate = new SimpleDateFormat("yyyyMMdd").parse(dateStr);
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("invalid date: "+dateStr+", should be: YYYYMMDD", e);
            }
        }

        public DateOp getDateOp() { return mDateOp; }
        public Date getDate() { return mDate; }
        public String getDateString() { return Sieve.DATE_PARSER.format(mDate); }

        @Override
        public String toConditionString() {
            return "date " + mDateOp.name().toLowerCase() + " "+ ZFilterRule.quotedString(getDateString());
        }

        @Override
        public String getName() {
            return C_DATE;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_DATE_TEST);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, mDateOp.toDateComparison().toString());
            if (mDateOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            test.addAttribute(MailConstants.A_DATE, mDate.getTime() / 1000);
            return test;
        }

    }

    public static final class ZCurrentTimeCondition extends ZFilterCondition {
        private DateOp dateOp;
        private String timeStr;

        public ZCurrentTimeCondition(DateOp op, String timeStr) {
            this.dateOp = op;
            this.timeStr = timeStr;
        }

        public DateOp getDateOp() { return dateOp; }
        public String getTimeStr() { return timeStr; }

        @Override
        public String toConditionString() {
            return "current_time " + dateOp.name().toLowerCase() + " " + timeStr;
        }

        @Override
        public String getName() {
            return C_CURRENT_TIME;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_CURRENT_TIME_TEST);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, dateOp.toDateComparison().toString());
            if (dateOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            test.addAttribute(MailConstants.A_TIME, timeStr);
            return test;
        }
    }

    public static final class ZTrueCondition extends ZFilterCondition {

        @Override
        Element toElement(Element parent) {
            return parent.addElement(MailConstants.E_TRUE_TEST);
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
        private HeaderOp headerOp;
        private boolean caseSensitive;
        private String headerName;
        private String value;

        public ZHeaderCondition(String headerName, HeaderOp op, String value) {
            this(headerName, op, false, value);
        }

        public ZHeaderCondition(String headerName, HeaderOp op, boolean caseSensitive, String value) {
            this.headerName = headerName;
            this.headerOp = op;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        public HeaderOp getHeaderOp() { return headerOp; }
        public boolean isCaseSensitive() { return caseSensitive; }
        public String getHeaderName() { return headerName; }
        public String getHeaderValue()  { return value; }

        @Override
        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + " " + headerOp.name().toLowerCase() +
                    " " + (caseSensitive ? "case_sensitive " : "") + ZFilterRule.quotedString(getHeaderValue());
        }

        @Override
        public String getName() {
            return C_HEADER;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_HEADER_TEST);
            test.addAttribute(MailConstants.A_HEADER, headerName);
            test.addAttribute(MailConstants.A_STRING_COMPARISON, headerOp.toStringComparison().toString());
            if (caseSensitive)
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            if (headerOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            if (!StringUtil.isNullOrEmpty(value)) {
                test.addAttribute(MailConstants.A_VALUE, value);
            }
            return test;
        }
    }

    public static final class ZMimeHeaderCondition extends ZFilterCondition {
        private HeaderOp headerOp;
        private boolean caseSensitive;
        private String headerName;
        private String value;

        public ZMimeHeaderCondition(String headerName, HeaderOp op, String value) {
            this(headerName, op, false, value);
        }

        public ZMimeHeaderCondition(String headerName, HeaderOp op, boolean caseSensitive, String value) {
            this.headerName = headerName;
            this.headerOp = op;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        public HeaderOp getHeaderOp() { return headerOp; }
        public boolean isCaseSensitive() { return caseSensitive; }
        public String getHeaderName() { return headerName; }
        public String getHeaderValue()  { return value; }

        @Override
        public String toConditionString() {
            return "mime_header " + ZFilterRule.quotedString(getHeaderName()) + " " + headerOp.name().toLowerCase() +
                    " " + (caseSensitive ? "case_sensitive " : "") + ZFilterRule.quotedString(getHeaderValue());
        }

        @Override
        public String getName() {
            return C_MIME_HEADER;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_MIME_HEADER_TEST);
            test.addAttribute(MailConstants.A_HEADER, headerName);
            test.addAttribute(MailConstants.A_STRING_COMPARISON, headerOp.toStringComparison().toString());
            if (caseSensitive)
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            if (headerOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            if (!StringUtil.isNullOrEmpty(value)) {
                test.addAttribute(MailConstants.A_VALUE, value);
            }
            return test;
        }
    }

    public static final class ZAddressCondition extends ZFilterCondition {
        private String headerName;
        private Sieve.AddressPart part;
        private HeaderOp headerOp;
        private boolean caseSensitive;
        private String value;

        public ZAddressCondition(
                String headerName, Sieve.AddressPart part, HeaderOp op, boolean caseSensitive, String value) {
            this.headerName = headerName;
            this.part = part;
            this.headerOp = op;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        @Override
        public String toConditionString() {
            return "address " + ZFilterRule.quotedString(headerName) + " " + part + " " +
                    headerOp.name().toLowerCase() + " " + (caseSensitive ? "case_sensitive " : "") +
                    ZFilterRule.quotedString(value);
        }

        @Override
        public String getName() {
            return C_ADDRESS;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_ADDRESS_TEST);
            test.addAttribute(MailConstants.A_HEADER, headerName);
            test.addAttribute(MailConstants.A_PART, part.toString());
            test.addAttribute(MailConstants.A_STRING_COMPARISON, headerOp.toStringComparison().toString());
            if (caseSensitive)
                test.addAttribute(MailConstants.A_CASE_SENSITIVE, caseSensitive);
            if (headerOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            if (!StringUtil.isNullOrEmpty(value)) {
                test.addAttribute(MailConstants.A_VALUE, value);
            }
            return test;
        }
    }

    public static final class ZHeaderExistsCondition extends ZFilterCondition {
        private boolean mExists;
        private String mHeaderName;

        public ZHeaderExistsCondition(String headerName, boolean exists) {
            mExists = exists;
            mHeaderName = headerName;
        }

        public boolean getExists() { return mExists; }
        public String getHeaderName() { return mHeaderName; }

        @Override
        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + (mExists ? " exists" : " not_exists");
        }

        @Override
        public String getName() {
            return C_EXISTS;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_HEADER_EXISTS_TEST);
            test.addAttribute(MailConstants.A_HEADER, mHeaderName);
            if (!mExists) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }
    }

    public static final class ZAttachmentExistsCondition extends ZFilterCondition {
        private boolean mExists;

        public ZAttachmentExistsCondition(boolean exists) {
            mExists = exists;
        }

        public boolean getExists() { return mExists; }

        @Override
        public String toConditionString() {
            return mExists ? "attachment exists" : "attachment not_exists";
        }

        @Override
        public String getName() {
            return C_ATTACHMENT;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_ATTACHMENT_TEST);
            if (!mExists) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
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

        private boolean mIsInvite;
        private List<String> mMethods = new ArrayList<String>();

        public ZInviteCondition(boolean isInvite) {
            mIsInvite = isInvite;
        }

        public ZInviteCondition(boolean isInvite, String method) {
            this(isInvite, Arrays.asList(method));
        }

        public ZInviteCondition(boolean isInvite, List<String> methods) {
            mIsInvite = isInvite;
            if (methods != null) {
                mMethods.addAll(methods);
            }
        }

        public void setMethods(String ... methods) {
            mMethods.clear();
            Collections.addAll(mMethods, methods);
        }

        public boolean isInvite() { return mIsInvite; }

        @Override
        public String toConditionString() {
            StringBuilder buf = new StringBuilder("invite ");
            if (!mIsInvite) {
                buf.append("not_");
            }
            buf.append("exists");
            if (!mMethods.isEmpty()) {
                buf.append(" method ").append(StringUtil.join(",", mMethods));
            }
            return buf.toString();
        }

        @Override
        public String getName() {
            return C_INVITE;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_INVITE_TEST);
            if (!mIsInvite) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            for (String method : mMethods) {
                test.addElement(MailConstants.E_METHOD).setText(method);
            }
            return test;
        }
    }
}

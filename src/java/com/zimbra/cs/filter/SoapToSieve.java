/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.filter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;

public final class SoapToSieve {

    private final List<FilterRule> rules;
    private StringBuilder buffer;

    public SoapToSieve(List<FilterRule> rules) {
        this.rules = rules;
    }

    public String getSieveScript() throws ServiceException {
        if (buffer == null) {
            buffer = new StringBuilder();
            buffer.append("require [\"fileinto\", \"reject\", \"tag\", \"flag\"];\n");
            for (FilterRule rule : rules) {
                buffer.append('\n');
                handleRule(rule);
            }
        }
        return buffer.toString();
    }

    private void handleRule(FilterRule rule) throws ServiceException {
        String name = rule.getName();
        boolean active = rule.isActive();

        FilterTests tests = rule.getFilterTests();
        Sieve.Condition condition = Sieve.Condition.fromString(tests.getCondition());
        if (condition == null) {
            condition = Sieve.Condition.allof;
        }

        // Rule name
        buffer.append("# ").append(name).append('\n');
        if (active) {
            buffer.append("if ");
        } else {
            buffer.append("disabled_if ");
        }
        buffer.append(condition).append(" (");

        // Handle tests
        Map<Integer, String> index2test = new TreeMap<Integer, String>(); // sort by index
        for (FilterTest test : tests.getTests()) {
            String result = handleTest(test);
            if (result != null) {
                FilterUtil.addToMap(index2test, test.getIndex(), result);
            }
        }
        Joiner.on(",\n  ").appendTo(buffer, index2test.values());
        buffer.append(") {\n");

        // Handle actions
        Map<Integer, String> index2action = new TreeMap<Integer, String>(); // sort by index
        for (FilterAction action : rule.getFilterActions()) {
            String result = handleAction(action);
            if (result != null) {
                FilterUtil.addToMap(index2action, action.getIndex(), result);
            }
        }
        for (String action : index2action.values()) {
            buffer.append("    ").append(action).append(";\n");
        }
        buffer.append("}\n");
    }

    private String handleTest(FilterTest test) throws ServiceException {
        String snippet = null;

        if (test instanceof FilterTest.HeaderTest) {
            snippet = toSieve((FilterTest.HeaderTest) test);
        } else if (test instanceof FilterTest.MimeHeaderTest) {
            snippet = toSieve((FilterTest.MimeHeaderTest) test);
        } else if (test instanceof FilterTest.AddressTest) {
            snippet = toSieve((FilterTest.AddressTest) test);
        } else if (test instanceof FilterTest.HeaderExistsTest) {
            String header = ((FilterTest.HeaderExistsTest) test).getHeader();
            snippet = String.format("exists \"%s\"", FilterUtil.escape(header));
        } else if (test instanceof FilterTest.SizeTest) {
            FilterTest.SizeTest sizeTest = (FilterTest.SizeTest) test;
            Sieve.NumberComparison comp = Sieve.NumberComparison.fromString(sizeTest.getNumberComparison());
            String size = sizeTest.getSize();
            try {
                FilterUtil.parseSize(size);
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST("Invalid size: " + size, e);
            }
            snippet = String.format("size :%s %s", comp, size);
        } else if (test instanceof FilterTest.DateTest) {
            FilterTest.DateTest dateTest = (FilterTest.DateTest) test;
            Sieve.DateComparison comp = Sieve.DateComparison.fromString(dateTest.getDateComparison());
            Date date = new Date(dateTest.getDate() * 1000L);
            snippet = String.format("date :%s \"%s\"", comp, Sieve.DATE_PARSER.format(date));
        } else if (test instanceof FilterTest.BodyTest) {
            FilterTest.BodyTest bodyTest = (FilterTest.BodyTest) test;
            String format = bodyTest.isCaseSensitive() ?
                    "body :contains :comparator \"i;octet\" \"%s\"" : "body :contains \"%s\"";
            snippet = String.format(format, FilterUtil.escape(bodyTest.getValue()));
        } else if (test instanceof FilterTest.AddressBookTest) {
            FilterTest.AddressBookTest abTest = (FilterTest.AddressBookTest) test;
            snippet = "addressbook :in \"" + FilterUtil.escape(abTest.getHeader()) + '"';
        } else if (test instanceof FilterTest.ContactRankingTest) {
            FilterTest.ContactRankingTest rankingTest = (FilterTest.ContactRankingTest) test;
            snippet = "contact_ranking :in \"" + FilterUtil.escape(rankingTest.getHeader()) + '"';
        } else if (test instanceof FilterTest.MeTest) {
            FilterTest.MeTest meTest = (FilterTest.MeTest) test;
            snippet = "me :in \"" + FilterUtil.escape(meTest.getHeader()) + '"';
        } else if (test instanceof FilterTest.AttachmentTest) {
            snippet = "attachment";
        } else if (test instanceof FilterTest.InviteTest) {
            snippet = toSieve((FilterTest.InviteTest) test);
        } else if (test instanceof FilterTest.CurrentTimeTest) {
            FilterTest.CurrentTimeTest timeTest = (FilterTest.CurrentTimeTest) test;
            Sieve.DateComparison comparison = Sieve.DateComparison.fromString(timeTest.getDateComparison());
            String time = timeTest.getTime();
            // validate time value
            String timeFormat = "HHmm";
            SimpleDateFormat parser = new SimpleDateFormat(timeFormat);
            parser.setLenient(false);
            try {
                parser.parse(time);
            } catch (ParseException e) {
                throw ServiceException.INVALID_REQUEST("Invalid time: " + time, e);
            }
            if (time.length() != timeFormat.length()) {
                throw ServiceException.INVALID_REQUEST("Time string must be of length " + timeFormat.length(), null);
            }
            snippet = String.format("current_time :%s \"%s\"", comparison, time);
        } else if (test instanceof FilterTest.CurrentDayOfWeekTest) {
            FilterTest.CurrentDayOfWeekTest dayTest = (FilterTest.CurrentDayOfWeekTest) test;
            String value = dayTest.getValues();
            String[] daysOfWeek = value.split(",");
            for (int i = 0; i < daysOfWeek.length; i ++) {
                // first validate value
                try {
                    int day = Integer.valueOf(daysOfWeek[i]);
                    if (day < 0 || day > 6) {
                        throw ServiceException.INVALID_REQUEST(
                                "Day of week index must be from 0 (Sunday) to 6 (Saturday)", null);
                    }
                } catch (NumberFormatException e) {
                    throw ServiceException.INVALID_REQUEST("Invalid day of week index: " + daysOfWeek[i], e);
                }

                daysOfWeek[i] = StringUtil.enclose(daysOfWeek[i], '"');
            }
            snippet = "current_day_of_week :is [" + Joiner.on(',').join(daysOfWeek) + "]";
        } else if (test instanceof FilterTest.ConversationTest) {
            FilterTest.ConversationTest convTest = (FilterTest.ConversationTest) test;
            String where = Objects.firstNonNull(convTest.getWhere(), "started");
            snippet = String.format("conversation :where \"%s\"", FilterUtil.escape(where));
        } else if (test instanceof FilterTest.FacebookTest) {
            snippet = "facebook";
        } else if (test instanceof FilterTest.LinkedInTest) {
            snippet = "linkedin";
        } else if (test instanceof FilterTest.SocialcastTest) {
            snippet = "socialcast";
        } else if (test instanceof FilterTest.TwitterTest) {
            snippet = "twitter";
        } else if (test instanceof FilterTest.ListTest) {
            snippet = "list";
        } else if (test instanceof FilterTest.BulkTest) {
            snippet = "bulk";
        } else if (test instanceof FilterTest.ImportanceTest) {
            FilterTest.ImportanceTest impTest = (FilterTest.ImportanceTest) test;
            snippet = String.format("importance \"%s\"", impTest.getImportance());
        } else if (test instanceof FilterTest.FlaggedTest) {
            FilterTest.FlaggedTest flaggedTest = (FilterTest.FlaggedTest) test;
            snippet = "flagged \"" + flaggedTest.getFlag() + "\"";
        } else if (test instanceof FilterTest.TrueTest) {
            snippet = "true";
        } else {
            ZimbraLog.soap.debug("Ignoring unexpected test: %s", test);
        }

        if (snippet != null && test.isNegative()) {
            snippet = "not " + snippet;
        }
        return snippet;
    }

    private static String toSieve(FilterTest.HeaderTest test) throws ServiceException {
        String header = getSieveHeaderList(test.getHeaders());
        Sieve.StringComparison comp = Sieve.StringComparison.fromString(test.getStringComparison());
        return toSieve("header", header, comp, test.isCaseSensitive(), test.getValue());
    }

    private static String toSieve(FilterTest.MimeHeaderTest test) throws ServiceException {
        String header = getSieveHeaderList(test.getHeaders());
        Sieve.StringComparison comp = Sieve.StringComparison.fromString(test.getStringComparison());
        return toSieve("mime_header", header, comp, test.isCaseSensitive(), test.getValue());
    }

    private static String toSieve(String name, String header, Sieve.StringComparison comp, boolean caseSensitive,
            String value) throws ServiceException {
        checkValue(comp, value);
        String format = caseSensitive ? "%s :%s :comparator \"" + Sieve.Comparator.ioctet + "\" %s \"%s\"" :
            "%s :%s %s \"%s\"";
        return String.format(format, name, comp, header, FilterUtil.escape(value));
    }

    private static String toSieve(FilterTest.AddressTest test) throws ServiceException {
        String header = getSieveHeaderList(test.getHeader());
        Sieve.AddressPart part = Sieve.AddressPart.fromString(test.getPart());
        if (part == null) {
            part = Sieve.AddressPart.all;
        }
        Sieve.StringComparison comp = Sieve.StringComparison.fromString(test.getStringComparison());
        String value = test.getValue();
        checkValue(comp, value);
        return String.format("address :%s :%s :comparator \"%s\" %s \"%s\"", part, comp,
                test.isCaseSensitive() ? Sieve.Comparator.ioctet : Sieve.Comparator.iasciicasemap,
                        header, FilterUtil.escape(value));
    }

    private static void checkValue(Sieve.StringComparison comparison, String value) throws ServiceException {
        // Bug 35983: disallow more than four asterisks in a row.
        if (comparison == Sieve.StringComparison.matches && value != null && value.contains("*****")) {
            throw ServiceException.INVALID_REQUEST(
                "Wildcard match value cannot contain more than four asterisks in a row.", null);
        }
    }

    private static String getSieveHeaderList(String header) throws ServiceException {
        if (header.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("header value is empty", null);
        }
        return getSieveMultiValue(header);
    }

    private static String getSieveMultiValue(String s) {
        String[] values = new String[0];
        // empty string means no value
        if (!s.isEmpty()) {
            values = s.split(",");
            for (int i = 0; i < values.length; i++) {
                values[i] = StringUtil.enclose(FilterUtil.escape(values[i]), '"');
            }
        }
        return new StringBuilder().append('[').append(StringUtil.join(",", values)).append(']').toString();
    }

    private static String toSieve(FilterTest.InviteTest test) {
        StringBuilder buf = new StringBuilder("invite");
        List<String> methods = test.getMethods();
        if (!methods.isEmpty()) {
            buf.append(" :method [");
            boolean firstTime = true;
            for (String method : methods) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    buf.append(", ");
                }
                buf.append('"');
                buf.append(FilterUtil.escape(method));
                buf.append('"');
            }
            buf.append("]");
        }
        return buf.toString();
    }

    private static String handleAction(FilterAction action) throws ServiceException {
        if (action instanceof FilterAction.KeepAction) {
            return "keep";
        } else if (action instanceof FilterAction.DiscardAction) {
            return "discard";
        } else if (action instanceof FilterAction.FileIntoAction) {
            FilterAction.FileIntoAction fileinto = (FilterAction.FileIntoAction) action;
            return String.format("fileinto \"%s\"", FilterUtil.escape(fileinto.getFolder()));
        } else if (action instanceof FilterAction.TagAction) {
            FilterAction.TagAction tag = (FilterAction.TagAction) action;
            return String.format("tag \"%s\"", FilterUtil.escape(tag.getTag()));
        } else if (action instanceof FilterAction.FlagAction) {
            FilterAction.FlagAction flag = (FilterAction.FlagAction) action;
            return String.format("flag \"%s\"", Sieve.Flag.valueOf(flag.getFlag()));
        } else if (action instanceof FilterAction.RedirectAction) {
            FilterAction.RedirectAction redirect = (FilterAction.RedirectAction) action;
            return String.format("redirect \"%s\"", FilterUtil.escape(redirect.getAddress()));
        } else if (action instanceof FilterAction.ReplyAction) {
            FilterAction.ReplyAction reply = (FilterAction.ReplyAction) action;
            return new StringBuilder("reply text:\r\n")
                .append(getDotStuffed(reply.getContent()))
                .append("\r\n.\r\n")
                .toString();
        } else if (action instanceof FilterAction.NotifyAction) {
            FilterAction.NotifyAction notify = (FilterAction.NotifyAction) action;
            String emailAddr = notify.getAddress();
            String subjectTemplate = Strings.nullToEmpty(notify.getSubject());
            String bodyTemplate = Strings.nullToEmpty(notify.getContent());
            int maxBodyBytes = Objects.firstNonNull(notify.getMaxBodySize(), -1);
            String origHeaders = Strings.nullToEmpty(notify.getOrigHeaders());
            if (!subjectTemplate.isEmpty() && containsSubjectHeader(origHeaders)) {
                throw ServiceException.INVALID_REQUEST("subject conflict", null);
            }
            return new StringBuilder("notify ").
                    append(StringUtil.enclose(emailAddr, '"')).append(' ').
                    append(StringUtil.enclose(subjectTemplate, '"')).append(' ').
                    append("text:\r\n").append(getDotStuffed(bodyTemplate)).append("\r\n.\r\n").
                    append(maxBodyBytes < 0 ? "" : " " + maxBodyBytes).
                    append(origHeaders.isEmpty() ? "" : " " + getSieveMultiValue(origHeaders)).toString();
        } else if (action instanceof FilterAction.StopAction) {
            return "stop";
        } else {
            ZimbraLog.soap.debug("Ignoring unexpected action: %s", action);
        }
        return null;
    }

    private static boolean containsSubjectHeader(String origHeaders) {
        for (String header : origHeaders.split(",")) {
            if ("Subject".equalsIgnoreCase(header)) {
                return true;
            }
        }
        return false;
    }

    private static String getDotStuffed(String bodyTemplate) {
        return bodyTemplate.replaceAll("\r\n\\.", "\r\n..");
    }
}

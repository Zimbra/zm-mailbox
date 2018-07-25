/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.mail.type.EditheaderTest;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;
import com.zimbra.soap.mail.type.FilterVariable;
import com.zimbra.soap.mail.type.FilterVariables;
import com.zimbra.soap.mail.type.NestedRule;

public final class SoapToSieve {

    private final List<FilterRule> rules;
    private StringBuilder buffer;

    // end of line in sieve script
    public static final String END_OF_LINE = ";\n";
    public static final String requireCommon = "\"fileinto\", \"copy\", \"reject\", \"tag\", \"flag\", "
            + "\"variables\", \"log\", \"enotify\", \"envelope\", \"body\", "
            + "\"ereject\", \"reject\", \"relational\", \"comparator-i;ascii-numeric\"";

    public SoapToSieve(List<FilterRule> rules) {
        this.rules = rules;
    }

    public String getSieveScript() throws ServiceException {
        if (buffer == null) {
            buffer = new StringBuilder();
            buffer.append("require [" + requireCommon + "]" + END_OF_LINE);
            for (FilterRule rule : rules) {
                buffer.append('\n');
                handleRule(rule);
            }
        }
        return buffer.toString();
    }

    public String getAdminSieveScript() throws ServiceException {
        if (buffer == null) {
            buffer = new StringBuilder();
            buffer.append("require [" + requireCommon + ", \"editheader\"]" + END_OF_LINE);
            for (FilterRule rule : rules) {
                buffer.append('\n');
                handleRule(rule, true);
            }
        }
        return buffer.toString();
    }

    private void handleRule(FilterRule rule) throws ServiceException {
        handleRule(rule, false);
    }

    private void handleRule(FilterRule rule, boolean isAdminScript) throws ServiceException {
        String name = rule.getName();
        boolean active = rule.isActive();

        // Rule name
        buffer.append("# ").append(name).append('\n');

        FilterVariables filterVariables = rule.getFilterVariables();

        buffer.append(handleVariables(filterVariables, null));

        FilterTests tests = rule.getFilterTests();
        if (hasTest(tests)) {
            Sieve.Condition condition = Sieve.Condition.fromString(tests.getCondition());
            if (condition == null) {
                condition = Sieve.Condition.allof;
            }

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
        }

        // Handle actions
        Map<Integer, String> index2action = new TreeMap<Integer, String>(); // sort by index
        List<FilterAction> filterActions = rule.getFilterActions();

        String variables = "";
        if (filterActions != null) {
            for (FilterAction action : filterActions) {
                if (action instanceof FilterVariables) {
                    FilterVariables var = (FilterVariables) action;
                    variables = handleVariables(var, "    ");
                } else {
                    String result = handleAction(action, isAdminScript);
                    if (result != null) {
                        FilterUtil.addToMap(index2action, action.getIndex(), result);
                    }
                }
            }
            for (String action : index2action.values()) {
                if (hasTest(tests)) {
                    buffer.append("    ");
                }
                buffer.append(action).append(END_OF_LINE);
            }
            if (!variables.isEmpty()) {
                buffer.append(variables);
            }
        }

        NestedRule child = rule.getChild();
        // Handle nested rule
        if(child!=null){
            // first nested block's indent is "    "
            String nestedRuleBlock = handleNest("    ", child, isAdminScript);
            buffer.append(nestedRuleBlock);
        }

        if ( filterActions == null && child == null) {   // if there is no action in this rule, filterActions is supposed to be null.
                // If there is no more nested rule, there should be at least one action.
                throw ServiceException.INVALID_REQUEST("Missing action", null);
        }

        if (hasTest(tests)) {
            buffer.append("}\n");
        }
    }

    private boolean hasTest(FilterTests tests) {
        return tests != null && tests.getTests() != null && !tests.getTests().isEmpty();
    }

    // Constructing nested rule block with base indents which is for entire block.
    private String handleNest(String baseIndents, NestedRule currentNestedRule, boolean isAdminScript) throws ServiceException {

        StringBuilder nestedIfBlock = new StringBuilder();

        FilterVariables filterVariables = currentNestedRule.getFilterVariables();

        nestedIfBlock.append(handleVariables(filterVariables, baseIndents));

        Sieve.Condition childCondition =
                Sieve.Condition.fromString(currentNestedRule.getFilterTests().getCondition());
        if (childCondition == null) {
            childCondition = Sieve.Condition.allof;
        }

        // assuming no disabled_if for child tests so far
        nestedIfBlock.append(baseIndents).append("if ");
        nestedIfBlock.append(childCondition).append(" (");

        // Handle tests
        Map<Integer, String> index2childTest = new TreeMap<Integer, String>(); // sort by index
        for (FilterTest childTest : currentNestedRule.getFilterTests().getTests()) {
            String childResult = handleTest(childTest);
            if (childResult != null) {
                FilterUtil.addToMap(index2childTest, childTest.getIndex(), childResult);
            }
        }
        Joiner.on(",\n  "+baseIndents).appendTo(nestedIfBlock, index2childTest.values());
        nestedIfBlock.append(") {\n");

        // Handle actions
        Map<Integer, String> index2childAction = new TreeMap<Integer, String>(); // sort by index
        List<FilterAction> childActions = currentNestedRule.getFilterActions();

        String variables = "";
        if (childActions != null) {
            for (FilterAction childAction : childActions) {
                if (childAction instanceof FilterVariables) {
                    FilterVariables var = (FilterVariables) childAction;
                    variables = handleVariables(var, baseIndents + "    ");
                } else {
                    String childResult = handleAction(childAction, isAdminScript);
                    if (childResult != null) {
                        FilterUtil.addToMap(index2childAction, childAction.getIndex(), childResult);
                    }
                }
            }
            for (String childAction : index2childAction.values()) {
                nestedIfBlock.append(baseIndents);
                nestedIfBlock.append("    ").append(childAction).append(END_OF_LINE);
            }
            if (!variables.isEmpty()) {
                nestedIfBlock.append(variables);
            }
        }
        // Handle nest
        if(currentNestedRule.getChild() != null){
            nestedIfBlock.append(handleNest(baseIndents + "    ", currentNestedRule.getChild(), isAdminScript));
        }

        if (childActions == null && currentNestedRule.getChild() == null) { // if there is no action in this rule, childActions is supposed to be null.
                // If there is no more nested rule, there should be at least one action.
                throw ServiceException.INVALID_REQUEST("Missing action", null);
        }

        nestedIfBlock.append(baseIndents);
        nestedIfBlock.append("}\n");
        return nestedIfBlock.toString();
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
            String header = abTest.getHeader();
            if (header.contains(",")) {
                String[] headerList = header.split(",");
                StringBuilder format = new StringBuilder();
                for (String item: headerList) {
                    if (format.length() > 0) {
                        format.append(",");
                    }
                    format.append("\"").append(FilterUtil.escape(item)).append("\"");
                }
                snippet = "addressbook :in [" + format.toString() + ']';
            } else {
                snippet = "addressbook :in \"" + FilterUtil.escape(header) + '"';
            }
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
            String where = MoreObjects.firstNonNull(convTest.getWhere(), "started");
            snippet = String.format("conversation :where \"%s\"", FilterUtil.escape(where));
        } else if (test instanceof FilterTest.FacebookTest) {
            snippet = "facebook";
        } else if (test instanceof FilterTest.LinkedInTest) {
            snippet = "linkedin";
        } else if (test instanceof FilterTest.SocialcastTest) {
            snippet = "socialcast";
        } else if (test instanceof FilterTest.TwitterTest) {
            snippet = "twitter";
        } else if (test instanceof FilterTest.CommunityRequestsTest) {
            snippet = "community_requests";
        } else if (test instanceof FilterTest.CommunityContentTest) {
            snippet = "community_content";
        } else if (test instanceof FilterTest.CommunityConnectionsTest) {
            snippet = "community_connections";
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
        String val = test.getValue();
        if (Strings.isNullOrEmpty(val)) {
            throw ServiceException.INVALID_REQUEST("missing required attribute: value" , null);
        }

        if (StringUtils.isNotEmpty(test.getStringComparison())) {
            Sieve.StringComparison comp = Sieve.StringComparison.fromString(test.getStringComparison());
            return toSieve("header", header, comp, test.isCaseSensitive(), test.getValue());
        }

        if (StringUtils.isNotEmpty(test.getValueComparison())) {
            Sieve.ValueComparison comp = Sieve.ValueComparison.fromString(test.getValueComparison());
            Sieve.Comparator valueComparisonComparator = Sieve.Comparator.fromString(test.getValueComparisonComparator());
            return toSieve("header", header, comp, valueComparisonComparator, test.isCaseSensitive(), test.getValue(), false, null);
        }

        if (StringUtils.isNotEmpty(test.getCountComparison())) {
            Sieve.ValueComparison comp = Sieve.ValueComparison.fromString(test.getCountComparison());
            return toSieve("header", header, comp, null, false, test.getValue(), true, null);
        }
        return null;
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

    private static String toSieve(String name, String header, Sieve.ValueComparison comp, Sieve.Comparator valueComparator, boolean caseSensitive, String value, boolean isCount, Sieve.AddressPart part) throws ServiceException {
        String countOrVal = isCount ? HeaderConstants.COUNT : HeaderConstants.VALUE;
        Sieve.Comparator comparator;
        if (valueComparator == null) {
            boolean numeric = true;
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                numeric = false;
            }
            //for :count, iasciinumeric comparator will be used always
            //for :value, if comparator value is not set in input, iasciinumeric comparator will be used if value is numeric
            //else iasciicasemap will be used for case-insensitive and ioctet for case-sensitive 
            comparator = Sieve.Comparator.iasciinumeric;
            if (!numeric && !isCount) {
                if (caseSensitive) {
                    comparator = Sieve.Comparator.ioctet;
                } else {
                    comparator = Sieve.Comparator.iasciicasemap;
                }
            }
        } else {
            comparator = valueComparator;
        }
        if (part == null) {
            String format = "%s " + countOrVal + " \"%s\" :comparator \"" + comparator + "\" %s \"%s\"";
            return String.format(format, name, comp, header, FilterUtil.escape(value));
        } else {
            String format = "%s " + countOrVal + " \"%s\" :%s :comparator \"" + comparator + "\" %s \"%s\"";
            return String.format(format, name, comp, part, header, FilterUtil.escape(value));
        }

    }

    private static String toSieve(FilterTest.AddressTest test) throws ServiceException {
        if (test instanceof FilterTest.EnvelopeTest) {
            return formatAddress((FilterTest.EnvelopeTest) test, "envelope");
        }
        return formatAddress(test, "address");
    }

    private static String formatAddress(FilterTest.AddressTest test, String testName) throws ServiceException {
        String header = getSieveHeaderList(test.getHeader());
        Sieve.AddressPart part = Sieve.AddressPart.fromString(test.getPart());
        if (part == null) {
            part = Sieve.AddressPart.all;
        }
        if (StringUtils.isNotEmpty(test.getStringComparison())) {
            Sieve.StringComparison comp = Sieve.StringComparison.fromString(test.getStringComparison());
            String value = test.getValue();
            checkValue(comp, value);
            String valueStr = null == value ? "" : FilterUtil.escape(value);
            return String.format("%s :%s :%s :comparator \"%s\" %s \"%s\"", testName, part, comp,
                    test.isCaseSensitive() ? Sieve.Comparator.ioctet : Sieve.Comparator.iasciicasemap,
                            header, valueStr);
        }

        if (StringUtils.isNotEmpty(test.getValueComparison())) {
            Sieve.ValueComparison comp = Sieve.ValueComparison.fromString(test.getValueComparison());
            Sieve.Comparator valueComparisonComparator = Sieve.Comparator.fromString(test.getValueComparisonComparator());
            return toSieve(testName, header, comp, valueComparisonComparator, test.isCaseSensitive(), test.getValue(), false, part);
        }

        if (StringUtils.isNotEmpty(test.getCountComparison())) {
            Sieve.ValueComparison comp = Sieve.ValueComparison.fromString(test.getCountComparison());
            return toSieve(testName, header, comp, null, false, test.getValue(), true, part);
        }
        return null;
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

    private String handleAction(FilterAction action, boolean isAdminScript) throws ServiceException {
        if (action instanceof FilterAction.KeepAction) {
            return "keep";
        } else if (action instanceof FilterAction.DiscardAction) {
            return "discard";
        } else if (action instanceof FilterAction.FileIntoAction) {
            FilterAction.FileIntoAction fileinto = (FilterAction.FileIntoAction) action;
            String folderPath = fileinto.getFolder();
            boolean copy = fileinto.isCopy();
            if (StringUtil.isNullOrEmpty(folderPath)) {
                throw ServiceException.INVALID_REQUEST("Missing folderPath", null);
            }
            if (copy) {
                return String.format("fileinto :copy \"%s\"", FilterUtil.escape(folderPath));
            } else {
                return String.format("fileinto \"%s\"", FilterUtil.escape(folderPath));
            }
        } else if (action instanceof FilterAction.TagAction) {
            FilterAction.TagAction tag = (FilterAction.TagAction) action;
            String tagName = tag.getTag();
            if (StringUtil.isNullOrEmpty(tagName)) {
                throw ServiceException.INVALID_REQUEST("Missing tag", null);
            }
            return String.format("tag \"%s\"", FilterUtil.escape(tagName));
        } else if (action instanceof FilterAction.FlagAction) {
            FilterAction.FlagAction flag = (FilterAction.FlagAction) action;
            String flagName = flag.getFlag();
            if (StringUtil.isNullOrEmpty(flagName)) {
                throw ServiceException.INVALID_REQUEST("Missing flag", null);
            }
            return String.format("flag \"%s\"", Sieve.Flag.valueOf(flagName));
        } else if (action instanceof FilterAction.RedirectAction) {
            FilterAction.RedirectAction redirect = (FilterAction.RedirectAction) action;
            String address = redirect.getAddress();
            boolean copy = redirect.isCopy();
            if (StringUtil.isNullOrEmpty(address)) {
                throw ServiceException.INVALID_REQUEST("Missing address", null);
            }
            if (copy) {
                return String.format("redirect :copy \"%s\"", FilterUtil.escape(address));
            } else {
                return String.format("redirect \"%s\"", FilterUtil.escape(address));
            }
        } else if (action instanceof FilterAction.ReplyAction) {
            FilterAction.ReplyAction reply = (FilterAction.ReplyAction) action;
            String content = reply.getContent();
            if (StringUtil.isNullOrEmpty(content)) {
                throw ServiceException.INVALID_REQUEST("Missing reply content", null);
            }
            return new StringBuilder("reply text:\r\n")
                .append(getDotStuffed(content))
                .append("\r\n.\r\n")
                .toString();
        } else if (action instanceof FilterAction.NotifyAction) {
            FilterAction.NotifyAction notify = (FilterAction.NotifyAction) action;
            String emailAddr = notify.getAddress();
            if (StringUtil.isNullOrEmpty(emailAddr)) {
                throw ServiceException.INVALID_REQUEST("Missing address", null);
            }
            String subjectTemplate = Strings.nullToEmpty(notify.getSubject());
            String bodyTemplate = Strings.nullToEmpty(notify.getContent());
            int maxBodyBytes = MoreObjects.firstNonNull(notify.getMaxBodySize(), -1);
            String origHeaders = Strings.nullToEmpty(notify.getOrigHeaders());
            if (!subjectTemplate.isEmpty() && containsSubjectHeader(origHeaders)) {
                throw ServiceException.INVALID_REQUEST("subject conflict", null);
            }
            return new StringBuilder("notify ").
                    append(StringUtil.enclose(FilterUtil.escape(emailAddr), '"')).append(' ').
                    append(StringUtil.enclose(subjectTemplate, '"')).append(' ').
                    append("text:\r\n").append(getDotStuffed(bodyTemplate)).append("\r\n.\r\n").
                    append(maxBodyBytes < 0 ? "" : " " + maxBodyBytes).
                    append(origHeaders.isEmpty() ? "" : " " + getSieveMultiValue(origHeaders)).toString();
        } else if (action instanceof FilterAction.RFCCompliantNotifyAction) {
            FilterAction.RFCCompliantNotifyAction notify = (FilterAction.RFCCompliantNotifyAction) action;
            String from = notify.getFrom();
            String importance = Strings.nullToEmpty(notify.getImportance());
            String options = Strings.nullToEmpty(notify.getOptions());
            String message = Strings.nullToEmpty(notify.getMessage());
            String method = Strings.nullToEmpty(notify.getMethod());
            if (StringUtil.isNullOrEmpty(method)) {
                throw ServiceException.INVALID_REQUEST("Missing notification mechanism", null);
            }

            StringBuilder filter = new StringBuilder("notify ");
            if (!from.isEmpty()) {
                filter.append(":from ").append(StringUtil.enclose(FilterUtil.escape(from), '"')).append(' ');
            }
            if (!importance.isEmpty()) {
                filter.append(":importance ").append(StringUtil.enclose(importance, '"')).append(' ');
            }
            if (!options.isEmpty()) {
                filter.append(":options ").append(StringUtil.enclose(options, '"')).append(' ');
            }
            if (!message.isEmpty()) {
                filter.append(":message ").append(StringUtil.enclose(message, '"')).append(' ');
            }
            if (method.indexOf("\n") < 0) {
                filter.append(StringUtil.enclose(method, '"'));
            } else {
                filter.append("text:\r\n").append(method).append("\r\n.\r\n");
            }
            return filter.toString();
        } else if (action instanceof FilterAction.StopAction) {
            return "stop";
        } else if (action instanceof FilterAction.RejectAction) {
            FilterAction.RejectAction rejectAction = (FilterAction.RejectAction)action;
            return handleRejectAction(rejectAction);
        } else if (action instanceof FilterAction.ErejectAction) {
            FilterAction.ErejectAction erejectAction = (FilterAction.ErejectAction)action;
            return handleRejectAction(erejectAction);
        } else if (action instanceof FilterAction.LogAction) {
            FilterAction.LogAction logAction = (FilterAction.LogAction)action;
            StringBuilder sb = new StringBuilder();
            sb.append("log");
            FilterAction.LogAction.LogLevel level = logAction.getLevel();
            if (level != null) {
                if (!(FilterAction.LogAction.LogLevel.fatal == level
                        || FilterAction.LogAction.LogLevel.error == level
                        || FilterAction.LogAction.LogLevel.warn == level
                        || FilterAction.LogAction.LogLevel.info == level
                        || FilterAction.LogAction.LogLevel.debug == level
                        || FilterAction.LogAction.LogLevel.trace == level
                        )) {
                    String message = "Invalid log action: Invalid log level found: " + level.toString();
                    throw ServiceException.PARSE_ERROR(message, null);
                }
                sb.append(" :").append(level.toString());
            }
            sb.append(" \"").append(logAction.getContent()).append("\"");
            return sb.toString();
        } else if (action instanceof FilterAction.AddheaderAction) {
            if (!isAdminScript) {
                throw ServiceException.PARSE_ERROR("Invalid addheader action: addheader action is not allowed in user scripts", null);
            }
            FilterAction.AddheaderAction addheaderAction = (FilterAction.AddheaderAction) action;
            if (StringUtil.isNullOrEmpty(addheaderAction.getHeaderName()) || StringUtil.isNullOrEmpty(addheaderAction.getHeaderValue())) {
                throw ServiceException.PARSE_ERROR("Invalid addheader action: Missing headerName or headerValue", null);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("addheader");
            if (addheaderAction.getLast() != null && addheaderAction.getLast()) {
                sb.append(" :last");
            }
            sb.append(" \"").append(addheaderAction.getHeaderName()).append("\"");
            sb.append(" \"").append(addheaderAction.getHeaderValue()).append("\"");
            return sb.toString();
        } else if (action instanceof FilterAction.ReplaceheaderAction) {
            if (!isAdminScript) {
                throw ServiceException.PARSE_ERROR("Invalid replaceheader action: replaceheader action is not allowed in user scripts", null);
            }
            FilterAction.ReplaceheaderAction replaceheaderAction = (FilterAction.ReplaceheaderAction) action;
            replaceheaderAction.validateReplaceheaderAction();
            StringBuilder sb  = new StringBuilder();
            sb.append("replaceheader");
            if (replaceheaderAction.getLast() != null && replaceheaderAction.getLast()) {
                sb.append(" :last");
            }
            if (replaceheaderAction.getOffset() != null) {
                sb.append(" :index ").append(replaceheaderAction.getOffset());
            }
            if (!StringUtil.isNullOrEmpty(replaceheaderAction.getNewName())) {
                sb.append(" :newname ").append("\"").append(replaceheaderAction.getNewName()).append("\"");
            }
            if (!StringUtil.isNullOrEmpty(replaceheaderAction.getNewValue())) {
                sb.append(" :newvalue ").append("\"").append(replaceheaderAction.getNewValue()).append("\"");
            }
            EditheaderTest test = replaceheaderAction.getTest();
            if (test.getCount() != null && test.getCount()) {
                sb.append(" :count");
            } else if (test.getValue() != null && test.getValue()) {
                sb.append(" :value");
            }
            if (!StringUtil.isNullOrEmpty(test.getRelationalComparator())) {
                sb.append(" \"").append(test.getRelationalComparator()).append("\"");
            }
            if (!StringUtil.isNullOrEmpty(test.getComparator())) {
                sb.append(" :comparator ").append("\"").append(test.getComparator()).append("\"");
            }
            if (!StringUtil.isNullOrEmpty(test.getMatchType())) {
                sb.append(" :").append(test.getMatchType());
            }
            if (!StringUtil.isNullOrEmpty(test.getHeaderName())) {
                sb.append(" \"").append(test.getHeaderName()).append("\"");
            }
            List<String> headerValues = test.getHeaderValue();
            if (headerValues != null && !headerValues.isEmpty()) {
                if (headerValues.size() > 1) {
                    sb.append(" [");
                }
                boolean first = true;
                for (String value : headerValues) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(" \"").append(value).append("\"");
                }
                if (headerValues.size() > 1) {
                    sb.append(" ]");
                }
            }
            return sb.toString();
        } else if (action instanceof FilterAction.DeleteheaderAction) {
            if (!isAdminScript) {
                throw ServiceException.PARSE_ERROR("Invalid deleteheader action: deleteheader action is not allowed in user scripts", null);
            }
            FilterAction.DeleteheaderAction deleteheaderAction = (FilterAction.DeleteheaderAction) action;
            deleteheaderAction.validateDeleteheaderAction();
            StringBuilder sb  = new StringBuilder();
            sb.append("deleteheader");
            if (deleteheaderAction.getLast() != null && deleteheaderAction.getLast()) {
                sb.append(" :last");
            }
            if (deleteheaderAction.getOffset() != null) {
                sb.append(" :index ").append(deleteheaderAction.getOffset());
            }
            EditheaderTest test = deleteheaderAction.getTest();
            if (test.getCount() != null && test.getCount()) {
                sb.append(" :count");
            } else if (test.getValue() != null && test.getValue()) {
                sb.append(" :value");
            }
            if (!StringUtil.isNullOrEmpty(test.getRelationalComparator())) {
                sb.append(" \"").append(test.getRelationalComparator()).append("\"");
            }
            if (!StringUtil.isNullOrEmpty(test.getComparator())) {
                sb.append(" :comparator ").append("\"").append(test.getComparator()).append("\"");
            }
            if (!StringUtil.isNullOrEmpty(test.getMatchType())) {
                sb.append(" :").append(test.getMatchType());
            }
            if (!StringUtil.isNullOrEmpty(test.getHeaderName())) {
                sb.append(" \"").append(test.getHeaderName()).append("\"");
            }
            List<String> headerValues = test.getHeaderValue();
            if (headerValues != null && !headerValues.isEmpty()) {
                if (headerValues.size() > 1) {
                    sb.append(" [");
                }
                boolean first = true;
                for (String value : headerValues) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(" \"").append(value).append("\"");
                }
                if (headerValues.size() > 1) {
                    sb.append(" ]");
                }
            }
            return sb.toString();
        } else {
            ZimbraLog.soap.debug("Ignoring unexpected action: %s", action);
        }
        return null;
    }

    private static String handleRejectAction(FilterAction.RejectAction rejectAction) throws ServiceException {
        String act = rejectAction instanceof FilterAction.ErejectAction ? "ereject" : "reject";
        if (!StringUtil.isNullOrEmpty(rejectAction.getContent())) {
            StringBuilder sb = new StringBuilder();
            sb.append(act);
            sb.append(" text:\r\n");
            sb.append(rejectAction.getContent());
            sb.append("\r\n.\r\n");
            return sb.toString();
        } else {
            String message = "Empty " + act + " action";
            throw ServiceException.PARSE_ERROR(message, null);
        }
    }

    private static String handleVariables(FilterVariables filterVariables, String indent) throws ServiceException {
        StringBuilder sb = new StringBuilder();
        if (filterVariables != null) {
            List<FilterVariable> variables = filterVariables.getVariables();
            if (variables != null && !variables.isEmpty()) {
                Iterator<FilterVariable> iterator = variables.iterator();
                while(iterator.hasNext()) {
                    FilterVariable filterVariable = iterator.next();
                    String varName = filterVariable.getName();
                    String varValue = filterVariable.getValue();
                    if (!StringUtil.isNullOrEmpty(varName) && varValue != null) {
                            if (indent != null) {
                                sb.append(indent);
                            }
                            sb.append("set \"").append(varName).append("\" \"").append(varValue).append("\"").append(END_OF_LINE);
                    } else {
                        String message = "";
                        if (StringUtil.isNullOrEmpty(varName)) {
                            message = "Filter variable should have a name";
                        } else if (varValue == null) {
                            message = "Filter variable should have a value";
                        }
                        throw ServiceException.PARSE_ERROR(message, null);
                    }
                }
            }
        }
        return sb.toString();
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

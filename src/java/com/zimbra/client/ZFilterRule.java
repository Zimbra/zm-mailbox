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

import com.google.common.collect.Lists;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.client.ZFilterAction.MarkOp;
import com.zimbra.client.ZFilterAction.ZDiscardAction;
import com.zimbra.client.ZFilterAction.ZFileIntoAction;
import com.zimbra.client.ZFilterAction.ZKeepAction;
import com.zimbra.client.ZFilterAction.ZMarkAction;
import com.zimbra.client.ZFilterAction.ZNotifyAction;
import com.zimbra.client.ZFilterAction.ZRedirectAction;
import com.zimbra.client.ZFilterAction.ZReplyAction;
import com.zimbra.client.ZFilterAction.ZStopAction;
import com.zimbra.client.ZFilterAction.ZTagAction;
import com.zimbra.client.ZFilterCondition.AddressBookOp;
import com.zimbra.client.ZFilterCondition.BodyOp;
import com.zimbra.client.ZFilterCondition.ContactRankingOp;
import com.zimbra.client.ZFilterCondition.DateOp;
import com.zimbra.client.ZFilterCondition.HeaderOp;
import com.zimbra.client.ZFilterCondition.MeOp;
import com.zimbra.client.ZFilterCondition.SimpleOp;
import com.zimbra.client.ZFilterCondition.SizeOp;
import com.zimbra.client.ZFilterCondition.ZAddressBookCondition;
import com.zimbra.client.ZFilterCondition.ZAddressCondition;
import com.zimbra.client.ZFilterCondition.ZAttachmentExistsCondition;
import com.zimbra.client.ZFilterCondition.ZBodyCondition;
import com.zimbra.client.ZFilterCondition.ZContactRankingCondition;
import com.zimbra.client.ZFilterCondition.ZCurrentDayOfWeekCondition;
import com.zimbra.client.ZFilterCondition.ZCurrentTimeCondition;
import com.zimbra.client.ZFilterCondition.ZDateCondition;
import com.zimbra.client.ZFilterCondition.ZHeaderCondition;
import com.zimbra.client.ZFilterCondition.ZHeaderExistsCondition;
import com.zimbra.client.ZFilterCondition.ZInviteCondition;
import com.zimbra.client.ZFilterCondition.ZMeCondition;
import com.zimbra.client.ZFilterCondition.ZMimeHeaderCondition;
import com.zimbra.client.ZFilterCondition.ZSizeCondition;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ZFilterRule implements ToZJSONObject {

    private final String name;
    private final boolean active;
    private final boolean allConditions;
    private final List<ZFilterCondition> conditions;
    private final List<ZFilterAction> actions;

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAllConditions() {
        return allConditions;
    }

    public List<ZFilterCondition> getConditions() {
        return conditions;
    }

    public List<ZFilterAction> getActions() {
        return actions;
    }

    public ZFilterRule(String name, boolean active, boolean allConditions,
                       List<ZFilterCondition> conditions, List<ZFilterAction> actions) {
        this.name = name;
        this.active = active;
        this.allConditions = allConditions;
        this.conditions = conditions;
        this.actions = actions;
    }

    public ZFilterRule(FilterRule rule) throws ServiceException {
        this.name = rule.getName();
        this.active = rule.isActive();
        this.allConditions = "allof".equalsIgnoreCase(rule.getFilterTests().getCondition());

        List<FilterTest> tests = Lists.newArrayList(rule.getFilterTests().getTests());
        Collections.sort(tests, new Comparator<FilterTest>() {
            @Override
            public int compare(FilterTest test1, FilterTest test2) {
                return test1.getIndex() - test2.getIndex();
            }
        });
        this.conditions = Lists.newArrayListWithCapacity(tests.size());
        for (FilterTest test : tests) {
            this.conditions.add(ZFilterCondition.of(test));
        }

        List<FilterAction> actions = Lists.newArrayList(rule.getFilterActions());
        Collections.sort(actions, new Comparator<FilterAction>() {
            @Override
            public int compare(FilterAction action1, FilterAction action2) {
                return action1.getIndex() - action2.getIndex();
            }
        });
        this.actions = Lists.newArrayListWithCapacity(actions.size());
        for (FilterAction action : actions) {
            this.actions.add(ZFilterAction.of(action));
        }
    }

    FilterRule toJAXB() {
        FilterRule rule = new FilterRule(name, active);
        FilterTests tests = new FilterTests(allConditions ? "allof" : "anyof");
        int index = 0;
        for (ZFilterCondition condition : conditions) {
            FilterTest test = condition.toJAXB();
            test.setIndex(index++);
            tests.addTest(test);
        }
        rule.setFilterTests(tests);
        index = 0;
        for (ZFilterAction zaction : actions) {
            FilterAction action = zaction.toJAXB();
            action.setIndex(index++);
            rule.addFilterAction(action);
        }
        return rule;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("name", name);
        jo.put("active", active);
        jo.put("allConditions", allConditions);
        jo.put("conditions", conditions);
        jo.put("actions", actions);
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZFilterRule %s]", name);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    static String quotedString(String s) {
        return "\"" + s.replaceAll("\"", "\\\"") + "\"";
    }

    /**
     * generate the human-readable form of a rule that parseFilterRule supports.
     * @return human-readable rule
     */
    public String generateFilterRule() {
        StringBuilder sb = new StringBuilder();
        sb.append(quotedString(name)).append(' ');
        sb.append(active ? "active" : "inactive").append(' ');
        sb.append(allConditions ? "all" : "any").append(' ');
        boolean needSpace = false;
        for (ZFilterCondition cond : conditions) {
            if (needSpace) sb.append(' ');
            sb.append(cond.toConditionString());
            needSpace = true;
        }

        for (ZFilterAction action : actions) {
            if (needSpace) sb.append(' ');
            sb.append(action.toActionString());
            needSpace = true;
        }
        return sb.toString();
    }

    public static ZFilterRule parseFilterRule(String[] args) throws ServiceException {
        String name = args[0];
        boolean all = true;
        boolean active = true;

        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();

        int i = 1;
        while (i < args.length) {
            String a = args[i++];
            if (a.equals("active")) {
                active = true;
            } else if (a.equals("inactive")) {
                active = false;
            } else if (a.equals("any")) {
                all = false;
            } else if (a.equals("all")) {
                all = true;
            } else if (a.equals("addressbook")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String op = args[i++];
                String header = args[i++];
                conditions.add(new ZAddressBookCondition(AddressBookOp.fromString(op), header));
            } else if (a.equals("contact_ranking")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String op = args[i++];
                String header = args[i++];
                conditions.add(new ZContactRankingCondition(ContactRankingOp.fromString(op), header));
            } else if (a.equals("me")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String op = args[i++];
                String header = args[i++];
                conditions.add(new ZMeCondition(MeOp.fromString(op), header));
            } else if (a.equals("attachment")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing exists arg", null);
                }
                conditions.add(new ZAttachmentExistsCondition(args[i++].equals("exists")));
            } else if (a.equals("body")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String op = args[i++];
                String nextArg = args[i++];
                boolean caseSensitive = false;
                if (ZFilterCondition.C_CASE_SENSITIVE.equals(nextArg)) {
                    caseSensitive = true;
                    if (i + 1 > args.length) {
                        throw ZClientException.CLIENT_ERROR("missing args", null);
                    }
                    nextArg = args[i++];
                }
                conditions.add(new ZBodyCondition(BodyOp.fromString(op), caseSensitive, nextArg));
            } else if (a.equals("size")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                conditions.add(new ZSizeCondition(SizeOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("date")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                conditions.add(new ZDateCondition(DateOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("current_time")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                conditions.add(new ZCurrentTimeCondition(DateOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("current_day_of_week")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                conditions.add(new ZCurrentDayOfWeekCondition(SimpleOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("header")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String headerName = args[i++];
                String op = args[i++];
                if (op.equals("exists")) {
                    conditions.add(new ZHeaderExistsCondition(headerName, true));
                } else if (op.equals("not_exists")) {
                    conditions.add(new ZHeaderExistsCondition(headerName, false));
                } else {
                    if (i + 1 > args.length) {
                        throw ZClientException.CLIENT_ERROR("missing args", null);
                    }
                    String nextArg = args[i++];
                    boolean caseSensitive = false;
                    if (ZFilterCondition.C_CASE_SENSITIVE.equals(nextArg)) {
                        caseSensitive = true;
                        if (i + 1 > args.length) {
                            throw ZClientException.CLIENT_ERROR("missing args", null);
                        }
                        nextArg = args[i++];
                    }
                    conditions.add(new ZHeaderCondition(headerName, HeaderOp.fromString(op), caseSensitive, nextArg));
                }
            } else if (a.equals("mime_header")) {
                if (i + 3 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String headerName = args[i++];
                String op = args[i++];
                String nextArg = args[i++];
                boolean caseSensitive = false;
                if (ZFilterCondition.C_CASE_SENSITIVE.equals(nextArg)) {
                    caseSensitive = true;
                    if (i + 1 > args.length) {
                        throw ZClientException.CLIENT_ERROR("missing args", null);
                    }
                    nextArg = args[i++];
                }
                conditions.add(new ZMimeHeaderCondition(headerName, HeaderOp.fromString(op), caseSensitive, nextArg));
            } else if (a.equals("address")) {
                if (i + 4 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String headerName = args[i++];
                String part = args[i++];
                String op = args[i++];
                String nextArg = args[i++];
                boolean caseSensitive = false;
                if (ZFilterCondition.C_CASE_SENSITIVE.equals(nextArg)) {
                    caseSensitive = true;
                    if (i + 1 > args.length) {
                        throw ZClientException.CLIENT_ERROR("missing args", null);
                    }
                    nextArg = args[i++];
                }
                conditions.add(new ZAddressCondition(headerName, Sieve.AddressPart.fromString(part),
                        HeaderOp.fromString(op), caseSensitive, nextArg));
            } else if (a.equals("invite")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing exists arg", null);
                }
                ZInviteCondition cond = new ZInviteCondition(args[i++].equals("exists"));
                if (i + 1 < args.length && args[i].equalsIgnoreCase("method")) {
                    i++; // method
                    cond.setMethods(args[i++].split(","));
                }
                conditions.add(cond);
            } else if (a.equals("conversation")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing where arg", null);
                }
                conditions.add(new ZFilterCondition.ZConversationCondition(
                        ZFilterCondition.ConversationOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("facebook")) {
                ZFilterCondition.SimpleOp op;
                if (i + 1 < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                conditions.add(new ZFilterCondition.ZFacebookCondition(op));
            } else if (a.equals("linkedin")) {
                ZFilterCondition.SimpleOp op;
                if (i + 1 < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                conditions.add(new ZFilterCondition.ZLinkedInCondition(op));
            } else if (a.equals("socialcast")) {
                ZFilterCondition.SimpleOp op;
                if (i + 1 < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                conditions.add(new ZFilterCondition.ZSocialcastCondition(op));
            } else if (a.equals("twitter")) {
                ZFilterCondition.SimpleOp op;
                if (i + 1 < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                conditions.add(new ZFilterCondition.ZTwitterCondition(op));
            } else if (a.equals("list")) {
                ZFilterCondition.SimpleOp op;
                if (i + 1 < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                conditions.add(new ZFilterCondition.ZListCondition(op));
            } else if (a.equals("bulk")) {
                ZFilterCondition.SimpleOp op;
                if (i + 1 < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                conditions.add(new ZFilterCondition.ZBulkCondition(op));
            } else if (a.equals("importance")) {
                if (i + 2 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String op = args[i++];
                String importance = args[i++];
                conditions.add(new ZFilterCondition.ZImportanceCondition(SimpleOp.fromString(op),
                        FilterTest.Importance.fromString(importance)));
            } else if (a.equals("flagged")) {
                ZFilterCondition.SimpleOp op;
                if (i < args.length && args[i].equalsIgnoreCase("not")) {
                    i++; // not
                    op = ZFilterCondition.SimpleOp.NOT_IS;
                } else {
                    op = ZFilterCondition.SimpleOp.IS;
                }
                if (i >= args.length) {
                    throw ZClientException.CLIENT_ERROR("missing a flag name after 'flagged'", null);
                }
                conditions.add(new ZFilterCondition.ZFlaggedCondition(op, Sieve.Flag.fromString(args[i++])));
            } else if (a.equals("keep")) {
                actions.add(new ZKeepAction());
            } else if (a.equals("discard")) {
                actions.add(new ZDiscardAction());
            } else if (a.equals("fileinto")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                actions.add(new ZFileIntoAction(args[i++]));
            } else if (a.equals("tag")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                actions.add(new ZTagAction(args[i++]));
            } else if (a.equals("mark")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                actions.add(new ZMarkAction(MarkOp.fromString(args[i++])));
            } else if (a.equals("redirect")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                actions.add(new ZRedirectAction(args[i++]));
            } else if (a.equals("reply")) {
                if (i + 1 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                actions.add(new ZReplyAction(args[i++]));
            } else if (a.equals("notify")) {
                if (i + 3 > args.length) {
                    throw ZClientException.CLIENT_ERROR("missing args", null);
                }
                String emailAddr = args[i++];
                String subjectTemplate = args[i++];
                String bodyTemplate = args[i++];
                int maxBodyBytes = -1;
                if (i + 1 <= args.length) {
                    try {
                        maxBodyBytes = Integer.valueOf(args[i]);
                        i++;
                    } catch (NumberFormatException ignored) {
                    }
                }
                actions.add(new ZNotifyAction(emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes));
            } else if (a.equals("stop")) {
                actions.add(new ZStopAction());
            } else {
                throw ZClientException.CLIENT_ERROR("unknown keyword: "+a, null);
            }
        }

        if (name == null || name.length() == 0) {
            throw ZClientException.CLIENT_ERROR("missing filter name", null);
        }
        if (actions.isEmpty()) {
            throw ZClientException.CLIENT_ERROR("must have at least one action", null);
        }
        if (conditions.isEmpty()) {
            throw ZClientException.CLIENT_ERROR("must have at least one condition", null);
        }
        return new ZFilterRule(name, active, all, conditions, actions);
    }

}

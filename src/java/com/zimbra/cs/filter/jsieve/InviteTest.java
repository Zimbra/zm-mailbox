/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.filter.jsieve;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessage.CalendarPartInfo;

public class InviteTest extends AbstractTest {

    private static final Set<String> ALL_METHODS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private static final Set<String> ALL_REQUEST_METHODS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private static final Set<String> ALL_REPLY_METHODS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private static final String ARG_SPEC;
    
    private Set<String> mMethods = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    static {
        addMethod(ALL_METHODS, ICalTok.PUBLISH);
        addMethod(ALL_METHODS, ICalTok.REQUEST);
        addMethod(ALL_METHODS, ICalTok.REPLY);
        addMethod(ALL_METHODS, ICalTok.ADD);
        addMethod(ALL_METHODS, ICalTok.CANCEL);
        addMethod(ALL_METHODS, ICalTok.REFRESH);
        addMethod(ALL_METHODS, ICalTok.COUNTER);
        addMethod(ALL_METHODS, ICalTok.DECLINECOUNTER);
        ALL_METHODS.add("anyrequest");
        ALL_METHODS.add("anyreply");

        addMethod(ALL_REQUEST_METHODS, ICalTok.PUBLISH);
        addMethod(ALL_REQUEST_METHODS, ICalTok.REQUEST);
        addMethod(ALL_REPLY_METHODS, ICalTok.REPLY);
        addMethod(ALL_REQUEST_METHODS, ICalTok.ADD);
        addMethod(ALL_REQUEST_METHODS, ICalTok.CANCEL);
        addMethod(ALL_REPLY_METHODS, ICalTok.REFRESH);
        addMethod(ALL_REPLY_METHODS, ICalTok.COUNTER);
        addMethod(ALL_REQUEST_METHODS, ICalTok.DECLINECOUNTER);
        
        ARG_SPEC = ":method [" + StringUtil.join("|", ALL_METHODS) + ", ...]";
    }
    
    private static void addMethod(Set<String> set, ICalTok method) {
        set.add(method.toString().toLowerCase());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
    throws SieveException {
        List<Argument> argList = arguments.getArgumentList();
        if (ListUtil.isEmpty(argList)) {
            // Method not specified.  Match all invite requests and replies.
            return;
        }
        
        if (argList.size() != 2) {
            validationError(argList);
        }
        Object arg1 = argList.get(0).getValue();
        Object arg2 = argList.get(1).getValue();
        
        if (!(arg1 instanceof String)) {
            validationError(argList);
        }
        if (!((String) arg1).equals(":method")) {
            validationError(argList);
        }
        if (!(arg2 instanceof List)) {
            validationError(argList);
        }
        if (((List) arg2).isEmpty()) {
            validationError(argList);
        }
        for (String methodArg : (List<String>) arg2) {
            if (!ALL_METHODS.contains(methodArg)) {
                validationError(argList);
            }
            mMethods.add(methodArg);
        }
    }
    
    private void validationError(List<Argument> argList)
    throws SieveException {
        throw new SieveException(
            "Invalid arguments to the invite test.  Expected " + ARG_SPEC + ", got " + argList);
    }

    /**
     * Returns <tt>true</tt> if the message has a calendar part.
     */
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context) {
        if (!(mail instanceof ZimbraMailAdapter))
            return false;
        ParsedMessage pm = ((ZimbraMailAdapter) mail).getParsedMessage();
        if (pm == null) {
            return false;
        }
        CalendarPartInfo calPart = pm.getCalendarPartInfo();
        if (calPart != null) {
            if (mMethods.isEmpty()) {
                // Test matches any invite.
                return true;
            }
            if (calPart.method == null) {
                // Method not specified in the calendar part.
                return false;
            }
            if (mMethods.contains(calPart.method.toString())) {
                // Calendar part method matches one of the methods in the test.
                return true;
            }
            if (mMethods.contains("anyrequest") && isRequest(calPart.method)) {
                return true;
            }
            if (mMethods.contains("anyreply") && isReply(calPart.method)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isRequest(ICalTok method) {
        if (method != null && ALL_REQUEST_METHODS.contains(method.toString())) {
            return true;
        }
        return false;
    }
    
    private static boolean isReply(ICalTok method) {
        if (method != null && ALL_REPLY_METHODS.contains(method.toString())) {
            return true;
        }
        return false;
    }
}

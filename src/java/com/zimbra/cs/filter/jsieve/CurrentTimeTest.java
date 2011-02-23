/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraSieveException;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;

public class CurrentTimeTest extends AbstractTest {

    static final String BEFORE = ":before";
    static final String AFTER = ":after";

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter))
            return false;

        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();

        // First argument MUST be a tag of ":before" or ":after"
        String comparator = null;
        if (argumentsIter.hasNext()) {
            Object argument = argumentsIter.next();
            if (argument instanceof TagArgument) {
                String tag = ((TagArgument) argument).getTag();
                if (tag.equals(BEFORE) || tag.equals(AFTER))
                    comparator = tag;
                else
                    throw new SyntaxException("Found unexpected: \"" + tag + "\"");
            }
        }
        if (comparator == null)
            throw new SyntaxException("Expecting \"" + BEFORE + "\" or \"" + AFTER + "\"");

        // Second argument MUST be a time in "HHmm" format
        DateFormat timeFormat = new SimpleDateFormat("HHmm");
        TimeZone accountTimeZone;
        try {
            accountTimeZone = ICalTimeZone.getAccountTimeZone(((ZimbraMailAdapter) mail).getMailbox().getAccount());
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        }
        timeFormat.setTimeZone(accountTimeZone);
        Date timeArg = null;
        if (argumentsIter.hasNext()) {
            Object argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                List<String> valList = ((StringListArgument) argument).getList();
                if (valList.size() != 1)
                    throw new SyntaxException("Expecting exactly one time value");
                String timeStr = valList.get(0);
                try {
                    timeArg = timeFormat.parse(timeStr);
                } catch (ParseException e) {
                    throw new SyntaxException(e);
                }
            }
        }
        if (timeArg == null)
            throw new SyntaxException("Expecting a time value");

        // There MUST NOT be any further arguments
        if (argumentsIter.hasNext())
            throw new SyntaxException("Found unexpected argument(s)");

        Calendar rightNow = Calendar.getInstance(accountTimeZone);

        Calendar timeToCompareWith = Calendar.getInstance(accountTimeZone);
        // set the time part
        timeToCompareWith.setTime(timeArg);
        // now set the right date
        timeToCompareWith.set(
                rightNow.get(Calendar.YEAR), rightNow.get(Calendar.MONTH), rightNow.get(Calendar.DAY_OF_MONTH));

        return BEFORE.equals(comparator) ?
                rightNow.getTime().before(timeToCompareWith.getTime()) :
                rightNow.getTime().after(timeToCompareWith.getTime());
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) {
        // override validation -- it's already done in executeBasic above
    }
}

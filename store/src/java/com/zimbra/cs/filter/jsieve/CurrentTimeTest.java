/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraSieveException;
import com.zimbra.cs.mailbox.calendar.Util;

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
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }

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
            accountTimeZone = Util.getAccountTimeZone(((ZimbraMailAdapter) mail).getMailbox().getAccount());
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

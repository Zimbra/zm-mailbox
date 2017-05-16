/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import java.net.URL;
import java.util.List;
import java.util.ListIterator;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

public class ValidNotifyMethodTest extends AbstractTest {

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context)
            throws SieveException {
        List<String> notificationUris = null;

        /*
         * Usage:  valid_notify_method <notification-uris: string-list>
         */
        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();

        if (argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                notificationUris = ((StringListArgument) argument).getList();
            }
        }
        if (null == notificationUris) {
            throw context.getCoordinate().syntaxException("Expecting a StringList of notification-uris");
        }
        return test(notificationUris);
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) {
        // override validation -- it's already done in executeBasic above
    }

    private boolean test(List<String> notificationUris) {
        for (final String uri: notificationUris) {
            try {
                URL url = new URL(uri);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}

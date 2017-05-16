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

package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.NumberArgument;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.JsieveConfigMapHandler;

import java.util.List;

public class Notify extends AbstractActionCommand {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        List<Argument> args = arguments.getArgumentList();
        if (args.size() < 3)
            throw new SyntaxException("Missing arguments");

        Argument nextArg = args.get(0);
        if (!(nextArg instanceof StringListArgument))
            throw new SyntaxException("Expected string");
        List<String> list = ((StringListArgument) nextArg).getList();
        if (list.size() != 1)
            throw new SyntaxException("Expected exactly one email address");
        String emailAddr = list.get(0);

        nextArg = args.get(1);
        if (!(nextArg instanceof StringListArgument))
            throw new SyntaxException("Expected string");
        list = ((StringListArgument) nextArg).getList();
        if (list.size() != 1)
            throw new SyntaxException("Expected exactly one subject");
        String subjectTemplate = list.get(0);

        nextArg = args.get(2);
        if (!(nextArg instanceof StringListArgument))
            throw new SyntaxException("Expected string");
        list = ((StringListArgument) nextArg).getList();
        if (list.size() != 1)
            throw new SyntaxException("Expected exactly one body");
        String bodyTemplate = list.get(0);

        int maxBodyBytes = -1;
        List<String> origHeaders = null;
        if (args.size() == 4) {
            nextArg = args.get(3);
            if (nextArg instanceof NumberArgument)
                maxBodyBytes = ((NumberArgument) nextArg).getInteger();
            else if (nextArg instanceof StringListArgument)
                origHeaders = ((StringListArgument) nextArg).getList();
            else
                throw new SyntaxException("Invalid argument");
        }

        if (args.size() == 5) {
            nextArg = args.get(3);
            if (!(nextArg instanceof NumberArgument))
                throw new SyntaxException("Expected int");
            maxBodyBytes = ((NumberArgument) nextArg).getInteger();
            nextArg = args.get(4);
            if (!(nextArg instanceof StringListArgument))
                throw new SyntaxException("Expected string list");
            origHeaders = ((StringListArgument) nextArg).getList();
        }

        mail.addAction(new ActionNotify(emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes, origHeaders));
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        // done in executeBasic()
    }
}

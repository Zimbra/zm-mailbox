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
        if (args.size() == 4) {
            nextArg = args.get(3);
            if (!(nextArg instanceof NumberArgument))
                throw new SyntaxException("Expected int");
            maxBodyBytes = ((NumberArgument) nextArg).getInteger();
        }

        mail.addAction(new ActionNotify(emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes));
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        // done in executeBasic()
    }
}

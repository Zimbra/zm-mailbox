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
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import java.util.List;

public class Reply extends AbstractActionCommand {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        String bodyTemplate = ((StringListArgument) arguments.getArgumentList().get(0)).getList().get(0);
        mail.addAction(new ActionReply(bodyTemplate));
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        List<Argument> args = arguments.getArgumentList();
        if (args.size() != 1)
            throw new SyntaxException("Exactly 1 argument permitted. Found " + args.size());

        Argument argument = args.get(0);
        if (!(argument instanceof StringListArgument))
            throw new SyntaxException("Expected text");

        if (((StringListArgument) argument).getList().size() != 1)
            throw new SyntaxException("Expected exactly one text");
    }
}

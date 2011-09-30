/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.mail.MailAdapter;

/**
 * @since Nov 8, 2004
 */
public final class Flag extends AbstractActionCommand {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments args, Block arg2, SieveContext context) {
        String flagName = ((StringListArgument) args.getArgumentList().get(0)).getList().get(0);
        ActionFlag action = ActionFlag.of(flagName);
        mail.addAction(action);
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
        List<Argument> args = arguments.getArgumentList();
        if (args.size() != 1) {
            throw new SyntaxException("Exactly 1 argument permitted. Found " + args.size());
        }
        Object argument = args.get(0);
        if (!(argument instanceof StringListArgument)) {
            throw new SyntaxException("Expecting a string-list");
        }
        List<String> strList = ((StringListArgument) argument).getList();
        if (1 != strList.size()) {
            throw new SyntaxException("Expecting exactly one argument");
        }
        String flagName = strList.get(0);
        if (ActionFlag.of(flagName.toLowerCase()) == null) {
            throw new SyntaxException("Invalid flag: " + flagName);
        }
    }
}

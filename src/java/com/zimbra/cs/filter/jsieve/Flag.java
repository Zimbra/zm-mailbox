/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 8, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.SyntaxException;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.mail.MailAdapter;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Flag extends AbstractActionCommand {

    private static Map FLAGS = new HashMap(7);
    static {
        FLAGS.put("read", new ActionFlag(com.zimbra.cs.mailbox.Flag.ID_FLAG_UNREAD, false, "read"));
        FLAGS.put("unread", new ActionFlag(com.zimbra.cs.mailbox.Flag.ID_FLAG_UNREAD, true, "unread"));
        FLAGS.put("flagged", new ActionFlag(com.zimbra.cs.mailbox.Flag.ID_FLAG_FLAGGED, true, "flagged"));
        FLAGS.put("unflagged", new ActionFlag(com.zimbra.cs.mailbox.Flag.ID_FLAG_FLAGGED, false, "unflagged"));
    }
    /* (non-Javadoc)
     * @see org.apache.jsieve.commands.AbstractCommand#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments, org.apache.jsieve.Block)
     */
    protected Object executeBasic(MailAdapter mail, Arguments args, Block arg2)
            throws SieveException {
        // TODO Auto-generated method stub
        String flagName =
            (String) ((StringListArgument) args.getArgumentList().get(0))
                .getList().get(0);
        ActionFlag action = (ActionFlag) FLAGS.get(flagName);
        mail.addAction(action);

        return null;
    }

    protected void validateArguments(Arguments arguments) throws SieveException
    {
        List args = arguments.getArgumentList();
        if (args.size() != 1)
            throw new SyntaxException(
                "Exactly 1 argument permitted. Found " + args.size());

        Object argument = args.get(0);
        if (!(argument instanceof StringListArgument))
            throw new SyntaxException("Expecting a string-list");

        List strList = ((StringListArgument) argument).getList();
        if (1 != strList.size())
            throw new SyntaxException("Expecting exactly one argument");
        String flagName = (String) strList.get(0);
        if (! FLAGS.containsKey(flagName.toLowerCase()))
            throw new SyntaxException("Invalid flag: " + flagName);
        
    }
}

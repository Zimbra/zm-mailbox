/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.extensions;

import java.util.List;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.filter.JsieveConfigMapHandler;
import com.zimbra.cs.filter.jsieve.ActionTag;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

import org.apache.jsieve.Argument;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SyntaxException;

/**
 * Dummy custom action extension for testing.
 *
 */
public class DummyCustomTag extends AbstractActionCommand implements ZimbraExtension {
    private static boolean initialized = false;
    private static boolean executed = false;
    private static boolean inactivated = false;

    public String getName() {
        return "tag";
    }

    public void init() {
        //if(!inactivated){
            ZimbraLog.extensions.info("init()");
            //JsieveConfigMapHandler.registerCommand("tag", this.getClass().getName());
            initialized = true;
        //}
    }

    public void destroy() {
        // after RuleManagerWithCustomActionFilterTest cases done, this class's class loader
        // is still active. So if some test execute ExtensionUtil.initAll, this class's init()
        // could be called again. So to be sure for JsieveConfigMapHandler.registerCommand() not to be called then,
        // have inactivated flag to be true here.
        inactivated = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isExecuted() {
        return executed;
    }

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        ZimbraLog.extensions.info("executeBasic()");
        mail.addAction(new ActionTag("zimbra"));
        executed = true;
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException
    {
        @SuppressWarnings("unchecked")
        List<Argument> args = arguments.getArgumentList();
        if (args.size() != 1)
            throw new SyntaxException(
                    "Exactly 1 argument permitted. Found " + args.size());

        Object argument = args.get(0);
        if (!(argument instanceof StringListArgument))
            throw new SyntaxException("Expecting a string-list");

        if (1 != ((StringListArgument) argument).getList().size())
            throw new SyntaxException("Expecting exactly one argument");
    }

}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * SIEVE command for controlling the Variables functionality
 * <p>
 * Internal-use-only built-in command for resetting the variable data.
 * <p>
 * {@code zimbravariablesctrl [:reset]}
 * <ul>
 *  <li>{@code :reset} Reset the variable data which is stored in the
 *  ZimbraMailAdapter object. Both name-based variable ("${abc}") and
 *  number-based variable ("${1}") will be reset.
 * </ul>
 */
public class ZimbraVariablesCtrl extends AbstractCommand {
    static final String RESET = ":reset";

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;

        for (Argument arg: arguments.getArgumentList()) {
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                String tagValue = tag.getTag();
                if (RESET.equalsIgnoreCase(tagValue)) {
                    mailAdapter.resetValues();
                } else {
                    throw new SyntaxException("Invalid tag: [" + tagValue + "]");
                }
            }
        }
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
    throws SieveException
    {
        List<Argument> args = arguments.getArgumentList();
        if (args.size() > 1) {
            throw new SyntaxException(
                "More than one argument found (" + args.size() + ")");
        }

        for (Argument arg: arguments.getArgumentList()) {
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                String tagValue = tag.getTag();
                if (!RESET.equalsIgnoreCase(tagValue)) {
                    throw new SyntaxException("Invalid tag: [" + tagValue + "]");
                }
            } else {
                if (arg instanceof StringListArgument) {
                    String argument = ((StringListArgument) arg).getList().get(0);
                    throw new SyntaxException("Invalid argument: [" + argument + "]");
                } else {
                    throw new SyntaxException("Invalid argument");
                }
            }
        }
    }
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016, 2017 Synacor, Inc.
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
 * Created on Nov 1, 2004
 *
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

import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class Tag extends AbstractActionCommand {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments args, Block block, SieveContext context) throws SyntaxException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }
        String tagName =
            (String) ((StringListArgument) args.getArgumentList().get(0))
                .getList().get(0);

        // Only one tag with the same tag name allowed, others should be
        // discarded?   
        tagName = FilterUtil.replaceVariables((ZimbraMailAdapter) mail, tagName);
        mail.addAction(new ActionTag(tagName));

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

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.util.Iterator;
import java.util.Set;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.google.common.collect.ImmutableSet;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * SIEVE test whether or not the message is flagged in a SIEVE rule that was examined before the current SIEVE rule.
 * <p>
 * For example, given a SIEVE rule set:
 * <pre>
 *  (1) if socialcast { flag "priority"; }
 *  (2) if me :in "To" { tag "me"; }
 *  (3) if flagged "priority" { stop; }
 *  (4) if bulk { fileinto "bulk"; }
 * <pre>
 * A message that matches (1) is still examined by (2). But, once the message is flagged as "priority" in (1), (4) is
 * not examined because (3) stops further processing.
 *
 * @author ysasaki
 */
public final class FlaggedTest extends AbstractTest {

    private Set<String> flags;

    @Override
    protected void validateArguments(Arguments args, SieveContext ctx) throws SieveException {
        Iterator<Argument> itr = args.getArgumentList().iterator();
        if (itr.hasNext()) {
            Argument arg = itr.next();
            if (arg instanceof StringListArgument) {
                flags = ImmutableSet.copyOf(((StringListArgument) arg).getList());
            } else {
                throw ctx.getCoordinate().syntaxException("Unexpected argument: " + arg.getValue());
            }
        } else {
            throw ctx.getCoordinate().syntaxException("No flag names specified");
        }
    }

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        assert(flags != null);
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;
        for (Action action : adapter.getActions()) {
            if (action instanceof ActionFlag) {
                ActionFlag flag = (ActionFlag) action;
                if (flag.isSetFlag() && flags.contains(flag.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}

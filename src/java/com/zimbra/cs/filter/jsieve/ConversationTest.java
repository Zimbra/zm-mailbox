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
import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraSieveException;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * SIEVE test whether the conversation which the message is about to belong to is what the user started or has
 * participated in. Conversations the user started are for which the oldest remaining message sorted by (date, id) has
 * FROM_ME flag AND it's not a reply. Conversations the user has participated are for which there is at least one
 * message with FROM_ME flag. These are all based on what the user currently has in its mailbox. Deleted messages are
 * irrelevant.
 *
 * @author ysasaki
 */
public final class ConversationTest extends AbstractTest {

    private enum Where {
        STARTED, PARTICIPATED;
        static final String TAG = ":where";
    }

    private Where where = Where.STARTED; // default

    @Override
    protected void validateArguments(Arguments args, SieveContext ctx) throws SieveException {
        Iterator<Argument> itr = args.getArgumentList().iterator();
        while (itr.hasNext()) {
            Argument arg = itr.next();
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                if (tag.is(Where.TAG)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument list = (StringListArgument) arg;
                            if (list.getList().size() == 1) {
                                String value = list.getList().get(0);
                                try {
                                    where = Where.valueOf(value.toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    throw ctx.getCoordinate().syntaxException("Unknown where: " + value);
                                }
                            } else {
                                throw ctx.getCoordinate().syntaxException("Too many where: " + list.getList());
                            }
                        } else {
                            throw ctx.getCoordinate().syntaxException(Where.TAG + " is missing an argument");
                        }
                    } else {
                        throw ctx.getCoordinate().syntaxException(Where.TAG + " is missing an argument");
                    }
                } else {
                    throw ctx.getCoordinate().syntaxException("Unknown tag: " + tag.getTag());
                }
            } else {
                throw ctx.getCoordinate().syntaxException("Unexpected argument: " + arg.getValue());
            }
        }
    }

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;
        Mailbox mbox = adapter.getMailbox();
        List<Conversation> convs;
        try {
            convs = mbox.lookupConversation(adapter.getParsedMessage());
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        }
        if (convs.isEmpty()) {
            return false;
        }
        switch (where) {
            case STARTED:
                for (Conversation conv : convs) {
                    if ((conv.getFlagBitmask() & Flag.BITMASK_FROM_ME) > 0) {
                        try {
                            List<Message> msgs = mbox.getMessagesByConversation(null, conv.getId(), SortBy.DATE_ASC, 1);
                            if (!msgs.isEmpty()) {
                                Message msg = msgs.get(0);
                                // the oldest message in the conversation is FROM_ME, but conversation can't be started
                                // by REPLY, which is likely that the user has deleted messages older than the remaining
                                // oldest.
                                if ((msg.getFlagBitmask() & Flag.BITMASK_FROM_ME) > 0 &&
                                        !ParsedMessage.isReply(msgs.get(0).getSubject())) {
                                    return true;
                                }
                            }
                        } catch (ServiceException e) {
                            throw new ZimbraSieveException(e);
                        }
                    }
                }
                break;
            case PARTICIPATED:
                for (Conversation conv : convs) {
                    if ((conv.getFlagBitmask() & Flag.BITMASK_FROM_ME) > 0) {
                        return true;
                    }
                }
                break;
            default:
                assert false : where;
        }
        return false;
    }

}

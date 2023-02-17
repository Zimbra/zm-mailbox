/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016, 2017, 2023 Synacor, Inc.
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

import java.util.Iterator;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.extensions.Log;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mime.Mime;
import com.zimbra.soap.mail.type.FilterAction;

import javax.mail.internet.MimeMessage;

public class VariableLog extends Log {
    private ZimbraMailAdapter mailAdapter = null;

    @Override 
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block,
            SieveContext context) throws SieveException {

        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }

        this.mailAdapter = (ZimbraMailAdapter) mail;
        MimeMessage mm = mailAdapter.getMimeMessage();
        if (LC.lmtp_extended_logs_enabled.booleanValue()) {
            Iterator<Argument> itr = arguments.getArgumentList().iterator();
            while (itr.hasNext()) {
                Argument arg = itr.next();
                ZimbraLog.filter.info(
                        "Log: " + arg + FilterUtil.getExtendedInfo(mm));
            }
        }
        return super.executeBasic(mail, arguments, block, context);

    }

    @Override
    protected void log(String logLevel, String message, SieveContext context) throws SyntaxException {
        message = FilterUtil.replaceVariables(mailAdapter, message);
        super.log(logLevel, message, context);
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
        if (arguments.getArgumentList().size() > 2) {
            throw new SyntaxException("Log: maximum 2 parameters allowed with Log");
        }
        boolean foundTagArg = false;
        int index = 0;
        Iterator<Argument> itr = arguments.getArgumentList().iterator();
        while (itr.hasNext()) {
            Argument arg = itr.next();
            index++;
            if (arg instanceof TagArgument) {
                if (foundTagArg) {
                    throw new SyntaxException("Log: Multiple log levels are not allowed.");
                }
                if (index > 1) {
                    throw new SyntaxException("Log: Log level must be mentioned before log message.");
                }
                TagArgument tag = (TagArgument) arg;
                if (!(tag.is(":" + FilterAction.LogAction.LogLevel.fatal.toString())
                        || tag.is(":" + FilterAction.LogAction.LogLevel.error.toString())
                        || tag.is(":" + FilterAction.LogAction.LogLevel.warn.toString())
                        || tag.is(":" + FilterAction.LogAction.LogLevel.info.toString())
                        || tag.is(":" + FilterAction.LogAction.LogLevel.debug.toString())
                        || tag.is(":" + FilterAction.LogAction.LogLevel.trace.toString())
                        )) {
                    throw new SyntaxException("Log: Invalid log level provided - " + tag.getTag());
                }
                foundTagArg = true;
            }
            if (index > 1 && !foundTagArg) {
                throw new SyntaxException("Log: Only 1 text message allowed with log statement.");
            }
        }
        ZimbraLog.filter.debug("Log: Validation successfful");
    }
}

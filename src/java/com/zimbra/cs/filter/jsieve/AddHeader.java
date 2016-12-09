/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.exception.OperationException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class AddHeader extends AbstractCommand {
    private static final String LAST = ":last";
    private String headerName = null;
    private String headerValue = null;
    private boolean last = Boolean.FALSE;

    @SuppressWarnings("unchecked")
    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments,
            Block block, SieveContext context) throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }

        Iterator<Argument> itr = arguments.getArgumentList().iterator();
        if (arguments.getArgumentList().size() == 2 || arguments.getArgumentList().size() == 3) {
            Argument arg = itr.next();
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                if (tag.is(LAST)) {
                    last = Boolean.TRUE;
                    arg = itr.next();
                } else {
                    throw new SyntaxException("Invalid argument with addheader.");
                }
            }

            if (arg instanceof StringListArgument) {
                StringListArgument sla = (StringListArgument) arg;
                headerName = sla.getList().get(0);
            } else {
                throw new SyntaxException("Invalid argument with addheader.");
            }

            if (itr.hasNext()) {
                arg = itr.next();
                if (arg instanceof StringListArgument) {
                    StringListArgument sla = (StringListArgument) arg;
                    headerValue = sla.getList().get(0);
                } else {
                    throw new SyntaxException("Invalid argument with addheader.");
                }
            } else {
                throw new SyntaxException("Invalid Number of arguments with addheader.");
            }

        } else {
            throw new SyntaxException("Invalid Number of arguments with addheader.");
        }

        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        headerName = FilterUtil.replaceVariables(mailAdapter.getVariables(), mailAdapter.getMatchedValues(), headerName);
        headerValue = FilterUtil.replaceVariables(mailAdapter.getVariables(), mailAdapter.getMatchedValues(), headerValue);
        try {
            headerValue = MimeUtility.fold(headerName.length() + 2, MimeUtility.encodeText(headerValue));
        } catch (UnsupportedEncodingException uee) {
            throw new OperationException("addheader: Error occured while encoding header value.", uee);
        }

        validateArguments(arguments, context);

        MimeMessage mm = mailAdapter.getMimeMessage();

        if (headerName != null && headerValue != null) {
            try {
                if (last) {
                    mm.addHeader(headerName,headerValue);
                } else {
                    List<Header> headerList = new ArrayList<Header>();
                    headerList.add(new Header(headerName,headerValue));
                    for (Enumeration<Header> e = mm.getAllHeaders(); e.hasMoreElements();) {
                        Header temp = e.nextElement();
                        mm.removeHeader(temp.getName());
                        headerList.add(temp);
                    }

                    for (Header header : headerList) {
                        mm.addHeader(header.getName(), header.getValue());
                    }
                }

                mm.saveChanges();
                mailAdapter.updateIncomingBlob();
                ZimbraLog.filter.info("New header is added in mime with name: %s and value: %s", headerName, headerValue);
            } catch (MessagingException e) {
                throw new OperationException("Error occured while adding new header in mime.", e);
            }
            return null;
        }

        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        if (headerName != null) {
            if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(headerName, CharsetUtil.US_ASCII))) {
                throw new SyntaxException("AddHeader:Header name must be printable ASCII only.");
            }
        }
    }
}

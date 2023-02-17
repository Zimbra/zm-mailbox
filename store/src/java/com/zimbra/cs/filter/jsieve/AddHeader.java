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

import static com.zimbra.cs.filter.JsieveConfigMapHandler.CAPABILITY_EDITHEADER;

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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter.PARSESTATUS;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeUtil;


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

        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        Require.checkCapability(mailAdapter, CAPABILITY_EDITHEADER);
        if (!mailAdapter.getAccount().isSieveEditHeaderEnabled()) {
            mailAdapter.setAddHeaderPresent(true);
            return null;
        }
        headerName = FilterUtil.replaceVariables(mailAdapter, headerName);
        FilterUtil.headerNameHasSpace(headerName);
        // make sure zcs do not add immutable header
        if (EditHeaderExtension.isImmutableHeaderKey(headerName, mailAdapter)) {
            ZimbraLog.filter.info("addheader: %s is immutable header, so exiting silently.", headerName);
            return null;
        }

        if (mailAdapter.getEditHeaderParseStatus() == PARSESTATUS.MIMEMALFORMED) {
            ZimbraLog.filter.debug("addheader: Triggering message is malformed MIME");
            return null;
        }

        if(mailAdapter.cloneParsedMessage()) {
            ZimbraLog.filter.debug("addheader: failed to clone parsed message, so exiting silently.");
            return null;
        }

        headerValue = FilterUtil.replaceVariables(mailAdapter, headerValue);
        try {
            headerValue = MimeUtility.fold(headerName.length() + 2, MimeUtil.encodeWord(headerValue, null, null, true));
        } catch (UnsupportedEncodingException uee) {
            throw new OperationException("addheader: Error occured while encoding header value.", uee);
        }

        MimeMessage mm = mailAdapter.getMimeMessage();
        if (headerName != null && headerValue != null) {
            try {
                if (last) {
                    mm.addHeaderLine(headerName + ": " + headerValue);
                } else {
                    List<Header> headerList = new ArrayList<Header>();
                    Enumeration<Header> e = mm.getAllHeaders();
                    // If the first line of the header is "Return-Path",
                    // keep it at the first line.
                    if (e.hasMoreElements()) {
                        Header temp = e.nextElement();
                        if ("Return-Path".equalsIgnoreCase(temp.getName())) {
                            mm.removeHeader(temp.getName());
                            headerList.add(temp);
                        } else {
                            // reset the iterator (push back the first item of the headers list)
                            e = mm.getAllHeaders();
                        }
                    }
                    headerList.add(new Header(headerName,headerValue));
                    while (e.hasMoreElements()) {
                        Header temp = e.nextElement();
                        mm.removeHeader(temp.getName());
                        headerList.add(temp);
                    }

                    for (Header header : headerList) {
                        mm.addHeaderLine(header.getName() + ": " + header.getValue());
                    }
                }
                EditHeaderExtension.saveChanges(mailAdapter, "addheader", mm);
                mailAdapter.updateIncomingBlob();
                if (LC.lmtp_extended_logs_enabled.booleanValue()) {
                    ZimbraLog.filter.info(
                            "addheader: name=%s value=%s", headerName, headerValue
                            + FilterUtil.getExtendedInfo(mm));
                } else {
                    ZimbraLog.filter.info(
                            "addheader: New header is added in mime with name: %s and value: %s",
                            headerName, headerValue);
                }
            } catch (MessagingException e) {
                throw new OperationException("addheader: Error occured while adding new header in mime.", e);
            }
            return null;
        }

        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        Iterator<Argument> itr = arguments.getArgumentList().iterator();
        if (arguments.getArgumentList().size() == 2 || arguments.getArgumentList().size() == 3) {
            Argument arg = itr.next();
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                if (tag.is(LAST)) {
                    last = Boolean.TRUE;
                    arg = itr.next();
                } else {
                    throw new SyntaxException("addheader: Invalid argument with addheader.");
                }
            }

            if (arg instanceof StringListArgument) {
                StringListArgument sla = (StringListArgument) arg;
                headerName = sla.getList().get(0);
            } else {
                throw new SyntaxException("addheader: Invalid argument with addheader.");
            }

            if (itr.hasNext()) {
                arg = itr.next();
                if (arg instanceof StringListArgument) {
                    StringListArgument sla = (StringListArgument) arg;
                    headerValue = sla.getList().get(0);
                } else {
                    throw new SyntaxException("addheader: Invalid argument with addheader.");
                }
            } else {
                throw new SyntaxException("addheader: Invalid Number of arguments with addheader.");
            }

        } else {
            throw new SyntaxException("addheader: Invalid Number of arguments with addheader.");
        }

        if (!StringUtil.isNullOrEmpty(headerName)) {
            if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(headerName, CharsetUtil.US_ASCII))) {
                throw new SyntaxException("addheader: Header name must be printable ASCII only.");
            }
        } else {
            throw new SyntaxException("addheader: Header name must be present.");
        }
    }
}

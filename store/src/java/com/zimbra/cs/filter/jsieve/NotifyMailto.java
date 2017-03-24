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

import org.apache.commons.lang.StringUtils;
import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;


/**
 * Class NotifyMailto implements the Notify Command as defined in
 * RFC 5435 (notify action) and RFC 5436 (mailto method).
 * This 'notify' action will be turned on only when the global
 * configuration zimbraMailSieveNotifyActionRFCCompliant is set
 * to TRUE.
 */
public class NotifyMailto extends AbstractActionCommand {

    public static final String NOTIFY_FROM = ":from";
    public static final String NOTIFY_IMPORTANCE = ":importance";
    public static final String NOTIFY_OPTIONS = ":options";
    public static final String NOTIFY_MESSAGE = ":message";
    public static final String NOTIFY_METHOD_MAILTO = "mailto:";

    /**
     * Parses the arguments of the 'notify' action, and adds
     * an ActionNotifyMailto to the List of Actions.
     */
    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        Map<String, String> variables = mailAdapter.getVariables();
        List<String> matchedVariables = mailAdapter.getMatchedValues();

        /*
         * RFC 5435 3.1. Notify Action
         * usage: notify [":from" string]
         *        [":importance" <"1" / "2" / "3">]
         *        [":options" string-list]
         *        [":message" string]
         *        <method: string>
         */

        String from = null;
        int importance = 0;
        String message = null;
        String method = null;
        String mailto = null;

        Map<String, String> options = null;
        Map<String, List<String>> mailtoParams = null;

        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();
        boolean stop = false;

        // Tag processing
        while (!stop && argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof TagArgument) {
                final String tag = ((TagArgument) argument).getTag();
                if (from == null && NOTIFY_FROM.equals(tag)) {
                    // The next argument must be a string
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            List<String> stringList = ((StringListArgument) argument).getList();
                            if (stringList.size() != 1) {
                                throw new SyntaxException("Expecting exactly one String for " + NOTIFY_FROM);
                            }
                            String email = FilterUtil.replaceVariables(mailAdapter, (String) stringList.get(0));
                            // Validate email address using javax.mail.internet.InternetAddress
                            try {
                                InternetAddress addr = new InternetAddress(email);
                                addr.validate();
                                from = email;
                            } catch (AddressException ex) {
                                // if the :from addr is not valid, the FilterUtil.notifyMailto() method takes
                                // care of the From header before composing the notification message.
                                ZimbraLog.filter.info("The value of the \":from\" [" + email + "] is not valid");
                            }
                        } else {
                            throw new SyntaxException("Expecting a StringList for " + NOTIFY_FROM);
                        }
                    } else {
                        throw new SyntaxException("Expecting a parameter for " + NOTIFY_FROM);
                    }
                } else if (importance == 0 && NOTIFY_IMPORTANCE.equals(tag)) {
                    // The next argument must be a number
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            List<String> stringList = ((StringListArgument) argument).getList();
                            if (stringList.size() != 1) {
                                throw new SyntaxException("Expecting exactly one String for " + NOTIFY_IMPORTANCE);
                            }
                            String strImportance = (String) stringList.get(0);
                            FilterUtil.replaceVariables(mailAdapter, strImportance);
                            importance = Integer.parseInt(strImportance);
                            if (!(importance  == 1 || importance == 2 || importance == 3)) {
                                throw new SyntaxException("Expecting an integer number (1, 2, 3) for " + NOTIFY_IMPORTANCE);
                            }
                        } else {
                            throw new SyntaxException("Expecting a StringList for " + NOTIFY_IMPORTANCE);
                        }
                    } else {
                        throw new SyntaxException("Expecting a parameter for " + NOTIFY_IMPORTANCE);
                    }
                } else if (options == null && NOTIFY_OPTIONS.equals(tag)) {
                    // The next argument must be a string-list of options "<optionname>=<value>[,<optionname>=<value]*"
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            List<String> listOptions = ((StringListArgument) argument).getList();
                            if (listOptions.size() == 0) {
                                throw new SyntaxException("Expecting exactly one String for " + NOTIFY_OPTIONS);
                            }
                            options = new HashMap<String, String>();
                            for (String option : listOptions) {
                                String[] token = option.split("=");
                                String key = null;
                                String value = null;
                                if (token.length == 2) {
                                    key = token[0];
                                    value = token[1];
                                } else if (token.length == 1) {
                                    key = token[0];
                                    value = "";
                                } else {
                                    key = "";
                                    value ="";
                                }
                                key = FilterUtil.replaceVariables(mailAdapter, key);
                                value = FilterUtil.replaceVariables(mailAdapter, value);
                                options.put(key, value);
                            }
                        } else {
                            throw new SyntaxException("Expecting a StringList for " + NOTIFY_OPTIONS);
                        }
                    } else {
                        throw new SyntaxException("Expecting a parameter for " + NOTIFY_OPTIONS);
                    }
                } else if (message == null && NOTIFY_MESSAGE.equals(tag)) {
                    // The next argment must be a string
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            List<String> stringList = ((StringListArgument) argument).getList();
                            if (stringList.size() != 1) {
                                throw new SyntaxException("Expecting exactly one String for " + NOTIFY_MESSAGE);
                            }
                            message = FilterUtil.replaceVariables(mailAdapter, (String) stringList.get(0));
                        } else {
                            throw new SyntaxException("Expecting a StringList for " + NOTIFY_MESSAGE);
                        }
                    } else {
                        throw new SyntaxException("Expecting a parameter for " + NOTIFY_MESSAGE);
                    }
                }
            } else {
                // Stop when a non-tag argument is encountered
                argumentsIter.previous();
                stop = true;
            }
        }

        // The next argument MUST be a <method: string>
        if (argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                List<String> stringList = ((StringListArgument) argument).getList();
                if (stringList.size() != 1) {
                    throw new SyntaxException("Expecting exactly one String");
                }
                method = (String) stringList.get(0);
            } else {
                throw new SyntaxException("Expecting a StringList");
            }
        }
        if (method == null) {
            throw context.getCoordinate().syntaxException("Expecting a method string");
        } else {
            method = FilterUtil.replaceVariables(mailAdapter, method);
        }
        
        mailtoParams = new HashMap<String, List<String>>();
        try {
            URL url = new URL(method);
            mailto = FilterUtil.replaceVariables(mailAdapter, url.getPath());
            String query = url.getQuery();

            if (!StringUtil.isNullOrEmpty(query)) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] token = param.split("=");
                    if (token.length > 2) {
                        throw new SyntaxException("'mailto' method syntax error: too many parameters");
                    } else {
                        if (StringUtils.isEmpty(token[0])) {
                            throw new SyntaxException("'mailto' method syntax error: empty parameter name");
                        }
                    }
                    // If the value or parameter name is URL encoded, it should be
                    // decoded.  If it is not even URL encoded, more or less decoding
                    // the string does not do harm the contents.
                    String headerName = null;
                    String headerValue = null;
                    try {
                        headerName = URLDecoder.decode(token[0].toLowerCase(), "UTF-8");
                    } catch (UnsupportedEncodingException e)  {
                        // No exception should be thrown because the charset is always "UTF-8"
                    } catch (IllegalArgumentException e) {
                        headerName = token[0].toLowerCase();
                    }
                    if (token.length == 1) {
                        // The value must be empty
                        headerValue = "";
                    } else {
                        headerValue = token[1];
                    }
                    try {
                        headerValue = URLDecoder.decode(headerValue, "UTF-8");
                    } catch (UnsupportedEncodingException e)  {
                        // No exception should be thrown because the charset is always "UTF-8"
                    } catch (IllegalArgumentException e) {
                        // Use token[1] as is
                    }

                    if (!mailtoParams.containsKey(headerName)) {
                        // Create a new entry for a header
                        List<String> value = new ArrayList<String>();
                        value.add(headerValue);
                        mailtoParams.put(headerName, value);
                    } else {
                        // Some headers, such as to or cc fields, can be specified multiple times
                        mailtoParams.get(headerName).add(headerValue);
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new SyntaxException("'mailto' method syntax error", e);
        }
        mail.addAction(new ActionNotifyMailto(from, options, importance, message, mailto, mailtoParams));
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        // done in executeBasic()
    }
}

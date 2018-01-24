/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2017 Synacor, Inc.
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
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import com.zimbra.common.filter.Sieve;
import com.zimbra.common.filter.Sieve.Comparator;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HtmlTextExtractor;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import javax.mail.Part;

import static com.zimbra.cs.filter.jsieve.ComparatorName.ASCII_NUMERIC_COMPARATOR;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ListIterator;

public class BodyTest extends AbstractTest {

    static final String CONTAINS = ":contains";
    static final String COMPARATOR = ":comparator";

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context)
            throws SieveException {
        String comparison = null;
        boolean caseSensitive = false;
        String key = null;
        @SuppressWarnings("unchecked")
        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();

        // First argument MUST be a tag of ":contains"
        // TODO: handles ":matches" with * and ?
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof TagArgument)
            {
                String tag = ((TagArgument) argument).getTag();
                if (tag.equals(CONTAINS))
                    comparison = tag;
                else
                    throw new SyntaxException(
                        "Found unexpected TagArgument: \"" + tag + "\"");
            }
        }
        if (null == comparison)
            throw new SyntaxException("Expecting \"" + CONTAINS + "\"");

        // Second argument could be :comparator tag or else the value (string)
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof TagArgument)
            {
                String tag = ((TagArgument) argument).getTag();
                if (tag.equals(COMPARATOR)) {
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            StringListArgument strList = (StringListArgument) argument;
                            try {
                                String comparator = strList.getList().get(0);
                                if (ASCII_NUMERIC_COMPARATOR.equalsIgnoreCase(comparator) && mail instanceof ZimbraMailAdapter) {
                                    Require.checkCapability((ZimbraMailAdapter) mail, ASCII_NUMERIC_COMPARATOR);
                                }
                                caseSensitive = Sieve.Comparator.ioctet == Sieve.Comparator.fromString(comparator);
                            } catch (ServiceException e) {
                                throw new SyntaxException(e.getMessage());
                            }

                            // Move to the last argument
                            if (argumentsIter.hasNext())
                                argument = argumentsIter.next();
                        } else {
                            throw new SyntaxException("Found unexpected argument after :comparator");
                        }
                    } else {
                        throw new SyntaxException("Unexpected end of arguments");
                    }
                } else {
                    throw new SyntaxException("Found unexpected TagArgument: \"" + tag + "\"");
                }
            }

            if (argument instanceof StringListArgument) {
                StringListArgument strList = (StringListArgument) argument;
                key = strList.getList().get(0);
            }
        }
        if (null == key)
            throw new SyntaxException("Expecting a string");

        // There MUST NOT be any further arguments
        if (argumentsIter.hasNext())
            throw new SyntaxException("Found unexpected argument(s)");

        return mail instanceof ZimbraMailAdapter && test(mail, caseSensitive, key);

    }
    
    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) {
        // override validation -- it's already done in executeBasic above
    }

    private boolean test(MailAdapter mail, boolean caseSensitive, String substring) {
        ZimbraMailAdapter zimbraMail = (ZimbraMailAdapter) mail;
        ParsedMessage pm = zimbraMail.getParsedMessage();
        if (pm == null) {
            return false;
        }

        Account acct = null;
        try {
            acct = zimbraMail.getMailbox().getAccount();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Error in getting account", e);
        }
        String defaultCharset = acct == null ? null : acct.getPrefMailDefaultCharset();

        for (MPartInfo mpi : pm.getMessageParts()) {
            String cType = mpi.getContentType();
            // Check only parts that are text/plain or text/html and are not attachments.
            if (!Part.ATTACHMENT.equals(mpi.getDisposition())) {
                if (cType.equals(MimeConstants.CT_TEXT_PLAIN)) {
                    InputStream in = null;
                    try {
                        in = mpi.getMimePart().getInputStream();
                        String cthdr = mpi.getMimePart().getHeader("Content-Type", null);
                        String charset = null;
                        if (cthdr != null) {
                            charset = Mime.getCharset(cthdr);
                        }
                        if (charset == null || !Charset.isSupported(charset)) {
                            charset = defaultCharset;
                        }
                        Reader reader = charset == null ? new InputStreamReader(in) : new InputStreamReader(in, charset);
                        if (contains(reader, caseSensitive, substring)) {
                            return true;
                        }
                    } catch (Exception e) {
                        ZimbraLog.filter.warn("Unable to test text body for substring '%s'", substring, e);
                    } finally {
                        ByteUtil.closeStream(in);
                    }
                } else if (cType.equals(MimeConstants.CT_TEXT_HTML)) {
                    InputStream in = null;

                    try {
                        // Extract up to 1MB of text and check for substring.
                        in = mpi.getMimePart().getInputStream();
                        String cthdr = mpi.getMimePart().getHeader("Content-Type", null);
                        Reader reader = Mime.getTextReader(in, cthdr, defaultCharset);
                        String text = HtmlTextExtractor.extract(reader, 1024 * 1024);
                        if (contains(new StringReader(text), caseSensitive, substring)) {
                            return true;
                        }
                    } catch (Exception e) {
                        ZimbraLog.filter.warn("Unable to test HTML body for substring '%s'", substring, e);
                    } finally {
                        ByteUtil.closeStream(in);
                    }
                }
            }
        }
        return false;
    }
    
    private boolean contains(Reader reader, boolean caseSensitive, String substring)
    throws IOException {
        int matchIndex = 0;
        if (!caseSensitive)
            substring = substring.toLowerCase();
        PushbackReader pb = new PushbackReader(reader, substring.length());
        char[] substringArray = substring.toCharArray();
        int c;
        while ((c = getNextChar(pb)) > 0) {
            if ((!caseSensitive && substring.charAt(matchIndex) == Character.toLowerCase(c)) ||
                    (caseSensitive && substring.charAt(matchIndex) == c)) {
                matchIndex++;
                if (matchIndex == substring.length())
                    return true;
            } else if (matchIndex > 0) {
                // unread this non-matching char
                pb.unread(c);
                // unread matched chars except the first char that matched
                pb.unread(substringArray, 1, matchIndex - 1);
                matchIndex = 0;
            }
        }
        return false;
    }
    
    private int getNextChar(PushbackReader reader)
    throws IOException {
        int c = reader.read();
        if (c != '\r' && c != '\n') {
            // The end, or not a newline character.
            return c;
        }
        
        // Replace multiple newline characters with a single space.
        do {
            c = reader.read();
        } while (c == '\r' || c == '\n');
            
        if (c >= 0) {
            // Push the last character back, so that it's read next time.
            reader.unread(c);
        }
        return ' ';
    }
}

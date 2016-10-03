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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.NumberArgument;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.comparators.ComparatorNames;
import org.apache.jsieve.comparators.ComparatorUtils;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.OperationException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.ComparatorTags;

import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class ReplaceHeader extends AbstractCommand {
    // index
    private static final String INDEX = ":index";
    private static final String LAST = ":last";
    private Integer index = null;
    private boolean last = false;
    // newname and newvalue
    private static final String NEW_NAME = ":newname";
    private static final String NEW_VALUE = ":newvalue";
    private String newName = null;
    private String newValue = null;
    // comparator
    private String comparator = null;
    private static final String I_ASCII_NUMERIC = "i;ascii-numeric";
    // match-type
    private boolean contains = false;
    private boolean is = false;
    private boolean matches = false;
    private static final String COUNT = ":count";
    private static final String VALUE = ":value";
    private boolean countTag = false;
    private boolean valueTag = false;
    private String relationalComparator = null;
    // key and valuelist
    private String key = null;
    private List<String> valueList = null;

    @SuppressWarnings("unchecked")
    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments,
            Block block, SieveContext context) throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            ZimbraLog.filter.info("Zimbra mail adapter not found.");
            return null;
        }

        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;

        // replace variables
        List<String> temp = new ArrayList<String>();
        if (valueList != null && !valueList.isEmpty()) {
            for (String value : valueList) {
                temp.add(Variables.replaceAllVariables(mailAdapter, value));
            }
        }
        valueList = temp;
        if (newValue != null) {
            newValue = Variables.replaceAllVariables(mailAdapter, newValue);
        }

        MimeMessage mm = mailAdapter.getMimeMessage();
        Enumeration<Header> headers;
        try {
            headers = mm.getAllHeaders();
            if (!headers.hasMoreElements()) {
                ZimbraLog.filter.info("No headers found in mime.");
                return null;
            }
        } catch (MessagingException e) {
            throw new OperationException("Error occured while fetching all headers from mime.", e);
        }

        int headerCount = 0;
        try {
            String[] headerValues = mm.getHeader(key);
            headerCount = headerValues != null ? headerValues.length : 0;
        } catch (MessagingException e) {
            throw new OperationException("Error occured while fetching " + key + " headers from mime.", e);
        }
        if (headerCount < 1) {
            ZimbraLog.filter.info("No headers found matching with \"%s\" in mime.", key);
            return null;
        }
        if (last && headerCount > index) {
            if (index == 0) {
                index = headerCount - index;
            } else  {
                index = headerCount - index + 1;
            }
        }
        int matchIndex = 0;
        Set<String> removedHeaders = new HashSet<String>();

        try {
            while (headers.hasMoreElements()) {
                Header header = headers.nextElement();
                String newHeaderName = null;
                String newHeaderValue = null;
                boolean replace = false;
                if (!(removedHeaders.contains(header.getName()))) {
                    mm.removeHeader(header.getName());
                    removedHeaders.add(header.getName());
                }
                if (header.getName().equalsIgnoreCase(key)){
                    matchIndex++;
                    if (index == null || (index != null && index == matchIndex)) {
                        for (String value : valueList) {

                            if (comparator.equals(I_ASCII_NUMERIC)) {
                                if (valueTag) {
                                    switch (relationalComparator) {
                                    case MatchRelationalOperators.GT_OP:
                                        if (Integer.valueOf(header.getValue()) > Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.GE_OP:
                                        if (Integer.valueOf(header.getValue()) >= Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.LT_OP:
                                        if (Integer.valueOf(header.getValue()) < Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.LE_OP:
                                        if (Integer.valueOf(header.getValue()) <= Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.EQ_OP:
                                        if (Integer.valueOf(header.getValue()) == Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.NE_OP:
                                        if (Integer.valueOf(header.getValue()) != Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    default:
                                        throw new SyntaxException("Invalid relational comparator provided in replaceheader.");
                                    }
                                } else if (countTag) {
                                    switch (relationalComparator) {
                                    case MatchRelationalOperators.GT_OP:
                                        if (headerCount > Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.GE_OP:
                                        if (headerCount >= Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.LT_OP:
                                        if (headerCount < Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.LE_OP:
                                        if (headerCount <= Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.EQ_OP:
                                        if (headerCount == Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    case MatchRelationalOperators.NE_OP:
                                        if (headerCount != Integer.valueOf(value)) {
                                            replace = true;
                                        }
                                        break;
                                    default:
                                        throw new SyntaxException("Invalid relational comparator provided in replaceheader.");
                                    }
                                } else {
                                    throw new SyntaxException(":value or :count not found for numeric operation in replaceheader.");
                                }
                            } else if (is && ComparatorUtils.is(comparator, header.getValue(), value, context)) {
                                replace = true;
                            } else if (contains && ComparatorUtils.contains(comparator, header.getValue(), value, context)) {
                                replace = true;
                            } else if (matches && ComparatorUtils.matches(comparator, header.getValue(), value, context)) {
                                replace = true;
                            } else {
                                ZimbraLog.filter.debug("Key: %s and Value: %s pair not matching requested criteria.", key, value);
                            }

                            if (replace) {
                                if (newName != null) {
                                    newHeaderName = newName;
                                } else {
                                    newHeaderName = header.getName();
                                }
                                if (newValue != null) {
                                    newHeaderValue = newValue;
                                } else {
                                    newHeaderValue = header.getValue();
                                }
                                header = new Header(newHeaderName, newHeaderValue);
                                break;
                            }
                        }
                    }
                }
                mm.addHeader(header.getName() ,header.getValue());
             }
            mm.saveChanges();
            mailAdapter.updateIncomingBlob();
        } catch (MessagingException me) {
            throw new OperationException("Error occured while operating mime.", me);
        }
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
            throws SieveException {
        ZimbraLog.filter.debug(arguments.getArgumentList().toString());
        // set up class variables
        Iterator<Argument> itr = arguments.getArgumentList().iterator();
        while (itr.hasNext()) {
            Argument arg = itr.next();
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                if (tag.is(INDEX)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof NumberArgument) {
                            index = ((NumberArgument) arg).getInteger();
                        } else {
                            throw new SyntaxException("Invalid index provided with replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(LAST)) {
                    last = true;
                } else if (tag.is(NEW_NAME)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            newName = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("New name not provided with :newname in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(NEW_VALUE)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            newValue = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("New value not provided with :newValue in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(COUNT)) {
                    if (valueTag) {
                        throw new SyntaxException(":count and :value both can not be used with replaceheader");
                    }
                    countTag = true;
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            relationalComparator = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("Relational comparator not provided with :count in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(VALUE)) {
                    if (countTag) {
                        throw new SyntaxException(":count and :value both can not be used with replaceheader");
                    }
                    valueTag = true;
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            relationalComparator = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("Relational comparator not provided with :value in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(ComparatorTags.COMPARATOR_TAG)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            comparator = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("Comparator not provided with :comparator in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(MatchTypeTags.CONTAINS_TAG)) {
                    contains = true;
                } else if (tag.is(MatchTypeTags.IS_TAG)) {
                    is = true;
                } else if (tag.is(MatchTypeTags.MATCHES_TAG)) {
                    matches = true;
                } else {
                    throw new SyntaxException("Invalid tag argument provided with replaceheader.");
                }
            } else if (arg instanceof StringListArgument) {
                StringListArgument sla = (StringListArgument) arg;
                key = sla.getList().get(0);
                if (itr.hasNext()) {
                    arg = itr.next();
                    sla = (StringListArgument) arg;
                    valueList = sla.getList();
                } else {
                    throw new SyntaxException("Value for " + key + " is not provided in replaceheader.");
                }
            } else {
                ZimbraLog.filter.info("Unknown argument provided: " + arg.getValue());
            }
        }

        // Actual validation starts from here

        // Match type or Comparator type condition must be present
        if (!(is || contains || matches || countTag || valueTag)) {
            throw new SyntaxException("Match type or Comparator type must be present in replaceheader.");
        }

        // Key and value both must be present at a time
        if (key == null || valueList == null) {
            throw new SyntaxException("key or value not found in replaceheader.");
        }

        // character set validation
        if (newName != null) {
            if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(newName, CharsetUtil.US_ASCII))) {
                throw new SyntaxException("newname must be printable ASCII only in replaceheader.");
            }
        }
        if (newValue != null) {
            if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(newValue, CharsetUtil.US_ASCII))) {
                throw new SyntaxException("newvalue must be printable ASCII only in replaceheader.");
            }
        }
        if (key != null) {
            if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(key, CharsetUtil.US_ASCII))) {
                throw new SyntaxException("key must be printable ASCII only in replaceheader.");
            }
        }
        if (valueList != null && !valueList.isEmpty()) {
            for (String value : valueList) {
                if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(value, CharsetUtil.US_ASCII))) {
                    throw new SyntaxException("value must be printable ASCII only in replaceheader.");
                }
            }
        }

        // relation comparator must be valid
        if (relationalComparator != null) {
            if (!(relationalComparator.equals(MatchRelationalOperators.GT_OP)
                    || relationalComparator.equals(MatchRelationalOperators.GE_OP)
                    || relationalComparator.equals(MatchRelationalOperators.LT_OP)
                    || relationalComparator.equals(MatchRelationalOperators.LE_OP)
                    || relationalComparator.equals(MatchRelationalOperators.EQ_OP)
                    || relationalComparator.equals(MatchRelationalOperators.NE_OP))) {
                throw new SyntaxException("Invalid relational comparator provided in replaceheader.");
            }
        }

        // comparator must be valid and if not set, then set to default i.e. ComparatorNames.ASCII_CASEMAP_COMPARATOR
        if (comparator != null) {
            if (!(comparator.equals(I_ASCII_NUMERIC)
                    || comparator.equals(ComparatorNames.OCTET_COMPARATOR)
                    || comparator.equals(ComparatorNames.ASCII_CASEMAP_COMPARATOR)
                    )) {
                throw new SyntaxException("Invalid comparator type provided");
            }
        } else {
            comparator = ComparatorNames.ASCII_CASEMAP_COMPARATOR;
            ZimbraLog.filter.info("No comparator type provided, so setting to default %s", ComparatorNames.ASCII_CASEMAP_COMPARATOR);
        }

        // relational comparator must be available with numeric comparision
        if (comparator.equals(I_ASCII_NUMERIC) && !(countTag || valueTag)) {
            throw new SyntaxException(":value or :count not found for numeric operation in replaceheader.");
        }

        // set index 0 if last tag argument is provided. So that, correct index can be calculated.
        if (index == null && last) {
            index = 0;
        }
    }
}
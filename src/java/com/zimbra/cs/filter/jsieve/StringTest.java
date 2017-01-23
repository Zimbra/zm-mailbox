/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import static org.apache.jsieve.comparators.ComparatorNames.ASCII_CASEMAP_COMPARATOR;
import static org.apache.jsieve.comparators.MatchTypeTags.IS_TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.Header;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author zimbra
 *
 */
public class StringTest extends Header {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jsieve.tests.AbstractTest#executeBasic(org.apache.jsieve.mail.
     * MailAdapter, org.apache.jsieve.Arguments, org.apache.jsieve.SieveContext)
     */
    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
         if (!(mail instanceof ZimbraMailAdapter)) {
                return false;
         }

         ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
         if (!SetVariable.isVariablesExtAvailable(mailAdapter)) {
             return false;
         }
         Map<String, String> existingVars = mailAdapter.getVariables();
         List<String> matchedValues = mailAdapter.getMatchedValues();
            
        String matchType = IS_TAG;
        String comparator = null;
        List<String> sourceValues = null;
        List<String> keyValues = null;

        int index = 0;
            
            for (Argument a: arguments.getArgumentList()) {
                if (a instanceof TagArgument) {
                    if (index == 0) {
                        matchType=  ((TagArgument) a).getTag();
                        ++index;
                    } else {
                        comparator = ((TagArgument) a).getTag();
                        index = 0;
                    }
                } else {
                    if (comparator != null && index == 0) {
                        String argument = ((StringListArgument) a).getList().get(0);
                        comparator = argument;
                        ++index;
                    } else {
                        if (index == 1) {
                            sourceValues = ((StringListArgument) a).getList();
                            ++index;
                        } else {
                            keyValues = ((StringListArgument) a).getList();
                        }
                    }
                }
            }
            List<String> tempSourceValues =  new ArrayList<String>();
            for (String sourceValue : sourceValues) {
                if (sourceValue.startsWith("${")) {
                    sourceValue = FilterUtil.replaceVariables(existingVars, matchedValues, sourceValue);
                }
                tempSourceValues.add(sourceValue);
            }
            sourceValues = tempSourceValues;
            
       
            return match(mail, (comparator == null ? ASCII_CASEMAP_COMPARATOR
                    : comparator), matchType, sourceValues, keyValues, context);

    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
        if (arguments.getArgumentList().size() < 3) {
            throw new SyntaxException("Atleast 3 argument are needed. Found " + arguments);
        }
        for (Argument a : arguments.getArgumentList()) {
            System.out.println(a);
            ZimbraLog.filter.debug(a);
        }
    }

    protected boolean match(MailAdapter mail, String comparator, String matchType,
            List<String> source, List<String>keys, SieveContext context) throws SieveException {
        
        return match(comparator, matchType, source, keys, context);
        
    }

    

}

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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class Variables extends AbstractActionCommand {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {

        return null;
    }

    /**
     * Lookup the variable table to get the set value.
     * If the 'sourceStr' contains a variable-ref (formatted with ${variable-name}), this method looks up the
     * variable table with its variable-name and replaces the variable with the text value assigned by
	 * the 'set' command.
     * According to the RFC 5229 Section 4., "Variable names are case insensitive."
     *
     * @param mail The target mail object for the sieve.
     * @param sourceStr text string that may contain "variable-ref" (RFC 5229 Section 3.)
     * @return Replaced text string
     */
    public static String replaceAllVariables(MailAdapter mail, String sourceStr) {
        if (!(mail instanceof ZimbraMailAdapter) || sourceStr == null || sourceStr.length() == 0) {
            return sourceStr;
        }
        Map<String, String> variables = ((ZimbraMailAdapter) mail).getVariables();
        String resultStr = leastGreedyReplace(variables, sourceStr);

        List<String> matchedVariables = ((ZimbraMailAdapter) mail).getMatchedValues();
        for (int i = 0; i < matchedVariables.size() && i < 10; i++) {
                String keyName = "(?i)" + "\\$\\{" + String.valueOf(i) + "\\}";
                resultStr = resultStr.replaceAll(keyName, Matcher.quoteReplacement(matchedVariables.get(i)));
        }
        return resultStr;
    }

    /**
     * Replaces all ${variable name} variables within the 'sourceStr' into the defined text value.
     *
     * The variable name matches as short as possible (non-greedy matching).  Unknown variables are replaced
     * by the empty string (RFC 5229 Section 3.)
     *
     * @param variables map table of the variable name and value
     * @param sourceStr text string that may contain one or more than one "variable-ref"
     * @return Replaced text string
     */
    public static String leastGreedyReplace(Map<String, String> variables, String sourceStr) {
        if (variables == null || sourceStr == null || sourceStr.length() == 0) {
            return sourceStr;
        }
        sourceStr = FilterUtil.handleQuotedAndEncodedVar(sourceStr);
        StringBuilder resultStr = new StringBuilder();
        int start1 = 0;
        int end = -1;
        while (start1 < sourceStr.length()) {
            int start2 = sourceStr.indexOf("${", start1);
            if (start2 >= 0) {
                resultStr.append(sourceStr.substring(start1, start2));
                end = sourceStr.indexOf("}", start2 + 2);
                if (end > 0) {
                    int start3 = sourceStr.indexOf("${", start2 + 2);
                    if (start3 > start2 && start3 < end) {
                        start1 = start3;
                        resultStr.append(sourceStr.substring(start2, start3));
                    } else {
                        // a variable name found
                        String key = sourceStr.substring(start2 + 2, end).toLowerCase();
                        if (key.matches(".*[\\p{Alpha}$_.]|[\\d]")) {
                            // the variable name is valid
                            String value = variables.get(key);
                            if (value != null) {
                                resultStr.append(value);
                            }
                            start1 = end + 1;
                        } else {
                            // the variable name contains some invalid characters
                            resultStr.append(sourceStr.substring(start2, end + 1));
                            start1 = end + 1;
                        }
                    }
                } else {
                    // no corresponding }
                    resultStr.append(sourceStr.substring(start2, sourceStr.length()));
                    break;
                }
            } else {
                // no more ${
                resultStr.append(sourceStr.substring(end + 1, sourceStr.length()));
                break;
            }
        }
        return resultStr.toString();
    }
}

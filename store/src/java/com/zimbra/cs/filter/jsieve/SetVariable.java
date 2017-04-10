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
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author rdesai
 *
 */
public class SetVariable extends AbstractCommand {
    
    public static final String ALL_LOWER_CASE = ":lower";
    public static final String ALL_UPPER_CASE = ":upper";
    public static final String LOWERCASE_FIRST = ":lowerfirst";
    public static final String UPPERCASE_FIRST = ":upperfirst";
    public static final String QUOTE_WILDCARD = ":quotewildcard";
    public static final String STRING_LENGTH = ":length";
    public static final String ENCODE_URL = ":encodeurl";

    public static final int OPERATIONS_IDX = 7;

    public static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("([\\p{Alpha}]|_)+([\\p{Alnum}|_])*",  Pattern.CASE_INSENSITIVE);

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {

        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        this.validateArguments(arguments, context);

        Map<String, String> existingVars = mailAdapter.getVariables();
        List<String> matchedValues = mailAdapter.getMatchedValues();
        String [] operations = new String[OPERATIONS_IDX];
        String key = null;
        String value = null;
        int index = 0;
        for (Argument a: arguments.getArgumentList()) {
            if (a instanceof TagArgument) {
                TagArgument tag = (TagArgument) a;
                String tagValue = tag.getTag();
                if (isValidModifier(tagValue)) {
                    operations[getIndex(tagValue)] = tagValue.toLowerCase();
                }            
            } else {
                String argument = ((StringListArgument) a).getList().get(0);
                if (index == 0) {
                    key = FilterUtil.handleQuotedAndEncodedVar(argument);
                } else {
                    if (argument.contains("${")) {
                        value = FilterUtil.replaceVariables(mailAdapter, argument);
                    } else {
                        value = argument;
                    }
                }
                ++index;
            }
        }
        value = applyModifiers(value, operations);
        mailAdapter.addVariable(key, value);
        return null;
    }

    /**
     * @param argument
     * @return
     */
    public static int getIndex(String operation) {
        int index = 0;

        if (!StringUtil.isNullOrEmpty(operation)) {
            operation = operation.toLowerCase();
        }
        if (operation.equals(STRING_LENGTH)) {         // Precedence: 10
            index = 4;
        } else if (operation.equals(ENCODE_URL)) {     // Precedence: 15
            index = 3;
        } else if (operation.equals(QUOTE_WILDCARD)) { // Precedence: 20
            index = 2;
        } else if (operation.equals(LOWERCASE_FIRST)) {// Precedence: 30
            index = 1;
        } else if (operation.equals(UPPERCASE_FIRST)) {// Precedence: 30
            index = 1;
        } else if (operation.equals(ALL_LOWER_CASE)) { // Precedence: 40
            index = 0;
        } else if (operation.equals(ALL_UPPER_CASE)) { // Precedence: 40
            index =0;
        } 
        
        return index;
    }

    /**
     * @param value
     * @param operations
     * @return
     */
    public static String applyModifiers(String value, String[] operations) {
        String temp = value;
        for (int i = 0; i < operations.length; ++i) {
            String operation = operations[i];
            if (operation == null) {
                continue;
            }
            if (operation.equals(STRING_LENGTH)) {
                temp = String.valueOf(value.length());
            } else if (operation.equals(QUOTE_WILDCARD)) {
                temp = temp.replaceAll("\\\\", "\\\\\\\\");
                temp = temp.replaceAll("\\*", "\\\\*");
                temp = temp.replaceAll("\\?", "\\\\?");    
            } else if (operation.equals(LOWERCASE_FIRST)) {
                temp = String.valueOf(temp.charAt(0)).toLowerCase() + temp.substring(1);
            } else if (operation.equals(UPPERCASE_FIRST)) {
                temp = String.valueOf(temp.charAt(0)).toUpperCase() + temp.substring(1);
            } else if (operation.equals(ALL_LOWER_CASE)) {
                temp = value.toLowerCase();
            } else if (operation.equals(ALL_UPPER_CASE)) {
                temp = temp.toUpperCase();
            } else if (operation.equals(ENCODE_URL)) {
                try {
                    temp = URLEncoder.encode(temp, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    temp = value;
                }
            } else {
                temp = value;
            }
        }
        return temp;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
        List<Argument> args = arguments.getArgumentList();
        // RFC 5229
        //  ":lower" / ":upper" / ":lowerfirst" / ":upperfirst" /
        //  ":quotewildcard" / ":length"
        //    set "name" "Ethelbert"
        //    set "a" "juMBlEd lETteRS";             => "juMBlEd lETteRS"
        //    set :length "b" "${a}";                => "15"
        //    set :lower "b" "${a}";                 => "jumbled letters"
        //    set :upperfirst "b" "${a}";            => "JuMBlEd lETteRS"
        //    set :upperfirst :lower "b" "${a}";     => "Jumbled letters"
        //    set :quotewildcard "b" "Rock*";        => "Rock\*"
        // RFC 5435
        //  ":encodeurl"
        //    set :encodeurl "body_param" "Safe body&evil=evilbody"; => "safe+body%26evil%3Devilbody"
        int varArgCount = 0;
        String key = null;
        String [] operations = new String[OPERATIONS_IDX];
        if (args.size() >= 2) {
            for (Argument arg: arguments.getArgumentList()) {
                
                if (arg instanceof TagArgument) {
                    TagArgument tag = (TagArgument) arg;
                    String tagValue = tag.getTag();
                    if (!isValidModifier(tagValue)) {
                        throw new SyntaxException("Invalid variable modifier:" + tagValue);
                    } else {
                        int index = getIndex(tagValue);
                        if (StringUtil.isNullOrEmpty(operations[index])) {
                            operations[index] = tagValue.toLowerCase();
                        } else {
                            throw new SyntaxException("Cannot use two or more modifiers of the same"
                                + " precedence in a single \"set\" action. Modifiers used: " + tagValue
                                + " and "  + operations[index]);
                        }
                    }
                } else {
                    String argument = ((StringListArgument) arg).getList().get(0);
                    ZimbraLog.filter.debug("set variable argument: " + argument);
                    if (varArgCount == 0) {
                        key = argument;
                    }
                    ++varArgCount;
                }
            }
            if (varArgCount != 2) {
                throw new SyntaxException("Exactly 2 arguments permitted. Found " + varArgCount);
            } else {
                if (!(isValidIdentifier(key))) {
                    throw new SyntaxException("Variable identifier is invalid, got identifier " + key);
                }
            }
        }  else {
            throw new SyntaxException("Minimum 2 arguments are needed. Usage: set :upperfirst \"b\" \"hello\";. Arguments found: " 
                    + arguments);
        }
    }

    /**
     * @param key
     * @return
     */
    public static boolean isValidIdentifier(String key) {
        if (VALID_IDENTIFIER_PATTERN.matcher(key).matches()) {
           return true;
        }
        return false;
    }

    /**
     * @param argument
     * @return
     */
    private boolean isValidModifier(String modifier) {
        String temp = modifier.toLowerCase();
        ZimbraLog.filter.debug("Set variable modifier is:" + temp);
        if (temp.equals(ALL_LOWER_CASE) || temp.equals(ALL_UPPER_CASE) || temp.equals(LOWERCASE_FIRST) 
                || temp.equals(UPPERCASE_FIRST) || temp.equals(QUOTE_WILDCARD) || temp.equals(STRING_LENGTH)
                || temp.equals(ENCODE_URL)) {
            return true;
        }
        return false;
    }
}

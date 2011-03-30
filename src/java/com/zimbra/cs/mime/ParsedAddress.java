/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.util.StringUtil;

/**
 * @since Jan 31, 2005
 */
public final class ParsedAddress implements Comparable<ParsedAddress> {

    public String emailPart;
    public String personalPart;
    public String honorific;
    public String firstName;
    public String middleName;
    public String lastName;
    public String suffix;

    private boolean parsed = false;

    public ParsedAddress(String address) {
        InternetAddress ia = new InternetAddress(address);
        initialize(ia.getAddress(), ia.getPersonal());
    }

    public ParsedAddress(InternetAddress ia) {
        initialize(ia.getAddress(), ia.getPersonal());
    }

    public ParsedAddress(javax.mail.internet.InternetAddress ia) {
        initialize(ia.getAddress(), ia.getPersonal());
    }

    public ParsedAddress(String email, String personal) {
        initialize(email, personal);
    }

    public ParsedAddress(ParsedAddress node) {
        emailPart    = node.emailPart;
        personalPart = node.personalPart;
        honorific    = node.honorific;
        firstName    = node.firstName;
        middleName   = node.middleName;
        lastName     = node.lastName;
        suffix       = node.suffix;
        parsed       = node.parsed;
    }

    private void initialize(String email, String personal) {
        emailPart    = email;
        personalPart = personal;
        if ("".equals(emailPart))
            emailPart = null;
        if (personalPart != null)
            personalPart = personalPart.trim().replaceAll("\\s+", " ");
    }

    public String getSortString() {
        parse();
        String sort = (personalPart != null ? personalPart : emailPart);
        return (sort != null ? sort : "");
    }

    public static String getSortString(List<ParsedAddress> addrs) {
        List<String> list = new ArrayList<String>(addrs.size());
        for (ParsedAddress addr : addrs) {
            list.add(addr.getSortString());
        }
        return Joiner.on(", ").join(list);
    }

    public Map<String, String> getAttributes() {
        parse();
        HashMap<String, String> map = new HashMap<String, String>();
        if (honorific != null)     map.put(ContactConstants.A_namePrefix, honorific);
        if (firstName != null)     map.put(ContactConstants.A_firstName, firstName);
        if (middleName != null)    map.put(ContactConstants.A_middleName, middleName);
        if (lastName != null)      map.put(ContactConstants.A_lastName, lastName);
        if (suffix != null)        map.put(ContactConstants.A_nameSuffix, suffix);
        if (personalPart != null)  map.put(ContactConstants.A_fullName, personalPart);
        if (emailPart != null)     map.put(ContactConstants.A_email, emailPart);
        return map;
    }

    public ParsedAddress parse() {
        if (parsed)
            return this;

        // we've seen clients sticking an extra pair of double quotes around the name
        if (personalPart != null && personalPart.startsWith("\"") && personalPart.indexOf('"', 1) == personalPart.length() - 1)
            personalPart = personalPart.substring(1, personalPart.length() - 1).trim().replaceAll("\\s+", " ");

        if (personalPart != null && personalPart.equals(""))
            personalPart = null;

        if (emailPart != null && emailPart.equals(personalPart))
            personalPart = null;

        if (emailPart != null && personalPart == null) {
            int p = emailPart.indexOf('@');
            String possible = (p == -1 ? emailPart : emailPart.substring(0, p)).replace('.', ' ').replace('_', ' ');
            if (possible.indexOf(' ') != -1)
                personalPart = possible;
            else
                firstName = possible;
        }

        if (personalPart != null) {
            List<NameToken> tokens = NameToken.tokenize(personalPart);
            // for debug purposes...
//            tokentypes = NameToken.asTokenString(tokens);
            if (tokens.size() == 1) {
                firstName = tokens.get(0).toString();  parsed = true;
            }

            if (!parsed)
                parsed = parseLastFirst(tokens);
            if (!parsed)
                parsed = parseFirstLast(tokens);

            if (!parsed) {
                for (NameToken token : tokens) {
                    NameTokenType type = token.getType();
                    if (type == NameTokenType.ABBREVIATION || type == NameTokenType.INITIAL || type == NameTokenType.PREFIX || type == NameTokenType.TOKEN) {
                        firstName = token.toString();
                        break;
                    }
                }
            }
        }

        parsed = true;
        return this;
    }

    // PREFIX* (PREFIX | TOKEN) COMMA HONORIFIC* (ABBREVIATION | (INITIAL? (INITIAL | PREFIX | TOKEN))) (INITIAL | ABBREVIATION | PREFIX | TOKEN)* .*
    private boolean parseLastFirst(List<NameToken> tokens) {
        int index = 0, total = tokens.size();

        // last name: PREFIX* (PREFIX | TOKEN)
        int lstart = 0, lend = index = readLastName(tokens, index);
        if (lend == lstart)
            return false;
        // delimiter: COMMA
        if (index == 0 || index >= total - 1 || tokens.get(index++).getType() != NameTokenType.COMMA)
            return false;
        // honorific: HONORIFIC*
        int hstart = index, hend = index = readHonorific(tokens, index);
        // first name: ABBREVIATION | (INITIAL? (INITIAL | TOKEN | PREFIX))
        int fstart = index, fend = index = readFirstName(tokens, index, true);
        if (fend == fstart)
            return false;
        // middle name: (INITIAL | ABBREVIATION | PREFIX | TOKEN)*
        int mstart = index, mend = index = readMiddleName(tokens, index);
        // slop at end is treated as suffix
        int suffixOffset = tokens.get(index - 1).getEndOffset();
        suffix = readSuffix(tokens, index);

        if (hstart != hend)   honorific  = StringUtil.join(" ", tokens.subList(hstart, hend));
        if (fstart != fend)   firstName  = StringUtil.join(" ", tokens.subList(fstart, fend));
        if (mstart != mend)   middleName = StringUtil.join(" ", tokens.subList(mstart, mend));
        if (lstart != lend)   lastName   = StringUtil.join(" ", tokens.subList(lstart, lend));

        personalPart = (honorific == null ? "" : honorific + ' ') + firstName +
                       (middleName == null ? "" : ' ' + middleName) + ' ' + lastName +
                       personalPart.substring(suffixOffset).replaceAll("\\\\", "");

        return true;
    }

    // HONORIFIC* (ABBREVIATION | (INITIAL? (INITIAL | PREFIX | TOKEN))) (INITIAL | ABBREVIATION | PREFIX | TOKEN)* PREFIX* (PREFIX | TOKEN) .*
    private boolean parseFirstLast(List<NameToken> tokens) {
        int index = 0, total = tokens.size();

        List<NameToken> ltokens, mtokens = null;
        NameTokenType type;

        // honorific: HONORIFIC*
        int hstart = 0, hend = index = readHonorific(tokens, index);
        // first name: ABBREVIATION | (INITIAL? (INITIAL | TOKEN | PREFIX))
        int fstart = index, fend = index = readFirstName(tokens, index, false);
        // middle name & last name: (INITIAL | ABBREVIATION | PREFIX | TOKEN)* PREFIX* (PREFIX | TOKEN)
        int start = index;
        for ( ; index < total; index++) {
            if ((type = tokens.get(index).getType()) != NameTokenType.INITIAL && type != NameTokenType.ABBREVIATION && type != NameTokenType.PREFIX && type != NameTokenType.TOKEN)
                break;
        }
        for ( ; index > start; index--) {
            if (tokens.get(index - 1).getType() != NameTokenType.ABBREVIATION)
                break;
        }
        if (start == index)
            return false;
        int lstart = index - 1;
        if (tokens.get(lstart).getType() == NameTokenType.INITIAL)
            return false;
        for ( ; lstart > start; lstart--) {
            if (tokens.get(lstart - 1).getType() != NameTokenType.PREFIX)
                break;
        }
        if (lstart > start)
            mtokens = tokens.subList(start, lstart);
        ltokens = tokens.subList(lstart, index);
        // slop at end is treated as suffix
        int suffixOffset = tokens.get(index - 1).getEndOffset();
        suffix = readSuffix(tokens, index);

        if (hstart != hend)   honorific  = StringUtil.join(" ", tokens.subList(hstart, hend));
        if (fstart != fend)   firstName  = StringUtil.join(" ", tokens.subList(fstart, fend));
        if (mtokens != null)  middleName = StringUtil.join(" ", mtokens);
        if (ltokens != null)  lastName   = StringUtil.join(" ", ltokens);

        personalPart = personalPart.substring(0, suffixOffset) + personalPart.substring(suffixOffset).replaceAll("\\\\", "");

        return true;
    }

    private int readHonorific(List<NameToken> tokens, int index) {
        // honorific: HONORIFIC*
        while (index < tokens.size() && tokens.get(index).getType() == NameTokenType.HONORIFIC)
            index++;
        return index;
    }

    private int readFirstName(List<NameToken> tokens, int index, boolean greedy) {
        // first name: ABBREVIATION | (INITIAL? (INITIAL | TOKEN | PREFIX))
        int total = tokens.size();
        NameTokenType type, type3;
        if (index >= total)
            return index;
        else if ((type = tokens.get(index).getType()) == NameTokenType.ABBREVIATION || type == NameTokenType.TOKEN || type == NameTokenType.PREFIX)
            return index + 1;
        else if (type != NameTokenType.INITIAL)
            return index;
        // if we're here, it's INITIAL followed by...?
        // GREEDY:
        //  INITIAL INITIAL* | X*
        //  INITIAL TOKEN | X
        //  INITIAL | PREFIX TOKEN
        //  INITIAL | PREFIX PREFIX
        //  INITIAL PREFIX | X
        // NON-GREEDY:
        //  INITIAL INITAL* | X
        //  INITIAL TOKEN | TOKEN
        //  INITIAL TOKEN | PREFIX
        //  INITIAL | TOKEN X*
        //  INITIAL | PREFIX X*
        index++;
        if (index >= (greedy ? total : total - 1))
            return index;
        switch (tokens.get(index).getType()) {
            case INITIAL:
                while (index < total && tokens.get(index).getType() == NameTokenType.INITIAL)
                    index++;
                return index;
            case TOKEN:
                return (greedy || (type3 = tokens.get(index + 1).getType()) == NameTokenType.TOKEN || type3 == NameTokenType.PREFIX) ? index + 1 : index;
            case PREFIX:
                return (greedy && index < total - 1 && (type3 = tokens.get(index + 1).getType()) != NameTokenType.TOKEN && type3 != NameTokenType.PREFIX) ? index + 1 : index;
            default:
                return index;
        }
    }

    private int readMiddleName(List<NameToken> tokens, int index) {
        // middle name: (INITIAL | ABBREVIATION | PREFIX | TOKEN)*
        NameTokenType type;
        for ( ; index < tokens.size(); index++) {
            if ((type = tokens.get(index).getType()) != NameTokenType.INITIAL && type != NameTokenType.ABBREVIATION && type != NameTokenType.PREFIX && type != NameTokenType.TOKEN)
                break;
        }
        return index;
    }

    private int readLastName(List<NameToken> tokens, int index) {
        // last name: PREFIX* (PREFIX | TOKEN)
        NameTokenType type;
        for ( ; index < tokens.size(); index++) {
            if ((type = tokens.get(index).getType()) != NameTokenType.PREFIX) {
                if (type == NameTokenType.TOKEN)
                    index++;
                break;
            }
        }
        return index;
    }

    private String readSuffix(List<NameToken> tokens, int index) {
        NameTokenType type;
        int end = tokens.size();
        // trim off leading and trailing commas and other delimiters
        while (index < end && ((type = tokens.get(index).getType()) == NameTokenType.COMMA || type == NameTokenType.DELIMITER))
            index++;
        while (index < end && ((type = tokens.get(end - 1).getType()) == NameTokenType.COMMA || type == NameTokenType.DELIMITER))
            end--;
        // if the whole suffix is parenthesized, trim the parentheses
        if (index < end && tokens.get(index).getType() == NameTokenType.OPEN && tokens.get(end - 1).getType() == NameTokenType.CLOSE) {
            boolean parenthesized = true;
            for (int i = index + 1; i < end - 1 && parenthesized; i++) {
                if ((type = tokens.get(i).getType()) == NameTokenType.OPEN || type == NameTokenType.CLOSE)
                    parenthesized = false;
            }
            if (parenthesized) {
                index++;  end--;
            }
        }
        // return only the content
        return index >= end ? null : personalPart.substring(tokens.get(index - 1).getEndOffset(), tokens.get(end - 1).getEndOffset()).trim();
    }

    @Override
    public int hashCode() {
        if (emailPart != null)
            return emailPart.hashCode();
        else if (personalPart != null)
            return personalPart.hashCode();
        else
            return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj == null || !(obj instanceof ParsedAddress))
            return false;
        else
            return compareTo((ParsedAddress) obj) == 0;
    }

    @Override
    public int compareTo(ParsedAddress pa) {
        if (emailPart != null && pa.emailPart != null)
            return emailPart.compareToIgnoreCase(pa.emailPart);
        else if (emailPart != null && pa.emailPart == null)
            return 1;
        else if (emailPart == null && pa.emailPart != null)
            return -1;
        else if (personalPart != null && pa.personalPart != null)
            return personalPart.compareToIgnoreCase(pa.personalPart);
        else if (personalPart != null && pa.personalPart == null)
            return 1;
        else if (personalPart == null && pa.personalPart != null)
            return -1;
        else
            return 0;
    }

    @Override
    public String toString() {
        if (emailPart == null)
            return personalPart;
        else if (personalPart == null)
            return emailPart;
        else
            return '"' + personalPart + "\" <" + emailPart + '>';
    }

    private enum NameTokenType {
        TOKEN, PREFIX, INITIAL, HONORIFIC, ABBREVIATION, NUMERAL, COMMA, OPEN, CLOSE, DELIMITER;
    }

    private static class NameToken {
        private NameTokenType mType;
        private String mValue;
        private int mEndOffset;

        private NameToken(char delimiter, int start) {
            mValue = "" + delimiter;
            mEndOffset = start + 1;

            if (delimiter == ',')
                mType = NameTokenType.COMMA;
            else if (delimiter == '(' || delimiter == '[' || delimiter == '\u3010')
                mType = NameTokenType.OPEN;
            else if (delimiter == ')' || delimiter == ']' || delimiter == '\u3011')
                mType = NameTokenType.CLOSE;
            else
                mType = NameTokenType.DELIMITER;
        }

        private NameToken(String token, int start) {
            int tstart = 0, tend = token.length();
            // nuke paired quotes from front and end  of token
            while (tstart < tend && token.charAt(tstart) == '\'' && token.charAt(tend - 1) == '\'') {
                tstart++;  tend--;
            }
            // trim non-letter characters off the front of the token
            for (char c; tstart < tend && (!Character.isLetterOrDigit(c = token.charAt(tstart)) || c == '-') && c != '\''; tstart++)
                ;
            // trim non-letter characters (except . ' !) off the end of the token
            for (char c; tstart < tend && (!Character.isLetterOrDigit(c = token.charAt(tend - 1)) || c == '-') && c != '.' && c != '\'' && (tstart > 0 || c != '!'); tend--)
                ;
            // treat it as a delimiter if there were no letters in the token
            boolean delimiter = tstart >= tend;

            mValue = delimiter ? token : token.substring(tstart, tend);
            mEndOffset = start + token.length();

            int length = token.length();
            if (delimiter)
                mType = NameTokenType.DELIMITER;
            else if (SURNAME_PREFIXES.contains(token.toLowerCase()))
                mType = NameTokenType.PREFIX;
            else if (length <= 2 && (length == 1 || token.charAt(1) == '.') && Character.isLetter(token.charAt(0)))
                mType = NameTokenType.INITIAL;
            else if (ROMAN_NUMERALS.contains(token))
                mType = NameTokenType.NUMERAL;
            else if (isAbbreviation())
                mType = NameTokenType.ABBREVIATION;
            else if (length > 2 && token.charAt(length - 1) == '.')
                mType = NameTokenType.HONORIFIC;
            else
                mType = NameTokenType.TOKEN;
        }

        private boolean isAbbreviation() {
            if (mValue.length() % 2 == 1)
                return false;

            for (int i = 0; i < mValue.length(); i += 2) {
                if (mValue.charAt(i + 1) != '.' || !Character.isLetter(mValue.charAt(i)))
                    return false;
            }
            return true;
        }

        private static final Set<String> SURNAME_PREFIXES = new HashSet<String>(Arrays.asList(
                "ab", "abu", "af", "al", "am", "an", "ap", "auf", "av", "bar", "ben", "bin",
                "da", "de", "degli", "dei", "del", "della", "de'", "delle", "dello", "den",
                "der", "di", "dos", "du", "el", "groot", "groote", "het", "ibn", "la", "las",
                "le", "lo", "los", "mac", "n\u00cd", "nic", "op", "ten", "ter", "toe", "van",
                "vande", "vander", "vom", "von", "zu", "zum", "zur", "'t", "\u00d3"
        ));

        private static final Set<String> ROMAN_NUMERALS = new HashSet<String>(Arrays.asList(
                "II", "III", "IIII", "IV", "V", "VI", "VII", "VIII", "Jr"
        ));

        NameTokenType getType() {
            return mType;
        }

        int getEndOffset() {
            return mEndOffset;
        }

        @Override
        public String toString() {
            return mValue;
        }

        private static final Set<Character> TOKEN_DELIMITERS = new HashSet<Character>(Arrays.asList(
                ',', ';', '/', ':', '(', ')', '[', ']', '\u3010', '\u3011'
        ));

        static List<NameToken> tokenize(String raw) {
            if (raw == null)
                return null;

            raw = raw.trim().replaceAll("\\s+", " ");
            int start = 0, index = 0, length = raw.length();
            List<NameToken> tokens = new ArrayList<NameToken>(6);
            do {
                start = index;
                while (index < length) {
                    char c = raw.charAt(index++);
                    boolean delimiter = TOKEN_DELIMITERS.contains(c) || c == '\\' || c == '"' || Character.isWhitespace(c);
                    // check for end of a token!
                    if (delimiter || index >= length) {
                        // only leading dashes are treated as delimiters, so special-case here
                        if (raw.charAt(start) == '-')
                            tokens.add(new NameToken('-', start++));
                        // the part leading up to the delimiter is the token
                        int end = delimiter ? index - 1 : index;
                        if (end > start)
                            tokens.add(new NameToken(raw.substring(start, end), start));
                        // and certain delimiters are also considered tokens (but '\\' and '"' are just ignored)
                        if (TOKEN_DELIMITERS.contains(c))
                            tokens.add(new NameToken(c, index - 1));
                        break;
                    }
                }
                while (index < length && Character.isWhitespace(raw.charAt(index)))
                    index++;
            } while (index < length);

            return tokens;
        }

        @SuppressWarnings("unused")
        static String asTokenString(List<NameToken> tokens) {
            StringBuilder sb = new StringBuilder();
            for (NameToken token : tokens)
                sb.append(sb.length() == 0 ? "[" : ", ").append(token.getType());
            return sb.append(']').toString();
        }
    }
}

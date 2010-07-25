/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Jan 31, 2005
 */
package com.zimbra.cs.mime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.util.StringUtil;

public class ParsedAddress implements Comparable<ParsedAddress> {

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


    @Override public int hashCode() {
        if (emailPart != null)
            return emailPart.hashCode();
        else if (personalPart != null)
            return personalPart.hashCode();
        else
            return 0;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj == null || !(obj instanceof ParsedAddress))
            return false;
        else
            return compareTo((ParsedAddress) obj) == 0;
    }

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

    @Override public String toString() {
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

        @Override public String toString() {
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


    private static boolean runNameTest(String name, String description, String honorific, String first, String middle, String last, String suffix) {
        return runTest(new ParsedAddress(null, name).parse(), name, description, honorific, first, middle, last, suffix);
    }

    private static boolean runAddressTest(String addr, String description, String honorific, String first, String middle, String last, String suffix) {
        return runTest(new ParsedAddress(addr).parse(), addr, description, honorific, first, middle, last, suffix);
    }

    private static boolean runTest(ParsedAddress pa, String raw, String description, String honorific, String first, String middle, String last, String suffix) {
        boolean fail = false;
        if (honorific == null ^ pa.honorific == null || (honorific != null && !honorific.equals(pa.honorific)))
            fail = true;
        else if (first == null ^ pa.firstName == null || (first != null && !first.equals(pa.firstName)))
            fail = true;
        else if (middle == null ^ pa.middleName == null || (middle != null && !middle.equals(pa.middleName)))
            fail = true;
        else if (last == null ^ pa.lastName == null || (last != null && !last.equals(pa.lastName)))
            fail = true;
        else if (suffix == null ^ pa.suffix == null || (suffix != null && !suffix.equals(pa.suffix)))
            fail = true;

        if (fail) {
            System.out.println("failed test: " + description);
            System.out.println("  raw:      {" + raw + '}');
//            System.out.println("  tokens:   " + pa.tokentypes);
            System.out.println("  personal: {" + bformat(pa.personalPart) + '}');
            System.out.println("  actual:   |" + bformat(pa.honorific) + '|' + bformat(pa.firstName) + '|' + bformat(pa.middleName) + '|' + bformat(pa.lastName) + '|' + bformat(pa.suffix));
            System.out.println("  expected: |" + bformat(honorific) + '|' + bformat(first) + '|' + bformat(middle) + '|' + bformat(last) + '|' + bformat(suffix));
        }

        return !fail;
    }

    private static String bformat(String s) {
        if (s == null)
            return "null";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c < 0x7F)
                sb.append(c);
            else
                sb.append(String.format("\\u%04X", (int) c));
        }
        return sb.toString();
    }

    private static void runNameParserSuite(String first, String middle, String last) {
        runNameTest(first + ' ' + last,              "first last",       null,  first, null, last, null);
        runNameTest("Mr. " + first + ' ' + last,     "hon. first last",  "Mr.", first, null, last, null);
        runNameTest(first + ' ' + last + ", MD",     "first last, suf",  null,  first, null, last, "MD");
        runNameTest(first + ' ' + last + " III",     "first last num",   null,  first, null, last, "III");
        runNameTest(first + ' ' + last + " (dec'd)", "first last (suf)", null,  first, null, last, "dec'd");
        runNameTest(first + ' ' + last + " [CPA]",   "first last [suf]", null,  first, null, last, "CPA");

        if (middle != null) {
            runNameTest(first + ' ' + middle + ' ' + last,              "first middle last",       null,   first, middle, last, null);
            runNameTest("Gov. " + first + ' ' + middle + ' ' + last,    "hon. first middle last", "Gov.",  first, middle, last, null);
            runNameTest(first + ' ' + middle + ' ' + last + " M.D.",    "first middle last, abbr", null,   first, middle, last, "M.D.");
            runNameTest(first + ' ' + middle + ' ' + last + " III",     "first middle last num",   null,   first, middle, last, "III");
            runNameTest(first + ' ' + middle + ' ' + last + " (dec'd)", "first middle last (suf)", null,   first, middle, last, "dec'd");
            runNameTest(first + ' ' + middle + ' ' + last + " [CPA]",   "first middle last [suf]", null,   first, middle, last, "CPA");
        }

        runNameTest(last + ", " + first,              "last, first",       null,    first, null, last, null);
        runNameTest(last + ", Prof. " + first,        "last, hon. first",  "Prof.", first, null, last, null);
        runNameTest(last + ", " + first + ", M.D.",   "last, first, suf",  null,    first, null, last, "M.D.");
        runNameTest(last + ", " + first + " Jr.",     "last, first suf",   null,    first, null, last, "Jr.");
        runNameTest(last + ", " + first + " (dec'd)", "last, first (suf)", null,    first, null, last, "dec'd");
        runNameTest(last + ", " + first + " [CPA]",   "last, first [suf]", null,    first, null, last, "CPA");

        if (middle != null) {
            runNameTest(last + ", " + first + ' ' + middle,                "last, first middle",           null,  first, middle, last, null);
            runNameTest(last + ", Dr. " + first + ' ' + middle,            "last, hon. first middle",      "Dr.", first, middle, last, null);
            runNameTest(last + ", " + first + ' ' + middle + ", Ph.D.",    "last, first middle, suf",      null,  first, middle, last, "Ph.D.");
            runNameTest(last + ", " + first + ' ' + middle + " (dec'd)",   "last, first middle (suf)",     null,  first, middle, last, "dec'd");
            runNameTest(last + ", " + first + ' ' + middle + " [CPA]",     "last, first middle [suf]",     null,  first, middle, last, "CPA");
            runNameTest(last + ", " + first + ' ' + middle + " \\(ITC\\)", "last, first middle \\(suf\\)", null,  first, middle, last, "ITC");
        }
    }

//    private String tokentypes;

    public static void main(String... args) throws java.io.IOException {
        runNameParserSuite("Steven", "Paul", "Jobs");
        runNameParserSuite("Steven", "P.", "ben Jobs");
        runNameParserSuite("F. Steve", "Paul", "Jobs");
        runNameParserSuite("F. S.", "Paul", "Jobs");
        runNameParserSuite("Steven", "X. Paul", "van der Jobs");
        runNameParserSuite("S.P.", null, "Jobs");
        runNameParserSuite("S.", "van der Woz", "del Jobs");
        runNameParserSuite("F. S. P.", "van der Woz", "Jobs");

        if (false) {
            runAddressTest("     \"Della Gonzales\" <rvhxhjyjxugp@example.com>", "prefix lastname", null, "Della", null, "Gonzales", null);
            runAddressTest("     \"Stan O'Neal- Chairman, CEO & President\" <antifraudcentre@example.com>", "trailing dash is delimiter", null, "Stan", null, "O'Neal", "Chairman, CEO & President");
            runAddressTest("=?iso-2022-jp?B?GyRCIVo4N0EqPnBKcyFbGyhC?=<teresa@example.com>", "chinese square brackets", null, "\u53B3\u9078\u60C5\u5831", null, null, null);
            runAddressTest("=?iso-2022-jp?B?GyRCNkEbKEI=?=<lovecherry_hibiki_15@example.co.jp>", "1-character name", null, "\u97FF", null, null, null);
            runAddressTest("=?iso-2022-jp?B?GyRCQTBAbhsoQiAbJEI9YxsoQg==?=<maekawa_junjun.pickpick1976@example.ne.jp>", "asian last first", null, "\u7D14", null, "\u524D\u5DDD", null);
            runAddressTest("=?iso-2022-jp?B?IhskQjJPOWdARTlhGyhCIjxzd2VldF9zaXp1MDMwNUB5YWhvby5jby5qcD4=?=<sweet_sizu0305@example.co.jp>", "email in name", null, "\u6CB3\u5408\u9759\u9999", null, null, null);
            runAddressTest("=?iso-2022-jp?B?cGVlcmxlc3NfcF8wQHlhaG9vLmNvLmpw?=<peerless_p_0@example.co.jp>", "numeral is delimiter", null, "\u6CB3\u5408\u9759\u9999", null, null, null);
            runAddressTest("=?iso-8859-1?Q?Janine_Gr=FCndel_-Intergroove-?= <JanineGruendel@example.com>", "leading dash is delimiter", null, "Janine", null, "Gr\u00FCndel", "Intergroove");
            runAddressTest("=?koi8-r?B?5tXOy8PJySDXzi4gwdXEydTB?= <info@example.ru>", "honorific middle name", null, "\u0424\u0443\u043D\u043A\u0446\u0438\u0438", "\u0432\u043D.", "\u0430\u0443\u0434\u0438\u0442\u0430", null);
            runAddressTest("=?koi8-r?B?MjcgzcHRIDIwMDg=?= <webmaster@example.org>", "numeral not first name", null, "\u043C\u0430\u044F", null, null, null);
            runAddressTest("Dr R G Edwards <rich.edwards@example.com>", "honorific w/o dot", "Dr", "R G", null, "Edwards", null);
            runAddressTest("Emerich R Winkler Jr <erwinklerjr@example.ca>", "suffix honorific w/o dot", null, "Emerich", "R", "Winkler", "Jr");
            runAddressTest("Farai Aggrey jnr <faraiggrey@example.com>", "nonstandard suffix honorific w/o dot", null, "Farai", null, "Aggrey", "jnr");
            runAddressTest("Luigi Ottaviani T <luigi.ottaviani@example.it>", "initial (no comma) as suffix", null, "Luigi", null, "Ottaviani", "T");
            runAddressTest("MRS JENNIFER PETERS <jenniferpeters2000@example.com>", "honorific w/o dot", "MRS", "JENNIFER", null, "PETERS", null);
            runAddressTest("\" <ymskjetuiuod@example.ru>", "mis-quoted address", null, "ymskjetuiuod", null, null, null);
            runAddressTest("\":PROFECO\"@dogfood.zimbra.com: <buzon@example.mx>", "oddball quoting", null, "PROFECO@dogfood.zimbra.com", null, null, null);
            runAddressTest("\"=?BIG5?B?tO6qzi+rT7C3pOiqa7Ddp9o=?=\" <whyslim4@example.com>", "inlined slash as delimiter", null, "\u6E1B\u80A5", null, null, null);
            runAddressTest("\"=?ISO-8859-1?Q?Francisco_Gon=E7alves?=\" <francis.goncalves@example.com>", "embedded quoted 2047", null, "Francisco", null, "Gon\u00E7alves", null);
            runAddressTest("\"Anthony (Tony) Foiani\" <tfoiani@yahoo-example.com>", "nickname in parens", null, "Anthony", null, "Foiani", null);
            runAddressTest("\"B. Winston Sandage, III\" <wsandage@example.com>", "numeral in suffix", null, "B. Winston", null, "Sandage", "III");
            runAddressTest("\"Benjamin Wu \\(y!mail\\)\" <wub@example.com>", "extraneous backslashes", null, "Benjamin", null, "Wu", "y!mail");
            runAddressTest("\"Comercial Ideas & Dise=?iso-8859-1?b?8Q==?=o\" <ideasydiseno@example.es>", "ampersand means company", null, "Comercial", null, null, null);
            runAddressTest("\"Eli Stevens (Y!)\" <elis@example.com>", "trailing !", null, "Eli", null, "Stevens", "Y!");
            runAddressTest("\"Ewald de Bever (Mr),\"<verbever1@example.com>", "trailing comma", null, "Ewald", null, "de Bever", "Mr");
            runAddressTest("\"Flickerbox, Inc.\" <paul@example.com>", "Inc. means company", null, "Flickerbox", null, null, null);
            runAddressTest("\"Goligoski, Cory (MENLO PARK, CA)\" <cory_r_goligoski@example.com>", "retain delimiters in suffix", null, "Cory", null, "Goligoski", "MENLO PARK, CA");
            runAddressTest("\"Hans Th. Prade\" <h.prade@example.com>", "honorific middle name", null, "Hans", "Th.", "Prade", null);
            runAddressTest("\"J.P. Maxwell / tipit.net\" <jp@example.net>", ".net means company", null, "J.P.", null, "Maxwell", null);
            runAddressTest("\"John Katsaros -- Internet Research Group\" <jkatsar1169@example.net>", "multi-dashes as delimiter", null, "John", null, "Katsaros", "Internet Research Group");
            runAddressTest("\"Louis Ferreira - BCX - Microsoft Competency\" <Louis.Ferreira@example.co.za>", "retain delimiters in suffix", null, "Louis", null, "Ferreira", "BCX - Microsoft Competency");
            runAddressTest("\"Macworld Conference & Expo\" <macworldexpo@example.com>", "ampersand means company", null, "Macworld", null, null, null);
            runAddressTest("\"Montgomery Research: Midmarket Strategies\" <yms@example.com>", "colon as delimiter", null, "Montgomery", null, "Research", "Midmarket Strategies");
            runAddressTest("\"NG Jiang Hao (jianghao@mjm.com.sg)\" <Jianghao@example.com.sg>", "skip duplicate (addr)", null, "NG", "Jiang", "Hao", null);
            runAddressTest("\"Nuevora, Inc.\" <accountspayable@example.com>", "Inc. means company", null, "Nuevora", null, null, null);
            runAddressTest("\"Patric. W. Chan \" <wikster1@example.net>", "too-long honorific", null, "Patric.", "W.", "Chan", null);
            runAddressTest("\"Philip W. Dalrymple III\" <pwd@example.com>", "roman numeral", null, "Philip", "W.", "Dalrymple", "III");
            runAddressTest("\"Prof. Dr. Karl-Heinz Kunzelmann\" <karl-heinz@example.de>", "multiple honorifics", "Prof. Dr.", "Karl-Heinz", null, "Kunzelmann", null);
            runAddressTest("\"Regina Ciardiello, Editor, VSR\" <edgell@example.com>", "retain delimiters in suffix", null, "Regina", null, "Ciardiello", "Editor, VSR");
            runAddressTest("\"Rene' N. Godwin\" <godwin4@example.net>", "trailing apostrophe", null, "Rene'", "N.", "Godwin", null);
            runAddressTest("\"Reuven Cohen [ruvnet@gmail.com]\" <ruv@example.com>", "nonduplicate addr", null, "Reuven", null, "Cohen", "ruvnet@gmail.com");
            runAddressTest("\"Robert Q.H. Bui\" <robertb@example.com.au>", "middle name abbr", null, "Robert", "Q.H.", "Bui", null);
            runAddressTest("\"Sander Manneke | Internet Today\" <s.manneke@example.nl>", "bar as delimiter", null, "Sander", null, "Manneke", "Internet Today");
            runAddressTest("\"Scheffler, Avis @ -P-f-i-z-e-r Supplies\" <turryeaProducts.Registration5876@example.com>", "at-sign delimiter", null, "Avis", null, "Scheffler", "P-f-i-z-e-r Supplies");
            runAddressTest("\"SearchWebJobs .com\" <jobs@example.com>", ".com is company", null, "SearchWebJobs", null, null, null);
            runAddressTest("\"William J. Robb III, M.D.\" <wrobb@example.com>", "roman numeral", null, "William", "J.", "Robb", "III, M.D.");
        }

        runAddressTest("     \"Della Gonzales\" <rvhxhjyjxugp@example.com>", "prefix lastname", null, "Della", null, "Gonzales", null);
        runAddressTest("Emerich R Winkler Jr <erwinklerjr@example.ca>", "suffix honorific w/o dot", null, "Emerich", "R", "Winkler", "Jr");
        runAddressTest("\"B. Winston Sandage, III\" <wsandage@example.com>", "numeral in suffix", null, "B. Winston", null, "Sandage", "III");
        runAddressTest("\"Eli Stevens (Y!)\" <elis@example.com>", "trailing !", null, "Eli", null, "Stevens", "Y!");
        runAddressTest("\"Ewald de Bever (Mr),\"<verbever1@example.com>", "trailing comma", null, "Ewald", null, "de Bever", "Mr");
        runAddressTest("\"Goligoski, Cory (MENLO PARK, CA)\" <cory_r_goligoski@example.com>", "retain delimiters in suffix", null, "Cory", null, "Goligoski", "MENLO PARK, CA");
        runAddressTest("\"interWays e.K.\" <info@example.de>", "terminal abbr", null, "interWays", null, null, null);
        runAddressTest("\"John Katsaros -- Internet Research Group\" <jkatsar1169@example.net>", "multi-dashes as delimiter", null, "John", null, "Katsaros", "Internet Research Group");
        runAddressTest("\"Louis Ferreira - BCX - Microsoft Competency\" <Louis.Ferreira@example.co.za>", "retain delimiters in suffix", null, "Louis", null, "Ferreira", "BCX - Microsoft Competency");
        runAddressTest("\"Philip W. Dalrymple III\" <pwd@example.com>", "roman numeral", null, "Philip", "W.", "Dalrymple", "III");
        runAddressTest("\"Prof. Dr. Karl-Heinz Kunzelmann\" <karl-heinz@example.de>", "multiple honorifics", "Prof. Dr.", "Karl-Heinz", null, "Kunzelmann", null);
        runAddressTest("\"Rene' N. Godwin\" <godwin4@example.net>", "drailing apostrophe", null, "Rene'", "N.", "Godwin", null);
        runAddressTest("\"Robert Q.H. Bui\" <robertb@example.com.au>", "middle name abbr", null, "Robert", "Q.H.", "Bui", null);
        runAddressTest("\"William J. Robb III, M.D.\" <wrobb@example.com>", "roman numeral", null, "William", "J.", "Robb", "III, M.D.");

        // "Howard J. Bleier \"J.R.\"" <hjbleier@example.com>
        // "Melissa Jill :)" <melissajill@example.com>

        if (args.length == 1) {
            java.io.File oldFile = new java.io.File(args[0] + ".out");
            if (oldFile.exists())
                oldFile.renameTo(new java.io.File(args[0] + "-" + (System.currentTimeMillis() / 1000) + ".out"));

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(args[0]));
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(new java.io.FileOutputStream(args[0] + ".out"));
            String address;
            while ((address = reader.readLine()) != null) {
                ParsedAddress pa = new ParsedAddress(address).parse();
                Map<String, String> attrs = pa.getAttributes();
//                Map<String, String> attrsOld = new ParsedAddressOld(address).getAttributes();
//                if (!attrs.toString().equals(attrsOld.toString())) {
//                    System.out.println(address + ": ");
//                    System.out.println("   new: " + attrs);
//                    System.out.println("   old: " + attrsOld);
//                }
                writer.append(address).append('\t');
//                writer.append(pa.tokentypes).append('\t');
                writer.append(bformat(attrs.toString())).append('\n');
            }
            writer.close();
        }
    }
}

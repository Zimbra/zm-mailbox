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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.mailbox.ContactConstants;


public class ParsedAddress implements Comparable<ParsedAddress> {

    private static String  HONORIFIC = "([^,\\.\\s]{2,}\\.\\s+)?";
    private static String  INITIAL = "(?:[^,\\s]\\.\\s*)";
    private static String  FIRST_NAME = "(" + INITIAL + "{2,}|" + INITIAL + "?[^,\\s]+)";
    private static String  MIDDLE_NAME = "[^,(;{\\[]*";
    private static String  LAST_NAME = "(\\S+)";
    private static Pattern NAME_SPACE_PATTERN = Pattern.compile(HONORIFIC + FIRST_NAME + "\\s+(.*)");
    private static Pattern COMMA_NAME_PATTERN = Pattern.compile(LAST_NAME + ",\\s*(" + HONORIFIC + FIRST_NAME + MIDDLE_NAME + ")(.*)");

	public String emailPart;
	public String personalPart;
    public String honorific;
    public String firstName;
    public String lastName;
    public String suffix;

    private boolean parsed = false;
    
	public ParsedAddress(String address) {
		try {
            InternetAddress ia = new InternetAddress(address); 
			initialize(ia.getAddress(), ia.getPersonal());
		} catch (AddressException ae) {
			personalPart = address;
		}
	}
    public ParsedAddress(InternetAddress ia) {
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
        lastName     = node.lastName;
        suffix       = node.suffix;
        parsed = node.parsed;
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
        if (lastName != null)      map.put(ContactConstants.A_lastName, lastName);
        if (personalPart != null)  map.put(ContactConstants.A_fullName, personalPart);
        if (emailPart != null)     map.put(ContactConstants.A_email, emailPart);
        return map;
    }

    public ParsedAddress parse() {
        if (parsed)
            return this;

        if (emailPart != null && emailPart.equals(personalPart))
            personalPart = null;
        if (personalPart != null) {
            if (personalPart.indexOf(' ') == -1 && personalPart.indexOf(',') == -1)
                firstName = personalPart;
            else {
                Matcher m = NAME_SPACE_PATTERN.matcher(personalPart);
                if (m.matches()) {
                    honorific = m.group(1);
                    firstName = m.group(2).trim();
                    lastName = m.group(3).trim();
                } else {
                    m = COMMA_NAME_PATTERN.matcher(personalPart);
                    if (m.matches()) {
                        honorific = m.group(3);
                        firstName = m.group(4);
                        lastName = m.group(1);
                        personalPart = m.group(2).trim() + ' ' + m.group(1);
                        String remainder = m.group(5);
                        if (remainder != null && !remainder.equals("")) {
                            if (!remainder.startsWith(",") && !remainder.startsWith(";"))
                                personalPart += ' ';
                            personalPart += remainder;
                        }
                    }
                }
            }
        }
        if (emailPart != null && firstName == null) {
            int p = emailPart.indexOf('@');
            if (p != -1) {
                String formatted = emailPart.substring(0, p).replace('.', ' ').replace('_', ' ');
                int space = formatted.indexOf(' ');
                firstName = space == -1 ? formatted : formatted.substring(0, space);
                if (space != -1 && personalPart == null)
                    personalPart = formatted;
            }
        }
        parsed = true;
        return this;
    }

    public boolean equals(Object obj) {
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

    @Override
    public String toString() {
        if (emailPart == null)
            return personalPart;
        else if (personalPart == null)
            return emailPart;
        else
        	return '"' + personalPart + "\" <" + emailPart + '>';
    }
}
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver;

import java.util.Map;
import java.util.HashMap;

public class LmtpAddress {

    private boolean mIsValid;
    private String mLocalPart;
    private String mNormalizedLocalPart;
    private String mDomainPart;
    private Map<String, String> mParameters;
    private LmtpReply mDeliveryReply;
    private String mEmailAddress;
    private String[] mAllowedParameters;
    
    private boolean mSkipFilters = false;
    private String mFolder = null;
    private String mFlags = null;
    private String mTags = null;

    private boolean mOnLocalServer = true;
    private String mRemoteServer; // if mOnLocalServer is false

    public LmtpAddress(String arg, String[] allowedParameters, String rcptDelim) {
	mAllowedParameters = allowedParameters;
	mParameters = new HashMap<String, String>();
	mIsValid = parse(arg);
	mDeliveryReply = LmtpReply.TEMPORARY_FAILURE;

	if (!mIsValid) {
	    return;
	}

	int delimIndex = -1;
	if (mLocalPart != null && rcptDelim != null && (delimIndex = mLocalPart.indexOf(rcptDelim)) > 0) {
	    // NB: > 0 makes sure that if the first character is the extension
	    // characater we do try to remove extension.
	    mNormalizedLocalPart = mLocalPart.substring(0, delimIndex); 
	} else {
	    mNormalizedLocalPart = mLocalPart;
	}

	int l1 = (mLocalPart != null) ? mLocalPart.length() : 0; 
	int l2 = (mDomainPart != null) ? mDomainPart.length() : 0;
	StringBuilder sb = new StringBuilder(l1 + l2 + 1);
	if (mNormalizedLocalPart != null) {
	    sb.append(mNormalizedLocalPart);
	}
	if (mDomainPart != null) {
	    sb.append("@").append(mDomainPart);
	}
	mEmailAddress = sb.toString();
    }

    public void setSkipFilters(boolean skip) {
	mSkipFilters = skip;
    }
    
    public boolean getSkipFilters() {
	return mSkipFilters;
    }
    
    public void setFolder(String folder) {
	mFolder = folder;
    }
    
    public String getFolder() {
	return mFolder;
    }
    
    public void setFlags(String flags) {
	mFlags = flags;
    }
    
    public String getFlags() {
	return mFlags;
    }
    
    public void setTags(String tags) {
	mTags = tags;
    }
    
    public String getTags() {
	return mTags;
    }
    
    public String getEmailAddress() {
	return mEmailAddress;
    }

    public String getLocalPart() {
	return mLocalPart;
    }

    public String getNormalizedLocalPart() {
	return mNormalizedLocalPart;
    }

    public String getDomainPart() {
	return mDomainPart;
    }

    public boolean isValid() {
	return mIsValid;
    }

    public Map<String, String> getParameters() {
	return mParameters;
    }

    public String getParameter(String key) {
	if (mParameters.isEmpty()) {
	    return null;
	}
	return (String)mParameters.get(key.toUpperCase());
    }

    public LmtpReply getDeliveryStatus() {
	return mDeliveryReply;
    }

    public void setDeliveryStatus(LmtpReply reply) {
	mDeliveryReply = reply;
    }

    /**
     * 'offset' is the *index* in the array of the next char that we
     * have not processed (other than lookahead).  Alternately, this
     * is also the length of the part that we have already processed
     * (think about it whichever way works for you).  For instance if
     * the content is:
     *
     *       <x@y.com>
     *       012345678
     *
     * After local part processing, offset should be 2.  After
     * domain parsing, offset should be 8.
     *
     * Yes, the naming convention here takes a break from 'm'
     * prefixing.  It's just more readable this way.
     */
    private int offset;
    private int length;
    private char[] array;
    private static final boolean debug = false;

    private static void say(String s) {
	System.out.println("  [debug] " + s);
    }

    private boolean eos() {
	return offset >= length;
    }

    /**
     * Return the next unprocessed character and advance the offset.
     */
    private int next() {
	if (offset < length) {
	    return array[offset++];
	} else {
	    return -1;
	}
    }

    /**
     * Return the next unprocessed character, but do NOT advance the
     * offset.
     *
     * When checking for the presence of something optional,
     * ie it may or may not be there, use peek()
     */
    private int peek() {
	if (offset < length) {
	    return array[offset];
	} else {
	    return -1;
	}
    }

    /**
     * Advance the offset by one.
     *
     * If the optional item was there, and you noticed it via peek(),
     * you might want to skip it.
     */
    private void skip() {
	offset++;
    }


    private void init(String p) {
	array = p.toCharArray();
	length = array.length;
	offset = 0;
    }

    private boolean parse(String p) {
	int ch;
	if (p == null || p.length() < 2) {
	    // atleast "<>" (length 2) required
	    return false;
	}

	init(p);

	/* Skip any white space, being liberal in what we accept */
	skipSpaces();

	/* Check starts with '<' */
	ch = next();
	if (ch == -1 || ch != '<') {
	    if (debug) say("does not begin with <");
	    return false;
	}

	/* Strip out source routes */
	if (!skipSourceRoutes()) {
	    if (debug) say("error in source route");
	    return false;
	}

	/* Parse local part of address (the stuff before '@') */
	if (!parseLocalPart()) {
	    if (debug) say("error in local part");
	    return false;
	}

	/* Check for an optional '@' */
	ch = peek(); 
	if (ch == '@' && !parseDomainPart()) {
	    if (debug) say("error in domain part");
	    return false;
	}

	/* Check address is finished with a '>' */
	ch = next();
	if (ch != '>') {
	    if (debug) say("does not end with >");
	    return false;
	}

	/* Check if there are any parameters */
	if (eos()) {
	    if (debug) say("no parameters that's ok");
	    return true;
	}

	ch = peek();
	if (ch != ' ') {
	    if (debug) say("there should be a space after the address");
	    return false;
	}

	/* Skip any white space after the address */
	skipSpaces();

	/* Parse parameters */
	while (!eos()) {
	    if (!parseParameter()) {
		if (debug) say("error in parameter");
		return false;
	    }
	    skipSpaces();
	}

	return true;
    }


    private void skipSpaces() {
	while (peek() == ' ') skip();
    }

    /**
     * Skip past source routes (ie, ignore them), return false if
     * invalid source route(s) present.
     *
     * Example of what is skipped from RFC 2821/Section 4.1.1.3:
     *
     * RCPT TO:<@hosta.int,@jkl.org:userc@d.bar.org>
     * _________^^^^^^^^^^^^^^^^^^^^________________
     */
    private boolean skipSourceRoutes() {
	if (debug) say("processing source routes");

	int ch = peek();
	if (ch != '@') {
	    if (debug) say("no source routes present");
	    return true;
	}
	skip(); // the '@'

	while (ch == '@') {
	    ch = peek();
	    if (ch == '[') {
		if (!skipAddress()) {
		    if (debug) say("error in address processing");
		    return false;
		}
	    } else {
		if (!skipHostname()) {
		    if (debug) say("error in hostname processing");
		    return false;
		}
	    }

	    ch = next();
	    if (ch == ',') {
		ch = next();
		if (debug) say("comma found processing next source route");
		continue;
	    } else if (ch == ':') {
		ch = peek();
		if (ch ==  '@') {
		    if (debug) say("colon-at processing another source route");
		    skip(); // the '@'
		    continue;
		} else {
		    if (debug) say("reached end of source routes");
		    break;
		}
	    } else {
		return false;
	    }
	}

	if (debug) say("successfully processed source routes");
	return true;
    }

    private boolean isDigit(int c) {
	return c >= '0' && c <= '9';
    }

    private boolean isLetter(int c) {
	return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean skipAddress() {
	if (debug) say("processing an address");
	int ch = next();
	if (ch != '[') {
	    if (debug) say("address did not start with [");
	    return false;
	}

	do {
	    ch = next();
	} while (isDigit(ch) || ch == '.');

	if (ch != ']') {
	    if (debug) say("address did not end with [");
	    return false;
	}

	if (debug) say("successfully processed an address");
	return true;
    }

    private boolean skipHostname() {
	if (debug) say("processing a hostname");
	while (true) {
	    int ch = peek();
	    if (isDigit(ch) || isLetter(ch) || ch == '.' || ch == '-') {
		skip();
		continue;
	    } else {
		break;
	    }
	}
	return true;
    }


    private boolean parseLocalPart() {
	int ch = peek();
	if (ch == '"') {
	    return parseQuotedLocalPart();
	} else {
	    return parsePlainLocalPart();
	}
    }

    private boolean parseQuotedLocalPart() {
	if (debug) say("parsing quoted local part");
	int soffset = offset;

	int ch = next();
	if (ch != '"') {
	    if (debug) say("quoted string does not begin with a quote");
	    return false;
	}

	while (!eos()) {
	    ch = next();
	    if (ch == '\\') {
		ch = next();
		if (ch == -1) {
		    if (debug) say("escape at end of string");
		    return false;
		}
	    } else if (ch == '"') {
		mLocalPart = new String(array, soffset, offset - soffset);
		if (debug) say("successfully processed quoted local part");
		return true;
	    }
	}

	if (debug) say("no end of quote found");
	return false;
    }

    private boolean parsePlainLocalPart() {
	if (debug) say("parsing plain local part");
	int soffset = offset;

	while (!eos()) {
	    int ch = peek();
	    /*
	     * <c> ::= any one of the 128 ASCII characters, but not
	     *         any <special> or <SP>
	     * 
	     * <special> ::= "<" | ">" | "(" | ")" | "[" | "]" | "\" | "."
	     *               | "," | ";" | ":" | "@"  """ | the control
	     *               characters (ASCII codes 0 through 31 inclusive
	     *               and 127)
	     */
	    if (ch < 33 || ch > 126) { // 32 is ' '
		if (debug) say("illegal character < 33 or > 126");
		return false; // any one of the 128 ascii characters
	    }

	    if ("<()[]\\,;:\"".indexOf(ch) > -1) {
		/* Left out '>' and '@' which are terminators in this
		 * context.  Also '.' is valid - there is more to the
		 * grammar than quoted above. */
		if (debug) say("special character found");
		return false;
	    }

	    if (ch == '@' || ch == '>') {
		mLocalPart = new String(array, soffset, offset - soffset);
		if (debug) say("successfully processed plain local part");
		return true;
	    }

	    skip();
	}

	/* Only happens if we abruptly reached end of string.
	 * Caller's responsibility to make sure that there is the
	 * termination character of their choice at the end.
	 */
	mLocalPart = new String(array, soffset, offset - soffset);
	if (debug) say("processed plain local part but reached eos");
	return true;
    }

    private boolean parseDomainPart() {
	if (debug) say("parsing domain part");
	int soffset;
	int ch;

	ch = next();
	if (ch != '@') {
	    return false;
	}

	soffset = offset; // don't do -1 here because we want to skip the @

	ch = peek();
	if (ch == '[') {
	    if (!skipAddress()) {
		return false;
	    }
	} else {
	    if (!skipHostname()) {
		return false;
	    }
	}

	mDomainPart = new String(array, soffset, offset - soffset);
	return true;
    }

    private boolean parseParameter() {
	if (debug) say("parsing parameter");

	String key = null;
	int koffset = offset;
	while (!eos()) {
	    int ch = next();
	    if (ch == '=') {
		key = new String(array, koffset, offset - koffset - 1); // -1 for the =
		if (allowedParameter(key)) {
		    break;
		} else {
		    return false;
		}
	    }
	}
	if (key == null) {
	    if (debug) say("eos while looking for = at the end of a parameter name");
	    return false;
	}

	int voffset = offset;
	while (true) {
	    int ch = peek();
	    if (ch == -1 || ch == ' ') {
		break;
	    }
	    skip();
	}
	String value = new String(array, voffset, offset - voffset);
	mParameters.put(key.toUpperCase(), value);
	if (debug) say("parameter accepted key=" + key + " value=" + value);
	return true;
    }

    private boolean allowedParameter(String key) {
	if (mAllowedParameters == null) {
	    if (debug) say("no paramters allowed in this context");
	    return false;
	}
	if (debug) say("checking " + mAllowedParameters.length + " allowed parameters");
	for (int i = 0; i < mAllowedParameters.length; i++) {
	    if (debug) say("checking key " + key + " against allowed " + mAllowedParameters[i]);
	    if (key.equalsIgnoreCase(mAllowedParameters[i])) {
		if (debug) say("parameter " + key + " is allowed");
		return true;
	    }
	}
	if (debug) say("parameter " + key + " is not allowed");
	return false;
    }

    public String toString() {
	return mEmailAddress;
    }

    public boolean isOnLocalServer() {
    return mOnLocalServer;
    }

    public void setOnLocalServer(boolean onLocalServer) {
    this.mOnLocalServer = onLocalServer;
    }

    public String getRemoteServer() {
    return mRemoteServer;
    }

    public void setRemoteServer(String remoteServer) {
    this.mRemoteServer = remoteServer;
    }
}

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

/**
 * Java fluff for an enum for the 2 things in:
 * 
 * 		RFC 1652 - SMTP Service Extension for 8bit-MIMEtransport
 */
public final class LmtpBodyType {
	private String mType;
	
	private LmtpBodyType(String type) {
		mType = type;
	}
	
	public String toString() {
		return mType;
	}
	
	public static final LmtpBodyType BODY_7BIT = new LmtpBodyType("7BIT");
	public static final LmtpBodyType BODY_8BITMIME = new LmtpBodyType("8BITMIME");
	
	public static LmtpBodyType getInstance(String type) {
		if (type.equalsIgnoreCase(BODY_7BIT.toString())) {
			return BODY_7BIT;
		}
		if (type.equalsIgnoreCase(BODY_8BITMIME.toString())) {
			return BODY_8BITMIME;
		}
		return null;
	}
}
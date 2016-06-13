/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
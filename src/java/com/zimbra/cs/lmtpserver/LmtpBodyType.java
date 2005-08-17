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
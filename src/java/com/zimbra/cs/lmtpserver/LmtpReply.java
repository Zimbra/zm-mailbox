/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010 Zimbra, Inc.
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


public enum LmtpReply {
    
    OK(250, "2.0.0", "OK"),
    SENDER_OK(250, "2.0.0", "Sender OK"),
    RECIPIENT_OK(250, "2.1.5", "Recipient OK"),
    DELIVERY_OK(250, "2.1.5", "Delivery OK"),
    OK_TO_SEND_DATA(354, null, "End data with <CR><LF>.<CR><LF>"),
    USE_RCPT_INSTEAD(252, "2.3.3", "Use RCPT to deliver messages"),

    BYE(221, null, new DetailCB() { protected String detail() { return LmtpConfig.getInstance().getGoodbye(); }}),
    GREETING(220, null, new DetailCB() { protected String detail() { return LmtpConfig.getInstance().getGreeting(); }}),
    
    SERVICE_DISABLED(421, "4.3.2", "Service not available, closing transmission channel"),
    MAILBOX_DISABLED(450, "4.2.1", "Mailbox disabled, not accepting messages"),
    MAILBOX_NOT_ON_THIS_SERVER(450, "4.2.0", "Mailbox is not on this server"),
    TEMPORARY_FAILURE(451, "4.0.0", "Temporary message delivery failure try again"),
    TEMPORARY_FAILURE_OVER_QUOTA(452, "4.2.2", "Over quota"),
    TIMEOUT(421, null, new DetailCB() { protected String detail() { return LmtpConfig.getInstance().getDescription() + " Timeout exceeded"; } }),
    
    NESTED_MAIL_COMMAND(503, "5.5.1", "Nested MAIL command"),
    NO_RECIPIENTS(503, "5.5.1", "No recipients"),
    MISSING_MAIL_TO(503, "5.5.1", "Need MAIL command"),
    SYNTAX_ERROR(500, "5.5.2", "Syntax error"),
    INVALID_RECIPIENT_ADDRESS(500, "5.5.2", "Syntax error in recipient address"),
    INVALID_SENDER_ADDRESS(501, "5.5.4", "Syntax error in sender address"),
    INVALID_BODY_PARAMETER(501, "5.5.4", "Syntax error in BODY parameter"),
    INVALID_SIZE_PARAMETER(501, "5.5.4", "Syntax error in SIZE parameter"),

    NO_SUCH_USER(550, "5.1.1", "No such user here"),
    PERMANENT_FAILURE_OVER_QUOTA(552, "5.2.2", "Over quota"),
    PERMANENT_FAILURE(554, "5.0.0", "Permanent message delivery failure");
    
    private int mCode;
    private String mEnhancedCode;
    private String mDetail;
    private DetailCB mDetailCallback;
    
    private abstract static class DetailCB {
	protected abstract String detail();
    }
    
    private LmtpReply(int code, String enhancedCode, String detail) {
	mCode = code;
	mEnhancedCode = enhancedCode;
	mDetail = detail;
    }

    private LmtpReply(int code, String enhancedCode, DetailCB detail) {
	mCode = code;
	mEnhancedCode = enhancedCode;
	mDetailCallback = detail;
    }
    
    public String toString() {
	String detail;
	if (mDetailCallback != null)
	    detail = mDetailCallback.detail();
	else
	    detail = mDetail;
	if (mEnhancedCode == null) 
	    return mCode + " " + detail; 
	else
	    return mCode + " " + mEnhancedCode + " " + detail;
    }

    public boolean success() {
	if (mCode > 199 && mCode < 400)
	    return true;
	return false;
    }
}

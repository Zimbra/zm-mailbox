/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.smtp;

/**
 * SMTP server error codes.
 */
public final class ReplyCode {
    public static final int COMMAND_SYNTAX_ERROR = 500;
    public static final int PARAMETER_SYNTAX_ERROR = 501;

    public static final int COMMAND_NOT_IMPLEMENTED = 502;
    public static final int BAD_COMMAND_SEQUENCE = 503;
    public static final int PARAMETER_NOT_IMPLEMENTED = 504;

    public static final int SYSTEM_STATUS = 211;
    public static final int HELP_MESSAGE = 214;
    
    public static final int SERVICE_READY = 220;
    public static final int SERVICE_CLOSING_CHANNEL = 221;
    public static final int SERVICE_UNAVAILABLE = 421;
    
    public static final int REQUEST_COMPLETED = 250;
    public static final int USER_NOT_LOCAL_WILL_FORWARD = 251;
    public static final int CANNOT_VRFY_USER = 252;
    public static final int MAILBOX_BUSY = 450;
    public static final int MAILBOX_NOT_FOUND = 550;
    public static final int REQUEST_ABORTED = 451;
    public static final int USER_NOT_LOCAL = 551;
    public static final int INSUFFICIENT_SYSTEM_STORAGE = 452;
    public static final int EXCEEDED_STORAGE_ALLOCATION = 552;
    public static final int MAILBOX_NAME_NOT_ALLOWED = 553;

    public static final int START_MAILBOX_INPUT = 354;
    public static final int TRANSACTION_FAILED = 554;

    /**
     * Returns <tt>true</tt> if the specified reply code indicates a 
     * Positive Preliminary (<b>1xx</b>) reply:
     *
     * <blockquote>
     *    <i>"The command has been accepted, but the requested action is being
     *    held in abeyance, pending confirmation of the information in this
     *    reply.  The SMTP client should send another command specifying
     *    whether to continue or abort the action.  Note: unextended SMTP
     *    does not have any commands that allow this type of reply, and so
     *    does not have continue or abort commands."</i>
     * </blockquote>
     *
     * @param code the reply code
     * @return <tt>true</tt> if a Positive Preliminary reply, <tt>false</tt> otherwise
     */
    public boolean isPreliminarySuccess(int code) {
        return code / 100 == 1;
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates a Positive
     * Completion (<b>2xx</b>) reply:
     *
     * <blockquote>
     *    <i>"The requested action has been successfully completed.  A new
     *    request may be initiated."</i>
     * </blockquote>
     *
     * @param code the request code 
     * @return <tt>true</tt> if a Positive Completion reply, <tt>false</tt> otherwise
     */
    public boolean isSuccess(int code) {
        return code / 200 == 1;
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates an Positive
     * Intermediate (<b>3xx</b>) reply:
     *
     * <blockquote>
     *    <i>"The command has been accepted, but the requested action is being
     *    held in abeyance, pending receipt of further information.  The
     *    SMTP client should send another command specifying this
     *    information.  This reply is used in command sequence groups (i.e.,
     *    in DATA)."</i>
     * </blockquote>
     *
     * @param code the reply code
     * @return <tt>true</tt> if a Positive Intermediate reply, <tt>false</tt> otherwise
     */
    public boolean isIntermediateSuccess(int code) {
        return code / 300 == 1;
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates a Transient
     * Negative Completion (<b>4xx</b>) reply:
     *
     * <blockquote>
     *  <i>"The command was not accepted, and the requested action did not
     *  occur.  However, the error condition is temporary and the action
     *  may be requested again.  The sender should return to the beginning
     *  of the command sequence (if any).  It is difficult to assign a
     *  meaning to "transient" when two different sites (receiver- and
     *  sender-SMTP agents) must agree on the interpretation.  Each reply
     *  in this category might have a different time value, but the SMTP
     *  client is encouraged to try again.  A rule of thumb to determine
     *  whether a reply fits into the 4yz or the 5yz category (see below)
     *  is that replies are 4yz if they can be successful if repeated
     *  without any change in command form or in properties of the sender
     *  or receiver (that is, the command is repeated identically and the
     *  receiver does not put up a new implementation.)"</i>
     * </blockquote>
     *
     * @param code the request code
     * @return <tt>true</tt> if a Transient Negative Completion reply,
     *         <tt>false</tt> otherwise
     */
    public boolean isTransientFailure(int code) {
        return code / 400 == 1;
    }

    /**
     * Returns <tt>true</tt> if the specified result code indicates a Permanent
     * Negative Completion (<b>5xx</b>) reply:
     *
     * <blockquote>
     * <i>"The command was not accepted and the requested action did not
     *  occur.  The SMTP client is discouraged from repeating the exact
     * request (in the same sequence).  Even some "permanent" error
     * conditions can be corrected, so the human user may want to direct
     * the SMTP client to reinitiate the command sequence by direct
     * action at some point in the future (e.g., after the spelling has
     * been changed, or the user has altered the account status)."</i>
     * </blockquote>
     *
     * @param code the reply code
     * @return <tt>true</tt> if a Permanent Negative Completion reply,
     *         <tt>false</tt> otherwise
     */
    public boolean isPermanentFailure(int code) {
        return code / 500 == 1;
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates a syntax
     * error.
     * 
     * @param code the reply code
     * @return <tt>true</tt> if syntax error, <tt>false</tt> otherwise
     */
    public boolean isSyntaxCategory(int code) {
        return (code / 10) % 10 == 0; 
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates an
     * informational reply.
     *
     * @param code the result code
     * @return <tt>true</tt> if informational message, <tt>false</tt> otherwise
     */
    public boolean isInformationCategory(int code) {
        return (code / 10) % 10 == 1;
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates a
     * transmission channel related reply.
     * 
     * @param code the reply code
     * @return <tt>true</tt> if transmission channel reply, <tt>false</tt> otherwise
     */
    public boolean isConnectionsCategory(int code) {
        return (code / 10) % 10 == 2;
    }

    /**
     * Returns <tt>true</tt> if the specified reply code indicates a mail
     * system related reply.
     *
     * @param code the reply code
     * @return <tt>true</tt> if mail system related reply, <tt>false</tt> otherwise
     */
    public boolean isMailSystemCategory(int code) {
        return (code / 10) % 10 == 5;
    }
}

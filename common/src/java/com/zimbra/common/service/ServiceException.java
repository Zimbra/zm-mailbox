/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on Jun 1, 2004
 *
 */
package com.zimbra.common.service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.StringUtil;

@SuppressWarnings("serial")
public class ServiceException extends Exception {

    public static final String FAILURE = "service.FAILURE";
    public static final String INVALID_REQUEST = "service.INVALID_REQUEST";
    public static final String UNKNOWN_DOCUMENT = "service.UNKNOWN_DOCUMENT";
    public static final String PARSE_ERROR = "service.PARSE_ERROR";
    public static final String RESOURCE_UNREACHABLE = "service.RESOURCE_UNREACHABLE";
    public static final String TEMPORARILY_UNAVAILABLE = "service.TEMPORARILY_UNAVAILABLE";
    public static final String PERM_DENIED = "service.PERM_DENIED";
    public static final String AUTH_REQUIRED = "service.AUTH_REQUIRED";
    public static final String CANNOT_DISABLE_TWO_FACTOR_AUTH = "account.CANNOT_DISABLE_TWO_FACTOR_AUTH";
    public static final String CANNOT_ENABLE_TWO_FACTOR_AUTH = "account.CANNOT_ENABLE_TWO_FACTOR_AUTH";
    public static final String AUTH_EXPIRED = "service.AUTH_EXPIRED";
    public static final String WRONG_HOST = "service.WRONG_HOST";
    public static final String NON_READONLY_OPERATION_DENIED = "service.NON_READONLY_OPERATION_DENIED";
    public static final String PROXY_ERROR = "service.PROXY_ERROR";
    public static final String TOO_MANY_HOPS = "service.TOO_MANY_HOPS";
    public static final String ALREADY_IN_PROGRESS = "service.ALREADY_IN_PROGRESS";
    public static final String NOT_IN_PROGRESS = "service.NOT_IN_PROGRESS";
    public static final String INTERRUPTED = "service.INTERRUPTED";
    public static final String NO_SPELL_CHECK_URL = "service.NO_SPELL_CHECK_URL";
    public static final String SAX_READER_ERROR = "service.SAX_READER_ERROR";
    public static final String UNSUPPORTED = "service.UNSUPPORTED";
    public static final String FORBIDDEN = "service.FORBIDDEN";
    // generic "not found" error for objects other than mail items
    public static final String NOT_FOUND = "service.NOT_FOUND";
    public static final String LOCK_FAILED = "mail.LOCK_FAILED";
    public static final String INTERNAL_ERROR = "service.INTERNAL_ERROR";
    public static final String INVALID_DATASOURCE_ID = "service.INVALID_DATASOURCE_ID";
    public static final String DATASOURCE_SMTP_DISABLED = "service.DATASOURCE_SMTP_DISABLED";
    public static final String ERROR_WHILE_PARSING_UPLOAD = "service.IOEXCEPTION_WHILE_PARSING_UPLOAD";

    //smime
    public static final String LOAD_CERTIFICATE_FAILED = "smime.LOAD_CERTIFICATE_FAILED";
    public static final String LOAD_PRIVATE_KEY_FAILED = "smime.LOAD_PRIVATE_KEY_FAILED";
    public static final String USER_CERT_MISMATCH = "smime.USER_CERT_MISMATCH";
    public static final String DECRYPTION_FAILED = "smime.DECRYPTION_FAILED";
    public static final String OPERATION_DENIED = "smime.OPERATION_DENIED";
    public static final String FEATURE_SMIME_DISABLED = "smime.FEATURE_SMIME_DISABLED";

    public static final String ZIMBRA_NETWORK_MODULES_NG_ENABLED = "extension.ZIMBRA_NETWORK_MODULES_NG_ENABLED";

    protected String mCode;
    private List<Argument> mArgs;
    private String mId;
    private String mThreadName;

    public static final String HOST                  = "host";
    public static final String URL                   = "url";
    public static final String MAILBOX_ID            = "mboxId";
    public static final String ACCOUNT_ID            = "acctId";
    public static final String TWO_FACTOR_AUTH_TOKEN = "twoFactorAuthToken";

    public static final String PROXIED_FROM_ACCT  = "proxiedFromAcct"; // exception proxied from remote account

    // to ensure that exception id is unique across multiple servers.
    private static String ID_KEY = null;

    static {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[8];
        random.nextBytes(key);
        ID_KEY = new String(Hex.encodeHex(key));
    }


    @Override public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        toRet.append("\nExceptionId:").append(mId);
        toRet.append("\nCode:").append(mCode);
        if (mArgs != null) {
            for (Argument arg : mArgs) {
                toRet.append(" Arg:").append(arg.toString()).append("");
            }
        }

        return toRet.toString();
    }

    public static class Argument {
        public static enum Type {
            IID,       // mail-item ID or UUID or mailbox-id
            ACCTID,    // account ID
            STR,       // opaque string
            NUM,       // opaque number
        }

        public Argument(String name, String value, Type type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public Argument(String name, long value, Type type) {
            this.name = name;
            this.value = Long.toString(value);
            this.type = type;
        }

        public boolean externalVisible() {
            return true;
        }

        public final String name;
        public final String value;
        public final Type type;

        @Override public String toString() {
            return "(" + name + ", " + type.name() + ", \"" + value + "\")";
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null && hashCode() == obj.hashCode());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name, value, type);
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Argument that should not be included in SOAP fault.
     * For example: url
     */
    public static class InternalArgument extends Argument {
        public InternalArgument(String name, String value, Type typ) {
            super(name, value, typ);
        }

        public InternalArgument(String name, long value, Type type) {
            super(name, value, type);
        }

        @Override public boolean externalVisible() {
            return false;
        }
    }

    /**
     * Comment for <code>mReceiver</code>
     *
     * Causes a Sender/Receiver element to show up in a soap fault. It is supposed to let the client know whether it
     * did something wrong or something is wrong on the server.

     * For example, ServiceException.FAILURE sets it to true, meaning something bad happened on the server-side and
     * the client could attempt a retry. The rest are false, as it generally means the client made a bad request.
     *
     */
    public static final boolean RECEIVERS_FAULT = true; // server's fault
    public static final boolean SENDERS_FAULT = false; // client's fault
    private boolean mReceiver;

    /**
     * This is for exceptions that are usually not logged and thus need to include an unique "label"
     * in the exception id so the thrown(or instantiation) location can be identified by the exception id alone
     * (without referencing the log - the stack won't be in the log).
     *
     * @param callSite call site of the stack where the caller wants to include in the exception id
     */
    public void setIdLabel(StackTraceElement callSite) {
        String fileName = callSite.getFileName();
        int i = fileName.lastIndexOf('.');
        if (i != -1)
            fileName = fileName.substring(0, i);

        mId = mId + ":" + fileName + callSite.getLineNumber();
    }

    private void setId() {
        mId = Thread.currentThread().getName() + ":"+ System.currentTimeMillis() + ":" + ID_KEY;
    }

    protected void setId(String id) { mId = id; }

    private void setThreadName() {
        String threadName = Thread.currentThread().getName();
        if (!StringUtil.isNullOrEmpty(threadName)) {
            threadName = threadName.split(":")[0];
        }
        mThreadName = threadName + ":"+ System.currentTimeMillis() + ":" + ID_KEY;
    }

    public String getThreadName() {
        return mThreadName;
    }

    protected ServiceException(String message, String code, boolean isReceiversFault, Throwable cause, Argument... arguments) {
        super(message, cause);
        List<Argument> argList = (arguments == null ? Collections.<Argument>emptyList() : Arrays.asList(arguments));
        init(message, code, isReceiversFault, cause, argList);
    }

    protected ServiceException(String message, String code, boolean isReceiversFault, Throwable cause, List<Argument> arguments) {
        super(message, cause);
        init(message, code, isReceiversFault, cause, arguments);
    }

    protected ServiceException(String message, String code, boolean isReceiversFault, Argument... arguments) {
        this(message, code, isReceiversFault, null, arguments);
    }

    private void init(String message, String code, boolean isReceiversFault, Throwable cause, List<Argument> arguments) {
        mCode = code;
        mReceiver = isReceiversFault;

        if (arguments == null) {
            mArgs = Collections.emptyList();
        } else {
            mArgs = Collections.unmodifiableList(Lists.newArrayList(arguments));
        }

        setId();
        setThreadName();
    }

    public String getCode() {
        return mCode;
    }

    /**
     * Returns the arguments, or an empty {@code List}.
     */
    public List<Argument> getArgs() {
        return mArgs;
    }

    public String getArgumentValue(String name) {
        for (Argument arg : mArgs) {
            if (Objects.equal(arg.name, name)) {
                return arg.value;
            }
        }
        return null;
    }

    public String getId() {
        return mId;
    }

    /**
     * @return See the comment for the mReceiver member
     */
    public boolean isReceiversFault() {
        return mReceiver;
    }

    /**
     * generic system failure. most likely a temporary situation.
     */
    public static ServiceException FAILURE(String message, Throwable cause) {
        return new ServiceException("system failure: "+message, FAILURE, RECEIVERS_FAULT, cause);
    }

    public static ServiceException ERROR_WHILE_PARSING_UPLOAD(String message, Throwable cause) {
        return new ServiceException(
                String.format("ioexception during upload: %s", message), ERROR_WHILE_PARSING_UPLOAD, RECEIVERS_FAULT, cause);
    }

    /**
     * The request was somehow invalid (wrong parameter, wrong target, etc)
     */
    public static ServiceException INVALID_REQUEST(String message, Throwable cause) {
        return new ServiceException("invalid request: "+message, INVALID_REQUEST, SENDERS_FAULT, cause);
    }

    /**
     * The request was invalid as datasource with the specified Id is not present.
     */
    public static ServiceException INVALID_DATASOURCE_ID(String message, Throwable cause) {
        return new ServiceException(
                String.format("wrong datasource id: %s", message), INVALID_DATASOURCE_ID, SENDERS_FAULT, cause);
    }

    /**
     * The request was invalid as datasource datasource SMTP is not enabled.
     */
    public static ServiceException DATASOURCE_SMTP_DISABLED(String message, Throwable cause) {
        return new ServiceException(
                String.format("datasource smtp not enabled: %s", message), DATASOURCE_SMTP_DISABLED, SENDERS_FAULT, cause);
    }

    /**
     * User sent an unknown SOAP command (the "document" is the Soap Request)
     */
    public static ServiceException UNKNOWN_DOCUMENT(String message, Throwable cause) {
        return new ServiceException("unknown document: "+message, UNKNOWN_DOCUMENT, SENDERS_FAULT, cause);
    }

    public static ServiceException PARSE_ERROR(String message, Throwable cause) {
        return new ServiceException("parse error: "+message, PARSE_ERROR, SENDERS_FAULT, cause);
    }

    public static ServiceException FIXING_SENDMSG_FOR_SENTBY_PARSE_ERROR(String message, Throwable cause) {
        return new ServiceException(
                String.format("parse error for SENT-BY: %s",  message), INTERNAL_ERROR, SENDERS_FAULT, cause);
    }

    public static ServiceException RESOURCE_UNREACHABLE(String message, Throwable cause, Argument... arguments) {
        return new ServiceException("resource unreachable: " + message, RESOURCE_UNREACHABLE, RECEIVERS_FAULT, cause, arguments);
    }

    public static ServiceException TEMPORARILY_UNAVAILABLE() {
        return new ServiceException("service temporarily unavailable", TEMPORARILY_UNAVAILABLE, RECEIVERS_FAULT);
    }

    public static ServiceException PERM_DENIED(String message) {
        return new ServiceException("permission denied: "+message, PERM_DENIED, SENDERS_FAULT);
    }

    public static ServiceException PERM_DENIED(String message, Argument... arguments) {
        return new ServiceException("permission denied: "+message, PERM_DENIED, SENDERS_FAULT, arguments);
    }

    public static ServiceException AUTH_EXPIRED(String message) {
        return new ServiceException("auth credentials have expired" + (message==null ? "" : ": "+message), AUTH_EXPIRED, SENDERS_FAULT);
    }

    public static ServiceException AUTH_EXPIRED() {
        return AUTH_EXPIRED(null);
    }

    // to defend against harvest attacks throw PERM_DENIED instead of NO_SUCH_ACCOUNT
    public static ServiceException DEFEND_ACCOUNT_HARVEST(String account) {
        return PERM_DENIED("can not access account " + account);
        // return new ServiceException("permission denied: can not access account " + account, PERM_DENIED, SENDERS_FAULT);
    }

    // to defend against harvest attacks throw PERM_DENIED instead of NO_SUCH_DISTRIBUTION_LIST
    public static ServiceException DEFEND_DL_HARVEST(String group) {
        return PERM_DENIED("can not access distribution list " + group);
    }

    // to defend against harvest attacks throw PERM_DENIED instead of NO_SUCH_CALENDAR_RESOURCE
    public static ServiceException DEFEND_CALENDAR_RESOURCE_HARVEST(String calendarResource) {
        return PERM_DENIED("can not access calendar resource " + calendarResource);
    }

    public static ServiceException AUTH_REQUIRED() {
        return new ServiceException("no valid authtoken present", AUTH_REQUIRED, SENDERS_FAULT);
    }

    public static ServiceException AUTH_REQUIRED(String email) {
        return new ServiceException("no valid authtoken present for " + email, AUTH_REQUIRED, SENDERS_FAULT);
    }

    public static ServiceException CANNOT_DISABLE_TWO_FACTOR_AUTH() {
        return new ServiceException("cannot disable two-factor authentication", CANNOT_DISABLE_TWO_FACTOR_AUTH, SENDERS_FAULT);
    }

    public static ServiceException CANNOT_ENABLE_TWO_FACTOR_AUTH() {
        return new ServiceException("cannot enable two-factor authentication", CANNOT_ENABLE_TWO_FACTOR_AUTH, SENDERS_FAULT);
    }

    public static ServiceException WRONG_HOST(String target, Throwable cause) {
        return new ServiceException("operation sent to wrong host (you want '" + target + "')", WRONG_HOST, SENDERS_FAULT, cause, new Argument(HOST, target, Argument.Type.STR));
    }

    public static ServiceException NON_READONLY_OPERATION_DENIED() {
        return new ServiceException("non-readonly operation denied", NON_READONLY_OPERATION_DENIED, SENDERS_FAULT);
    }

    public static ServiceException PROXY_ERROR(Throwable cause, String url) {
        return new ServiceException("error while proxying request to target server: " + (cause != null ? cause.getMessage() : "unknown reason"),
                PROXY_ERROR, RECEIVERS_FAULT, cause, new InternalArgument(URL, url, Argument.Type.STR));
    }

    public static ServiceException PROXY_ERROR(String statusLine, String url) {
        return new ServiceException("error while proxying request to target server: " + statusLine,
                PROXY_ERROR, RECEIVERS_FAULT, new InternalArgument(URL, url, Argument.Type.STR));
    }

    public static ServiceException TOO_MANY_HOPS() {
        return new ServiceException("mountpoint or proxy loop detected", TOO_MANY_HOPS, SENDERS_FAULT);
    }

    public static ServiceException TOO_MANY_HOPS(String acctId) {
        return new ServiceException("mountpoint or proxy loop detected", TOO_MANY_HOPS, SENDERS_FAULT, new Argument(ACCOUNT_ID, acctId, Argument.Type.STR));
    }

    public static ServiceException TOO_MANY_PROXIES(String url) {
        return new ServiceException("proxy loop detected", TOO_MANY_HOPS, SENDERS_FAULT, new Argument(URL, HttpUtil.sanitizeURL(url), Argument.Type.STR));
    }

    public static ServiceException ALREADY_IN_PROGRESS(String message) {
        return new ServiceException(message, ALREADY_IN_PROGRESS, SENDERS_FAULT);
    }

    public static ServiceException ALREADY_IN_PROGRESS(String mboxId, String action) {
        return new ServiceException("mbox "+mboxId+" is already running action "+action, ALREADY_IN_PROGRESS, SENDERS_FAULT, new Argument(MAILBOX_ID, mboxId, Argument.Type.IID), new Argument("action", action, Argument.Type.STR));
    }

    public static ServiceException NOT_IN_PROGRESS(String mboxId, String action) {
        return new ServiceException("mbox "+mboxId+" is not currently running action "+action, NOT_IN_PROGRESS, SENDERS_FAULT, new Argument(MAILBOX_ID, mboxId, Argument.Type.IID), new Argument("action", action, Argument.Type.STR));
    }

    public static ServiceException INTERRUPTED(String str) {
        return new ServiceException("The operation has been interrupted "+str!=null?str:"", INTERRUPTED, RECEIVERS_FAULT);
    }

    public static ServiceException NO_SPELL_CHECK_URL(String str) {
        return new ServiceException("Spell Checking Not Available "+str!=null?str:"", NO_SPELL_CHECK_URL, RECEIVERS_FAULT);
    }

    public static ServiceException SAX_READER_ERROR(String str, Throwable cause) {
        return new ServiceException("SAX Reader Error: " + (str != null ? str : ""), SAX_READER_ERROR, SENDERS_FAULT, cause);
    }

    public static ServiceException UNSUPPORTED() {
        return new ServiceException("unsupported", UNSUPPORTED, RECEIVERS_FAULT);
    }

    public static ServiceException FORBIDDEN(String str) {
        return new ServiceException("forbidden: " + str, FORBIDDEN, SENDERS_FAULT);
    }

    public static ServiceException NOT_FOUND(String msg) {
        return new ServiceException("not found" + msg != null ? msg : "", NOT_FOUND, RECEIVERS_FAULT);
    }

    public static ServiceException NOT_FOUND(String msg, Throwable cause) {
        return new ServiceException("not found" + msg != null ? msg : "", NOT_FOUND, RECEIVERS_FAULT, cause);
    }

    public static ServiceException OPERATION_DENIED(String message) {
        return new ServiceException("operation denied: "+message, OPERATION_DENIED, SENDERS_FAULT);
    }

    public static ServiceException NETWORK_MODULES_NG_ENABLED(String str) {
        return new ServiceException("ZimbraNetworkModulesNG: "+ str + " is not enabled.", ZIMBRA_NETWORK_MODULES_NG_ENABLED, RECEIVERS_FAULT);
    }

    public static ServiceException LOCK_FAILED(String message) {
        return new ServiceException(message, LOCK_FAILED, RECEIVERS_FAULT, (Throwable) null);
    }

    public static ServiceException LOCK_FAILED(String message, Throwable cause) {
        return new ServiceException(message, LOCK_FAILED, RECEIVERS_FAULT, cause);
    }

    public static class TransactionRollbackException extends ServiceException {
        public TransactionRollbackException(Throwable cause) {
            super("system failure: statement was automatically rolled back by the " +
                            " database because of deadlock or other transaction serialization failures",
                    FAILURE, RECEIVERS_FAULT, cause);
        }
    }

    public static TransactionRollbackException TRANSACTION_ROLLBACK(Throwable cause) {
        return new TransactionRollbackException(cause);
    }
}

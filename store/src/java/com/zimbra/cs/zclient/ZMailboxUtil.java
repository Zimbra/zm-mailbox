/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.zclient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.zimbra.client.ZAce;
import com.zimbra.client.ZAppointmentHit;
import com.zimbra.client.ZAutoCompleteMatch;
import com.zimbra.client.ZContact;
import com.zimbra.client.ZContactHit;
import com.zimbra.client.ZConversation;
import com.zimbra.client.ZConversation.ZMessageSummary;
import com.zimbra.client.ZConversationHit;
import com.zimbra.client.ZDocument;
import com.zimbra.client.ZDocumentHit;
import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFilterRules;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGetMessageParams;
import com.zimbra.client.ZGrant;
import com.zimbra.client.ZGrant.GranteeType;
import com.zimbra.client.ZIdentity;
import com.zimbra.client.ZJSONObject;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Fetch;
import com.zimbra.client.ZMailbox.GalEntryType;
import com.zimbra.client.ZMailbox.OwnerBy;
import com.zimbra.client.ZMailbox.SharedItemBy;
import com.zimbra.client.ZMailbox.ZApptSummaryResult;
import com.zimbra.client.ZMailbox.ZSearchGalResult;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessage.ZMimePart;
import com.zimbra.client.ZMessageHit;
import com.zimbra.client.ZMountpoint;
import com.zimbra.client.ZSearchFolder;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchPagerResult;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.client.ZSignature;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.client.event.ZCreateEvent;
import com.zimbra.client.event.ZDeleteEvent;
import com.zimbra.client.event.ZEventHandler;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZRefreshEvent;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.SoapTransport.DebugListener;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.soap.SoapAccountInfo;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.DelegateAuthResponse;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityType;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.soap.mail.message.GetRelatedContactsResponse;
import com.zimbra.soap.mail.type.RelatedContactResult;
import com.zimbra.soap.mail.type.RelatedContactsTarget;
import com.zimbra.soap.type.SearchSortBy;

/**
 * @author schemers
 */
public class ZMailboxUtil implements DebugListener {

    private boolean mInteractive = false;
    private boolean mGlobalVerbose = false;
    private boolean mDebug = false;
    private String mAdminAccountName = null;
    private String mAuthAccountName = null;
    private String mTargetAccountName = null;
    private String mPassword = null;
    private ZAuthToken mAdminAuthToken = null;

    private static final String DEFAULT_ADMIN_URL = "https://"+LC.zimbra_zmprov_default_soap_server.value()+":" + LC.zimbra_admin_service_port.intValue()+"/";
    private static final String DEFAULT_URL = "http://" + LC.zimbra_zmprov_default_soap_server.value()
                    + (LC.zimbra_mail_service_port.intValue() == 80 ? "" : ":" + LC.zimbra_mail_service_port.intValue()) + "/";

    private String mUrl = DEFAULT_URL;

    private Map<String,Command> mCommandIndex;
    private ZMailbox mMbox;
    private String mPrompt = "mbox> ";
    ZSearchParams mSearchParams;
    int mSearchPage;
    ZSearchParams mConvSearchParams;
    ZSearchResult mConvSearchResult;
    SoapProvisioning mProv;
    private int mTimeout = LC.httpclient_internal_connmgr_so_timeout.intValue();

    private final Map<Integer, String> mIndexToId = new HashMap<Integer, String>();

    /** current command */
    private Command mCommand;

    /** current command line */
    private CommandLine mCommandLine;

    /** parser for internal commands */
    private final CommandLineParser mParser = new GnuParser();

    public void setDebug(boolean debug) { mDebug = debug; }

    public void setVerbose(boolean verbose) { mGlobalVerbose = verbose; }

    public void setInteractive(boolean interactive) { mInteractive = interactive; }

    public void setAdminAccountName(String account) { mAdminAccountName = account; }

    public void setAuthAccountName(String account) { mAuthAccountName = account; }

    public void setTargetAccountName(String account) { mTargetAccountName = account; }

    public void setPassword(String password) { mPassword = password; }

    public void setAdminAuthToken(ZAuthToken authToken) { mAdminAuthToken = authToken; }

    public void setUrl(String url, boolean admin) throws ServiceException {
        mUrl = ZMailbox.resolveUrl(url, admin);
    }

    public void setTimeout(String timeout) throws ServiceException {
        int num;
        try {
            num = Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("Invalid timeout value " + timeout, e);
        }
        if (num < 0) {
            throw ServiceException.INVALID_REQUEST("Timeout can't be negative", null);
        }
        if (num > 2147483) {
            throw ServiceException.INVALID_REQUEST("Timeout can't exceed 2147483 seconds", null);
        }
        mTimeout = num * 1000;
    }

    private void usage() {
        if (mCommand != null) {
            stdout.printf("usage:%n%n%s%n", mCommand.getFullUsage());
        }

        if (mInteractive)
            return;

        stdout.println("");
        stdout.println("zmmailbox [args] [cmd] [cmd-args ...]");
        stdout.println("");
        stdout.println("  -h/--help                                display usage");
        stdout.println("  -f/--file                                use file as input stream");
        stdout.println("  -u/--url      http[s]://{host}[:{port}]  server hostname and optional port. must use admin port with -z/-a");
        stdout.println("  -a/--admin    {name}                     admin account name to auth as");
        stdout.println("  -z/--zadmin                              use zimbra admin name/password from localconfig for admin/password");
        stdout.println("  -y/--authtoken {authtoken}               " + SoapCLI.OPT_AUTHTOKEN.getDescription());
        stdout.println("  -Y/--authtokenfile {authtoken file}      " + SoapCLI.OPT_AUTHTOKENFILE.getDescription());
        stdout.println("  -m/--mailbox  {name}                     mailbox to open");
        stdout.println("     --auth     {name}                     account name to auth as; defaults to -m unless -A is used");
        stdout.println("  -A/--admin-priv                          execute requests with admin privileges");
        stdout.println("                                           requires -z or -a, and -m");
        stdout.println("  -p/--password {pass}                     auth password");
        stdout.println("  -P/--passfile {file}                     read password from file");
        stdout.println("  -t/--timeout                             timeout (in seconds)");
        stdout.println("  -v/--verbose                             verbose mode (dumps full exception stack trace)");
        stdout.println("  -d/--debug                               debug mode (dumps SOAP messages)");

        stdout.println("");
        doHelp(null);
        System.exit(1);
    }

    public static enum Category {
        ADMIN("help on admin-related commands"),
        ACCOUNT("help on account-related commands"),
        APPOINTMENT("help on appoint-related commands",
                " absolute date-specs:\n" +
                "\n" +
                "  mm/dd/yyyy (i.e., 12/25/1998)\n" +
                "  yyyy/dd/mm (i.e., 1989/12/25)\n" +
                "  \\d+       (num milliseconds, i.e., 1132276598000)\n" +
                "\n" +
                "  relative date-specs:\n" +
                "\n" +
                "  [mp+-]?([0-9]+)([mhdwy][a-z]*)?g\n" +
                " \n" +
                "   +/{not-specified}   current time plus an offset\n" +
                "   -                   current time minus an offset\n" +
                "  \n"+
                "   (0-9)+    value\n" +
                "\n" +
                "   ([mhdwy][a-z]*)  units, everything after the first character is ignored (except for \"mi\" case):\n" +
                "   m(onths)\n" +
                "   mi(nutes)\n" +
                "   d(ays)\n" +
                "   w(eeks)\n" +
                "   h(ours)\n" +
                "   y(ears)\n" +
                "   \n" +
                "  examples:\n" +
                "     1day     1 day from now\n" +
                "    +1day     1 day from now \n" +
                "    p1day     1 day from now\n" +
                "    +60mi     60 minutes from now\n" +
                "    +1week    1 week from now\n"+
                "    +6mon     6 months from now \n" +
                "    1year     1 year from now\n" +
                "\n"),
        COMMANDS("help on all commands"),
        CONTACT("help on contact-related commands"),
        CONVERSATION("help on conversation-related commands"),
        FILTER("help on filter-realted commnds",
                "  {conditions}:\n" +
                "    address \"comma-separated-header-names\" all|localpart|domain is|not_is|contains|not_contains|matches|not_matches [case_sensitive] \"value\"\n" +
                "    addressbook in|not_in \"header-name\"\n" +
                "    attachment exists|not_exists\n" +
                "    body contains|not_contains [case_sensitive] \"text\"\n" +
                "    bulk [not]\n" +
                "    conversation where|not_where started|participated\n" +
                "    current_day_of_week is|not_is \"comma-separated-days(0=Sun,6=Sat)\"\n" +
                "    current_time before|not_before|after|not_after \"HHmm\"\n" +
                "    date before|not_before|after|not_after \"YYYYMMDD\"\n" +
                "    facebook [not]\n" +
                "    flagged [not] \"flag\"\n" +
                "    header \"comma-separated-names\" is|not_is|contains|not_contains|matches|not_matches [case_sensitive] \"value\"\n" +
                "    header \"comma-separated-names\" exists|not_exists\n" +
                "    importance is|not_is high|normal|low\n" +
                "    invite exists|not_exists\n" +
                "    linkedin [not]\n" +
                "    list [not]\n" +
                "    me in|not_in \"header-name\"\n" +
                "    mime_header \"name\" is|not_is|contains|not_contains|matches|not_matches [case_sensitive] \"value\"\n" +
                "    ranking in|not_in \"header-name\"\n" +
                "    size under|not_under|over|not_over \"1|1K|1M\"\n" +
                "    socialcast [not]\n" +
                "    twitter [not]\n" +
                "\n" +
                "  {actions}:\n" +
                "    keep\n" +
                "    discard\n" +
                "    fileinto \"/path\"\n" +
                "    tag \"tag\"\n" +
                "    flag \"flag\"\n" +
                "    mark read|flagged|priority\n" +
                "    redirect \"address\"\n" +
                "    reply \"body-template\"\n" +
                "    notify \"address\" \"subject-template\" \"body-template\" [\"max-body-size(bytes)\"]\n" +
                "    notify rfc \"from\" [\"importance\" \"options\"] \"message(subject)\" \"mailto url\"\n" +
                "    stop\n"),
        FOLDER("help on folder-related commands"),
        ITEM("help on item-related commands"),
        MESSAGE("help on message-related commands"),
        MISC("help on misc commands"),
        RIGHT("help on right commands",
                "To grant/revoke rights on entries an account can inherit grants from(group, domain, global), use \"zmprov grr/rvr\" commands."),
        SEARCH("help on search-related commands"),
        TAG("help on tag-related commands");

        String mDesc;
        String mCategoryHelp;

        public String getDescription() { return mDesc; }
        public String getCategoryHelp() { return mCategoryHelp; }

        Category(String desc) {
            mDesc = desc;
        }

        Category(String desc, String help) {
            mDesc = desc;
            mCategoryHelp = help;
        }
    }

    public static Option getOption(String shortName, String longName, boolean hasArgs, String help) {
        return new Option(shortName, longName, hasArgs, help);
    }

    static Option O_AUTH = new Option(null, "auth", true, "account to auth as");
    static Option O_AS_ADMIN = new Option("A", "admin-priv", false, "execute requests with admin privileges");
    static Option O_AFTER = new Option("a", "after", true, "add after filter-name");
    static Option O_BEFORE = new Option("b", "before", true, "add before filter-name");
    static Option O_COLOR = new Option("c", "color", true, "color");
    static Option O_CONTENT_TYPE = new Option("c", "contentType", true, "content-type");
    static Option O_CURRENT = new Option("c", "current", false, "current page of search results");
    static Option O_DATE = new Option("d", "date", true,  "received date (msecs since epoch)");
    static Option O_FIRST = new Option("f", "first", false, "add as first filter rule");
    static Option O_FLAGS = new Option("F", "flags", true, "flags");
    static Option O_FOLDER = new Option("f", "folder", true, "folder-path-or-id");
    static Option O_IGNORE = new Option("i", "ignore", false, "ignore unknown contact attrs");
    static Option O_IGNORE_ERROR = new Option("i", "ignore", false, "ignore and continue on error during ics import");
    static Option O_LAST = new Option("l", "last", false, "add as last filter rule");
    static Option O_LIMIT = new Option("l", "limit", true, "max number of results to return (1-1000, default=25)");
    static Option O_NEXT = new Option("n", "next", false, "next page of search results");
    static Option O_OUTPUT_FILE = new Option("o", "output", true, "output filename");
    static Option O_PRESERVE_ALARMS = new Option(null, "preserveAlarms", false,
            "preserve existing calendar alarms during ics import (default is to use alarms in ics file)");
    static Option O_START_TIME = new Option(null, "startTime", true, "start time for ics export");
    static Option O_END_TIME = new Option(null, "endTime", true, "end time for ics export");
    static Option O_PREVIOUS = new Option("p", "previous", false,  "previous page of search results");
    static Option O_SORT = new Option("s", "sort", true, "sort order TODO");
    static Option O_REPLACE = new Option("r", "replace", false, "replace contact (default is to merge)");
    static Option O_TAGS = new Option("T", "tags", true, "list of tag ids/names");
    static Option O_TYPES = new Option("t", "types", true, "list of types to search for (message,conversation,contact,appointment,document,task,wiki)");
    static Option O_URL = new Option("u", "url", true, "url to connect to");
    static Option O_VERBOSE = new Option("v", "verbose", false, "verbose output");
    static Option O_VIEW = new Option("V", "view", true, "default type for folder (appointment,contact,conversation,document,message,task,wiki)");
    static Option O_NO_VALIDATION = new Option(null, "noValidation", false, "don't validate file content");
    static Option O_DUMPSTER = new Option(null, "dumpster", false, "search in dumpster");

    enum Command {
        ADD_INCOMING_FILTER_RULE("addFilterRule", "afrl", "{name}  [*active|inactive] [any|*all] {conditions}+ {actions}+", "add incoming filter rule", Category.FILTER,  2, Integer.MAX_VALUE, O_AFTER, O_BEFORE, O_FIRST, O_LAST),
        ADD_OUTGOING_FILTER_RULE("addOutgoingFilterRule", "aofrl", "{name}  [*active|inactive] [any|*all] {conditions}+ {actions}+", "add outgoing filter rule", Category.FILTER,  2, Integer.MAX_VALUE, O_AFTER, O_BEFORE, O_FIRST, O_LAST),
        ADD_MESSAGE("addMessage", "am", "{dest-folder-path} {filename-or-dir} [{filename-or-dir} ...]", "add a message to a folder", Category.MESSAGE, 2, Integer.MAX_VALUE, O_TAGS, O_DATE, O_FLAGS, O_NO_VALIDATION),
        ADMIN_AUTHENTICATE("adminAuthenticate", "aa", "{admin-name} {admin-password}", "authenticate as an admin. can only be used by an admin", Category.ADMIN, 2, 2, O_URL),
        AUTHENTICATE("authenticate", "a", "{auth-account-name} {password} [target-account-name]", "authenticate as account and open target mailbox; target defaults to auth account if unspecified", Category.MISC, 2, 3, O_URL),
        AUTO_COMPLETE("autoComplete", "ac", "{query}", "contact auto autocomplete", Category.CONTACT,  1, 1, O_VERBOSE),
        AUTO_COMPLETE_GAL("autoCompleteGal", "acg", "{query}", "gal auto autocomplete", Category.CONTACT,  1, 1, O_VERBOSE),
        CHECK_RIGHT("checkRight", "ckr", "{name} {right}", "check if the user has the specified right on target.", Category.RIGHT, 2, 2, O_VERBOSE),
        CLEAR_SEARCH_HISTORY("clearSearchHistory", "csh", "", "clear search history for this user", Category.SEARCH, 0, 0),
        CREATE_CONTACT("createContact", "cct", "[attr1 value1 [attr2 value2...]]", "create contact", Category.CONTACT, 2, Integer.MAX_VALUE, O_FOLDER, O_IGNORE, O_TAGS),
        CREATE_FOLDER("createFolder", "cf", "{folder-path}", "create folder", Category.FOLDER, 1, 1, O_VIEW, O_COLOR, O_FLAGS, O_URL),
        CREATE_IDENTITY("createIdentity", "cid", "{identity-name} [attr1 value1 [attr2 value2...]]", "create identity", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        CREATE_MOUNTPOINT("createMountpoint", "cm", "{folder-path} {owner-id-or-name} {remote-item-id-or-path} [{reminder-enabled (0*|1)}]", "create mountpoint", Category.FOLDER, 3, 4, O_VIEW, O_COLOR, O_FLAGS),
        CREATE_SEARCH_FOLDER("createSearchFolder", "csf", "{folder-path} {query}", "create search folder", Category.FOLDER, 2, 2, O_SORT, O_TYPES, O_COLOR),
        CREATE_SIGNATURE("createSignature", "csig", "{signature-name} [signature-value}", "create signature", Category.ACCOUNT, 2, 2),
        CREATE_TAG("createTag", "ct", "{tag-name}", "create tag", Category.TAG, 1, 1, O_COLOR),
        DELETE_CONTACT("deleteContact", "dct", "{contact-ids}", "hard delete contact(s)", Category.CONTACT, 1, 1),
        DELETE_CONVERSATION("deleteConversation", "dc", "{conv-ids}", "hard delete conversastion(s)", Category.CONVERSATION, 1, 1),
        DELETE_ITEM("deleteItem", "di", "{item-ids}", "hard delete item(s)", Category.ITEM, 1, 1),
        DELETE_IDENTITY("deleteIdentity", "did", "{identity-name}", "delete an identity", Category.ACCOUNT, 1, 1),
        DELETE_INCOMING_FILTER_RULE("deleteFilterRule", "dfrl", "{name}", "delete incoming filter rule", Category.FILTER,  1, 1),
        DELETE_OUTGOING_FILTER_RULE("deleteOutgoingFilterRule", "dofrl", "{name}", "delete outgoing filter rule", Category.FILTER,  1, 1),
        DELETE_FOLDER("deleteFolder", "df", "{folder-path}", "hard delete a folder (and subfolders)", Category.FOLDER, 1, 1),
        DELETE_MESSAGE("deleteMessage", "dm", "{msg-ids}", "hard delete message(s)", Category.MESSAGE, 1, 1),
        DELETE_SIGNATURE("deleteSignature", "dsig", "{signature-name|signature-id}", "delete signature", Category.ACCOUNT, 1, 1),
        DELETE_TAG("deleteTag", "dt", "{tag-name}", "delete a tag", Category.TAG, 1, 1),
        DUMPSTER_DELETE_ITEM("dumpsterDeleteItem", "ddi", "{item-ids}", "permanently delete item(s) from the dumpster", Category.ITEM, 1, 1),
        EMPTY_DUMPSTER("emptyDumpster", null, "", "empty the dumpster", Category.MISC, 0, 0),
        EMPTY_FOLDER("emptyFolder", "ef", "{folder-path}", "empty all the items in a folder (including subfolders)", Category.FOLDER, 1, 1),
        EXIT("exit", "quit", "", "exit program", Category.MISC, 0, 0),
        FLAG_CONTACT("flagContact", "fct", "{contact-ids} [0|1*]", "flag/unflag contact(s)", Category.CONTACT, 1, 2),
        FLAG_CONVERSATION("flagConversation", "fc", "{conv-ids} [0|1*]", "flag/unflag conversation(s)", Category.CONVERSATION, 1, 2),
        FLAG_ITEM("flagItem", "fi", "{item-ids} [0|1*]", "flag/unflag item(s)", Category.ITEM, 1, 2),
        FLAG_MESSAGE("flagMessage", "fm", "{msg-ids} [0|1*]", "flag/unflag message(s)", Category.MESSAGE, 1, 2),
        GET_ALL_CONTACTS("getAllContacts", "gact", "[attr1 [attr2...]]", "get all contacts", Category.CONTACT, 0, Integer.MAX_VALUE, O_VERBOSE, O_FOLDER),
        GET_ALL_FOLDERS("getAllFolders", "gaf", "", "get all folders", Category.FOLDER, 0, 0, O_VERBOSE),
        GET_ALL_TAGS("getAllTags", "gat", "", "get all tags", Category.TAG, 0, 0, O_VERBOSE),
        GET_APPOINTMENT_SUMMARIES("getAppointmentSummaries", "gaps", "{start-date-spec} {end-date-spec} {folder-path}", "get appointment summaries", Category.APPOINTMENT, 2, 3, O_VERBOSE),
        GET_CONTACTS("getContacts", "gct", "{contact-ids} [attr1 [attr2...]]", "get contact(s)", Category.CONTACT, 1, Integer.MAX_VALUE, O_VERBOSE),
        GET_CONVERSATION("getConversation", "gc", "{conv-id}", "get a converation", Category.CONVERSATION, 1, 1, O_VERBOSE),
        GET_IDENTITIES("getIdentities", "gid", "", "get all identites", Category.ACCOUNT, 0, 0, O_VERBOSE),
        GET_INCOMING_FILTER_RULES("getFilterRules", "gfrl", "", "get incoming filter rules", Category.FILTER,  0, 0),
        GET_OUTGOING_FILTER_RULES("getOutgoingFilterRules", "gofrl", "", "get outgoing filter rules", Category.FILTER,  0, 0),
        GET_FOLDER("getFolder", "gf", "{folder-path}", "get folder", Category.FOLDER, 1, 1, O_VERBOSE),
        GET_FOLDER_REQUEST("getFolderRequest", "gfr", "{folder-id}", "get folder request (always issues a GetFolderRequest)", Category.FOLDER, 1, 1, O_VERBOSE),
        GET_FOLDER_GRANT("getFolderGrant", "gfg", "{folder-path}", "get folder grants", Category.FOLDER, 1, 1, O_VERBOSE),
        GET_MESSAGE("getMessage", "gm", "{msg-id}", "get a message", Category.MESSAGE, 1, 1, O_VERBOSE),
        GET_MAILBOX_SIZE("getMailboxSize", "gms", "", "get mailbox size", Category.MISC, 0, 0, O_VERBOSE),
        GET_RELATED_CONTACTS("getRelatedContacts", "grc", "target [target [...]] [field:all*|to|cc|bcc] [limit:#]", "get related contacts", Category.CONTACT, 1, Integer.MAX_VALUE, O_VERBOSE),
        GET_RIGHTS("getRights", "gr", "[right1 [right2...]]", "get rights currently granted", Category.RIGHT, 0, Integer.MAX_VALUE, O_VERBOSE),
        GET_REST_URL("getRestURL", "gru", "{relative-path}", "do a GET on a REST URL relative to the mailbox", Category.MISC, 1, 1,
                O_OUTPUT_FILE, O_START_TIME, O_END_TIME, O_URL),
        GET_SEARCH_HISTORY("getSearchHistory", "gsh", "[limit]", "get search history", Category.SEARCH, 0, 1, O_VERBOSE),
        GET_SIGNATURES("getSignatures", "gsig", "", "get all signatures", Category.ACCOUNT, 0, 0, O_VERBOSE),
        GRANT_RIGHT("grantRight", "grr", "{account {name}|group {name}|domain {name}||all|public|guest {email} [{password}]|key {email} [{accesskey}] {[-]right}}", "allow or deny a right to a grantee or a group of grantee. to deny a right, put a '-' in front of the right", Category.RIGHT, 2, 4),
        HELP("help", "?", "commands", "return help on a group of commands, or all commands. Use -v for detailed help.", Category.MISC, 0, 1, O_VERBOSE),
        IMPORT_URL_INTO_FOLDER("importURLIntoFolder", "iuif", "{folder-path} {url}", "add the contents to the remote feed at {target-url} to the folder", Category.FOLDER, 2, 2),
        LIST_RIGHTS("listRights", "lr", "", "list and describe all rights that can be granted", Category.RIGHT, 0, 0, O_VERBOSE),
        MARK_CONVERSATION_READ("markConversationRead", "mcr", "{conv-ids} [0|1*]", "mark conversation(s) as read/unread", Category.CONVERSATION, 1, 2),
        MARK_CONVERSATION_SPAM("markConversationSpam", "mcs", "{conv} [0|1*] [{dest-folder-path}]", "mark conversation as spam/not-spam, and optionally move", Category.CONVERSATION, 1, 3),
        MARK_ITEM_READ("markItemRead", "mir", "{item-ids} [0|1*]", "mark item(s) as read/unread", Category.ITEM, 1, 2),
        MARK_FOLDER_READ("markFolderRead", "mfr", "{folder-path}", "mark all items in a folder as read", Category.FOLDER, 1, 1),
        MARK_MESSAGE_READ("markMessageRead", "mmr", "{msg-ids} [0|1*]", "mark message(s) as read/unread", Category.MESSAGE, 1, 2),
        MARK_MESSAGE_SPAM("markMessageSpam", "mms", "{msg} [0|1*] [{dest-folder-path}]", "mark a message as spam/not-spam, and optionally move", Category.MESSAGE, 1, 3),
        MARK_TAG_READ("markTagRead", "mtr", "{tag-name}", "mark all items with this tag as read", Category.TAG, 1, 1),
        MODIFY_CONTACT("modifyContactAttrs", "mcta", "{contact-id} [attr1 value1 [attr2 value2...]]", "modify a contact", Category.CONTACT, 3, Integer.MAX_VALUE, O_REPLACE, O_IGNORE),
        MODIFY_INCOMING_FILTER_RULE("modifyFilterRule", "mfrl", "{name} [*active|inactive] [any|*all] {conditions}+ {actions}+", "modify incoming filter rule", Category.FILTER,  2, Integer.MAX_VALUE),
        MODIFY_OUTGOING_FILTER_RULE("modifyOutgoingFilterRule", "mofrl", "{name} [*active|inactive] [any|*all] {conditions}+ {actions}+", "modify outgoing filter rule", Category.FILTER,  2, Integer.MAX_VALUE),
        MODIFY_FOLDER_CHECKED("modifyFolderChecked", "mfch", "{folder-path} [0|1*]", "modify whether a folder is checked in the UI", Category.FOLDER, 1, 2),
        MODIFY_FOLDER_COLOR("modifyFolderColor", "mfc", "{folder-path} {new-color}", "modify a folder's color", Category.FOLDER, 2, 2),
        MODIFY_FOLDER_EXCLUDE_FREE_BUSY("modifyFolderExcludeFreeBusy", "mfefb", "{folder-path} [0|1*]", "change whether folder is excluded from free-busy", Category.FOLDER, 1, 2),
        MODIFY_FOLDER_FLAGS("modifyFolderFlags", "mff", "{folder-path} {folder-flags}", "replaces the flags on the folder (subscribed, checked, etc.)", Category.FOLDER, 2, 2),
        MODIFY_FOLDER_GRANT("modifyFolderGrant", "mfg", "{folder-path} {account {name}|group {name}|cos {name}|domain {name}|all|public|guest {email}|key {email} [{accesskey}] {permissions|none}}", "add/remove a grant to a folder", Category.FOLDER, 3, 5),
        MODIFY_FOLDER_URL("modifyFolderURL", "mfu", "{folder-path} {url}", "modify a folder's URL", Category.FOLDER, 2, 2),
        MODIFY_IDENTITY("modifyIdentity", "mid", "{identity-name} [attr1 value1 [attr2 value2...]]", "modify an identity", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        MODIFY_ITEM_FLAGS("modifyItemFlags", "mif", "{item-ids} {item-flags}", "replaces the flags on the items (answered, unread, flagged, etc.)", Category.ITEM, 2, 2),
        MODIFY_MOUNTPOINT_ENABLE_SHARED_REMINDER("modifyMountpointEnableSharedReminder", "mmesr", "{mountpoint-path} {0|1}", "enable/disable appointment/task reminder on shared calendar", Category.FOLDER, 2, 2),
        MODIFY_SIGNATURE("modifySignature", "msig", "{signature-name|signature-id} {value}", "modify signature value", Category.ACCOUNT, 2, 2),
        MODIFY_TAG_COLOR("modifyTagColor", "mtc", "{tag-name} {tag-color}", "modify a tag's color", Category.TAG, 2, 2),
        MOVE_CONTACT("moveContact", "mct", "{contact-ids} {dest-folder-path}", "move contact(s) to a new folder", Category.CONTACT, 2, 2),
        MOVE_CONVERSATION("moveConversation", "mc", "{conv-ids} {dest-folder-path}", "move conversation(s) to a new folder", Category.CONVERSATION, 2, 2),
        MOVE_ITEM("moveItem", "mi", "{item-ids} {dest-folder-path}", "move item(s) to a new folder", Category.ITEM, 2, 2),
        MOVE_MESSAGE("moveMessage", "mm", "{msg-ids} {dest-folder-path}", "move message(s) to a new folder", Category.MESSAGE, 2, 2),
        NOOP("noOp", "no", "", "do a NoOp SOAP call to the server", Category.MISC, 0, 1),
        POST_REST_URL("postRestURL", "pru", "{relative-path} {file-name}", "do a POST on a REST URL relative to the mailbox", Category.MISC, 2, 2,
                O_CONTENT_TYPE, O_IGNORE_ERROR, O_PRESERVE_ALARMS, O_URL),
        RECOVER_ITEM("recoverItem", "ri", "{item-ids} {dest-folder-path}", "recover item(s) from the dumpster to a folder", Category.ITEM, 2, 2),
        REJECT_SAVED_SEARCH_PROMPT("rejectSavedSearchPrompt", "rssp", "{query}", "reject a prompt to create a saved search folder", Category.SEARCH, 1, 1),
        RENAME_FOLDER("renameFolder", "rf", "{folder-path} {new-folder-path}", "rename folder", Category.FOLDER, 2, 2),
        RENAME_SIGNATURE("renameSignature", "rsig", "{signature-name|signature-id} {new-name}", "rename signature", Category.ACCOUNT, 2, 2),
        RENAME_TAG("renameTag", "rt", "{tag-name} {new-tag-name}", "rename tag", Category.TAG, 2, 2),
        REVOKE_RIGHT("revokeRight", "rvr", "{account {name}|group {name}|domain {name}||all|public|guest {email} [{password}]|key {email} [{accesskey}] {[-]right}}", "revoke a right previously granted to a grantee or a group of grantees. to revoke a denied right, put a '-' in front of the right", Category.RIGHT, 2, 4),
        SEARCH("search", "s", "{query}", "perform search", Category.SEARCH, 0, 1, O_LIMIT, O_SORT, O_TYPES, O_VERBOSE, O_CURRENT, O_NEXT, O_PREVIOUS, O_DUMPSTER),
        SEARCH_CONVERSATION("searchConv", "sc", "{conv-id} {query}", "perform search on conversation", Category.SEARCH, 0, 2, O_LIMIT, O_SORT, O_TYPES, O_VERBOSE, O_CURRENT, O_NEXT, O_PREVIOUS),
        SELECT_MAILBOX("selectMailbox", "sm", "{name}", "select a different mailbox. can only be used by an admin", Category.ADMIN, 1, 1, O_AUTH, O_AS_ADMIN),
        SEARCH_SUGGEST("searchSuggest", "ss", "{query} [limit]", "return search suggestions based on search history", Category.SEARCH, 1, 2),
        SYNC_FOLDER("syncFolder", "sf", "{folder-path}", "synchronize folder's contents to the remote feed specified by folder's {url}", Category.FOLDER, 1, 1),
        TAG_CONTACT("tagContact", "tct", "{contact-ids} {tag-name} [0|1*]", "tag/untag contact(s)", Category.CONTACT, 2, 3),
        TAG_CONVERSATION("tagConversation", "tc", "{conv-ids} {tag-name} [0|1*]", "tag/untag conversation(s)", Category.CONVERSATION, 2, 3),
        TAG_ITEM("tagItem", "ti", "{item-ids} {tag-name} [0|1*]", "tag/untag item(s)", Category.ITEM, 2, 3),
        TAG_MESSAGE("tagMessage", "tm", "{msg-ids} {tag-name} [0|1*]", "tag/untag message(s)", Category.MESSAGE, 2, 3),
        WHOAMI("whoami", null, "", "show current auth'd/opened mailbox", Category.MISC, 0, 0);

        private String mName;
        private String mAlias;
        private String mSyntax;
        private String mHelp;
        private Option[] mOpt;
        private Category mCat;
        private int mMinArgLength = 0;
        private int mMaxArgLength = Integer.MAX_VALUE;

        public String getName() { return mName; }
        public String getAlias() { return mAlias; }
        public String getSyntax() { return mSyntax; }
        public String getHelp() { return mHelp; }
        public Category getCategory() { return mCat; }
        public boolean hasHelp() { return mSyntax != null; }
        public boolean checkArgsLength(String args[]) {
            int len = args == null ? 0 : args.length;
            return len >= mMinArgLength && len <= mMaxArgLength;
        }

        // it appears we have to create a new Option object everytime we call parse!
        // otherwise strange things were happening (i.e., looks like there is state
        // being stored in an Option, when one would assume they are immutable.
        public Options getOptions() {
            Options opts = new Options();
            for (Option o : mOpt) {
                opts.addOption(o.getOpt(), o.getLongOpt(), o.hasArg(), o.getDescription());
            }
            return opts;
        }

        public String getCommandHelp() {
            String commandName;
            String alias = getAlias();
            if (alias != null)
                commandName = String.format("%s(%s)", getName(), alias);
            else
                commandName = getName();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-38s %s%n", commandName, getHelp()));
            return sb.toString();
        }

        public String getFullUsage() {
            String commandName;
            String alias = getAlias();
            if (alias != null) {
                commandName = String.format("%s(%s)", getName(), alias);
            } else {
                commandName = getName();
            }
            @SuppressWarnings("rawtypes")
            Collection opts = getOptions().getOptions();

            StringBuilder sb = new StringBuilder();

            sb.append(String.format("  %-28s %s%n", commandName, (opts.size() > 0 ? "[opts] ":"") + getSyntax()));
            //if (opts.size() > 0)
            //    stdout.println();

            for (Object o: opts) {
                Option opt = (Option) o;
                String arg = opt.hasArg() ? " <arg>" : "";
                String shortOpt = opt.getOpt();
                String longOpt = opt.getLongOpt();
                String optStr;
                if (shortOpt != null)
                    optStr = String.format("  -%s/--%s%s", shortOpt, longOpt, arg);
                else
                    optStr = String.format("  --%s%s", longOpt, arg);
                sb.append(String.format("  %-30s %s%n", optStr, opt.getDescription()));
            }
            //sb.append(String.format("%n    %s%n%n", getHelp()));
            return sb.toString();
        }

        private Command(String name, String alias, String syntax, String help, Category cat, int minArgLength, int maxArgLength, Option ... opts)  {
            mName = name;
            mAlias = alias != null && alias.length() > 0 ? alias : null;
            mSyntax = syntax;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;
            mOpt = opts;
        }

    }

    private static final long KBYTES = 1024;
    private static final long MBYTES = 1024*1024;
    private static final long GBYTES = 1024*1024*1024;

    private String formatSize(long size) {
        if (size > GBYTES) {
            return String.format("%.2f GB", (((double) size) / GBYTES));
        } else if (size > MBYTES) {
            return String.format("%.2f MB", (((double) size) / MBYTES));
        } else if (size > KBYTES) {
            return String.format("%.2f KB", (((double) size) / KBYTES));
        } else {
            return String.format("%d B", size);
        }
    }

    private void addCommand(Command command) {
        String name = command.getName().toLowerCase();
        if (mCommandIndex.get(name) != null) {
            throw new RuntimeException("duplicate command: " + name);
        }

        mCommandIndex.put(name, command);
        String alias = command.getAlias();
        if (alias != null) {
            alias = alias.toLowerCase();
            if (mCommandIndex.get(alias) != null) {
                throw new RuntimeException("duplicate command: " + alias);
            }
            mCommandIndex.put(alias, command);
        }
    }

    private void initCommands() {
        mCommandIndex = new HashMap<String, Command>();

        for (Command c : Command.values()) {
            addCommand(c);
        }
    }

    private Command lookupCommand(String command) {
        return mCommandIndex.get(command.toLowerCase());
    }

    public ZMailboxUtil() {
        initCommands();
    }

    private ZMailbox.Options getMailboxOptions(SoapProvisioning prov, String authAccount, String targetAccount, int lifetimeSeconds)
    throws ServiceException {
        ZMailbox.Options options;
        AccountBy targetBy = StringUtil.isUUID(targetAccount) ? AccountBy.id : AccountBy.name;
        if (!StringUtil.isNullOrEmpty(authAccount)) {
            AccountBy authBy = StringUtil.isUUID(authAccount) ? AccountBy.id : AccountBy.name;
            SoapAccountInfo sai = prov.getAccountInfo(authBy, authAccount);
            DelegateAuthResponse dar = prov.delegateAuth(authBy, authAccount, lifetimeSeconds > 0 ? lifetimeSeconds: Constants.SECONDS_PER_DAY);
            options = new ZMailbox.Options(dar.getAuthToken(), sai.getAdminSoapURL());
        } else {
            SoapAccountInfo sai = prov.getAccountInfo(targetBy, targetAccount);
            options = new ZMailbox.Options(mAdminAuthToken, sai.getAdminSoapURL());
        }
        options.setTargetAccount(targetAccount);
        options.setTargetAccountBy(targetBy);
        return options;
    }

    /*
     * called when sm command is embeded in zmprov
     */
    public void selectMailbox(String authAccount, SoapProvisioning prov) throws ServiceException {
        selectMailbox(authAccount, authAccount, prov);
    }

    public void selectMailbox(String authAccount, String targetAccount, SoapProvisioning prov) throws ServiceException {
        boolean success = false;
        try {
            if (prov == null) {
                throw ZClientException.CLIENT_ERROR("can only select mailbox after adminAuthenticate", null);
            } else if (mProv == null) {
                mProv = prov;
            }
            mMbox = null; //make sure to null out current value so if select fails any further ops will fail
            setAuthAccountName(authAccount);
            setTargetAccountName(targetAccount);
            ZMailbox.Options options = getMailboxOptions(prov, authAccount, targetAccount, Constants.SECONDS_PER_DAY);
            options.setTimeout(mTimeout);

            if (prov.soapGetTransportDebugListener() != null) {
                options.setDebugListener(prov.soapGetTransportDebugListener());
            } else {  // use the same debug listener used by zmprov
                options.setHttpDebugListener(prov.soapGetHttpTransportDebugListener());
            }

            mMbox = ZMailbox.getMailbox(options);
            dumpMailboxConnect();
            mPrompt = String.format("mbox %s> ", mMbox.getName());
            mSearchParams = null;
            mConvSearchParams = null;
            mConvSearchResult = null;
            mIndexToId.clear();
            // TODO: clear all other mailbox-state

            success = true;
        } finally {
            if (!success) {
                setAuthAccountName(null);
                setTargetAccountName(null);
            }
        }
    }

    private void adminAuth(String name, String password, String uri) throws ServiceException {
        mAdminAccountName = name;
        mPassword = password;
        SoapTransport.DebugListener listener = mDebug ? this : null;
        mProv = new SoapProvisioning();
        mProv.soapSetURI(ZMailbox.resolveUrl(uri, true));
        if (listener != null) {
            mProv.soapSetTransportDebugListener(listener);
        }
        mProv.soapAdminAuthenticate(name, password);
        mAdminAuthToken = mProv.getAuthToken();
    }

    private void adminAuth(ZAuthToken zat, String uri) throws ServiceException {
        SoapTransport.DebugListener listener = mDebug ? this : null;
        mProv = new SoapProvisioning();
        mProv.soapSetURI(ZMailbox.resolveUrl(uri, true));
        if (listener != null) {
            mProv.soapSetTransportDebugListener(listener);
        }
        mProv.soapAdminAuthenticate(zat);
    }

    private void auth(String authAccountName, String password, String targetAccountName, String uri) throws ServiceException {
        boolean success = false;
        try {
            mPassword = password;
            ZMailbox.Options options = new ZMailbox.Options();
            options.setAccount(authAccountName);
            options.setAccountBy(StringUtil.isUUID(authAccountName) ? AccountBy.id : AccountBy.name);
            options.setPassword(mPassword);
            options.setTargetAccount(targetAccountName);
            options.setTargetAccountBy(StringUtil.isUUID(targetAccountName) ? AccountBy.id : AccountBy.name);
            options.setUri(ZMailbox.resolveUrl(uri, false));
            options.setDebugListener(mDebug ? this : null);
            options.setTimeout(mTimeout);
            mMbox = ZMailbox.getMailbox(options);
            mPrompt = String.format("mbox %s> ", mMbox.getName());
            setAuthAccountName(authAccountName);
            setTargetAccountName(targetAccountName);
            dumpMailboxConnect();
            success = true;
        } finally {
            if (!success) {
                setAuthAccountName(null);
                setTargetAccountName(null);
            }
        }
    }

    static class Stats {
        int numMessages;
        int numUnread;
    }

    private void computeStats(ZFolder f, Stats s) {
        s.numMessages += f.getMessageCount();
        s.numUnread += f.getUnreadCount();
        for (ZFolder c : f.getSubFolders()) {
            computeStats(c, s);
        }
    }

    private void dumpMailboxConnect() throws ServiceException {
        if (!mInteractive) return;
        if (StringUtil.isNullOrEmpty(mTargetAccountName)) {
            stdout.format("no mailbox opened%n");
            if (!StringUtil.isNullOrEmpty(mAdminAccountName)) {
                stdout.format("authenticated as %s (admin)%n", mAdminAccountName);
            } else {
                stdout.format("not authenticated%n");
            }
            return;
        }
        Stats s = new Stats();
        computeStats(mMbox.getUserRoot(), s);
        stdout.format("mailbox: %s, size: %s, messages: %d, unread: %d%n",
                mMbox.getName(),
                formatSize(mMbox.getSize()),
                s.numMessages,
                s.numUnread);
        if (StringUtil.equalIgnoreCase(mTargetAccountName, mAuthAccountName) ||
            !StringUtil.isNullOrEmpty(mAuthAccountName)) {
            stdout.format("authenticated as %s%n", mAuthAccountName);
        } else {
            stdout.format("authenticated as %s (admin)%n", mAdminAccountName);
        }
    }

    public void initMailbox() throws ServiceException {
        if (mPassword == null && mAdminAuthToken == null)
            return;

        if (mAdminAccountName != null) {
            adminAuth(mAdminAccountName, mPassword, mUrl);
        } else if (mAdminAuthToken != null) {
            adminAuth(mAdminAuthToken, mUrl);
        }

        if (mTargetAccountName == null) {
            mAuthAccountName = null;
            return;
        }

        if (mAdminAccountName != null) {
            selectMailbox(mAuthAccountName, mTargetAccountName, mProv);
        } else {
            auth(mAuthAccountName, mPassword, mTargetAccountName, mUrl);
        }
    }

    private ZTag lookupTag(String idOrName) throws ServiceException {
        ZTag tag = mMbox.getTagByName(idOrName);
        if (tag == null) {
            tag = mMbox.getTagById(idOrName);
        }
        if (tag == null) {
            throw ZClientException.CLIENT_ERROR("unknown tag: "+idOrName, null);
        }
        return tag;
    }

    /**
     * takes a list of ids or names, and trys to resolve them all to valid tag ids
     *
     * @param idsOrNames
     * @return
     * @throws SoapFaultException
     */
    private String lookupTagIds(String idsOrNames) throws ServiceException {
        StringBuilder ids = new StringBuilder();
        for (String t : idsOrNames.split(",")) {
            ZTag tag = lookupTag(t);
            if (ids.length() > 0) {
                ids.append(",");
            }
            ids.append(tag.getId());
        }
        return ids.toString();
    }

    /**
     * takes a list of ids, and trys to resolve them all to tag names
     *
     */
    private String lookupTagNames(String ids) throws ServiceException {
        StringBuilder names = new StringBuilder();
        for (String tid : ids.split(",")) {
            ZTag tag = lookupTag(tid);
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(tag == null ? tid : tag.getName());
        }
        return names.toString();
    }

    private String lookupFolderId(String pathOrId) throws ServiceException {
        return lookupFolderId(pathOrId, false);
    }

    Pattern sTargetConstraint = Pattern.compile("\\{(.*)\\}$");

    private String id(String indexOrId) throws ServiceException {
        Matcher m = sTargetConstraint.matcher(indexOrId);
        if (m.find()) {
            indexOrId = m.replaceAll("");
        }

        StringBuilder ids = new StringBuilder();
        for (String t : indexOrId.split(",")) {
            if (t.length() > 1 && t.charAt(0) == '#') {
                t = t.substring(1);
                //stdout.println(t);
                int i = t.indexOf('-');
                if (i != -1) {
                    int start = Integer.parseInt(t.substring(0, i));
                    int end = Integer.parseInt(t.substring(i+1, t.length()));
                    for (int j = start; j <= end; j++) {
                        String id = mIndexToId.get(j);
                        if (id == null) {
                            throw ZClientException.CLIENT_ERROR("unknown index: "+t, null);
                        }
                        if (ids.length() > 0) {
                            ids.append(",");
                        }
                        ids.append(id);
                    }
                } else {
                    String id = mIndexToId.get(Integer.parseInt(t));
                    if (id == null) {
                        throw ZClientException.CLIENT_ERROR("unknown index: "+t, null);
                    }
                    if (ids.length() > 0) {
                        ids.append(",");
                    }
                    ids.append(id);
                }
            } else {
                if (ids.length() > 0) {
                    ids.append(",");
                }
                ids.append(t);
            }
        }
        return ids.toString();
    }

    private String lookupFolderId(String pathOrId, boolean parent) throws ServiceException {
        if (parent && pathOrId != null) {
            pathOrId = ZMailbox.getParentPath(pathOrId);
        }
        if (pathOrId == null || pathOrId.length() == 0) {
            return null;
        }
        ZFolder folder = mMbox.getFolderById(pathOrId);
        if (folder == null) {
            folder = mMbox.getFolderByPath(pathOrId);
        }
        if (folder == null) {
            throw ZClientException.CLIENT_ERROR("unknown folder: " + pathOrId, null);
        }
        return folder.getId();
    }

    private ZFolder lookupFolder(String pathOrId) throws ServiceException {
        if (pathOrId == null || pathOrId.length() == 0) {
            return null;
        }
        ZFolder folder = mMbox.getFolderById(pathOrId);
        if (folder == null) {
            folder = mMbox.getFolderByPath(pathOrId);
        }
        if (folder == null) {
            throw ZClientException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        }
        return folder;
    }

    private String param(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }

    private boolean paramb(String[] args, int index, boolean defaultValue) {
        return args.length > index ? args[index].equals("1") : defaultValue;
    }

    private String param(String[] args, int index) {
        return param(args, index, null);
    }

    private ZTag.Color tagColorOpt() throws ServiceException {
        String color = mCommandLine.getOptionValue(O_COLOR.getOpt());
        return color == null ? null : ZTag.Color.fromString(color);
    }

    private String tagsOpt() throws ServiceException {
        String tags = mCommandLine.getOptionValue(O_TAGS.getOpt());
        return tags == null ? null : lookupTagIds(tags);
    }

    private ZFolder.Color folderColorOpt() throws ServiceException {
        String color = mCommandLine.getOptionValue(O_COLOR.getOpt());
        return color == null ? null : ZFolder.Color.fromString(color);
    }

    private ZFolder.View folderViewOpt() {
        String view = mCommandLine.getOptionValue(O_VIEW.getOpt());
        return view == null ? null : ZFolder.View.fromString(view);
    }

    private String flagsOpt() {
        return mCommandLine.getOptionValue(O_FLAGS.getOpt());
    }

    private String urlOpt(boolean admin) {
        String url = mCommandLine.getOptionValue(O_URL.getOpt());
        return (url == null && admin) ? mUrl : url;
    }

    private String outputFileOpt() {
        return mCommandLine.getOptionValue(O_OUTPUT_FILE.getOpt());
    }

    private String contentTypeOpt() {
        return mCommandLine.getOptionValue(O_CONTENT_TYPE.getOpt());
    }

    private boolean ignoreAndContinueOnErrorOpt() {
        return mCommandLine.hasOption(O_IGNORE_ERROR.getOpt());
    }

    private boolean preserveAlarmsOpt() {
        return mCommandLine.hasOption(O_PRESERVE_ALARMS.getLongOpt());
    }

    private String startTimeOpt() {
        return mCommandLine.getOptionValue(O_START_TIME.getLongOpt());
    }

    private String endTimeOpt() {
        return mCommandLine.getOptionValue(O_END_TIME.getLongOpt());
    }

    private String typesOpt() throws ServiceException {
        String t = mCommandLine.getOptionValue(O_TYPES.getOpt());
        return t == null ? null : ZSearchParams.getCanonicalTypes(t);
    }

    private long dateOpt(long def) {
        String ds = mCommandLine.getOptionValue(O_DATE.getOpt());
        return ds == null ? def : Long.parseLong(ds);
    }

    private String folderOpt() {
        return mCommandLine.getOptionValue(O_FOLDER.getOpt());
    }

    private boolean ignoreOpt() {
        return mCommandLine.hasOption(O_IGNORE.getOpt());
    }

    private boolean verboseOpt() {
        return mCommandLine.hasOption(O_VERBOSE.getOpt());
    }

    private boolean currrentOpt() { return mCommandLine.hasOption(O_CURRENT.getOpt()); }

    private boolean nextOpt()     { return mCommandLine.hasOption(O_NEXT.getOpt()); }

    private boolean previousOpt() { return mCommandLine.hasOption(O_PREVIOUS.getOpt()); }

    private boolean firstOpt() { return mCommandLine.hasOption(O_FIRST.getOpt()); }

    private String  beforeOpt() { return mCommandLine.getOptionValue(O_BEFORE.getOpt()); }

    private String  afterOpt() { return mCommandLine.getOptionValue(O_AFTER.getOpt()); }

    private SearchSortBy searchSortByOpt() throws ServiceException {
        String sort = mCommandLine.getOptionValue(O_SORT.getOpt());
        return sort == null ? null : SearchSortBy.fromString(sort);
    }

    private boolean validateOpt() {
        return !mCommandLine.hasOption(O_NO_VALIDATION.getLongOpt());
    }

    enum ExecuteStatus {OK, EXIT};

    public ExecuteStatus execute(String argsIn[]) throws ServiceException, IOException {
        mCommand = lookupCommand(argsIn[0]);

        // shift them over for parser
        String args[] = new String[argsIn.length-1];
        System.arraycopy(argsIn, 1, args, 0, args.length);

        if (mCommand == null)
            throw ZClientException.CLIENT_ERROR("Unknown command: ("+argsIn[0]+ ") Type: 'help commands' for a list", null);

        try {
            mCommandLine = mParser.parse(mCommand.getOptions(), args, true);
            args = mCommandLine.getArgs();
        } catch (ParseException e) {
            usage();
            return ExecuteStatus.OK;
        }

        if (!mCommand.checkArgsLength(args)) {
            usage();
            return ExecuteStatus.OK;
        }

        if (
                mCommand != Command.EXIT &&
                mCommand != Command.HELP &&
                mCommand != Command.AUTHENTICATE &&
                mCommand != Command.ADMIN_AUTHENTICATE &&
                mCommand != Command.SELECT_MAILBOX &&
                mCommand != Command.WHOAMI
        ) {
            if (mMbox == null) {
                throw ZClientException.CLIENT_ERROR("no mailbox opened. select one with authenticate/adminAuthenticate/selectMailbox", null);
            }
        }

        switch(mCommand) {
        case AUTO_COMPLETE:
            doAutoComplete(args);
            break;
        case AUTO_COMPLETE_GAL:
            doAutoCompleteGal(args);
            break;
        case AUTHENTICATE:
            doAuth(args);
            break;
        case ADD_INCOMING_FILTER_RULE:
            doAddIncomingFilterRule(args);
            break;
        case ADD_OUTGOING_FILTER_RULE:
            doAddOutgoingFilterRule(args);
            break;
        case ADD_MESSAGE:
            doAddMessage(args);
            break;
        case ADMIN_AUTHENTICATE:
            doAdminAuth(args);
            break;
        case CHECK_RIGHT:
            doCheckRight(args);
            break;
        case CLEAR_SEARCH_HISTORY:
            doClearSearchHistory();
            break;
        case CREATE_CONTACT:
            String ccId = mMbox.createContact(lookupFolderId(folderOpt()),tagsOpt(), getContactMap(args, 0, !ignoreOpt())).getId();
            stdout.println(ccId);
            break;
        case CREATE_IDENTITY:
            mMbox.createIdentity(new ZIdentity(args[0], getMultiMap(args, 1)));
            break;
        case CREATE_FOLDER:
            doCreateFolder(args);
            break;
        case CREATE_MOUNTPOINT:
            doCreateMountpoint(args);
            break;
        case CREATE_SEARCH_FOLDER:
            doCreateSearchFolder(args);
            break;
        case CREATE_SIGNATURE:
            doCreateSignature(args);
            break;
        case CREATE_TAG:
            ZTag ct = mMbox.createTag(args[0], tagColorOpt());
            stdout.println(ct.getId());
            break;
        case DELETE_CONTACT:
            mMbox.deleteContact(args[0]);
            break;
        case DELETE_CONVERSATION:
            mMbox.deleteConversation(id(args[0]), param(args, 1));
            break;
        case DELETE_INCOMING_FILTER_RULE:
            doDeleteIncomingFilterRule(args);
            break;
        case DELETE_OUTGOING_FILTER_RULE:
            doDeleteOutgoingFilterRule(args);
            break;
        case DELETE_FOLDER:
            mMbox.deleteFolder(lookupFolderId(args[0]));
            break;
        case DELETE_IDENTITY:
            mMbox.deleteIdentity(args[0]);
            break;
        case DELETE_ITEM:
            mMbox.deleteItem(id(args[0]), param(args, 1));
            break;
        case DELETE_MESSAGE:
            mMbox.deleteMessage(id(args[0]));
            break;
        case DELETE_SIGNATURE:
            mMbox.deleteSignature(lookupSignatureId(args[0]));
            break;
        case DELETE_TAG:
            mMbox.deleteTag(lookupTag(args[0]).getId());
            break;
        case DUMPSTER_DELETE_ITEM:
            mMbox.dumpsterDeleteItem(id(args[0]));
            break;
        case EMPTY_DUMPSTER:
            mMbox.emptyDumpster();
            break;
        case EMPTY_FOLDER:
            mMbox.emptyFolder(lookupFolderId(args[0]));
            break;
        case EXIT:
            return ExecuteStatus.EXIT;
            //break;
        case FLAG_CONTACT:
            mMbox.flagContact(id(args[0]), paramb(args, 1, true));
            break;
        case FLAG_CONVERSATION:
            mMbox.flagConversation(id(args[0]), paramb(args, 1, true), param(args, 2));
            break;
        case FLAG_ITEM:
            mMbox.flagItem(id(args[0]), paramb(args, 1, true), param(args, 2));
            break;
        case FLAG_MESSAGE:
            mMbox.flagMessage(id(args[0]), paramb(args, 1, true));
            break;
        case GET_ALL_CONTACTS:
            doGetAllContacts(args);
            break;
        case GET_CONTACTS:
            doGetContacts(args);
            break;
        case GET_IDENTITIES:
            doGetIdentities();
            break;
            case GET_SIGNATURES:
            doGetSignatures();
            break;
        case GET_ALL_FOLDERS:
            doGetAllFolders();
            break;
        case GET_ALL_TAGS:
            doGetAllTags();
            break;
        case GET_APPOINTMENT_SUMMARIES:
            doGetAppointmentSummaries(args);
            break;
        case GET_CONVERSATION:
            doGetConversation(args);
            break;
        case GET_INCOMING_FILTER_RULES:
            doGetIncomingFilterRules();
            break;
        case GET_OUTGOING_FILTER_RULES:
            doGetOutgoingFilterRules();
            break;
        case GET_FOLDER:
            doGetFolder(args);
            break;
        case GET_FOLDER_REQUEST:
            doGetFolderRequest(args);
            break;
        case GET_FOLDER_GRANT:
            doGetFolderGrant(args);
            break;
        case GET_MAILBOX_SIZE:
            if (verboseOpt()) stdout.format("%d%n", mMbox.getSize());
            else stdout.format("%s%n", formatSize(mMbox.getSize()));
            break;
        case GET_MESSAGE:
            doGetMessage(args);
            break;
        case GET_RIGHTS:
            doGetRights(args);
            break;
        case GET_RELATED_CONTACTS:
            doGetRelatedContacts(args);
            break;
        case GET_REST_URL:
            doGetRestURL(args);
            break;
        case GET_SEARCH_HISTORY:
            doGetSearchHistory(args);
            break;
        case GRANT_RIGHT:
            doGrantRight(args);
            break;
        case HELP:
            doHelp(args);
            break;
        case IMPORT_URL_INTO_FOLDER:
            mMbox.importURLIntoFolder(lookupFolderId(args[0]), args[1]);
            break;
        case LIST_RIGHTS:
            doListRights();
            break;
        case MARK_CONVERSATION_READ:
            mMbox.markConversationRead(id(args[0]), paramb(args, 1, true), param(args, 2));
            break;
        case MARK_ITEM_READ:
            mMbox.markItemRead(id(args[0]), paramb(args, 1, true), param(args, 2));
            break;
        case MARK_FOLDER_READ:
            mMbox.markFolderRead(lookupFolderId(args[0]));
            break;
        case MARK_MESSAGE_READ:
            mMbox.markMessageRead(id(args[0]), paramb(args, 1, true));
            break;
        case MARK_CONVERSATION_SPAM:
            mMbox.markConversationSpam(id(args[0]), paramb(args, 1, true), lookupFolderId(param(args, 2)), param(args, 3));
            break;
        case MARK_MESSAGE_SPAM:
            mMbox.markMessageSpam(id(args[0]), paramb(args, 1, true), lookupFolderId(param(args, 2)));
            break;
        case MARK_TAG_READ:
            mMbox.markTagRead(lookupTag(args[0]).getId());
            break;
        case MODIFY_CONTACT:
            doModifyContact(args);
            break;
        case MODIFY_INCOMING_FILTER_RULE:
            doModifyIncomingFilterRule(args);
            break;
        case MODIFY_OUTGOING_FILTER_RULE:
            doModifyOutgoingFilterRule(args);
            break;
        case MODIFY_FOLDER_CHECKED:
            mMbox.modifyFolderChecked(lookupFolderId(args[0]), paramb(args, 1, true));
            break;
        case MODIFY_FOLDER_COLOR:
            mMbox.modifyFolderColor(lookupFolderId(args[0]), ZFolder.Color.fromString(args[1]));
            break;
        case MODIFY_FOLDER_EXCLUDE_FREE_BUSY:
            mMbox.modifyFolderExcludeFreeBusy(lookupFolderId(args[0]), paramb(args, 1, true));
            break;
        case MODIFY_FOLDER_GRANT:
            doModifyFolderGrant(args);
            break;
        case MODIFY_FOLDER_FLAGS:
            mMbox.updateFolder(lookupFolderId(args[0]), null, null, null, null, args[1], null);
            break;
        case MODIFY_FOLDER_URL:
            mMbox.modifyFolderURL(lookupFolderId(args[0]), args[1]);
            break;
        case MODIFY_IDENTITY:
            mMbox.modifyIdentity(new ZIdentity(args[0], getMultiMap(args, 1)));
            break;
        case MODIFY_ITEM_FLAGS:
            mMbox.updateItem(id(args[0]), null, null, args[1], null);
            break;
        case MODIFY_MOUNTPOINT_ENABLE_SHARED_REMINDER:
            mMbox.enableSharedReminder(lookupFolderId(args[0]), paramb(args, 1, false));
            break;
        case MODIFY_SIGNATURE:
            doModifySignature(args);
            break;
        case MODIFY_TAG_COLOR:
            mMbox.modifyTagColor(lookupTag(args[0]).getId(), Color.fromString(args[1]));
            break;
        case MOVE_CONVERSATION:
            mMbox.moveConversation(id(args[0]), lookupFolderId(param(args, 1)), param(args, 2));
            break;
        case MOVE_ITEM:
            mMbox.moveItem(id(args[0]), lookupFolderId(param(args, 1)), param(args, 2));
            break;
        case MOVE_MESSAGE:
            mMbox.moveMessage(id(args[0]), lookupFolderId(param(args, 1)));
            break;
        case MOVE_CONTACT:
            mMbox.moveContact(id(args[0]), lookupFolderId(param(args, 1)));
            break;
        case NOOP:
            doNoop(args);
            break;
        case POST_REST_URL:
            doPostRestURL(args);
            break;
        case RECOVER_ITEM:
            mMbox.recoverItem(id(args[0]), lookupFolderId(param(args, 1)));
            break;
        case REJECT_SAVED_SEARCH_PROMPT:
            mMbox.rejectSaveSearchFolderPrompt(args[0]);
            break;
        case RENAME_FOLDER:
            mMbox.renameFolder(lookupFolderId(args[0]), args[1]);
            break;
        case RENAME_SIGNATURE:
            doRenameSignature(args);
            break;
        case RENAME_TAG:
            mMbox.renameTag(lookupTag(args[0]).getId(), args[1]);
            break;
        case REVOKE_RIGHT:
            doRevokeRight(args);
            break;
        case SEARCH:
            doSearch(args);
            break;
        case SEARCH_CONVERSATION:
            doSearchConv(args);
            break;
        case SEARCH_SUGGEST:
            doSearchSuggest(args);
            break;
        case SELECT_MAILBOX:
            doSelectMailbox(args);
            break;
        case SYNC_FOLDER:
            mMbox.syncFolder(lookupFolderId(args[0]));
            break;
        case TAG_CONTACT:
            mMbox.tagContact(id(args[0]), lookupTag(args[1]).getId(), paramb(args, 2, true));
            break;
        case TAG_CONVERSATION:
            mMbox.tagConversation(id(args[0]), lookupTag(args[1]).getId(), paramb(args, 2, true), param(args, 3));
            break;
        case TAG_ITEM:
            mMbox.tagItem(id(args[0]), lookupTag(args[1]).getId(), paramb(args, 2, true), param(args, 3));
            break;
        case TAG_MESSAGE:
            mMbox.tagMessage(id(args[0]), lookupTag(args[1]).getId(), paramb(args, 2, true));
            break;
        case WHOAMI:
            dumpMailboxConnect();
            break;
        default:
            throw ZClientException.CLIENT_ERROR("Unhandled command: ("+mCommand.name()+ ")", null);
        }
        return ExecuteStatus.OK;
    }

    private final ZEventHandler mTraceHandler = new TraceHandler();

    private static class TraceHandler extends ZEventHandler {

        @Override public void handleRefresh(ZRefreshEvent refreshEvent, ZMailbox mailbox) {
            stdout.println("ZRefreshEvent: "+refreshEvent);
        }

        @Override public void handleModify(ZModifyEvent event, ZMailbox mailbox) {
            stdout.println(event.getClass().getSimpleName()+": "+event);
        }

           @Override public void handleCreate(ZCreateEvent event, ZMailbox mailbox) {
            stdout.println(event.getClass().getSimpleName()+": "+ event);
        }

           @Override public void handleDelete(ZDeleteEvent event, ZMailbox mailbox) {
               stdout.println("ZDeleteEvent: "+event);
           }
    }

    private void doNoop(String[] args) throws ServiceException {
        if (args.length == 0 || !args[0].equals("-t"))
            mMbox.noOp();
        else {
            mMbox.addEventHandler(mTraceHandler);
            while(true) {
                stdout.println("NoOp: "+LdapDateUtil.toGeneralizedTime(new Date()));
                mMbox.noOp();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        //testCreateAppt();

    }

    /*


<CreateAppointmentRequest xmlns="urn:zimbraMail">
 <m d="1173202005230" l="10">
  <inv>
   <comp status="CONF" fb="B" transp="O" allDay="0" name="test yearly">
    <s tz="(GMT-08.00) Pacific Time (US &amp; Canada)" d="20070308T130000"/>
    <e tz="(GMT-08.00) Pacific Time (US &amp; Canada)" d="20070308T150000"/>
      <or a="user1@slapshot.liquidsys.com"/>
       <recur>
        <add>
         <rule freq="YEA">
            <interval ival="1"/>
         </rule>
        </add>
      </recur>
    </comp></inv><su>test yearly</su><mp ct="multipart/alternative"><mp ct="text/plain"><content></content></mp><mp ct="text/html"><content>&lt;html&gt;&lt;body&gt;&lt;/body&gt;&lt;/html&gt;</content></mp></mp></m></CreateAppointmentRequest>
     */
/*
    private void testCreateAppt() throws ServiceException {

        ZMailbox.ZOutgoingMessage message = new ZOutgoingMessage();
        message.setSubject("test zclient API");
        message.setMessagePart(new MessagePart("text/plain", "this is da body"));
        ZInvite invite = new ZInvite();
        ZComponent comp = new ZComponent();
        comp.setStart(new ZDateTime("20070309T170000", mMbox.getPrefs().getTimeZoneWindowsId()));
        comp.setEnd(new ZDateTime("20070309T210000", mMbox.getPrefs().getTimeZoneWindowsId()));
        comp.setOrganizer(new ZOrganizer(mMbox.getName()));
        comp.setName("test zclient API");
        comp.setLocation("Zimbra");
        invite.getComponents().add(comp);

        ZAppointmentResult response = mMbox.createAppointment(ZFolder.ID_CALENDAR, null, message, invite, null);
        stdout.printf("calItemId(%s) inviteId(%s)%n", response.getCalItemId(), response.getInviteId());
    }
*/

    private void doGetAppointmentSummaries(String args[]) throws ServiceException {
        long startTime = DateUtil.parseDateSpecifier(args[0], new Date().getTime());
        long endTime = DateUtil.parseDateSpecifier(args[1], (new Date().getTime()) + Constants.MILLIS_PER_WEEK);
        String folderId = args.length == 3 ? lookupFolderId(args[2]) : null;
        List<ZApptSummaryResult> results = mMbox.getApptSummaries(null, startTime, endTime, new String[] {folderId}, TimeZone.getDefault(), ZSearchParams.TYPE_APPOINTMENT);
        if (results.size() != 1) return;
        ZApptSummaryResult result = results.get(0);

        stdout.print("[");
        boolean first = true;
        for (ZAppointmentHit appt : result.getAppointments()) {
            if (!first) stdout.println(",");
            stdout.print(ZJSONObject.toString(appt));
            if (first) first = false;
        }
        stdout.println("]");
    }

    /*
    addFilterRule(afrl)
  --before {existing-rule-name}
  --after {existing-rule-name}
  --first
  --last

  {name}  [*active|inactive] [any|*all] {conditions}+ {actions}+
    */

    private void doAddIncomingFilterRule(String[] args) throws ServiceException {
        ZFilterRules rules = mMbox.getIncomingFilterRules();
        doAddFilterRule(args, rules);
        mMbox.saveIncomingFilterRules(rules);
    }

    private void doAddOutgoingFilterRule(String[] args) throws ServiceException {
        ZFilterRules rules = mMbox.getOutgoingFilterRules();
        doAddFilterRule(args, rules);
        mMbox.saveOutgoingFilterRules(rules);
    }

    private void doAddFilterRule(String[] args, ZFilterRules rules) throws ServiceException {
        ZFilterRule newRule = ZFilterRule.parseFilterRule(args);
        List<ZFilterRule> list = rules.getRules();
        if (firstOpt()) {
            list.add(0, newRule);
        } else if (afterOpt() != null) {
            boolean found = false;
            String name = afterOpt();
            for (int i=0; i < list.size(); i++) {
                found = list.get(i).getName().equalsIgnoreCase(name);
                if (found) {
                    if (i+1 >= list.size())
                        list.add(newRule);
                    else
                        list.add(i+1, newRule);
                    break;
                }
            }
            if (!found) {
                throw ZClientException.CLIENT_ERROR("can't find rule: "+name, null);
            }
        } else if (beforeOpt() != null) {
            String name = beforeOpt();
            boolean found = false;
            for (int i=0; i < list.size(); i++) {
                found = list.get(i).getName().equalsIgnoreCase(name);
                if (found) {
                    list.add(i, newRule);
                    break;
                }
            }
            if (!found) {
                throw ZClientException.CLIENT_ERROR("can't find rule: "+name, null);
            }
        } else {
            // add to end
            list.add(newRule);
        }
    }

    private void doModifyIncomingFilterRule(String[] args) throws ServiceException {
        ZFilterRules rules = mMbox.getIncomingFilterRules(true);
        doModifyFilterRule(args, rules);
        mMbox.saveIncomingFilterRules(rules);
    }

    private void doModifyOutgoingFilterRule(String[] args) throws ServiceException {
        ZFilterRules rules = mMbox.getOutgoingFilterRules(true);
        doModifyFilterRule(args, rules);
        mMbox.saveOutgoingFilterRules(rules);
    }

    private static void doModifyFilterRule(String[] args, ZFilterRules rules) throws ServiceException {
        ZFilterRule modifiedRule = ZFilterRule.parseFilterRule(args);
        List<ZFilterRule> list = rules.getRules();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equalsIgnoreCase(modifiedRule.getName())) {
                list.set(i, modifiedRule);
                return;
            }
        }
        throw ZClientException.CLIENT_ERROR("can't find rule: " + args[0], null);
    }

    private void doDeleteIncomingFilterRule(String[] args) throws ServiceException {
        ZFilterRules rules = mMbox.getIncomingFilterRules(true);
        doDeleteFilterRule(args, rules);
        mMbox.saveIncomingFilterRules(rules);
    }

    private void doDeleteOutgoingFilterRule(String[] args) throws ServiceException {
        ZFilterRules rules = mMbox.getOutgoingFilterRules(true);
        doDeleteFilterRule(args, rules);
        mMbox.saveOutgoingFilterRules(rules);
    }

    private static void doDeleteFilterRule(String[] args, ZFilterRules rules) throws ServiceException {
        String name = args[0];
        List<ZFilterRule> list = rules.getRules();
        for (int i=0; i < list.size(); i++) {
            if (list.get(i).getName().equalsIgnoreCase(name)) {
                list.remove(i);
                return;
            }
        }
        throw ZClientException.CLIENT_ERROR("can't find rule: " + args, null);
    }

    private void doGetIncomingFilterRules() throws ServiceException {
        ZFilterRules rules = mMbox.getIncomingFilterRules(true);
        printFilterRules(rules);
    }

    private void doGetOutgoingFilterRules() throws ServiceException {
        ZFilterRules rules = mMbox.getOutgoingFilterRules(true);
        printFilterRules(rules);
    }

    private static void printFilterRules(ZFilterRules rules) {
        for (ZFilterRule r : rules.getRules()) {
            stdout.println(r.generateFilterRule());
        }
    }

    private String getGranteeDisplay(GranteeType type) {
        switch (type) {
            case usr: return "account";
            case grp: return "group";
            case cos: return "cos";
            case pub: return "public";
            case all: return "all";
            case dom: return "domain";
            case guest: return "guest";
            case key: return "key";
            default: return "unknown";
        }
    }

    /*
     * after bug 40178, grantee names are not returned in the refresh block,
     * they are only returned in GetFolderResponse.
     * lookupFolder returns the cached folder (inited from the refresh block), after that,
     * do a GetFolderRequest with the folder id to get the full grant info that includes grantee name.
     */
    private ZFolder getFolderWithFullGrantInfo(String pathOrId) throws ServiceException {
        ZFolder f = lookupFolder(pathOrId);
        return mMbox.getFolderRequestById(f.getId());
    }

    private void doGetFolderGrant(String[] args) throws ServiceException {
        ZFolder f = getFolderWithFullGrantInfo(args[0]);

        if (verboseOpt()) {
            StringBuilder sb = new StringBuilder();
            for (ZGrant g : f.getGrants()) {
                if (sb.length() > 0) sb.append(",\n");
                sb.append(g.dump());
            }
            stdout.format("[%n%s%n]%n", sb.toString());
        } else {
            String format = "%11.11s  %8.8s  %s%n";
            stdout.format(format, "Permissions", "Type",     "Display");
            stdout.format(format, "-----------", "--------", "-------");

            for (ZGrant g : f.getGrants()) {
                GranteeType gt = g.getGranteeType();
                String dn = (gt == GranteeType.all || gt == GranteeType.pub) ? "" :
                            ((gt == GranteeType.guest || gt == GranteeType.key) ? g.getGranteeId() :
                            (g.getGranteeName() != null ? g.getGranteeName() :
                            g.getGranteeId()));
                stdout.format(format, g.getPermissions(), getGranteeDisplay(g.getGranteeType()), dn);
            }
        }
    }

    private GranteeType getGranteeType(String name) throws ServiceException {
        if (name.equalsIgnoreCase("account")) return GranteeType.usr;
        else if (name.equalsIgnoreCase("group")) return GranteeType.grp;
        else if (name.equalsIgnoreCase("cos")) return GranteeType.cos;
        else if (name.equalsIgnoreCase("public")) return GranteeType.pub;
        else if (name.equalsIgnoreCase("all")) return GranteeType.all;
        else if (name.equalsIgnoreCase("domain")) return GranteeType.dom;
        else if (name.equalsIgnoreCase("guest")) return GranteeType.guest;
        else if (name.equalsIgnoreCase("key")) return GranteeType.key;
        else throw ZClientException.CLIENT_ERROR("unknown grantee type: "+name, null);
    }

    private void doModifyFolderGrant(String[] args) throws ServiceException {
        String folderId = lookupFolderId(args[0], false);

        GranteeType type = getGranteeType(args[1]);
        String grantee = null;
        String perms = null;
        String password = null;
        switch (type) {
        case usr:
        case grp:
        case cos:
        case dom:
            if (args.length != 4) {
                throw ZClientException.CLIENT_ERROR("not enough args", null);
            }
            grantee = args[2];
            perms = args[3];
            break;
        case pub:
            grantee = GuestAccount.GUID_PUBLIC;
            perms = args[2];
            break;
        case all:
            grantee = GuestAccount.GUID_AUTHUSER;
            perms = args[2];
            break;
        case guest:
            if (args.length != 4 && args.length != 5) throw ZClientException.CLIENT_ERROR("not enough args", null);
            grantee = args[2];
            if (args.length == 5) {
                password = args[3];
                perms = args[4];
            } else {
                password = null;
                perms = args[3];
            }
            break;
        case key:
            if (args.length != 4 && args.length != 5) throw ZClientException.CLIENT_ERROR("not enough args", null);
            grantee = args[2];
            if (args.length == 5) {
                password = args[3];
                perms = args[4];
            } else {
                perms = args[3];
            }
            break;
        }

        boolean revoke = (perms != null && (perms.equalsIgnoreCase("none") || perms.length() == 0));

        if (revoke) {
            // convert grantee to grantee id if it is a name
            ZFolder f = getFolderWithFullGrantInfo(folderId);
            String zid = null;
            for (ZGrant g : f.getGrants()) {
                if (grantee.equalsIgnoreCase(g.getGranteeName()) ||
                    grantee.equalsIgnoreCase(g.getGranteeId())) {
                    zid = g.getGranteeId();
                    break;
                }
            }

            if (zid != null || (type == GranteeType.all || type == GranteeType.pub)) {
                if (zid != null)
                    grantee = zid;
                mMbox.modifyFolderRevokeGrant(folderId, grantee);
            } else {
                // zid is null
                /*
                 * It could be we are trying to revoke a grant on a sub folder.
                 * e.g. /top/sub
                 *      mfg /top account user2 r
                 *      mfg /top/sub account user2 none
                 *      or
                 *      mfg /top account all r
                 *      mfg /top/sub account user3 none
                 *
                 * or simply just want to grant "no right" to a user
                 * e.g.
                 *      mfg /top account user2 none
                 *
                 * If this is the case zid wil be null because there is no such
                 * grant on the specified folder.   Just let it go through by issuing
                 * a grant action, instead of revoke.
                 */
                mMbox.modifyFolderGrant(folderId, type, grantee, "", password);
            }


        } else {
            mMbox.modifyFolderGrant(folderId, type, grantee, perms, password);
        }
    }

    private void doListRights() throws ServiceException {
        for (Right r : RightManager.getInstance().getAllUserRights().values()) {
            stdout.println("  " + r.getName() + ": " + r.getDesc());
            stdout.println();
        }
    }


    private void doGetRights(String[] args) throws ServiceException {
        if (verboseOpt()) {
            StringBuilder sb = new StringBuilder();
            for (ZAce g : mMbox.getRights(args)) {
                if (sb.length() > 0) sb.append(",\n");
                sb.append(g.dump());
            }
            stdout.format("[%n%s%n]%n", sb.toString());
        } else {
            String format = "%16.16s  %8.8s  %s%n";
            stdout.format(format, "Right",            "Type",     "Display");
            stdout.format(format, "----------------", "--------", "-------");

            List<ZAce> result = mMbox.getRights(args);
            Comparator<ZAce> comparator = new Comparator<ZAce>() {
                @Override
                public int compare(ZAce a, ZAce b) {
                    // sort by right -> grantee type -> grantee name
                    String aKey = a.getRight() + a.getGranteeTypeSortOrder() + (a.getGranteeName()==null?"":a.getGranteeName());
                    String bKey = b.getRight() + b.getGranteeTypeSortOrder() + (b.getGranteeName()==null?"":b.getGranteeName());
                    int order = aKey.compareTo(bKey);
                    if (order == 0) // a grantee is denied and allowed, not likely, but put the deny before allow if such entry does exist
                        order = a.getDeny()?-1:1;
                    return order;
                }
            };
            Collections.sort(result, comparator);

            for (ZAce ace : result) {
                stdout.format(format, ace.getRightDisplay(), ace.getGranteeTypeDisplay(), ace.getGranteeName());
            }
        }
        stdout.println();
    }

    private ZAce getAceFromArgs(String[] args) throws ServiceException {
        ZAce.GranteeType type = ZAce.getGranteeTypeFromDisplay(args[0]);
        String granteeName = null;
        String granteeId = null;
        String right = null;
        boolean deny = false;
        String secret = null;

        switch (type) {
        case usr:
        case grp:
        case dom:
            if (args.length != 3) throw ZClientException.CLIENT_ERROR("wrong number of args", null);
            granteeName = args[1];
            right = args[2];
            break;
        case all:
            if (args.length != 2) throw ZClientException.CLIENT_ERROR("wrong number of args", null);
            granteeId = GuestAccount.GUID_AUTHUSER;
            right = args[1];
            break;
        case pub:
            if (args.length != 2) throw ZClientException.CLIENT_ERROR("wrong number of args", null);
            granteeId = GuestAccount.GUID_PUBLIC;
            right = args[1];
            break;
        case gst:
            if (args.length != 4) throw ZClientException.CLIENT_ERROR("wrong number of args", null);
            granteeName = args[1];
            secret = args[2];
            right = args[3];
            break;
        case key:
            if (args.length != 3 && args.length != 4) throw ZClientException.CLIENT_ERROR("wrong number of args", null);
            granteeName = args[1];
            if (args.length == 3) {
                right = args[2];
            } else {
                secret = args[2];
                right = args[3];
            }
            break;
        }

        if (right.charAt(0) == '-') {
            deny = true;
            right = right.substring(1);
        }

        return new ZAce(type, granteeId, granteeName, right, deny, secret);
    }

    private void doGrantRight(String[] args) throws ServiceException {
        ZAce ace = getAceFromArgs(args);
        List<ZAce> granted = mMbox.grantRight(ace);
        if (granted.size() == 0)
            stdout.println("  granted no right");
        else {
            stdout.println("  granted: ");
            for (ZAce g : granted)
                stdout.println("    " + g.getGranteeTypeDisplay() + " " + g.getGranteeName() + " " + g.getRightDisplay());
        }
    }

    private void doRevokeRight(String[] args) throws ServiceException {
        ZAce ace = getAceFromArgs(args);

        ZAce.GranteeType granteeType= ace.getGranteeType();
        if (granteeType == ZAce.GranteeType.usr || granteeType == ZAce.GranteeType.grp || granteeType == ZAce.GranteeType.dom) {
            // convert grantee to grantee id if it is a name
            String zid = null;
            String granteeName = ace.getGranteeName();
            String[] rights = new String[] {ace.getRight()};
            for (ZAce g : mMbox.getRights(rights)) {
                if (granteeName.equalsIgnoreCase(g.getGranteeName()) ||
                    granteeName.equalsIgnoreCase(g.getGranteeId())) {
                    zid = g.getGranteeId();
                    break;
                }
            }
            if (zid == null)
                throw ZClientException.CLIENT_ERROR("no such grant", null);
            ace.setGranteeId(zid);
        }

        List<ZAce> revoked = mMbox.revokeRight(ace);
        if (revoked.size() == 0)
            stdout.println("  revoked no right");
        else {
            stdout.println("  revoked: ");
            for (ZAce r : revoked)
                stdout.println("    " + r.getGranteeTypeDisplay() + " " + r.getGranteeName() + " " + r.getRightDisplay());
        }
    }

    private void doCheckRight(String[] args) throws ServiceException {
        String user = args[0];
        List<String> rights = new ArrayList<String>();
        rights.add(args[1]); // support only one right in CLI

        boolean allow =  mMbox.checkRights(user, rights);
        stdout.println((allow?"allowed":"not allowed"));
    }

    private void doAdminAuth(String[] args) throws ServiceException {
        adminAuth(args[0], args[1], urlOpt(true));
    }

    private void doAuth(String[] args) throws ServiceException {
        auth(args[0], args[1], param(args, 2, args[0]), urlOpt(true));
    }

    private void doSelectMailbox(String[] args) throws ServiceException {
        String targetAccount = args[0];
        String authAccount;
        if (mCommandLine.hasOption(O_AS_ADMIN.getLongOpt())) {
            authAccount = null;
        } else {
            authAccount = mCommandLine.getOptionValue(O_AUTH.getLongOpt());
            if (StringUtil.isNullOrEmpty(authAccount)) {
                authAccount = targetAccount;
            }
        }
        selectMailbox(authAccount, targetAccount, mProv);
    }

    private static PrintWriter stdout;
    private static PrintWriter stderr;
    private static Session mSession;
    static {
        try {
            stdout = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true);
            stderr = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"), true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.exit(1);
        }
        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        mSession = Session.getInstance(props);
        // Assume that most malformed base64 errors occur due to incorrect delimiters,
        // as opposed to errors in the data itself.  See bug 11213 for more details.
        System.setProperty("mail.mime.base64.ignoreerrors", "true");
    }

    private void addMessage(String folderId, String flags, String tags, long date, File file, boolean validate) throws ServiceException, IOException {
        //String aid = mMbox.uploadAttachments(new File[] {file}, 5000);

        InputStream in = new BufferedInputStream(new FileInputStream(file));  // Buffering required for gzip check
        long sizeHint = -1;
        if (ByteUtil.isGzipped(in)) {
            in = new GZIPInputStream(in);
        } else {
            sizeHint = file.length();
        }

        byte[] data = ByteUtil.getContent(in, (int) sizeHint);
        if (validate && !EmailUtil.isRfc822Message(new ByteArrayInputStream(data))) {
            throw ZClientException.CLIENT_ERROR(file.getPath() + " does not contain a valid RFC 822 message", null);
        }

        try {
            if (date == -1) {
                MimeMessage mm = new ZMimeMessage(mSession, new SharedByteArrayInputStream(data));
                Date d = mm.getSentDate();
                if (d != null) date = d.getTime();
                else date = 0;
            }
        } catch (MessagingException e) {
            date = 0;
        }
        String id = mMbox.addMessage(folderId, flags, tags, date, data, false);
        stdout.format("%s (%s)%n", id, file.getPath());
    }

    private void doAddMessage(String[] args) throws ServiceException, IOException {
        String folderId = lookupFolderId(args[0], false);
        String tags = tagsOpt();
        String flags = flagsOpt();
        long date = dateOpt(-1);
        boolean validate = validateOpt();

        for (int i=1; i < args.length; i++) {
            File file = new File(args[i]);
            if (file.isDirectory()) {
                // TODO: should we recurse?
                for (File child : file.listFiles()) {
                    if (child.isFile())
                        addMessage(folderId, flags, tags, date, child, validate);
                }
            } else {
                addMessage(folderId, flags, tags, date, file, validate);
            }
        }
    }

    private String emailAddrs(List<ZEmailAddress> addrs) {
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress e : addrs) {
            if (sb.length() >0) sb.append(", ");
            sb.append(e.getDisplay());
        }
        return sb.toString();
    }

    private void doCreateFolder(String args[]) throws ServiceException {
        ZFolder cf = mMbox.createFolder(
                lookupFolderId(args[0], true),
                ZMailbox.getBasePath(args[0]),
                folderViewOpt(),
                folderColorOpt(),
                flagsOpt(),
                urlOpt(false));
        stdout.println(cf.getId());
    }

    private void doCreateSignature(String args[]) throws ServiceException {
        ZSignature sig = new ZSignature(args[0], args[1]);
        stdout.println(mMbox.createSignature(sig));
    }

    private void doModifySignature(String args[]) throws ServiceException {
        ZSignature sig = lookupSignature(args[0]);
        ZSignature modSig = new ZSignature(sig.getId(), sig.getName(), args[1]);
        mMbox.modifySignature(modSig);
    }

    private void doRenameSignature(String args[]) throws ServiceException {
        ZSignature sig = lookupSignature(args[0]);
        ZSignature modSig = new ZSignature(sig.getId(), args[1], sig.getValue());
        mMbox.modifySignature(modSig);
    }

    private ZSignature lookupSignature(String idOrName) throws ServiceException {
        for (ZSignature sig : mMbox.getSignatures()) {
            if (sig.getName().equalsIgnoreCase(idOrName) || sig.getId().equalsIgnoreCase(idOrName))
                return sig;
        }
        throw ZClientException.CLIENT_ERROR("unknown signature: "+idOrName, null);
    }

    private String lookupSignatureId(String idOrName) throws ServiceException {
        if (StringUtil.isUUID(idOrName)) {
            return idOrName;
        } else {
            for (ZSignature sig : mMbox.getSignatures()) {
                if (sig.getName().equalsIgnoreCase(idOrName)) {
                    return sig.getId();
                }
            }
            throw ZClientException.CLIENT_ERROR("unknown signature: "+idOrName, null);
        }
    }

    private void doCreateSearchFolder(String args[]) throws ServiceException {
        ZSearchFolder csf = mMbox.createSearchFolder(
                lookupFolderId(args[0], true),
                ZMailbox.getBasePath(args[0]),
                args[1],
                typesOpt(),
                searchSortByOpt(),
                folderColorOpt());
        stdout.println(csf.getId());
    }

    private void doCreateMountpoint(String args[]) throws ServiceException {
        String cmPath = args[0];
        String cmOwner = args[1];
        String cmItem = args[2];
        boolean reminderEnabled = paramb(args, 3, false);

        OwnerBy ownerBy = OwnerBy.BY_NAME;
        if (StringUtil.isUUID(cmOwner)) {
            ownerBy = OwnerBy.BY_ID;
        }

        SharedItemBy sharedItemBy = SharedItemBy.BY_PATH;
        String sharedItem = cmItem;

        int colonAt = cmItem.indexOf(':');
        if (colonAt != -1 && colonAt != 0 && colonAt != cmItem.length()-1) {
            String itemOwnerId = cmItem.substring(0, colonAt);
            String itemId = cmItem.substring(colonAt+1);
            if (StringUtil.isUUID(itemOwnerId)) {
                sharedItemBy = SharedItemBy.BY_ID;
                sharedItem = itemId;
            }
        }

        ZMountpoint cm = mMbox.createMountpoint(lookupFolderId(cmPath, true), ZMailbox.getBasePath(cmPath),
                    folderViewOpt(), folderColorOpt(), flagsOpt(), ownerBy, cmOwner, sharedItemBy, sharedItem, reminderEnabled);
        stdout.println(cm.getId());
    }

    private void doEnableSharedReminder(String args[]) throws ServiceException {

    }

    private void doSearch(String[] args) throws ServiceException {

        if (currrentOpt()) {
            doSearchRedisplay();
            return;
        } else if (previousOpt()) {
            doSearchPrevious();
            return;
        } else if (nextOpt()) {
            doSearchNext();
            return;
        } else if (args.length == 0) {
            usage();
            return;
        }

        mSearchParams = new ZSearchParams(args[0]);

//        [limit {limit}] [sortby {sortBy}] [types {types}]

        mSearchParams.setLimit(getOptLimit());

        SearchSortBy sortBy = searchSortByOpt();
        mSearchParams.setSortBy(sortBy != null ?  sortBy : SearchSortBy.dateDesc);

        String types = typesOpt();
        mSearchParams.setTypes(types != null ? types : ZSearchParams.TYPE_CONVERSATION);

        mSearchParams.setInDumpster(mCommandLine.hasOption(O_DUMPSTER.getLongOpt()));
        mIndexToId.clear();
        mSearchPage = 0;
        ZSearchPagerResult pager = mMbox.search(mSearchParams, mSearchPage, false, false);
        //stdout.println(result);
        dumpSearch(pager.getResult(), verboseOpt());
    }

    private int getOptLimit() throws ServiceException {
        int limit;
        try {
            limit = Integer.parseInt(mCommandLine.getOptionValue(O_LIMIT.getOpt(), "25"));
        } catch (NumberFormatException e) {
            throw ZClientException.CLIENT_ERROR("limit must be a number", e);
        }
        if (limit <= 0 || limit > 1000) {
            throw ZClientException.CLIENT_ERROR("limit must be 1-1000", null);
        }
        return limit;
    }

    private void doSearchRedisplay() throws ServiceException {
        if (mSearchParams == null) return;
        ZSearchPagerResult pager = mMbox.search(mSearchParams, mSearchPage, true, false);
        mSearchPage = pager.getActualPage();
        if (pager.getResult().getHits().size() == 0) return;
        dumpSearch(pager.getResult(), verboseOpt());
    }

    private void doSearchNext() throws ServiceException {
        if (mSearchParams == null) return;
        ZSearchPagerResult pager = mMbox.search(mSearchParams, ++mSearchPage, true, false);
        mSearchPage = pager.getActualPage();
        if (pager.getResult().getHits().size() == 0) return;
        dumpSearch(pager.getResult(), verboseOpt());
    }

    private void doSearchPrevious() throws ServiceException {
        if (mSearchParams == null || mSearchPage == 0)
            return;
        ZSearchPagerResult pager = mMbox.search(mSearchParams, --mSearchPage, true, false);
        mSearchPage = pager.getActualPage();
        if (pager.getResult().getHits().size() == 0) return;
        dumpSearch(pager.getResult(), verboseOpt());
    }

    String mConvSearchConvId;

    private void doSearchConv(String[] args) throws ServiceException {

        if (currrentOpt()) {
            doSearchConvRedisplay();
            return;
        } else if (previousOpt()) {
            doSearchConvPrevious();
            return;
        } else if (nextOpt()) {
            doSearchConvNext();
            return;
        } else if (args.length != 2) {
            usage();
            return;
        }

        mConvSearchConvId = id(args[0]);
        mConvSearchParams = new ZSearchParams(args[1]);

//        [limit {limit}] [sortby {sortBy}] [types {types}]

        mConvSearchParams.setLimit(getOptLimit());

        SearchSortBy sortBy = searchSortByOpt();
        mConvSearchParams.setSortBy(sortBy != null ?  sortBy : SearchSortBy.dateDesc);

        String types = typesOpt();
        mConvSearchParams.setTypes(types != null ? types : ZSearchParams.TYPE_CONVERSATION);

        mIndexToId.clear();
        //stdout.println(result);
        dumpConvSearch(mMbox.searchConversation(mConvSearchConvId, mConvSearchParams), verboseOpt());
    }

    private void doClearSearchHistory() throws ServiceException {
        mMbox.clearSearchHistory();
    }

    private void doSearchSuggest(String[] args) throws ServiceException {
        String prefix = args[0];
        if (args.length == 1) {
            dumpSearches(mMbox.getSearchSuggestions(prefix), "Search Suggestions");
        } else {
            int limit = Integer.parseInt(args[1]);
            dumpSearches(mMbox.getSearchSuggestions(prefix, limit), "Search Suggestions");
        }
    }

    private void doGetSearchHistory(String[] args) throws ServiceException {
        if (args.length == 0) {
            dumpSearches(mMbox.getSearchHistory(), "Search History");
        } else {
            int limit = Integer.parseInt(args[0]);
            dumpSearches(mMbox.getSearchHistory(limit), "Search History");
        }
    }

    private void dumpSearches(List<String> searches, String header) {
        if (searches.isEmpty()) {
            stdout.println("no results");
            return;
        }
        int maxLength = searches.stream().map(str -> str.length()).max(Integer::compare).get();
        stdout.println(header);
        stdout.println(StringUtils.repeat("-", maxLength));
        for (String s: searches) {
            stdout.println(s);
        }
    }

    private void doGetRelatedContacts(String[] args) throws ServiceException {
        String requestedAffinityType = AffinityType.all.name();
        Integer limit = null;
        List<RelatedContactsTarget> targets = Lists.newArrayList();
        for (int i=0; i<=args.length-1; i++) {
            String arg = args[i];
            String targetEmail;
            String targetAffinity;
            if (arg.indexOf(":") > -1) {
                String[] tokens = arg.split(":", 2);
                String tok = tokens[0];
                String val = tokens[1];
                if (tok.equals("limit")) {
                    limit = Integer.valueOf(val);
                    continue;
                } else if (tok.equals("field")) {
                    requestedAffinityType = val;
                    continue;
                } else {
                    targetAffinity = tok;
                    targetEmail = val;
                }
            } else {
                targetAffinity = "";
                targetEmail = arg;
            }
            targets.add(new RelatedContactsTarget(targetEmail, targetAffinity));
        }
        GetRelatedContactsResponse resp = mMbox.getRelatedContacts(targets, requestedAffinityType, limit);
        List<String> relatedContacts = resp.getRelatedContacts().stream().map(r -> toRelatedContactRow(r)).collect(Collectors.toList());
        dumpSearches(relatedContacts, "Related Contacts");
    }

    private String toRelatedContactRow(RelatedContactResult result) {
        String email = result.getEmail();
        String name = result.getName();
        int scope = result.getScope();
        if (name != null) {
            return String.format("[scope=%d] %s (%s)", scope, email, name);
        } else {
            return String.format("[scope=%d] %s", scope, email);
        }
    }

    private void doSearchConvRedisplay() {
        ZSearchResult sr = mConvSearchResult;
        if (sr == null) return;
        dumpConvSearch(mConvSearchResult, verboseOpt());
    }

    private void doSearchConvNext() throws ServiceException {
        ZSearchParams sp = mConvSearchParams;
        ZSearchResult sr = mConvSearchResult;
        if (sp == null || sr == null || !sr.hasMore())
            return;

        List<ZSearchHit> hits = sr.getHits();
        if (hits.size() == 0) return;
        sp.setOffset(sp.getOffset() + hits.size());
        dumpConvSearch(mMbox.searchConversation(mConvSearchConvId, sp), verboseOpt());
    }

    private void doSearchConvPrevious() throws ServiceException {
        ZSearchParams sp = mConvSearchParams;
        ZSearchResult sr = mConvSearchResult;
        if (sp == null || sr == null || sp.getOffset() == 0)
            return;
        sp.setOffset(sp.getOffset() - sr.getHits().size());
        dumpConvSearch(mMbox.searchConversation(mConvSearchConvId, sp), verboseOpt());
    }

    private int colWidth(int num) {
        int i = 1;
        while (num >= 10) {
            i++;
            num /= 10;
        }
        return i;
    }

    private void dumpSearch(ZSearchResult sr, boolean verbose) {
        if (verbose) {
            stdout.println(sr.dump());
            return;
        }

        int offset = mSearchPage * mSearchParams.getLimit();
        int first = offset+1;
        int last = offset+sr.getHits().size();

        if (sr.hasSavedSearchPrompt()) {
            stdout.printf("num: %d, more: %s, saveSearchPrompt=true%n%n", sr.getHits().size(), sr.hasMore());
        } else {
            stdout.printf("num: %d, more: %s%n%n", sr.getHits().size(), sr.hasMore());
        }
        int width = colWidth(last);

        if (sr.getHits().size() == 0) {
            return;
        }

        final int FROM_LEN = 20;
        int id_len = 4;
        for (ZSearchHit hit: sr.getHits()) {
            id_len = Math.max(id_len, hit.getId().length());
        }
        Calendar cal = Calendar.getInstance();
        String headerFormat = String.format("%%%d.%ds  %%%d.%ds  %%4s   %%-20.20s  %%-50.50s  %%s%%n",
                width, width, id_len, id_len);
        String itemFormat = String.format(  "%%%d.%ds. %%%d.%ds  %%4s   %%-20.20s  %%-50.50s  %%tD %%<tR%%n",
                width, width, id_len, id_len);
        stdout.format(headerFormat, "", "Id", "Type", "From", "Subject", "Date");
        stdout.format(headerFormat, "",
                "----------------------------------------------------------------------------------------------------",
                "----", "--------------------", "--------------------------------------------------", "--------------");
        int i = first;
        for (ZSearchHit hit: sr.getHits()) {
            if (hit instanceof ZConversationHit) {
                ZConversationHit ch = (ZConversationHit) hit;
                cal.setTimeInMillis(ch.getDate());
                String sub = ch.getSubject();
                String from = emailAddrs(ch.getRecipients());
                if (ch.getMessageCount() > 1) {
                    String numMsg = " ("+ch.getMessageCount()+")";
                    int space = FROM_LEN - numMsg.length();
                    from = ( (from.length() < space) ? from : from.substring(0, space)) + numMsg;
                }
                //if (ch.getFragment() != null || ch.getFragment().length() > 0)
                //    sub += " (" + ch.getFragment()+")";
                mIndexToId.put(i, ch.getId());
                stdout.format(itemFormat, i++, ch.getId(), "conv", from, sub, cal);
            } else if (hit instanceof ZContactHit) {
                ZContactHit ch = (ZContactHit) hit;
                cal.setTimeInMillis(ch.getDate());
                String from = getFirstEmail(ch);
                String sub = ch.getFileAsStr();
                mIndexToId.put(i, ch.getId());
                stdout.format(itemFormat, i++, ch.getId(), "cont", from, sub, cal);
            } else if (hit instanceof ZMessageHit) {
                ZMessageHit mh = (ZMessageHit) hit;
                cal.setTimeInMillis(mh.getDate());
                String sub = mh.getSubject();
                String from = mh.getSender() == null ? "<none>" : mh.getSender().getDisplay();
                mIndexToId.put(i, mh.getId());
                stdout.format(itemFormat, i++, mh.getId(), "mess", from, sub, cal);
            } else if (hit instanceof ZAppointmentHit) {
                ZAppointmentHit ah = (ZAppointmentHit) hit;
                if (ah.getInstanceExpanded()) {
                    cal.setTimeInMillis(ah.getStartTime());
                } else {
                    cal.setTimeInMillis(ah.getHitDate());
                }
                String sub = ah.getName();
                String from = "<na>";
                mIndexToId.put(i, ah.getId());
                stdout.format(itemFormat, i++, ah.getId(), ah.getIsTask() ? "task" : "appo", from, sub, cal);
            } else if (hit instanceof ZDocumentHit) {
                ZDocumentHit dh = (ZDocumentHit) hit;
                ZDocument doc = dh.getDocument();
                cal.setTimeInMillis(doc.getModifiedDate());
                String name = doc.getName();
                String editor = doc.getEditor();
                mIndexToId.put(i, dh.getId());
                stdout.format(itemFormat, i++, dh.getId(), doc.isWiki()?"wiki":"doc", editor, name, cal);
            }
        }
        stdout.println();
    }

    private String getFirstEmail(ZContactHit ch) {
        if (ch.getEmail() != null) return ch.getEmail();
        else if (ch.getEmail2() != null) return ch.getEmail2();
        else if (ch.getEmail3() != null) return ch.getEmail3();
        else return "<none>";
    }

    private void dumpConvSearch(ZSearchResult sr, boolean verbose) {
        mConvSearchResult =  sr;
        if (verbose) {
            stdout.println(sr.dump());
            return;
        }

        int offset = sr.getOffset();
        int first = offset+1;
        int last = offset+sr.getHits().size();

        stdout.printf("num: %d, more: %s%n%n", sr.getHits().size(), sr.hasMore());
        int width = colWidth(last);

        if (sr.getHits().size() == 0) {
            return;
        }

        int id_len = 4;
        for (ZSearchHit hit: sr.getHits()) {
            id_len = Math.max(id_len, hit.getId().length());
        }

        Calendar c = Calendar.getInstance();
        String headerFormat = String.format("%%%d.%ds  %%%d.%ds  %%-20.20s  %%-50.50s  %%s%%n", width, width, id_len, id_len);
        //String headerFormat = String.format("%10.10s  %-20.20s  %-50.50s  %-6.6s  %s%n");

        String itemFormat = String.format(  "%%%d.%ds. %%%d.%ds  %%-20.20s  %%-50.50s  %%tD %%<tR%%n", width, width,id_len, id_len);
        //String itemFormat = "%10.10s  %-20.20s  %-50.50s  %-6.6s  %tD %5$tR%n";

        stdout.format(headerFormat, "", "Id", "From", "Subject", "Date");
        stdout.format(headerFormat, "", "----------------------------------------------------------------------------------------------------", "--------------------", "--------------------------------------------------", "--------------");
        int i = first;
        for (ZSearchHit hit: sr.getHits()) {
            if (hit instanceof ZMessageHit) {
                ZMessageHit mh = (ZMessageHit) hit;
                c.setTimeInMillis(mh.getDate());
                String sub = mh.getSubject();
                String from = mh.getSender().getDisplay();
                mIndexToId.put(i, mh.getId());
                stdout.format(itemFormat, i++, mh.getId(), from, sub, c);
            }
        }
        stdout.println();
    }

    private void doGetAllTags() throws ServiceException {
        if (verboseOpt()) {
            StringBuilder sb = new StringBuilder();
            for (String tagName: mMbox.getAllTagNames()) {
                ZTag tag = mMbox.getTagByName(tagName);
                if (sb.length() > 0) sb.append(",\n");
                sb.append(tag.dump());
            }
            stdout.format("[%n%s%n]%n", sb.toString());
        } else {
            if (mMbox.getAllTagNames().size() == 0) return;
            String hdrFormat = "%10.10s  %10.10s  %10.10s  %s%n";
            stdout.format(hdrFormat, "Id", "Unread", "Color", "Name");
            stdout.format(hdrFormat, "----------", "----------", "----------", "----------");
            for (String tagName: mMbox.getAllTagNames()) {
                ZTag tag = mMbox.getTagByName(tagName);
                stdout.format("%10.10s  %10d  %10.10s  %s%n",
                        tag.getId(), tag.getUnreadCount(), tag.getColor().name(), tag.getName());
            }
        }
    }

    private void doDumpFolder(ZFolder folder, boolean recurse) {
        String path;
        if (folder instanceof ZSearchFolder) {
            path = String.format("%s (%s)", folder.getPath(), ((ZSearchFolder)folder).getQuery());
        } else if (folder instanceof ZMountpoint) {
            ZMountpoint mp = (ZMountpoint) folder;
            path = String.format("%s (%s:%s)", folder.getPath(), mp.getOwnerDisplayName(), mp.getRemoteId());
        } else if (folder.getRemoteURL() != null) {
            path = String.format("%s (%s)", folder.getPath(), folder.getRemoteURL());
        } else {
            path = folder.getPath();
        }

        stdout.format("%10.10s  %4.4s  %10d  %10d  %s%n",
                folder.getId(), folder.getDefaultView().name(), folder.getUnreadCount(), folder.getMessageCount(), path);
        if (recurse) {
            for (ZFolder child : folder.getSubFolders()) {
                doDumpFolder(child, recurse);
            }
        }
    }

    private void doGetAllFolders() throws ServiceException {
        if (verboseOpt()) {
            stdout.println(mMbox.getUserRoot().dump());
        } else {
            String hdrFormat = "%10.10s  %4.4s  %10.10s  %10.10s  %s%n";
            stdout.format(hdrFormat, "Id", "View", "Unread", "Msg Count", "Path");
            stdout.format(hdrFormat, "----------", "----", "----------", "----------",  "----------");
            doDumpFolder(mMbox.getUserRoot(), true);
        }
    }

    private void doGetFolder(String[] args) throws ServiceException {
        ZFolder f = lookupFolder(args[0]);
        stdout.println(f.dump());
        /*
        if (verboseOpt()) {

        } else {
            stdout
        }
        */
    }

    private void doGetFolderRequest(String[] args) throws ServiceException {
        ZFolder f = mMbox.getFolderRequestById(args[0]);
        stdout.println(f);
    }

    private void dumpIdentities(List<ZIdentity> identities) {
        if (verboseOpt()) {
            stdout.println("[");
            boolean first = true;
            for (ZIdentity identity: identities) {
                if (first) first = false;else stdout.println(",");
                stdout.println(identity.dump());
            }
            stdout.println("]");
        } else {
            if (identities.size() == 0) return;
            for (ZIdentity identity: identities) {
                stdout.println(identity.getName());
            }
        }
    }

    private void doGetIdentities() throws ServiceException {
        dumpIdentities(mMbox.getIdentities());
    }

    private void dumpSignatures(List<ZSignature> signatures) {
        if (verboseOpt()) {
            stdout.println("[");
            boolean first = true;
            for (ZSignature sig : signatures) {
                if (first) first = false;else stdout.println(",");
                stdout.println(sig.dump());
            }
            stdout.println("]");
        } else {
            if (signatures.size() == 0) return;
            for (ZSignature sig : signatures) {
                stdout.println(sig.getName());
            }
        }
    }

    private void doGetSignatures() throws ServiceException {
        dumpSignatures(mMbox.getSignatures());
    }


    private void dumpContacts(List<ZContact> contacts) throws ServiceException {
        if (verboseOpt()) {
            stdout.println("[");
            boolean first = true;
            for (ZContact cn: contacts) {
                if (first) first = false;else stdout.println(",");
                stdout.println(cn.dump());
            }
            stdout.println("]");
        } else {
            if (contacts.size() == 0) return;
            for (ZContact cn: contacts) {
                dumpContact(cn);
            }
        }
    }

    private void dumpAutoCompleteMatches(List<ZAutoCompleteMatch> matches) {
        if (matches.isEmpty()) {
            stdout.println("no matches");
            return;
        }
        int idLen = 2;
        int fidLen = 3;
        int displayLen = 7;
        for (ZAutoCompleteMatch match : matches) {
            String id = match.getId();
            if (id != null && id.length() > idLen)
                idLen = id.length();
            String fid = match.getFolderId();
            if (fid != null && fid.length() > fidLen)
                fidLen = fid.length();
            String display = match.getDisplayName();
            if (display != null && display.length() > displayLen)
                displayLen = display.length();
        }
        String format = "%7s %7s";
        if (verboseOpt()) {
            format += String.format(" %%%d.%ds", idLen, idLen);
            format += String.format(" %%%d.%ds", fidLen, fidLen);
            format += String.format(" %%%d.%ds", displayLen, displayLen);
        }
        format += " %s %n";
        if (verboseOpt()) {
            stdout.format(format, "Ranking", "type", "id", "fid", "display", "email");
            stdout.println("-------------------------------------------------------------------");
        } else {
            stdout.format(format, "Ranking", "type", "email");
            stdout.println("-------------------------------------------------------------------");
        }
        for (ZAutoCompleteMatch match : matches) {
            if (verboseOpt()) {
                String folderId = match.getFolderId();
                String id = match.getId();
                String display = match.getDisplayName();
                if (folderId == null) folderId = "";
                if (id == null) id = "";
                if (display == null) display = "";
                stdout.format(format, match.getRanking(), match.getType(), id, folderId, display, match.getEmail());
            } else {
                stdout.format(format, match.getRanking(), match.getType(), match.getEmail());
            }
        }
    }

    private void doGetAllContacts(String[] args) throws ServiceException {
        dumpContacts(mMbox.getAllContacts(lookupFolderId(folderOpt()), null, true, getList(args, 0)));
    }

    private void doGetContacts(String[] args) throws ServiceException {
        dumpContacts(mMbox.getContacts(id(args[0]), null, true, getList(args, 1)));
    }

    private void doAutoComplete(String[] args) throws ServiceException {
        dumpAutoCompleteMatches(mMbox.autoComplete(args[0], 20));
    }

    private void doAutoCompleteGal(String[] args) throws ServiceException {
        ZSearchGalResult result = mMbox.autoCompleteGal(args[0], GalEntryType.account, 20);
        dumpContacts(result.getContacts());
    }

    private void dumpConversation(ZConversation conv) throws ServiceException {
        int first = 1;
        int last = first + conv.getMessageCount();
        int width = colWidth(last);

        mIndexToId.clear();

        stdout.format("%nSubject: %s%n", conv.getSubject());
        stdout.format("Id: %s%n", conv.getId());

        if (conv.hasTags()) stdout.format("Tags: %s%n", lookupTagNames(conv.getTagIds()));
        if (conv.hasFlags()) stdout.format("Flags: %s%n", ZConversation.Flag.toNameList(conv.getFlags()));
        stdout.format("Num-Messages: %d%n%n", conv.getMessageCount());

        if (conv.getMessageCount() == 0) return;

        int id_len = 4;
        for (ZMessageSummary ms : conv.getMessageSummaries()) {
            id_len = Math.max(id_len, ms.getId().length());
        }

        String headerFormat = String.format("%%%d.%ds  %%%d.%ds  %%-15.15s  %%-50.50s  %%s%%n", width, width, id_len, id_len);
        String itemFormat   = String.format("%%%d.%ds. %%%d.%ds  %%-15.15s  %%-50.50s  %%tD %%<tR%%n", width, width, id_len, id_len);
        stdout.format(headerFormat, "","Id", "Sender", "Fragment", "Date");
        stdout.format(headerFormat, "", "----------------------------------------------------------------------------------------------------", "---------------", "--------------------------------------------------", "--------------");
        int i = first;
        for (ZMessageSummary ms : conv.getMessageSummaries()) {
            stdout.format(itemFormat,
                    i, ms.getId(), ms.getSender().getDisplay(), ms.getFragment(), ms.getDate());
            mIndexToId.put(i++, ms.getId());
        }
        stdout.println();
    }

    private void doGetConversation(String[] args) throws ServiceException {
        ZConversation conv = mMbox.getConversation(id(args[0]), Fetch.none);
        if (verboseOpt()) {
            stdout.println(conv.dump());
        } else {
            dumpConversation(conv);
        }
    }

    private static int addEmail(StringBuilder sb, String email, int line) {
        if (sb.length() > 0) { sb.append(','); line++; }
        if (line > 76) { sb.append("\n"); line = 1; }
        if (sb.length() > 0) { sb.append(' '); line++; }
        if (line > 20 && (line + email.length() > 76)) {
            sb.append("\n ");
            line = 1;
        }
        sb.append(email);
        line += email.length();
        return line;
    }

    public static String formatEmail(ZEmailAddress e) {
        String p = e.getPersonal();
        String a = e.getAddress();
        if (a == null) a = "";
        if (p == null)
            return String.format("<%s>", a);
        else
            return String.format("%s <%s>", p, a);
    }

    public static String formatEmail(List<ZEmailAddress> list, String type, int used) {
        if (list == null || list.size() == 0) return "";

        StringBuilder sb = new StringBuilder();

        for (ZEmailAddress e: list) {
            if (e.getType().equalsIgnoreCase(type)) {
                String fe = formatEmail(e);
                used = addEmail(sb, fe, used);
            }
        }
        return sb.toString();
    }

    private void doHeader(List<ZEmailAddress> list, String hdrName, String addrType) {
        String val = formatEmail(list, addrType, hdrName.length()+2);
        if (val == null || val.length() == 0) return;
        stdout.format("%s: %s%n", hdrName, val);
    }

    private void dumpMessage(ZMessage msg) throws ServiceException {
        stdout.format("Id: %s%n", msg.getId());
        stdout.format("Conversation-Id: %s%n", msg.getConversationId());
        ZFolder f =  mMbox.getFolderById(msg.getFolderId());
        stdout.format("Folder: %s%n", f == null ? msg.getFolderId() : f.getPath());
        stdout.format("Subject: %s%n", msg.getSubject());
        doHeader(msg.getEmailAddresses(), "From", ZEmailAddress.EMAIL_TYPE_FROM);
        doHeader(msg.getEmailAddresses(), "To", ZEmailAddress.EMAIL_TYPE_TO);
        doHeader(msg.getEmailAddresses(), "Cc", ZEmailAddress.EMAIL_TYPE_CC);
        stdout.format("Date: %s\n", DateUtil.toRFC822Date(new Date(msg.getReceivedDate())));
        if (msg.hasTags()) stdout.format("Tags: %s%n", lookupTagNames(msg.getTagIds()));
        if (msg.hasFlags()) stdout.format("Flags: %s%n", ZMessage.Flag.toNameList(msg.getFlags()));
        stdout.format("Size: %s%n", formatSize(msg.getSize()));
        stdout.println();
        if (dumpBody(msg.getMimeStructure()))
            stdout.println();
    }

    private void doGetMessage(String[] args) throws ServiceException {
        ZGetMessageParams params = new ZGetMessageParams();
        params.setMarkRead(true);
        params.setId(id(args[0]));
        ZMessage msg = mMbox.getMessage(params);
        if (verboseOpt()) {
            stdout.println(msg.dump());
        } else {
            dumpMessage(msg);
        }
    }

    private boolean dumpBody(ZMimePart mp) {
        if (mp == null) return false;

        if (mp.isBody()) {
            stdout.println(mp.getContent());
            return true;
        } else {
            for (ZMimePart child : mp.getChildren()) {
                if (dumpBody(child)) return true;
            }
        }
        return false;
    }

    private void doModifyContact(String[] args) throws ServiceException {
        String id = mMbox.modifyContact(id(args[0]),  mCommandLine.hasOption('r'), getContactMap(args, 1, !ignoreOpt())).getId();
        stdout.println(id);
    }

    private void dumpContact(ZContact contact) throws ServiceException {
        stdout.format("Id: %s%n", contact.getId());
        ZFolder f =  mMbox.getFolderById(contact.getFolderId());
        stdout.format("Folder: %s%n", f == null ? contact.getFolderId() : f.getPath());
        stdout.format("Date: %tD %<tR%n", contact.getMetaDataChangedDate());
        if (contact.hasTags()) stdout.format("Tags: %s%n", lookupTagNames(contact.getTagIds()));
        if (contact.hasFlags()) stdout.format("Flags: %s%n", ZContact.Flag.toNameList(contact.getFlags()));
        stdout.format("Revision: %s%n", contact.getRevision());
        stdout.format("Attrs:%n");
        Map<String, String> attrs = contact.getAttrs();
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            stdout.format("  %s: %s%n", entry.getKey(), entry.getValue());
        }
        if (contact.isGroup()) {
            stdout.format("GroupMembers: %s%n", contact.getMembers().keySet());
        }
        stdout.println();
    }

    private Map<String, String> getContactMap(String[] args, int offset, boolean validate) throws ServiceException {
        Map<String, String> result = getMap(args, offset);
        if (validate) {
            for (String name : result.keySet()) {
                ContactConstants.Attr.fromString(name);
            }
        }
        return result;
    }

    private Map<String, Object> getMultiMap(String[] args, int offset) throws ServiceException {
        try {
            return StringUtil.keyValueArrayToMultiMap(args, offset);
        } catch (IllegalArgumentException iae) {
            throw ZClientException.CLIENT_ERROR("not enough arguments", null);
        }
    }

    private Map<String, String> getMap(String[] args, int offset) throws ServiceException {
        Map<String, String> attrs = new HashMap<String, String>();
        for (int i = offset; i < args.length; i+=2) {
            String n = args[i];
            if (i+1 >= args.length)
                throw ZClientException.CLIENT_ERROR("not enough arguments", null);
            String v = args[i+1];
            attrs.put(n, v);
        }
        return attrs;
    }

    private List<String> getList(String[] args, int offset) {
        List<String> attrs = new ArrayList<String>();
        for (int i = offset; i < args.length; i++) {
            attrs.add(args[i]);
        }
        return attrs;
    }

    public void interactive(BufferedReader in) throws IOException {
        while (true) {
            stdout.print(mPrompt);
            stdout.flush();
            String line = StringUtil.readLine(in);
            if (line == null || line.length() == -1)
                break;
            if (mGlobalVerbose) {
                stdout.println(line);
            }
            String args[] = StringUtil.parseLine(line);
            if (args.length == 0)
                continue;
            try {
                switch(execute(args)) {
                case EXIT:
                    return;
                    //break;
                }
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                stderr.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" +
                        (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
                if (mGlobalVerbose) e.printStackTrace(stderr);
            }
        }
    }

    public static void main(String args[]) throws IOException, ServiceException {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmmailbox", BuildInfo.VERSION);

        ZMailboxUtil pu = new ZMailboxUtil();
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("a", "admin", true, "admin account name to auth as");
        options.addOption("z", "zadmin", false, "use zimbra admin name/password from localconfig for admin/password");
        options.addOption("h", "help", false, "display usage");
        options.addOption("f", "file", true, "use file as input stream");
        options.addOption("u", "url", true, "http[s]://host[:port] of server to connect to");
        options.addOption("r", "protocol", true, "protocol to use for request/response [soap11, soap12, json]");
        options.addOption("m", "mailbox", true, "mailbox to open");
        options.addOption(null, "auth", true, "account name to auth as; defaults to -m unless -A is used");
        options.addOption("A", "admin-priv", false, "execute requests with admin privileges");
        options.addOption("p", "password", true, "auth password");
        options.addOption("P", "passfile", true, "filename with password in it");
        options.addOption("t", "timeout", true, "timeout (in seconds)");
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption("d", "debug", false, "debug mode");
        options.addOption(SoapCLI.OPT_AUTHTOKEN);
        options.addOption(SoapCLI.OPT_AUTHTOKENFILE);

        CommandLine cl = null;
        boolean err = false;

        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            stderr.println("error: " + pe.getMessage());
            err = true;
        }

        if (err || cl.hasOption('h')) {
            pu.usage();
        }

        try {
            boolean isAdmin = false;
            pu.setVerbose(cl.hasOption('v'));
            if (cl.hasOption('a')) {
                pu.setAdminAccountName(cl.getOptionValue('a'));
                pu.setUrl(DEFAULT_ADMIN_URL, true);
                isAdmin = true;
            }
            if (cl.hasOption('z')) {
                pu.setAdminAccountName(LC.zimbra_ldap_user.value());
                pu.setPassword(LC.zimbra_ldap_password.value());
                pu.setUrl(DEFAULT_ADMIN_URL, true);
                isAdmin = true;
            }

            if (cl.hasOption(SoapCLI.O_AUTHTOKEN) && cl.hasOption(SoapCLI.O_AUTHTOKENFILE))
                pu.usage();
            if (cl.hasOption(SoapCLI.O_AUTHTOKEN)) {
                pu.setAdminAuthToken(ZAuthToken.fromJSONString(cl.getOptionValue(SoapCLI.O_AUTHTOKEN)));
                pu.setUrl(DEFAULT_ADMIN_URL, true);
                isAdmin = true;
            }
            if (cl.hasOption(SoapCLI.O_AUTHTOKENFILE)) {
                String authToken = StringUtil.readSingleLineFromFile(cl.getOptionValue(SoapCLI.O_AUTHTOKENFILE));
                pu.setAdminAuthToken(ZAuthToken.fromJSONString(authToken));
                pu.setUrl(DEFAULT_ADMIN_URL, true);
                isAdmin = true;
            }

            String authAccount, targetAccount;
            if (cl.hasOption('m')) {
                if (!cl.hasOption('p') && !cl.hasOption('P') && !cl.hasOption('y') && !cl.hasOption('Y') && !cl.hasOption('z')) {
                    throw ZClientException.CLIENT_ERROR("-m requires one of the -p/-P/-y/-Y/-z options", null);
                }
                targetAccount = cl.getOptionValue('m');
            } else {
                targetAccount = null;
            }
            if ((cl.hasOption("A") || cl.hasOption("auth")) && !cl.hasOption('m')) {
                throw ZClientException.CLIENT_ERROR("-A/--auth requires -m", null);
            }
            if (cl.hasOption("A")) {
                if (!isAdmin) {
                    throw ZClientException.CLIENT_ERROR("-A requires admin auth", null);
                }
                if (cl.hasOption("auth")) {
                    throw ZClientException.CLIENT_ERROR("-A cannot be combined with --auth", null);
                }
                authAccount = null;
            } else if (cl.hasOption("auth")) {
                authAccount = cl.getOptionValue("auth");
            } else {
                // default case
                authAccount = targetAccount;
            }
            if (!StringUtil.isNullOrEmpty(authAccount)) pu.setAuthAccountName(authAccount);
            if (!StringUtil.isNullOrEmpty(targetAccount)) pu.setTargetAccountName(targetAccount);

            if (cl.hasOption('u')) pu.setUrl(cl.getOptionValue('u'), isAdmin);
            if (cl.hasOption('p')) pu.setPassword(cl.getOptionValue('p'));
            if (cl.hasOption('P')) {
                pu.setPassword(StringUtil.readSingleLineFromFile(cl.getOptionValue('P')));
            }
            if (cl.hasOption('d')) pu.setDebug(true);

            if (cl.hasOption('t')) pu.setTimeout(cl.getOptionValue('t'));

            args = cl.getArgs();

            pu.setInteractive(args.length < 1);

            pu.initMailbox();
            if (args.length < 1) {
                InputStream is = null;
                if (cl.hasOption('f')) {
                    is = new FileInputStream(cl.getOptionValue('f'));
                } else {
                    if (LC.command_line_editing_enabled.booleanValue()) {
                        try {
                            CliUtil.enableCommandLineEditing(LC.zimbra_home.value() + "/.zmmailbox_history");
                        } catch (IOException e) {
                            System.err.println("Command line editing will be disabled: " + e);
                            if (pu.mGlobalVerbose) {
                                e.printStackTrace(System.err);
                            }
                        }
                    }
                    is = System.in;  // This has to happen last because JLine modifies System.in.
                }
                pu.interactive(new BufferedReader(new InputStreamReader(is, "UTF-8")));
            } else {
                pu.execute(args);
            }
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            stderr.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" +
                    (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
            if (pu.mGlobalVerbose) e.printStackTrace(stderr);
            System.exit(2);
        }
    }

    private void doHelp(String[] args) {
        Category cat = null;
        if (args != null && args.length >= 1) {
            String s = args[0].toUpperCase();
            try {
                cat = Category.valueOf(s);
            } catch (IllegalArgumentException e) {
                for (Category c : Category.values()) {
                    if (c.name().startsWith(s)) {
                        cat = c;
                        break;
                    }
                }
            }
        }

        if (args == null || args.length == 0 || cat == null) {
            stdout.println(" zmmailbox is used for mailbox management. Try:");
            stdout.println("");
            for (Category c: Category.values()) {
                stdout.printf("     zmmailbox help %-15s %s\n", c.name().toLowerCase(), c.getDescription());
            }

        }

        if (cat != null) {
            stdout.println("");
            for (Command c : Command.values()) {
                if (!c.hasHelp()) continue;
                if (cat == Category.COMMANDS || cat == c.getCategory()) {
                    stdout.print(c.getFullUsage());
                    stdout.println();
                }
            }
            if (cat.getCategoryHelp() != null)
            stdout.println(cat.getCategoryHelp());
        }
        stdout.println();
    }

    private long mSendStart;

    @Override
    public void receiveSoapMessage(Element envelope) {
        long end = System.currentTimeMillis();
        stdout.printf("======== SOAP RECEIVE =========\n");
        stdout.println(envelope.prettyPrint());
        stdout.printf("=============================== (%d msecs)\n", end-mSendStart);

    }

    @Override
    public void sendSoapMessage(Element envelope) {
        mSendStart = System.currentTimeMillis();
        stdout.println("========== SOAP SEND ==========");
        stdout.println(envelope.prettyPrint());
        stdout.println("===============================");
    }

    public static String encodeURL(String unencoded) {
        // Look for a query string.  It's supposed to be URL encoded already, so encode only what comes before.
        String queryString = null;
        int queryStringStart = unencoded.indexOf('?');
        if (queryStringStart != -1) {
            queryString = unencoded.substring(queryStringStart);
            unencoded = unencoded.substring(0, queryStringStart);
        }
        StringBuilder encoded = new StringBuilder();
        String parts[] = unencoded.split("/");
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i] = HttpUtil.encodePath(parts[i]);
            }
            encoded.append(StringUtil.join("/", parts));
        } else {
            encoded.append(unencoded);
        }
        if (queryString != null) {
            String encodedQuery;
            try {
                URI uri = new URI(null, null, null, queryString.substring(1), null);
                encodedQuery = uri.toString();
            } catch (URISyntaxException e) {
                encodedQuery = queryString;
            }
            encoded.append(encodedQuery);
        }
        return encoded.toString();
    }

    private void doGetRestURL(String args[]) throws ServiceException {
        OutputStream os = null;
        String outputFile = outputFileOpt();
        boolean hasOutputFile = outputFile != null;

        try {
            os = hasOutputFile ? new FileOutputStream(outputFile) : System.out;
            mMbox.getRESTResource(encodeURL(args[0]), os, hasOutputFile, startTimeOpt(), endTimeOpt(), mTimeout, urlOpt(false));
        } catch (IOException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            if (hasOutputFile && os != null) try { os.close(); } catch (IOException e) {}
        }
    }

    private void doPostRestURL(String args[]) throws ServiceException {
        try {
            File file = new File(args[1]);
            mMbox.postRESTResource(encodeURL(args[0]), new FileInputStream(file), true, file.length(),
                    contentTypeOpt(), ignoreAndContinueOnErrorOpt(), preserveAlarmsOpt(), mTimeout, urlOpt(false));
        } catch (FileNotFoundException e) {
            throw ZClientException.CLIENT_ERROR("file not found: "+args[1], e);
        }
    }
}

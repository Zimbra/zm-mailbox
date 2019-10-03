/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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

package com.zimbra.cs.account;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.AccountLogger;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.Version;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CountAccountResult;
import com.zimbra.cs.account.Provisioning.MailMode;
import com.zimbra.cs.account.Provisioning.PublishedShareInfoVisitor;
import com.zimbra.cs.account.Provisioning.RightsDoc;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.Provisioning.SetPasswordResult;
import com.zimbra.cs.account.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttrRight;
import com.zimbra.cs.account.accesscontrol.ComboRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Help;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.Right.RightType;
import com.zimbra.cs.account.accesscontrol.RightClass;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.IndexStatsInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.MailboxInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.MemcachedClientConfig;
import com.zimbra.cs.account.soap.SoapProvisioning.QuotaUsage;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexBy;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.fb.FbCli;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.cs.zclient.ZMailboxUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.type.HABGroupMember;
import com.zimbra.soap.admin.message.LockoutMailboxRequest;
import com.zimbra.soap.admin.message.UnregisterMailboxMoveOutRequest;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.MailboxMoveSpec;
import com.zimbra.soap.type.AccountNameSelector;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.TargetBy;

import net.spy.memcached.DefaultHashAlgorithm;

/**
 * @author schemers
 */
public class ProvUtil implements HttpDebugListener {

    private static final String ERR_VIA_SOAP_ONLY = "can only be used with SOAP";
    private static final String ERR_VIA_LDAP_ONLY = "can only be used with  \"zmprov -l/--ldap\"";
    private static final String ERR_INVALID_ARG_EV = "arg -e is invalid unless -v is also specified";

    private static final PrintStream console = System.out;
    private static final PrintStream errConsole = System.err;

    enum SoapDebugLevel {
        none, // no SOAP debug
        normal, // SOAP request and response payload
        high; // SOAP payload and http transport header
    }

    private boolean batchMode = false;
    private boolean interactiveMode = false;
    private boolean verboseMode = false;
    private SoapDebugLevel debugLevel = SoapDebugLevel.none;
    private boolean useLdap = LC.zimbra_zmprov_default_to_ldap.booleanValue();
    private boolean useLdapMaster = false;
    private String account = null;
    private String password = null;
    private ZAuthToken authToken = null;
    private String serverHostname = LC.zimbra_zmprov_default_soap_server.value();
    private int serverPort = LC.zimbra_admin_service_port.intValue();
    private Command command;
    private Map<String, Command> commandIndex;
    private Provisioning prov;
    private BufferedReader cliReader;
    private boolean outputBinaryToFile;
    private boolean allowMultiValuedAttrReplacement;
    private long sendStart;
    private boolean forceDisplayAttrValue;

    private boolean errorOccursDuringInteraction = false; // bug 58554

    public void setDebug(SoapDebugLevel value) {
        debugLevel = value;
    }

    public void setVerbose(boolean value) {
        verboseMode = value;
    }

    public void setUseLdap(boolean ldap, boolean master) {
        useLdap = ldap;
        useLdapMaster = master;
    }

    public void setAccount(String value) {
        account = value;
        useLdap = false;
    }

    public void setPassword(String value) {
        password = value;
        useLdap = false;
    }

    public void setAuthToken(ZAuthToken value) {
        authToken = value;
        useLdap = false;
    }

    private void setOutputBinaryToFile(boolean value) {
        outputBinaryToFile = value;
    }

    private void setBatchMode(boolean value) {
        batchMode = value;
    }

    private void setAllowMultiValuedAttrReplacement(boolean value) {
        allowMultiValuedAttrReplacement = value;
    }

    private boolean outputBinaryToFile() {
        return outputBinaryToFile;
    }

    private void setForceDisplayAttrValue(boolean value) {
        this.forceDisplayAttrValue = value;
    }

    public void setServer(String value) {
        int i = value.indexOf(":");
        if (i == -1) {
            serverHostname = value;
        } else {
            serverHostname = value.substring(0, i);
            serverPort = Integer.parseInt(value.substring(i + 1));
        }
        useLdap = false;
    }

    public boolean useLdap() {
        return useLdap;
    }

    private void deprecated() {
        console.println("This command has been deprecated.");
        System.exit(1);
    }

    private void usage() {
        usage(null);
    }

    private void usage(Command.Via violatedVia) {
        boolean givenHelp = false;
        if (command != null) {
            if (violatedVia == null) {
                console.printf("usage:  %s(%s) %s\n", command.getName(), command.getAlias(), command.getHelp());
                givenHelp = true;
                CommandHelp extraHelp = command.getExtraHelp();
                if (extraHelp != null) {
                    extraHelp.printHelp();
                }
            } else {
                if (violatedVia == Command.Via.ldap) {
                    console.printf("%s %s\n", command.getName(), ERR_VIA_LDAP_ONLY);
                } else {
                    console.printf("%s %s\n", command.getName(), ERR_VIA_SOAP_ONLY);
                }
            }
        }
        if (interactiveMode) {
            return;
        }
        if (givenHelp) {
            console.println("For general help, type : zmprov --help");
            System.exit(1);
        }
        console.println("");
        console.println("zmprov [args] [cmd] [cmd-args ...]");
        console.println("");
        console.println("  -h/--help                             display usage");
        console.println("  -f/--file                             use file as input stream");
        console.println("  -s/--server   {host}[:{port}]         server hostname and optional port");
        console.println("  -l/--ldap                             provision via LDAP instead of SOAP");
        console.println("  -L/--logpropertyfile                  log4j property file, valid only with -l");
        console.println("  -a/--account  {name}                  account name to auth as");
        console.println("  -p/--password {pass}                  password for account");
        console.println("  -P/--passfile {file}                  read password from file");
        console.println("  -z/--zadmin                           use zimbra admin name/password from localconfig for admin/password");
        console.println("  -y/--authtoken {authtoken}            " + SoapCLI.OPT_AUTHTOKEN.getDescription());
        console.println("  -Y/--authtokenfile {authtoken file}   " + SoapCLI.OPT_AUTHTOKENFILE.getDescription());
        console.println("  -v/--verbose                          verbose mode (dumps full exception stack trace)");
        console.println("  -d/--debug                            debug mode (dumps SOAP messages)");
        console.println("  -m/--master                           use LDAP master (only valid with -l)");
        console.println("  -r/--replace                          allow replacement of safe-guarded multi-valued attributes configured in localconfig key \"zmprov_safeguarded_attrs\"");
        console.println("");
        doHelp(null);
        System.exit(1);
    }

    public static enum Category {
        ACCOUNT("help on account-related commands"), CALENDAR("help on calendar resource-related commands"), COMMANDS(
                "help on all commands"), CONFIG("help on config-related commands"), COS("help on COS-related commands"), DOMAIN(
                "help on domain-related commands"), FREEBUSY("help on free/busy-related commands"), LIST(
                "help on distribution list-related commands"), LOG("help on logging commands"), MISC(
                "help on misc commands"), MAILBOX("help on mailbox-related commands"), REVERSEPROXY(
                "help on reverse proxy related commands"), RIGHT("help on right-related commands"), SEARCH(
                "help on search-related commands"), SERVER("help on server-related commands"), ALWAYSONCLUSTER(
                "help on alwaysOnCluster-related commands"), UCSERVICE("help on ucservice-related commands"), SHARE(
                "help on share related commands"), HAB("help on HAB commands");

        private final String description;

        public String getDescription() {
            return description;
        }

        Category(String desc) {
            description = desc;
        }

        static void help(Category cat) {
            switch (cat) {
            case CALENDAR:
                helpCALENDAR();
                break;
            case RIGHT:
                helpRIGHT();
                break;
            case LOG:
                helpLOG();
                break;
            }
        }

        static void helpCALENDAR() {
            console.println("");
            StringBuilder sb = new StringBuilder();
            EntrySearchFilter.Operator vals[] = EntrySearchFilter.Operator.values();
            for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(vals[i].toString());
            }
            console.println("    op = " + sb.toString());
        }

        static void helpRIGHT() {
            helpRIGHTCommon(true);
            helpRIGHTRights(false, true);
        }

        static void helpRIGHTCommand(boolean printRights, boolean secretPossible, boolean modifierPossible) {
            helpRIGHTCommon(secretPossible);
            helpRIGHTRights(false, modifierPossible);
        }

        static void helpRIGHTRights(boolean printRights, boolean modifierPossible) {
            // rights
            console.println();
            if (modifierPossible) {
                console.println("    {right}: can have the following prefixes:");
                for (RightModifier rm : RightModifier.values()) {
                    console.println("            " + rm.getModifier() + " : " + rm.getDescription());
                }
                console.println();
            }

            if (printRights) {
                try {
                    Map<String, AdminRight> allAdminRights = RightManager.getInstance().getAllAdminRights();
                    // print non-combo rights first
                    for (com.zimbra.cs.account.accesscontrol.Right r : allAdminRights.values()) {
                        if (RightType.combo != r.getRightType()) {
                            console.println("        " + r.getName() + " (" + r.getRightType().toString() + ")");
                        }
                    }
                    // then combo rights
                    for (com.zimbra.cs.account.accesscontrol.Right r : allAdminRights.values()) {
                        if (RightType.combo == r.getRightType()) {
                            console.println("        " + r.getName() + " (" + r.getRightType().toString() + ")");
                        }
                    }
                } catch (ServiceException e) {
                    console.println("cannot get RightManager instance: " + e.getMessage());
                }
            } else {
                console.println("         for complete list of rights, do \"zmprov gar -c ALL\"");
            }

            console.println();
        }

        static void helpRIGHTCommon(boolean secretPossible) {
            // target types
            console.println();
            StringBuilder tt = new StringBuilder();
            StringBuilder ttNeedsTargetIdentity = new StringBuilder();
            StringBuilder ttNoTargetId = new StringBuilder();
            TargetType[] tts = TargetType.values();
            for (int i = 0; i < tts.length; i++) {
                if (i > 0) {
                    tt.append(", ");
                }
                tt.append(tts[i].getCode());
                if (tts[i].needsTargetIdentity()) {
                    ttNeedsTargetIdentity.append(tts[i].getCode() + " ");
                } else {
                    ttNoTargetId.append(tts[i].getCode() + " ");
                }
            }
            console.println("    {target-type} = " + tt.toString());
            console.println();
            console.println("    {target-id|target-name} is required if target-type is: " + ttNeedsTargetIdentity);
            console.println("    {target-id|target-name} should not be specified if target-type is: " + ttNoTargetId);

            // grantee types
            console.println();
            StringBuilder gt = new StringBuilder();
            StringBuilder gtNeedsGranteeIdentity = new StringBuilder();
            StringBuilder gtNoGranteeId = new StringBuilder();
            StringBuilder gtNeedsSecret = new StringBuilder();
            StringBuilder gtNoSecret = new StringBuilder();
            GranteeType[] gts = GranteeType.values();
            for (int i = 0; i < gts.length; i++) {
                if (i > 0) {
                    gt.append(", ");
                }
                gt.append(gts[i].getCode());
                if (gts[i].needsGranteeIdentity()) {
                    gtNeedsGranteeIdentity.append(gts[i].getCode() + " ");
                } else {
                    gtNoGranteeId.append(gts[i].getCode() + " ");
                }
                if (secretPossible) {
                    if (gts[i].allowSecret()) {
                        gtNeedsSecret.append(gts[i].getCode() + " ");
                    } else {
                        gtNoSecret.append(gts[i].getCode() + " ");
                    }
                }
            }
            console.println("    {grantee-type} = " + gt.toString());
            console.println();
            console.println("    {grantee-id|grantee-name} is required if grantee-type is one of: "
                    + gtNeedsGranteeIdentity);
            console.println("    {grantee-id|grantee-name} should not be specified if grantee-type is one of: "
                    + gtNoGranteeId);
            if (secretPossible) {
                console.println();
                console.println("    {secret} is required if grantee-type is one of: " + gtNeedsSecret);
                console.println("    {secret} should not be specified if grantee-type is one of: " + gtNoSecret);
            }
        }

        static void helpLOG() {
            console.println("    Log categories:");
            int maxNameLength = 0;
            for (String name : ZimbraLog.CATEGORY_DESCRIPTIONS.keySet()) {
                if (name.length() > maxNameLength) {
                    maxNameLength = name.length();
                }
            }
            for (String name : ZimbraLog.CATEGORY_DESCRIPTIONS.keySet()) {
                console.print("        " + name);
                for (int i = 0; i < (maxNameLength - name.length()); i++) {
                    console.print(" ");
                }
                console.format(" - %s\n", ZimbraLog.CATEGORY_DESCRIPTIONS.get(name));
            }
        }
    }

    // TODO: refactor to own class
    interface CommandHelp {
        public void printHelp();
    }

    static class RightCommandHelp implements CommandHelp {
        boolean printRights;
        boolean secretPossible;
        boolean modifierPossible;

        RightCommandHelp(boolean printRights, boolean secretPossible, boolean modifierPossible) {
            this.printRights = printRights;
            this.secretPossible = secretPossible;
            this.modifierPossible = modifierPossible;
        }

        @Override
        public void printHelp() {
            Category.helpRIGHTCommand(printRights, secretPossible, modifierPossible);
        }
    }

    static class ReindexCommandHelp implements CommandHelp {
        @Override
        public void printHelp() {
            /*
             * copied from soap-admin.txt Not exactly match all types in MailboxIndex TODO: cleanup
             */
            console.println();
            console.println("Valid types:");
            console.println("    appointment");
            // console.println("    briefcase");
            // console.println("    chat");
            console.println("    contact");
            console.println("    conversation");
            console.println("    document");
            console.println("    message");
            console.println("    note");
            // console.println("    tag");
            console.println("    task");
            console.println();
        }
    }

    public enum Command {
        ADD_ACCOUNT_ALIAS("addAccountAlias", "aaa", "{name@domain|id} {alias@domain}", Category.ACCOUNT, 2, 2),
        ADD_ACCOUNT_LOGGER(
                "addAccountLogger", "aal",
                "[-s/--server hostname] {name@domain|id} {logging-category} {trace|debug|info|warn|error}",
                Category.LOG, 3, 5),
        ADD_DISTRIBUTION_LIST_ALIAS("addDistributionListAlias", "adla", "{list@domain|id} {alias@domain}",
                Category.LIST, 2, 2),
        ADD_DISTRIBUTION_LIST_MEMBER("addDistributionListMember", "adlm", "{list@domain|id} {member@domain}+",
                Category.LIST, 2, Integer.MAX_VALUE),
        AUTO_COMPLETE_GAL("autoCompleteGal", "acg", "{domain} {name}", Category.SEARCH, 2, 2),
        AUTO_PROV_CONTROL("autoProvControl", "apc", "{start|status|stop}", Category.COMMANDS, 1, 1),
        CHECK_PASSWORD_STRENGTH("checkPasswordStrength", "cps", "{name@domain|id} {password}", Category.ACCOUNT, 2, 2),
        CHECK_RIGHT("checkRight","ckr",
                "{target-type} [{target-id|target-name}] {grantee-id|grantee-name (note:can only check internal user)} {right}",
                Category.RIGHT, 3, 4, null, new RightCommandHelp(false, false, true)),
        COPY_COS("copyCos", "cpc", "{src-cos-name|id} {dest-cos-name}", Category.COS, 2, 2),
        COUNT_ACCOUNT("countAccount", "cta", "{domain|id}", Category.DOMAIN, 1, 1),
        COUNT_OBJECTS("countObjects", "cto", "{"
                + CountObjectsType.names("|") + "} [-d {domain|id}] [-u {UCService|id}]", Category.MISC, 1, 4),
        CREATE_ACCOUNT("createAccount", "ca",
                "{name@domain} {password} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 2, Integer.MAX_VALUE),
        CREATE_ALIAS_DOMAIN("createAliasDomain", "cad",
                "{alias-domain-name} {local-domain-name|id} [attr1 value1 [attr2 value2...]]", Category.DOMAIN, 2,
                Integer.MAX_VALUE),
        CREATE_ALWAYSONCLUSTER("createAlwaysOnCluster", "caoc",
                "{name} [attr1 value1 [attr2 value2...]]", Category.ALWAYSONCLUSTER, 1, Integer.MAX_VALUE),
        CREATE_BULK_ACCOUNTS(
                "createBulkAccounts", "cabulk", "{domain} {namemask} {number of accounts to create}", Category.MISC, 3,
                3),
        CREATE_CALENDAR_RESOURCE("createCalendarResource", "ccr",
                "{name@domain} {password} [attr1 value1 [attr2 value2...]]", Category.CALENDAR, 2, Integer.MAX_VALUE),
        CREATE_COS(
                "createCos", "cc", "{name} [attr1 value1 [attr2 value2...]]", Category.COS, 1, Integer.MAX_VALUE),
        CREATE_DATA_SOURCE(
                "createDataSource",
                "cds",
                "{name@domain} {ds-type} {ds-name} zimbraDataSourceEnabled {TRUE|FALSE} zimbraDataSourceFolderId {folder-id} [attr1 value1 [attr2 value2...]]",
                Category.ACCOUNT, 3, Integer.MAX_VALUE),
        CREATE_DISTRIBUTION_LIST("createDistributionList", "cdl",
                "{list@domain}", Category.LIST, 1, Integer.MAX_VALUE),
        CREATE_DYNAMIC_DISTRIBUTION_LIST(
                "createDynamicDistributionList", "cddl", "{list@domain}", Category.LIST, 1, Integer.MAX_VALUE),
        CREATE_DISTRIBUTION_LISTS_BULK(
                "createDistributionListsBulk", "cdlbulk"),
        CREATE_DOMAIN("createDomain", "cd",
                "{domain} [attr1 value1 [attr2 value2...]]", Category.DOMAIN, 1, Integer.MAX_VALUE),
        CREATE_SERVER(
                "createServer", "cs", "{name} [attr1 value1 [attr2 value2...]]", Category.SERVER, 1, Integer.MAX_VALUE),
        CREATE_UC_SERVICE(
                "createUCService", "cucs", "{name} [attr1 value1 [attr2 value2...]]", Category.UCSERVICE, 1,
                Integer.MAX_VALUE),
        CREATE_IDENTITY("createIdentity", "cid",
                "{name@domain} {identity-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 2,
                Integer.MAX_VALUE),
        CREATE_SIGNATURE("createSignature", "csig",
                "{name@domain} {signature-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 2,
                Integer.MAX_VALUE),
        CREATE_XMPP_COMPONENT("createXMPPComponent", "cxc",
                "{short-name} {domain}  {server} {classname} {category} {type} [attr value1 [attr2 value2...]]",
                Category.CONFIG, 6, Integer.MAX_VALUE),
        DELETE_ACCOUNT("deleteAccount", "da", "{name@domain|id}",
                Category.ACCOUNT, 1, 1),
        DELETE_ALWAYSONCLUSTER("deleteAlwaysOnCluster", "daoc", "{name|id}",
                Category.ALWAYSONCLUSTER, 1, 1),
        DELETE_CALENDAR_RESOURCE("deleteCalendarResource", "dcr",
                "{name@domain|id}", Category.CALENDAR, 1, 1),
        DELETE_COS("deleteCos", "dc", "{name|id}", Category.COS,
                1, 1),
        DELETE_DATA_SOURCE("deleteDataSource", "dds", "{name@domain|id} {ds-name|ds-id}",
                Category.ACCOUNT, 2, 2),
        DELETE_DISTRIBUTION_LIST("deleteDistributionList", "ddl", "{list@domain|id} [true|false]",
                Category.LIST, 1, 2),
        DELETE_DOMAIN("deleteDomain", "dd", "{domain|id}", Category.DOMAIN, 1, 1),
        DELETE_IDENTITY(
               "deleteIdentity", "did", "{name@domain|id} {identity-name}", Category.ACCOUNT, 2, 2),
        DELETE_SIGNATURE(
               "deleteSignature", "dsig", "{name@domain|id} {signature-name}", Category.ACCOUNT, 2, 2),
        DELETE_SERVER(
               "deleteServer", "ds", "{name|id}", Category.SERVER, 1, 1),
        DELETE_UC_SERVICE("deleteUCService", "ducs",
               "{name|id}", Category.UCSERVICE, 1, 1),
        DELETE_XMPP_COMPONENT("deleteXMPPComponent", "dxc",
               "{xmpp-component-name}", Category.CONFIG, 1, 1),
        DESCRIBE("describe", "desc",
               "[[-v] [-ni] [{entry-type}]] | [-a {attribute-name}]", Category.MISC, 0, Integer.MAX_VALUE, null, null,
               true),
        EXIT("exit", "quit", "", Category.MISC, 0, 0),
        FLUSH_CACHE("flushCache", "fc", "[-a] {"
               + CacheEntryType.names() + "|<extension-cache-type>} [name1|id1 [name2|id2...]]", Category.MISC, 1,
               Integer.MAX_VALUE),
        GENERATE_DOMAIN_PRE_AUTH("generateDomainPreAuth", "gdpa",
               "{domain|id} {name|id|foreignPrincipal} {by} {timestamp|0} {expires|0}", Category.MISC, 5, 6),
        GENERATE_DOMAIN_PRE_AUTH_KEY(
               "generateDomainPreAuthKey", "gdpak", "[-f] {domain|id}", Category.MISC, 1, 2),
        GET_ACCOUNT(
               "getAccount", "ga", "[-e] {name@domain|id} [attr1 [attr2...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_ALWAYSONCLUSTER(
               "getAlwaysOnCluster", "gaoc", "{name|id} [attr1 [attr2...]]", Category.ALWAYSONCLUSTER, 1,
               Integer.MAX_VALUE),
        GET_DATA_SOURCES("getDataSources", "gds", "{name@domain|id} [arg1 [arg2...]]",
               Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_IDENTITIES("getIdentities", "gid",
               "{name@domain|id} [arg1 [arg...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_SIGNATURES(
               "getSignatures", "gsig", "{name@domain|id} [arg1 [arg...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_ACCOUNT_MEMBERSHIP(
               "getAccountMembership", "gam", "{name@domain|id}", Category.ACCOUNT, 1, 2),
        GET_ALL_ACCOUNTS(
               "getAllAccounts", "gaa", "[-v] [-e] [-s server] [{domain}]", Category.ACCOUNT, 0, 5, Via.ldap),
        GET_ACCOUNT_LOGGERS(
               "getAccountLoggers", "gal", "[-s/--server hostname] {name@domain|id}", Category.LOG, 1, 3),
        GET_ALL_ACTIVE_SERVERS(
               "getAllActiveServers", "gaas", "[-v]", Category.SERVER, 0, 1),
        GET_ALL_ACCOUNT_LOGGERS(
               "getAllAccountLoggers", "gaal", "[-s/--server hostname]", Category.LOG, 0, 2),
        GET_ALL_ADMIN_ACCOUNTS(
               "getAllAdminAccounts", "gaaa", "[-v] [-e] [attr1 [attr2...]]", Category.ACCOUNT, 0, Integer.MAX_VALUE),
        GET_ALL_ALWAYSONCLUSTERS(
               "getAllAlwaysOnClusters", "gaaoc", "[-v]", Category.ALWAYSONCLUSTER, 0, 1),
        GET_ALL_CALENDAR_RESOURCES(
               "getAllCalendarResources", "gacr", "[-v] [-e] [-s server] [{domain}]", Category.CALENDAR, 0, 5),
        GET_ALL_CONFIG(
               "getAllConfig", "gacf", "[attr1 [attr2...]]", Category.CONFIG, 0, Integer.MAX_VALUE),
        GET_ALL_COS(
               "getAllCos", "gac", "[-v]", Category.COS, 0, 1),
        GET_ALL_DISTRIBUTION_LISTS("getAllDistributionLists",
               "gadl", "[-v] [{domain}]", Category.LIST, 0, 2),
        GET_ALL_DOMAINS("getAllDomains", "gad",
               "[-v] [-e] [attr1 [attr2...]]", Category.DOMAIN, 0, Integer.MAX_VALUE),
        GET_ALL_EFFECTIVE_RIGHTS(
               "getAllEffectiveRights", "gaer",
               "{grantee-type} {grantee-id|grantee-name} [expandSetAttrs] [expandGetAttrs]", Category.RIGHT, 2, 4),
        GET_ALL_FREEBUSY_PROVIDERS(
               "getAllFbp", "gafbp", "[-v]", Category.FREEBUSY, 0, 1),
        GET_ALL_RIGHTS("getAllRights", "gar",
               "[-v] [-t {target-type}] [-c " + RightClass.allValuesInString("|") + "]", Category.RIGHT, 0, 5),
        GET_ALL_SERVERS(
               "getAllServers", "gas", "[-v] [-e] [service]", Category.SERVER, 0, 3),
        GET_ALL_UC_SERVICES(
               "getAllUCServices", "gaucs", "[-v]", Category.UCSERVICE, 0, 3),
        GET_ALL_XMPP_COMPONENTS(
               "getAllXMPPComponents", "gaxcs", "", Category.CONFIG, 0, 0),
        GET_AUTH_TOKEN_INFO("getAuthTokenInfo",
               "gati", "{auth-token}", Category.MISC, 1, 1),
        GET_CALENDAR_RESOURCE("getCalendarResource", "gcr",
               "{name@domain|id} [attr1 [attr2...]]", Category.CALENDAR, 1, Integer.MAX_VALUE),
        GET_CONFIG(
               "getConfig", "gcf", "{name}", Category.CONFIG, 1, 1),
        GET_COS("getCos", "gc",
               "{name|id} [attr1 [attr2...]]", Category.COS, 1, Integer.MAX_VALUE),
        GET_DISTRIBUTION_LIST(
               "getDistributionList", "gdl", "{list@domain|id} [attr1 [attr2...]]", Category.LIST, 1,
               Integer.MAX_VALUE),
        GET_DISTRIBUTION_LIST_MEMBERSHIP("getDistributionListMembership", "gdlm",
               "{name@domain|id}", Category.LIST, 1, 1),
        GET_DOMAIN("getDomain", "gd",
               "[-e] {domain|id} [attr1 [attr2...]]", Category.DOMAIN, 1, Integer.MAX_VALUE),
        GET_DOMAIN_INFO(
               "getDomainInfo", "gdi", "name|id|virtualHostname {value} [attr1 [attr2...]]", Category.DOMAIN, 2,
               Integer.MAX_VALUE),
        GET_CONFIG_SMIME_CONFIG("getConfigSMIMEConfig", "gcsc", "[configName]",
               Category.DOMAIN, 0, 1),
        GET_DOMAIN_SMIME_CONFIG("getDomainSMIMEConfig", "gdsc", "name|id [configName]",
               Category.DOMAIN, 1, 2),
        GET_EFFECTIVE_RIGHTS("getEffectiveRights", "ger",
               "{target-type} [{target-id|target-name}] {grantee-id|grantee-name} [expandSetAttrs] [expandGetAttrs]",
               Category.RIGHT, 1, 5, null, new RightCommandHelp(false, false, false)),
        // for testing the provisioning interface only, comment out after testing, the soap is only used by admin console
        GET_CREATE_OBJECT_ATTRS("getCreateObjectAttrs", "gcoa",
                "{target-type} {domain-id|domain-name} {cos-id|cos-name} {grantee-id|grantee-name}", Category.RIGHT, 3,
                4),
        GET_FREEBUSY_QUEUE_INFO("getFreebusyQueueInfo", "gfbqi", "[{provider-name}]", Category.FREEBUSY, 0, 1),
        GET_GRANTS(
               "getGrants",
               "gg",
               "[-t {target-type} [{target-id|target-name}]] [-g {grantee-type} {grantee-id|grantee-name} [{0|1 (whether to include grants granted to groups the grantee belongs)}]]",
               Category.RIGHT, 2, 7, null, new RightCommandHelp(false, false, false)),
        GET_MAILBOX_INFO(
               "getMailboxInfo", "gmi", "{account}", Category.MAILBOX, 1, 1),
        GET_QUOTA_USAGE("getQuotaUsage", "gqu",
               "{server}", Category.MAILBOX, 1, 1),
        GET_RIGHT("getRight", "gr",
                "{right} [-e] (whether to expand combo rights recursively)", Category.RIGHT, 1, 2),
        GET_RIGHTS_DOC(
               "getRightsDoc", "grd", "[java packages]", Category.RIGHT, 0, Integer.MAX_VALUE),
        GET_SERVER(
               "getServer", "gs", "[-e] {name|id} [attr1 [attr2...]]", Category.SERVER, 1, Integer.MAX_VALUE),
        GET_UC_SERVICES(
               "getUCService", "gucs", "[-e] {name|id} [attr1 [attr2...]]", Category.UCSERVICE, 1, Integer.MAX_VALUE),
        GET_SHARE_INFO(
               "getShareInfo", "gsi", "{owner-name|owner-id}", Category.SHARE, 1, 1),
        GET_SPNEGO_DOMAIN(
               "getSpnegoDomain", "gsd", "", Category.MISC, 0, 0),
        GET_XMPP_COMPONENT("getXMPPComponent", "gxc",
               "{name|id} [attr1 [attr2...]]", Category.CONFIG, 1, Integer.MAX_VALUE),
        GRANT_RIGHT("grantRight",
               "grr",
               "{target-type} [{target-id|target-name}] {grantee-type} [{grantee-id|grantee-name} [secret]] {right}",
              Category.RIGHT, 3, 6, null, new RightCommandHelp(false, true, true)),
        HELP("help", "?", "commands",
               Category.MISC, 0, 1),
        LDAP(".ldap", ".l"),
        MODIFY_ACCOUNT("modifyAccount", "ma",
               "{name@domain|id} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 3, Integer.MAX_VALUE),
        MODIFY_ALWAYSONCLUSTER(
               "modifyAlwaysOnCluster", "maoc", "{name|id} [attr1 value1 [attr2 value2...]]",
               Category.ALWAYSONCLUSTER, 3, Integer.MAX_VALUE),
        MODIFY_CALENDAR_RESOURCE("modifyCalendarResource",
               "mcr", "{name@domain|id} [attr1 value1 [attr2 value2...]]", Category.CALENDAR, 3, Integer.MAX_VALUE),
        MODIFY_CONFIG(
               "modifyConfig", "mcf", "attr1 value1 [attr2 value2...]", Category.CONFIG, 2, Integer.MAX_VALUE),
        MODIFY_COS(
               "modifyCos", "mc", "{name|id} [attr1 value1 [attr2 value2...]]", Category.COS, 3, Integer.MAX_VALUE),
        MODIFY_DATA_SOURCE(
               "modifyDataSource", "mds", "{name@domain|id} {ds-name|ds-id} [attr1 value1 [attr2 value2...]]",
               Category.ACCOUNT, 4, Integer.MAX_VALUE),
        MODIFY_DISTRIBUTION_LIST("modifyDistributionList", "mdl",
               "{list@domain|id} attr1 value1 [attr2 value2...]", Category.LIST, 3, Integer.MAX_VALUE),
        MODIFY_DOMAIN(
               "modifyDomain", "md", "{domain|id} [attr1 value1 [attr2 value2...]]", Category.DOMAIN, 3,
               Integer.MAX_VALUE),
        MODIFY_CONFIG_SMIME_CONFIG("modifyConfigSMIMEConfig", "mcsc",
               "configName [attr2 value2...]]", Category.DOMAIN, 1, Integer.MAX_VALUE),
        MODIFY_DOMAIN_SMIME_CONFIG(
               "modifyDomainSMIMEConfig", "mdsc", "name|id configName [attr2 value2...]]", Category.DOMAIN, 2,
               Integer.MAX_VALUE),
        MODIFY_IDENTITY("modifyIdentity", "mid",
               "{name@domain|id} {identity-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 4,
               Integer.MAX_VALUE),
        MODIFY_SIGNATURE("modifySignature", "msig",
               "{name@domain|id} {signature-name|signature-id} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 4,
               Integer.MAX_VALUE),
        MODIFY_SERVER("modifyServer", "ms", "{name|id} [attr1 value1 [attr2 value2...]]",
               Category.SERVER, 3, Integer.MAX_VALUE),
        MODIFY_UC_SERVICE("modifyUCService", "mucs",
               "{name|id} [attr1 value1 [attr2 value2...]]", Category.UCSERVICE, 3, Integer.MAX_VALUE),
        MODIFY_XMPP_COMPONENT(
               "modifyXMPPComponent", "mxc", "{name@domain} [attr1 value1 [attr value2...]]", Category.CONFIG, 3,
               Integer.MAX_VALUE),
        PUSH_FREEBUSY("pushFreebusy", "pfb", "[account-id ...]", Category.FREEBUSY, 1,
               Integer.MAX_VALUE),
        PUSH_FREEBUSY_DOMAIN("pushFreebusyDomain", "pfbd", "{domain}", Category.FREEBUSY,
               1, 1),
        PURGE_ACCOUNT_CALENDAR_CACHE("purgeAccountCalendarCache", "pacc", "{name@domain|id} [...]",
               Category.CALENDAR, 1, Integer.MAX_VALUE),
        PURGE_FREEBUSY_QUEUE("purgeFreebusyQueue", "pfbq",
               "[{provider-name}]", Category.FREEBUSY, 0, 1),
        RECALCULATE_MAILBOX_COUNTS("recalculateMailboxCounts",
               "rmc", "{name@domain|id}", Category.MAILBOX, 1, 1),
        REMOVE_ACCOUNT_ALIAS("removeAccountAlias", "raa",
               "{name@domain|id} {alias@domain}", Category.ACCOUNT, 2, 2),
        REMOVE_ACCOUNT_LOGGER(
               "removeAccountLogger", "ral", "[-s/--server hostname] [{name@domain|id}] [{logging-category}]",
               Category.LOG, 0, 4),
        REMOVE_DISTRIBUTION_LIST_ALIAS("removeDistributionListAlias", "rdla",
               "{list@domain|id} {alias@domain}", Category.LIST, 2, 2),
        REMOVE_DISTRIBUTION_LIST_MEMBER(
               "removeDistributionListMember", "rdlm", "{list@domain|id} {member@domain}", Category.LIST, 2,
               Integer.MAX_VALUE),
        REMOVE_CONFIG_SMIME_CONFIG("removeConfigSMIMEConfig", "rcsc", "configName",
               Category.DOMAIN, 1, 1),
        REMOVE_DOMAIN_SMIME_CONFIG("removeDomainSMIMEConfig", "rdsc",
               "name|id configName", Category.DOMAIN, 2, 2),
        RENAME_ACCOUNT("renameAccount", "ra",
               "{name@domain|id} {newName@domain}", Category.ACCOUNT, 2, 2),
        CHANGE_PRIMARY_EMAIL("changePrimaryEmail", "cpe",
                "{name@domain|id} {newName@domain}", Category.ACCOUNT, 2, 2),
        RENAME_CALENDAR_RESOURCE(
               "renameCalendarResource", "rcr", "{name@domain|id} {newName@domain}", Category.CALENDAR, 2, 2),
        RENAME_COS(
               "renameCos", "rc", "{name|id} {newName}", Category.COS, 2, 2),
        RENAME_DISTRIBUTION_LIST(
               "renameDistributionList", "rdl", "{list@domain|id} {newName@domain}", Category.LIST, 2, 2),
        RENAME_DOMAIN(
               "renameDomain", "rd", "{domain|id} {newDomain}", Category.DOMAIN, 2, 2, Via.ldap),
        RENAME_UCSERVICE(
               "renameUCService", "rucs", "{name|id} {newName}", Category.UCSERVICE, 2, 2),
        REINDEX_MAILBOX(
               "reIndexMailbox", "rim",
               "{name@domain|id} {start|status|cancel} [{types|ids} {type or id} [,type or id...]]", Category.MAILBOX,
               2, Integer.MAX_VALUE, null, new ReindexCommandHelp()),
        COMPACT_INBOX_MAILBOX("compactIndexMailbox",
               "cim", "{name@domain|id} {start|status}", Category.MAILBOX, 2, Integer.MAX_VALUE),
        VERIFY_INDEX(
               "verifyIndex", "vi", "{name@domain|id}", Category.MAILBOX, 1, 1),
        GET_INDEX_STATS("getIndexStats",
               "gis", "{name@domain|id}", Category.MAILBOX, 1, 1),
        REVOKE_RIGHT("revokeRight", "rvr",
               "{target-type} [{target-id|target-name}] {grantee-type} [{grantee-id|grantee-name}] {right}",
               Category.RIGHT, 3, 5, null, new RightCommandHelp(false, false, true)),
        SEARCH_ACCOUNTS(
               "searchAccounts",
               "sa",
               "[-v] {ldap-query} [limit {limit}] [offset {offset}] [sortBy {attr}] [sortAscending 0|1*] [domain {domain}]",
               Category.SEARCH, 1, Integer.MAX_VALUE),
        SEARCH_CALENDAR_RESOURCES("searchCalendarResources", "scr",
               "[-v] domain attr op value [attr op value...]", Category.SEARCH, 1, Integer.MAX_VALUE, Via.ldap),
        SEARCH_GAL(
               "searchGal", "sg", "{domain} {name} [limit {limit}] [offset {offset}] [sortBy {attr}]",
               Category.SEARCH, 2, Integer.MAX_VALUE),
        SET_LOCAL_SERVER_ONLINE("setLocalServerOnline", "slso", "",
               Category.SERVER, 0, 0),
        SELECT_MAILBOX("selectMailbox", "sm", "{account-name} [{zmmailbox commands}]",
               Category.MAILBOX, 1, Integer.MAX_VALUE),
        SET_ACCOUNT_COS("setAccountCos", "sac",
               "{name@domain|id} {cos-name|cos-id}", Category.ACCOUNT, 2, 2),
        SET_PASSWORD("setPassword", "sp",
               "{name@domain|id} {password}", Category.ACCOUNT, 2, 2),
        SET_SERVER_OFFLINE("setServerOffline", "sso",
               "{name|id}", Category.SERVER, 1, 1),
        GET_ALL_MTA_AUTH_URLS("getAllMtaAuthURLs", "gamau", "",
               Category.SERVER, 0, 0),
        GET_ALL_REVERSE_PROXY_URLS("getAllReverseProxyURLs", "garpu", "",
               Category.REVERSEPROXY, 0, 0),
        GET_ALL_REVERSE_PROXY_BACKENDS("getAllReverseProxyBackends", "garpb", "",
               Category.REVERSEPROXY, 0, 0),
        GET_ALL_REVERSE_PROXY_DOMAINS("getAllReverseProxyDomains", "garpd", "",
               Category.REVERSEPROXY, 0, 0, Via.ldap),
        GET_ALL_MEMCACHED_SERVERS("getAllMemcachedServers", "gamcs",
               "", Category.SERVER, 0, 0),
        RELOAD_MEMCACHED_CLIENT_CONFIG("reloadMemcachedClientConfig", "rmcc",
               "all | mailbox-server [...]", Category.MISC, 1, Integer.MAX_VALUE, Via.soap),
        GET_MEMCACHED_CLIENT_CONFIG(
               "getMemcachedClientConfig", "gmcc", "all | mailbox-server [...]", Category.MISC, 1, Integer.MAX_VALUE,
               Via.soap),
        SOAP(".soap", ".s"),
        SYNC_GAL("syncGal", "syg", "{domain} [{token}]", Category.MISC, 1, 2),
        UPDATE_PRESENCE_SESSION_ID(
               "updatePresenceSessionId", "upsid", "{UC service name or id} {app-username} {app-password}",
               Category.MISC, 3, 3, Via.soap),
        RESET_ALL_LOGGERS("resetAllLoggers", "rlog", "[-s/--server hostname]",
               Category.LOG, 0, 2),
        UNLOCK_MAILBOX("unlockMailbox", "ulm", "{name@domain|id} [hostname (When unlocking a mailbox after a failed move attempt provide the hostname of the server that was the target for the failed move. Otherwise, do not include hostname parameter)]", Category.MAILBOX, 1, 2, Via.soap),
        CREATE_HAB_OU("createHABOrgUnit", "chou",
            "{domain} {ouName}", Category.HAB , 2, 2),
        LIST_HAB_OU("listHABOrgUnit", "lhou",
            "{domain}", Category.HAB , 1, 1),
        RENAME_HAB_OU("renameHABOrgUnit", "rhou",
            "{domain} {ouName} {newName}", Category.HAB , 3, 3),
        DELETE_HAB_OU("deleteHABOrgUnit", "dhou",
            "{domain} {ouName}", Category.HAB , 2, 2),
        CREATE_HAB_GROUP("createHABGroup", "chg",
            "{groupName} {ouName} {name@domain} {TRUE|FALSE} [attr1 value1 [attr2 value2...]]", Category.HAB , 3, Integer.MAX_VALUE),
        GET_HAB("getHAB", "ghab",
            "{habRootGrpId}", Category.HAB, 1, 1),
        MOVE_HAB_GROUP("moveHABGroup", "mhg",
            "{habRootGrpId} {habParentGrpId} {targetHabParentGrpId}", Category.HAB , 3, 3),
        ADD_HAB_GROUP_MEMBER("addHABGroupMember", "ahgm", "{name@domain|id} {member@domain}+",
                Category.HAB, 2, Integer.MAX_VALUE),
        REMOVE_HAB_GROUP_MEMBER(
                "removeHABGroupMember", "rhgm", "{name@domain|id} {member@domain}", Category.HAB, 2,
                Integer.MAX_VALUE),
        DELETE_HAB_GROUP("deleteHABGroup", "dhg", "{name@domain|id} [true|false]",
                Category.HAB, 1, 2),
        MODIFY_HAB_GROUP_SENIORITY("modifyHABGroupSeniority", "mhgs",
        "{habGrpId} {seniorityIndex} ", Category.HAB, 2, 2),
        GET_HAB_GROUP_MEMBERS("getHABGroupMembers", "ghgm", "{name@domain|id}",
                Category.HAB, 1, 1);

        private String mName;
        private String mAlias;
        private String mHelp;
        private CommandHelp mExtraHelp;
        private Category mCat;
        private int mMinArgLength = 0;
        private int mMaxArgLength = Integer.MAX_VALUE;
        private Via mVia;
        private boolean mNeedsSchemaExtension = false;

        public static enum Via {
            soap, ldap;
        }

        public String getName() {
            return mName;
        }

        public String getAlias() {
            return mAlias;
        }

        public String getHelp() {
            return mHelp;
        }

        public CommandHelp getExtraHelp() {
            return mExtraHelp;
        }

        public Category getCategory() {
            return mCat;
        }

        public boolean hasHelp() {
            return mHelp != null;
        }

        public boolean checkArgsLength(String args[]) {
            int len = args == null ? 0 : args.length - 1;
            return len >= mMinArgLength && len <= mMaxArgLength;
        }

        public Via getVia() {
            return mVia;
        }

        public boolean needsSchemaExtension() {
            return mNeedsSchemaExtension || (mCat == Category.RIGHT);
        }

        public boolean isDeprecated() {
            return false; // Used to return true if mCat was Category.NOTEBOOK - which has now been removed
        }

        private Command(String name, String alias) {
            mName = name;
            mAlias = alias;
        }

        private Command(String name, String alias, String help, Category cat) {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
        }

        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength) {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;
        }

        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength,
                Via via) {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;
            mVia = via;
        }

        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength,
                Via via, CommandHelp extraHelp) {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;
            mVia = via;
            mExtraHelp = extraHelp;
        }

        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength,
                Via via, CommandHelp extraHelp, boolean needsSchemaExtension) {
            this(name, alias, help, cat, minArgLength, maxArgLength, via, extraHelp);
            mNeedsSchemaExtension = needsSchemaExtension;
        }
    }

    private void addCommand(Command command) {
        String name = command.getName().toLowerCase();
        if (commandIndex.get(name) != null) {
            throw new RuntimeException("duplicate command: " + name);
        }
        String alias = command.getAlias().toLowerCase();
        if (commandIndex.get(alias) != null) {
            throw new RuntimeException("duplicate command: " + alias);
        }
        commandIndex.put(name, command);
        commandIndex.put(alias, command);
    }

    private void initCommands() {
        commandIndex = new HashMap<String, Command>();
        for (Command c : Command.values()) {
            addCommand(c);
        }
    }

    private Command lookupCommand(String command) {
        return commandIndex.get(command.toLowerCase());
    }

    /**
     * Commands that should always use LdapProv, but for convenience don't require the -l option specified.
     *
     * Commands that must use -l (e.g. gaa) are indicated in the Via field of the command definition
     */
    private boolean forceLdapButDontRequireUseLdapOption(Command command) {
        return (command == Command.DESCRIBE);
    }

    private boolean needProvisioningInstance(Command command) {
        return !(command == Command.HELP);
    }

    private ProvUtil() {
        initCommands();
    }

    public void initProvisioning() throws ServiceException {
        if (useLdap) {
            if (useLdapMaster) {
                LdapClient.masterOnly();
            }
            prov = Provisioning.getInstance();
        } else {
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI(LC.zimbra_admin_service_scheme.value() + serverHostname + ":" + serverPort
                    + AdminConstants.ADMIN_SERVICE_URI);
            if (debugLevel != SoapDebugLevel.none) {
                sp.soapSetHttpTransportDebugListener(this);
            }
            if (account != null && password != null) {
                sp.soapAdminAuthenticate(account, password);
            } else if (authToken != null) {
                sp.soapAdminAuthenticate(authToken);
            } else {
                sp.soapZimbraAdminAuthenticate();
            }
            prov = sp;
        }
    }

    private Command.Via violateVia(Command cmd) {
        Command.Via via = cmd.getVia();
        if (via == null) {
            return null;
        }
        if (via == Command.Via.ldap && !(prov instanceof LdapProv)) {
            return Command.Via.ldap;
        }
        if (via == Command.Via.soap && !(prov instanceof SoapProvisioning)) {
            return Command.Via.soap;
        }
        return null;
    }

    private boolean execute(String args[]) throws ServiceException, ArgException, IOException, HttpException {
        String[] members;
        Account account;
        AccountLoggerOptions alo;
        command = lookupCommand(args[0]);
        if (command == null) {
            return false;
        }
        Command.Via violatedVia = violateVia(command);
        if (violatedVia != null) {
            usage(violatedVia);
            return true;
        }
        if (!command.checkArgsLength(args)) {
            usage();
            return true;
        }
        if (command.needsSchemaExtension()) {
            loadLdapSchemaExtensionAttrs();
        }
        switch (command) {
        case ADD_ACCOUNT_ALIAS:
            prov.addAlias(lookupAccount(args[1]), args[2]);
            break;
        case ADD_ACCOUNT_LOGGER:
            alo = parseAccountLoggerOptions(args);
            if (!command.checkArgsLength(alo.args)) {
                usage();
                return true;
            }
            doAddAccountLogger(alo);
            break;
        case AUTO_COMPLETE_GAL:
            doAutoCompleteGal(args);
            break;
        case AUTO_PROV_CONTROL:
            prov.autoProvControl(args[1]);
            break;
        case CHANGE_PRIMARY_EMAIL:
            doChangePrimaryEmail(args);
            break;
        case COPY_COS:
            console.println(prov.copyCos(lookupCos(args[1]).getId(), args[2]).getId());
            break;
        case COUNT_ACCOUNT:
            doCountAccount(args);
            break;
        case COUNT_OBJECTS:
            doCountObjects(args);
            break;
        case CREATE_ACCOUNT:
            console.println(prov.createAccount(args[1], args[2].equals("") ? null : args[2],
                    getMapAndCheck(args, 3, true)).getId());
            break;
        case CREATE_ALIAS_DOMAIN:
            console.println(doCreateAliasDomain(args[1], args[2], getMapAndCheck(args, 3, true)).getId());
            break;
        case CREATE_ALWAYSONCLUSTER:
            console.println(prov.createAlwaysOnCluster(args[1], getMapAndCheck(args, 2, true)).getId());
            break;
        case CREATE_COS:
            console.println(prov.createCos(args[1], getMapAndCheck(args, 2, true)).getId());
            break;
        case CREATE_DOMAIN:
            console.println(prov.createDomain(args[1], getMapAndCheck(args, 2, true)).getId());
            break;
        case CREATE_IDENTITY:
            prov.createIdentity(lookupAccount(args[1]), args[2], getMapAndCheck(args, 3, true));
            break;
        case CREATE_SIGNATURE:
            console.println(prov.createSignature(lookupAccount(args[1]), args[2], getMapAndCheck(args, 3, true))
                    .getId());
            break;
        case CREATE_DATA_SOURCE:
            console.println(prov.createDataSource(lookupAccount(args[1]), DataSourceType.fromString(args[2]), args[3],
                    getMapAndCheck(args, 4, true)).getId());
            break;
        case CREATE_SERVER:
            console.println(prov.createServer(args[1], getMapAndCheck(args, 2, true)).getId());
            break;
        case CREATE_UC_SERVICE:
            console.println(prov.createUCService(args[1], getMapAndCheck(args, 2, true)).getId());
            break;
        case CREATE_XMPP_COMPONENT:
            doCreateXMPPComponent(args);
            break;
        case DESCRIBE:
            doDescribe(args);
            break;
        case EXIT:
            System.exit(errorOccursDuringInteraction ? 2 : 0);
            break;
        case FLUSH_CACHE:
            doFlushCache(args);
            break;
        case GENERATE_DOMAIN_PRE_AUTH_KEY:
            doGenerateDomainPreAuthKey(args);
            break;
        case GENERATE_DOMAIN_PRE_AUTH:
            doGenerateDomainPreAuth(args);
            break;
        case GET_ACCOUNT:
            doGetAccount(args);
            break;
        case GET_ACCOUNT_MEMBERSHIP:
            doGetAccountMembership(args);
            break;
        case GET_ALWAYSONCLUSTER:
            doGetAlwaysOnCluster(args);
            break;
        case GET_IDENTITIES:
            doGetAccountIdentities(args);
            break;
        case GET_SIGNATURES:
            doGetAccountSignatures(args);
            break;
        case GET_DATA_SOURCES:
            doGetAccountDataSources(args);
            break;
        case GET_ACCOUNT_LOGGERS:
            alo = parseAccountLoggerOptions(args);
            if (!command.checkArgsLength(alo.args)) {
                usage();
                return true;
            }
            doGetAccountLoggers(alo);
            break;
        case GET_ALL_ACCOUNT_LOGGERS:
            alo = parseAccountLoggerOptions(args);
            if (!command.checkArgsLength(alo.args)) {
                usage();
                return true;
            }
            doGetAllAccountLoggers(alo);
            break;
        case GET_ALL_ACCOUNTS:
            doGetAllAccounts(args);
            break;
        case GET_ALL_ACTIVE_SERVERS:
            doGetAllActiveServers(args);
            break;
        case GET_ALL_ADMIN_ACCOUNTS:
            doGetAllAdminAccounts(args);
            break;
        case GET_ALL_ALWAYSONCLUSTERS:
            doGetAllAlwaysOnClusters(args);
            break;
        case GET_ALL_CONFIG:
            dumpAttrs(prov.getConfig().getAttrs(), getArgNameSet(args, 1));
            break;
        case GET_ALL_COS:
            doGetAllCos(args);
            break;
        case GET_ALL_DOMAINS:
            doGetAllDomains(args);
            break;
        case GET_ALL_FREEBUSY_PROVIDERS:
            doGetAllFreeBusyProviders();
            break;
        case GET_ALL_RIGHTS:
            doGetAllRights(args);
            break;
        case GET_ALL_SERVERS:
            doGetAllServers(args);
            break;
        case GET_ALL_UC_SERVICES:
            doGetAllUCServices(args);
            break;
        case GET_CONFIG:
            doGetConfig(args);
            break;
        case GET_COS:
            dumpCos(lookupCos(args[1]), getArgNameSet(args, 2));
            break;
        case GET_DISTRIBUTION_LIST_MEMBERSHIP:
            doGetDistributionListMembership(lookupGroup(args[1]));
            break;
        case GET_DOMAIN:
            doGetDomain(args);
            break;
        case GET_DOMAIN_INFO:
            doGetDomainInfo(args);
            break;
        case GET_CONFIG_SMIME_CONFIG:
            doGetConfigSMIMEConfig(args);
            break;
        case GET_DOMAIN_SMIME_CONFIG:
            doGetDomainSMIMEConfig(args);
            break;
        case GET_FREEBUSY_QUEUE_INFO:
            doGetFreeBusyQueueInfo(args);
            break;
        case GET_RIGHT:
            doGetRight(args);
            break;
        case GET_RIGHTS_DOC:
            doGetRightsDoc(args);
            break;
        case GET_SERVER:
            doGetServer(args);
            break;
        case GET_UC_SERVICES:
            dumpUCService(lookupUCService(args[1]), getArgNameSet(args, 2));
            break;
        case GET_XMPP_COMPONENT:
            doGetXMPPComponent(args);
            break;
        case CHECK_RIGHT:
            doCheckRight(args);
            break;
        case GET_ALL_EFFECTIVE_RIGHTS:
            doGetAllEffectiveRights(args);
            break;
        case GET_EFFECTIVE_RIGHTS:
            doGetEffectiveRights(args);
            break;
        case GET_CREATE_OBJECT_ATTRS:
            doGetCreateObjectAttrs(args);
            break;
        case GET_GRANTS:
            doGetGrants(args);
            break;
        case GRANT_RIGHT:
            doGrantRight(args);
            break;
        case REVOKE_RIGHT:
            doRevokeRight(args);
            break;
        case HELP:
            doHelp(args);
            break;
        case MODIFY_ACCOUNT:
            prov.modifyAttrs(lookupAccount(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case MODIFY_ALWAYSONCLUSTER:
            prov.modifyAttrs(lookupAlwaysOnCluster(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case MODIFY_DATA_SOURCE:
            account = lookupAccount(args[1]);
            prov.modifyDataSource(account, lookupDataSourceId(account, args[2]), getMapAndCheck(args, 3, false));
            break;
        case MODIFY_IDENTITY:
            account = lookupAccount(args[1]);
            prov.modifyIdentity(account, args[2], getMapAndCheck(args, 3, false));
            break;
        case MODIFY_SIGNATURE:
            account = lookupAccount(args[1]);
            prov.modifySignature(account, lookupSignatureId(account, args[2]), getMapAndCheck(args, 3, false));
            break;
        case MODIFY_COS:
            prov.modifyAttrs(lookupCos(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case MODIFY_CONFIG:
            prov.modifyAttrs(prov.getConfig(), getMapAndCheck(args, 1, false), true);
            break;
        case MODIFY_DOMAIN:
            prov.modifyAttrs(lookupDomain(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case MODIFY_CONFIG_SMIME_CONFIG:
            doModifyConfigSMIMEConfig(args);
            break;
        case MODIFY_DOMAIN_SMIME_CONFIG:
            doModifyDomainSMIMEConfig(args);
            break;
        case MODIFY_SERVER:
            prov.modifyAttrs(lookupServer(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case MODIFY_UC_SERVICE:
            prov.modifyAttrs(lookupUCService(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case DELETE_ACCOUNT:
            doDeleteAccount(args);
            break;
        case DELETE_ALWAYSONCLUSTER:
            prov.deleteAlwaysOnCluster(lookupAlwaysOnCluster(args[1]).getId());
            break;
        case DELETE_COS:
            prov.deleteCos(lookupCos(args[1]).getId());
            break;
        case DELETE_DOMAIN:
            prov.deleteDomain(lookupDomain(args[1]).getId());
            break;
        case DELETE_IDENTITY:
            prov.deleteIdentity(lookupAccount(args[1]), args[2]);
            break;
        case DELETE_SIGNATURE:
            account = lookupAccount(args[1]);
            prov.deleteSignature(account, lookupSignatureId(account, args[2]));
            break;
        case DELETE_DATA_SOURCE:
            account = lookupAccount(args[1]);
            prov.deleteDataSource(account, lookupDataSourceId(account, args[2]));
            break;
        case DELETE_SERVER:
            prov.deleteServer(lookupServer(args[1]).getId());
            break;
        case DELETE_UC_SERVICE:
            prov.deleteUCService(lookupUCService(args[1]).getId());
            break;
        case DELETE_XMPP_COMPONENT:
            prov.deleteXMPPComponent(lookupXMPPComponent(args[1]));
            break;
        case PUSH_FREEBUSY:
            doPushFreeBusy(args);
            break;
        case PUSH_FREEBUSY_DOMAIN:
            doPushFreeBusyForDomain(args);
            break;
        case PURGE_FREEBUSY_QUEUE:
            doPurgeFreeBusyQueue(args);
            break;
        case PURGE_ACCOUNT_CALENDAR_CACHE:
            doPurgeAccountCalendarCache(args);
            break;
        case REMOVE_ACCOUNT_ALIAS:
            Account acct = lookupAccount(args[1], false);
            prov.removeAlias(acct, args[2]);
            // even if acct is null, we still invoke removeAlias and throw an exception afterwards.
            // this is so dangling aliases can be cleaned up as much as possible
            if (acct == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(args[1]);
            }
            break;
        case REMOVE_ACCOUNT_LOGGER:
            alo = parseAccountLoggerOptions(args);
            if (!command.checkArgsLength(alo.args)) {
                usage();
                return true;
            }
            doRemoveAccountLogger(alo);
            break;
        case REMOVE_CONFIG_SMIME_CONFIG:
            doRemoveConfigSMIMEConfig(args);
            break;
        case REMOVE_DOMAIN_SMIME_CONFIG:
            doRemoveDomainSMIMEConfig(args);
            break;
        case RENAME_ACCOUNT:
            doRenameAccount(args);
            break;
        case RENAME_COS:
            prov.renameCos(lookupCos(args[1]).getId(), args[2]);
            break;
        case RENAME_DOMAIN:
            doRenameDomain(args);
            break;
        case RENAME_UCSERVICE:
            prov.renameUCService(lookupUCService(args[1]).getId(), args[2]);
            break;
        case SET_ACCOUNT_COS:
            prov.setCOS(lookupAccount(args[1]), lookupCos(args[2]));
            break;
        case SET_SERVER_OFFLINE:
            doSetServerOffline(args);
            break;
        case SET_LOCAL_SERVER_ONLINE:
            doSetLocalServerOnline();
            break;
        case SEARCH_ACCOUNTS:
            doSearchAccounts(args);
            break;
        case SEARCH_GAL:
            doSearchGal(args);
            break;
        case SYNC_GAL:
            doSyncGal(args);
            break;
        case SET_PASSWORD:
            SetPasswordResult result = prov.setPassword(lookupAccount(args[1]), args[2]);
            if (result.hasMessage()) {
                console.println(result.getMessage());
            }
            break;
        case CHECK_PASSWORD_STRENGTH:
            prov.checkPasswordStrength(lookupAccount(args[1]), args[2]);
            console.println("Password passed strength check.");
            break;
        case CREATE_DISTRIBUTION_LIST:
            console.println(prov.createGroup(args[1], getMapAndCheck(args, 2, true), false).getId());
            break;
        case CREATE_DYNAMIC_DISTRIBUTION_LIST:
            console.println(prov.createGroup(args[1], getMapAndCheck(args, 2, true), true).getId());
            break;
        case CREATE_DISTRIBUTION_LISTS_BULK:
            doCreateDistributionListsBulk(args);
            break;
        case GET_ALL_DISTRIBUTION_LISTS:
            doGetAllDistributionLists(args);
            break;
        case GET_DISTRIBUTION_LIST:
            dumpGroup(lookupGroup(args[1]), getArgNameSet(args, 2));
            break;
        case GET_ALL_XMPP_COMPONENTS:
            doGetAllXMPPComponents();
            break;
        case MODIFY_DISTRIBUTION_LIST:
            prov.modifyAttrs(lookupGroup(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case DELETE_DISTRIBUTION_LIST:
            doDeleteDistributionList(args);
            break;
        case ADD_DISTRIBUTION_LIST_MEMBER:
            doAddMember(args);
            break;
        case REMOVE_DISTRIBUTION_LIST_MEMBER:
            doRemoveMember(args);
            break;
        case CREATE_BULK_ACCOUNTS:
            doCreateAccountsBulk(args);
            break;
        case ADD_DISTRIBUTION_LIST_ALIAS:
            prov.addGroupAlias(lookupGroup(args[1]), args[2]);
            break;
        case REMOVE_DISTRIBUTION_LIST_ALIAS:
            Group dl = lookupGroup(args[1], false);
            // Even if dl is null, we still invoke removeAlias.
            // This is so dangling aliases can be cleaned up as much as possible.
            // If dl is null, the NO_SUCH_DISTRIBUTION_LIST thrown by SOAP will contain
            // null as the dl identity, because SoapProvisioning sends no id to the server.
            // In this case, we catch the NO_SUCH_DISTRIBUTION_LIST and throw another one
            // with the named/id entered on the comand line.
            try {
                prov.removeGroupAlias(dl, args[2]);
            } catch (ServiceException e) {
                if (!(dl == null && AccountServiceException.NO_SUCH_DISTRIBUTION_LIST.equals(e.getCode()))) {
                    throw e;
                }
                // else eat the exception, we will throw below
            }
            if (dl == null) {
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(args[1]);
            }
            break;
        case RENAME_DISTRIBUTION_LIST:
            prov.renameGroup(lookupGroup(args[1]).getId(), args[2]);
            break;
        case CREATE_CALENDAR_RESOURCE:
            console.println(prov.createCalendarResource(args[1], args[2].isEmpty() ? null : args[2],
                    getMapAndCheck(args, 3, true)).getId());
            break;
        case DELETE_CALENDAR_RESOURCE:
            prov.deleteCalendarResource(lookupCalendarResource(args[1]).getId());
            break;
        case MODIFY_CALENDAR_RESOURCE:
            prov.modifyAttrs(lookupCalendarResource(args[1]), getMapAndCheck(args, 2, false), true);
            break;
        case RENAME_CALENDAR_RESOURCE:
            prov.renameCalendarResource(lookupCalendarResource(args[1]).getId(), args[2]);
            break;
        case GET_CALENDAR_RESOURCE:
            dumpCalendarResource(lookupCalendarResource(args[1]), true, getArgNameSet(args, 2));
            break;
        case GET_ALL_CALENDAR_RESOURCES:
            doGetAllCalendarResources(args);
            break;
        case SEARCH_CALENDAR_RESOURCES:
            doSearchCalendarResources(args);
            break;
        case GET_SHARE_INFO:
            doGetShareInfo(args);
            break;
        case GET_SPNEGO_DOMAIN:
            doGetSpnegoDomain();
            break;
        case GET_QUOTA_USAGE:
            doGetQuotaUsage(args);
            break;
        case GET_MAILBOX_INFO:
            doGetMailboxInfo(args);
            break;
        case REINDEX_MAILBOX:
            doReIndexMailbox(args);
            break;
        case COMPACT_INBOX_MAILBOX:
            doCompactIndexMailbox(args);
            break;
        case VERIFY_INDEX:
            doVerifyIndex(args);
            break;
        case GET_INDEX_STATS:
            doGetIndexStats(args);
            break;
        case RECALCULATE_MAILBOX_COUNTS:
            doRecalculateMailboxCounts(args);
            break;
        case SELECT_MAILBOX:
            if (!(prov instanceof SoapProvisioning)) {
                throwSoapOnly();
            }
            ZMailboxUtil util = new ZMailboxUtil();
            util.setVerbose(verboseMode);
            util.setDebug(debugLevel != SoapDebugLevel.none);
            boolean smInteractive = interactiveMode && args.length < 3;
            util.setInteractive(smInteractive);
            util.selectMailbox(args[1], (SoapProvisioning) prov);
            if (smInteractive) {
                util.interactive(cliReader);
            } else if (args.length > 2) {
                String newArgs[] = new String[args.length - 2];
                System.arraycopy(args, 2, newArgs, 0, newArgs.length);
                util.execute(newArgs);
            } else {
                throw ZClientException.CLIENT_ERROR("command only valid in interactive mode or with arguments", null);
            }
            break;
        case GET_ALL_MTA_AUTH_URLS:
            doGetAllMtaAuthURLs();
            break;
        case GET_ALL_REVERSE_PROXY_URLS:
            doGetAllReverseProxyURLs();
            break;
        case GET_ALL_REVERSE_PROXY_BACKENDS:
            doGetAllReverseProxyBackends();
            break;
        case GET_ALL_REVERSE_PROXY_DOMAINS:
            doGetAllReverseProxyDomains();
            break;
        case GET_ALL_MEMCACHED_SERVERS:
            doGetAllMemcachedServers();
            break;
        case RELOAD_MEMCACHED_CLIENT_CONFIG:
            doReloadMemcachedClientConfig(args);
            break;
        case GET_MEMCACHED_CLIENT_CONFIG:
            doGetMemcachedClientConfig(args);
            break;
        case GET_AUTH_TOKEN_INFO:
            doGetAuthTokenInfo(args);
            break;
        case UPDATE_PRESENCE_SESSION_ID:
            doUpdatePresenceSessionId(args);
            break;
        case SOAP:
            // HACK FOR NOW
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI("https://localhost:" + serverPort + AdminConstants.ADMIN_SERVICE_URI);
            sp.soapZimbraAdminAuthenticate();
            prov = sp;
            break;
        case LDAP:
            // HACK FOR NOW
            prov = Provisioning.getInstance();
            break;
        case RESET_ALL_LOGGERS:
            doResetAllLoggers(args);
            break;
        case UNLOCK_MAILBOX:
            doUnlockMailbox(args);
            break;
        case CREATE_HAB_OU:
            doCreateHabOrgUnit(args);
            break;
        case LIST_HAB_OU:
            doListHabOrgUnit(args);
            break;
        case RENAME_HAB_OU:
            doRenameHabOrgUnit(args);
            break;
        case DELETE_HAB_OU:
            doDeleteHabOrgUnit(args);
            break;
        case CREATE_HAB_GROUP:
           doCreateHabGroup(args);
           break;
        case GET_HAB:
            doGetHab(args);
            break;
        case MOVE_HAB_GROUP:
            modifyHabGroup(args);
            break;
        case MODIFY_HAB_GROUP_SENIORITY:
            modifyHabGroupSeniority(args);
            break;
        case ADD_HAB_GROUP_MEMBER:
            doAddMember(args);
            break;
        case DELETE_HAB_GROUP:
            doDeleteDistributionList(args);
            break;
        case REMOVE_HAB_GROUP_MEMBER:
            doRemoveMember(args);
            break;
        case GET_HAB_GROUP_MEMBERS:
            doGetHABGroupMembers(args);
            break;
        default:
            return false;
        }
        return true;
    }

    private void doAddMember(String[] args) throws ServiceException {
        String[] members = new String[args.length - 2];
        System.arraycopy(args, 2, members, 0, args.length - 2);
        prov.addGroupMembers(lookupGroup(args[1]), members);
    }

    private void doRemoveMember(String[] args) throws ServiceException {
        String[] members = new String[args.length - 2];
        System.arraycopy(args, 2, members, 0, args.length - 2);
        prov.removeGroupMembers(lookupGroup(args[1]), members);
    }

    private void doGetHABGroupMembers(String[] args) throws ServiceException {
        List<HABGroupMember> groupMembers = prov.getHABGroupMembers(lookupGroup(args[1]));
        groupMembers.stream().forEach(console::println);
    }

    private void sendMailboxLockoutRequest(String acctName, String server, String operation) throws ServiceException, IOException, HttpException {
        LockoutMailboxRequest req =  LockoutMailboxRequest.create(AccountNameSelector.fromName(acctName));
        req.setOperation(operation);
        String url = URLUtil.getAdminURL(server);
        ZAuthToken token = ((SoapProvisioning)prov).getAuthToken();
        SoapHttpTransport transport = new SoapHttpTransport(url);
        transport.setAuthToken(token);
        transport.invokeWithoutSession(JaxbUtil.jaxbToElement(req));
    }

    private void doUnlockMailbox(String[] args) throws ServiceException {
        String accountVal = null;
        if(args.length > 1) {
            accountVal = args[1];
        } else {
            usage();
            return;
        }

        if(accountVal != null) {
            Account acct = lookupAccount(accountVal); //will throw NO_SUCH_ACCOUNT if not found
            if(!acct.getAccountStatus().isActive()) {
                throw ServiceException.FAILURE(String.format("Cannot unlock mailbox for account %s. Account status must be %s. Curent account status is %s. "
                        + "You must change the value of zimbraAccountStatus to '%s' first",
                        accountVal, AccountStatus.active, acct.getAccountStatus(), AccountStatus.active), null);
            }
            String accName = acct.getName();
            String server = acct.getMailHost();
            try {
                sendMailboxLockoutRequest(accName, server, AdminConstants.A_END);
            } catch (ServiceException e) {
                if (ServiceException.UNKNOWN_DOCUMENT.equals(e.getCode())) {
                    throw ServiceException.FAILURE("source server version does not support " + AdminConstants.E_LOCKOUT_MAILBOX_REQUEST, e);
                } else if (ServiceException.NOT_FOUND.equals(e.getCode())) { //if mailbox is not locked, move on
                    printOutput("Warning: " + e.getMessage());
                } else {
                    throw e;
                }
            } catch (IOException | HttpException e) {
                throw ServiceException.FAILURE(String.format("Error sending %s (operation = %s) request for %s to %s",AdminConstants.E_LOCKOUT_MAILBOX_REQUEST, AdminConstants.A_END, accountVal, server), e);
            }

            //unregister moveout if hostname is provided
            if(args.length > 2) {
                //set account status to maintenance and lock the mailbox to avoid race conditions
                acct.setAccountStatus(AccountStatus.maintenance);
                try {
                    sendMailboxLockoutRequest(accName, server, AdminConstants.A_START);
                } catch (IOException | HttpException e) {
                    throw ServiceException.FAILURE(String.format("Error sending %s (opertion = %s) request for %s to %s.\n Warning: Account is left in maintenance state!",AdminConstants.E_LOCKOUT_MAILBOX_REQUEST, AdminConstants.A_START, accountVal, server), e);
                }

                //unregister moveout via SOAP
                String targetServer = args[2];
                try {
                    UnregisterMailboxMoveOutRequest unregisterReq = UnregisterMailboxMoveOutRequest.create(MailboxMoveSpec.createForNameAndTarget(accName, targetServer));
                    String url = URLUtil.getAdminURL(server);
                    ZAuthToken token = ((SoapProvisioning)prov).getAuthToken();
                    SoapHttpTransport transport = new SoapHttpTransport(url);
                    transport.setAuthToken(token);
                    transport.invokeWithoutSession(JaxbUtil.jaxbToElement(unregisterReq));
                } catch (ServiceException e) {
                    if (ServiceException.UNKNOWN_DOCUMENT.equals(e.getCode())) {
                        throw ServiceException.FAILURE(String.format("target server version does not support %s.", BackupConstants.E_UNREGISTER_MAILBOX_MOVE_OUT_REQUEST), e);
                    } else {
                        throw ServiceException.FAILURE("Failed to unregister mailbox moveout", e);
                    }
                } catch (IOException e) {
                    throw ServiceException.FAILURE(String.format("Error sending %s request for %s to %s.",BackupConstants.E_UNREGISTER_MAILBOX_MOVE_OUT_REQUEST, accountVal, server), e);
                } finally {
                    //unlock mailbox object and end account maintenance even if failed to unregister moveout
                    try {
                        sendMailboxLockoutRequest(accName, server, AdminConstants.A_END);
                    } catch (ServiceException e) {
                        //print error messages, but don't throw any more exceptions, because we have to set account status back to 'active'
                        if (ServiceException.UNKNOWN_DOCUMENT.equals(e.getCode())) {
                            printError("source server version does not support " + AdminConstants.E_LOCKOUT_MAILBOX_REQUEST);
                        } else {
                            printError(String.format("Error: failed to unregister mailbox moveout.\n Exception: %s.", e.getMessage()));
                        }
                    } catch (IOException | HttpException e) {
                        printError(String.format("Error sending %s (operation = %s) request for %s to %s after unregistering moveout. Exception: %s", AdminConstants.E_LOCKOUT_MAILBOX_REQUEST, AdminConstants.A_END, accountVal, server, e.getMessage()));
                    }
                    //end account maintenance
                    acct.setAccountStatus(AccountStatus.active);
                }
            }
        }
    }
    
    private void doCreateHabOrgUnit(String[] args) throws ServiceException {
        if(args.length != 3) { 
            usage();
            return;
        }
        Domain domain = lookupDomain(args[1], prov, Boolean.FALSE);
        
        if (prov instanceof SoapProvisioning) {
            ((SoapProvisioning) prov).createHabOrgUnit(domain, args[2]);
        } else {
            prov.createHabOrgUnit(domain, args[2]);
        }
    }

    private void doListHabOrgUnit(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
            return;
        }
        Domain domain = lookupDomain(args[1], prov, Boolean.FALSE);
        Set<String> resultSet;
        if (prov instanceof SoapProvisioning) {
            resultSet = ((SoapProvisioning) prov).listHabOrgUnit(domain);
        } else {
            resultSet = prov.listHabOrgUnit(domain);
        }
        for (String result : resultSet) {
            console.printf("%s\n", result);
        }
        return;
    }
    
    private void doRenameHabOrgUnit(String[] args)  throws ServiceException {
        if(args.length != 4) { 
            usage();
            return;
        }
        Domain domain = lookupDomain(args[1], prov, Boolean.FALSE);
        if (prov instanceof SoapProvisioning) {
            ((SoapProvisioning) prov).renameHabOrgUnit(domain, args[2], args[3]);
        } else {
            prov.renameHabOrgUnit(domain, args[2], args[3]);
        }
    }
    
    private void doDeleteHabOrgUnit(String[] args)  throws ServiceException {
        if(args.length != 3) { 
            usage();
            return;
        }
        Domain domain = lookupDomain(args[1], prov, Boolean.FALSE);
        if (prov instanceof SoapProvisioning) {
            ((SoapProvisioning) prov).deleteHabOrgUnit(domain, args[2]);
        } else {
            prov.deleteHabOrgUnit(domain, args[2]);
        }
    }

    private void doGetHab(String[] args)  throws ServiceException {
        if(args.length != 2) { 
            usage();
            return;
        }
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Element response = sp.getHab(args[1]);
        printOutput(response.prettyPrint());
    }

    private void modifyHabGroup(String[] args)  throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        //{habRootGrpId} {habParentGrpId} {targetHabParentGrpId} 
        if (args.length == 4) {
            ((SoapProvisioning) prov).modifyHabGroup(args[1], args[2], args[3]);
        } else if (args.length == 3) {
            ((SoapProvisioning) prov).modifyHabGroup(args[1], null, args[2]);
        } else {
            usage();
            return;
        }
    }
    
    private void modifyHabGroupSeniority(String[] args)  throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        
        //{habGrpId} {seniorityIndex} 
        if (args.length == 3) {
            ((SoapProvisioning) prov).modifyHabGroupSeniority(args[1], args[2]);
        } else {
            usage();
            return;
        }
    }

    private void doCreateHabGroup(String args[]) throws ServiceException, ArgException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        if(args.length < 4) {
            usage();
            return;
        }
        String isDynamic = "false";
        if (args.length > 4) {
            isDynamic = args[4];
        }
        ((SoapProvisioning) prov).createHabGroup(args[1],args[2],args[3], isDynamic, getMapAndCheck(args, 5, false));
    }
    private void doGetDomain(String[] args) throws ServiceException {
        boolean applyDefault = true;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-e")) {
                applyDefault = false;
            } else {
                break;
            }
            i++;
        }
        if (i >= args.length) {
            usage();
            return;
        }
        dumpDomain(lookupDomain(args[i], prov, applyDefault), applyDefault, getArgNameSet(args, i + 1));
    }

    private void doGetDomainInfo(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Key.DomainBy by = Key.DomainBy.fromString(args[1]);
        String key = args[2];
        Domain domain = sp.getDomainInfo(by, key);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(key);
        } else {
            dumpDomain(domain, getArgNameSet(args, 3));
        }
    }

    private void doRenameDomain(String[] args) throws ServiceException {

        // bug 56768
        // if we are not already using master only, force it to use master.
        // Note: after rename domain, the zmprov instance will stay in "master only" mode.
        if (!useLdapMaster) {
            ((LdapProv) prov).alwaysUseMaster();
        }

        LdapProv lp = (LdapProv) prov;
        Domain domain = lookupDomain(args[1]);
        lp.renameDomain(domain.getId(), args[2]);
        printOutput("domain " + args[1] + " renamed to " + args[2]);
        printOutput("Note: use zmlocalconfig to check and update any localconfig settings referencing domain '"
                + args[1] + "' on all servers.");
        printOutput("Use /opt/zimbra/libexec/zmdkimkeyutil to recreate the DKIM entries for new domain name if required.");
    }

    private void doGetQuotaUsage(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        List<QuotaUsage> result = sp.getQuotaUsage(args[1]);
        for (QuotaUsage u : result) {
            console.printf("%s %d %d\n", u.getName(), u.getLimit(), u.getUsed());
        }
    }

    private void doGetMailboxInfo(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = lookupAccount(args[1]);
        MailboxInfo info = sp.getMailbox(acct);
        console.printf("mailboxId: %s\nquotaUsed: %d\n", info.getMailboxId(), info.getUsed());
    }

    private void doReIndexMailbox(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = lookupAccount(args[1]);
        ReIndexBy by = null;
        String[] values = null;
        if (args.length > 3) {
            try {
                by = ReIndexBy.valueOf(args[3]);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid reindex-by", null);
            }
            if (args.length > 4) {
                values = new String[args.length - 4];
                System.arraycopy(args, 4, values, 0, args.length - 4);
            } else {
                throw ServiceException.INVALID_REQUEST("missing reindex-by values", null);
            }
        }
        ReIndexInfo info = sp.reIndex(acct, args[2], by, values);
        ReIndexInfo.Progress progress = info.getProgress();
        console.printf("status: %s\n", info.getStatus());
        if (progress != null) {
            console.printf("progress: numSucceeded=%d, numFailed=%d, numRemaining=%d\n", progress.getNumSucceeded(),
                    progress.getNumFailed(), progress.getNumRemaining());
        }
    }

    private void doCompactIndexMailbox(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = lookupAccount(args[1]);
        String status = sp.compactIndex(acct, args[2]);
        console.printf("status: %s\n", status);
    }

    private void doVerifyIndex(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        console.println("Verifying, on a large index it can take quite a long time...");
        SoapProvisioning soap = (SoapProvisioning) prov;
        SoapProvisioning.VerifyIndexResult result = soap.verifyIndex(lookupAccount(args[1]));
        console.println();
        console.print(result.message);
        if (!result.status) {
            throw ServiceException.FAILURE("The index may be corrupted. Run reIndexMailbox(rim) to repair.", null);
        }
    }

    private void doGetIndexStats(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = lookupAccount(args[1]);
        IndexStatsInfo stats = sp.getIndexStats(acct);
        console.printf("stats: maxDocs:%d numDeletedDocs:%d\n", stats.getMaxDocs(), stats.getNumDeletedDocs());
    }

    private void doRecalculateMailboxCounts(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account account = lookupAccount(args[1]);
        long quotaUsed = sp.recalculateMailboxCounts(account);
        console.printf("account: " + account.getName() + "\nquotaUsed: " + quotaUsed + "\n");
    }

    private class AccountLoggerOptions {
        String server;
        String[] args;
    }

    /**
     * Handles an optional <tt>-s</tt> or <tt>--server</tt> argument that may be passed to the logging commands. Returns
     * an <tt>AccountLogggerOptions</tt> object that contains all arguments except the server option and value.
     */
    private AccountLoggerOptions parseAccountLoggerOptions(String[] args) throws ServiceException {
        AccountLoggerOptions alo = new AccountLoggerOptions();
        if (args.length > 1 && (args[1].equals("-s") || args[1].equals("--server"))) {
            if (args.length == 2) {
                throw ServiceException.FAILURE("Server name not specified.", null);
            }
            alo.server = args[2];

            int numArgs = args.length - 2;
            alo.args = new String[numArgs];
            alo.args[0] = args[0];
            for (int i = 1; i < numArgs; i++) {
                alo.args[i] = args[i + 2];
            }
        } else {
            alo.args = args;
        }
        return alo;
    }

    private void doAddAccountLogger(AccountLoggerOptions alo) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = lookupAccount(alo.args[1]);
        sp.addAccountLogger(acct, alo.args[2], alo.args[3], alo.server);
    }

    private void doGetAccountLoggers(AccountLoggerOptions alo) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = lookupAccount(alo.args[1]);
        for (AccountLogger accountLogger : sp.getAccountLoggers(acct, alo.server)) {
            console.printf("%s=%s\n", accountLogger.getCategory(), accountLogger.getLevel());
        }
    }

    private void doGetAllAccountLoggers(AccountLoggerOptions alo) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;

        Map<String, List<AccountLogger>> allLoggers = sp.getAllAccountLoggers(alo.server);
        for (String accountName : allLoggers.keySet()) {
            console.printf("# name %s\n", accountName);
            for (AccountLogger logger : allLoggers.get(accountName)) {
                console.printf("%s=%s\n", logger.getCategory(), logger.getLevel());
            }
        }
    }

    private void doRemoveAccountLogger(AccountLoggerOptions alo) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sp = (SoapProvisioning) prov;
        Account acct = null;
        String category = null;
        if (alo.args.length == 2) {
            // Hack: determine if it's an account or category, based on the name.
            String arg = alo.args[1];
            if (arg.startsWith("zimbra.") || arg.startsWith("com.zimbra")) {
                category = arg;
            } else {
                acct = lookupAccount(alo.args[1]);
            }
        }
        if (alo.args.length == 3) {
            acct = lookupAccount(alo.args[1]);
            category = alo.args[2];
        }
        sp.removeAccountLoggers(acct, category, alo.server);
    }

    private void doResetAllLoggers(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        SoapProvisioning sprov = (SoapProvisioning) prov;
        String server = null;
        if (args.length > 1 && ("-s".equals(args[1]) || "--server".equals(args[1]))) {
            server = args.length > 0 ? args[2] : null;
        }
        sprov.resetAllLoggers(server);
    }

    private void doCreateAccountsBulk(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String domain = args[1];
            String password = "test123";
            String nameMask = args[2];
            int numAccounts = Integer.parseInt(args[3]);
            for (int ix = 0; ix < numAccounts; ix++) {
                String name = nameMask + Integer.toString(ix) + "@" + domain;
                Map<String, Object> attrs = new HashMap<String, Object>();
                String displayName = nameMask + " N. " + Integer.toString(ix);
                StringUtil.addToMultiMap(attrs, "displayName", displayName);
                Account account = prov.createAccount(name, password, attrs);
                console.println(account.getId());
            }
        }
    }

    private Domain doCreateAliasDomain(String aliasDomain, String localDoamin, Map<String, Object> attrs)
            throws ServiceException {
        Domain local = lookupDomain(localDoamin);
        if (!local.isLocal()) {
            throw ServiceException.INVALID_REQUEST("target domain must be a local domain", null);
        }
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.alias.name());
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, local.getId());
        return prov.createDomain(aliasDomain, attrs);
    }

    private void doGetAccount(String[] args) throws ServiceException {
        boolean applyDefault = true;
        int acctPos = 1;

        if (args[1].equals("-e")) {
            if (args.length > 1) {
                applyDefault = false;
                acctPos = 2;
            } else {
                usage();
                return;
            }
        }

        dumpAccount(lookupAccount(args[acctPos], true, applyDefault), applyDefault, getArgNameSet(args, acctPos + 1));
    }

    private void doGetAccountMembership(String[] args) throws ServiceException {
        String key = null;
        boolean idsOnly = false;
        if (args.length > 2) {
            idsOnly = args[1].equals("-i");
            key = args[2];
        } else {
            key = args[1];
        }
        Account account = lookupAccount(key);
        if (idsOnly) {
            Set<String> lists = prov.getGroups(account);
            for (String id : lists) {
                console.println(id);
            }
        } else {
            HashMap<String, String> via = new HashMap<String, String>();
            List<Group> groups = prov.getGroups(account, false, via);
            for (Group group : groups) {
                String viaDl = via.get(group.getName());
                if (viaDl != null) {
                    console.println(group.getName() + " (via " + viaDl + ")");
                } else {
                    console.println(group.getName());
                }
            }
        }
    }

    private static class ShareInfoVisitor implements PublishedShareInfoVisitor {

        private static final String mFormat = "%-36.36s %-15.15s %-15.15s %-5.5s %-20.20s %-10.10s %-10.10s %-10.10s %-5.5s %-5.5s %-36.36s %-15.15s %-15.15s\n";

        private static void printHeadings() {
            console.printf(mFormat, "owner id", "owner email", "owner display", "id", "path", "view", "type", "rights",
                    "mid", "gt", "grantee id", "grantee name", "grantee display");

            console.printf(mFormat, "------------------------------------", // owner id
                    "---------------", // owner email
                    "---------------", // owner display
                    "-----", // id
                    "--------------------", // path
                    "----------", // default view
                    "----------", // type
                    "----------", // rights
                    "-----", // mountpoint id if mounted
                    "-----", // grantee type
                    "------------------------------------", // grantee id
                    "---------------", // grantee name
                    "---------------"); // grantee display
        }

        @Override
        public void visit(ShareInfoData shareInfoData) throws ServiceException {
            console.printf(mFormat, shareInfoData.getOwnerAcctId(), shareInfoData.getOwnerAcctEmail(),
                    shareInfoData.getOwnerAcctDisplayName(), String.valueOf(shareInfoData.getItemId()),
                    shareInfoData.getPath(), shareInfoData.getFolderDefaultView(), shareInfoData.getType().name(),
                    shareInfoData.getRights(), shareInfoData.getMountpointId_zmprov_only(),
                    shareInfoData.getGranteeType(), shareInfoData.getGranteeId(), shareInfoData.getGranteeName(),
                    shareInfoData.getGranteeDisplayName());
        }
    };

    private void doGetShareInfo(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        Account owner = lookupAccount(args[1]);

        ShareInfoVisitor.printHeadings();
        prov.getShareInfo(owner, new ShareInfoVisitor());
    }

    private void doGetSpnegoDomain() throws ServiceException {
        Config config = prov.getConfig();
        String spnegoAuthRealm = config.getSpnegoAuthRealm();
        if (spnegoAuthRealm != null) {
            Domain domain = prov.get(Key.DomainBy.krb5Realm, spnegoAuthRealm);
            if (domain != null) {
                console.println(domain.getName());
            }
        }
    }

    private boolean confirm(String msg) {
        if (batchMode) {
            return true;
        }

        console.println(msg);
        console.print("Continue? [Y]es, [N]o: ");

        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            String line = StringUtil.readLine(in);
            if ("y".equalsIgnoreCase(line) || "yes".equalsIgnoreCase(line)) {
                return true;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }

    private void doDeleteAccount(String[] args) throws ServiceException {
        if (prov instanceof LdapProv) {
            boolean confirmed = confirm("-l option is specified.  "
                    + "Only the LDAP entry of the account will be deleted.\n"
                    + "DB data of the account and associated blobs will not be deleted.\n");

            if (!confirmed) {
                console.println("aborted");
                return;
            }
        }

        String key = args[1];
        Account acct = lookupAccount(key);
        if (key.equalsIgnoreCase(acct.getId()) || key.equalsIgnoreCase(acct.getName())
                || acct.getName().equalsIgnoreCase(key + "@" + acct.getDomainName())) {
            prov.deleteAccount(acct.getId());
        } else {
            throw ServiceException.INVALID_REQUEST(
                    "argument to deleteAccount must be an account id or the account's primary name", null);
        }
    }

    private void doRenameAccount(String[] args) throws ServiceException {
        if (prov instanceof LdapProv) {
            boolean confirmed = confirm("-l option is specified.  "
                    + "Only the LDAP portion of the account will be deleted.\n"
                    + "DB data of the account will not be renamed.\n");

            if (!confirmed) {
                console.println("aborted");
                return;
            }
        }

        prov.renameAccount(lookupAccount(args[1]).getId(), args[2]);
    }

    private void doChangePrimaryEmail(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        ((SoapProvisioning) prov).changePrimaryEmail(lookupAccount(args[1]).getId(), args[2]);
    }

    private void doGetAccountIdentities(String[] args) throws ServiceException {
        Account account = lookupAccount(args[1]);
        Set<String> argNameSet = getArgNameSet(args, 2);
        for (Identity identity : prov.getAllIdentities(account)) {
            dumpIdentity(identity, argNameSet);
        }
    }

    private void doGetAccountSignatures(String[] args) throws ServiceException {
        Account account = lookupAccount(args[1]);
        Set<String> argNameSet = getArgNameSet(args, 2);
        for (Signature signature : prov.getAllSignatures(account)) {
            dumpSignature(signature, argNameSet);
        }
    }

    private void dumpDataSource(DataSource dataSource, Set<String> argNameSet) throws ServiceException {
        console.println("# name " + dataSource.getName());
        console.println("# type " + dataSource.getType());
        Map<String, Object> attrs = dataSource.getAttrs();
        dumpAttrs(attrs, argNameSet);
        console.println();
    }

    private void doGetAccountDataSources(String[] args) throws ServiceException {
        Account account = lookupAccount(args[1]);
        Set<String> attrNameSet = getArgNameSet(args, 2);
        for (DataSource dataSource : prov.getAllDataSources(account)) {
            dumpDataSource(dataSource, attrNameSet);
        }
    }

    private void doGetDistributionListMembership(Group group) throws ServiceException {
        String[] members;
        if (group instanceof DynamicGroup) {
            members = ((DynamicGroup) group).getAllMembers(true);
        } else {
            members = group.getAllMembers();
        }

        int count = members == null ? 0 : members.length;
        console.println("# distributionList " + group.getName() + " memberCount=" + count);
        console.println();
        console.println("members");
        for (String member : members) {
            console.println(member);
        }
    }

    private void doGetConfig(String[] args) throws ServiceException {
        String key = args[1];
        Set<String> needAttr = new HashSet<String>();
        needAttr.add(key);
        dumpAttrs(prov.getConfig(key).getAttrs(), needAttr);
    }

    /**
     * prov is always LdapProv here
     */
    private void doGetAllAccounts(LdapProv ldapProv, Domain domain, Server server, final boolean verbose,
            final boolean applyDefault, final Set<String> attrNames) throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
                if (verbose) {
                    dumpAccount((Account) entry, applyDefault, attrNames);
                } else {
                    console.println(entry.getName());
                }
            }
        };

        SearchAccountsOptions options = new SearchAccountsOptions();
        if (domain != null) {
            options.setDomain(domain);
        }
        options.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        if (!applyDefault) {
            options.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        }

        if (server == null) {
            options.setFilter(ZLdapFilterFactory.getInstance().allAccountsOnly());
            ldapProv.searchDirectory(options, visitor);
        } else {
            ldapProv.searchAccountsOnServer(server, options, visitor);
        }
    }

    private void doGetAllAccounts(String[] args) throws ServiceException {

        LdapProv ldapProv = (LdapProv) prov;

        boolean verbose = false;
        boolean applyDefault = true;
        String d = null;
        String s = null;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-e")) {
                applyDefault = false;
            } else if (arg.equals("-s")) {
                i++;
                if (i < args.length) {
                    if (s == null) {
                        s = args[i];
                    } else {
                        console.println("invalid arg: " + args[i] + ", already specified -s with " + s);
                        usage();
                        return;
                    }
                } else {
                    usage();
                    return;
                }
            } else {
                if (d == null) {
                    d = arg;
                } else {
                    console.println("invalid arg: " + arg + ", already specified domain: " + d);
                    usage();
                    return;
                }
            }
            i++;
        }

        if (!applyDefault && !verbose) {
            console.println(ERR_INVALID_ARG_EV);
            usage();
            return;
        }

        Server server = null;
        if (s != null) {
            server = lookupServer(s);
        }
        if (d == null) {
            doGetAllAccounts(ldapProv, null, server, verbose, applyDefault, null);
        } else {
            Domain domain = lookupDomain(d, ldapProv);
            doGetAllAccounts(ldapProv, domain, server, verbose, applyDefault, null);
        }
    }

    private void doSearchAccounts(String[] args) throws ServiceException, ArgException {
        boolean verbose = false;
        int i = 1;

        if (args[i].equals("-v")) {
            verbose = true;
            i++;
            if (args.length < i - 1) {
                usage();
                return;
            }
        }

        if (args.length < i + 1) {
            usage();
            return;
        }

        String query = args[i];

        Map<String, Object> attrs = getMap(args, i + 1);
        String limitStr = (String) attrs.get("limit");
        int limit = limitStr == null ? Integer.MAX_VALUE : Integer.parseInt(limitStr);

        String offsetStr = (String) attrs.get("offset");
        int offset = offsetStr == null ? 0 : Integer.parseInt(offsetStr);

        String sortBy = (String) attrs.get("sortBy");
        String sortAscending = (String) attrs.get("sortAscending");
        boolean isSortAscending = (sortAscending != null) ? "1".equalsIgnoreCase(sortAscending) : true;

        String[] attrsToGet = null;

        String typesStr = (String) attrs.get("types");
        if (typesStr == null) {
            typesStr = SearchDirectoryOptions.ObjectType.accounts.name() + ","
                    + SearchDirectoryOptions.ObjectType.aliases.name() + ","
                    + SearchDirectoryOptions.ObjectType.distributionlists.name() + ","
                    + SearchDirectoryOptions.ObjectType.dynamicgroups.name() + ","
                    + SearchDirectoryOptions.ObjectType.resources.name();
        }

        String domainStr = (String) attrs.get("domain");

        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(attrsToGet);
        if (domainStr != null) {
            Domain d = lookupDomain(domainStr, prov);
            searchOpts.setDomain(d);
        }
        searchOpts.setTypes(typesStr);
        searchOpts.setSortOpt(isSortAscending ? SortOpt.SORT_ASCENDING : SortOpt.SORT_DESCENDING);
        searchOpts.setSortAttr(sortBy);

        // if LdapClient is not initialized(the case for SoapProvisioning), FilterId
        // is not initialized. Use null for SoapProvisioning, it will be set to
        // FilterId.ADMIN_SEARCH in SearchDirectory soap handler.
        FilterId filterId = (prov instanceof LdapProv) ? FilterId.ADMIN_SEARCH : null;
        searchOpts.setFilterString(filterId, query);
        searchOpts.setConvertIDNToAscii(true); // query must be already RFC 2254 escaped

        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);

        for (int j = offset; j < offset + limit && j < accounts.size(); j++) {
            NamedEntry account = accounts.get(j);
            if (verbose) {
                if (account instanceof Account) {
                    dumpAccount((Account) account, true, null);
                } else if (account instanceof Alias) {
                    dumpAlias((Alias) account);
                } else if (account instanceof DistributionList) {
                    dumpGroup((DistributionList) account, null);
                } else if (account instanceof Domain) {
                    dumpDomain((Domain) account, null);
                }
            } else {
                console.println(account.getName());
            }
        }
    }

    private void doSyncGal(String[] args) throws ServiceException {
        String domain = args[1];
        String token = args.length == 3 ? args[2] : "";

        Domain d = lookupDomain(domain);

        SearchGalResult result = null;
        if (prov instanceof LdapProv) {
            GalContact.Visitor visitor = new GalContact.Visitor() {
                @Override
                public void visit(GalContact gc) throws ServiceException {
                    dumpContact(gc);
                }
            };
            result = prov.syncGal(d, token, visitor);
        } else {
            result = ((SoapProvisioning) prov).searchGal(d, "", GalSearchType.all, token, 0, 0, null);
            for (GalContact contact : result.getMatches()) {
                dumpContact(contact);
            }
        }

        if (result.getToken() != null) {
            console.println("\n# token = " + result.getToken() + "\n");
        }
    }

    private void doSearchGal(String[] args) throws ServiceException, ArgException {
        if (args.length < 3) {
            usage();
            return;
        }
        String domain = args[1];
        String query = args[2];
        Map<String, Object> attrs = getMap(args, 3);
        String limitStr = (String) attrs.get("limit");
        int limit = limitStr == null ? 0 : Integer.parseInt(limitStr);
        String offsetStr = (String) attrs.get("offset");
        int offset = offsetStr == null ? 0 : Integer.parseInt(offsetStr);
        String sortBy = (String) attrs.get("sortBy");
        Domain d = lookupDomain(domain);

        SearchGalResult result;

        if (prov instanceof LdapProv) {
            if (offsetStr != null) {
                throw ServiceException.INVALID_REQUEST("offset is not supported with -l", null);
            }

            if (sortBy != null) {
                throw ServiceException.INVALID_REQUEST("sortBy is not supported with -l", null);
            }

            GalContact.Visitor visitor = new GalContact.Visitor() {
                @Override
                public void visit(GalContact gc) throws ServiceException {
                    dumpContact(gc);
                }
            };
            result = prov.searchGal(d, query, GalSearchType.all, limit, visitor);

        } else {
            result = ((SoapProvisioning) prov).searchGal(d, query, GalSearchType.all, null, limit, offset, sortBy);
            for (GalContact contact : result.getMatches()) {
                dumpContact(contact);
            }
        }
    }

    private void doAutoCompleteGal(String[] args) throws ServiceException {
        String domain = args[1];
        String query = args[2];
        int limit = 100;

        Domain d = lookupDomain(domain);

        GalContact.Visitor visitor = new GalContact.Visitor() {
            @Override
            public void visit(GalContact gc) throws ServiceException {
                dumpContact(gc);
            }
        };
        SearchGalResult result = prov.autoCompleteGal(d, query, GalSearchType.all, limit, visitor);
    }

    private void doCountAccount(String[] args) throws ServiceException {
        String domain = args[1];
        Domain d = lookupDomain(domain);

        CountAccountResult result = prov.countAccount(d);
        String formatHeading = "%-20s %-40s %s\n";
        String format = "%-20s %-40s %d\n";
        console.printf(formatHeading, "cos name", "cos id", "# of accounts");
        console.printf(formatHeading, "--------------------", "----------------------------------------",
                "--------------------");
        for (CountAccountResult.CountAccountByCos c : result.getCountAccountByCos()) {
            console.printf(format, c.getCosName(), c.getCosId(), c.getCount());
        }

        console.println();
    }

    private void doCountObjects(String[] args) throws ServiceException {

        CountObjectsType type = CountObjectsType.fromString(args[1]);

        Domain domain = null;
        UCService ucService = null;
        int idx = 2;
        while (args.length > idx) {
            String arg = args[idx];

            if (arg.equals("-d")) {
                if (domain != null) {
                    throw ServiceException.INVALID_REQUEST("domain is already specified as:" + domain.getName(), null);
                }
                idx++;
                if (args.length <= idx) {
                    usage();
                    throw ServiceException.INVALID_REQUEST("expecting domain, not enough args", null);
                }
                domain = lookupDomain(args[idx]);
            } else if (arg.equals("-u")) {
                if (ucService != null) {
                    throw ServiceException.INVALID_REQUEST("UCService is already specified as:" + ucService.getName(),
                            null);
                }
                idx++;
                if (args.length <= idx) {
                    usage();
                    throw ServiceException.INVALID_REQUEST("expecting UCService, not enough args", null);
                }
                ucService = lookupUCService(args[idx]);
            } else {
                usage();
                return;
            }

            idx++;

        }
        long result = prov.countObjects(type, domain, ucService);
        console.println(result);
    }

    private void doGetAllAdminAccounts(String[] args) throws ServiceException {
        boolean verbose = false;
        boolean applyDefault = true;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-e")) {
                applyDefault = false;
            } else {
                break;
            }
            i++;
        }

        if (!applyDefault && !verbose) {
            console.println(ERR_INVALID_ARG_EV);
            usage();
            return;
        }

        List<Account> accounts;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            accounts = soapProv.getAllAdminAccounts(applyDefault);
        } else {
            accounts = prov.getAllAdminAccounts();
        }
        Set<String> attrNames = getArgNameSet(args, i);
        for (Account account : accounts) {
            if (verbose) {
                dumpAccount(account, applyDefault, attrNames);
            } else {
                console.println(account.getName());
            }
        }
    }

    private void doGetAllCos(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        Set<String> attrNames = getArgNameSet(args, verbose ? 2 : 1);
        List<Cos> allcos = prov.getAllCos();
        for (Cos cos : allcos) {
            if (verbose) {
                dumpCos(cos, attrNames);
            } else {
                console.println(cos.getName());
            }
        }
    }

    private void dumpCos(Cos cos, Set<String> attrNames) throws ServiceException {
        console.println("# name " + cos.getName());
        Map<String, Object> attrs = cos.getAttrs();
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void doGetAllDomains(String[] args) throws ServiceException {
        boolean verbose = false;
        boolean applyDefault = true;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-e")) {
                applyDefault = false;
            } else {
                break;
            }
            i++;
        }

        if (!applyDefault && !verbose) {
            console.println(ERR_INVALID_ARG_EV);
            usage();
            return;
        }

        Set<String> attrNames = getArgNameSet(args, i);

        List<Domain> domains;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            domains = soapProv.getAllDomains(applyDefault);
        } else {
            domains = prov.getAllDomains();
        }
        for (Domain domain : domains) {
            if (verbose) {
                dumpDomain(domain, attrNames);
            } else {
                console.println(domain.getName());
            }
        }
    }

    private void dumpDomain(Domain domain, Set<String> attrNames) throws ServiceException {
        dumpDomain(domain, true, attrNames);
    }

    private void dumpDomain(Domain domain, boolean expandConfig, Set<String> attrNames) throws ServiceException {
        console.println("# name " + domain.getName());
        Map<String, Object> attrs = domain.getAttrs(expandConfig);
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void dumpGroup(Group group, Set<String> attrNames) throws ServiceException {

        String[] members;
        if (group instanceof DynamicGroup) {
            members = ((DynamicGroup) group).getAllMembers(true);
        } else {
            members = group.getAllMembers();
        }

        int count = members == null ? 0 : members.length;
        console.println("# distributionList " + group.getName() + " memberCount=" + count);
        Map<String, Object> attrs = group.getAttrs();
        dumpAttrs(attrs, attrNames);
        console.println();
        console.println("members");
        for (String member : members) {
            console.println(member);
        }
    }

    private void dumpAlias(Alias alias) throws ServiceException {
        console.println("# alias " + alias.getName());
        Map<String, Object> attrs = alias.getAttrs();
        dumpAttrs(attrs, null);
    }

    private void doGetRight(String[] args) throws ServiceException, ArgException {
        boolean expandComboRight = false;
        String right = args[1];
        if (args.length > 2) {
            if (args[2].equals("-e")) {
                expandComboRight = true;
            } else {
                throw new ArgException("invalid arguments");
            }
        }
        dumpRight(lookupRight(right), expandComboRight);
    }

    private void doGetAllRights(String[] args) throws ServiceException, ArgException {
        boolean verbose = false;
        String targetType = null;
        String rightClass = null;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-t")) {
                i++;
                if (i == args.length) {
                    throw new ArgException("not enough arguments");
                } else {
                    targetType = args[i];
                }
            } else if (arg.equals("-c")) {
                i++;
                if (i == args.length) {
                    throw new ArgException("not enough arguments");
                } else {
                    rightClass = args[i];
                }
            } else {
                throw new ArgException("invalid arg: " + arg);
            }
            i++;
        }

        List<Right> allRights = prov.getAllRights(targetType, false, rightClass);
        for (Right right : allRights) {
            if (verbose) {
                dumpRight(right);
            } else {
                console.println(right.getName());
            }
        }
    }

    private void dumpRight(Right right) {
        dumpRight(right, true);
    }

    private void dumpRight(Right right, boolean expandComboRight) {
        String tab = "    ";
        String indent = tab;
        String indent2 = indent + indent;

        console.println();
        console.println("------------------------------");

        console.println(right.getName());
        console.println(indent + "      description: " + right.getDesc());
        console.println(indent + "       right type: " + right.getRightType().name());

        String targetType = right.getTargetTypeStr();
        console.println(indent + "   target type(s): " + (targetType == null ? "" : targetType));

        String grantTargetType = right.getGrantTargetTypeStr();
        console.println(indent + "grant target type: " + (grantTargetType == null ? "(default)" : grantTargetType));

        console.println(indent + "      right class: " + right.getRightClass().name());

        if (right.isAttrRight()) {
            AttrRight attrRight = (AttrRight) right;
            console.println();
            console.println(indent + "attributes:");
            if (attrRight.allAttrs()) {
                console.println(indent2 + "all attributes");
            } else {
                for (String attrName : attrRight.getAttrs()) {
                    console.println(indent2 + attrName);
                }
            }
        } else if (right.isComboRight()) {
            ComboRight comboRight = (ComboRight) right;
            console.println();
            console.println(indent + "rights:");
            dumpComboRight(comboRight, expandComboRight, indent, new HashSet<String>());
        }
        console.println();

        Help help = right.getHelp();
        if (help != null) {
            console.println(help.getDesc());
            List<String> helpItems = help.getItems();
            for (String helpItem : helpItems) {
                // console.println(FileGenUtil.wrapComments(helpItem, 70, prefix) + "\n");
                console.println("- " + helpItem.trim());
                console.println();
            }
        }
        console.println();
    }

    private void dumpComboRight(ComboRight comboRight, boolean expandComboRight, String indent, Set<String> seen) {
        // safety check, should not happen,
        // detect circular combo rights
        if (seen.contains(comboRight.getName())) {
            console.println("Circular combo right: " + comboRight.getName() + " !!");
            return;
        }

        String indent2 = indent + indent;

        for (Right r : comboRight.getRights()) {
            String tt = r.getTargetTypeStr();
            tt = tt == null ? "" : " (" + tt + ")";
            // console.format("%s%10.10s: %s %s\n", indent2, r.getRightType().name(), r.getName(), tt);
            console.format("%s %s: %s %s\n", indent2, r.getRightType().name(), r.getName(), tt);

            seen.add(comboRight.getName());

            if (r.isComboRight() && expandComboRight) {
                dumpComboRight((ComboRight) r, expandComboRight, indent2, seen);
            }

            seen.clear();
        }
    }

    private void doGetRightsDoc(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        String[] packages;

        StringBuilder argsDump = new StringBuilder();
        if (args.length > 1) {
            // args[0] is "grd", starting from args[1]
            packages = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                packages[i - 1] = args[i];
                argsDump.append(" " + args[i]);
            }
        } else {
            packages = new String[] { "com.zimbra.cs.service.admin", "com.zimbra.bp", "com.zimbra.cert",
                    "com.zimbra.cs.network", "com.zimbra.cs.network.license.service", "com.zimbra.cs.service.backup",
                    "com.zimbra.cs.service.hsm", "com.zimbra.xmbxsearch" };
        }

        console.println("#");
        console.println("#  Generated by: zmprov grd" + argsDump);
        console.println("#");
        console.println("#  Date: " + DateFormat.getDateInstance(DateFormat.LONG).format(new Date()));
        console.println("# ");
        console.println("#  Packages:");
        for (String pkg : packages) {
            console.println("#       " + pkg);
        }
        console.println("# ");
        console.println("\n");

        Map<String, List<RightsDoc>> allDocs = prov.getRightsDoc(packages);
        for (Map.Entry<String, List<RightsDoc>> docs : allDocs.entrySet()) {
            console.println("========================================");
            console.println("Package: " + docs.getKey());
            console.println("========================================");
            console.println();

            for (RightsDoc doc : docs.getValue()) {
                console.println("------------------------------");
                console.println(doc.getCmd() + "\n");

                console.println("    Related rights:");
                for (String r : doc.getRights()) {
                    console.println("        " + r);
                }
                console.println();
                console.println("    Notes:");
                for (String n : doc.getNotes()) {
                    console.println(FileGenUtil.wrapComments(StringUtil.escapeHtml(n), 70, "        ") + "\n");
                }
                console.println();
            }
        }
    }

    private void doGetAllServers(String[] args) throws ServiceException {
        boolean verbose = false;
        boolean applyDefault = true;
        String service = null;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-e")) {
                applyDefault = false;
            } else {
                if (service == null) {
                    service = arg;
                } else {
                    console.println("invalid arg: " + arg + ", already specified service: " + service);
                    usage();
                    return;
                }
            }
            i++;
        }

        if (!applyDefault && !verbose) {
            console.println(ERR_INVALID_ARG_EV);
            usage();
            return;
        }

        List<Server> servers;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            servers = soapProv.getAllServers(service, applyDefault);
        } else {
            servers = prov.getAllServers(service);
        }
        for (Server server : servers) {
            if (verbose) {
                dumpServer(server, applyDefault, null);
            } else {
                console.println(server.getName());
            }
        }
    }

    private void doGetAllAlwaysOnClusters(String[] args) throws ServiceException {
        boolean verbose = false;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            }
            i++;
        }

        List<AlwaysOnCluster> clusters;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            clusters = soapProv.getAllAlwaysOnClusters();
        } else {
            clusters = prov.getAllAlwaysOnClusters();
        }
        for (AlwaysOnCluster cluster : clusters) {
            if (verbose) {
                dumpAlwaysOnCluster(cluster, null);
            } else {
                console.println(cluster.getName());
            }
        }
    }

    private void doGetAllActiveServers(String[] args) throws ServiceException {
        boolean verbose = false;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            }
            i++;
        }

        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }

        List<Server> servers = ((SoapProvisioning) prov).getAllActiveServers();
        for (Server server : servers) {
            if (verbose) {
                dumpServer(server, true, null);
            } else {
                console.println(server.getName());
            }
        }
    }

    private void doGetAllUCServices(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        Set<String> attrNames = getArgNameSet(args, verbose ? 2 : 1);
        List<UCService> allUCServices = prov.getAllUCServices();
        for (UCService ucService : allUCServices) {
            if (verbose) {
                dumpUCService(ucService, attrNames);
            } else {
                console.println(ucService.getName());
            }
        }
    }

    private void dumpServer(Server server, boolean expandConfig, Set<String> attrNames) throws ServiceException {
        console.println("# name " + server.getName());
        Map<String, Object> attrs = server.getAttrs(expandConfig);
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void dumpAlwaysOnCluster(AlwaysOnCluster cluster, Set<String> attrNames) throws ServiceException {
        console.println("# name " + cluster.getName());
        Map<String, Object> attrs = cluster.getAttrs();
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void dumpUCService(UCService ucService, Set<String> attrNames) throws ServiceException {
        console.println("# name " + ucService.getName());
        Map<String, Object> attrs = ucService.getAttrs();
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void dumpXMPPComponent(XMPPComponent comp, Set<String> attrNames) throws ServiceException {
        console.println("# name " + comp.getName());
        Map<String, Object> attrs = comp.getAttrs();
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void doGetAllXMPPComponents() throws ServiceException {
        List<XMPPComponent> components = prov.getAllXMPPComponents();
        for (XMPPComponent comp : components) {
            dumpXMPPComponent(comp, null);
        }
    }

    private void dumpAccount(Account account, boolean expandCos, Set<String> attrNames) throws ServiceException {
        console.println("# name " + account.getName());
        Map<String, Object> attrs = account.getAttrs(expandCos);
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void dumpCalendarResource(CalendarResource resource, boolean expandCos, Set<String> attrNames)
            throws ServiceException {
        console.println("# name " + resource.getName());
        Map<String, Object> attrs = resource.getAttrs(expandCos);
        dumpAttrs(attrs, attrNames);
        console.println();
    }

    private void dumpContact(GalContact contact) throws ServiceException {
        console.println("# name " + contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        dumpAttrs(attrs, null);
        console.println();
    }

    private void dumpIdentity(Identity identity, Set<String> attrNameSet) throws ServiceException {
        console.println("# name " + identity.getName());
        Map<String, Object> attrs = identity.getAttrs();
        dumpAttrs(attrs, attrNameSet);
        console.println();
    }

    private void dumpSignature(Signature signature, Set<String> attrNameSet) throws ServiceException {
        console.println("# name " + signature.getName());
        Map<String, Object> attrs = signature.getAttrs();
        dumpAttrs(attrs, attrNameSet);
        console.println();
    }

    private void dumpAttrs(Map<String, Object> attrsIn, Set<String> specificAttrs) throws ServiceException {
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        Map<String, Set<String>> specificAttrValues = null;

        if (specificAttrs != null) {
            specificAttrValues = new HashMap<String, Set<String>>();
            for (String specificAttr : specificAttrs) {
                int colonAt = specificAttr.indexOf("=");
                String attrName = null;
                String attrValue = null;
                if (colonAt == -1) {
                    attrName = specificAttr;
                } else {
                    attrName = specificAttr.substring(0, colonAt);
                    attrValue = specificAttr.substring(colonAt + 1);
                    if (attrValue.length() < 1) {
                        throw ServiceException.INVALID_REQUEST("missing value for " + specificAttr, null);
                    }
                }

                attrName = attrName.toLowerCase();
                Set<String> values = specificAttrValues.get(attrName);
                if (values == null) { // haven't seen the attr yet
                    values = new HashSet<String>();
                }
                if (attrValue != null) {
                    values.add(attrValue);
                }
                specificAttrValues.put(attrName, values);
            }
        }

        AttributeManager attrMgr = AttributeManager.getInstance();

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = dateFmt.format(new Date());

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();

            boolean isBinary = needsBinaryIO(attrMgr, name);

            Set<String> specificValues = null;
            if (specificAttrValues != null) {
                specificValues = specificAttrValues.get(name.toLowerCase());
            }
            if (specificAttrValues == null || specificAttrValues.keySet().contains(name.toLowerCase())) {

                Object value = entry.getValue();

                if (value instanceof String[]) {
                    String sv[] = (String[]) value;
                    for (int i = 0; i < sv.length; i++) {
                        String aSv = sv[i];
                        // don't print permission denied attr
                        if (this.forceDisplayAttrValue || aSv.length() > 0
                                && (specificValues == null || specificValues.isEmpty() || specificValues.contains(aSv))) {
                            printAttr(name, aSv, i, isBinary, timestamp);
                        }
                    }
                } else if (value instanceof String) {
                    // don't print permission denied attr
                    if (this.forceDisplayAttrValue || ((String) value).length() > 0
                            && (specificValues == null || specificValues.isEmpty() || specificValues.contains(value))) {
                        printAttr(name, (String) value, null, isBinary, timestamp);
                    }
                }
            }
        }

        // force display empty value attribute
        if (this.forceDisplayAttrValue) {
            for (String attr : specificAttrs) {
                if (!attrs.containsKey(attr)) {
                    AttributeInfo ai = attrMgr.getAttributeInfo(attr);
                    if (ai != null) {
                        printAttr(attr, "", null, false, timestamp);
                    }
                }
            }
        }

    }

    private void doCreateDistributionListsBulk(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String domain = args[1];
            String nameMask = args[2];
            int numAccounts = Integer.parseInt(args[3]);
            for (int i = 0; i < numAccounts; i++) {
                String name = nameMask + Integer.toString(i) + "@" + domain;
                Map<String, Object> attrs = new HashMap<String, Object>();
                String displayName = nameMask + " N. " + Integer.toString(i);
                StringUtil.addToMultiMap(attrs, "displayName", displayName);
                DistributionList dl = prov.createDistributionList(name, attrs);
                console.println(dl.getId());
            }
        }
    }

    private void doGetAllDistributionLists(String[] args) throws ServiceException {
        String d = null;
        boolean verbose = false;
        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else {
                if (d == null) {
                    d = arg;
                } else {
                    console.println("invalid arg: " + arg + ", already specified domain: " + d);
                    usage();
                    return;
                }
            }
            i++;
        }

        if (d == null) {
            List<Domain> domains = prov.getAllDomains();
            for (Domain domain : domains) {
                Collection<?> dls = prov.getAllGroups(domain);
                for (Object obj : dls) {
                    Group dl = (Group) obj;
                    if (verbose) {
                        dumpGroup(dl, null);
                    } else {
                        console.println(dl.getName());
                    }
                }
            }
        } else {
            Domain domain = lookupDomain(d);
            Collection<?> dls = prov.getAllGroups(domain);
            for (Object obj : dls) {
                Group dl = (Group) obj;
                if (verbose) {
                    dumpGroup(dl, null);
                } else {
                    console.println(dl.getName());
                }
            }
        }
    }

    private void doGetAllCalendarResources(String[] args) throws ServiceException {
        boolean verbose = false;
        boolean applyDefault = true;
        String d = null;
        String s = null;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-e")) {
                applyDefault = false;
            } else if (arg.equals("-s")) {
                i++;
                if (i < args.length) {
                    if (s == null) {
                        s = args[i];
                    } else {
                        console.println("invalid arg: " + args[i] + ", already specified -s with " + s);
                        usage();
                        return;
                    }
                } else {
                    usage();
                    return;
                }
            } else {
                if (d == null) {
                    d = arg;
                } else {
                    console.println("invalid arg: " + arg + ", already specified domain: " + d);
                    usage();
                    return;
                }
            }
            i++;
        }

        if (!applyDefault && !verbose) {
            console.println(ERR_INVALID_ARG_EV);
            usage();
            return;
        }

        // always use LDAP
        Provisioning prov = Provisioning.getInstance();

        Server server = null;
        if (s != null) {
            server = lookupServer(s);
        }
        if (d == null) {
            List<Domain> domains = prov.getAllDomains();
            for (Domain domain : domains) {
                doGetAllCalendarResources(prov, domain, server, verbose, applyDefault);
            }
        } else {
            Domain domain = lookupDomain(d, prov);
            doGetAllCalendarResources(prov, domain, server, verbose, applyDefault);
        }
    }

    private void doGetAllCalendarResources(Provisioning prov, Domain domain, Server server, final boolean verbose,
            final boolean applyDefault) throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
                if (verbose) {
                    dumpCalendarResource((CalendarResource) entry, applyDefault, null);
                } else {
                    console.println(entry.getName());
                }
            }
        };
        prov.getAllCalendarResources(domain, server, visitor);
    }

    private void doSearchCalendarResources(String[] args) throws ServiceException {

        boolean verbose = false;
        int i = 1;

        if (args.length < i + 1) {
            usage();
            return;
        }
        if (args[i].equals("-v")) {
            verbose = true;
            i++;
        }
        if (args.length < i + 1) {
            usage();
            return;
        }
        Domain d = lookupDomain(args[i++]);

        if ((args.length - i) % 3 != 0) {
            usage();
            return;
        }

        EntrySearchFilter.Multi multi = new EntrySearchFilter.Multi(false, EntrySearchFilter.AndOr.and);
        for (; i < args.length;) {
            String attr = args[i++];
            String op = args[i++];
            String value = args[i++];
            try {
                EntrySearchFilter.Single single = new EntrySearchFilter.Single(false, attr, op, value);
                multi.add(single);
            } catch (IllegalArgumentException e) {
                printError("Bad search op in: " + attr + " " + op + " '" + value + "'");
                e.printStackTrace();
                usage();
                return;
            }
        }
        EntrySearchFilter filter = new EntrySearchFilter(multi);
        String filterStr = LdapEntrySearchFilter.toLdapCalendarResourcesFilter(filter);

        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions();
        searchOpts.setDomain(d);
        searchOpts.setTypes(ObjectType.resources);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);
        searchOpts.setFilterString(FilterId.ADMIN_SEARCH, filterStr);

        List<NamedEntry> resources = prov.searchDirectory(searchOpts);

        // List<NamedEntry> resources = prov.searchCalendarResources(d, filter, null, null, true);
        for (NamedEntry entry : resources) {
            CalendarResource resource = (CalendarResource) entry;
            if (verbose) {
                dumpCalendarResource(resource, true, null);
            } else {
                console.println(resource.getName());
            }
        }
    }

    private Account lookupAccount(String key, boolean mustFind, boolean applyDefault) throws ServiceException {
        Account account;
        if (applyDefault == true || (prov instanceof LdapProv)) {
            account = prov.getAccount(key);
        } else {
            /*
             * oops, do not apply default, and we are SoapProvisioning
             *
             * This a bit awkward because the applyDefault is controlled at the Entry.getAttrs, not at the provisioning
             * interface. But for SOAP, this needs to be passed to the get(AccountBy) method so it can set the flag in
             * SOAP. We do not want to add a provisioning method for this. Instead, we make it a SOAPProvisioning only
             * method.
             */
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            account = soapProv.getAccount(key, applyDefault);
        }

        if (mustFind && account == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        } else {
            return account;
        }
    }

    private Account lookupAccount(String key) throws ServiceException {
        return lookupAccount(key, true, true);
    }

    private Account lookupAccount(String key, boolean mustFind) throws ServiceException {
        return lookupAccount(key, mustFind, true);
    }

    private CalendarResource lookupCalendarResource(String key) throws ServiceException {
        CalendarResource res = prov.get(guessCalendarResourceBy(key), key);
        if (res == null) {
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(key);
        } else {
            return res;
        }
    }

    private Domain lookupDomain(String key) throws ServiceException {
        return lookupDomain(key, prov);
    }

    private Domain lookupDomain(String key, Provisioning prov) throws ServiceException {
        return lookupDomain(key, prov, true);
    }

    private Domain lookupDomain(String key, Provisioning prov, boolean applyDefault) throws ServiceException {
        Domain domain;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            domain = soapProv.get(guessDomainBy(key), key, applyDefault);
        } else {
            domain = prov.get(guessDomainBy(key), key);
        }
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(key);
        } else {
            return domain;
        }
    }

    private Cos lookupCos(String key) throws ServiceException {
        Cos cos = prov.get(guessCosBy(key), key);
        if (cos == null) {
            throw AccountServiceException.NO_SUCH_COS(key);
        } else {
            return cos;
        }
    }

    private Right lookupRight(String rightName) throws ServiceException {
        return prov.getRight(rightName, false);
    }

    private Server lookupServer(String key) throws ServiceException {
        return lookupServer(key, true);
    }

    private Server lookupServer(String key, boolean applyDefault) throws ServiceException {
        Server server;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            server = soapProv.get(guessServerBy(key), key, applyDefault);
        } else {
            server = prov.get(guessServerBy(key), key);
        }
        if (server == null) {
            throw AccountServiceException.NO_SUCH_SERVER(key);
        } else {
            return server;
        }
    }

    private AlwaysOnCluster lookupAlwaysOnCluster(String key) throws ServiceException {
        AlwaysOnCluster cluster;
        if (prov instanceof SoapProvisioning) {
            SoapProvisioning soapProv = (SoapProvisioning) prov;
            cluster = soapProv.get(guessAlwaysOnClusterBy(key), key);
        } else {
            cluster = prov.get(guessAlwaysOnClusterBy(key), key);
        }
        if (cluster == null) {
            throw AccountServiceException.NO_SUCH_ALWAYSONCLUSTER(key);
        } else {
            return cluster;
        }
    }

    private UCService lookupUCService(String key) throws ServiceException {
        UCService ucService = prov.get(guessUCServiceBy(key), key);
        if (ucService == null) {
            throw AccountServiceException.NO_SUCH_UC_SERVICE(key);
        } else {
            return ucService;
        }
    }

    private String lookupDataSourceId(Account account, String key) throws ServiceException {
        if (Provisioning.isUUID(key)) {
            return key;
        }
        DataSource ds = prov.get(account, Key.DataSourceBy.name, key);
        if (ds == null) {
            throw AccountServiceException.NO_SUCH_DATA_SOURCE(key);
        } else {
            return ds.getId();
        }
    }

    private String lookupSignatureId(Account account, String key) throws ServiceException {
        Signature sig = prov.get(account, guessSignatureBy(key), key);
        if (sig == null) {
            throw AccountServiceException.NO_SUCH_SIGNATURE(key);
        } else {
            return sig.getId();
        }
    }

    private DistributionList lookupDistributionList(String key, boolean mustFind) throws ServiceException {
        DistributionList dl = prov.get(guessDistributionListBy(key), key);
        if (mustFind && dl == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(key);
        } else {
            return dl;
        }
    }

    private DistributionList lookupDistributionList(String key) throws ServiceException {
        return lookupDistributionList(key, true);
    }

    private Group lookupGroup(String key, boolean mustFind) throws ServiceException {
        Group dl = prov.getGroup(guessDistributionListBy(key), key);
        if (mustFind && dl == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(key);
        } else {
            return dl;
        }
    }

    private Group lookupGroup(String key) throws ServiceException {
        return lookupGroup(key, true);
    }

    private XMPPComponent lookupXMPPComponent(String value) throws ServiceException {
        if (Provisioning.isUUID(value)) {
            return prov.get(Key.XMPPComponentBy.id, value);
        } else {
            return prov.get(Key.XMPPComponentBy.name, value);
        }
    }

    public static AccountBy guessAccountBy(String value) {
        if (Provisioning.isUUID(value)) {
            return AccountBy.id;
        }
        return AccountBy.name;
    }

    public static Key.CosBy guessCosBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.CosBy.id;
        }
        return Key.CosBy.name;
    }

    public static Key.DomainBy guessDomainBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.DomainBy.id;
        }
        return Key.DomainBy.name;
    }

    public static Key.ServerBy guessServerBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.ServerBy.id;
        }
        return Key.ServerBy.name;
    }

    public static Key.AlwaysOnClusterBy guessAlwaysOnClusterBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.AlwaysOnClusterBy.id;
        }
        return Key.AlwaysOnClusterBy.name;
    }

    public static Key.UCServiceBy guessUCServiceBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.UCServiceBy.id;
        }
        return Key.UCServiceBy.name;
    }

    public static Key.CalendarResourceBy guessCalendarResourceBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.CalendarResourceBy.id;
        }
        return Key.CalendarResourceBy.name;
    }

    public static Key.DistributionListBy guessDistributionListBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.DistributionListBy.id;
        }
        return Key.DistributionListBy.name;
    }

    public static Key.SignatureBy guessSignatureBy(String value) {
        if (Provisioning.isUUID(value)) {
            return Key.SignatureBy.id;
        }
        return Key.SignatureBy.name;
    }

    public static TargetBy guessTargetBy(String value) {
        if (Provisioning.isUUID(value)) {
            return TargetBy.id;
        }
        return TargetBy.name;
    }

    public static GranteeBy guessGranteeBy(String value) {
        if (Provisioning.isUUID(value)) {
            return GranteeBy.id;
        }
        return GranteeBy.name;
    }

    private void checkDeprecatedAttrs(Map<String, ? extends Object> attrs) throws ServiceException {
        AttributeManager am = AttributeManager.getInstance();
        boolean hadWarnings = false;
        for (String attr : attrs.keySet()) {
            AttributeInfo ai = am.getAttributeInfo(attr);
            if (ai == null) {
                continue;
            }

            if (ai.isDeprecated()) {
                hadWarnings = true;
                console.println("Warn: attribute " + attr + " has been deprecated since " + ai.getDeprecatedSince());
            }
        }

        if (hadWarnings) {
            console.println();
        }
    }

    private static boolean needsBinaryIO(AttributeManager attrMgr, String attr) {
        return attrMgr.containsBinaryData(attr);
    }

    /**
     * get map and check/warn deprecated attrs.
     */
    private Map<String, Object> getMapAndCheck(String[] args, int offset, boolean isCreateCmd) throws ArgException,
            ServiceException {
        Map<String, Object> attrs = getAttrMap(args, offset, isCreateCmd);
        checkDeprecatedAttrs(attrs);
        return attrs;
    }

    /**
     * Convert an array of the form:
     *
     * a1 v1 a2 v2 a2 v3
     *
     * to a map of the form:
     *
     * a1 -> v1 a2 -> [v2, v3]
     *
     * For binary attribute, the argument following an attribute name will be treated as a file path and value for the
     * attribute will be the base64 encoded string of the content of the file.
     */
    private Map<String, Object> keyValueArrayToMultiMap(String[] args, int offset, boolean isCreateCmd)
            throws IOException, ServiceException {
        AttributeManager attrMgr = AttributeManager.getInstance();

        Map<String, Object> attrs = new HashMap<String, Object>();

        String safeguarded_attrs_prop = LC.get("zmprov_safeguarded_attrs");
        Set<String> safeguarded_attrs = safeguarded_attrs_prop == null ? Sets.<String> newHashSet() : Sets
                .newHashSet(safeguarded_attrs_prop.toLowerCase().split(","));
        Multiset<String> multiValAttrsToCheck = HashMultiset.create();

        for (int i = offset; i < args.length; i += 2) {
            String n = args[i];
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("not enough arguments");
            }
            String v = args[i + 1];
            String attrName = n;
            if (n.charAt(0) == '+' || n.charAt(0) == '-') {
                attrName = attrName.substring(1);
            } else if (safeguarded_attrs.contains(attrName.toLowerCase()) && attrMgr.isMultiValued(attrName)) {
                multiValAttrsToCheck.add(attrName.toLowerCase());
            }
            if (needsBinaryIO(attrMgr, attrName) && v.length() > 0) {
                File file = new File(v);
                byte[] bytes = ByteUtil.getContent(file);
                v = ByteUtil.encodeLDAPBase64(bytes);
            }
            StringUtil.addToMultiMap(attrs, n, v);
        }

        if (!allowMultiValuedAttrReplacement && !isCreateCmd) {
            for (Multiset.Entry<String> entry : multiValAttrsToCheck.entrySet()) {
                if (entry.getCount() == 1) {
                    // If multiple values are being assigned to an attr as part of the same command
                    // then we don't consider it an unsafe replacement
                    printError("error: cannot replace multi-valued attr value unless -r is specified");
                    System.exit(2);
                }
            }
        }

        return attrs;
    }

    private Map<String, Object> getAttrMap(String[] args, int offset, boolean isCreateCmd) throws ArgException,
            ServiceException {
        try {
            return keyValueArrayToMultiMap(args, offset, isCreateCmd);
        } catch (IllegalArgumentException iae) {
            throw new ArgException("not enough arguments");
        } catch (IOException ioe) {
            throw ServiceException.INVALID_REQUEST("unable to process arguments", ioe);
        }
    }

    private Map<String, Object> getMap(String[] args, int offset) throws ArgException {
        try {
            return StringUtil.keyValueArrayToMultiMap(args, offset);
        } catch (IllegalArgumentException iae) {
            throw new ArgException("not enough arguments");
        }
    }

    private Set<String> getArgNameSet(String[] args, int offset) {
        if (offset >= args.length) {
            return null;
        }
        Set<String> result = new HashSet<String>();
        for (int i = offset; i < args.length; i++) {
            result.add(args[i].toLowerCase());
        }
        return result;
    }

    private void interactive(BufferedReader in) throws IOException {
        cliReader = in;
        interactiveMode = true;
        while (true) {
            console.print("prov> ");
            String line = StringUtil.readLine(in);
            if (line == null) {
                break;
            }
            if (verboseMode) {
                console.println(line);
            }
            String args[] = StringUtil.parseLine(line);
            if (args.length == 0) {
                continue;
            }
            try {
                if (!execute(args)) {
                    console.println("Unknown command. Type: 'help commands' for a list");
                }
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                errorOccursDuringInteraction = true;
                String errText = "ERROR: "
                        + e.getCode()
                        + " ("
                        + e.getMessage()
                        + ")"
                        + (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage()
                                + ")");
                printError(errText);
                if (verboseMode) {
                    e.printStackTrace(errConsole);
                }
            } catch (ArgException | HttpException e) {
                usage();
            } 
        }
    }

    /**
     * Output binary attribute to file.
     *
     * value is written to: {LC.zmprov_tmp_directory}/{attr-name}[_{index-if-multi-valued}]{timestamp}
     *
     * e.g. /opt/zimbra/data/tmp/zmprov/zimbraFoo_20110202161621 /opt/zimbra/data/tmp/zmprov/zimbraBar_0_20110202161507
     * /opt/zimbra/data/tmp/zmprov/zimbraBar_1_20110202161507
     */
    private void outputBinaryAttrToFile(String attrName, Integer idx, byte[] value, String timestamp)
            throws ServiceException {
        StringBuilder sb = new StringBuilder(LC.zmprov_tmp_directory.value());
        sb.append(File.separator).append(attrName);
        if (idx != null) {
            sb.append("_" + idx);
        }
        sb.append("_" + timestamp);

        File file = new File(sb.toString());
        if (file.exists()) {
            file.delete();
        }

        try {
            FileUtil.ensureDirExists(file.getParentFile());
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to create directory " + file.getParentFile().getAbsolutePath(), e);
        }

        try {
            ByteUtil.putContent(file.getAbsolutePath(), value);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to write to file " + file.getAbsolutePath(), e);
        }
    }

    private void printAttr(String attrName, String value, Integer idx, boolean isBinary, String timestamp)
            throws ServiceException {
        if (isBinary) {
            byte[] binary = ByteUtil.decodeLDAPBase64(value);
            if (outputBinaryToFile()) {
                outputBinaryAttrToFile(attrName, idx, binary, timestamp);
            } else {
                // print base64 encoded content
                // follow ldapsearch notion of using two colons when printing base64 encoded data
                // re-encode into 76 character blocks
                String based64Chunked = new String(Base64.encodeBase64Chunked(binary));
                // strip off the \n at the end
                if (based64Chunked.charAt(based64Chunked.length() - 1) == '\n') {
                    based64Chunked = based64Chunked.substring(0, based64Chunked.length() - 1);
                }
                printOutput(attrName + ":: " + based64Chunked);
            }
        } else {
            printOutput(attrName + ": " + value);
        }
    }

    private static void printError(String text) {
        PrintStream ps = errConsole;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ps, Charsets.UTF_8));
            writer.write(text + "\n");
            writer.flush();
        } catch (IOException e) {
            ps.println(text);
        }
    }

    private static void printOutput(String text) {
        PrintStream ps = console;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ps, Charsets.UTF_8));
            writer.write(text + "\n");
            writer.flush();
        } catch (IOException e) {
            ps.println(text);
        }
    }

    public static void main(String args[]) throws IOException, ServiceException {
        CliUtil.setCliSoapHttpTransportTimeout();
        ZimbraLog.toolSetupLog4jConsole("INFO", true, false); // send all logs to stderr
        SocketFactories.registerProtocols();

        SoapTransport.setDefaultUserAgent("zmprov", BuildInfo.VERSION);

        ProvUtil pu = new ProvUtil();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("h", "help", false, "display usage");
        options.addOption("f", "file", true, "use file as input stream");
        options.addOption("s", "server", true, "host[:port] of server to connect to");
        options.addOption("l", "ldap", false, "provision via LDAP");
        options.addOption("L", "logpropertyfile", true, "log4j property file");
        options.addOption("a", "account", true, "account name (not used with --ldap)");
        options.addOption("p", "password", true, "password for account");
        options.addOption("P", "passfile", true, "filename with password in it");
        options.addOption("z", "zadmin", false, "use zimbra admin name/password from localconfig for account/password");
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption("d", "debug", false, "debug mode (SOAP request and response payload)");
        options.addOption("D", "debughigh", false, "debug mode (SOAP req/resp payload and http headers)");
        options.addOption("m", "master", false, "use LDAP master (has to be used with --ldap)");
        options.addOption("t", "temp", false,
                "write binary values to files in temporary directory specified in localconfig key zmprov_tmp_directory");
        options.addOption("r", "replace", false, "allow replacement of multi-valued attr value");
        options.addOption("fd", "forcedisplay", false, "force display attr value");
        options.addOption(SoapCLI.OPT_AUTHTOKEN);
        options.addOption(SoapCLI.OPT_AUTHTOKENFILE);

        CommandLine cl = null;
        boolean err = false;

        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            printError("error: " + pe.getMessage());
            err = true;
        }

        if (err || cl.hasOption('h')) {
            pu.usage();
        }

        if (cl.hasOption('l') && cl.hasOption('s')) {
            printError("error: cannot specify both -l and -s at the same time");
            System.exit(2);
        }

        pu.setVerbose(cl.hasOption('v'));
        if (cl.hasOption('l')) {
            pu.setUseLdap(true, cl.hasOption('m'));
        }

        if (cl.hasOption('L')) {
            if (cl.hasOption('l')) {
                ZimbraLog.toolSetupLog4j("INFO", cl.getOptionValue('L'));
            } else {
                printError("error: cannot specify -L when -l is not specified");
                System.exit(2);
            }
        }

        if (cl.hasOption('z')) {
            pu.setAccount(LC.zimbra_ldap_user.value());
            pu.setPassword(LC.zimbra_ldap_password.value());
        }

        if (cl.hasOption(SoapCLI.O_AUTHTOKEN) && cl.hasOption(SoapCLI.O_AUTHTOKENFILE)) {
            printError("error: cannot specify " + SoapCLI.O_AUTHTOKEN + " when " + SoapCLI.O_AUTHTOKENFILE
                    + " is specified");
            System.exit(2);
        }
        if (cl.hasOption(SoapCLI.O_AUTHTOKEN)) {
            ZAuthToken zat = ZAuthToken.fromJSONString(cl.getOptionValue(SoapCLI.O_AUTHTOKEN));
            pu.setAuthToken(zat);
        }
        if (cl.hasOption(SoapCLI.O_AUTHTOKENFILE)) {
            String authToken = StringUtil.readSingleLineFromFile(cl.getOptionValue(SoapCLI.O_AUTHTOKENFILE));
            ZAuthToken zat = ZAuthToken.fromJSONString(authToken);
            pu.setAuthToken(zat);
        }

        if (cl.hasOption('s')) {
            pu.setServer(cl.getOptionValue('s'));
        }
        if (cl.hasOption('a')) {
            pu.setAccount(cl.getOptionValue('a'));
        }
        if (cl.hasOption('p')) {
            pu.setPassword(cl.getOptionValue('p'));
        }
        if (cl.hasOption('P')) {
            pu.setPassword(StringUtil.readSingleLineFromFile(cl.getOptionValue('P')));
        }

        if (cl.hasOption('d') && cl.hasOption('D')) {
            printError("error: cannot specify both -d and -D at the same time");
            System.exit(2);
        }
        if (cl.hasOption('D')) {
            pu.setDebug(SoapDebugLevel.high);
        } else if (cl.hasOption('d')) {
            pu.setDebug(SoapDebugLevel.normal);
        }

        if (!pu.useLdap() && cl.hasOption('m')) {
            printError("error: cannot specify -m when -l is not specified");
            System.exit(2);
        }

        if (cl.hasOption('t')) {
            pu.setOutputBinaryToFile(true);
        }

        if (cl.hasOption('r')) {
            pu.setAllowMultiValuedAttrReplacement(true);
        }

        if (cl.hasOption("fd")) {
            pu.setForceDisplayAttrValue(true);
        }

        args = recombineDecapitatedAttrs(cl.getArgs(), options, args);

        try {
            if (args.length < 1) {
                pu.initProvisioning();
                InputStream is = null;
                if (cl.hasOption('f')) {
                    pu.setBatchMode(true);
                    is = new FileInputStream(cl.getOptionValue('f'));
                } else {
                    if (LC.command_line_editing_enabled.booleanValue()) {
                        try {
                            CliUtil.enableCommandLineEditing(LC.zimbra_home.value() + "/.zmprov_history");
                        } catch (IOException e) {
                            errConsole.println("Command line editing will be disabled: " + e);
                            if (pu.verboseMode) {
                                e.printStackTrace(errConsole);
                            }
                        }
                    }

                    // This has to happen last because JLine modifies System.in.
                    is = System.in;
                }
                pu.interactive(new BufferedReader(new InputStreamReader(is, "UTF-8")));
            } else {
                Command cmd = pu.lookupCommand(args[0]);
                if (cmd == null) {
                    pu.usage();
                }
                if (cmd.isDeprecated()) {
                    pu.deprecated();
                }
                if (pu.forceLdapButDontRequireUseLdapOption(cmd)) {
                    pu.setUseLdap(true, false);
                }

                if (pu.needProvisioningInstance(cmd)) {
                    pu.initProvisioning();
                }

                try {
                    if (!pu.execute(args)) {
                        pu.usage();
                    }
                } catch (ArgException | HttpException e) {
                    pu.usage();
                }
            }
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            String errText = "ERROR: " + e.getCode() + " (" + e.getMessage() + ")"
                    + (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")");

            printError(errText);

            if (pu.verboseMode) {
                e.printStackTrace(errConsole);
            }
            System.exit(2);
        }
    }

    class ArgException extends Exception {
        ArgException(String msg) {
            super(msg);
        }
    }

    private static class DescribeArgs {

        enum Field {
            type, // attribute type
            value, // value for enum or regex attributes
            callback, // class name of AttributeCallback object to invoke on changes to attribute.
            immutable, // whether this attribute can be modified directly
            cardinality, // single or multi
            requiredIn, // comma-seperated list containing classes in which this attribute is required
            optionalIn, // comma-seperated list containing classes in which this attribute can appear
            flags, // attribute flags
            defaults, // default value on global config or default COS(for new install) and all upgraded COS's
            min, // min value for integers and durations. defaults to Integer.MIN_VALUE"
            max, // max value for integers and durations, max length for strings/email, defaults to Integer.MAX_VALUE
            id, // leaf OID of the attribute
            requiresRestart, // server(s) need be to restarted after changing this attribute
            since, // version since which the attribute had been introduced
            deprecatedSince; // version since which the attribute had been deprecaed

            static String formatDefaults(AttributeInfo ai) {
                StringBuilder sb = new StringBuilder();
                for (String d : ai.getDefaultCosValues()) {
                    sb.append(d + ",");
                }
                for (String d : ai.getGlobalConfigValues()) {
                    sb.append(d + ",");
                }
                return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1); // trim the ending ,
            }

            static String formatRequiredIn(AttributeInfo ai) {
                Set<AttributeClass> requiredIn = ai.getRequiredIn();
                if (requiredIn == null) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();

                for (AttributeClass ac : requiredIn) {
                    sb.append(ac.name() + ",");
                }
                return sb.substring(0, sb.length() - 1); // trim the ending ,
            }

            static String formatOptionalIn(AttributeInfo ai) {
                Set<AttributeClass> optionalIn = ai.getOptionalIn();
                if (optionalIn == null) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                for (AttributeClass ac : optionalIn) {
                    sb.append(ac.name() + ",");
                }
                return sb.substring(0, sb.length() - 1); // trim the ending ,
            }

            static String formatFlags(AttributeInfo ai) {
                StringBuilder sb = new StringBuilder();
                for (AttributeFlag f : AttributeFlag.values()) {
                    if (ai.hasFlag(f)) {
                        sb.append(f.name() + ",");
                    }
                }
                return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1); // trim the ending ,
            }

            static String formatRequiresRestart(AttributeInfo ai) {
                StringBuilder sb = new StringBuilder();
                List<AttributeServerType> requiresRetstart = ai.getRequiresRestart();
                if (requiresRetstart != null) {
                    for (AttributeServerType ast : requiresRetstart) {
                        sb.append(ast.name() + ",");
                    }
                }
                return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1); // trim the ending ,
            }

            static String print(Field field, AttributeInfo ai) {
                String out = null;

                switch (field) {
                case type:
                    out = ai.getType().getName();
                    break;
                case value:
                    out = ai.getValue();
                    break;
                case callback:
                    AttributeCallback acb = ai.getCallback();
                    if (acb != null) {
                        out = acb.getClass().getSimpleName();
                    }
                    break;
                case immutable:
                    out = Boolean.toString(ai.isImmutable());
                    break;
                case cardinality:
                    AttributeCardinality card = ai.getCardinality();
                    if (card != null) {
                        out = card.name();
                    }
                    break;
                case requiredIn:
                    out = formatRequiredIn(ai);
                    break;
                case optionalIn:
                    out = formatOptionalIn(ai);
                    break;
                case flags:
                    out = formatFlags(ai);
                    break;
                case defaults:
                    out = formatDefaults(ai);
                    break;
                case min:
                    long min = ai.getMin();
                    if (min != Long.MIN_VALUE && min != Integer.MIN_VALUE) {
                        out = Long.toString(min);
                    }
                    break;
                case max:
                    long max = ai.getMax();
                    if (max != Long.MAX_VALUE && max != Integer.MAX_VALUE) {
                        out = Long.toString(max);
                    }
                    break;
                case id:
                    int id = ai.getId();
                    if (id != -1) {
                        out = Integer.toString(ai.getId());
                    }
                    break;
                case requiresRestart:
                    out = formatRequiresRestart(ai);
                    break;
                case since:
                    List<Version> since = ai.getSince();
                    if (since != null) {
                        StringBuilder sb = new StringBuilder();
                        for (Version version : since) {
                            sb.append(version.toString()).append(",");
                        }
                        sb.setLength(sb.length() - 1);
                        out = sb.toString();
                    }
                    break;
                case deprecatedSince:
                    Version depreSince = ai.getDeprecatedSince();
                    if (depreSince != null) {
                        out = depreSince.toString();
                    }
                    break;
                }

                if (out == null) {
                    out = "";
                }
                return out;
            }

        }

        /*
         * args when an object class is specified
         */
        boolean mNonInheritedOnly;
        boolean mOnThisObjectTypeOnly;
        AttributeClass mAttrClass;
        boolean mVerbose;

        /*
         * args when a specific attribute is specified
         */
        String mAttr;
    }

    static String formatLine(int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append("-");
        }
        return sb.toString();
    }

    private String formatAllEntryTypes() {
        StringBuilder sb = new StringBuilder();
        for (AttributeClass ac : AttributeClass.values()) {
            if (ac.isProvisionable()) {
                sb.append(ac.name() + ",");
            }
        }
        return sb.substring(0, sb.length() - 1); // trim the ending ,
    }

    private void descAttrsUsage(Exception e) {
        console.println(e.getMessage() + "\n");

        console.printf("usage:  %s(%s) %s\n", command.getName(), command.getAlias(), command.getHelp());

        console.println();
        console.println("Valid entry types: " + formatAllEntryTypes() + "\n");

        console.println("Examples:");

        console.println("zmprov desc");
        console.println("    print attribute name of all attributes" + "\n");

        console.println("zmprov desc -v");
        console.println("    print attribute name and description of all attributes" + "\n");

        console.println("zmprov desc account");
        console.println("    print attribute name of all account attributes" + "\n");

        console.println("zmprov desc -ni -v account");
        console.println("    print attribute name and description of all non-inherited account attributes, ");
        console.println("    that is, attributes that are on account but not on cos" + "\n");

        console.println("zmprov desc -ni domain");
        console.println("    print attribute name of all non-inherited domain attributes, ");
        console.println("    that is, attributes that are on domain but not on global config" + "\n");

        /*
         * -only is *not* a documented option, we could expose it if we want, handy for engineering tasks, not as useful
         * for users
         *
         * console.println("zmprov desc -only globalConfig");
         * console.println("    print attribute name of all attributes that are on global config only" + "\n");
         */

        console.println("zmprov desc -a zimbraId");
        console.println("    print attribute name, description, and all properties of attribute zimbraId\n");

        console.println("zmprov desc account -a zimbraId");
        console.println("    error: can only specify either an entry type or a specific attribute\n");

        usage();
    }

    private DescribeArgs parseDescribeArgs(String[] args) throws ServiceException {
        DescribeArgs descArgs = new DescribeArgs();

        int i = 1;
        while (i < args.length) {
            if ("-v".equals(args[i])) {
                if (descArgs.mAttr != null) {
                    throw ServiceException.INVALID_REQUEST("cannot specify -v when -a is specified", null);
                }
                descArgs.mVerbose = true;
            } else if (args[i].startsWith("-ni")) {
                if (descArgs.mAttr != null) {
                    throw ServiceException.INVALID_REQUEST("cannot specify -ni when -a is specified", null);
                }
                descArgs.mNonInheritedOnly = true;
            } else if (args[i].startsWith("-only")) {
                if (descArgs.mAttr != null) {
                    throw ServiceException.INVALID_REQUEST("cannot specify -only when -a is specified", null);
                }
                descArgs.mOnThisObjectTypeOnly = true;
            } else if (args[i].startsWith("-a")) {
                if (descArgs.mAttrClass != null) {
                    throw ServiceException.INVALID_REQUEST("cannot specify -a when entry type is specified", null);
                }
                if (descArgs.mAttr != null) {
                    throw ServiceException.INVALID_REQUEST("attribute is already specified as " + descArgs.mAttr, null);
                }
                if (args.length <= i + 1) {
                    throw ServiceException.INVALID_REQUEST("not enough args", null);
                }
                i++;
                descArgs.mAttr = args[i];

            } else {
                if (descArgs.mAttr != null) {
                    throw ServiceException.INVALID_REQUEST("too many args", null);
                }
                if (descArgs.mAttrClass != null) {
                    throw ServiceException.INVALID_REQUEST("entry type is already specified as " + descArgs.mAttrClass,
                            null);
                }
                AttributeClass ac = AttributeClass.fromString(args[i]);
                if (ac == null || !ac.isProvisionable()) {
                    throw ServiceException.INVALID_REQUEST("invalid entry type " + ac.name(), null);
                }
                descArgs.mAttrClass = ac;
            }
            i++;
        }

        if ((descArgs.mNonInheritedOnly == true || descArgs.mOnThisObjectTypeOnly == true)
                && descArgs.mAttrClass == null) {
            throw ServiceException.INVALID_REQUEST("-ni -only must be specified with an entry type", null);
        }

        return descArgs;
    }

    private void doDescribe(String[] args) throws ServiceException {
        DescribeArgs descArgs = null;
        try {
            descArgs = parseDescribeArgs(args);
        } catch (ServiceException e) {
            descAttrsUsage(e);
            return;
        } catch (NumberFormatException e) {
            descAttrsUsage(e);
            return;
        }

        SortedSet<String> attrs = null;
        String specificAttr = null;

        AttributeManager am = AttributeManager.getInstance();

        if (descArgs.mAttr != null) {
            // specific attr
            specificAttr = descArgs.mAttr;
        } else if (descArgs.mAttrClass != null) {
            // attrs in a class
            attrs = new TreeSet<String>(am.getAllAttrsInClass(descArgs.mAttrClass));
            if (descArgs.mNonInheritedOnly) {
                Set<String> inheritFrom = null;
                Set<String> netAttrs = null;
                switch (descArgs.mAttrClass) {
                case account:
                    netAttrs = new HashSet<String>(attrs);
                    inheritFrom = new HashSet<String>(am.getAllAttrsInClass(AttributeClass.cos));
                    netAttrs = SetUtil.subtract(netAttrs, inheritFrom);
                    inheritFrom = new HashSet<String>(am.getAllAttrsInClass(AttributeClass.domain)); // for
                                                                                                     // accountCosDomainInherited
                    netAttrs = SetUtil.subtract(netAttrs, inheritFrom);
                    break;
                case domain:
                case server:
                    netAttrs = new HashSet<String>(attrs);
                    inheritFrom = new HashSet<String>(am.getAllAttrsInClass(AttributeClass.globalConfig));
                    netAttrs = SetUtil.subtract(netAttrs, inheritFrom);
                    break;
                }

                if (netAttrs != null) {
                    attrs = new TreeSet<String>(netAttrs);
                }
            }

            if (descArgs.mOnThisObjectTypeOnly) {
                TreeSet<String> netAttrs = new TreeSet<String>();
                for (String attr : attrs) {
                    AttributeInfo ai = am.getAttributeInfo(attr);
                    if (ai == null) {
                        continue;
                    }
                    Set<AttributeClass> requiredIn = ai.getRequiredIn();
                    Set<AttributeClass> optionalIn = ai.getOptionalIn();
                    if ((requiredIn == null || requiredIn.size() == 1)
                            && (optionalIn == null || optionalIn.size() == 1)) {
                        netAttrs.add(attr);
                    }
                }
                attrs = netAttrs;
            }

        } else {
            //
            // all attrs
            //

            // am.getAllAttrs() only contains attrs with AttributeInfo
            // not extension attrs
            // attrs = new TreeSet<String>(am.getAllAttrs());

            // attr sets for each AttributeClass contain attrs in the extensions, use them
            attrs = new TreeSet<String>();
            for (AttributeClass ac : AttributeClass.values()) {
                attrs.addAll(am.getAllAttrsInClass(ac));
            }
        }

        if (specificAttr != null) {
            AttributeInfo ai = am.getAttributeInfo(specificAttr);
            if (ai == null) {
                console.println("no attribute info for " + specificAttr);
            } else {
                console.println(ai.getName());
                // description
                String desc = ai.getDescription();
                console.println(FileGenUtil.wrapComments((desc == null ? "" : desc), 70, "    "));
                console.println();

                for (DescribeArgs.Field f : DescribeArgs.Field.values()) {
                    console.format("    %15s : %s\n", f.name(), DescribeArgs.Field.print(f, ai));
                }
            }
            console.println();

        } else {
            for (String attr : attrs) {
                AttributeInfo ai = am.getAttributeInfo(attr);
                if (ai == null) {
                    console.println(attr + " (no attribute info)");
                    continue;
                }
                String attrName = ai.getName(); // camel case name
                console.println(attrName);
                if (descArgs.mVerbose) {
                    String desc = ai.getDescription();
                    console.println(FileGenUtil.wrapComments((desc == null ? "" : desc), 70, "    ") + "\n");
                }
            }
        }
    }

    private void doFlushCache(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }

        boolean allServers = false;

        int argIdx = 1;
        if (args[argIdx].equals("-a")) {
            allServers = true;
            if (args.length > 2) {
                argIdx++;
            } else {
                usage();
                return;
            }
        }
        String type = args[argIdx++];

        CacheEntry[] entries = null;

        if (args.length > argIdx) {
            entries = new CacheEntry[args.length - argIdx];
            for (int i = argIdx; i < args.length; i++) {
                Key.CacheEntryBy entryBy;
                if (Provisioning.isUUID(args[i])) {
                    entryBy = Key.CacheEntryBy.id;
                } else {
                    entryBy = Key.CacheEntryBy.name;
                }
                entries[i - argIdx] = new CacheEntry(entryBy, args[i]);
            }
        }

        SoapProvisioning sp = (SoapProvisioning) prov;
        sp.flushCache(type, entries, allServers);
    }

    private void doGenerateDomainPreAuthKey(String[] args) throws ServiceException {
        String key = null;
        boolean force = false;
        if (args.length == 3) {
            if (args[1].equals("-f")) {
                force = true;
            } else {
                usage();
                return;
            }
            key = args[2];
        } else {
            key = args[1];
        }

        Domain domain = lookupDomain(key);
        String curPreAuthKey = domain.getAttr(Provisioning.A_zimbraPreAuthKey);
        if (curPreAuthKey != null && !force) {
            throw ServiceException.INVALID_REQUEST("pre auth key exists for domain " + key
                    + ", use command -f option to force overwriting the existing key", null);
        }
        String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        prov.modifyAttrs(domain, attrs);
        console.printf("preAuthKey: %s\n", preAuthKey);
        if (curPreAuthKey != null) {
            console.printf("previous preAuthKey: %s\n", curPreAuthKey);
        }
    }

    private void doGenerateDomainPreAuth(String[] args) throws ServiceException {
        String key = args[1];
        Domain domain = lookupDomain(key);
        String preAuthKey = domain.getAttr(Provisioning.A_zimbraPreAuthKey, null);
        if (preAuthKey == null) {
            throw ServiceException.INVALID_REQUEST("domain not configured for preauth", null);
        }
        String name = args[2];
        String by = args[3];
        long timestamp = Long.parseLong(args[4]);
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        long expires = Long.parseLong(args[5]);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("account", name);
        params.put("by", by);
        params.put("timestamp", timestamp + "");
        params.put("expires", expires + "");
        if (args.length == 7) {
            params.put("admin", args[6]);
        }
        console.printf("account: %s\nby: %s\ntimestamp: %s\nexpires: %s\npreauth: %s\n", name, by, timestamp, expires,
                PreAuthKey.computePreAuth(params, preAuthKey));
    }

    private void doGetAllMtaAuthURLs() throws ServiceException {
        List<Server> servers = prov.getAllServers();
        for (Server server : servers) {
            boolean isTarget = server.getBooleanAttr(Provisioning.A_zimbraMtaAuthTarget, false);
            if (isTarget) {
                String url = URLUtil.getMtaAuthURL(server) + " ";
                console.print(url);
            }
        }
        console.println();
    }

    private void doGetAllReverseProxyURLs() throws ServiceException {
        String REVERSE_PROXY_PROTO = ""; // don't need proto for nginx.conf
        String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";

        List<Server> servers = prov.getAllMailClientServers();
        for (Server server : servers) {
            int port = server.getIntAttr(Provisioning.A_zimbraExtensionBindPort, 7072);
            boolean isTarget = server.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
            if (isTarget) {
                String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, "");
                console.print(REVERSE_PROXY_PROTO + serviceName + ":" + port + REVERSE_PROXY_PATH + " ");
            }
        }
        console.println();
    }

    private void doGetAllReverseProxyBackends() throws ServiceException {
        List<Server> servers = prov.getAllServers();
        boolean atLeastOne = false;
        for (Server server : servers) {
            boolean isTarget = server.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
            if (!isTarget) {
                continue;
            }

            // (For now) assume HTTP can be load balanced to...
            String mode = server.getAttr(Provisioning.A_zimbraMailMode, null);
            if (mode == null) {
                continue;
            }
            MailMode mailMode = Provisioning.MailMode.fromString(mode);

            boolean isPlain = (mailMode == Provisioning.MailMode.http || (!LC.zimbra_require_interprocess_security
                    .booleanValue() && (mailMode == Provisioning.MailMode.mixed || mailMode == Provisioning.MailMode.both)));

            int backendPort;
            if (isPlain) {
                backendPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
            } else {
                backendPort = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
            }

            String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, "");
            console.println("    server " + serviceName + ":" + backendPort + ";");
            atLeastOne = true;
        }

        if (!atLeastOne) {
            // workaround zmmtaconfig not being able to deal with empty output
            console.println("    server localhost:8080;");
        }
    }

    private void doGetAllReverseProxyDomains() throws ServiceException {

        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                if (entry.getAttr(Provisioning.A_zimbraVirtualHostname) != null
                        && entry.getAttr(Provisioning.A_zimbraSSLPrivateKey) != null
                        && entry.getAttr(Provisioning.A_zimbraSSLCertificate) != null) {
                    StringBuilder virtualHosts = new StringBuilder();
                    for (String vh : entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname)) {
                        virtualHosts.append(vh + " ");
                    }
                    console.println(entry.getName() + " " + virtualHosts);
                }
            }
        };

        prov.getAllDomains(visitor, new String[] { Provisioning.A_zimbraVirtualHostname,
                Provisioning.A_zimbraSSLPrivateKey, Provisioning.A_zimbraSSLCertificate });
    }

    private void doGetAllMemcachedServers() throws ServiceException {
        List<Server> servers = prov.getAllServers(Provisioning.SERVICE_MEMCACHED);
        for (Server server : servers) {
            console.print(server.getAttr(Provisioning.A_zimbraServiceHostname, "") + ":"
                    + server.getAttr(Provisioning.A_zimbraMemcachedBindPort, "") + " ");
        }
        console.println();
    }

    private List<Pair<String /* hostname */, Integer /* port */>> getMailboxServersFromArgs(String[] args)
            throws ServiceException {
        List<Pair<String, Integer>> entries = new ArrayList<Pair<String, Integer>>();
        if (args.length == 2 && "all".equalsIgnoreCase(args[1])) {
            // Get all mailbox servers.
            List<Server> servers = prov.getAllMailClientServers();
            for (Server svr : servers) {
                String host = svr.getAttr(Provisioning.A_zimbraServiceHostname);
                int port = (int) svr.getLongAttr(Provisioning.A_zimbraAdminPort, serverPort);
                Pair<String, Integer> entry = new Pair<String, Integer>(host, port);
                entries.add(entry);
            }
        } else {
            // Only named servers.
            for (int i = 1; i < args.length; ++i) {
                String arg = args[i];
                if (serverHostname.equalsIgnoreCase(arg)) {
                    entries.add(new Pair<String, Integer>(serverHostname, serverPort));
                } else {
                    Server svr = prov.getServerByServiceHostname(arg);
                    if (svr == null) {
                        throw AccountServiceException.NO_SUCH_SERVER(arg);
                    }
                    // TODO: Verify svr has mailbox service enabled.
                    int port = (int) svr.getLongAttr(Provisioning.A_zimbraAdminPort, serverPort);
                    entries.add(new Pair<String, Integer>(arg, port));
                }
            }
        }
        return entries;
    }

    private void doReloadMemcachedClientConfig(String[] args) throws ServiceException {
        List<Pair<String, Integer>> servers = getMailboxServersFromArgs(args);
        // Send command to each server.
        for (Pair<String, Integer> server : servers) {
            String hostname = server.getFirst();
            int port = server.getSecond();
            if (verboseMode) {
                console.print("Updating " + hostname + " ... ");
            }
            boolean success = false;
            try {
                SoapProvisioning sp = new SoapProvisioning();
                sp.soapSetURI(LC.zimbra_admin_service_scheme.value() + hostname + ":" + port
                        + AdminConstants.ADMIN_SERVICE_URI);
                if (debugLevel != SoapDebugLevel.none) {
                    sp.soapSetHttpTransportDebugListener(this);
                }
                if (account != null && password != null) {
                    sp.soapAdminAuthenticate(account, password);
                } else if (authToken != null) {
                    sp.soapAdminAuthenticate(authToken);
                } else {
                    sp.soapZimbraAdminAuthenticate();
                }
                sp.reloadMemcachedClientConfig();
                success = true;
            } catch (ServiceException e) {
                if (verboseMode) {
                    console.println("fail");
                    e.printStackTrace(console);
                } else {
                    console.println("Error updating " + hostname + ": " + e.getMessage());
                }
            } finally {
                if (verboseMode && success) {
                    console.println("ok");
                }
            }
        }
    }

    private void doGetMemcachedClientConfig(String[] args) throws ServiceException {
        List<Pair<String, Integer>> servers = getMailboxServersFromArgs(args);
        // Send command to each server.
        int longestHostname = 0;
        for (Pair<String, Integer> server : servers) {
            String hostname = server.getFirst();
            longestHostname = Math.max(longestHostname, hostname.length());
        }
        String hostnameFormat = String.format("%%-%ds", longestHostname);
        boolean consistent = true;
        String prevConf = null;
        for (Pair<String, Integer> server : servers) {
            String hostname = server.getFirst();
            int port = server.getSecond();
            try {
                SoapProvisioning sp = new SoapProvisioning();
                sp.soapSetURI(LC.zimbra_admin_service_scheme.value() + hostname + ":" + port
                        + AdminConstants.ADMIN_SERVICE_URI);
                if (debugLevel != SoapDebugLevel.none) {
                    sp.soapSetHttpTransportDebugListener(this);
                }
                if (account != null && password != null) {
                    sp.soapAdminAuthenticate(account, password);
                } else if (authToken != null) {
                    sp.soapAdminAuthenticate(authToken);
                } else {
                    sp.soapZimbraAdminAuthenticate();
                }
                MemcachedClientConfig config = sp.getMemcachedClientConfig();
                String serverList = config.serverList != null ? config.serverList : "none";
                if (verboseMode) {
                    console.printf(hostnameFormat
                            + " => serverList=[%s], hashAlgo=%s, binaryProto=%s, expiry=%ds, timeout=%dms\n", hostname,
                            serverList, config.hashAlgorithm, config.binaryProtocol, config.defaultExpirySeconds,
                            config.defaultTimeoutMillis);
                } else if (config.serverList != null) {
                    if (DefaultHashAlgorithm.KETAMA_HASH.toString().equals(config.hashAlgorithm)) {
                        // Don't print the default hash algorithm to keep the output clutter-free.
                        console.printf(hostnameFormat + " => %s\n", hostname, serverList);
                    } else {
                        console.printf(hostnameFormat + " => %s (%S)\n", hostname, serverList, config.hashAlgorithm);
                    }
                } else {
                    console.printf(hostnameFormat + " => none\n", hostname);
                }

                String listAndAlgo = serverList + "/" + config.hashAlgorithm;
                if (prevConf == null) {
                    prevConf = listAndAlgo;
                } else if (!prevConf.equals(listAndAlgo)) {
                    consistent = false;
                }
            } catch (ServiceException e) {
                console.printf(hostnameFormat + " => ERROR: unable to get configuration\n", hostname);
                if (verboseMode) {
                    e.printStackTrace(console);
                }
            }
        }
        if (!consistent) {
            console.println("Inconsistency detected!");
        }
    }

    private void doGetServer(String[] args) throws ServiceException {
        boolean applyDefault = true;

        int i = 1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-e")) {
                applyDefault = false;
            } else {
                break;
            }
            i++;
        }
        if (i >= args.length) {
            usage();
            return;
        }
        dumpServer(lookupServer(args[i], applyDefault), applyDefault, getArgNameSet(args, i + 1));
    }

    private void doGetAlwaysOnCluster(String[] args) throws ServiceException {
        dumpAlwaysOnCluster(lookupAlwaysOnCluster(args[1]), getArgNameSet(args, 2));
    }

    private void doPurgeAccountCalendarCache(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                Account acct = lookupAccount(args[i], true);
                prov.purgeAccountCalendarCache(acct.getId());
            }
        }
    }

    private void doCreateXMPPComponent(String[] args) throws ServiceException, ArgException {
        // 4 = class
        // 5 = category
        // 6 = type
        Map<String, Object> map = getMapAndCheck(args, 7, true);
        map.put(Provisioning.A_zimbraXMPPComponentClassName, args[4]);
        map.put(Provisioning.A_zimbraXMPPComponentCategory, args[5]);
        map.put(Provisioning.A_zimbraXMPPComponentType, args[6]);
        Domain d = lookupDomain(args[2]);
        String routableName = args[1] + "." + d.getName();
        console.println(prov.createXMPPComponent(routableName, lookupDomain(args[2]), lookupServer(args[3]), map));
    }

    private void doGetXMPPComponent(String[] args) throws ServiceException {
        dumpXMPPComponent(lookupXMPPComponent(args[1]), getArgNameSet(args, 2));
    }

    private void doSetServerOffline(String[] args) throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        String key = args[1];
        ((SoapProvisioning) prov).setServerOffline(guessServerBy(key), key);
    }

    private void doSetLocalServerOnline() throws ServiceException {
        if (!(prov instanceof SoapProvisioning)) {
            throwSoapOnly();
        }
        ((SoapProvisioning) prov).setLocalServerOnline();
    }

    static private class RightArgs {
        String mTargetType;
        String mTargetIdOrName;
        String mGranteeType;
        String mGranteeIdOrName;
        String mSecret;
        String mRight;
        RightModifier mRightModifier;

        String[] mArgs;
        int mCurPos = 1;

        RightArgs(String[] args) {
            mArgs = args;
            mCurPos = 1;
        }

        String getNextArg() throws ServiceException {
            if (hasNext()) {
                return mArgs[mCurPos++];
            } else {
                throw ServiceException.INVALID_REQUEST("not enough arguments", null);
            }
        }

        boolean hasNext() {
            return (mCurPos < mArgs.length);
        }
    }

    private void getRightArgsTarget(RightArgs ra) throws ServiceException, ArgException {
        if (ra.mCurPos >= ra.mArgs.length) {
            throw new ArgException("not enough arguments");
        }
        ra.mTargetType = ra.mArgs[ra.mCurPos++];
        TargetType tt = TargetType.fromCode(ra.mTargetType);
        if (tt.needsTargetIdentity()) {
            if (ra.mCurPos >= ra.mArgs.length) {
                throw new ArgException("not enough arguments");
            }
            ra.mTargetIdOrName = ra.mArgs[ra.mCurPos++];
        } else {
            ra.mTargetIdOrName = null;
        }
    }

    private void getRightArgsGrantee(RightArgs ra, boolean needGranteeType, boolean needSecret)
            throws ServiceException, ArgException {
        if (ra.mCurPos >= ra.mArgs.length) {
            throw new ArgException("not enough arguments");
        }
        GranteeType gt = null;
        if (needGranteeType) {
            ra.mGranteeType = ra.mArgs[ra.mCurPos++];
            gt = GranteeType.fromCode(ra.mGranteeType);
        } else {
            ra.mGranteeType = null;
        }
        if (gt == GranteeType.GT_AUTHUSER || gt == GranteeType.GT_PUBLIC) {
            return;
        }
        if (ra.mCurPos >= ra.mArgs.length) {
            throw new ArgException("not enough arguments");
        }
        ra.mGranteeIdOrName = ra.mArgs[ra.mCurPos++];

        if (needSecret && gt != null) {
            if (gt.allowSecret()) {
                if (ra.mCurPos >= ra.mArgs.length) {
                    throw new ArgException("not enough arguments");
                }
                ra.mSecret = ra.mArgs[ra.mCurPos++];
            }
        }
    }

    private void getRightArgsRight(RightArgs ra) throws ServiceException, ArgException {
        if (ra.mCurPos >= ra.mArgs.length) {
            throw new ArgException("not enough arguments");
        }

        ra.mRight = ra.mArgs[ra.mCurPos++];
        ra.mRightModifier = RightModifier.fromChar(ra.mRight.charAt(0));
        if (ra.mRightModifier != null) {
            ra.mRight = ra.mRight.substring(1);
        }
    }

    private void getRightArgs(RightArgs ra, boolean needGranteeType, boolean needSecret) throws ServiceException,
            ArgException {
        getRightArgsTarget(ra);
        getRightArgsGrantee(ra, needGranteeType, needSecret);
        getRightArgsRight(ra);
    }

    private void doCheckRight(String[] args) throws ServiceException, ArgException {
        RightArgs ra = new RightArgs(args);
        getRightArgs(ra, false, false); // todo, handle secret

        Map<String, Object> attrs = getMap(args, ra.mCurPos);

        TargetBy targetBy = (ra.mTargetIdOrName == null) ? null : guessTargetBy(ra.mTargetIdOrName);
        GranteeBy granteeBy = guessGranteeBy(ra.mGranteeIdOrName);

        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        boolean allow = prov.checkRight(ra.mTargetType, targetBy, ra.mTargetIdOrName, granteeBy, ra.mGranteeIdOrName,
                ra.mRight, attrs, via);

        console.println(allow ? "ALLOWED" : "DENIED");
        if (via.available()) {
            console.println("Via:");
            console.println("    target type  : " + via.getTargetType());
            console.println("    target       : " + via.getTargetName());
            console.println("    grantee type : " + via.getGranteeType());
            console.println("    grantee      : " + via.getGranteeName());
            console.println("    right        : " + (via.isNegativeGrant() ? "DENY " : "") + via.getRight());
            console.println();
        }
    }

    private void doGetAllEffectiveRights(String[] args) throws ServiceException, ArgException {
        RightArgs ra = new RightArgs(args);

        if (prov instanceof LdapProv) {
            // must provide grantee info
            getRightArgsGrantee(ra, true, false);
        } else {
            // has more args, use it for the requested grantee
            if (ra.mCurPos < args.length) {
                getRightArgsGrantee(ra, true, false);
            }
        }

        boolean expandSetAttrs = false;
        boolean expandGetAttrs = false;

        // if there are more args, see if they are expandSetAttrs/expandGetAttrs
        for (int i = ra.mCurPos; i < args.length; i++) {
            if ("expandSetAttrs".equals(args[i])) {
                expandSetAttrs = true;
            } else if ("expandGetAttrs".equals(args[i])) {
                expandGetAttrs = true;
            } else {
                throw new ArgException("unrecognized arg: " + args[i]);
            }
        }

        GranteeBy granteeBy = (ra.mGranteeIdOrName == null) ? null : guessGranteeBy(ra.mGranteeIdOrName);

        RightCommand.AllEffectiveRights allEffRights = prov.getAllEffectiveRights(ra.mGranteeType, granteeBy,
                ra.mGranteeIdOrName, expandSetAttrs, expandGetAttrs);

        console.println(allEffRights.granteeType() + " " + allEffRights.granteeName() + "(" + allEffRights.granteeId()
                + ")" + " has the following rights:");

        for (Map.Entry<TargetType, RightCommand.RightsByTargetType> rightsByTargetType : allEffRights
                .rightsByTargetType().entrySet()) {
            RightCommand.RightsByTargetType rbtt = rightsByTargetType.getValue();
            if (!rbtt.hasNoRight()) {
                dumpRightsByTargetType(rightsByTargetType.getKey(), rbtt, expandSetAttrs, expandGetAttrs);
            }
        }
    }

    private void dumpRightsByTargetType(TargetType targetType, RightCommand.RightsByTargetType rbtt,
            boolean expandSetAttrs, boolean expandGetAttrs) {
        console.println("------------------------------------------------------------------");
        console.println("Target type: " + targetType.getCode());
        console.println("------------------------------------------------------------------");

        RightCommand.EffectiveRights er = rbtt.all();
        if (er != null) {
            console.println("On all " + targetType.getPrettyName() + " entries");
            dumpEffectiveRight(er, expandSetAttrs, expandGetAttrs);
        }

        if (rbtt instanceof RightCommand.DomainedRightsByTargetType) {
            RightCommand.DomainedRightsByTargetType domainedRights = (RightCommand.DomainedRightsByTargetType) rbtt;

            for (RightCommand.RightAggregation rightsByDomains : domainedRights.domains()) {
                dumpRightAggregation(targetType, rightsByDomains, true, expandSetAttrs, expandGetAttrs);
            }
        }

        for (RightCommand.RightAggregation rightsByEntries : rbtt.entries()) {
            dumpRightAggregation(targetType, rightsByEntries, false, expandSetAttrs, expandGetAttrs);
        }
    }

    private void dumpRightAggregation(TargetType targetType, RightCommand.RightAggregation rightAggr,
            boolean domainScope, boolean expandSetAttrs, boolean expandGetAttrs) {
        Set<String> entries = rightAggr.entries();
        RightCommand.EffectiveRights er = rightAggr.effectiveRights();

        for (String entry : entries) {
            if (domainScope) {
                console.println("On " + targetType.getCode() + " entries in domain " + entry);
            } else {
                console.println("On " + targetType.getCode() + " " + entry);
            }
        }
        dumpEffectiveRight(er, expandSetAttrs, expandGetAttrs);
    }

    private void doGetEffectiveRights(String[] args) throws ServiceException, ArgException {
        RightArgs ra = new RightArgs(args);
        getRightArgsTarget(ra);

        if (prov instanceof LdapProv) {
            // must provide grantee info
            getRightArgsGrantee(ra, false, false);
        } else {
            // has more args, use it for the requested grantee
            if (ra.mCurPos < args.length) {
                getRightArgsGrantee(ra, false, false);
            }
        }

        boolean expandSetAttrs = false;
        boolean expandGetAttrs = false;

        // if there are more args, see if they are expandSetAttrs/expandGetAttrs
        for (int i = ra.mCurPos; i < args.length; i++) {
            if ("expandSetAttrs".equals(args[i])) {
                expandSetAttrs = true;
            } else if ("expandGetAttrs".equals(args[i])) {
                expandGetAttrs = true;
            } else {
                throw new ArgException("unrecognized arg: " + args[i]);
            }
        }

        TargetBy targetBy = (ra.mTargetIdOrName == null) ? null : guessTargetBy(ra.mTargetIdOrName);
        GranteeBy granteeBy = (ra.mGranteeIdOrName == null) ? null : guessGranteeBy(ra.mGranteeIdOrName);

        RightCommand.EffectiveRights effRights = prov.getEffectiveRights(ra.mTargetType, targetBy, ra.mTargetIdOrName,
                granteeBy, ra.mGranteeIdOrName, expandSetAttrs, expandGetAttrs);

        console.println("Account " + effRights.granteeName() + " has the following rights on target "
                + effRights.targetType() + " " + effRights.targetName());
        dumpEffectiveRight(effRights, expandSetAttrs, expandGetAttrs);
    }

    private void dumpEffectiveRight(RightCommand.EffectiveRights effRights, boolean expandSetAttrs,
            boolean expandGetAttrs) {

        List<String> presetRights = effRights.presetRights();
        if (presetRights != null && presetRights.size() > 0) {
            console.println("================");
            console.println("Preset rights");
            console.println("================");
            for (String r : presetRights) {
                console.println("    " + r);
            }
        }

        displayAttrs("set", expandSetAttrs, effRights.canSetAllAttrs(), effRights.canSetAttrs());
        displayAttrs("get", expandGetAttrs, effRights.canGetAllAttrs(), effRights.canGetAttrs());

        console.println();
        console.println();
    }

    private void displayAttrs(String op, boolean expandAll, boolean allAttrs,
            SortedMap<String, RightCommand.EffectiveAttr> attrs) {
        if (!allAttrs && attrs.isEmpty()) {
            return;
        }
        String format = "    %-50s %-30s\n";
        console.println();
        console.println("=========================");
        console.println(op + " attributes rights");
        console.println("=========================");
        if (allAttrs) {
            console.println("Can " + op + " all attributes");
        }
        if (!allAttrs || expandAll) {
            console.println("Can " + op + " the following attributes");
            console.println("--------------------------------");
            console.printf(format, "attribute", "default");
            console.printf(format, "----------------------------------------", "--------------------");
            for (RightCommand.EffectiveAttr ea : attrs.values()) {
                boolean first = true;
                if (ea.getDefault().isEmpty()) {
                    console.printf(format, ea.getAttrName(), "");
                } else {
                    for (String v : ea.getDefault()) {
                        if (first) {
                            console.printf(format, ea.getAttrName(), v);
                            first = false;
                        } else {
                            console.printf(format, "", v);
                        }
                    }
                }
            }
        }
    }

    /**
     * for testing only, not used in production
     */
    private void doGetCreateObjectAttrs(String[] args) throws ServiceException {
        String targetType = args[1];

        Key.DomainBy domainBy = null;
        String domain = null;
        if (!args[2].equals("null")) {
            domainBy = guessDomainBy(args[2]);
            domain = args[2];
        }

        Key.CosBy cosBy = null;
        String cos = null;
        if (!args[3].equals("null")) {
            cosBy = guessCosBy(args[3]);
            cos = args[3];
        }

        GranteeBy granteeBy = null;
        String grantee = null;

        // take grantee arg only if LdapProv
        // for SoapProvisioning, -a {admin account} -p {password} is required with zmprov
        if (prov instanceof LdapProv) {
            granteeBy = guessGranteeBy(args[4]);
            grantee = args[4];
        }

        console.println("Domain:  " + domain);
        console.println("Cos:     " + cos);
        console.println("Grantee: " + grantee);
        console.println();

        RightCommand.EffectiveRights effRights = prov.getCreateObjectAttrs(targetType, domainBy, domain, cosBy, cos,
                granteeBy, grantee);
        displayAttrs("set", true, effRights.canSetAllAttrs(), effRights.canSetAttrs());
    }

    private void doGetGrants(String[] args) throws ServiceException, ArgException {
        RightArgs ra = new RightArgs(args);

        boolean granteeIncludeGroupsGranteeBelongs = true;

        while (ra.hasNext()) {
            String arg = ra.getNextArg();
            if ("-t".equals(arg)) {
                getRightArgsTarget(ra);
            } else if ("-g".equals(arg)) {
                getRightArgsGrantee(ra, true, false);
                if (ra.hasNext()) {
                    String includeGroups = ra.getNextArg();
                    if ("1".equals(includeGroups)) {
                        granteeIncludeGroupsGranteeBelongs = true;
                    } else if ("0".equals(includeGroups)) {
                        granteeIncludeGroupsGranteeBelongs = false;
                    } else {
                        throw ServiceException.INVALID_REQUEST(
                                "invalid value for the include group flag, must be 0 or 1", null);
                    }
                }
            }
        }

        TargetBy targetBy = (ra.mTargetIdOrName == null) ? null : guessTargetBy(ra.mTargetIdOrName);
        GranteeBy granteeBy = (ra.mGranteeIdOrName == null) ? null : guessGranteeBy(ra.mGranteeIdOrName);

        RightCommand.Grants grants = prov.getGrants(ra.mTargetType, targetBy, ra.mTargetIdOrName, ra.mGranteeType,
                granteeBy, ra.mGranteeIdOrName, granteeIncludeGroupsGranteeBelongs);

        String format = "%-12.12s %-36.36s %-30.30s %-12.12s %-36.36s %-30.30s %s\n";
        console.printf(format, "target type", "target id", "target name", "grantee type", "grantee id", "grantee name",
                "right");
        console.printf(format, "------------", "------------------------------------",
                "------------------------------", "------------", "------------------------------------",
                "------------------------------", "--------------------");

        for (RightCommand.ACE ace : grants.getACEs()) {
            // String deny = ace.deny()?"-":"";
            RightModifier rightModifier = ace.rightModifier();
            String rm = (rightModifier == null) ? "" : String.valueOf(rightModifier.getModifier());
            console.printf(format, ace.targetType(), ace.targetId(), ace.targetName(), ace.granteeType(),
                    ace.granteeId(), ace.granteeName(), rm + ace.right());
        }
        console.println();
    }

    private void doGrantRight(String[] args) throws ServiceException, ArgException {
        RightArgs ra = new RightArgs(args);
        getRightArgs(ra, true, true);

        TargetBy targetBy = (ra.mTargetIdOrName == null) ? null : guessTargetBy(ra.mTargetIdOrName);
        GranteeBy granteeBy = (ra.mGranteeIdOrName == null) ? null : guessGranteeBy(ra.mGranteeIdOrName);

        prov.grantRight(ra.mTargetType, targetBy, ra.mTargetIdOrName, ra.mGranteeType, granteeBy, ra.mGranteeIdOrName,
                ra.mSecret, ra.mRight, ra.mRightModifier);
    }

    private void doRevokeRight(String[] args) throws ServiceException, ArgException {
        RightArgs ra = new RightArgs(args);
        getRightArgs(ra, true, false);

        TargetBy targetBy = (ra.mTargetIdOrName == null) ? null : guessTargetBy(ra.mTargetIdOrName);
        GranteeBy granteeBy = (ra.mGranteeIdOrName == null) ? null : guessGranteeBy(ra.mGranteeIdOrName);

        prov.revokeRight(ra.mTargetType, targetBy, ra.mTargetIdOrName, ra.mGranteeType, granteeBy, ra.mGranteeIdOrName,
                ra.mRight, ra.mRightModifier);
    }

    private void doGetAuthTokenInfo(String[] args) {
        String authToken = args[1];

        try {
            Map attrs = AuthToken.getInfo(authToken);
            List keys = new ArrayList(attrs.keySet());
            Collections.sort(keys);

            for (Object k : keys) {
                String key = k.toString();
                String value = attrs.get(k).toString();

                if ("exp".equals(key)) {
                    long exp = Long.parseLong(value);
                    console.format("%s: %s (%s)\n", key, value, DateUtil.toRFC822Date(new Date(exp)));
                } else {
                    console.format("%s: %s\n", key, value);
                }
            }
        } catch (AuthTokenException e) {
            console.println("Unable to parse auth token: " + e.getMessage());
        }

        console.println();
    }

    private void doUpdatePresenceSessionId(String[] args) throws ServiceException {

        String idOrName = args[1];
        String username = args[2];
        String password = args[3];

        UCService ucService = lookupUCService(idOrName);

        /*
         * soap only
         */
        String newSessionId = prov.updatePresenceSessionId(ucService.getId(), username, password);
        console.println(newSessionId);
    }

    private void doGetAllFreeBusyProviders() throws ServiceException, IOException, HttpException {
        FbCli fbcli = new FbCli();
        for (FbCli.FbProvider fbprov : fbcli.getAllFreeBusyProviders()) {
            console.println(fbprov.toString());
        }
    }

    private void doGetFreeBusyQueueInfo(String[] args) throws ServiceException, IOException, HttpException {
        FbCli fbcli = new FbCli();
        String name = null;
        if (args.length > 1) {
            name = args[1];
        }
        for (FbCli.FbQueue fbqueue : fbcli.getFreeBusyQueueInfo(name)) {
            console.println(fbqueue.toString());
        }
    }

    private void doPushFreeBusy(String[] args) throws ServiceException, IOException, HttpException {
        FbCli fbcli = new FbCli();
        Map<String, HashSet<String>> accountMap = new HashMap<String, HashSet<String>>();
        for (int i = 1; i < args.length; i++) {
            String acct = args[i];
            Account account = prov.getAccountById(acct);
            if (account == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(acct);
            }
            String host = account.getMailHost();
            HashSet<String> accountSet = accountMap.get(host);
            if (accountSet == null) {
                accountSet = new HashSet<String>();
                accountMap.put(host, accountSet);
            }
            accountSet.add(acct);
        }
        for (String host : accountMap.keySet()) {
            console.println("pushing to server " + host);
            fbcli.setServer(host);
            fbcli.pushFreeBusyForAccounts(accountMap.get(host));
        }
    }

    private void doPushFreeBusyForDomain(String[] args) throws ServiceException, IOException, HttpException {
        lookupDomain(args[1]);
        FbCli fbcli = new FbCli();
        for (Server server : prov.getAllMailClientServers()) {
            console.println("pushing to server " + server.getName());
            fbcli.setServer(server.getName());
            fbcli.pushFreeBusyForDomain(args[1]);
        }
    }

    private void doPurgeFreeBusyQueue(String[] args) throws ServiceException, IOException, HttpException {
        String provider = null;
        if (args.length > 1) {
            provider = args[1];
        }
        FbCli fbcli = new FbCli();
        fbcli.purgeFreeBusyQueue(provider);
    }

    private void dumpSMIMEConfigs(Map<String, Map<String, Object>> smimeConfigs) throws ServiceException {
        for (Map.Entry<String, Map<String, Object>> smimeConfig : smimeConfigs.entrySet()) {
            String configName = smimeConfig.getKey();
            Map<String, Object> configAttrs = smimeConfig.getValue();

            console.println("# name " + configName);
            dumpAttrs(configAttrs, null);
            console.println();
        }
    }

    private void doGetConfigSMIMEConfig(String[] args) throws ServiceException {
        String configName = null;
        if (args.length > 1) {
            configName = args[1];
        }

        Map<String, Map<String, Object>> smimeConfigs = prov.getConfigSMIMEConfig(configName);
        dumpSMIMEConfigs(smimeConfigs);
    }

    private void doGetDomainSMIMEConfig(String[] args) throws ServiceException {
        String domainName = args[1];
        Domain domain = lookupDomain(domainName);

        String configName = null;
        if (args.length > 2) {
            configName = args[2];
        }

        Map<String, Map<String, Object>> smimeConfigs = prov.getDomainSMIMEConfig(domain, configName);
        dumpSMIMEConfigs(smimeConfigs);
    }

    private void doModifyConfigSMIMEConfig(String[] args) throws ServiceException, ArgException {
        String configName = args[1];
        prov.modifyConfigSMIMEConfig(configName, getMapAndCheck(args, 2, false));
    }

    private void doModifyDomainSMIMEConfig(String[] args) throws ServiceException, ArgException {
        String domainName = args[1];
        Domain domain = lookupDomain(domainName);

        String configName = args[2];
        prov.modifyDomainSMIMEConfig(domain, configName, getMapAndCheck(args, 3, false));
    }

    private void doRemoveConfigSMIMEConfig(String[] args) throws ServiceException {
        String configName = null;
        if (args.length > 1) {
            configName = args[1];
        }

        prov.removeConfigSMIMEConfig(configName);
    }

    private void doRemoveDomainSMIMEConfig(String[] args) throws ServiceException {
        String domainName = args[1];
        Domain domain = lookupDomain(domainName);

        String configName = null;
        if (args.length > 2) {
            configName = args[2];
        }

        prov.removeDomainSMIMEConfig(domain, configName);
    }

    private void doHelp(String[] args) {
        Category cat = null;
        if (args != null && args.length >= 2) {
            String s = args[1].toUpperCase();
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

        if (args == null || args.length == 1 || cat == null) {
            console.println(" zmprov is used for provisioning. Try:");
            console.println("");
            for (Category c : Category.values()) {
                console.printf("     zmprov help %-15s %s\n", c.name().toLowerCase(), c.getDescription());
            }

        }

        if (cat != null) {
            console.println("");
            for (Command c : Command.values()) {
                if (!c.hasHelp()) {
                    continue;
                }
                if (cat == Category.COMMANDS || cat == c.getCategory()) {
                    Command.Via via = c.getVia();
                    console.printf("  %s(%s) %s\n", c.getName(), c.getAlias(), c.getHelp());
                    if (via == Command.Via.ldap) {
                        console.printf("    -- NOTE: %s can only be used with \"zmprov -l/--ldap\"\n", c.getName());
                    }
                    console.printf("\n");
                }
            }

            Category.help(cat);
        }
        console.println();
    }

    @Override
    public void receiveSoapMessage(HttpPost postMethod, Element envelope) {
        console.printf("======== SOAP RECEIVE =========\n");

        if (debugLevel == SoapDebugLevel.high) {
            Header[] headers = postMethod.getAllHeaders();
            for (Header header : headers) {
                console.println(header.toString().trim()); // trim the ending crlf
            }
        }

        long end = System.currentTimeMillis();
        console.println(envelope.prettyPrint());
        console.printf("=============================== (%d msecs)\n", end - sendStart);
    }

    @Override
    public void sendSoapMessage(HttpPost postMethod, Element envelope, BasicCookieStore httpState) {
        console.println("========== SOAP SEND ==========");

        if (debugLevel == SoapDebugLevel.high) {
            
                URI uri = postMethod.getURI();
                console.println(uri.toString());
                Header[] headers = postMethod.getAllHeaders();
            for (Header header : headers) {
                console.println(header.toString().trim()); // trim the ending crlf
            }
            console.println();
        }

        sendStart = System.currentTimeMillis();

        console.println(envelope.prettyPrint());
        console.println("===============================");
    }

    private void throwSoapOnly() throws ServiceException {
        throw ServiceException.INVALID_REQUEST(ERR_VIA_SOAP_ONLY, null);
    }

    private void throwLdapOnly() throws ServiceException {
        throw ServiceException.INVALID_REQUEST(ERR_VIA_LDAP_ONLY, null);
    }

    private void loadLdapSchemaExtensionAttrs() {
        if (prov instanceof LdapProv) {
            AttributeManager.loadLdapSchemaExtensionAttrs((LdapProv) prov);
        }
    }

    /**
     * To remove a particular instance of an attribute, the prefix indicator '-' is used before the attribute name. When
     * the attribute name is started with one of the valid command arguments, such as -z or -a, the parser mistakenly
     * divides it into two parts instead of treating as one parameter of the '-' and attribute name.
     * <p>
     * This method detects such decapitated attribute, and recombines those two into one attribute name with '-'.
     *
     * @param parsedArgs
     *            [cmd-args] which are parsed by PosixParser
     * @param options
     *            set of valid [args]
     * @param args
     * @throws ServiceException
     */
    static private String[] recombineDecapitatedAttrs(String[] parsedArgs, Options options, String[] orgArgs)
            throws ServiceException {
        List<String> newArgs = new ArrayList<String>(parsedArgs.length);
        String headStr = null;
        for (int i = 0; i < parsedArgs.length; i++) {
            String arg = parsedArgs[i];
            if (arg.startsWith("-") && arg.length() == 2 && options.hasOption(arg)) {
                // Detect legitimate POSIX style parameters even after operation command;
                // such as "zmprov describe -a <attr>"
                if (i < parsedArgs.length - 1) {
                    boolean missParsed = false;
                    String tmpParam = arg + parsedArgs[i + 1];
                    for (String orgArg : orgArgs) {
                        if (orgArg.equals(tmpParam)) {
                            missParsed = true;
                            break;
                        }
                    }
                    if (missParsed) {
                        headStr = arg;
                    } else {
                        newArgs.add(arg);
                    }
                } else {
                    newArgs.add(arg);
                }
            } else if (headStr != null) {
                newArgs.add(headStr + arg);
                headStr = null;
            } else {
                newArgs.add(arg);
            }
        }
        return newArgs.toArray(new String[newArgs.size()]);
    }

    private void doDeleteDistributionList(String[] args) throws ServiceException {
        String groupId = lookupGroup(args[1]).getId();
        Boolean cascadeDelete = false;
        if (args.length > 2) {
            cascadeDelete = Boolean.valueOf(args[2]) != null ? Boolean.valueOf(args[2]) : false;
        }
        prov.deleteGroup(groupId, cascadeDelete);
    }
}

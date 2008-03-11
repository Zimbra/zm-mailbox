/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.calendar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.mailbox.calendar.tzfixup.XmlFixupRules;
import com.zimbra.cs.util.SoapCLI;

public class FixCalendarTZUtil extends SoapCLI {

    protected static final String O_RULEFILE = "rulefile";
    protected static final String O_ACCOUNT = "a";
    protected static final String O_AFTER = "after";
    protected static final String O_SYNC = "sync";

    protected void setupCommandLineOptions() {
        super.setupCommandLineOptions();
        Options options = getOptions();
        Option rulefileOpt = new Option(null, O_RULEFILE, true, "xml file containing fixup rules");
        rulefileOpt.setRequired(true);
        options.addOption(rulefileOpt);
        Option accountOpt = new Option(O_ACCOUNT, "account", true,
                "account email addresses seperated by white space or \"all\" for all accounts");
        accountOpt.setRequired(true);
        accountOpt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(accountOpt);
        options.addOption(new Option(null, O_AFTER, true,
                "fixup calendar items after this time; defaults to beginning of 2007"));
        options.addOption(new Option(null, O_SYNC, false,
                "run synchronously; default is asynchronous"));
    }

    protected String getCommandUsage() {
        return "zmfixtz --rulefile <rule file> -a <account(s)> [options]";
    }

    public FixCalendarTZUtil() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    public static void main(String[] args) {
        CliUtil.toolSetup();
        FixCalendarTZUtil util = null;
        try {
            util = new FixCalendarTZUtil();
        } catch (ServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        try {
            CommandLine cl = util.getCommandLine(args);
            if (cl == null)
                return;
            if (!cl.hasOption(O_RULEFILE))
                throw new ParseException("Missing required option --" + O_RULEFILE);
            String after = null;
            if (cl.hasOption(O_AFTER))
                after = cl.getOptionValue(O_AFTER);
            
            util.doit(cl.getOptionValue(O_RULEFILE), cl.getOptionValues(O_ACCOUNT), after, cl.hasOption(O_SYNC));
            System.exit(0);
        } catch (ParseException e) {
            util.usage(e);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            util.usage(null);
        }
        System.exit(1);
    }

    private static Element parseRuleFile(String ruleFilePath)
    throws IOException, DocumentException, ServiceException {
        File ruleFile = new File(ruleFilePath);
        if (!ruleFile.exists())
            throw new IOException("Missing rule file " + ruleFile.getAbsolutePath());

        String docStr = new String(ByteUtil.getContent(ruleFile), "utf-8");
        Element tzfixupElem = Element.parseXML(docStr);
        tzfixupElem.detach();
        XmlFixupRules.parseTzFixup(tzfixupElem);  // parse it to make sure it's good
        return tzfixupElem;
    }

    private void doit(String ruleFilePath, String[] accts, String after, boolean sync)
    throws SoapFaultException, IOException, DocumentException, ServiceException {
        Element req = new Element.XMLElement(AdminConstants.FIX_CALENDAR_TZ_REQUEST);

        Element tzfixupElem = parseRuleFile(ruleFilePath);
        req.addElement(tzfixupElem);

        if (accts == null || accts.length == 0)
            throw ServiceException.INVALID_REQUEST("Missing -" + O_ACCOUNT + " option", null);
        for (String acct : accts) {
            Element acctElem = req.addElement(AdminConstants.E_ACCOUNT);
            acctElem.addAttribute(AdminConstants.A_NAME, acct);
        }

        if (after != null) {
            Date afterTime = parseDatetime(after);
            if (afterTime != null) {
                SimpleDateFormat f = new SimpleDateFormat(CANONICAL_DATETIME_FORMAT);
                String tstamp = f.format(afterTime);
                System.out.printf("using cutoff time of %s\n", tstamp);
                req.addAttribute(AdminConstants.A_TZFIXUP_AFTER, afterTime.getTime());
            } else {
                System.err.printf("Invalid timestamp \"%s\" specified for --%s option\n",
                                  after, O_AFTER);
                System.err.println();
                System.err.print(getAllowedDatetimeFormatsHelp());
                System.exit(1);
            }
        }

        if (sync)
            req.addAttribute(AdminConstants.A_TZFIXUP_SYNC, true);

        auth();
        getTransport().invokeWithoutSession(req);
    }
}

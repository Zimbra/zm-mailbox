/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;

public class FixCalendarTimeZoneUtil extends SoapCLI {

    protected static final String O_ACCOUNT = "a";
    protected static final String O_AFTER = "after";
    protected static final String O_COUNTRY = "country";
    protected static final String O_SYNC = "sync";

    protected void setupCommandLineOptions() {
        super.setupCommandLineOptions();
        Options options = getOptions();
        Option accountOpt = new Option(O_ACCOUNT, "account", true,
                "account email addresses seperated by white space or \"all\" for all accounts");
        accountOpt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(accountOpt);
        options.addOption(new Option(null, O_AFTER, true,
                "fixup calendar items after this time; defaults to beginning of 2007"));
        options.addOption(new Option(null, O_COUNTRY, true,
                "two-letter country code if running country-specific fixup"));
        options.addOption(new Option(null, O_SYNC, false,
                "run synchronously; default is asynchronous"));
    }

    protected String getCommandUsage() {
        return "zmfixtz -a <account(s)> [options]";
    }

    public FixCalendarTimeZoneUtil() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    public static void main(String[] args) {
        Zimbra.toolSetup();
        FixCalendarTimeZoneUtil util = null;
        try {
            util = new FixCalendarTimeZoneUtil();
        } catch (ServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        try {
            CommandLine cl = util.getCommandLine(args);
            if (cl == null)
                return;
            String after = null;
            if (cl.hasOption(O_AFTER))
                after = cl.getOptionValue(O_AFTER);
            String country = null;
            if (cl.hasOption(O_COUNTRY))
                country = cl.getOptionValue(O_COUNTRY);
            util.doit(cl.getOptionValues(O_ACCOUNT), after, country, cl.hasOption(O_SYNC));
            System.exit(0);
        } catch (ParseException e) {
            util.usage(e);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            util.usage(null);
        }
        System.exit(1);
    }

    private void doit(String[] accts, String after, String country, boolean sync)
    throws SoapFaultException, IOException, ServiceException {
        Element req = new Element.XMLElement(AdminService.FIX_CALENDAR_TIME_ZONE_REQUEST);
        if (accts == null || accts.length == 0)
            throw ServiceException.INVALID_REQUEST("Missing -" + O_ACCOUNT + " option", null);
        for (String acct : accts) {
            Element acctElem = req.addElement(AdminService.E_ACCOUNT);
            acctElem.addAttribute(AdminService.A_NAME, acct);
        }
        if (after != null) {
            Date afterTime = parseDatetime(after);
            if (afterTime != null) {
                SimpleDateFormat f = new SimpleDateFormat(CANONICAL_DATETIME_FORMAT);
                String tstamp = f.format(afterTime);
                System.out.printf("using cutoff time of %s\n", tstamp);
                req.addAttribute(AdminService.A_TZFIXUP_AFTER, afterTime.getTime());
            } else {
                System.err.printf("Invalid timestamp \"%s\" specified for --%s option\n",
                                  after, O_AFTER);
                System.err.println();
                printAllowedDatetimeFormats(System.err);
                System.exit(1);
            }
        }
        if (country != null)
            req.addAttribute(AdminService.A_COUNTRY, country);
        if (sync)
            req.addAttribute(AdminService.A_TZFIXUP_SYNC, true);

        auth();
        getTransport().invokeWithoutSession(req);
    }
}

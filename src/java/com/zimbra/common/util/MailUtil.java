/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException.Argument;

public class MailUtil {
    public static void populateFailureDeliveryMessageFields(MimeMessage failedDeliverymm, String subject, String to, Argument[] addressArgs) throws MessagingException, UnsupportedEncodingException {		
        failedDeliverymm.setSubject("Send Partial Failure Notice");
        failedDeliverymm.setSentDate(new Date());
        failedDeliverymm.setFrom(new InternetAddress("donotreply@host.local", "Failed Delivery Notifier"));
        failedDeliverymm.setRecipient(RecipientType.TO, new JavaMailInternetAddress(to));

        StringBuilder text = new StringBuilder();
        String sentDate = new SimpleDateFormat("MMMMM d, yyyy").format(new Date());
        String sentTime = new SimpleDateFormat("h:mm a").format(new Date());

        text.append("Your message \"" + subject + "\" sent on " + sentDate + " at " + sentTime + " " + TimeZone.getDefault().getDisplayName() + " " +
                "could not be delivered to one or more recipients.\n\n" +
                "For further assistance, please send mail to postmaster.\n\n");

        List<String> invalidAddrs = new ArrayList<String> ();
        List<String> unsentAddrs = new ArrayList<String> ();

        if (addressArgs != null) {
            for (Argument arg : addressArgs) {
                if (arg.mName != null && arg.mValue != null && arg.mValue.length() > 0) {
                    if (arg.mName.equals("invalid"))
                        invalidAddrs.add(arg.mValue);
                    else if (arg.mName.equals("unsent"))
                        unsentAddrs.add(arg.mValue);
                }
            }
        }

        for (String addr : invalidAddrs)
            text.append(addr + ":" + "Invalid Address\n");

        for (String addr : unsentAddrs)
            text.append(addr + ":" + "Unsent Address\n");

        failedDeliverymm.setText(text.toString());
        failedDeliverymm.saveChanges();
    }
}
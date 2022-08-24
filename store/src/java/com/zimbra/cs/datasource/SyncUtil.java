/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.datasource;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.util.JMSession;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class SyncUtil {
    private static final Flags EMPTY_FLAGS = new Flags();

    // Excludes non-IMAP related Zimbra flags
    private static int IMAP_FLAGS_BITMASK =
         Flag.BITMASK_REPLIED | Flag.BITMASK_DELETED |
         Flag.BITMASK_DRAFT | Flag.BITMASK_FLAGGED | Flag.BITMASK_UNREAD;

    private SyncUtil() {
    }

    public static int imapToZimbraFlags(Flags flags) {
        int zflags = 0;
        if (flags.isAnswered()) zflags |= Flag.BITMASK_REPLIED;
        if (flags.isDeleted())  zflags |= Flag.BITMASK_DELETED;
        if (flags.isDraft())    zflags |= Flag.BITMASK_DRAFT;
        if (flags.isFlagged())  zflags |= Flag.BITMASK_FLAGGED;
        if (!flags.isSeen())    zflags |= Flag.BITMASK_UNREAD;
        return zflags;
    }

    public static Flags zimbraToImapFlags(int zflags) {
        return getFlagsToAdd(EMPTY_FLAGS, zflags);
    }

    public static int imapFlagsOnly(int zflags) {
        return zflags & IMAP_FLAGS_BITMASK;
    }

    public static Flags getFlagsToAdd(Flags flags, int zflags) {
        Flags toAdd = new Flags();
        if (!flags.isAnswered() && (zflags & Flag.BITMASK_REPLIED) != 0) {
            toAdd.set(CAtom.F_ANSWERED.atom());
        }
        if (!flags.isDeleted() && (zflags & Flag.BITMASK_DELETED) != 0) {
            toAdd.set(CAtom.F_DELETED.atom());
        }
        if (!flags.isDraft() && (zflags & Flag.BITMASK_DRAFT) != 0) {
            toAdd.set(CAtom.F_DRAFT.atom());
        }
        if (!flags.isFlagged() && (zflags & Flag.BITMASK_FLAGGED) != 0) {
            toAdd.set(CAtom.F_FLAGGED.atom());
        }
        if (!flags.isSeen() && (zflags & Flag.BITMASK_UNREAD) == 0) {
            toAdd.set(CAtom.F_SEEN.atom());
        }
        return toAdd;
    }

    public static Flags getFlagsToRemove(Flags flags, int zflags) {
        Flags toRemove = new Flags();
        if (flags.isAnswered() && (zflags & Flag.BITMASK_REPLIED) == 0) {
            toRemove.set(CAtom.F_ANSWERED.atom());
        }
        if (flags.isDeleted() && (zflags & Flag.BITMASK_DELETED) == 0) {
            toRemove.set(CAtom.F_DELETED.atom());
        }
        if (flags.isDraft() && (zflags & Flag.BITMASK_DRAFT) == 0) {
            toRemove.set(CAtom.F_DRAFT.atom());
        }
        if (flags.isFlagged() && (zflags & Flag.BITMASK_FLAGGED) == 0) {
            toRemove.set(CAtom.F_FLAGGED.atom());
        }
        if (flags.isSeen() && (zflags & Flag.BITMASK_UNREAD) != 0) {
            toRemove.set(CAtom.F_SEEN.atom());
        }
        return toRemove;
    }

    public static Date getInternalDate(Message msg, MimeMessage mm) {
        Date date = null;
        try {
            date = mm.getReceivedDate();
        } catch (MessagingException e) {
            // Fall through
        }
        return date != null ? date : new Date(msg.getDate());
    }

    public static void setSyncEnabled(Mailbox mbox, int folderId, boolean enabled) throws ServiceException {
        mbox.alterTag(new OperationContext(mbox), folderId, MailItem.Type.FOLDER, Flag.FlagInfo.SYNC, enabled, null);
    }

    public static Log getTraceLogger(Log parent, String id) {
        String category = parent.getCategory();
        Log log = LogFactory.getLog(category + '.' + id + '.' + category);
        log.setLevel(Log.Level.trace);
        return log;
    }

    public static boolean sendReportEmail(String subject, String text, String[] toStrs) {
        String fromStr;
        try {
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getLocalServer();
            StringBuilder listOfEmail = new StringBuilder();
            if ((toStrs.length == 0) || (toStrs.length == 1 && StringUtil.isNullOrEmpty(toStrs[0]))) {
                toStrs = new String[prov.getAllAdminAccounts().size()];
                for (int i = 0; i < toStrs.length; i++) {
                    toStrs[i] = String.valueOf(prov.getAllAdminAccounts().get(i).getName());
                    listOfEmail.append(toStrs[i]).append(System.getProperty("line.separator"));
                }
                ZimbraLog.mailbox.debug("Notification email with subject %s will be sent to %s ", subject, listOfEmail);
            }
            if (toStrs == null || toStrs.length == 0) {
                return false;
            }
            fromStr = server.getAttr(Provisioning.A_zimbraBackupReportEmailSender);
            if (fromStr == null) {
                fromStr = "root@" + server.getName();
            }
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        Exception error = null;
        MimeMessage mm = null;
        try {
            mm = new ZMimeMessage(JMSession.getSmtpSession());
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to send notification report email", e);
            return false;
        }
        InternetAddress from = null, to = null;
        try {
            List<Address> toList = new ArrayList<Address>(toStrs.length);
            for (String addrStr : toStrs) {
                try {
                    if (addrStr != null && addrStr.length() > 0) {
                        Address addr = new JavaMailInternetAddress(addrStr);
                        toList.add(addr);
                    }
                } catch (MessagingException mex) {
                    ZimbraLog.mailbox.warn("Ignoring invalid notification recipient address \"" + addrStr + "\"");
                }
            }
            if (toList.size() == 0) {
                ZimbraLog.mailbox.warn("Skipping email notification because no valid recipient address was found");
                return false;
            }
            Address[] rcpts = new Address[toList.size()];
            toList.toArray(rcpts);
            mm.setRecipients(javax.mail.Message.RecipientType.TO, rcpts);
            ZimbraLog.mailbox.debug("Sending notification report email");
            from = new JavaMailInternetAddress(fromStr);
            mm.setFrom(from);
            mm.setSubject(subject);
            mm.setContent(text, "text/html");
            mm.setSentDate(new Date());
            mm.saveChanges();
            Transport.send(mm);
        } catch (MessagingException mex) {
            error = new MailSender.SafeMessagingException(mex);
        }

        if (error != null) {
            ZimbraLog.mailbox.warn("Unable to send notification report email to " + toStrs.toString() +
                            ", subject = " + subject,
                    error);
        }
        return true;
    }
}

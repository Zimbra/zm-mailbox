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

import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.util.Date;

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

}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.EnumSet;
import java.util.HashMap;

public enum MailboxOperation {
    Checkpoint(1),
    CommitTxn(3),
    AbortTxn(4),
    Rollover(6),
    CreateMailbox(7),
    DeleteMailbox(8),
    BackupMailbox(9),
    ReindexMailbox(10),
    PurgeOldMessages(11),
    CreateSavedSearch(12),
    ModifySavedSearch(13),
    CreateTag(14),
    RenameTag(15),
    ColorItem(16),
    IndexItem(17),
    AlterItemTag(18),
    SetItemTags(19),
    MoveItem(20),
    DeleteItem(21),
    CopyItem(22),
    CreateFolderPath(23),
    RenameFolderPath(24),
    EmptyFolder(25),
    StoreIncomingBlob(26),
    CreateMessage(27),
    SaveDraft(28),
    SetImapUid(29),
    CreateContact(30),
    ModifyContact(31),
    CreateNote(32),
    EditNote(33),
    RepositionNote(34),
    CreateMountpoint(35),
    ModifyInviteFlag(36),
    ModifyInvitePartStat(37),
    CreateVolume(38),
    ModifyVolume(39),
    DeleteVolume(40),
    SetCurrentVolume(41),
    MoveBlobs(42),
    CreateInvite(43),
    SetCalendarItem(44),
    TrackSync(45),
    SetConfig(46),
    GrantAccess(47),
    RevokeAccess(48),
    SetFolderUrl(49),
    SetSubscriptionData(50),
    SetPermissions(51),
    SaveWiki(52),
    SaveDocument(53),
    AddDocumentRevision(54),
    TrackImap(55),
    ImapCopyItem(56),
    ICalReply(57),
    CreateFolder(58),
    RenameFolder(59),
    FixCalendarItemTimeZone(60),
    RenameItem(61),
    RenameItemPath(62),
    CreateChat(63),
    SaveChat(64),
    PurgeImapDeleted(65),
    DismissCalendarItemAlarm(66),
    FixCalendarItemEndTime(67),
    IndexDeferredItems(68),
    RenameMailbox(69),
    FixCalendarItemTZ(70),
    DateItem(71),
    SetFolderDefaultView(72),
    SetCustomData(73),
    LockItem(74),
    UnlockItem(75),
    PurgeRevision(76),
    DeleteItemFromDumpster(77),
    FixCalendarItemPriority(78),
    RecoverItem(79),
    EnableSharedReminder(80),
    Download(81),    // not a true mailbox operation but it's used for audit logging
    Preview(82),     // ditto
    SnoozeCalendarItemAlarm(83),
    CreateComment(84),
    CreateLink(85),
    SetRetentionPolicy(86),
    Watch(87),       // Octopus item watch / follow
    Unwatch(88),
    RefreshMountpoint(89),
    ExpireAccess(90),
    SetDisableActiveSync(91),
    SetWebOfflineSyncDays(92),
    DeleteConfig(93),
    //search history redo OPs
    AddSearchHistoryEntry(94),
    PurgeSearchHistory(95),
    DeleteSearchHistory(96),
    SetSavedSearchStatus(97),
    //SmartFolders
    CreateSmartFolder(98);


    private MailboxOperation(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }

    private int code;

    private static final HashMap<Integer,MailboxOperation> operations = new HashMap<Integer,MailboxOperation>();

    static {
        for (MailboxOperation op : EnumSet.allOf(MailboxOperation.class)) {
            operations.put(op.getCode(), op);
        }
    }

    public static MailboxOperation fromInt(int op) {
        return operations.get(op);
    }
}

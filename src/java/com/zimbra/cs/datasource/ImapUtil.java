package com.zimbra.cs.datasource;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.Status;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.CommandFailedException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.imap.Literal;

import javax.mail.MessagingException;
import javax.mail.Folder;
import javax.mail.Flags;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.io.IOException;
import java.io.File;

/**
 * IMAP support utility methods.
 */
final class ImapUtil {
     /*
     * List subfolders given specified root and pattern. Returns folders
     * in ascending order of folder name length with duplicate names removed.
     */
    public static List<IMAPFolder> listFolders(IMAPFolder root, String pattern)
            throws MessagingException {
        // Get list of remote subfolders, removing duplicates (see bug 26483)
        Folder[] folders = root.list(pattern);
        List<IMAPFolder> folderList = new ArrayList<IMAPFolder>(folders.length);
        Set<String> names = new HashSet<String>(folders.length);
        for (Folder folder : folders) {
            String name = getNormalizedName(folder.getFullName(), folder.getSeparator());
            if (names.contains(name)) {
                ZimbraLog.datasource.warn("Ignoring duplicate folder name: " + folder.getFullName());
            } else {
                names.add(name);
                folderList.add((IMAPFolder) folder);
            }
        }
        // When deleting remote folders we delete children before parents to
        // avoid complications, so sort in descending order by full name length.
        Collections.sort(folderList, new Comparator<Folder>() {
            public int compare(Folder f1, Folder f2) {
                return f2.getFullName().length() - f1.getFullName().length();
            }
        });
        return folderList;
    }

    /*
     * Normalize IMAP path name. If specified path is INBOX or a subfolder
     * of INBOX, then return path with INBOX converted to upper case. This
     * is needed to workaround issues where GMail sometimes returns the same
     * folder differing only in the case of the INBOX part (see bug 26483).
     */
    private static String getNormalizedName(String path, char separator) {
        int len = path.length();
        if (len < 5 || !path.substring(0, 5).equalsIgnoreCase("INBOX")) {
            return path;
        }
        if (len == 5) return "INBOX";
        return path.charAt(5) == separator ? "INBOX" + path.substring(5) : path;
    }

    public static Status getStatus(IMAPFolder folder) throws MessagingException {
        Status status = doStatus(folder, "UIDVALIDITY", "UIDNEXT");
        if (status != null && status.uidvalidity == 0) {
            IMAPStore store = (IMAPStore) folder.getStore();
            if (store.hasCapability("XAOL-NETMAIL")) {
                // Workaround for bug 25623: if this is AOL mail and STATUS
                // returns a UIDVALIDITY of 0, then assume a correct value of 1.
                status.uidvalidity = 1;
            }
        }
        if (status == null || status.uidvalidity <= 0 || status.uidnext <= 0) {
            throw new MessagingException("STATUS command failed");
        }
        return status;
    }

    private static Status doStatus(final IMAPFolder folder, final String... items)
            throws MessagingException {
        return (Status) folder.doCommand(new IMAPFolder.ProtocolCommand() {
            public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                try {
                    return protocol.status(folder.getFullName(), items);
                } catch (CommandFailedException e) {
                    return null;
                }
            }
        });
    }

    public static boolean isSelectable(IMAPFolder folder) throws MessagingException {
        return !hasAttribute(folder, "\\Noselect");
    }

    public static boolean hasAttribute(IMAPFolder folder, String attribute)
            throws MessagingException {
        String[] attrs = folder.getAttributes();
        if (attrs != null) {
            for (String attr : attrs) {
                if (attribute.equalsIgnoreCase(attr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void rename(Folder folder, String newName) throws MessagingException {
        if (!folder.renameTo(folder.getStore().getFolder(newName))) {
            throw new MessagingException("Unable to rename folder '" +
                folder.getFullName() + "' to '" + newName + "'");
        }
    }
}

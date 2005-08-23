/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 7, 2004
 */
package com.zimbra.cs.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.mail.MessagingException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.index.ConversationHit;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class DbTest {
	private Mailbox mailbox;
	private int     location = Mailbox.ID_FOLDER_INBOX;
	private String  folderName = "/INBOX";

	private static String padLeft(String str, int length) {
		return padTo(str, length, false, false);
	}

	private static String padLeft(String str, int length, boolean trim) {
		return padTo(str, length, false, trim);
	}

	private static String padRight(String str, int length) {
		return padTo(str, length, true, false);
	}

	private static String padRight(String str, int length, boolean trim) {
		return padTo(str, length, true, trim);
	}
	
	private static String padTo(String str, int length, boolean padRight, boolean trim) {
		if (str == null)
			str = new String("");
		int strlen = str.length();
		if (strlen >= length) {
			if (trim)
				return str.substring(0, length);
			else
				return str;
		}
		StringBuffer sbPad = new StringBuffer();
		for (int i = 0; i < length - strlen; i++)
			sbPad.append(' ');
		if (padRight)
			return str + sbPad;
		else
			return sbPad + str;
	}
	
	private static String shortenAddress(ParsedAddress sender) {
		return shortenAddress(sender, false);
	}
	
	private static String shortenAddress(ParsedAddress sender, boolean realShort) {
		if (sender == null)
			return "";
        else if (realShort && sender.firstName != null)
            return sender.firstName;
        else
            return sender.getSortString();
	}

	private void displayConversationSummary(Conversation conv) throws ServiceException {
		if (conv == null)
			return;
		System.out.print(padLeft(Long.toString(conv.getId()), 9));
		System.out.print(' ');
		System.out.print(conv.isFlagged()     ? '*' : ' ');
		System.out.print(conv.isUnread()      ? 'U' : ' ');
		System.out.print(conv.hasAttachment() ? '@' : ' ');
		System.out.print(' ');
			SenderList senders = conv.getSenderList();
			StringBuffer sbSenders = new StringBuffer();
			ParsedAddress others[] = senders.getLastAddresses();
			boolean realShort = (others.length != 0);
			sbSenders.append(shortenAddress(senders.getFirstAddress(), realShort));
			if (others.length > 2)
				sbSenders.append(" ? ");
			else if (others.length != 0)
				sbSenders.append(", ");
			if (others.length > 1)
				sbSenders.append(shortenAddress(others[others.length-2], realShort)).append(", ");
			if (others.length > 0)
				sbSenders.append(shortenAddress(others[others.length-1], realShort));
		System.out.print(padRight(sbSenders.toString(), 20, true));
		System.out.print(' ');
		System.out.print(padLeft("(" + conv.getMessageCount() + ")", 4));
		System.out.print(' ');
		System.out.print(padRight(conv.getSubject().replace('\t', ' '), 40, true));
		System.out.print("  ");
		System.out.print(new Date(conv.getDate()));
		String tags = conv.getTagString();
		if (tags != null && !tags.equals(""))
			System.out.print("  {" + tags + "}");
		System.out.println();
	}
	
	private static void displayMessageSummary(Message msg) {
		if (msg == null)
			return;
		System.out.print(padLeft("M" + msg.getId(), 9));
		System.out.print(' ');
		System.out.print(msg.isFlagged()     ? '*' : ' ');
		System.out.print(msg.isUnread()      ? 'U' : ' ');
		System.out.print(msg.hasAttachment() ? '@' : ' ');
		System.out.print(' ');
		System.out.print(padRight(msg.isFromMe() ? "me" : shortenAddress(new ParsedAddress(msg.getSender())), 25, true));
		System.out.print(' ');
		System.out.print(padRight(msg.getFragment().replace('\n', ' ').replace('\t', ' '), 40, true));
		System.out.print(' ');
		System.out.print(padLeft("[" + (msg.getSize() + 500) / 1000 + "K]", 7));
		System.out.print("  ");
		System.out.print(new Date(msg.getDate()));
		if (msg.getTagString() != null && !msg.getTagString().equals(""))
			System.out.print("  {" + msg.getTagString() + "}");
		System.out.println();
	}
	
	private static void displayTagSummary(Tag tag) {
		System.out.print(padLeft("T" + tag.getId(), 9));
		System.out.print(' ');
		System.out.print(padLeft("{" + tag.getColor() + "}", 5));
		System.out.print(' ');
		System.out.print(padRight(tag.getName(), 25));
		System.out.print(" [");
		System.out.print(tag.getUnreadCount());
		System.out.print(']');
		System.out.println();
	}
	
	private int fetchLocation(StringTokenizer tok, String usage) throws ServiceException {
		if (tok.countTokens() == 1)
			return fetchLocation(tok.nextToken(), false);
		System.out.println(usage);
		return -1;
	}
	private int fetchLocation(String path, boolean setDefault) throws ServiceException {
		String fullPath = getFullPath(path);
        try {
    		int folderId = mailbox.getFolderByPath(fullPath).getId();
			if (setDefault) {
				folderName = fullPath;
				location   = folderId;
			}
			return folderId;
        } catch (MailServiceException.NoSuchItemException nsie) { }
		return -1;
	}
	String getFullPath(String path) {
		if (path == null || path.startsWith("/"))
			return path;
		String fullPath = folderName;
		String tokens[] = path.split("/");
		for (int i = 0; i < tokens.length; i++) {
			String segment = tokens[i];
			if (segment.equals(".."))
				fullPath = fullPath.substring(0, Math.max(fullPath.lastIndexOf('/'), 1));
			else if (!segment.equals(".") && !segment.equals(""))
				fullPath = fullPath + (fullPath.equals("/") ? "" : "/") + segment;
		}
		return fullPath;
	}
	
	private int fetchItemId(StringTokenizer tok, String usage) {
		int result = -1;
		if (tok.hasMoreTokens()) {
	    	try {
	    		result = Integer.parseInt(tok.nextToken());
	    	} catch (NumberFormatException e) { }
		}
		if (result == -1)
			System.out.println(usage);
		return result;
	}

	private int fetchMessageId(StringTokenizer tok, String usage) {
		int result = -1;
		if (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (token.charAt(0) == 'M' && token.length() > 1) {
		    	try {
		    		result = Math.max(-1, Integer.parseInt(token.substring(1)));
		    	} catch (NumberFormatException e) { }
			}
		}
		if (result == -1)
			System.out.println(usage);
		return result;
	}

	private int fetchTagId(StringTokenizer tok, String usage) {
		int result = 0;
		if (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (token.charAt(0) == 'T' && token.length() > 1) {
		    	try {
		    		result = Integer.parseInt(token.substring(1));
		    	} catch (NumberFormatException e) { }
			}
		}
		if (result == 0)
			System.out.println(usage);
		return result;
	}

	private void doLogin(StringTokenizer tok) throws ServiceException {
		int id = -1;
		if (tok.countTokens() == 1) {
	    	try {
	    		id = Math.max(-1, Integer.parseInt(tok.nextToken()));
	    	} catch (NumberFormatException e) { }
		}
		if (id == -1)
			System.out.println("usage: login <mailbox_id>");
		else
			mailbox = Mailbox.getMailboxById(id);
	}
	
	private void doCd(StringTokenizer tok) throws ServiceException {
		if (tok.countTokens() > 1) {
			System.out.println("usage: cd (<folder>)");
			return;
		}
		String path = tok.hasMoreTokens() ? tok.nextToken() : "/INBOX";
		int newLocation = fetchLocation(path, true);
		if (newLocation == -1)
			System.out.println(path + ": no such folder");
	}
	
	private void createFolder(StringTokenizer tok) throws ServiceException {
		if (!tok.hasMoreTokens())
			System.out.println("usage: mkdir <folder>");
		else {
			String name = tok.nextToken();
			if (name.indexOf('/') != -1)
				System.out.println(name + ": invalid folder name");
			else {
				Folder folder = mailbox.createFolder(null, name, location);
				if (folder == null)
					System.out.println("error: failure during request");
			}
		}
	}
	
	private void deleteFolder(StringTokenizer tok) throws ServiceException {
		if (!tok.hasMoreTokens())
			System.out.println("usage: rmdir <folder>");
		else {
			String path = tok.nextToken();
			int targetId = fetchLocation(path, false);
			if (targetId == -1)
				System.out.println(path + ": no such folder");
			else
				mailbox.delete(null, targetId, MailItem.TYPE_FOLDER);
		}
	}
	
	private void createSearchFolder(StringTokenizer tok) throws ServiceException {
		if (tok.countTokens() < 2)
			System.out.println("usage: sf create <name> <search>");
		else {
			String name = tok.nextToken();
			String query = "";
			if (name.indexOf('/') != -1)
				System.out.println(name + ": invalid folder name");
			else {
				while (tok.hasMoreTokens())
					query = query + " " + tok.nextToken();
				mailbox.createSearchFolder(null, location, name, query.trim(), null, null);
			}
		}
	}

	private void deleteSearchFolder(StringTokenizer tok) throws ServiceException {
		if (tok.countTokens() != 1)
			System.out.println("usage: sf delete <name>");
		else {
			String path = tok.nextToken();
			int targetId = fetchLocation(path, false);
			if (targetId == -1)
				System.out.println(path + ": no such folder");
			else
				mailbox.delete(null, targetId, MailItem.TYPE_SEARCHFOLDER);
		}
	}

	private void searchfolder(StringTokenizer tok) throws ServiceException {
		if (tok.hasMoreTokens()) {
			String subcommand = tok.nextToken();
			if (subcommand.equals("create"))
				createSearchFolder(tok);
			else if (subcommand.equals("delete"))
				deleteSearchFolder(tok);
			else
				System.out.println("unknown subcommand \"" + subcommand + "\"; must be one of: create, delete");
		} else
			System.out.println("usage: sf create|delete ...");
	}
	
	private void createNote(StringTokenizer tok) throws ServiceException {
		if (tok.countTokens() == 0)
			System.out.println("usage: note create <content>");
		else {
			String content = "";
			while (tok.hasMoreTokens())
				content = content + " " + tok.nextToken();
			mailbox.createNote(null, content, new Rectangle(10, 10, 100, 200), Note.DEFAULT_COLOR, location);
		}
	}

	private void deleteNote(StringTokenizer tok) throws ServiceException {
		int noteId = fetchItemId(tok, "usage: note delete <id>");
		if (noteId == -1)
			return;
		mailbox.delete(null, noteId, MailItem.TYPE_NOTE);
	}

	private void note(StringTokenizer tok) throws ServiceException {
		if (tok.hasMoreTokens()) {
			String subcommand = tok.nextToken();
			if (subcommand.equals("create"))
				createNote(tok);
			else if (subcommand.equals("delete"))
				deleteNote(tok);
			else
				System.out.println("unknown subcommand \"" + subcommand + "\"; must be one of: create, delete");
		} else
			System.out.println("usage: note create|delete ...");
	}

	private void listConversations(StringTokenizer tok) throws IOException, ParseException, ServiceException {
		String target = folderName;
		byte type = MailItem.TYPE_CONVERSATION, returnTypes[] = new byte[1];
		if (tok != null && tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (token.startsWith("-")) {
				for (token = token.substring(1); token.length() > 0; token = token.substring(1))
					if (token.startsWith("m"))
					    type = MailItem.TYPE_MESSAGE;
					else {
						System.out.println("unknown option: -" + token.charAt(0));
						return;
					}
				token = tok.hasMoreTokens() ? tok.nextToken() : null;
			}
			if (token != null)
				target = getFullPath(token);
			if (tok.hasMoreTokens()) {
				System.out.println("usage: ls [-m] (<folder>)");
				return;
			}
		}
        returnTypes[0] = type;
		ZimbraQueryResults results = mailbox.search("in:" + target, returnTypes, MailboxIndex.SEARCH_ORDER_DATE_DESC);
		try {
		    for (ZimbraHit hit = results.getFirstHit(); hit != null; hit = results.getNext())
		    	if (type == MailItem.TYPE_CONVERSATION)
		    		displayConversationSummary(((ConversationHit) hit).getConversation());
		    	else
		    		displayMessageSummary(((MessageHit) hit).getMessage());
		} finally {
		    results.doneWithSearchResults();
		}
        try {
    		Folder folder = mailbox.getFolderByPath(target);
			System.out.println("./  [" + folder.getUnreadCount() + "]");
			List subfolders = folder.getSubfolders();
			if (subfolders != null) {
				for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
					Folder subfolder = (Folder) it.next();
					System.out.println(subfolder.getName() + "/  [" + subfolder.getUnreadCount() + "]");
				}
			}
        } catch (MailServiceException.NoSuchItemException nsie) { }
	}

	private void flagMessage(StringTokenizer tok) throws ServiceException {
		int messageId = fetchMessageId(tok, "usage: mflag M<mailbox_blob_id>");
		if (messageId == -1)
			return;
		mailbox.alterTag(null, messageId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FLAGGED, true);
	}
	
	private void flagConversation(StringTokenizer tok) throws ServiceException {
		int conversationId = fetchItemId(tok, "usage: flag <conversation_id>");
		if (conversationId == -1)
			return;
		mailbox.alterTag(null, conversationId, MailItem.TYPE_CONVERSATION, Flag.ID_FLAG_FLAGGED, true);
		displayConversationSummary(mailbox.getConversationById(conversationId));
	}

	private void unflagMessage(StringTokenizer tok) throws ServiceException {
		int messageId = fetchMessageId(tok, "usage: munflag M<mailbox_blob_id>");
		if (messageId == -1)
			return;
		mailbox.alterTag(null, messageId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FLAGGED, false);
		displayMessageSummary(mailbox.getMessageById(messageId));
	}
	
	private void unflagConversation(StringTokenizer tok) throws ServiceException {
		int conversationId = fetchItemId(tok, "usage: unflag <conversation_id>");
		if (conversationId == -1)
			return;
		mailbox.alterTag(null, conversationId, MailItem.TYPE_CONVERSATION, Flag.ID_FLAG_FLAGGED, false);
		displayConversationSummary(mailbox.getConversationById(conversationId));
	}
	
	private void showConversation(StringTokenizer tok) throws ServiceException {
		int conversationId = fetchItemId(tok, "usage: show <conversation_id>");
		if (conversationId == -1)
			return;
		Message[] messages = mailbox.getMessagesByConversation(conversationId);
		if (messages != null)
			for (int i = 0; i < messages.length; i++)
				displayMessageSummary(messages[i]);
	}
	
	private void listTags() throws ServiceException {
		List tagList = mailbox.getTagList();
		if (tagList != null)
			for (Iterator it = tagList.iterator(); it.hasNext(); )
				displayTagSummary((Tag) it.next());
	}

	private void tagMessage(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: tag apply T<tag_id> <message_id>";
		int tagId = fetchTagId(tok, usage);
		if (tagId == 0)
			return;
		int messageId = fetchItemId(tok, usage);
		if (messageId == -1)
			return;
		mailbox.alterTag(null, messageId, MailItem.TYPE_MESSAGE, tagId, true);
		displayMessageSummary(mailbox.getMessageById(messageId));
	}

	private void untagMessage(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: tag remove T<tag_id> <message_id>";
		int tagId = fetchTagId(tok, usage);
		if (tagId == 0)
			return;
		int messageId = fetchItemId(tok, usage);
		if (messageId == -1)
			return;
		mailbox.alterTag(null, messageId, MailItem.TYPE_MESSAGE, tagId, false);
		displayMessageSummary(mailbox.getMessageById(messageId));
	}

	private void taggedConversations(StringTokenizer tok) throws ServiceException, MailServiceException, IOException, ParseException {
		final String usage = "usage: tag targets T<tag_id>";
		int tagId = fetchTagId(tok, usage);
		if (tagId == 0)
			return;
		Tag tag = mailbox.getTagById(tagId);
		if (tag == null)
			return;
		ZimbraQueryResults results = mailbox.search("tag:" + tag.getName(), new byte[] {MailItem.TYPE_CONVERSATION}, MailboxIndex.SEARCH_ORDER_DATE_DESC);
		try {
		    for (ZimbraHit hit = results.getFirstHit(); hit != null; hit = results.getNext())
		        displayConversationSummary(((ConversationHit) hit).getConversation());
		} finally {
		    results.doneWithSearchResults();
		}
	}
	
	private void createTag(StringTokenizer tok) throws ServiceException {
		if (tok.hasMoreTokens()) {
			String tagName = tok.nextToken();
			byte tagColor = (tok.hasMoreTokens() ? Byte.parseByte(tok.nextToken()) : Tag.DEFAULT_COLOR);
			Tag tag = mailbox.createTag(null, tagName, tagColor);
			if (tag != null)
				displayTagSummary(tag);
		} else
			System.out.println("usage: tag create <tag_name> (<color_number>)");
	}
	
	private void deleteTag(StringTokenizer tok) throws ServiceException {
		int tagId = fetchTagId(tok, "usage: tag delete T<tag_id>");
		if (tagId == 0)
			return;
		mailbox.delete(null, tagId, MailItem.TYPE_TAG);
	}

	private void renameTag(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: tag rename T<tag_id> <tag_name>";
		int tagId = fetchTagId(tok, usage);
		if (tagId == 0)
			return;
		if (!tok.hasMoreTokens()) {
			System.out.println(usage);
			return;
		}
		mailbox.renameTag(null, tagId, tok.nextToken());
		displayTagSummary(mailbox.getTagById(tagId));
	}
	
	private void colorTag(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: tag color T<tag_id> <color_number>";
		int tagId = fetchTagId(tok, usage);
		if (tagId == 0)
			return;
		if (!tok.hasMoreTokens()) {
			System.out.println(usage);
			return;
		}
		mailbox.colorTag(null, tagId, Byte.parseByte(tok.nextToken()));
		displayTagSummary(mailbox.getTagById(tagId));
	}
	
	private void tag(StringTokenizer tok) throws MailServiceException, ServiceException, IOException, ParseException {
		if (tok.hasMoreTokens()) {
			String subcommand = tok.nextToken();
			if (subcommand.equals("apply"))
				tagMessage(tok);
			else if (subcommand.equals("remove"))
				untagMessage(tok);
			else if (subcommand.equals("targets"))
				taggedConversations(tok);
			else if (subcommand.equals("create"))
				createTag(tok);
			else if (subcommand.equals("delete"))
				deleteTag(tok);
			else if (subcommand.equals("rename"))
				renameTag(tok);
			else if (subcommand.equals("list"))
				listTags();
			else if (subcommand.equals("color"))
				colorTag(tok);
			else
				System.out.println("unknown subcommand \"" + subcommand + "\"; must be one of: list, create, delete, rename, color, apply, remove");
		} else
			System.out.println("usage: tag list|create|delete|apply|remove ...");
	}

	private void readMessage(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: mread M<message_id>";
		int messageId = fetchMessageId(tok, usage);
		if (messageId == -1)
			return;
		mailbox.alterTag(null, messageId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, false);
		displayMessageSummary(mailbox.getMessageById(messageId));
	}

    private void readConversation(StringTokenizer tok) throws ServiceException {
        final String usage = "usage: read <conversation_id>";
        int conversationId = fetchItemId(tok, usage);
        if (conversationId == -1)
            return;
        mailbox.alterTag(null, conversationId, MailItem.TYPE_CONVERSATION, Flag.ID_FLAG_UNREAD, false);
        displayConversationSummary(mailbox.getConversationById(conversationId));
    }

    private void readFolder(StringTokenizer tok) throws IOException, ParseException, ServiceException {
        if (!tok.hasMoreTokens())
            System.out.println("usage: fread <folder>");
        else {
            String path = tok.nextToken();
            int loc = fetchLocation(path, false);
            if (loc == -1)
                return;
            mailbox.alterTag(null, location, MailItem.TYPE_FOLDER, Flag.ID_FLAG_UNREAD, false);
            listConversations(new StringTokenizer(path));
        }
    }

	private void unreadMessage(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: munread M<message_id>";
		int messageId = fetchMessageId(tok, usage);
		if (messageId == -1)
			return;
		mailbox.alterTag(null, messageId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, true);
		displayMessageSummary(mailbox.getMessageById(messageId));
	}

	private void unreadConversation(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: unread <conversation_id>";
		int conversationId = fetchItemId(tok, usage);
		if (conversationId == -1)
			return;
		mailbox.alterTag(null, conversationId, MailItem.TYPE_CONVERSATION, Flag.ID_FLAG_UNREAD, true);
		displayConversationSummary(mailbox.getConversationById(conversationId));
	}

	private void moveConversation(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: mv <conversation_id> <folder>";
		int conversationId = fetchItemId(tok, usage);
		if (conversationId == -1)
			return;
		int newLocation = fetchLocation(tok, usage);
		if (newLocation == -1)
			return;
		mailbox.move(null, conversationId, MailItem.TYPE_CONVERSATION, newLocation);
	}

	private void moveMessage(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: mmv M<message_id> <folder>";
		int messageId = fetchMessageId(tok, usage);
		if (messageId == -1)
			return;
		int newLocation = fetchLocation(tok, usage);
		if (newLocation == -1)
			return;
		mailbox.move(null, messageId, MailItem.TYPE_MESSAGE, newLocation);
	}
	
	private void deleteConversation(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: rm <conversation_id>";
		int conversationId = fetchItemId(tok, usage);
		if (conversationId == -1)
			return;
		mailbox.delete(null, conversationId, MailItem.TYPE_CONVERSATION);
	}

	private void deleteMessage(StringTokenizer tok) throws ServiceException {
		final String usage = "usage: mrm M<message_id>";
		int messageId = fetchMessageId(tok, usage);
		if (messageId == -1)
			return;
		mailbox.delete(null, messageId, MailItem.TYPE_MESSAGE);
	}
	
	private void search(StringTokenizer tok) throws IOException, ParseException, ServiceException {
		final String usage = "usage: search <term>";
		if (tok.hasMoreTokens()) {
			StringBuffer sb = new StringBuffer();
			while (tok.hasMoreTokens()) {
				if (sb.length() > 0)
					sb.append(' ');
				sb.append(tok.nextToken());
			}
			ZimbraQueryResults results = mailbox.search(sb.toString(), new byte[] {MailItem.TYPE_CONVERSATION}, MailboxIndex.SEARCH_ORDER_DATE_DESC);
			try {
			    for (ZimbraHit hit = results.getFirstHit(); hit != null; hit = results.getNext())
			        displayConversationSummary(((ConversationHit) hit).getConversation());
			} finally {
			    results.doneWithSearchResults();
			}
		} else
			System.out.println(usage);
	}

    private void deleteMailbox(StringTokenizer tok) throws ServiceException {
        mailbox.deleteMailbox();

        mailbox = null;
        location = Mailbox.ID_FOLDER_INBOX;
        folderName = "/INBOX";
    }

    private void purgeMessages(StringTokenizer tok) throws ServiceException {
        mailbox.purgeMessages(null);
    }
	
    private void mainLoop() throws ParseException, IOException, ServiceException {
    	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    while (true) {
	    	if (mailbox == null)
	    		System.out.print("not logged in> ");
	    	else {
	    		System.out.print(mailbox.getId());
	    		System.out.print(':');
	    		System.out.print(folderName);
	    		System.out.print("> ");
	    	}
	    	String line = in.readLine();
	    	if (line == null || line.length() == -1)
	    		break;
	    	StringTokenizer tok = new StringTokenizer(line);
	    	if (tok.countTokens() == 0)
	    		continue;
	    	String cmd = tok.nextToken();
	    	if (cmd.equals("quit") || cmd.equals("exit"))
	    		break;
	    	else if (!cmd.equals("login") && mailbox == null) {
    			System.out.println("error: must run 'login' first");
    			continue;
	    	}
	    	
	    	if (cmd.equals("login"))
	    		doLogin(tok);
	    	else if (cmd.equals("ls"))
	    		listConversations(tok);
	    	else if (cmd.equals("cd"))
	    		doCd(tok);
	    	else if (cmd.equals("mkdir"))
	    		createFolder(tok);
	    	else if (cmd.equals("rmdir"))
	    		deleteFolder(tok);
	    	else if (cmd.equals("search"))
	    		search(tok);
	    	else if (cmd.equals("mv"))
	    		moveConversation(tok);
	    	else if (cmd.equals("mmv"))
	    		moveMessage(tok);
	    	else if (cmd.equals("flag"))
	    		flagConversation(tok);
	    	else if (cmd.equals("unflag"))
	    		unflagConversation(tok);
	    	else if (cmd.equals("mflag"))
	    		flagMessage(tok);
	    	else if (cmd.equals("munflag"))
	    		unflagMessage(tok);
	    	else if (cmd.equals("show"))
	    		showConversation(tok);
	    	else if (cmd.equals("tag"))
	    		tag(tok);
	    	else if (cmd.equals("unread"))
	    		unreadConversation(tok);
	    	else if (cmd.equals("read"))
	    		readConversation(tok);
	    	else if (cmd.equals("munread"))
	    		unreadMessage(tok);
            else if (cmd.equals("mread"))
                readMessage(tok);
            else if (cmd.equals("fread"))
                readFolder(tok);
	    	else if (cmd.equals("rm"))
	    		deleteConversation(tok);
	    	else if (cmd.equals("mrm"))
	    		deleteMessage(tok);
	    	else if (cmd.equals("sf"))
	    		searchfolder(tok);
            else if (cmd.equals("note"))
                note(tok);
            else if (cmd.equals("nuke"))
                deleteMailbox(tok);
            else if (cmd.equals("purge"))
                purgeMessages(tok);
	    	else
	    		System.out.println("unknown command '" + cmd + "'; use one of: " +
	    				"login, ls, cd, search, mkdir, rmdir, mv, mmv, flag, unflag, mflag, munflag, show, tag, read, unread, mread, munread, rm, mrm, sf, note, nuke, purge");
	    }
    }


    public static void main(String[] args) throws MessagingException, IOException, ParseException, ServiceException {
    	Account acct = Provisioning.getInstance().getAccountByName("user1");
        Mailbox mbox = Mailbox.getMailboxByAccount(acct);
        System.out.println(mbox);
        if (mbox != null) {
            Connection conn = DbPool.getConnection();
	        DbMailItem.search(conn, mbox.getId(), new Tag[] {mbox.mAttachFlag}, null, 
	                null, null, MailItem.TYPE_MESSAGE, (byte) (DbMailItem.SORT_BY_DATE | DbMailItem.SORT_DESCENDING));
//	        DbMailItem.listByFolder(mbox.getFolderById(Mailbox.ID_FOLDER_TRASH), MailItem.TYPE_MESSAGE);
            Collection result = DbMailItem.search(conn, mbox.getId(), new Tag[] {mbox.mUnreadFlag}, null, new Folder[] {mbox.getFolderById(Mailbox.ID_FOLDER_INBOX)},
	                null, MailItem.TYPE_MESSAGE, (byte) (DbMailItem.SORT_BY_SUBJECT | DbMailItem.SORT_DESCENDING));
            for (Iterator it = result.iterator(); it.hasNext(); )
                System.out.println(it.next());
	        conn.close();
        }
        (new DbTest()).mainLoop();
    }
}

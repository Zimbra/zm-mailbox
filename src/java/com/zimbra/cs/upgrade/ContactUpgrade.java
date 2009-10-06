/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008 Zimbra, Inc.
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

package com.zimbra.cs.upgrade;

import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.mailbox.ContactConstants;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class to upgrade contacts.
 *
 * @author Andy Clark
 */
public class ContactUpgrade {

	//
	// Constructors
	//

	/** This class can not be instantiated. */
	private ContactUpgrade() {}

	//
	// Public static functions
	//

	public static void upgradeContactsTo1_6(Mailbox mbox) throws ServiceException {
		// bug 41144 -- need to map "workEmail" fields back to "email"
		OperationContext octxt = new OperationContext(mbox);
		List<Contact> contacts = mbox.getContactList(octxt, -1, SortBy.NONE);
		for (Contact contact : contacts) {
			if (contact.get("workEmail") != null) {
				upgradeContactTo1_6(octxt, mbox, contact);
			}
		}
	}

	//
	// Static functions
	//

	static void upgradeContactTo1_6(OperationContext octxt, Mailbox mbox, Contact contact)
	throws ServiceException {
		Map<String,String> fields = contact.getFields();
		Map<String,String> nfields = new HashMap<String,String>();

		// find highest numbered "email" field
		int emailLen = ContactConstants.A_email.length();
		int number = 0;
		for (String key : fields.keySet()) {
			if (!key.startsWith(ContactConstants.A_email)) continue;
			try {
				int num = key.equals(ContactConstants.A_email) ? 1 : Integer.parseInt(key.substring(emailLen), 10);
				if (num > number) {
					number = num;
				}
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}

		// collect "workEmail" fields
		List<String> keys = new java.util.LinkedList<String>();
		for (String key : fields.keySet()) {
			if (!key.startsWith("workEmail")) continue;
			keys.add(key);
		}

		// rename "workEmail" to "email"+number
		for (String key : keys) {
			String nkey = ContactConstants.A_email+(++number > 1 ? String.valueOf(number) : "");
			nfields.put(key, "");
			nfields.put(nkey, fields.get(key));
		}

		// save modified metadata
		ParsedContact pcontact = new ParsedContact(contact);
		pcontact.modify(nfields, null);
		mbox.modifyContact(octxt, contact.getId(), pcontact);
	}

} // class ContactUpgrade
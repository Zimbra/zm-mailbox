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
 * Created on 2004. 7. 12.
 */
package com.zimbra.cs.tools;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.zimbra.cs.db.DbConfig;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class Configure {

	public static void main(String[] args) {
		String op = null;
		String name = null;
		String value = null;

		if (args.length >= 1)
			op = args[0];
		else
			usage();

		if (op.compareToIgnoreCase("list") == 0)
			list();
		else if (op.compareToIgnoreCase("get") == 0 && args.length >= 2)
			get(args[1]);
		else if (op.compareToIgnoreCase("set") == 0 && args.length >= 3)
			set(args[1], args[2]);
		else if (op.compareToIgnoreCase("unset") == 0 && args.length >= 2)
			unset(args[1]);
		else
			usage();
	}

	public static void usage() {
		System.out.println("Usage: ");
		System.out.println("    lconf list");
		System.out.println("    lconf get <key name>");
		System.out.println("    lconf set <key name> <key value>");
		System.out.println("    lconf unset <key name>");
		System.out.println("Note configuration key names are case-sensitive.");
		System.exit(1);
	}

	public static void set(String name, String value) {
		Connection conn = null;
		try {
			conn = DbPool.getConnection();
			DbConfig.set(conn, name, value);
			conn.commit();
		} catch (ServiceException e) {
			System.err.println("Unable to set key \"" + name + "\" to \"" + value + "\"");
			e.printStackTrace();
		} finally {
			DbPool.quietClose(conn);
		}
	}

	public static void unset(String name) {
		Connection conn = null;
		try {
			conn = DbPool.getConnection();
			boolean b = DbConfig.delete(conn, name);
			if (b)
				conn.commit();
			else
				System.out.println("Key \"" + name + "\" not found");
		} catch (ServiceException e) {
			System.err.println("Unable to unset key \"" + name + "\"");
			e.printStackTrace();
		} finally {
			DbPool.quietClose(conn);
		}
	}

	public static void get(String name) {
		Connection conn = null;
		try {
			conn = DbPool.getConnection();
			DbConfig c = DbConfig.get(conn, name);
			if (c != null)
				System.out.println(c.getValue());
			else
				System.out.println("Key \"" + name + "\" not found");
		} catch (ServiceException e) {
			System.err.println("Unable to get key \"" + name + "\"");
			e.printStackTrace();
		} finally {
			DbPool.quietClose(conn);
		}
	}

	public static void list() {
		Connection conn = null;
		try {
			conn = DbPool.getConnection();
			SortedMap confKeys = new TreeMap(DbConfig.getAll(conn, null));
			Set keys = confKeys.keySet();
			for (Iterator i = keys.iterator(); i.hasNext(); ) {
				String name = (String) i.next();
				DbConfig conf = (DbConfig) confKeys.get(name);
				System.out.println(name + " = " + conf.getValue());
			}
		} catch (ServiceException e) {
			System.err.println("Unable to list keys");
			e.printStackTrace();
		} finally {
			DbPool.quietClose(conn);
		}
	}
}

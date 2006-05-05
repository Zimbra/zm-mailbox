/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

public class MetadataDump {

    private static String getUsage() {
        return "Usage: MetadataDump <mailbox ID> <mail item ID>";
    }

    private static String getSQL(int mboxId, int itemId) {
        return
            "SELECT metadata FROM mailbox" + mboxId +
            ".mail_item WHERE id=" + itemId;
    }

    private static String getMetadata(int mboxId, int itemId)
    throws SQLException, ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(getSQL(mboxId, itemId));
            rs = stmt.executeQuery();

            if (!rs.next())
                throw ServiceException.FAILURE(
                        "No such item: mbox=" + mboxId + ", item=" + itemId,
                        null);
            return rs.getString(1);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void main(String[] args) throws Exception {
        Zimbra.toolSetup();
        int mboxId;
        int itemId;
        if (args.length >= 2) {
            mboxId = Integer.parseInt(args[0]);
            itemId = Integer.parseInt(args[1]);
        } else {
            System.out.println(getUsage());
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
            String line;
            System.out.print("Enter mailbox ID: ");
            System.out.flush();
            line = reader.readLine();
            mboxId = Integer.parseInt(line);
            System.out.print("Enter item ID: ");
            System.out.flush();
            line = reader.readLine();
            itemId = Integer.parseInt(line);
            reader.close();
        }

        String encoded = getMetadata(mboxId, itemId);
        Metadata md = new Metadata(encoded);
        String pretty = md.prettyPrint();
        System.out.println(pretty);
    }
}

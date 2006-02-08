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

/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Aug 8, 2005
 *
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;

public class DbRedologSequence {

    private static final String CN_SEQ = "sequence";
    
    private static long mSequence = -1;
    
    private static Log mLog = LogFactory.getLog(DbRedologSequence.class);
    
    public static long getSequence(Connection conn) throws ServiceException {
        if (mSequence != -1) {
            mLog.info("getting cached redo log sequence " + mSequence);
            return mSequence;
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT sequence FROM redolog_sequence");
            rs = stmt.executeQuery();
            if (rs.next()) {
                mSequence = rs.getLong(1);
                mLog.info("getting redo log sequence from db: " + mSequence);
                return mSequence;
            }
            throw ServiceException.FAILURE("current redo log sequence not found", null);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting current redo log sequence", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

    }
    
    public static long setSequence(Connection conn, long sequence) throws ServiceException {
        PreparedStatement stmt = null;
        mSequence = -1;     // invalidated cached sequence
        try {
            stmt = conn.prepareStatement("UPDATE redolog_sequence SET sequence = ?");
            stmt.setLong(1, sequence);
            stmt.executeUpdate();
            conn.commit();
            mSequence = sequence;
            mLog.info("setting redo log sequence to " + mSequence);
            return mSequence;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("setting redo log sequence to " + sequence, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
        
    }
    
    public static long incrementSequence(Connection conn) throws ServiceException {
        long seq = getSequence(conn) + 1;
        return setSequence(conn, seq);
    }
}

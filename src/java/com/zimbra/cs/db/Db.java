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
 * Portions created by Zimbra are Copyright (C) 2004, 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.db;

import java.sql.SQLException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author schemers
 */
public abstract class Db {

    public static enum Error {
        DEADLOCK_DETECTED,
        DUPLICATE_ROW,
        FOREIGN_KEY_CHILD_EXISTS,
        FOREIGN_KEY_NO_PARENT,
        NO_SUCH_DATABASE,
        NO_SUCH_TABLE;
    }

    public static enum Capability {
        BITWISE_OPERATIONS,
        BOOLEAN_DATATYPE,
        CASE_SENSITIVE_COMPARISON,
        DISABLE_CONSTRAINT_CHECK,
        LIMIT_CLAUSE,
        MULTITABLE_UPDATE,
        NULL_IN_UNIQUE_INDEXES,
        ON_DUPLICATE_KEY,
        ON_UPDATE_CASCADE;
    }


    private static Db sDatabase;

    public synchronized static Db getInstance() {
        if (sDatabase == null) {
            String className = LC.zimbra_class_database.value();
            if (className != null && !className.equals("")) {
                try {
                    sDatabase = (Db) Class.forName(className).newInstance();
                } catch (Exception e) {
                    ZimbraLog.system.error("could not instantiate database configuration '" + className + "'; defaulting to MySQL", e);
                }
            }
            if (sDatabase == null)
                sDatabase = new MySQL();
        }
        return sDatabase;
    }


    /** Returns whether the currently-configured database supports the given
     *  {@link Db.Capability}. */
    public static boolean supports(Db.Capability capability) {
        return getInstance().supportsCapability(capability);
    }

    abstract boolean supportsCapability(Db.Capability capability);


    /** Returns whether the given {@link SQLException} is an instance of the
     *  specified {@link Db.Error}. */
    public static boolean errorMatches(SQLException e, Db.Error error) {
        return getInstance().compareError(e, error);
    }

    abstract boolean compareError(SQLException e, Db.Error error);


    abstract DbPool.PoolConfig getPoolConfig();


    /** Generates a SELECT expression representing a BOOLEAN.  For databases
     *  that don't support a BOOLEAN datatype, returns an appropriate CASE
     *  clause that evaluates to 1 when the given BOOLEAN clause is true and
     *  0 when it's false. */
    static String selectBOOLEAN(String clause) {
        if (supports(Capability.BOOLEAN_DATATYPE))
            return clause;
        else
            return "CASE WHEN " + clause + " THEN 1 ELSE 0 END";
    }


    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column equals a string later specified by <tt>stmt.setString()</tt>
     *  under a case-insensitive comparison.  Note that the caller should
     *  pass an upcased version of the comparison string in the subsequent
     *  call to <tt>stmt.setString()</tt>. */
    static String equalsSTRING(String column) {
        if (supports(Capability.CASE_SENSITIVE_COMPARISON))
            return "UPPER(" + column + ") = ?";
        else
            return column + " = ?";
    }

    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column is a case-insensitive match to a SQL pattern string later
     *  specified by <tt>stmt.setString()</tt> under a  comparison.  Note that
     *  the caller should pass an upcased version of the comparison string in
     *  the subsequent call to <tt>stmt.setString()</tt>. */
    static String likeSTRING(String column) {
        if (supports(Capability.CASE_SENSITIVE_COMPARISON))
            return "UPPER(" + column + ") LIKE ?";
        else
            return column + " LIKE ?";
    }


    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column matches a bitmask later specified by <tt>stmt.setLong()</tt>.
     *  Note that this is only valid when the bitmask has only 1 bit set. */
    static String bitmaskAND(String column) {
        if (supports(Capability.BITWISE_OPERATIONS))
            return column + " & ?";
        else
            return "MOD(" + column + " / ?, 2) = 1";
    }

    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column matches the given bitmask.  Note that this is only valid when
     *  the bitmask has only 1 bit set. */
    static String bitmaskAND(String column, long bitmask) {
        if (supports(Capability.BITWISE_OPERATIONS))
            return column + " & " + bitmask;
        else
            return "MOD(" + column + " / " + bitmask + ", 2) = 1";
    }
}

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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author schemers
 */
public abstract class Db {

    public static class Error {
    	public static int DUPLICATE_ROW;
    	public static int DEADLOCK_DETECTED;
    	public static int FOREIGN_KEY_NO_PARENT;
    	public static int FOREIGN_KEY_CHILD_EXISTS;
        public static int NO_SUCH_TABLE;
    }

    public static class Capability {
        public static boolean LIMIT_CLAUSE;
        public static boolean BOOLEAN_DATATYPE;
        public static boolean ON_DUPLICATE_KEY;
        public static boolean ON_UPDATE_CASCADE;
        public static boolean MULTITABLE_UPDATE;
        public static boolean BITWISE_OPERATIONS;
        public static boolean DISABLE_CONSTRAINT_CHECK;
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

            // now that the database has been selected, configure the constants
            sDatabase.setErrorConstants();
            sDatabase.setCapabilities();
        }
        return sDatabase;
    }

    abstract void setErrorConstants();
    abstract void setCapabilities();

    abstract DbPool.PoolConfig getPoolConfig();

    /** Generates a SELECT expression representing a BOOLEAN.  For databases
     *  that don't support a BOOLEAN datatype, returns an appropriate CASE
     *  clause that evaluates to 1 when the given BOOLEAN clause is true and
     *  0 when it's false. */
    static String selectBOOLEAN(String clause) {
        if (Capability.BOOLEAN_DATATYPE)
            return clause;
        else
            return "CASE WHEN " + clause + " THEN 1 ELSE 0 END";
    }

    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column matches a bitmask later specified by <tt>stmt.setLong()</tt>.
     *  Note that this is only valid when the bitmask has only 1 bit set. */
    static String bitmaskAND(String column) {
        if (Capability.BITWISE_OPERATIONS)
            return column + " & ?";
        else
            return "MOD(" + column + " / ?, 2) = 1";
    }

    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column matches the given bitmask.  Note that this is only valid when
     *  the bitmask has only 1 bit set. */
    static String bitmaskAND(String column, long bitmask) {
        if (Capability.BITWISE_OPERATIONS)
            return column + " & " + bitmask;
        else
            return "MOD(" + column + " / " + bitmask + ", 2) = 1";
    }
}

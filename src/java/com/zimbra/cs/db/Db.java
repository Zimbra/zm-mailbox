/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

/*
 * Created on Apr 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.db;


/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Db 
{

    public static final String END_OF_TIME_STRING = "9999-12-31 23:59:59";
    public static final long   END_OF_TIME_LONG   = 253402329599000L;

    public static final long AUTO_INCREMENT_ID = 0;

    public static class Error {
    	public static final int DUPLICATE_ROW = 1062;
    	public static final int DEADLOCK_DETECTED = 1213;
    	public static final int FOREIGN_KEY_NO_PARENT = 1216;
    	public static final int FOREIGN_KEY_CHILD_EXISTS = 1217;
        public static final int NO_SUCH_TABLE = 1146;
    }
}

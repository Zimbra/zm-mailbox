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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.DevNullOutputStream;
import com.zimbra.cs.redolog.util.RedoLogVerify;


public class TestRedoLog
extends TestCase {
    
    public void testRedoLogVerify()
    throws Exception {
        RedoLogVerify verify = new RedoLogVerify(false, false, new DevNullOutputStream());
        assertTrue(verify.verifyFile(new File("/opt/zimbra/redolog/redo.log")));
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestRedoLog.class));
    }
}

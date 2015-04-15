/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.util.EnumSet;

import org.springframework.context.annotation.Bean;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.DefaultRedoLogProvider;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.util.Zimbra;

public class RedoableOpSubclassChecker {
    protected static Log mLog = LogFactory.getLog(RedoableOpSubclassChecker.class);
    private static String sPackageName = RedoableOpSubclassChecker.class.getPackage().getName();


    public static void main(String[] args) throws ServiceException {
        Zimbra.startupMinimal(MyZimbraConfig.class);
        if (!checkSubclasses()) {
            mLog.error(
                    "Some RedoableOp subclasses are incomplete.  " +
                "Hint: Make sure the subclass defines a default constructor.");
            System.exit(1);
        }
    }

    private static boolean checkSubclasses() {
        boolean allGood = true;
        for (MailboxOperation opcode : EnumSet.allOf(MailboxOperation.class)) {
            String className = opcode.name();
            if (className == null) {
                mLog.error("Invalid redo operation code: " + opcode);
                allGood = false;
            } else if (className.compareTo("UNKNOWN") != 0) {
                Class clz = null;
                try {
                    clz = RedoableOp.loadOpClass(sPackageName + "." + className);
                    clz.newInstance();
                } catch (ClassNotFoundException e) {
                    // Some classes may not be found depending on which
                    // optional packages are installed.
                    mLog.info("Ignoring ClassNotFoundException for redo operation " + className);
                } catch (InstantiationException e) {
                    String msg = "Unable to instantiate " + className + "; Check default constructor is defined.";
                    mLog.error(msg, e);
                    allGood = false;
                } catch (IllegalAccessException e) {
                    String msg = "IllegalAccessException while instantiating " + className;
                    mLog.error(msg, e);
                    allGood = false;
                }
            }
        }
        return allGood;
    }

    public static class MyZimbraConfig {
        @Bean
        public RedoLogProvider redologProvider() throws Exception {
            return new DefaultRedoLogProvider();
        }

    }
}

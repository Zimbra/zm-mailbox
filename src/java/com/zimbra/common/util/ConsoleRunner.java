/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.io.*;
import java.util.*;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.ConsoleReaderInputStream;
import jline.History;

/**
 * A version of JLine's <tt>ConsoleRunner</tt> that doesn't fail when
 * command history cannot be saved.
 *  <p>
 *  A pass-through application that sets the system input stream to a
 *  {@link ConsoleReader} and invokes the specified main method.
 *  </p>
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ConsoleRunner {
    public static final String property = "jline.history";

    public static void main(final String[] args) throws Exception {
        String historyFileName = null;

        List argList = new ArrayList(Arrays.asList(args));

        if (argList.size() == 0) {
            usage();

            return;
        }

        historyFileName = System.getProperty(ConsoleRunner.property, null);

        // invoke the main() method
        String mainClass = (String) argList.remove(0);

        // setup the inpout stream
        ConsoleReader reader = new ConsoleReader();
        File historyFile = null;

        try {
            if (historyFileName != null) {
                historyFile = new File
                    (System.getProperty("user.home"),
                        ".jline-" + mainClass
                        + "." + historyFileName + ".history");
            } else {
                historyFile = new File
                    (System.getProperty("user.home"),
                        ".jline-" + mainClass + ".history");
            }
            reader.setHistory(new History(historyFile));
        } catch (IOException e) {
            System.err.format("Unable to write to %s.  Command history will not be saved.\n", historyFile);
        }

        String completors = System.getProperty
            (ConsoleRunner.class.getName() + ".completors", "");
        List completorList = new ArrayList();

        for (StringTokenizer tok = new StringTokenizer(completors, ",");
            tok.hasMoreTokens();) {
            completorList.add
                ((Completor) Class.forName(tok.nextToken()).newInstance());
        }

        if (completorList.size() > 0) {
            reader.addCompletor(new ArgumentCompletor(completorList));
        }

        ConsoleReaderInputStream.setIn(reader);

        try {
            Class.forName(mainClass).
                getMethod("main", new Class[] { String[].class }).
                invoke(null, new Object[] { argList.toArray(new String[0]) });
        } finally {
            // just in case this main method is called from another program
            ConsoleReaderInputStream.restoreIn();
        }
    }

    private static void usage() {
        System.out.println("Usage: \n   java " + "[-Djline.history='name'] "
            + ConsoleRunner.class.getName()
            + " <target class name> [args]"
            + "\n\nThe -Djline.history option will avoid history"
            + "\nmangling when running ConsoleRunner on the same application."
            + "\n\nargs will be passed directly to the target class name.");
    }
}

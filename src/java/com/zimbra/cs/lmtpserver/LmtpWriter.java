/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class LmtpWriter extends PrintWriter {

    private static final String lineSeparator = "\r\n";

    public LmtpWriter(Writer out) {
        super(out, false);
    }

    public LmtpWriter(OutputStream out) {
        super(out, false);
    }

    public void println () {
        synchronized (lock) {
            write(lineSeparator);
        }
    }

    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    public void println(char x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (int x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (long x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (float x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (double x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (char[] x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (String x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (Object x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }
}

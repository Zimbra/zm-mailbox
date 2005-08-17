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

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.util.Zimbra;

class TestBoth {

    private static Log mLog = LogFactory.getLog(TestBoth.class);

	private static Options mOptions = new Options();
	
	static {
	    mOptions.addOption("t", "threads",     true, "number of client threads (default 4)");
	    mOptions.addOption("s", "shutdown",    true, "shutdown server every specified seconds");
        mOptions.addOption("i", "iterations",  true, "iterations of shutdown test (default 1000)");
	    mOptions.addOption("p", "port",        true, "port (default 10043)");
	    mOptions.addOption("T", "trace",       false, "trace server/client traffic");
	    mOptions.addOption("D", "debug",       false, "print debug info");
	    mOptions.addOption("h", "help",        false, "show this help");
	}
	
	private static void usage(String errmsg) {
		if (errmsg != null) { 
			mLog.error(errmsg);
		}
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TestBoth [options]", mOptions);
		System.exit((errmsg == null) ? 0 : 1);
	}

	private static CommandLine parseArgs(String args[]) {
		StringBuffer gotCL = new StringBuffer("cmdline: ");
		for (int i = 0; i < args.length; i++) {
			gotCL.append("'").append(args[i]).append("' ");
		}
		//mLog.info(gotCL);
		
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        
        if (cl.hasOption('h')) {
        	usage(null);
        }
        return cl;
    }

    static class TestClientThread extends Thread {
    	
        private int mPort;
        private boolean mShutdownRequested;
        
        public TestClientThread(int port, int num) {
            super("TestClientThread-" + num);
            setDaemon(true);
            mPort = port;
        }

        public void shutdown() {
            synchronized (this) {
                mShutdownRequested = true;
            }
        }
        
        public void run() {
            while (true) {
                synchronized (this) {
                    if (mShutdownRequested) {
                        return;
                    }
                }
                try {
                    TestClient.run(mPort);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        
    }
    
    public static void main(String[] args) throws IOException {
    	Zimbra.toolSetup("INFO", true);

    	CommandLine cl = parseArgs(args);

    	if (cl.hasOption('D')) {
    		Zimbra.toolSetup("DEBUG", true);
    	}
    	if (cl.hasOption('T')) {
    		Zimbra.toolSetup("TRACE", true);
    	}
        
        int optThreads = 4;
    	if (cl.hasOption('t')) {
    		try {
    			optThreads = Integer.parseInt(cl.getOptionValue('t'));
    		} catch (NumberFormatException nfe) {
    			mLog.error("exception occurred", nfe);
    			usage("bad value for number of threads");
    		}
    	}
        final int numThreads = optThreads;
    	
    	int optPort = 10043;
    	if (cl.hasOption('p')) {
            try {
                optPort = Integer.parseInt(cl.getOptionValue('p'));
            } catch (NumberFormatException nfe) {
                mLog.error("exception occurred", nfe);
                usage("bad value for port");
            }
        }
        final int port = optPort;
        
        int optIterations = 1000;
        if (cl.hasOption('i')) {
            try {
                optIterations = Integer.parseInt(cl.getOptionValue('i'));
            } catch (NumberFormatException nfe) {
                mLog.error("exception occurred", nfe);
                usage("bad value for iterations");
            }
        }
        final int iterations = optIterations;

        if (cl.hasOption('s')) {
            int optSeconds = 0;
            try {
                optSeconds = Integer.parseInt(cl.getOptionValue('s'));
            } catch (NumberFormatException nfe) {
                mLog.error("exception occurred", nfe);
                usage("bad value for shutdown seconds");
            }
            final int seconds = optSeconds;
            new Thread() {
                public void run() {
                    try {
                        for (int i = 0; i < iterations; i++) {
                            startTest(port, numThreads);
                            Thread.sleep(seconds * 1000);
                            endTest();
                        }
                    } catch (InterruptedException ie) {
                        
                    } catch (IOException ioe) { 
                        ioe.printStackTrace();
                    }
                }
            }.start();
        } else {
            startTest(port, numThreads);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            br.readLine();
            endTest();
        }
    }
        
    private static TestServer mTestServer;
    
    private static TestClientThread[] mTestClientThreads;
    
    private static void startTest(int port, int numThreads) throws IOException {
        mTestServer = new TestServer(port);
        mLog.info("creating " + numThreads + " client threads");
        mTestClientThreads = new TestClientThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            mTestClientThreads[i] = new TestClientThread(port, i);
            mTestClientThreads[i].start();
        }
    }
    
    private static void endTest() {
        for (int i = 0; i < mTestClientThreads.length; i++) {
            try {
                mTestClientThreads[i].shutdown();
                mTestClientThreads[i].join();
            } catch (InterruptedException ie) {
                mLog.error("Interrupted while trying to join test clients", ie);
            }
        }
        mTestServer.shutdown();
    }
}

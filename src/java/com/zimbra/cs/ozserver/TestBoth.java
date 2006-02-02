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

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

class TestBoth {

    private static Log mLog = LogFactory.getLog(TestBoth.class);

    private static Options mOptions = new Options();
        
    static {
        mOptions.addOption("a", "address",     true,  "host client should connect to (default: localhost)");
        mOptions.addOption("t", "threads",     true,  "number of client threads (default: 4)");
        mOptions.addOption("s", "shutdown",    true,  "shutdown server every specified seconds");
        mOptions.addOption("S", "secure",      false, "whether to use SSL");
        mOptions.addOption("i", "iterations",  true,  "iterations of shutdown test (default: 1000)");
        mOptions.addOption("c", "count",       true,  "number of transactions per client thread");
        mOptions.addOption("l", "logfile",     true,  "send logging output to given file");
        mOptions.addOption("p", "port",        true,  "port (default: 10043)");
        mOptions.addOption("T", "trace",       false, "trace server/client traffic");
        mOptions.addOption("D", "debug",       false, "print debug info");
        mOptions.addOption("h", "help",        false, "show this help");
        mOptions.addOption("m", "mode",        true,  "run client or server only");
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
        
        private boolean mSecure;
        private String mHost;
        private int mPort;
        private boolean mShutdownRequested;
        private int mTransactions;
        
        public TestClientThread(String host, int port, boolean secure, int threadNumber, int numTransactions) {
            super("TestClientThread-" + threadNumber);
            setDaemon(true);
            mHost = host;
            mPort = port;
            mSecure = secure;
            mTransactions = numTransactions;
        }

        public void shutdown() {
            synchronized (this) {
                mShutdownRequested = true;
            }
        }
        
        public void run() {
            for (int i = 0; i < mTransactions; i++) {
                synchronized (this) {
                    if (mShutdownRequested) {
                        return;
                    }
                }
                try {
                    TestClient.run(mHost, mPort, mSecure);
                } catch (Throwable t) {
                    TestServer.mLog.warn("exception in test client thread", t);
                }
            }
        }
        
    }

    private static boolean mRunClient;
    private static boolean mRunServer;
    
    public static void main(String[] args) throws IOException, ServiceException {
        CommandLine cl = parseArgs(args);

        String logFile = null;
        if (cl.hasOption('l')) {
            logFile = cl.getOptionValue('l');
        }
        
        if (cl.hasOption('T')) {
            Zimbra.toolSetup("TRACE", logFile, true);
        } else if (cl.hasOption('D')) {
            Zimbra.toolSetup("DEBUG", logFile, true);
        } else {
            Zimbra.toolSetup("INFO", logFile, true);
        }

        if (cl.hasOption('m')) {
        	if (cl.getOptionValue('m').equalsIgnoreCase("client")) {
        		mRunClient = true;
        		mRunServer = false;
        		mLog.info("test mode - client only");
        	} else if (cl.getOptionValue('m').equalsIgnoreCase("server")) {
        		mRunClient = false;
        		mRunServer = true;
        		mLog.info("test mode - server only");
        	} else {
        		mRunClient = true;
        		mRunClient = true;
        		mLog.info("test mode - server and client");
        	}
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
        
        String optHost = "localhost";
        if (cl.hasOption('a')) {
            optHost = cl.getOptionValue('a');
        }
        final String host = optHost;

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

        
        int optTransactions = Integer.MAX_VALUE;
        if (cl.hasOption('c')) {
            try {
                optTransactions = Integer.parseInt(cl.getOptionValue('c'));
            } catch (NumberFormatException nfe) {
                mLog.error("exception occurred", nfe);
                usage("bad value for transaction count");
            }
        }
        final int transactions = optTransactions;

        
        final boolean secure = cl.hasOption('S');
        
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
                            startTest(host, port, secure, numThreads, transactions);
                            Thread.sleep(seconds * 1000);
                            endTest();
                        }
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    } catch (IOException ioe) { 
                        ioe.printStackTrace();
                    } catch (ServiceException se) {
                        se.printStackTrace();
                    }
                }
            }.start();
        } else {
            startTest(host, port, secure, numThreads, transactions);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            br.readLine();
            endTest();
        }
    }
        
    private static TestServer mTestServer;
    
    private static TestClientThread[] mTestClientThreads;
    
    private static void startTest(String host, int port, boolean secure, int numThreads, int numTransactions) throws IOException, ServiceException {
    	if (mRunServer) {
    		mTestServer = new TestServer(port, secure);
    	}
    	
    	if (mRunClient) {
    		mLog.info("creating " + numThreads + " client threads");
    		mTestClientThreads = new TestClientThread[numThreads];
    		for (int i = 0; i < numThreads; i++) {
    			mTestClientThreads[i] = new TestClientThread(host, port, secure, i, numTransactions);
    			mTestClientThreads[i].start();
    		}
    	}
    }
    
    private static void endTest() {
    	if (mRunClient) {
    		for (int i = 0; i < mTestClientThreads.length; i++) {
    			try {
    				mTestClientThreads[i].shutdown();
    				mTestClientThreads[i].join();
    			} catch (InterruptedException ie) {
    				mLog.error("Interrupted while trying to join test clients", ie);
    			}
    		}
    	}
    	if (mRunServer) {
    		mTestServer.shutdown();
    	}
    }
}

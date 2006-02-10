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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import com.zimbra.cs.util.ZimbraLog;

class TestConnectionHandler implements OzConnectionHandler {

    private OzByteArrayMatcher mCommandMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLF, null);
    
    private OzByteArrayMatcher mSumDataMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFDOTCRLF, null);
    
    private OzCountingMatcher mNsumDataMatcher = new OzCountingMatcher();
    
    private int mReadState;
    
    long mSum;
    
    private final OzConnection mConnection;
    
    private OzByteBufferGatherer mIncomingData;
    
    private static final int READING_COMMAND = 1;
    private static final int READING_SUM_DATA = 2;
    private static final int READING_NSUM_DATA = 3;
    
    public static final int TIMEOUT_SECONDS = 10;
    
    public static final int MAX_COMMAND_LENGTH = 100;
    public static final int MAX_SUM_LENGTH = 32000;
    
    TestConnectionHandler(OzConnection connection) {
        mConnection = connection;
        mIncomingData = new OzByteBufferGatherer(256, MAX_COMMAND_LENGTH);
    }
    
    private void gotoReadingCommandState() {
        mReadState = READING_COMMAND;
        mCommandMatcher.reset();
        mIncomingData.clear();
        mIncomingData.limit(MAX_COMMAND_LENGTH);
        mConnection.setMatcher(mCommandMatcher);
        mConnection.enableReadInterest();
        TestServer.mLog.info("entered command read state");
    }
    
    private void gotoReadingSumDataState() {
        mReadState = READING_SUM_DATA;
        mSumDataMatcher.reset();
        mIncomingData.clear();
        mIncomingData.limit(MAX_SUM_LENGTH);
        mConnection.setMatcher(mSumDataMatcher);
        mConnection.enableReadInterest();
        TestServer.mLog.info("entered sum read state");
    }
    
    private void gotoReadingNsumDataState(int target) {
        mReadState = READING_NSUM_DATA;
        mNsumDataMatcher.target(target);
        mNsumDataMatcher.reset();
        mIncomingData.clear();
        mIncomingData.limit(MAX_SUM_LENGTH);
        mConnection.setMatcher(mNsumDataMatcher);
        mConnection.enableReadInterest();
        TestServer.mLog.info("entered nsum read state");
    }
    
    public void handleConnect() throws IOException {
        // Write greeting
        mConnection.autoClose(TIMEOUT_SECONDS * 1000);
        mConnection.writeAsciiWithCRLF("200 Hello, welcome to test server cid=" + mConnection.getId());
        gotoReadingCommandState();
    }   
    
    private void doCommand() throws IOException 
    {
        assert(mReadState == READING_COMMAND);
        if (mIncomingData.overflowed()) {
            mConnection.writeAsciiWithCRLF("525 request too long");
            gotoReadingCommandState();
        }
        String cmd = mIncomingData.toAsciiString();
        try {
            TestServer.mLog.info("server executing: " + cmd);
            doCommandInternal(cmd);
        } finally {
            TestServer.mLog.info("server finished: " + cmd);
        }
    }

    private static Pattern ECHO = Pattern.compile("echo\\s+([0-9]+)\\s+([0-9]+)");

    private void doCommandInternal(String cmd) throws IOException 
    {
        if (cmd.equals("helo")) {
            mConnection.cancelAutoClose();
            mConnection.writeAsciiWithCRLF("200 pleased to meet you");
            gotoReadingCommandState();
        } else if (cmd.equals("quit")) {
            mConnection.writeAsciiWithCRLF("200 buh bye");
            mConnection.close();
        } else if (cmd.equals("sum")) {
            gotoReadingSumDataState();
        } else if (cmd.startsWith("nsum")) {
            int bytesToRead = 0;
            int spIdx = cmd.indexOf(' '); 
            if (spIdx == -1) {
                mConnection.writeAsciiWithCRLF("500 bad nsum command");
                gotoReadingCommandState();
                return;
            }
            String number = cmd.substring(spIdx + 1, cmd.length());
            try {
                bytesToRead = Integer.valueOf(number).intValue();
            } catch (Exception nfe) {
                mConnection.writeAsciiWithCRLF("500 number format exception");
                gotoReadingCommandState();
            }
            TestServer.mLog.info("nsum target is " + bytesToRead);
            gotoReadingNsumDataState(bytesToRead); 
        } else if (cmd.startsWith("echo")) {
            Matcher m = ECHO.matcher(cmd);
            if (m.find()) {
                int chunk = Integer.parseInt(m.group(1));
                int times = Integer.parseInt(m.group(2));
                for (int i = 0; i < times; i++) {
                    byte[] arr = new byte[chunk];
                    Arrays.fill(arr, (byte)('A' + i));
                    ByteBuffer bb = ByteBuffer.wrap(arr);
                    mConnection.write(bb);
                }
                TestServer.mLog.info("echo wrote " + times * chunk + " bytes");
            } else {
            	mConnection.writeAsciiWithCRLF("500 bad syntax for echo command");
            }
        	gotoReadingCommandState();
        } else if (cmd.equalsIgnoreCase("starttls")) {
            mConnection.writeAsciiWithCRLF("250 go ahead, start tls negotiation now");
            mConnection.addFilter(new OzTLSFilter(mConnection, TestServer.mLog.isTraceEnabled(), TestServer.mLog));
            gotoReadingCommandState();
        } else {
            mConnection.writeAsciiWithCRLF("500 command " + cmd + " not understood");
            gotoReadingCommandState();
        }
    }
    
    private void doSum() throws IOException
    {
        assert(!mIncomingData.overflowed());
        byte[] data = mIncomingData.array();
        
        int n = data.length;
        for (int i = 0; i < mIncomingData.size(); i++) {
            mSum += data[i];
        }
        mConnection.writeAsciiWithCRLF(new Long(mSum).toString());
        mSum = 0;
        gotoReadingCommandState();
    }

    public void handleInput(ByteBuffer content, boolean matched) throws IOException
    {
        int newBytes = content.remaining();
        int oldSize = mIncomingData.size();
        mIncomingData.add(content);
        int newSize = mIncomingData.size();
        if (TestServer.mLog.isDebugEnabled()) TestServer.mLog.debug("accumulator: oldsz=" + oldSize + " newb=" + newBytes + " newsz=" + newSize);
        
        if (matched) {
            mIncomingData.trim(mConnection.getMatcher().trailingTrimLength());
            if (mReadState == READING_COMMAND) {
                doCommand();
            } else if (mReadState == READING_SUM_DATA || mReadState == READING_NSUM_DATA) {
                doSum();
            } else {
                mConnection.writeAsciiWithCRLF("500 internal server error - wrong read state" + mReadState);
                mConnection.close();
            }
        }
    }
    
    public void handleAutoClose() throws IOException {
        TestServer.mLog.info("connection was idle, terminating");
        mConnection.writeAsciiWithCRLF("550 sorry you have been idle and are being terminated");
        mConnection.close();
    }
    
    public void handleDisconnect() {
        TestServer.mLog.info("connection disconnect");
    }
}

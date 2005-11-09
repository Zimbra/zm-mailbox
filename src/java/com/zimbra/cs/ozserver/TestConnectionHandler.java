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

class TestConnectionHandler implements OzConnectionHandler {

    private OzByteArrayMatcher mCommandMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLF);
    
    private OzByteArrayMatcher mSumDataMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFDOTCRLF);
    
    private OzCountingMatcher mNsumDataMatcher = new OzCountingMatcher();
    
    private int mReadState;
    
    long mSum;
    
    private final OzConnection mConnection;
    
    private static final int READING_COMMAND = 1;
    private static final int READING_SUM_DATA = 2;
    private static final int READING_NSUM_DATA = 3;
    
    TestConnectionHandler(OzConnection connection) {
        mConnection = connection;
    }
    
    private void gotoReadingCommandState() {
        mReadState = READING_COMMAND;
        mCommandMatcher.clear();
        mConnection.setMatcher(mCommandMatcher);
        TestServer.mLog.info("entered command read state");
    }
    
    private void gotoReadingSumDataState() {
        mReadState = READING_SUM_DATA;
        mSumDataMatcher.clear();
        mConnection.setMatcher(mSumDataMatcher);
        TestServer.mLog.info("entered sum read state");
    }
    
    private void gotoReadingNsumDataState(int target) {
        mReadState = READING_NSUM_DATA;
        mNsumDataMatcher.target(target);
        mNsumDataMatcher.clear();
        mConnection.setMatcher(mNsumDataMatcher);
        TestServer.mLog.info("entered nsum read state");
    }
    
    public void handleConnect() throws IOException {
        // Write greeting
        mConnection.writeAscii("200 Hello, welcome to test server cid=" + mConnection.getId(), true);
        gotoReadingCommandState();
    }   
    
    private void readCommand(ByteBuffer content, boolean matched) throws IOException 
    {
        assert(mReadState == READING_COMMAND);
        
        if (!matched) {
            // A command has to fit in the standard buffer size
            mConnection.writeAscii("500 command too long!", true); // TODO test this
            mConnection.close();
            return;
        }
        String cmd = OzUtil.asciiByteArrayToString(content);
        TestServer.mLog.info("got: " + cmd);
        if (cmd.equals("helo")) {
            mConnection.writeAscii("200 pleased to meet you", true);
            gotoReadingCommandState();
        } else if (cmd.equals("quit")) {
            mConnection.writeAscii("200 buh bye", true);
            mConnection.close();
        } else if (cmd.equals("sum")) {
            gotoReadingSumDataState();
        } else if (cmd.startsWith("nsum")) {
            int bytesToRead = 0;
            int spIdx = cmd.indexOf(' '); 
            if (spIdx == -1) {
                mConnection.writeAscii("500 bad nsum command", true);
                gotoReadingCommandState();
                return;
            }
            String number = cmd.substring(spIdx + 1, cmd.length());
            try {
                bytesToRead = Integer.valueOf(number).intValue();
            } catch (Exception nfe) {
                mConnection.writeAscii("500 number format exception", true);
                gotoReadingCommandState();
            }
            TestServer.mLog.info("nsum target is " + bytesToRead);
            gotoReadingNsumDataState(bytesToRead); 
        } else {
            mConnection.writeAscii("500 command " + cmd + " not understood", true);
            gotoReadingCommandState();
        }
    }
    
    private void readSumData(ByteBuffer content, boolean matched) throws IOException
    {
        int n = content.limit();
        for (int i = content.position(); i < n; i++) {
            mSum += content.get();
        }
        if (matched) {
            mConnection.writeAscii(new Long(mSum).toString(), true);
            mSum = 0;
            gotoReadingCommandState();
        }
    }
    private void readNsumData(ByteBuffer content, boolean matched) throws IOException
    {
        int n = content.limit();
        for (int i = content.position(); i < n; i++) {
            mSum += content.get();
        }
        if (matched) {
            mConnection.writeAscii(new Long(mSum).toString(), true);
            mSum = 0;
            gotoReadingCommandState();
        }
    }
    
    public void handleInput(ByteBuffer content, boolean matched) throws IOException
    {
        if (mReadState == READING_COMMAND) {
            readCommand(content, matched);
        } else if (mReadState == READING_SUM_DATA) {
            readSumData(content, matched);
        } else if (mReadState == READING_NSUM_DATA) {
            readNsumData(content, matched);
        } else {
            mConnection.writeAscii("500 internal server error - wrong read state" + mReadState, true);
            mConnection.close();
        }
    }
    
    public void handleDisconnect() {
        TestServer.mLog.info("connection closed by client");
    }
}

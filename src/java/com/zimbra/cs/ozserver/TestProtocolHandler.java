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

import java.io.IOException;
import java.nio.ByteBuffer;

class TestProtocolHandler implements OzProtocolHandler {

    private OzByteArrayMatcher mCommandMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLF);
    
    private OzByteArrayMatcher mSumDataMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFDOTCRLF);
    
    private OzCountingMatcher mNsumDataMatcher = new OzCountingMatcher();
    
    private int mReadState;
    
    long mSum;
    
    private static final int READING_COMMAND = 1;
    private static final int READING_SUM_DATA = 2;
    private static final int READING_NSUM_DATA = 3;
    
    private void gotoReadingCommandState(OzConnection connection) {
        mReadState = READING_COMMAND;
        mCommandMatcher.clear();
        connection.setMatcher(mCommandMatcher);
        TestServer.mLog.info("entered command read state");
    }
    
    private void gotoReadingSumDataState(OzConnection connection) {
        mReadState = READING_SUM_DATA;
        mSumDataMatcher.clear();
        connection.setMatcher(mSumDataMatcher);
        TestServer.mLog.info("entered sum read state");
    }
    
    private void gotoReadingNsumDataState(OzConnection connection, int target) {
        mReadState = READING_NSUM_DATA;
        mNsumDataMatcher.target(target);
        mNsumDataMatcher.clear();
        connection.setMatcher(mNsumDataMatcher);
        TestServer.mLog.info("entered nsum read state");
    }
    
    public void handleConnect(OzConnection connection) throws IOException {
        // Write greeting
        connection.writeAscii("200 Hello, welcome to test server cid=" + connection.getId(), true);
        gotoReadingCommandState(connection);
    }   
    
    private void readCommand(OzConnection connection, ByteBuffer content, boolean matched) throws IOException 
    {
        assert(mReadState == READING_COMMAND);
        
        if (!matched) {
            // A command has to fit in the standard buffer size
            connection.writeAscii("500 command too long!", true); // TODO test this
            connection.close();
            return;
        }
        String cmd = OzUtil.asciiByteArrayToString(content);
        TestServer.mLog.info("got: " + cmd);
        if (cmd.equals("helo")) {
            connection.writeAscii("200 pleased to meet you", true);
            gotoReadingCommandState(connection);
        } else if (cmd.equals("quit")) {
            connection.writeAscii("200 buh bye", true);
            connection.close();
        } else if (cmd.equals("sum")) {
            gotoReadingSumDataState(connection);
        } else if (cmd.startsWith("nsum")) {
            int bytesToRead = 0;
            int spIdx = cmd.indexOf(' '); 
            if (spIdx == -1) {
                connection.writeAscii("500 bad nsum command", true);
                gotoReadingCommandState(connection);
                return;
            }
            String number = cmd.substring(spIdx + 1, cmd.length());
            try {
                bytesToRead = Integer.valueOf(number).intValue();
            } catch (Exception nfe) {
                connection.writeAscii("500 number format exception", true);
                gotoReadingCommandState(connection);
            }
            TestServer.mLog.info("nsum target is " + bytesToRead);
            gotoReadingNsumDataState(connection, bytesToRead); 
        } else {
            connection.writeAscii("500 command " + cmd + " not understood", true);
            gotoReadingCommandState(connection);
        }
    }
    
    private void readSumData(OzConnection connection, ByteBuffer content, boolean matched) throws IOException
    {
        int n = content.limit();
        for (int i = content.position(); i < n; i++) {
            mSum += content.get();
        }
        if (matched) {
            connection.writeAscii(new Long(mSum).toString(), true);
            mSum = 0;
            gotoReadingCommandState(connection);
        }
    }
    private void readNsumData(OzConnection connection, ByteBuffer content, boolean matched) throws IOException
    {
        int n = content.limit();
        for (int i = content.position(); i < n; i++) {
            mSum += content.get();
        }
        if (matched) {
            connection.writeAscii(new Long(mSum).toString(), true);
            mSum = 0;
            gotoReadingCommandState(connection);
        }
    }
    
    public void handleInput(OzConnection connection, ByteBuffer content, boolean matched) throws IOException
    {
        if (mReadState == READING_COMMAND) {
            readCommand(connection, content, matched);
        } else if (mReadState == READING_SUM_DATA) {
            readSumData(connection, content, matched);
        } else if (mReadState == READING_NSUM_DATA) {
            readNsumData(connection, content, matched);
        } else {
            connection.writeAscii("500 internal server error - wrong read state" + mReadState, true);
            connection.close();
        }
    }
    
    public void handleDisconnect(OzConnection connection, boolean byClient) {
        TestServer.mLog.info("Test connection closed byclient=" + byClient);
    }
}

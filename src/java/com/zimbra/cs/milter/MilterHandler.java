/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.milter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.server.NioHandler;

/**
 * Milter protocol handler.
 *
 * <ul>
 *  <li>Check ACL to see if the sender is allowed to send a message to the DL.
 *  <li>Add {@code X-Zimbra-DL: DL1, DL2...} header if one or more recipients are a DL. {@code List-Id} header
 *      (RFC 2919) is not supported because it requires to fork the message. Let's say, "To: listA, listB", user1
 *      belongs to listA, user2 belongs to listB. The user1 should receive "List-Id: listA", and the user2 should
 *      receive "List-Id: listB", which requires Milter to fork the message into each DLs. However, this is not
 *      supported by Milter.
 *  <li>Add {@code Reply-To: DL1, DL2...} header if one or more recipients are a DL.
 * </ul>
 *
 * @see http://cpansearch.perl.org/src/AVAR/Sendmail-PMilter-1.00/doc/milter-protocol.txt
 * @author jmhe
 * @author ysasaki
 */
public final class MilterHandler implements NioHandler {

    private enum Context {
        HOSTNAME, ADDRESS, PORT, PROTOFAMILY, SENDER, RECIPIENT
    }

    /* macro keys */
    private static final String MACRO_MAIL_ADDR = "{mail_addr}";
    private static final String MACRO_RCPT_ADDR = "{rcpt_addr}";

    /* option masks */
    //private static final int SMFIP_NOCONNECT = 0x01; // Skip SMFIC_CONNECT
    private static final int SMFIP_NOHELO = 0x02; // Skip SMFIC_HELO
    private static final int SMFIP_NOMAIL = 0x04; // Skip SMFIC_MAIL
    //private static final int SMFIP_NORCPT = 0x08; // Skip SMFIC_RCPT
    private static final int SMFIP_NOBODY = 0x10; // Skip SMFIC_BODY
    private static final int SMFIP_NOHDRS = 0x20; // Skip SMFIC_HEADER
    private static final int SMFIP_NOEOH  = 0x40; // Skip SMFIC_EOH

    // action masks
    private static final int SMFIF_ADDHDRS = 0x01; // Add headers (SMFIR_ADDHEADER)
    //private static final int SMFIF_CHGBODY = 0x02; // Change body chunks (SMFIR_REPLBODY)
    //private static final int SMFIF_ADDRCPT = 0x04; // Add recipients (SMFIR_ADDRCPT)
    //private static final int SMFIF_DELRCPT = 0x08; // Remove recipients (SMFIR_DELRCPT)
    private static final int SMFIF_CHGHDRS = 0x10; // Change or delete headers (SMFIR_CHGHEADER)
    //private static final int SMFIF_QUARANTINE = 0x20; // Quarantine message (SMFIR_QUARANTINE)

    // response codes
    //private static final byte SMFIR_ADDRCPT = '+'; // Add recipient (modification action)
    //private static final byte SMFIR_DELRCPT = '-'; // Remove recipient (modification action)
    private static final byte SMFIR_ACCEPT = 'a'; // Accept message completely (accept/reject action)
    //private static final byte SMFIR_REPLBODY = 'b'; // Replace body (modification action)
    private static final byte SMFIR_CONTINUE = 'c'; // Accept and keep processing (accept/reject action)
    //private static final byte SMFIR_DISCARD = 'd'; // Set discard flag for entire message (accept/reject action)
    //private static final byte SMFIR_ADDHEADER = 'h'; // Add header (modification action)
    private static final byte SMFIR_CHGHEADER = 'm'; // Change header (modification action)
    //private static final byte SMFIR_PROGRESS = 'p'; // Progress (asynchronous action)
    //private static final byte SMFIR_QUARANTINE = 'q'; // Quarantine message (modification action)
    //private static final byte SMFIR_REJECT = 'r'; // Reject command/recipient with a 5xx (accept/reject action)
    private static final byte SMFIR_TEMPFAIL = 't'; // Reject command/recipient with a 4xx (accept/reject action)
    private static final byte SMFIR_REPLYCODE = 'y'; // Send specific Nxx reply message (accept/reject action)
    private static final byte SMFIC_OPTNEG = 'O'; // Option negotiation (in response to SMFIC_OPTNEG)

    private static final Charset CHARSET = Charsets.US_ASCII;

    private final Map<Context, String> context = new EnumMap<Context, String>(Context.class);
    private final Set<Group> lists = Sets.newHashSetWithExpectedSize(0);
    private final Provisioning prov;
    private final AccessManager accessMgr;
    private final NioConnection connection;

    public MilterHandler(NioConnection conn) {
        prov = Provisioning.getInstance();
        accessMgr = AccessManager.getInstance();
        connection = conn;
    }

    private void clear() {
        context.clear();
        lists.clear();
    }

    @Override
    public void connectionClosed() throws IOException {
        ZimbraLog.milter.info("Connection closed");
        dropConnection();
    }

    @Override
    public void connectionIdle() throws IOException {
        ZimbraLog.milter.debug("Dropping connection for inactivity");
        dropConnection();
    }

    @Override
    public void connectionOpened() throws IOException {
        ZimbraLog.milter.info("Connection opened");
        clear();
    }

    @Override
    public void messageReceived(Object msg) throws IOException {
        MilterPacket command = (MilterPacket) msg;
        try {
            processCommand(command);
        } catch (ServiceException e) {
            ZimbraLog.milter.error("Server error: %s", e.getMessage(), e);
            dropConnection(); // aborting the session
        }
    }

    @Override
    public void dropConnection() {
        if (connection.isOpen()) {
            connection.close();
        }
    }

    @Override
    public void setLoggingContext() {
        ZimbraLog.addConnectionIdToContext(String.valueOf(connection.getId()));
    }

    @Override
    public void exceptionCaught(Throwable e) {
        dropConnection();
    }

    private void processCommand(MilterPacket command) throws IOException, ServiceException {
        switch((char) command.getCommand()) {
            case 'O':
                SMFIC_OptNeg();
                break;
            case 'D':
                SMFIC_Macro(command);
                break;
            case 'C':
                SMFIC_Connect(command);
                break;
            case 'M':
                SMFIC_Mail();
                break;
            case 'R':
                SMFIC_Rcpt();
                break;
            case 'L':
                SMFIC_Header();
                break;
            case 'E':
                SMFIC_BodyEOB();
                break;
            case 'A':
                SMFIC_Abort();
                break;
            case 'Q':
                SMFIC_Quit();
                break;
            default: // for unimplemented commands that require responses, always return "Continue" for now
                connection.send(new MilterPacket(SMFIR_CONTINUE));
                break;
        }
    }

    private IoBuffer getDataBuffer(MilterPacket command) {
        byte[] data = command.getData();
        if (data != null && data.length > 0) {
            IoBuffer buf = IoBuffer.allocate(data.length, false);
            buf.put(data);
            buf.flip();
            return buf;
        } else {
            return null;
        }
    }

    private String normalizeAddr(String a) {
        String addr = a.toLowerCase();
        int lb = addr.indexOf('<');
        int rb = addr.indexOf('>');
        return lb >= 0 && rb > lb ? addr.substring(lb + 1, rb) : addr;
    }

    private void getAddrFromMacro(IoBuffer macroData, String macro, Context attr) throws IOException {
        Map<String, String> macros = parseMacros(macroData);
        String addr = macros.get(macro);
        if (addr != null) {
            String value = normalizeAddr(addr);
            context.put(attr, value);
            ZimbraLog.milter.debug("%s=%s", attr, value);
        }
    }

    private Map<String, String> parseMacros(IoBuffer buf) throws IOException {
        Map<String, String> macros = new HashMap<String, String>();
        while (buf.hasRemaining()) {
            String key = buf.getString(CHARSET.newDecoder());
            if (buf.hasRemaining()) {
                String value = buf.getString(CHARSET.newDecoder());
                if (key != null && value != null) {
                    macros.put(key, value);
                }
            }
        }
        return macros;
    }

    private void SMFIR_ReplyCode(String code, String reason) {
        int len = 1 + 3 + 1 + reason.length() + 1; // cmd + 3-digit code + space + null-terminated text
        String dataStr = code + " " + reason;
        byte[] data = new byte[len - 1];

        int dataStrLen = dataStr.length();
        for (int i = 0; i < dataStrLen; i++) {
            data[i] = (byte)(dataStr.charAt(i));
        }
        data[dataStrLen] = 0;
        connection.send(new MilterPacket(len, SMFIR_REPLYCODE, data));
    }

    private void SMFIR_ChgHeader(int index, String name, String value) throws IOException {
        ZimbraLog.milter.info("Add %s: %s", name, value);
        // sizeof(unit32) + name.length + NUL + value.length + NUL
        IoBuffer buf = IoBuffer.allocate(6 + name.length() + value.length());
        buf.putUnsignedInt(index);
        buf.putString(name, name.length() + 1, CHARSET.newEncoder());
        buf.putString(value, value.length() + 1, CHARSET.newEncoder());
        connection.send(new MilterPacket(buf.position() + 1, SMFIR_CHGHEADER, buf.array()));
    }

    private void SMFIC_Connect(MilterPacket command) throws IOException {
        ZimbraLog.milter.debug("SMFIC_Connect");
        IoBuffer data = getDataBuffer(command);
        if (data != null) {
            context.put(Context.HOSTNAME, data.getString(CHARSET.newDecoder()));
            context.put(Context.PROTOFAMILY, new String(new byte[] {data.get()}, CHARSET));
            context.put(Context.PORT, String.valueOf(data.getUnsignedShort()));
            context.put(Context.ADDRESS, data.getString(CHARSET.newDecoder()));
            ZimbraLog.milter.info("Connection Info %s", context);
        }
        connection.send(new MilterPacket(SMFIR_CONTINUE));
    }

    private void SMFIC_Mail() {
        ZimbraLog.milter.debug("SMFIC_Mail");
        connection.send(new MilterPacket(SMFIR_CONTINUE));
    }

    private void SMFIC_Rcpt() throws ServiceException {
        ZimbraLog.milter.debug("SMFIC_Rcpt");
        String sender = context.get(Context.SENDER);
        if (sender == null) {
            ZimbraLog.milter.warn("Empty sender");
        }
        String rcpt = context.get(Context.RECIPIENT);
        if (rcpt == null) {
            ZimbraLog.milter.warn("Empty recipient");
        }
        if (sender == null || rcpt == null) {
            connection.send(new MilterPacket(SMFIR_TEMPFAIL));
            return;
        }
        if (prov.isDistributionList(rcpt)) {
            Group group = prov.getGroupBasic(Key.DistributionListBy.name, rcpt);
            if (group != null && !accessMgr.canDo(sender, group, User.R_sendToDistList, false)) {
                SMFIR_ReplyCode("571", "571 Sender is not allowed to email this distribution list: " + rcpt);
                return;
            }
            lists.add(group);
        }
        connection.send(new MilterPacket(SMFIR_CONTINUE));
    }

    private void SMFIC_Abort() {
        ZimbraLog.milter.info("SMFIC_Abort session reset");
        clear();
    }

    private void SMFIC_Macro(MilterPacket command) throws IOException {
        ZimbraLog.milter.debug("SMFIC_Macro");
        IoBuffer data = getDataBuffer(command);
        if (data != null) {
            byte cmd = data.get();
            if ((char) cmd == 'M') {
                getAddrFromMacro(data, MACRO_MAIL_ADDR, Context.SENDER);
            } else if ((char) cmd == 'R') {
                getAddrFromMacro(data, MACRO_RCPT_ADDR, Context.RECIPIENT);
            }
        }
    }

    private void SMFIC_OptNeg() {
        ZimbraLog.milter.debug("SMFIC_OptNeg");
        IoBuffer data = IoBuffer.allocate(12, false);
        data.putInt(2); // version
        data.putInt(SMFIF_ADDHDRS | SMFIF_CHGHDRS); // actions
        data.putInt(SMFIP_NOHELO | SMFIP_NOMAIL | SMFIP_NOHDRS | SMFIP_NOEOH | SMFIP_NOBODY); // protocol
        byte[] dataArray = new byte[12];
        System.arraycopy(data.array(), 0, dataArray, 0, 12);
        connection.send(new MilterPacket(13, SMFIC_OPTNEG, dataArray));
    }

    private void SMFIC_Header() {
        ZimbraLog.milter.debug("SMFIC_Header");
        connection.send(new MilterPacket(SMFIR_ACCEPT)); // stop processing when we hit headers
    }

    private void SMFIC_BodyEOB() throws IOException {
        ZimbraLog.milter.debug("SMFIC_BodyEOB");
        Set<String> listAddrs = Sets.newHashSetWithExpectedSize(lists.size());
        Set<String> replyToAddrs = Sets.newHashSetWithExpectedSize(lists.size());
        for (Group group : lists) {
            listAddrs.add(group.getMail());
            if (group.isPrefReplyToEnabled()) {
                String addr = group.getPrefReplyToAddress();
                if (Strings.isNullOrEmpty(addr)) {
                    addr = group.getMail(); // fallback to the default email address
                }
                String disp = group.getPrefReplyToDisplay();
                if (Strings.isNullOrEmpty(disp)) {
                    disp = group.getDisplayName(); // fallback to the default display name
                }
                replyToAddrs.add(new InternetAddress(disp, addr).toString());
            }
        }
        if (!listAddrs.isEmpty()) {
            SMFIR_ChgHeader(1, "X-Zimbra-DL", Joiner.on(", ").join(listAddrs));
        }
        if (!replyToAddrs.isEmpty()) {
            SMFIR_ChgHeader(1, "Reply-To", Joiner.on(", ").join(replyToAddrs));
        }
        connection.send(new MilterPacket(SMFIR_ACCEPT));
    }

    private void SMFIC_Quit() {
        ZimbraLog.milter.debug("SMFIC_Quit");
        dropConnection();
    }

}

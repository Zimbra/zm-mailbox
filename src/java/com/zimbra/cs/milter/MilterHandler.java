/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.accesscontrol.Rights.User;

public abstract class MilterHandler extends ProtocolHandler {
    protected MilterConfig config;
    protected Map<String, Object> context = new HashMap<String, Object>();
    protected long sessId;
    protected String sessPrefix;
    private Provisioning prov;
    private AccessManager accessMgr;
    
    private static final MilterPacket RESPONSE_CONTINUE = new MilterPacket(1, (byte)'c', null);
    private static final MilterPacket RESPONSE_ACCEPT = new MilterPacket(1, (byte)'a', null);
    private static final MilterPacket RESPONSE_REJECT = new MilterPacket(1, (byte)'r', null);
    private static final MilterPacket RESPONSE_TEMPFAIL = new MilterPacket(1, (byte)'t', null);
    
    private final CharsetDecoder asciiDecoder = Charset.forName("US-ASCII").newDecoder();
    
    /* context attrs */
    private static final String CONNECTINFO_HOSTNAME = "cinfo_hostname";
    private static final String CONNECTINFO_PROTOFAMILY = "cinfo_protofamily";
    private static final String CONNECTINFO_PORT = "cinfo_port";
    private static final String CONNECTINFO_ADDRESS = "cinfo_address";
    private static final String MAILFROM_SENDER = "mailfrom_sender";
    private static final String CURRENT_RECIPIENT = "current_recipient";
    
    /* macro keys */
    private static final String MACRO_MAIL_ADDR = "{mail_addr}";
    private static final String MACRO_RCPT_ADDR = "{rcpt_addr}";
    
    /* option masks */
    private static final int SMFIP_NOCONNECT = 0x01;
    private static final int SMFIP_NOHELO = 0x02;
    private static final int SMFIP_NOMAIL = 0x04;
    private static final int SMFIP_NORCPT = 0x08;
    private static final int SMFIP_NOBODY = 0x10;
    private static final int SMFIP_NOHDRS = 0x20;
    private static final int SMFIP_NOEOH  = 0x40;
    
    private static Long nextSessId = new Long(0);
    
    public MilterHandler(MilterServer server) {
        super(null);
        config = server.getConfig();
        prov = Provisioning.getInstance();
        accessMgr = AccessManager.getInstance();
    }
    
    protected void newSession() {
        synchronized (nextSessId) {
            sessId = nextSessId++;
        }
        sessPrefix = "[session-" + String.valueOf(sessId) + "] ";
        context.clear();
    }
    
    protected MilterPacket processCommand(MilterPacket command) throws ServiceException {
        switch((char)command.getCommand()) {
            case 'O': return SMFIC_OptNeg(command);
            case 'D': return SMFIC_Macro(command);
            case 'C': return SMFIC_Connect(command);
            case 'M': return SMFIC_Mail(command);
            case 'R': return SMFIC_Rcpt(command);
            case 'L': return SMFIC_Header(command);
            case 'A': return SMFIC_Abort();
            case 'Q': return SMFIC_Quit(command);
            // for unimplemented commands that require responses, always return "Continue" for now
            default: return RESPONSE_CONTINUE;
        }
    }

    private ByteBuffer getDataBuffer(MilterPacket command) {
        byte[] data = command.getData();
        if (data != null && data.length > 0) {
            ByteBuffer buf = ByteBuffer.allocate(data.length, false);
            buf.put(data);
            buf.flip();
            return buf;
        } else {
            return null;
        }
    }
    
    private void concatStringAttr(String attr, StringBuilder sb) {
        String v = (String)context.get(attr);
        if (v != null) {
            if (sb.length() > 0)
                sb.append(',');
            sb.append(v);
        }
    }
    
    private String getConnInfoString() {
        StringBuilder sb = new StringBuilder();
        concatStringAttr(CONNECTINFO_HOSTNAME, sb);
        concatStringAttr(CONNECTINFO_PROTOFAMILY, sb);
        concatStringAttr(CONNECTINFO_PORT, sb);
        concatStringAttr(CONNECTINFO_ADDRESS, sb);
        return sb.toString();
    }
    
    private String normalizeAddr(String a) {
        String addr = a.toLowerCase();
        int lb = addr.indexOf("<");
        int rb = addr.indexOf(">");
        return lb >= 0 && rb > lb ? addr.substring(lb + 1, rb) : addr;
    }
    
    private void getAddrFromMacro(ByteBuffer macroData, String macro, String attr) {
        Map<String, String> macros = parseMacros(macroData);
        String addr = macros.get(macro);
        if (addr != null)
            context.put(attr, normalizeAddr(addr));
    }
    
    private Map<String, String> parseMacros(ByteBuffer buf) {
        Map<String, String> macros = new HashMap<String, String>();
        while (buf.hasRemaining()) {
            try {
                String key = buf.getString(asciiDecoder);
                if (buf.hasRemaining()) {
                    String value = buf.getString(asciiDecoder);
                    if (key != null && value != null)
                        macros.put(key, value);
                }   
            } catch (CharacterCodingException e) {}
        }
        return macros;
    }
       
    protected MilterPacket SMFIR_ReplyCode(String code, String reason) {
        int len = 1 + 3 + 1 + reason.length() + 1; // cmd + 3-digit code + space + null-terminated text
        String dataStr = code + " " + reason;
        byte[] data = new byte[len - 1];
        
        int dataStrLen = dataStr.length();
        for (int i = 0; i < dataStrLen; i++) {
            data[i] = (byte)(dataStr.charAt(i));
        }
        data[dataStrLen] = 0;
        return new MilterPacket(len, (byte)'y', data);
    }
    
    protected MilterPacket SMFIC_Connect(MilterPacket command) {
        ByteBuffer data = getDataBuffer(command);
        if (data != null) {
            try {
                context.put(CONNECTINFO_HOSTNAME, data.getString(asciiDecoder));
                context.put(CONNECTINFO_PROTOFAMILY, new String(new byte[] {data.get()}, "US-ASCII"));
                context.put(CONNECTINFO_PORT, String.valueOf(data.getUnsignedShort()));
                context.put(CONNECTINFO_ADDRESS, data.getString(asciiDecoder));
                ZimbraLog.milter.info(sessPrefix + "Connection Info: " + getConnInfoString());
            } catch(Exception e) {
                ZimbraLog.milter.warn(sessPrefix + "Unable to read connection information: " + e.getMessage());
            }
        }
        return RESPONSE_CONTINUE;
    }
    
    protected MilterPacket SMFIC_Mail(MilterPacket command) {
        return RESPONSE_CONTINUE;
    }
    
    protected MilterPacket SMFIC_Rcpt(MilterPacket command) throws ServiceException {
        String sender = (String) context.get(MAILFROM_SENDER);
        if (sender == null)
            ZimbraLog.milter.warn(sessPrefix + "Empty sender");
        String rcpt = (String) context.get(CURRENT_RECIPIENT);
        if (rcpt == null)
            ZimbraLog.milter.warn(sessPrefix + "Empty recipient");
        if (sender == null || rcpt == null)
            return RESPONSE_TEMPFAIL;
        
        if (prov.isDistributionList(rcpt)) {
            DistributionList dl = prov.getAclGroup(Provisioning.DistributionListBy.name, rcpt);
            if (dl != null && !accessMgr.canDo(sender, dl, User.R_sendToDistList, false))
                return SMFIR_ReplyCode("571", "571 Sender is not allowed to email this distribution list: " + rcpt);;
        }
        return RESPONSE_CONTINUE;            
    }
    
    protected MilterPacket SMFIC_Abort() {
        ZimbraLog.milter.info(sessPrefix + "Session reset");
        newSession();
        return null;
    }
    
    protected MilterPacket SMFIC_Macro(MilterPacket command) {
        ByteBuffer data = getDataBuffer(command);
        if (data != null) {
            byte cmd = data.get();
            if ((char)cmd == 'M')
                getAddrFromMacro(data, MACRO_MAIL_ADDR, MAILFROM_SENDER);
            else if ((char)cmd == 'R')
                getAddrFromMacro(data, MACRO_RCPT_ADDR, CURRENT_RECIPIENT);
        }
        return null;
    }
    
    protected MilterPacket SMFIC_OptNeg(MilterPacket command) {
        int version = 2;
        int actions = 0;
        int protocol = SMFIP_NOHELO | SMFIP_NOBODY | SMFIP_NOEOH;
        ByteBuffer data = ByteBuffer.allocate(12, false);
        data.putInt(version);
        data.putInt(actions);
        data.putInt(protocol);
        byte[] dataArray = new byte[12];
        System.arraycopy(data.array(), 0, dataArray, 0, 12);
        return new MilterPacket(13, (byte)'O', dataArray);
    }
    
    protected MilterPacket SMFIC_Header(MilterPacket command) {
        return RESPONSE_ACCEPT; // stop processing when we hit headers
    }
    
    protected MilterPacket SMFIC_Quit(MilterPacket command) {
        ZimbraLog.milter.debug(sessPrefix + "Received quit command from MTA");
        dropConnection();
        return null;
    }
}

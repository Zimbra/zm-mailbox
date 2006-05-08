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
package com.zimbra.cs.im.xmpp.srv.filetransfer;

import com.zimbra.cs.im.xmpp.util.CacheSizes;
import com.zimbra.cs.im.xmpp.util.Cacheable;

import java.net.Socket;
import java.util.concurrent.Future;

/**
 * Tracks the different connections related to a file transfer. There are two connections, the
 * initiator and the target and when both connections are completed the transfer can begin.
 */
public class ProxyTransfer implements Cacheable {

    private String initiatorJID;

    private Socket initiatorSocket;

    private Socket targetSocket;

    private String targetJID;

    private String transferDigest;

    private String transferSession;
    
    private Future<?> future;

    public ProxyTransfer(String transferDigest, Socket targetSocket) {
        this.transferDigest = transferDigest;
        this.targetSocket = targetSocket;
    }

    public String getInitiatorJID() {
        return initiatorJID;
    }

    public void setInitiatorJID(String initiatorJID) {
        this.initiatorJID = initiatorJID;
    }

    public Socket getInitiatorSocket() {
        return initiatorSocket;
    }

    public void setInitiatorSocket(Socket initiatorSocket) {
        this.initiatorSocket = initiatorSocket;
    }

    public Socket getTargetSocket() {
        return targetSocket;
    }

    public void setTargetSocket(Socket targetSocket) {
        this.targetSocket = targetSocket;
    }

    public String getTargetJID() {
        return targetJID;
    }

    public void setTargetJID(String targetJID) {
        this.targetJID = targetJID;
    }

    public String getTransferDigest() {
        return transferDigest;
    }

    public void setTransferDigest(String transferDigest) {
        this.transferDigest = transferDigest;
    }

    public String getTransferSession() {
        return transferSession;
    }

    public void setTransferSession(String transferSession) {
        this.transferSession = transferSession;
    }

    /**
     * Returns true if the Bytestream is ready to be activated and the transfer can begin.
     *
     * @return Returns true if the Bytestream is ready to be activated.
     */
    public boolean isActivatable() {
        return ((initiatorSocket != null) && (targetSocket != null));
    }

    public void setTransferFuture(Future<?> future) {
        this.future = future;
    }

    public int getCachedSize() {
        // Approximate the size of the object in bytes by calculating the size
        // of each field.
        int size = 0;
        size += CacheSizes.sizeOfObject();              // overhead of object
        size += CacheSizes.sizeOfString(initiatorJID);
        size += CacheSizes.sizeOfString(targetJID);
        size += CacheSizes.sizeOfString(transferDigest);
        size += CacheSizes.sizeOfString(transferSession);
        return size;
    }
}

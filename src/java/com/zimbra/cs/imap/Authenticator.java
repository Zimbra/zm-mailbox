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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

import java.io.IOException;

abstract class Authenticator {
    protected final ImapHandler mHandler;
    protected final String mTag;
    protected final Mechanism mMechanism;
    protected boolean mComplete;
    protected boolean mCanContinue = true;

    public enum Mechanism { PLAIN, GSSAPI }
    
    public Authenticator(ImapHandler handler, String tag, Mechanism mechanism) {
        mHandler = handler;
        mTag = tag;
        mMechanism = mechanism;
    }

    public abstract boolean initialize() throws IOException;

    public abstract void handle(byte[] data) throws IOException;

    public String getTag() {
        return mTag;
    }
    
    public boolean isComplete() {
        return mComplete;
    }

    public boolean canContinue() {
        return mCanContinue;
    }

    public Mechanism getMechanism() {
        return mMechanism;
    }

    protected boolean login(String authorizeId, String authenticateId,
                            String password) throws IOException {
        mCanContinue = mHandler.login(authorizeId, authenticateId, password,
                                      "authentication", mTag, mMechanism);
        mComplete = true;
        return mHandler.getCredentials() != null;
    }

    protected void sendBadRequest() throws IOException {
        mHandler.sendBAD(mTag, "malformed authentication request");
        mComplete = true;
    }

    protected void sendFailed() throws IOException {
        mHandler.sendNO(mTag, "authentication failed");
        mComplete = true;
    }
}

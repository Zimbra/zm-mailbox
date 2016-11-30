/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;


public class ImapServerListener {
    protected static ImapServerListener instance;
    
    public static synchronized ImapServerListener getInstance() {
        if(instance == null) {
            instance = new ImapServerListener();
        }
        return instance;
    }
    
    ImapServerListener() {
    }

    public void addListener(ImapListener listener) {
    }
    
    public void removeListener(ImapListener listener) {
    }
    
    public boolean isListeningOn(String accountId) {
        return false;
    }

    /**
     * Deletes any remaining waitsets to release resources on remote servers. ImapServer should call this method before dying.
     */
    public void shutdown() {
       
    }
}

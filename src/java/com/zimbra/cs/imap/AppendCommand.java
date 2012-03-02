/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.imap;

import java.util.List;

import com.zimbra.cs.imap.AppendMessage.Part;

public class AppendCommand extends ImapCommand {
    private ImapPath path;
    private List<AppendMessage> appends;

    /**
     * @param path
     * @param appends
     */
    public AppendCommand(ImapPath path, List<AppendMessage> appends) {
        super();
        this.path = path;
        this.appends = appends;
    }

    @Override
    protected boolean hasSameParams(ImapCommand command) {
        if (this.equals(command)) {
            return true;
        }
        AppendCommand other = (AppendCommand) command;
        if (path != null && path.equals(other.path)) {
            if (appends != null) {
                if (other.appends == null || appends.size() != other.appends.size()) {
                    return false;
                }

                // both have appends, both same size
                for (int i = 0; i < appends.size(); i++) {
                    AppendMessage myMsg = appends.get(i);
                    AppendMessage otherMsg = other.appends.get(i);
                    if ((myMsg.getDate() != null && !myMsg.getDate().equals(otherMsg.getDate()))
                            || (myMsg.getDate() == null && otherMsg.getDate() != null)) {
                        return false;
                        // date mismatch
                    } else if ((myMsg.getPersistentFlagNames() != null && !myMsg.getPersistentFlagNames().equals(
                            otherMsg.getPersistentFlagNames()))
                            || (myMsg.getPersistentFlagNames() == null && otherMsg.getPersistentFlagNames() != null)) {
                        return false;
                        // flag name mismatch
                    }
                    List<Part> myParts = myMsg.getParts();
                    List<Part> otherParts = otherMsg.getParts();
                    if ((myParts == null && otherParts != null) || (myParts != null && otherParts == null)
                            || (myParts.size() != otherParts.size())) {
                        return false;
                    }
                    for (int j = 0; j < myParts.size(); j++) {
                        Part myPart = myParts.get(j);
                        Part otherPart = otherParts.get(j);

                        if ((myPart.getLiteral() != null && otherPart.getLiteral() != null && myPart.getLiteral()
                                .size() != otherPart.getLiteral().size())
                                || (myPart.getLiteral() == null && otherPart.getLiteral() != null)
                                || (myPart.getLiteral() != null && otherPart.getLiteral() == null)) {
                            return false;
                            // just checking literal size here; can't check blob
                            // content since it is streamed and not kept in heap
                            // this is good enough for now; if a client is
                            // flooding with bunches of same-length blobs we'll
                            // block them
                        }
                        if (myPart.getUrl() != null && !myPart.getUrl().equals(otherPart.getUrl())
                                || (myPart.getUrl() == null && otherPart.getUrl() != null)) {
                            return false;
                        }
                    }
                }
                return true; // all appends have same size, date, flags; URL
                             // same or Literals with same size
            } else {
                return other.appends != null;
            }
        } else {
            return false;
        }
    }

}

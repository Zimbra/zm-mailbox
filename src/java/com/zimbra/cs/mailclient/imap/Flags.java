/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.imap;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * IMAP message flags.
 */
public final class Flags {
    private int flagMask;
    private List<Atom> extensions;

    private int MASK_ANSWERED   = 0x01;
    private int MASK_FLAGGED    = 0x02;
    private int MASK_DELETED    = 0x04;
    private int MASK_SEEN       = 0x08;
    private int MASK_DRAFT      = 0x10;
    private int MASK_RECENT     = 0x20;
    private int MASK_STAR       = 0x40;

    public static Flags read(ImapInputStream is) throws IOException {
        is.skipChar('(');
        Flags flags = new Flags();
        do {
            flags.set(is.readFlag());
        } while (is.match(' '));
        is.skipChar(')');
        return flags;
    }
    
    public Flags() {}

    public void setAnswered() { set(MASK_ANSWERED); }
    public void setFlagged()  { set(MASK_FLAGGED); }
    public void setDeleted()  { set(MASK_DELETED); }
    public void setSeen()     { set(MASK_SEEN); }
    public void setDraft()    { set(MASK_DRAFT); }
    public void setRecent()   { set(MASK_RECENT); }

    public void set(String flag) {
        set(new Atom(flag));
    }
    
    private void set(Atom flag) {
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            set(mask);
        } else if (!getExtensions().contains(flag)) {
            extensions.add(flag);
        }
    }
    
    private void set(int mask) {
        flagMask |= mask;
    }

    public void unsetAnswered() { unset(MASK_ANSWERED); }
    public void unsetFlagged()  { unset(MASK_FLAGGED); }
    public void unsetDeleted()  { unset(MASK_DELETED); }
    public void unsetSeen()     { unset(MASK_SEEN); }
    public void unsetDraft()    { unset(MASK_DRAFT); }
    public void unsetRecent()   { unset(MASK_RECENT); }

    public void unset(String name) {
        Atom flag = new Atom(name);
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            unset(mask);
        } else {
            getExtensions().remove(flag);
        }
    }
    
    private void unset(int mask) {
        flagMask &= ~mask;
    }

    public boolean isAnswered() { return isSet(MASK_ANSWERED); }
    public boolean isFlagged()  { return isSet(MASK_FLAGGED); }
    public boolean isDeleted()  { return isSet(MASK_DELETED); }
    public boolean isSeen()     { return isSet(MASK_SEEN); }
    public boolean isDraft()    { return isSet(MASK_DRAFT); }
    public boolean isRecent()   { return isSet(MASK_RECENT); }
    public boolean isStar()     { return isSet(MASK_STAR); }

    public boolean isSet(String name) {
        Atom flag = new Atom(name);
        int mask = getMask(CAtom.get(flag));
        return mask != 0 ?
            isSet(mask) : extensions != null && extensions.contains(flag);
    }

    private boolean isSet(int mask) {
        return (flagMask & mask) != 0;
    }
    
    private List<Atom> getExtensions() {
        if (extensions == null) {
            extensions = new ArrayList<Atom>();
        }
        return extensions;
    }

    private int getMask(CAtom catom) {
        switch (catom) {
        case F_ANSWERED:
            return MASK_ANSWERED;
        case F_FLAGGED:
            return MASK_FLAGGED;
        case F_DELETED:
            return MASK_DELETED;
        case F_SEEN:
            return MASK_SEEN;
        case F_DRAFT:
            return MASK_DRAFT;
        case F_RECENT:
            return MASK_RECENT;
        case F_STAR:
            return MASK_STAR;
        default:
            return 0;
        }
    }
}

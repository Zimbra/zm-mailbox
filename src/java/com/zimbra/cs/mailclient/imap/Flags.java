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

    public CAtom ANSWERED = CAtom.F_ANSWERED;
    public CAtom FLAGGED  = CAtom.F_FLAGGED;
    public CAtom DELETED  = CAtom.F_DELETED;
    public CAtom SEEN     = CAtom.F_SEEN;
    public CAtom DRAFT    = CAtom.F_DRAFT;
    public CAtom RECENT   = CAtom.F_RECENT;

    public static Flags read(ImapInputStream is) throws IOException {
        is.skipChar('(');
        Flags flags = new Flags();
        do {
            flags.set(is.readAtom().getName());
        } while (is.match(' '));
        is.skipChar(')');
        return flags;
    }
    
    public Flags() {}

    public void set(String name) {
        Atom flag = new Atom(name);
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            flagMask |= mask;
        } else if (!getExtensions().contains(flag)) {
            extensions.add(flag);
        }
    }

    public void set(CAtom flag) {
        flagMask |= getMask(flag);
    }

    public void setAnswered() { set(ANSWERED); }
    public void setFlagged()  { set(FLAGGED); }
    public void setDeleted()  { set(DELETED); }
    public void setSeen()     { set(SEEN); }
    public void setDraft()    { set(DRAFT); }
    public void setRecent()   { set(RECENT); }
    
    public void unset(String name) {
        Atom flag = new Atom(name);
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            flagMask &= ~mask;
        } else {
            getExtensions().remove(flag);
        }
    }

    public void unsetAnswered() { unset(ANSWERED); }
    public void unsetFlagged()  { unset(FLAGGED); }
    public void unsetDeleted()  { unset(DELETED); }
    public void unsetSeen()     { unset(SEEN); }
    public void unsetDraft()    { unset(DRAFT); }
    public void unsetRecent()   { unset(RECENT); }
    
    public void unset(CAtom flag) {
        flagMask &= ~getMask(flag);
    }
    
    public boolean isSet(String name) {
        Atom flag = new Atom(name);
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            return (flagMask & mask) != 0;
        } else {
            return extensions != null && extensions.contains(flag);
        }
    }

    public boolean isSet(CAtom flag) {
        return (flagMask & getMask(flag)) != 0;
    }

    public boolean isAnswered() { return isSet(ANSWERED); }
    public boolean isFlagged()  { return isSet(FLAGGED); }
    public boolean isDeleted()  { return isSet(DELETED); }
    public boolean isSeen()     { return isSet(SEEN); }
    public boolean isDraft()    { return isSet(DRAFT); }
    public boolean isRecent()   { return isSet(RECENT); }

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
        default:
            return 0;
        }
    }
}

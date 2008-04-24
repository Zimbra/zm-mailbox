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
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.AbstractSet;
import java.io.IOException;

/**
 * IMAP message flags.
 */
public final class Flags extends AbstractSet<Atom> {
    private int flagMask;
    private Set<Atom> otherFlags;

    private static final int MASK_ANSWERED    = 0x01;
    private static final int MASK_FLAGGED     = 0x02;
    private static final int MASK_DELETED     = 0x04;
    private static final int MASK_SEEN        = 0x08;
    private static final int MASK_DRAFT       = 0x10;
    private static final int MASK_RECENT      = 0x20;
    private static final int MASK_STAR        = 0x40;

    // LIST response flags
    private static final int MASK_NOSELECT    = 0x80;
    private static final int MASK_MARKED      = 0x100;
    private static final int MASK_UNMARKED    = 0x200;
    private static final int MASK_NOINFERIORS = 0x400;

    private static final CAtom[] FLAG_ATOMS = {
        CAtom.F_ANSWERED, CAtom.F_FLAGGED,  CAtom.F_DELETED, CAtom.F_SEEN,
        CAtom.F_DRAFT,    CAtom.F_RECENT,   CAtom.F_STAR,    CAtom.F_NOSELECT,
        CAtom.F_MARKED,   CAtom.F_UNMARKED, CAtom.F_NOINFERIORS
    };

    public static Flags read(ImapInputStream is) throws IOException {
        is.skipChar('(');
        Flags flags = new Flags();
        if (is.peekChar() != ')') {
            do {
                flags.set(is.readFlag());
            } while (is.match(' '));
        }
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
    
    public void set(Atom flag) {
        add(flag);
    }

    @Override
    public boolean add(Atom flag) {
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            boolean b = isSet(mask);
            set(mask);
            return b;
        }
        if (otherFlags == null) {
            otherFlags = new HashSet<Atom>();
        }
        return otherFlags.add(flag);
    }

    public void unsetAnswered() { unset(MASK_ANSWERED); }
    public void unsetFlagged()  { unset(MASK_FLAGGED); }
    public void unsetDeleted()  { unset(MASK_DELETED); }
    public void unsetSeen()     { unset(MASK_SEEN); }
    public void unsetDraft()    { unset(MASK_DRAFT); }
    public void unsetRecent()   { unset(MASK_RECENT); }

    public void unset(String flag) {
        unset(new Atom(flag));
    }
    
    public void unset(Atom flag) {
        remove(flag);
    }

    @Override
    public boolean remove(Object obj) {
        if (!(obj instanceof Atom)) return false;
        Atom flag = (Atom) obj;
        int mask = getMask(CAtom.get(flag));
        if (mask != 0) {
            boolean b = !isSet(mask);
            unset(mask);
            return b;
        }
        return otherFlags.remove(flag);
    }

    public boolean isAnswered() { return isSet(MASK_ANSWERED); }
    public boolean isFlagged()  { return isSet(MASK_FLAGGED); }
    public boolean isDeleted()  { return isSet(MASK_DELETED); }
    public boolean isSeen()     { return isSet(MASK_SEEN); }
    public boolean isDraft()    { return isSet(MASK_DRAFT); }
    public boolean isRecent()   { return isSet(MASK_RECENT); }
    public boolean isStar()     { return isSet(MASK_STAR); }
    public boolean isNoselect() { return isSet(MASK_NOSELECT); }
    public boolean isMarked()   { return isSet(MASK_MARKED); }
    public boolean isUnmarked() { return isSet(MASK_UNMARKED); }
    public boolean isNoinferiors() { return isSet(MASK_NOINFERIORS); }

    public boolean isSet(String flag) {
        return isSet(new Atom(flag));
    }
    
    public boolean isSet(Atom flag) {
        return contains(flag);
    }

    @Override
    public boolean contains(Object obj) {
        if (!(obj instanceof Atom)) return false;
        Atom flag = (Atom) obj;
        int mask = getMask(CAtom.get(flag));
        return mask != 0 ?
            isSet(mask) : otherFlags != null && otherFlags.contains(flag);
    }

    @Override
    public int size() {
        int size = otherFlags != null ? otherFlags.size() : 0;
        return size + bitcount(flagMask);
    }

    @Override
    public Iterator<Atom> iterator() {
        List<Atom> flags = new ArrayList<Atom>(size());
        for (CAtom catom : FLAG_ATOMS) {
            if (isSet(getMask(catom))) {
                flags.add(catom.atom());
            }
        }
        if (otherFlags != null) {
            flags.addAll(otherFlags);
        }
        assert flags.size() == size();
        return flags.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        Iterator<Atom> it = iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(' ').append(it.next());
            }
        }
        return sb.append(')').toString();
    }

    public void write(ImapOutputStream os) throws IOException {
        os.write('(');
        Iterator<Atom> it = iterator();
        if (it.hasNext()) {
            it.next().write(os);
            while (it.hasNext()) {
                os.write(' ');
                it.next().write(os);
            }
        }
        os.write(')');
    }
    
    private void set(int mask) {
        flagMask |= mask;
    }

    private void unset(int mask) {
        flagMask &= ~mask;
    }

    private boolean isSet(int mask) {
        return (flagMask & mask) != 0;
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
        case F_NOSELECT:
            return MASK_NOSELECT;
        default:
            return 0;
        }
    }

    // MIT HACKMEM Count
    private static int bitcount(int n) {
        long tmp = (long) n & 0xffffffffL;
        tmp = tmp - ((tmp >> 1) & 033333333333L) - ((tmp >> 2) & 011111111111L);
        return (int) (((tmp + (tmp >> 3)) & 030707070707L) % 63);
    }
}

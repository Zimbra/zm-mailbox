/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.util.HashMap;
import java.util.Map;

/**
 * Constant atom definitions.
 */
public enum CAtom {
    NIL, OK, NO, BAD, BYE, PREAUTH, CAPABILITY, LOGOUT, NOOP, ID,
    STARTTLS, CHECK, CLOSE, EXPUNGE, COPY, CREATE, DELETE, EXAMINE, FETCH,
    ALL, FULL, FAST, ENVELOPE, FLAGS, INTERNALDATE, RFC822_HEADER("RFC822.HEADER"),
    RFC822_SIZE("RFC822.SIZE"), RFC822_TEXT("RFC822.TEXT"), BODY, BODYSTRUCTURE,
    STRUCTURE, UID, BODY_PEEK("BODY.PEEK"), LIST, XLIST, LOGIN, LSUB, INBOX,
    SEARCH, SORT, STATUS, EXISTS, RECENT, APPLICATION, AUDIO, IMAGE, MESSAGE, VIDEO,
    TEXT, RENAME, ALERT, BADCHARSET, PERMANENTFLAGS, SELECT, AUTHENTICATE, SETACL,
    READ_ONLY("READ-ONLY"), READ_WRITE("READ-WRITE"), TRYCREATE, UIDNEXT,
    UIDVALIDITY, UNSEEN, CHARSET, ANSWERED, BCC, BEFORE, CC, PARSE,
    DELETED, FROM, KEYWORD, NEW, OLD, ON, SEEN, SINCE, SUBJECT, TO, COPYUID,
    UNANSWERED, UNDELETED, UNFLAGGED, UNKEYWORD, DRAFT, HEADER, APPENDUID,
    LARGER, NOT, OR, SENTBEFORE, SENTON, SENTSINCE, SMALLER, UNDRAFT, IDLE,
    HEADER_FIELDS("HEADER.FIELDS"), HEADER_FIELDS_NOT("HEADER.FIELDS.NOT"),
    MIME, MESSAGES, STORE, FLAGS_SILENT("FLAGS.SILENT"), SUBSCRIBE, UNSELECT,
    UNSUBSCRIBE, APPEND, CATENATE, URL, F_ANSWERED("\\Answered"),
    F_FLAGGED("\\Flagged"), F_DELETED("\\Deleted"), F_SEEN("\\Seen"),
    F_DRAFT("\\Draft"), F_RECENT("\\Recent"), F_NOINFERIORS("\\Noinferiors"),
    F_NOSELECT("\\Noselect"), F_MARKED("\\Marked"), F_UNMARKED("\\Unmarked"),
    F_STAR("\\*"), UNKNOWN(""), FLUSHCACHE;

    private final Atom atom;

    private static final Map<Atom, CAtom> byAtom = new HashMap<Atom, CAtom>();

    static {
        for (CAtom catom : values()) {
            byAtom.put(catom.atom(), catom);
        }
    }

    public static CAtom get(String name) {
        return get(new Atom(name));
    }

    public static CAtom get(Atom atom) {
        CAtom ca = byAtom.get(atom);
        return ca != null ? ca : UNKNOWN;
    }

    private CAtom() {
        atom = new Atom(name());
    }

    private CAtom(String s) {
        atom = new Atom(s);
    }

    public Atom atom() {
        return atom;
    }

    @Override
    public String toString() {
        return atom().getName();
    }
}

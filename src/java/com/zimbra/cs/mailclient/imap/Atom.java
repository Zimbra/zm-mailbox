package com.zimbra.cs.mailclient.imap;

import java.util.Map;
import java.util.HashMap;

public enum Atom {
    UNKNOWN(""), NIL, OK, NO, BAD, BYE, PREAUTH, CAPABILITY, LOGOUT, NOOP,
    STARTTLS, CHECK, CLOSE, EXPUNGE, COPY, CREATE, DELETE, EXAMINE, FETCH,
    ALL, FULL, FAST, ENVELOPE, FLAGS, INTERNALDATE, RFC822_HEADER("RFC822.HEADER"),
    RFC822_SIZE("RFC822.SIZE"), RFC822_TEXT("RFC822.TEXT"), BODY, BODYSTRUCTURE,
    STRUCTURE, UID, BODY_PEEK("BODY.PEEK"), LIST, LOGIN, LSUB, INBOX,
    SEARCH, STATUS, EXISTS, RECENT, APPLICATION, AUDIO, IMAGE, MESSAGE, VIDEO,
    TEXT, RENAME, ALERT, BADCHARSET, PERMANENTFLAGS,
    READ_ONLY("READ-ONLY"), READ_WRITE("READ-WRITE"), TRYCREATE, UIDNEXT,
    UIDVALIDITY, UNSEEN, CHARSET, ANSWERED, BCC, BEFORE, CC, PARSE,
    DELETED, FROM, KEYWORD, NEW, OLD, ON, SEEN, SINCE, SUBJECT, TO,
    UNANSWERED, UNDELETED, UNFLAGGED, UNKEYWORD, DRAFT, HEADER,
    LARGER, NOT, OR, SENTBEFORE, SENTON, SENTSINCE, SMALLER, UNDRAFT,
    HEADER_FIELDS("HEADER.FIELDS"), HEADER_FIELDS_NOT("HEADER.FIELDS.NOT"),
    MIME, MESSAGES, STORE, FLAGS_SILENT("FLAGS.SILENT"), SUBSCRIBE,
    UNSUBSCRIBE, F_ANSWERED("\\Answered"),
    F_FLAGGED("\\Flagged"), F_DELETED("\\Deleted"), F_SEEN("\\Seen"),
    F_DRAFT("\\Draft"), F_RECENT("\\Recent"), F_NOINFERIORS("\\Noinferiors"),
    F_NOSELECT("\\Noselect"), F_MARKED("\\Marked"), F_UNMARKED("\\Unmarked"),
    F_STAR("\\*");

    private final String mName;

    private static final Map<String, Atom> byName = new HashMap<String, Atom>();

    static {
        for (Atom atom : values()) {
            byName.put(atom.mName.toUpperCase(), atom);
        }
    }

    public static Atom get(String name) {
        Atom atom = byName.get(name.toUpperCase());
        return atom != null ? atom : UNKNOWN;
    }
    
    private Atom() {
        mName = name();
    }

    private Atom(String s) {
        mName = s;
    }

    public String getName() {
        return mName;
    }
}

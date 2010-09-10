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
package com.zimbra.cs.index.query.parser;

import java.io.StringReader;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.lucene.analysis.Analyzer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.query.AddrQuery;
import com.zimbra.cs.index.query.AttachmentQuery;
import com.zimbra.cs.index.query.BuiltInQuery;
import com.zimbra.cs.index.query.ConjQuery;
import com.zimbra.cs.index.query.ConvCountQuery;
import com.zimbra.cs.index.query.ConvQuery;
import com.zimbra.cs.index.query.DateQuery;
import com.zimbra.cs.index.query.DomainQuery;
import com.zimbra.cs.index.query.HasQuery;
import com.zimbra.cs.index.query.InQuery;
import com.zimbra.cs.index.query.ItemQuery;
import com.zimbra.cs.index.query.ModseqQuery;
import com.zimbra.cs.index.query.Query;
import com.zimbra.cs.index.query.AddrQuery.Address;
import com.zimbra.cs.index.query.Query.Modifier;
import com.zimbra.cs.index.query.SenderQuery;
import com.zimbra.cs.index.query.SizeQuery;
import com.zimbra.cs.index.query.SubQuery;
import com.zimbra.cs.index.query.SubjectQuery;
import com.zimbra.cs.index.query.TagQuery;
import com.zimbra.cs.index.query.TextQuery;
import com.zimbra.cs.index.query.TypeQuery;
import static com.zimbra.cs.index.query.parser.ParserConstants.*;
import static com.zimbra.cs.index.query.parser.ParserTreeConstants.*;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;

/**
 * Parser for search query.
 *
 * @author ysasaki
 */
public final class QueryParser {

    private static final Map<String, Integer> IMG2JJ;
    static {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (int i = 0; i < tokenImage.length; i++) {
            String token = tokenImage[i];
            if (token.startsWith("\"") || token.endsWith(":\"")) {
                builder.put(token.substring(1, token.length() - 1), i);
            }
        }
        IMG2JJ = builder.build();
    }

    private static final Map<String, Integer> FOLDER2ID =
        new ImmutableMap.Builder<String, Integer>()
        .put("inbox", Mailbox.ID_FOLDER_INBOX)
        .put("trash", Mailbox.ID_FOLDER_TRASH)
        .put("junk", Mailbox.ID_FOLDER_SPAM)
        .put("sent", Mailbox.ID_FOLDER_SENT)
        .put("drafts", Mailbox.ID_FOLDER_DRAFTS)
        .put("contacts", Mailbox.ID_FOLDER_CONTACTS)
        .build();

    private static final Map<Integer, String> JJ2LUCENE =
        new ImmutableMap.Builder<Integer, String>()
        .put(CONTACT, LuceneFields.L_CONTACT_DATA)
        .put(CONTENT, LuceneFields.L_CONTENT)
        .put(MSGID, LuceneFields.L_H_MESSAGE_ID)
        .put(ENVFROM, LuceneFields.L_H_X_ENV_FROM)
        .put(ENVTO, LuceneFields.L_H_X_ENV_TO)
        .put(FROM, LuceneFields.L_H_FROM)
        .put(TO, LuceneFields.L_H_TO)
        .put(CC, LuceneFields.L_H_CC)
        .put(SUBJECT, LuceneFields.L_H_SUBJECT)
        .put(FILENAME, LuceneFields.L_FILENAME)
        .put(TYPE, LuceneFields.L_MIMETYPE)
        .put(ATTACHMENT, LuceneFields.L_ATTACHMENTS)
        .put(FIELD, LuceneFields.L_FIELD)
        .put(IN, "IN")
        .put(HAS, "HAS")
        .put(IS, "IS")
        .put(DATE, "DATE")
        .put(AFTER, "AFTER")
        .put(BEFORE, "BEFORE")
        .put(APPT_START, "APPT-START")
        .put(APPT_END, "APPT-END")
        .put(SIZE, "SIZE")
        .put(BIGGER, "BIGGER")
        .put(SMALLER, "SMALLER")
        .put(TAG, "TAG")
        .put(MY, "MY")
        .put(MESSAGE, "MESSAGE")
        .put(CONV, "CONV")
        .put(CONV_COUNT, "CONV-COUNT")
        .put(CONV_MINM, "CONV_MINM")
        .put(CONV_MAXM, "CONV_MAXM")
        .put(CONV_START, "CONV-START")
        .put(CONV_END, "CONV-END")
        .put(AUTHOR, "AUTHOR")
        .put(TITLE, "TITLE")
        .put(KEYWORDS,"KEYWORDS")
        .put(COMPANY, "COMPANY")
        .put(METADATA, "METADATA")
        .put(ITEM, "ITEMID")
        .build();

    private final Mailbox mailbox;
    private final Analyzer analyzer;
    private TimeZone timezone = TimeZone.getTimeZone("UTC");
    private Locale locale = Locale.ENGLISH;
    private int defaultField = CONTENT;
    private String sortBy;

    /**
     * Constructs a new {@link QueryParser}.
     *
     * @param mbox mailbox to search
     * @param analyzer Lucene analyzer
     */
    public QueryParser(Mailbox mbox, Analyzer analyzer) {
        this.mailbox = mbox;
        this.analyzer = analyzer;
    }

    /**
     * Sets the current time zone for date-time translation.
     *
     * @param value time zone
     */
    public void setTimeZone(TimeZone value) {
        timezone = value;
    }

    /**
     * Sets the current locale for date-time format.
     *
     * @param value locale
     */
    public void setLocale(Locale value) {
        locale = value;
    }

    /**
     * Sets the default field in case field name is omitted.
     *
     * @param name field name
     * @throws ServiceException if the name is invalid
     */
    public void setDefaultField(String name) throws ServiceException {
        Integer jj = IMG2JJ.get(name);
        if (jj == null) {
            throw MailServiceException.QUERY_PARSE_ERROR(
                    name, null, name, -1, "UNKNOWN_QUERY_TYPE");
        }
        defaultField = jj;
    }

    /**
     * Returns the sort field detected in the query string.
     * <p>
     * The value is set after {@link #parse(String)}.
     *
     * @return sort field
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Parses the query string.
     *
     * @param src query string
     * @return query clauses
     * @throws ServiceException if a grammar error detected
     */
    public List<Query> parse(String src) throws ServiceException {
        Parser parser = new Parser(new StringReader(src));
        SimpleNode node;
        try {
            node = parser.parse();
        } catch (TokenMgrError e) {
            throw MailServiceException.QUERY_PARSE_ERROR(src, e,
                    "", -1, e.getMessage());
        } catch (ParseException e) {
            throw MailServiceException.QUERY_PARSE_ERROR(src, e,
                    e.currentToken.image, e.currentToken.beginColumn,
                    e.getMessage());
        }

        assert(node.id == JJTROOT);
        assert(node.jjtGetNumChildren() == 1);

        try {
            return toQuery((SimpleNode) node.jjtGetChild(0));
        } catch (ParseException e) {
            throw MailServiceException.QUERY_PARSE_ERROR(src, e,
                    e.currentToken.image, e.currentToken.beginColumn,
                    e.getMessage());
        }
    }

    private List<Query> toQuery(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTQUERY);

        List<Query> result = new LinkedList<Query>();
        ConjQuery conj = null;
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            switch (child.id) {
                case JJTCONJUNCTION:
                    conj = toConjunction(child);
                    break;
                case JJTCLAUSE:
                    if (!result.isEmpty()) {
                        if (conj == null) {
                            result.add(new ConjQuery(ConjQuery.Conjunction.AND));
                        } else {
                            result.add(conj);
                            conj = null;
                        }
                    }
                    result.add(toClause(child));
                    break;
                case JJTSORTBY:
                    processSortBy(child);
                    break;
                default:
                    assert(false);
            }
        }
        return result;
    }

    private Query toClause(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTCLAUSE);
        int num = node.jjtGetNumChildren();
        assert(num > 0 && num <= 2);

        Query clause = null;
        SimpleNode child = (SimpleNode) node.jjtGetChild(num - 1);
        switch (child.id) {
            case JJTTEXTCLAUSE:
                clause = toTextClause(child);
                break;
            case JJTITEMCLAUSE:
                clause = toItemClause(child);
                break;
            case JJTDATECLAUSE:
                clause = toDateClause(child);
                break;
            case JJTQUERY:
                clause = toSubQuery(child);
                break;
            case JJTDEFAULTCLAUSE:
                clause = toDefaultClause(child);
                break;
            default:
                assert(false);
                return null;
        }
        if (node.jjtGetNumChildren() > 1) {
            clause.setModifier(toModifier((SimpleNode) node.jjtGetChild(0)));
        }
        return clause;
    }

    private Query toSubQuery(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTQUERY);
        return new SubQuery(toQuery(node));
    }

    private Query toTextClause(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTTEXTCLAUSE);
        assert(node.jjtGetNumChildren() == 1);

        return toTerm(node.jjtGetFirstToken(), (SimpleNode) node.jjtGetChild(0));
    }

    private Query toDefaultClause(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTDEFAULTCLAUSE);

        return createQuery(Token.newToken(defaultField),
                node.jjtGetFirstToken(), toString(node));
    }

    private Query toItemClause(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTITEMCLAUSE);
        assert(node.jjtGetNumChildren() == 1);

        return toTerm(node.jjtGetFirstToken(), (SimpleNode) node.jjtGetChild(0));
    }

    private Query toDateClause(SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTDATECLAUSE);
        assert(node.jjtGetNumChildren() == 1);

        return toTerm(node.jjtGetFirstToken(), (SimpleNode) node.jjtGetChild(0));
    }

    private Query toTerm(Token field, SimpleNode node)
        throws ParseException, ServiceException {

        assert(node.id == JJTDATETERM || node.id == JJTTEXTTERM ||
                node.id == JJTITEMTERM);

        if (node.jjtGetNumChildren() == 0) {
            Token token = node.jjtGetFirstToken();
            return createQuery(field, token, toString(node));
        } else {
            List<Query> sub = new LinkedList<Query>();
            ConjQuery conj = null;
            Modifier mod = null;
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                SimpleNode child = (SimpleNode) node.jjtGetChild(i);
                switch (child.id) {
                    case JJTMODIFIER:
                        mod = toModifier(child);
                        break;
                    case JJTCONJUNCTION:
                        conj = toConjunction(child);
                        break;
                    case JJTTEXTTERM:
                    case JJTITEMTERM:
                    case JJTDATETERM:
                        if (!sub.isEmpty()) {
                            if (conj == null) {
                                sub.add(new ConjQuery(ConjQuery.Conjunction.AND));
                            } else {
                                sub.add(conj);
                                conj = null;
                            }
                        }
                        Query term = toTerm(field, child);
                        if (mod != null) {
                            term.setModifier(mod);
                            mod = null;
                        }
                        sub.add(term);
                        break;
                    default:
                        assert(false);
                }
            }
            return new SubQuery(sub);
        }
    }

    private String toString(SimpleNode node) {
        assert(node.jjtGetNumChildren() == 0);

        switch (node.id) {
            case JJTTEXTTERM:
            case JJTITEMTERM:
            case JJTDEFAULTCLAUSE:
                return toString(node.jjtGetFirstToken());
            case JJTDATETERM:
                Token token = node.jjtGetFirstToken();
                switch (token.kind) {
                    case PLUS:
                    case MINUS:
                        return token.image + toString(token.next);
                    default:
                        return toString(token);
                }
            default:
                assert(false);
                return "";
        }

    }

    private String toString(Token token) {
        switch (token.kind) {
            case TERM:
                return token.image;
            case QUOTED_TERM:
            case BRACED_TERM:
                // trim
                return token.image.substring(1, token.image.length() - 1);
            default:
                assert(false);
                return "";
        }
    }

    private ConjQuery toConjunction(SimpleNode node) {
        assert(node.id == JJTCONJUNCTION);

        switch (node.jjtGetFirstToken().kind) {
            case AND:
                return new ConjQuery(ConjQuery.Conjunction.AND);
            case OR:
                return new ConjQuery(ConjQuery.Conjunction.OR);
            default:
                assert(false);
                return null;
        }
    }

    private Query.Modifier toModifier(SimpleNode node) {
        assert(node.id == JJTMODIFIER);
        switch (node.jjtGetFirstToken().kind) {
            case PLUS:
                return Query.Modifier.PLUS;
            case MINUS:
            case NOT:
                return Query.Modifier.MINUS;
            default:
                return Query.Modifier.NONE;
        }
    }

    private void processSortBy(SimpleNode node) {
        assert(node.id == JJTSORTBY);
        sortBy = node.jjtGetFirstToken().next.image;
    }

    private Query createQuery(Token field, Token term, String text)
        throws ParseException, ServiceException {

        switch (field.kind) {
          case HAS:
            if (text.equalsIgnoreCase("attachment")) {
                return new AttachmentQuery(mailbox, "any");
            } else {
                return new HasQuery(mailbox, text);
            }
          case ATTACHMENT:
            return new AttachmentQuery(mailbox, text);
          case TYPE:
            return new TypeQuery(mailbox, text);
          case ITEM:
            return ItemQuery.create(mailbox, text);
          case UNDERID:
          case INID: {
              ItemId iid = null;
              int subfolderSplit = text.indexOf('/');
              String iidStr;
              String subfolderPath = null;
              if (subfolderSplit > 0) {
                  iidStr = text.substring(0, subfolderSplit);
                  subfolderPath = text.substring(subfolderSplit + 1);
              } else {
                  iidStr = text;
              }
              iid = new ItemId(iidStr, (String) null);
              try {
                  return InQuery.create(mailbox, iid, subfolderPath, (field.kind == UNDERID));
              } catch (ServiceException e) {
                  // bug: 18623 -- dangling mountpoints create problems with 'is:remote'
                  ZimbraLog.index.debug("Ignoring INID/UNDERID clause b/c of ServiceException", e);
                  return InQuery.create(InQuery.In.NONE, false);
              }
          }
          case UNDER:
          case IN: {
              Integer folderId = FOLDER2ID.get(text.toLowerCase());
              if (folderId != null) {
                  return InQuery.create(mailbox, folderId, (field.kind == UNDER));
              } else {
                  return InQuery.create(mailbox, text, (field.kind == UNDER));
              }
          }
          case TAG:
              return new TagQuery(mailbox, text, true);
          case IS:
              try {
                  return BuiltInQuery.getQuery(text.toLowerCase(), mailbox, analyzer);
              } catch (IllegalArgumentException e) {
                  throw exception("UNKNOWN_TEXT_AFTER_IS", term);
              }
          case CONV:
              return ConvQuery.create(mailbox, text);
          case CONV_COUNT:
              return ConvCountQuery.create(text);
          case DATE:
              return createDateQuery(DateQuery.Type.DATE, term, text);
          case DAY:
              return createDateQuery(DateQuery.Type.DAY, term, text);
          case WEEK:
              return createDateQuery(DateQuery.Type.WEEK, term, text);
          case MONTH:
              return createDateQuery(DateQuery.Type.MONTH, term, text);
          case YEAR:
              return createDateQuery(DateQuery.Type.YEAR, term, text);
          case AFTER:
              return createDateQuery(DateQuery.Type.AFTER, term, text);
          case BEFORE:
              return createDateQuery(DateQuery.Type.BEFORE, term, text);
          case CONV_START:
              return createDateQuery(DateQuery.Type.CONV_START, term, text);
          case CONV_END:
              return createDateQuery(DateQuery.Type.CONV_END, term, text);
          case APPT_START:
              return createDateQuery(DateQuery.Type.APPT_START, term, text);
          case APPT_END:
              return createDateQuery(DateQuery.Type.APPT_END, term,text);
          case TOFROM:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_TOFROM", term);
              }
              return AddrQuery.create(mailbox, analyzer,
                      EnumSet.of(Address.TO, Address.FROM), text);
          case TOCC:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_TOCC", term);
              }
              return AddrQuery.create(mailbox, analyzer,
                      EnumSet.of(Address.TO, Address.CC), text);
          case FROMCC:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_FROMCC", term);
              }
              return AddrQuery.create(mailbox, analyzer,
                      EnumSet.of(Address.FROM, Address.CC), text);
          case TOFROMCC:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_TOFROMCC", term);
               }
              return AddrQuery.create(mailbox, analyzer,
                      EnumSet.of(Address.TO, Address.FROM, Address.CC), text);
          case FROM:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_FROM", term);
              }
              return SenderQuery.create(mailbox, analyzer, text);
          case TO:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_TO", term);
              }
              return createAddrDomainQuery(LuceneFields.L_H_TO, text);
          case CC:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_CC", term);
              }
              return createAddrDomainQuery(LuceneFields.L_H_CC, text);
          case ENVTO:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_ENVTO", term);
              }
              return createAddrDomainQuery(LuceneFields.L_H_X_ENV_TO, text);
          case ENVFROM:
              if (Strings.isNullOrEmpty(text)) {
                  throw exception("MISSING_TEXT_AFTER_ENVFROM", term);
              }
              return createAddrDomainQuery(LuceneFields.L_H_X_ENV_FROM, text);
          case MODSEQ:
              return new ModseqQuery(text);
          case SIZE:
              return createSizeQuery(SizeQuery.Type.EQ, term, text);
          case BIGGER:
              return createSizeQuery(SizeQuery.Type.GT, term, text);
          case SMALLER:
              return createSizeQuery(SizeQuery.Type.LT, term, text);
          case SUBJECT:
              return SubjectQuery.create(mailbox, analyzer, text);
          case FIELD:
              int open = field.image.indexOf('[');
              if (open >= 0) {
                  int close = field.image.indexOf(']');
                  if (close >= 0 && close > open) {
                      text = field.image.substring(open + 1, close) + ":" + text;
                  }
              } else if (field.image.charAt(0) == '#') {
                  text = field.image.substring(1) + text;
              }
              return new TextQuery(mailbox, analyzer, LuceneFields.L_FIELD, text);
          default: //TODO reachable?
              return new TextQuery(mailbox, analyzer,
                      JJ2LUCENE.get(field.kind), text);
        }
    }

    private SizeQuery createSizeQuery(SizeQuery.Type type, Token term,
            String text) throws ParseException {
        try {
            return new SizeQuery(type, text);
        } catch (java.text.ParseException e) {
            throw exception("INVALID_SIZE", term);
        }
    }

    private DateQuery createDateQuery(DateQuery.Type type, Token term,
            String text) throws ParseException {
        DateQuery query = new DateQuery(type);
        try {
            query.parseDate(text, timezone, locale);
        } catch (java.text.ParseException e) {
            throw exception("INVALID_DATE", term);
        }
        return query;
    }

    private Query createAddrDomainQuery(String field, String term)
        throws ServiceException {

        if (term.startsWith("@")) {
            return new DomainQuery(mailbox, field, term);
        } else {
            return new TextQuery(mailbox, analyzer, field, term);
        }
    }

    private ParseException exception(String message, Token token) {
        ParseException e = new ParseException(message);
        e.currentToken = token;
        return e;
    }

}

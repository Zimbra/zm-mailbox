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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.ZimbraQuery.BaseQuery;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Parser for search query.
 *
 * @author ysasaki
 */
public final class QueryParser implements ParserConstants {

    private final Mailbox mailbox;
    private final Analyzer analyzer;
    private TimeZone timezone;
    private Locale locale;
    private String defaultField;
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
     * @param value field name
     */
    public void setDefaultField(String value) {
        defaultField = value;
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
    public List<BaseQuery> parse(String src) throws ServiceException {
        Parser parser = new Parser(new StringReader(src));
        SimpleNode node;
        try {
            node = parser.parse();
        } catch (TokenMgrError e) {
            throw MailServiceException.QUERY_PARSE_ERROR(src, e,
                    "", -1, e.getMessage());
        } catch (ParseException e) {
            throw MailServiceException.QUERY_PARSE_ERROR(src, e,
                    e.currentToken.image, e.currentToken.beginColumn, e.getMessage());
        }
        QueryParserVisitor visitor = new QueryParserVisitor(mailbox, analyzer);
        if (timezone != null) {
            visitor.setTimeZone(timezone);
        }
        if (locale != null) {
            visitor.setLocale(locale);
        }
        if (defaultField != null) {
            visitor.setDefaultField(defaultField);
        }
        try {
            node.jjtAccept(visitor, null);
        } catch (QueryParserException e) {
            throw MailServiceException.QUERY_PARSE_ERROR(src, e,
                    e.getText(), e.getErrorOffset(), e.getMessage());
        }
        sortBy = visitor.getSortBy();
        return visitor.getQuery();
    }

}

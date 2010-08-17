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
package com.zimbra.cs.index.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeUtility;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.util.StringUtil;

/**
 * RFC822 address tokenizer.
 * <p>
 * For example:
 * {@literal "Tim Brennan" <tim@bar.foo.com>} is tokenized as:
 * <ul>
 *  <li>tim
 *  <li>brennan
 *  <li>tim@bar.foo.com
 *  <li>tim
 *  <li>@bar.foo.com
 *  <li>bar.foo.com
 *  <li>foo
 * </ul>
 * <p>
 * We tokenize RFC822 addresses casually (relaxed parsing) and formally (strict
 * parsing). We do both in case strict parsing mistakenly strips tokens. This
 * way, we might have false hits, but won't have hit miss.
 *
 * @author ysasaki
 */
public final class RFC822AddressTokenStream extends TokenStream {

    private final List<String> tokens = new LinkedList<String>();
    private Iterator<String> itr;
    private final TermAttribute termAttr = addAttribute(TermAttribute.class);

    public RFC822AddressTokenStream(String raw) {
        if (StringUtil.isNullOrEmpty(raw)) {
            return;
        }

        String decoded;
        try {
            decoded = MimeUtility.decodeText(raw);
        } catch (UnsupportedEncodingException e) {
            decoded = raw;
        }

        // casually parse addresses, then tokenize them
        Set<String> emails = new HashSet<String>();
        Tokenizer tokenizer = new AddrCharTokenizer(new StringReader(decoded));
        TermAttribute term = tokenizer.addAttribute(TermAttribute.class);
        try {
            while (tokenizer.incrementToken()) {
                String token = term.term();
                if (token.length() > 1) { // ignore short term text
                    tokenize(token, emails);
                }
            }
            tokenizer.close();
        } catch (IOException ignore) {
        }

        // formally parse RFC822 addresses, then add them unless duplicate.
        // comments of RFC822 addr-spec are stripped out.
        for (InternetAddress iaddr : InternetAddress.parse(raw)) {
            tokenize(iaddr, emails);
        }

        itr = tokens.iterator();
    }

    private void tokenize(String src, Set<String> emails) {
        tokens.add(src);
        int at = src.lastIndexOf('@');
        if (at <= 0) { // not an email address
            return;
        }
        emails.add(src); // for duplicate check

        // split on @
        String localpart = src.substring(0, at);
        tokens.add(localpart);

        // now, split the local-part on the "."
        if (localpart.indexOf('.') > 0) {
            for (String part : localpart.split("\\.")) {
                tokens.add(part);
            }
        }

        if (src.endsWith("@")) { // no domain
            return;
        }
        String domain = src.substring(at + 1);
        tokens.add("@" + domain);
        tokens.add(domain);

        String[] parts = domain.split("\\.");
        if (parts.length > 1) { // "zimbra" of "lab.zimbra.com"
            String part = parts[parts.length - 2];
            tokens.add(part);
            tokens.add("@" + part); // weird, but for backward compatibility
        }
        //TODO: how about "zimbra" of "zimbra.co.jp"?
    }

    private void tokenize(InternetAddress iaddr, Set<String> emails) {
        String email = iaddr.getAddress();
        if (!StringUtil.isNullOrEmpty(email)) {
            email = email.toLowerCase();
            if (!emails.contains(email)) { // skip if duplicate
                tokenize(email, emails);
            }
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (itr.hasNext()) {
            termAttr.setTermBuffer(itr.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
        itr = tokens.iterator();
    }

    public List<String> getAllTokens() {
        return Collections.unmodifiableList(tokens);
    }

}

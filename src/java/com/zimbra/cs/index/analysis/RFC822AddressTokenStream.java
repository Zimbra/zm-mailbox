/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import com.zimbra.common.mime.InternetAddress;

/**
 * RFC822 address tokenizer.
 * <p>
 * For example:
 * {@literal "Zimbra Japan" <support@zimbra.vmware.co.jp>} is tokenized as:
 * <ul>
 *  <li>zimbra japan
 *  <li>zimbra
 *  <li>japan
 *  <li>support@zimbra.vmware.co.jp
 *  <li>support
 *  <li>@zimbra.vmware.co.jp
 *  <li>zimbra.vmware.co.jp
 *  <li>@vmware
 *  <li>vmware
 * </ul>
 * <p>
 * We tokenize RFC822 addresses casually (relaxed parsing) and formally (strict
 * parsing). We do both in case strict parsing mistakenly strips tokens. This
 * way, we might have false hits, but won't have hit miss.
 *
 * @author ysasaki
 */
public final class RFC822AddressTokenStream extends TokenStream {
    private static final int MAX_TOKEN_LEN = 256;
    private static final int MAX_TOKEN_COUNT = 100;

    private final List<String> tokens = new LinkedList<String>();
    private Iterator<String> itr;
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public RFC822AddressTokenStream(String raw) {
        if (Strings.isNullOrEmpty(raw)) {
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
        CharTermAttribute term = tokenizer.addAttribute(CharTermAttribute.class);
        try {
            while (tokenizer.incrementToken()) {
                if (term.length() == 1 && !Character.isLetter(term.charAt(0))) { // ignore single signs
                    continue;
                }
                tokenize(term.toString(), emails);
            }
            tokenizer.close();
        } catch (IOException ignore) {
        }

        // formally parse RFC822 addresses, then add them unless duplicate.
        // comments of RFC822 addr-spec are stripped out.
        for (InternetAddress iaddr : InternetAddress.parseHeader(raw)) {
            tokenize(iaddr, emails);
        }

        itr = tokens.iterator();
    }

    public RFC822AddressTokenStream(RFC822AddressTokenStream stream) {
        tokens.addAll(stream.tokens);
        itr = tokens.iterator();
    }

    private void tokenize(String src, Set<String> emails) {
        add(src);
        int at = src.lastIndexOf('@');
        if (at <= 0) { // not an email address
            return;
        }
        emails.add(src); // for duplicate check

        // split on @
        String localpart = src.substring(0, at);
        add(localpart);

        // now, split the local-part on the "."
        if (localpart.indexOf('.') > 0) {
            for (String part : localpart.split("\\.")) {
                add(part);
            }
        }

        if (src.endsWith("@")) { // no domain
            return;
        }
        String domain = src.substring(at + 1);
        add("@" + domain);
        add(domain);

        try {
            String top = InternetDomainName.fromLenient(domain).topPrivateDomain().parts().get(0);
            add(top);
            add("@" + top); // for backward compatibility
        } catch (IllegalArgumentException ignore) {
        } catch (IllegalStateException ignore) {
            // skip unless it's a valid domain
        }
    }

    private void tokenize(InternetAddress iaddr, Set<String> emails) {
        if (iaddr instanceof InternetAddress.Group) {
            InternetAddress.Group group = (InternetAddress.Group) iaddr;
            for (InternetAddress member : group.getMembers()) {
                tokenize(member, emails);
            }
        } else {
            String email = iaddr.getAddress();
            if (!Strings.isNullOrEmpty(email)) {
                email = email.toLowerCase();
                if (!emails.contains(email)) { // skip if duplicate
                    tokenize(email, emails);
                }
            }
        }
    }

    private void add(String token) {
        if (token.length() <= MAX_TOKEN_LEN && tokens.size() < MAX_TOKEN_COUNT) {
            tokens.add(token);
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (itr.hasNext()) {
            termAttr.setEmpty().append(itr.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
        itr = tokens.iterator();
    }

    @Override
    public void close() {
        tokens.clear();
    }

    public List<String> getAllTokens() {
        return Collections.unmodifiableList(tokens);
    }

}

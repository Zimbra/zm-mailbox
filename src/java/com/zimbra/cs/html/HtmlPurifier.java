/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.html;

import java.util.regex.Pattern;

import org.apache.xerces.xni.XMLString;
import org.cyberneko.html.filters.Purifier;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;


/**
 * @author zimbra
 *
 */
public class HtmlPurifier extends Purifier {
    private static final Pattern VALID_IMG_TAG = Pattern.compile("<\\s*img", Pattern.CASE_INSENSITIVE);
    private static final PolicyFactory sanitizer = Sanitizers.IMAGES;

    /* (non-Javadoc)
     * @see org.cyberneko.html.filters.Purifier#purifyText(org.apache.xerces.xni.XMLString)
     */
    @Override
    protected XMLString purifyText(XMLString text) {
        String temp = text.toString();

        if (VALID_IMG_TAG.matcher(temp).find()) {
            temp = sanitizer.sanitize(temp);
        }

        XMLString n = new XMLString();
        n.setValues(temp.toCharArray(), 0, temp.length());

        return super.purifyText(n);
    }

}

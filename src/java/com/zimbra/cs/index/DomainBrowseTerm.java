/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.util.EnumSet;
import java.util.Set;

/**
 * From/To/CC domain term.
 *
 * @author ysasaki
 */
public final class DomainBrowseTerm extends BrowseTerm {

    public enum Field {
        FROM, TO, CC;
    }

    private Set<Field> fields = EnumSet.noneOf(Field.class);

    public DomainBrowseTerm(BrowseTerm domain) {
        super(domain.term, domain.freq);
    }

    public String getDomain() {
        return term;
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public String getHeaderFlags() {
        StringBuilder result = new StringBuilder();
        if (fields.contains(Field.FROM)) {
            result.append('f');
        }
        if (fields.contains(Field.TO)) {
            result.append('t');
        }
        if (fields.contains(Field.CC)) {
            result.append('c');
        }
        return result.toString();
    }

    public boolean contains(Field field) {
        return fields.contains(field);
    }

}

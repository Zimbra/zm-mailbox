/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

    public DomainBrowseTerm(BrowseTerm term) {
        super(term.getText(), term.getFreq());
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

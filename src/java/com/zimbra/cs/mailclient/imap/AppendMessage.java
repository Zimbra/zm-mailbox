/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class AppendMessage {
    private Flags flags;
    private Date date;
    private List<Object> parts = new ArrayList<Object>();
    
    public AppendMessage() {}

    public AppendMessage(Flags flags, Date date, Object... parts) {
        flags(flags).date(date);
        for (Object part : parts) {
            part(part);
        }
    }
    
    public AppendMessage flags(Flags flags) {
        this.flags = flags;
        return this;
    }

    public AppendMessage date(Date date) {
        this.date = date;
        return this;
    }

    public AppendMessage part(Object obj) {
        if (obj instanceof String || obj instanceof Literal) {
            parts.add(obj);
        } else {
            throw new IllegalArgumentException("APPEND part must be url or literal");
        }
        return this;
    }

    boolean isCatenate() {
        return parts.size() > 1 || !parts.isEmpty() && parts.get(0) instanceof String;
    }

    public Flags flags() { return flags; }
    public Date date() { return date; }
    public List<Object> parts() { return parts; }

    public List<Object> getData() {
        List<Object> data = new ArrayList<Object>();
        if (flags != null) data.add(flags);
        if (date != null) data.add(date);
        if (isCatenate()) {
            data.add(CAtom.CATENATE);
            List<Object> list = new ArrayList<Object>(parts.size());
            for (Object part : parts) {
                if (part instanceof String) {
                    list.add(CAtom.URL);
                    list.add(new Quoted((String) part));
                } else {
                    list.add(CAtom.TEXT);
                    list.add(part);
                }
            }
            data.add(list);
        } else {
            data.addAll(parts);
        }
        return data;
    }
}

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.imap;

public class SearchCommand extends ImapCommand {
    private ImapSearch search;
    private Integer options;

    public SearchCommand(ImapSearch search, Integer options) {
        super();
        this.search = search;
        this.options = options;
    }

    public ImapSearch getSearch() {
        return search;
    }

    public Integer getOptions() {
        return options;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((options == null) ? 0 : options.hashCode());
        result = prime * result + ((search == null) ? 0 : search.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SearchCommand other = (SearchCommand) obj;
        if (options == null) {
            if (other.options != null) {
                return false;
            }
        } else if (!options.equals(other.options)) {
            return false;
        }
        if (search == null) {
            if (other.search != null) {
                return false;
            }
        } else if (!search.equals(other.search)) {
            return false;
        }
        return true;
    }
}

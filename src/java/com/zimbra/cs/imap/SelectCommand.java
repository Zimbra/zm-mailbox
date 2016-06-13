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

public class SelectCommand extends ImapCommand {

    private ImapPath path;
    byte params;
    QResyncInfo qri;

    /**
     * @param path
     * @param params
     * @param qri
     */
    public SelectCommand(ImapPath path, byte params, QResyncInfo qri) {
        super();
        this.path = path;
        this.params = params;
        this.qri = qri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + params;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((qri == null) ? 0 : qri.hashCode());
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
        SelectCommand other = (SelectCommand) obj;
        if (params != other.params) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (qri == null) {
            if (other.qri != null) {
                return false;
            }
        } else if (!qri.equals(other.qri)) {
            return false;
        }
        return true;
    }
}

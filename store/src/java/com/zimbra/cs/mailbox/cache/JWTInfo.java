/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.mailbox.cache;

import java.io.Serializable;

public class JWTInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private String salt;
    private long expiryTime;

    public JWTInfo() {
        // empty
    }

    public JWTInfo(String salt, long expiryTime) {
        this.salt = salt;
        this.expiryTime = expiryTime;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
         this.expiryTime = expiryTime;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (expiryTime ^ (expiryTime >>> 32));
        result = prime * result + ((salt == null) ? 0 : salt.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JWTInfo)) {
            return false;
        }
        JWTInfo other = (JWTInfo) obj;
        if (expiryTime != other.expiryTime) {
            return false;
        }
        if (salt == null) {
            if (other.salt != null) {
                return false;
            }
        } else if (!salt.equals(other.salt)) {
            return false;
        }
        return true;
    }
}

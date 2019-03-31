/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;


/**
 * @author zimbra
 *
 */
public class SecureObjectInputStream extends ObjectInputStream {

    /**
     * @param string
     * @param fileInputStream
     * @throws IOException
     * @throws SecurityException
     */

    private String acceptedClassname = null;
    protected SecureObjectInputStream() throws IOException, SecurityException {
        super();
    }

    /**
     * @param in
     * @throws IOException
     */
    public SecureObjectInputStream(InputStream in, String acceptedClassname) throws IOException {
        super(in);
        this.acceptedClassname = acceptedClassname;
    }

    /**
     * Only deserialize instances of known zimbra classes
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
        if (desc.getName().equals(this.acceptedClassname)) {
            return super.resolveClass(desc);
        }
        throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
    }
}

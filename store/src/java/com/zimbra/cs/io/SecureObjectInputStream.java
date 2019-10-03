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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.StringUtil;

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

    private Set<String> acceptedClassname;
    protected SecureObjectInputStream() throws IOException, SecurityException {
        super();
    }

    /**
     * @param in
     * @throws IOException
     */
    public SecureObjectInputStream(InputStream in, String acceptedClassname) throws IOException {
        super(in);
        this.acceptedClassname = new HashSet<String>();
        this.acceptedClassname.add(acceptedClassname);
    }

    /**
     * @param in
     * @throws IOException
     */
    public SecureObjectInputStream(InputStream in, Set<String> acceptedClassname) throws IOException {
        super(in);
        this.acceptedClassname = acceptedClassname;
    }

    /**
     * Only deserialize instances of known zimbra classes
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {

        String acceptedClassString = LC.zimbra_deserialize_classes.value();
        if (!StringUtil.isNullOrEmpty(acceptedClassString)) {
            this.acceptedClassname.addAll(Arrays.asList(acceptedClassString.split(",")));
        }

        if (desc.getName().startsWith("java.") || desc.getName().startsWith("[Ljava.")) {
            return super.resolveClass(desc);
        } else {
            for (String className: this.acceptedClassname) {
                if (desc.getName().equals(className)) {
                    return super.resolveClass(desc);
                }
            }
        }
        throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
    }
}

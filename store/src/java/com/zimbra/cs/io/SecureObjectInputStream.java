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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;


/**
 * @author zimbra
 *
 */
public class SecureObjectInputStream extends ObjectInputStream {


    /**
     * @throws IOException
     * @throws SecurityException
     */
    protected SecureObjectInputStream() throws IOException, SecurityException {
        super();

    }



    /**
     * @param in
     * @throws IOException
     */
    public SecureObjectInputStream(InputStream in) throws IOException {
        super(in);
    }



    /**
     * Only deserialize instances of known zimbra classes
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
        String[] config = {};
        try {
             config = Provisioning.getInstance().getConfig().getDeserializerWhiteList();
            for (int i = 0; i < config.length; ++i) {
                if (desc.getName().equals(config[i].getClass().getName())) {
                    return super.resolveClass(desc);
                }
            }
            throw new InvalidClassException( "Unauthorized deserialization attempt", desc.getName());
        } catch (ServiceException e) {
            ZimbraLog.misc.errorQuietly("Error fetching  white list of deseializable  classes", e);
            throw new IOException("Error fetching the white list of deseializable  classes");
        }
    }




}

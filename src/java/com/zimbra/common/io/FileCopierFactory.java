/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.io;

import com.zimbra.common.io.FileCopierOptions.IOType;
import com.zimbra.common.io.FileCopierOptions.Method;

public class FileCopierFactory {

    public static FileCopier createCopier(FileCopierOptions opts) {
        return createCopier(opts.getMethod(),
                            opts.getIOType(),
                            opts.getOIOCopyBufferSize(),
                            opts.getAsyncQueueCapacity(),
                            opts.getNumPipes(),
                            opts.getNumReadersPerPipe(),
                            opts.getNumWritersPerPipe(),
                            opts.getPipeBufferSize());
    }

    public static FileCopier createCopier(
            Method method, IOType ioType, int oioCopyBufSize,
            int queueSize,
            int numPipes, int readConcurrency, int writeConcurrency,
            int pipeBufSize) {
        FileCopier copier;
        switch (method) {
        case PARALLEL:
            copier = new AsyncFileCopier(ioType.equals(IOType.NIO), oioCopyBufSize, queueSize, readConcurrency);
            break;
        case PIPE:
            copier = new AsyncPipedFileCopier(
                    ioType.equals(IOType.NIO), oioCopyBufSize,
                    queueSize, numPipes, readConcurrency, writeConcurrency,
                    pipeBufSize);
            break;
        case SERIAL:
            copier = new SerialFileCopier(ioType.equals(IOType.NIO), oioCopyBufSize);
            break;
        default:
            throw new IllegalArgumentException("Invalid method " + method);
        }
        return copier;
    }
}

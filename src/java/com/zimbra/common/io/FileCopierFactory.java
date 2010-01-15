/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.io;

import com.zimbra.common.io.FileCopierOptions.IOType;
import com.zimbra.common.io.FileCopierOptions.Method;

public class FileCopierFactory {

    public static FileCopier createCopier(FileCopierOptions opts) {
        return createCopier(opts.getMethod(),
                            opts.getIOType(),
                            opts.getOIOCopyBufferSize(),
                            opts.getAsyncQueueCapacity(),
                            opts.getNumParallelWorkers(),
                            opts.getNumPipes(),
                            opts.getNumReadersPerPipe(),
                            opts.getNumWritersPerPipe(),
                            opts.getPipeBufferSize());
    }

    public static FileCopier createCopier(
            Method method, IOType ioType, int oioCopyBufSize,
            int queueSize, int parallelWorkers,
            int numPipes, int readConcurrency, int writeConcurrency,
            int pipeBufSize) {
        FileCopier copier;
        switch (method) {
        case PARALLEL:
            copier = new AsyncFileCopier(
                    ioType.equals(IOType.NIO), oioCopyBufSize, 
                    queueSize, parallelWorkers);
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

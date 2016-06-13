/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FileCopierSpec {

    /**
     * @zm-api-field-tag file-copier-method
     * @zm-api-field-description File copier method - <b>PARALLEL | PIPE | SERIAL</b>
     */
    @XmlAttribute(name=BackupConstants.A_FC_METHOD /* fcMethod */, required=false)
    private String method;

    /**
     * @zm-api-field-description fcIOTYpe - <b>OIO | NIO</b>.  For all methods
     */
    @XmlAttribute(name=BackupConstants.A_FC_IOTYPE /* fcIOType */, required=false)
    private String iotype;

    /**
     * @zm-api-field-description fcOIOCopyBufferSize in bytes.   For all methods
     */
    @XmlAttribute(name=BackupConstants.A_FC_OIO_COPY_BUFSIZE /* fcOIOCopyBufferSize */, required=false)
    private Integer oioCopyBufSize;

    /**
     * @zm-api-field-description fcAsyncQueueCapacity.  For PARALLEL and PIPE only
     */
    @XmlAttribute(name=BackupConstants.A_FC_ASYNC_QUEUE_CAPACITY /* fcAsyncQueueCapacity */, required=false)
    private Integer asyncQueueCapacity;

    /**
     * @zm-api-field-description fcParallelWorkers.  For PARALLEL only
     */
    @XmlAttribute(name=BackupConstants.A_FC_PARALLEL_WORKERS /* fcParallelWorkers */, required=false)
    private Integer parallelWorkers;

    /**
     * @zm-api-field-description fcPipes.  For PIPE only
     */
    @XmlAttribute(name=BackupConstants.A_FC_PIPES /* fcPipes */, required=false)
    private Integer pipes;

    /**
     * @zm-api-field-description fcPipeBufferSize. For PIPE only
     */
    @XmlAttribute(name=BackupConstants.A_FC_PIPE_BUFFER_SIZE /* fcPipeBufferSize */, required=false)
    private Integer pipeBufferSize;

    /**
     * @zm-api-field-description fcPipeReadersPerPipe. For PIPE only
     */
    @XmlAttribute(name=BackupConstants.A_FC_PIPE_READERS /* fcPipeReadersPerPipe */, required=false)
    private Integer pipeReaders;

    /**
     * @zm-api-field-description fcPipeWritersPerPipe. Ffor PIPE only
     */
    @XmlAttribute(name=BackupConstants.A_FC_PIPE_WRITERS /* fcPipeWritersPerPipe */, required=false)
    private Integer pipeWriters;

    public FileCopierSpec() {
    }

    public void setMethod(String method) { this.method = method; }
    public void setIotype(String iotype) { this.iotype = iotype; }
    public void setOioCopyBufSize(Integer oioCopyBufSize) {
        this.oioCopyBufSize = oioCopyBufSize;
    }
    public void setAsyncQueueCapacity(Integer asyncQueueCapacity) {
        this.asyncQueueCapacity = asyncQueueCapacity;
    }
    public void setParallelWorkers(Integer parallelWorkers) {
        this.parallelWorkers = parallelWorkers;
    }
    public void setPipes(Integer pipes) { this.pipes = pipes; }
    public void setPipeBufferSize(Integer pipeBufferSize) {
        this.pipeBufferSize = pipeBufferSize;
    }
    public void setPipeReaders(Integer pipeReaders) { this.pipeReaders = pipeReaders; }
    public void setPipeWriters(Integer pipeWriters) { this.pipeWriters = pipeWriters; }
    public String getMethod() { return method; }
    public String getIotype() { return iotype; }
    public Integer getOioCopyBufSize() { return oioCopyBufSize; }
    public Integer getAsyncQueueCapacity() { return asyncQueueCapacity; }
    public Integer getParallelWorkers() { return parallelWorkers; }
    public Integer getPipes() { return pipes; }
    public Integer getPipeBufferSize() { return pipeBufferSize; }
    public Integer getPipeReaders() { return pipeReaders; }
    public Integer getPipeWriters() { return pipeWriters; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("method", method)
            .add("iotype", iotype)
            .add("oioCopyBufSize", oioCopyBufSize)
            .add("asyncQueueCapacity", asyncQueueCapacity)
            .add("parallelWorkers", parallelWorkers)
            .add("pipes", pipes)
            .add("pipeBufferSize", pipeBufferSize)
            .add("pipeReaders", pipeReaders)
            .add("pipeWriters", pipeWriters);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}

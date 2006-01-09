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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ozserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.apache.commons.logging.Log;

import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.util.Zimbra;

public class OzTLSFilter extends OzFilter {
    
    private static SSLContext mSSLContext;
    
    static {
        try {
            char[] passphrase = "zimbra".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(LC.tomcat_keystore.value()), passphrase);
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            mSSLContext = SSLContext.getInstance("TLS");
            mSSLContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            Zimbra.halt("exception initializing TLS context", e);
        }
    }

    public static SSLContext getTLSContext() {
        return mSSLContext;
    }
    
    private SSLEngine mSSLEngine;
    private Object mHandshakeLock = new Object();
    private HandshakeStatus mHandshakeStatus;
    private boolean mHandshakeComplete;
    private ByteBuffer mHandshakeBB;
    
    private OzConnection mConnection;

    private final boolean mDebug;
    private final boolean mTrace;
    
    private Log mLog;

    private int mPacketBufferSize;
    private int mApplicationBufferSize;
    
    public OzTLSFilter(OzConnection connection, Log log) {
        mSSLEngine = mSSLContext.createSSLEngine();
        mSSLEngine.setUseClientMode(false);
        mHandshakeStatus = HandshakeStatus.NEED_UNWRAP;
        mHandshakeComplete = false;
        mConnection = connection;
        mPacketBufferSize = mSSLEngine.getSession().getPacketBufferSize();
        mApplicationBufferSize = mSSLEngine.getSession().getApplicationBufferSize();
        mHandshakeBB = ByteBuffer.allocate(0);

        mLog = log;
        mDebug = mLog.isDebugEnabled();
        mTrace = mLog.isTraceEnabled();
        
        if (mDebug) debug("appsize=" + mApplicationBufferSize + " pktsize=" + mPacketBufferSize);

        if (false) {
            debug("EnabledCipherSuites=" + Arrays.deepToString(mSSLEngine.getEnabledCipherSuites()));
            debug("SupportedCipherSuites=" + Arrays.deepToString(mSSLEngine.getSupportedCipherSuites()));
            debug("EnabledProtocols=" + Arrays.deepToString(mSSLEngine.getEnabledProtocols()));
            debug("SupportedProtocols=" + Arrays.deepToString(mSSLEngine.getSupportedProtocols()));
        }

    }

    private void handshakeCompleted() throws IOException {
        if (mDebug) debug("handshake completed");
        mHandshakeComplete = true;
        processPendingWrites();
        mConnection.enableReadInterest();
    }
    
    private boolean handshake(ByteBuffer rbb) throws IOException {
        synchronized (mHandshakeLock) {
            handshakeLocked(rbb);
            return mHandshakeComplete;
        }
    }

    private void handshakeLocked(ByteBuffer rbb) throws IOException {
        if (mHandshakeComplete) {
            if (mDebug) debug("handshake deferred: already complete");
            return;
        }
        
        if (rbb != null && !rbb.hasRemaining()) {
            if (mDebug) debug("handshake deferred: no bytes in input");
            return;
        }
        
        if (mConnection.isWritePending()) {
            if (mDebug) debug("handshake deferred: write pending");
            return;
        }

        switch (mHandshakeStatus) {
        case NEED_UNWRAP:
            unwrap: while (mHandshakeStatus == HandshakeStatus.NEED_UNWRAP) {
                ByteBuffer unwrappedBB = ByteBuffer.allocate(mApplicationBufferSize);
                rbb.flip();
                SSLEngineResult result = mSSLEngine.unwrap(rbb, unwrappedBB);
                rbb.compact();
                if (mDebug) debug("handshake: unwrap result " + result);
                mHandshakeStatus = result.getHandshakeStatus();
                switch (result.getStatus()) {
                case OK:
                    switch(mHandshakeStatus) {
                    case NOT_HANDSHAKING: 
                        throw new IOException("NOT_HANDSHAKING while handshaking");
                    case NEED_TASK:
                        mHandshakeStatus = runTasks();
                        break;
                    case FINISHED:
                        handshakeCompleted();
                        break unwrap;
                    }
                    break;

                case BUFFER_UNDERFLOW:
                    if (mDebug) debug("handshake: need more bytes");
                    mConnection.enableReadInterest();
                    break unwrap;
                    
                case BUFFER_OVERFLOW:
                case CLOSED:
                    throw new IOException("TLS handshake: in unwrap got " + result.getStatus());
                }
            }
            if (mHandshakeStatus != HandshakeStatus.NEED_WRAP) {
                break;
            }
            /* Falling through, because while unwrapping, we were asked to wrap. */
            
        case NEED_WRAP:
            wrap: while (true) {
                ByteBuffer dest = ByteBuffer.allocate(mPacketBufferSize);
                SSLEngineResult result = mSSLEngine.wrap(mHandshakeBB, dest);
                dest.flip();
                mHandshakeStatus = result.getHandshakeStatus();
                if (mDebug) debug("handshake: wrap result " + result);
                switch (result.getStatus()) {
                case OK:
                    if (mHandshakeStatus == HandshakeStatus.NEED_TASK) {
                        mHandshakeStatus = runTasks();
                    }
                    if (mDebug) debug("handshake: writing bytes");
                    getNextFilter().write(dest, true);
                    if (mHandshakeStatus != HandshakeStatus.NEED_WRAP) {
                        break wrap;
                    }
                    break;
                case BUFFER_OVERFLOW:
                case BUFFER_UNDERFLOW:
                case CLOSED:
                    throw new IOException("TLS handshake: in wrap got status " + result.getStatus());
                }
            }
            break;
        
        default:
            throw new RuntimeException("TLS handshake: can not deal with handshake status: " + mHandshakeStatus);
        }
    }
    
    private HandshakeStatus runTasks() {
        Runnable runnable;
        while ((runnable = mSSLEngine.getDelegatedTask()) != null) {
            if (mDebug) debug("run task " + runnable);
            runnable.run();
        }
        
        return mSSLEngine.getHandshakeStatus();
    }
    
    public ByteBuffer read(ByteBuffer rbb) throws IOException {
        if (mDebug) debug("read: readBB: " + rbb);

        if (!handshake(rbb)) {
            return null; 
        }
        
        SSLEngineResult result;
        ByteBuffer unwrappedBB = ByteBuffer.allocate(mApplicationBufferSize);
        
        do {
            rbb.flip();

            if (mDebug) debug("read: invoking unwrap");
            result = mSSLEngine.unwrap(rbb, unwrappedBB);
            if (mTrace) trace(OzUtil.byteBufferDebugDump("read: after unwrap unwrappedBB", unwrappedBB, true));
            if (mTrace) trace(OzUtil.byteBufferDebugDump("read: after unwrap readBB", rbb, true));
            
            rbb.compact();
            
            if (mDebug) debug("read: unwrap result " + result);
            
            switch (result.getStatus()) {
            case BUFFER_UNDERFLOW:
            case CLOSED:
            case OK:
                if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                    runTasks();
                }
                break;
            case BUFFER_OVERFLOW:
                throw new IOException("TLS filter: SSLEngine error during read: " + result.getStatus());
            }
        } while (rbb.position() != 0 && result.getStatus() == Status.OK);
        
        return unwrappedBB;
    }
    
    private void resizeUnwrappedBB() {
        // TODO Auto-generated method stub
    }

    private ByteBuffer mWrappedBB = ByteBuffer.allocate(1024); // REMIND - size this

    List<ByteBuffer> mPendingWriteBuffers = new LinkedList<ByteBuffer>();
    
    private boolean mPendingFlush;
    
    public void writeCompleted() throws IOException {
        if (mDebug) debug("write completed: checking if handshake complete: " + mHandshakeStatus);

        if (isClosePending()) {
            if (mDebug) debug("write completed: close pending");
            if (!isNextStageClosed()) {
                initiateNextStageClose();
            }
            return;
        }
        
        switch (mHandshakeStatus) {
        case FINISHED:
            handshakeCompleted();
            break;
            
        case NEED_UNWRAP:
            mConnection.enableReadInterest();
            break;
            
        case NEED_WRAP:
            handshake(null);
            break;
        }
    }

    public void processPendingWrites() throws IOException {
        synchronized (mPendingWriteBuffers) {
            if (mDebug) debug("write: pending buffers size " + mPendingWriteBuffers.size());
            
            for (Iterator<ByteBuffer> iter = mPendingWriteBuffers.iterator(); iter.hasNext();) {
                ByteBuffer srcbb = iter.next();
                ByteBuffer dest = ByteBuffer.allocate(mPacketBufferSize);
                if (mTrace) trace(OzUtil.byteBufferDebugDump("application buffer before wrap", srcbb, false));
                SSLEngineResult result = mSSLEngine.wrap(srcbb, dest);
                if (mTrace) trace(OzUtil.byteBufferDebugDump("network buffer after wrap", dest, true));
                if (mDebug) debug("write wrap result " + result);
                switch (result.getStatus()) {
                case OK:
                    if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                        runTasks();
                    }
                    dest.flip();
                    getNextFilter().write(dest, mPendingFlush);
                    iter.remove();
                    break;
                case BUFFER_OVERFLOW:
                case BUFFER_UNDERFLOW:
                case CLOSED:
                    throw new RuntimeException("can not handle wrap status: " + result.getStatus());
                }
            }
        }
    }
    
    public void write(ByteBuffer src, boolean flush) throws IOException {
        if (mDebug) debug("write called");
        synchronized (mPendingWriteBuffers) {
            if (!mHandshakeComplete) {
                mPendingWriteBuffers.add(src);
                if (flush) {
                    mPendingFlush = true;
                }
                if (mDebug) debug("write: handshake in progress, queueing inside filter, flush state=" + mPendingFlush);
                return;
            }

            if (!mPendingFlush && !flush) {
                if (mDebug) debug("write: flush not requested, queueing inside filter");
                mPendingWriteBuffers.add(src);
                return;
            }
            mPendingWriteBuffers.add(src);
            processPendingWrites();
        }
    }

    public void closeNow() throws IOException {
        if (mDebug) debug("closeNow");
        mSSLEngine.closeInbound();
        getNextFilter().closeNow();
    }

    private Object mCloseLock = new Object();
    private boolean mClosePending = false;
    private boolean mClosedNextStage = false;
    
    private boolean isClosePending() {
        synchronized (mCloseLock) {
            return mClosePending;
        }
    }

    private void initiateNextStageClose() throws IOException {
        synchronized (mCloseLock) {
            mClosedNextStage = true;
            getNextFilter().close();
        }
    }
    
    private boolean isNextStageClosed() {
        synchronized (mCloseLock) {
            return mClosedNextStage;
        }
    }
    
    private void initiateClose() {
        if (mDebug) debug("initiating close");
        synchronized (mCloseLock) {
            if (!mClosePending) {
                mSSLEngine.closeOutbound();
                mClosePending = true;
            }
        }
    }

    public void close() throws IOException {
        if (mDebug) debug("close");
    
        initiateClose();

        /* "fire and forget" close_notify */
        ByteBuffer dest = ByteBuffer.allocate(mPacketBufferSize);
        SSLEngineResult result = mSSLEngine.wrap(mHandshakeBB, dest);
        if (result.getStatus() != Status.CLOSED) {
            throw new SSLException("Improper close state");
        }
        dest.flip();
        getNextFilter().write(dest, true);
    }

    private void debug(String msg) {
        mLog.debug("TLS: " + msg);
    }

    private void trace(String msg) {
        mLog.trace("TLS: " + msg);
    }

    public int getPreferredReadBufferSize() {
        return mPacketBufferSize;
    }
}

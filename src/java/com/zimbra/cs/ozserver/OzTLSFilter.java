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
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.apache.commons.logging.Log;

import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.util.Zimbra;

public class OzTLSFilter implements OzFilter {
    
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
    private HandshakeStatus mHandshakeStatus;
    private boolean mHandshakeComplete;
    
    private OzConnection mConnection;

    private final boolean mDebug;
    private Log mLog;
    private ByteBuffer mHandshakeBB;
    private ByteBuffer mUnwrappedBB;

    private ByteBuffer mReadBB;
    
    private int mPacketBufferSize;
    private int mApplicationBufferSize;
    
    public OzTLSFilter(OzConnection connection) {
        mSSLEngine = mSSLContext.createSSLEngine();
        mSSLEngine.setUseClientMode(false);
        mHandshakeStatus = HandshakeStatus.NEED_UNWRAP;
        mHandshakeComplete = false;
        mConnection = connection;
        mPacketBufferSize = mSSLEngine.getSession().getPacketBufferSize();
        mApplicationBufferSize = mSSLEngine.getSession().getApplicationBufferSize();
        mHandshakeBB = ByteBuffer.allocate(0);
        mReadBB = ByteBuffer.allocate(mPacketBufferSize);
        mLog = connection.getLog();
        mDebug = mLog.isDebugEnabled();
        
        if (mDebug) debug("appsize=" + mApplicationBufferSize + " pktsize=" + mPacketBufferSize);

        if (false) {
            debug("EnabledCipherSuites=" + Arrays.deepToString(mSSLEngine.getEnabledCipherSuites()));
            debug("SupportedCipherSuites=" + Arrays.deepToString(mSSLEngine.getSupportedCipherSuites()));
            debug("EnabledProtocols=" + Arrays.deepToString(mSSLEngine.getEnabledProtocols()));
            debug("SupportedProtocols=" + Arrays.deepToString(mSSLEngine.getSupportedProtocols()));
        }

    }

    private boolean handshake() throws IOException {
        synchronized (mReadBB) {
            handshake0(mReadBB);
            if (!mReadBB.hasRemaining()) {
            }
            return mHandshakeComplete;
        }
    }

    private void handshake0(ByteBuffer rbb) throws IOException {
        if (mHandshakeComplete) {
            if (mDebug) debug("handshake deferred: already complete");
            return;
        }
        
        if (!rbb.hasRemaining()) {
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
                if (mDebug) debug("handshake unwrap result " + result);
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
                        if (mDebug) debug("handshake finished");
                        mHandshakeComplete = true;
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
            /* Falling through, because while unwrapping, we are asked to wrap. */
            
        case NEED_WRAP:
            ByteBuffer dest = ByteBuffer.allocate(mPacketBufferSize);
            dest.clear();
            SSLEngineResult result = mSSLEngine.wrap(mHandshakeBB, dest);
            mHandshakeStatus = result.getHandshakeStatus();
            if (mDebug) debug("handshake wrap result " + result);
            switch (result.getStatus()) {
            case OK:
                switch (mHandshakeStatus) {
                case NEED_TASK: 
                    mHandshakeStatus = runTasks();
                    // fall through
                case FINISHED:
                case NEED_UNWRAP:
                case NEED_WRAP:
                    if (mDebug) debug("in wrap handshake status was " + result.getStatus());
                    dest.flip();
                    mConnection.writeQueuePrepend(dest, true);
                }
                break;
            case BUFFER_OVERFLOW:
            case BUFFER_UNDERFLOW:
            case CLOSED:
                throw new IOException("TLS handshake: XXX in wrap got status " + result.getStatus());
            }
            break;
        default:
            throw new RuntimeException();
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
    
    public void read() throws IOException {
        if (mDebug) debug("readBB position=" + mReadBB.position() + " limit=" + mReadBB.limit() + " capacity=" + mReadBB.capacity());

        if (!handshake()) {
            return; 
        }
        
        resizeUnwrappedBB();
        
        mReadBB.flip();
        SSLEngineResult result = mSSLEngine.unwrap(mReadBB, mUnwrappedBB);
        mReadBB.compact();
        if (mDebug) debug("read unwrap result " + result);
        switch (result.getStatus()) {
        case BUFFER_UNDERFLOW:
        case OK:
            switch (result.getHandshakeStatus()) {
            case NEED_TASK:
                runTasks();
                break;
            case NEED_UNWRAP:
            case FINISHED:
            case NOT_HANDSHAKING:
            default:
                throw new RuntimeException("TLS filter: can't yet handle: " + result.getHandshakeStatus());
            }
            break;
        case BUFFER_OVERFLOW:
        case CLOSED:
            throw new IOException("TLS filter: SSLEngine error during read: " + result.getStatus());
        }
    }
    
    private void resizeUnwrappedBB() {
        // TODO Auto-generated method stub
    }

    private ByteBuffer mWrappedBB = ByteBuffer.allocate(1024); // REMIND - size this

    List<ByteBuffer> mPendingWriteBuffers = new LinkedList<ByteBuffer>();
    
    private boolean mPendingFlush;
    
    public void wrote(int totalWritten) throws IOException {
        if (mDebug) mLog.debug("write completed " + totalWritten + " written try continue handshake");
        handshake();
    }

    public void write(ByteBuffer src, boolean flush) throws IOException {
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
            if (mDebug) debug("write: pending buffers size " + mPendingWriteBuffers.size());
            
            for (ByteBuffer srcbb : mPendingWriteBuffers) {
                ByteBuffer dest = ByteBuffer.allocate(1024);
                SSLEngineResult result = mSSLEngine.wrap(srcbb, dest);
                if (mDebug) debug("write wrap result " + result);
                switch (result.getStatus()) {
                case OK:
                    switch (result.getHandshakeStatus()) {
                    case NEED_TASK:
                        runTasks();
                    case FINISHED:
                    case NEED_UNWRAP:
                    case NEED_WRAP:
                    case NOT_HANDSHAKING:
                        throw new RuntimeException("can not handle wrap HANDSHAKE status: " + result.getHandshakeStatus());
                    }
                case BUFFER_OVERFLOW:
                case BUFFER_UNDERFLOW:
                case CLOSED:
                    throw new RuntimeException("can not handle wrap status: " + result.getStatus());
                }
            }
        }
    }

    public void close() throws IOException {
        mSSLEngine.closeInbound();
    }
    
    public void shutdown() throws IOException {
        // TODO
    }

    private void debug(String msg) {
        mLog.debug("TLS filter: " + msg);
    }

    public ByteBuffer getReadBuffer() {
        return mReadBB;
    }
}

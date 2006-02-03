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
    private HandshakeStatus mHandshakeStatus;
    private boolean mHandshakeComplete;
    private ByteBuffer mHandshakeBB;
    
    private OzConnection mConnection;

    private final boolean mDebug;
    private final boolean mTrace;
    
    private Log mLog;

    private int mPacketBufferSize;
    private int mApplicationBufferSize;
    
    public OzTLSFilter(OzConnection connection, boolean logBuffers, Log log) {
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
        mTrace = logBuffers;
        
        mUnwrapped = ByteBuffer.allocate(mApplicationBufferSize);
        
        if (mTrace) trace("appsize=" + mApplicationBufferSize + " pktsize=" + mPacketBufferSize);

        if (false) {
            trace("EnabledCipherSuites=" + Arrays.deepToString(mSSLEngine.getEnabledCipherSuites()));
            trace("SupportedCipherSuites=" + Arrays.deepToString(mSSLEngine.getSupportedCipherSuites()));
            trace("EnabledProtocols=" + Arrays.deepToString(mSSLEngine.getEnabledProtocols()));
            trace("SupportedProtocols=" + Arrays.deepToString(mSSLEngine.getSupportedProtocols()));
        }

    }

    private void handshakeCompleted() throws IOException {
        if (mDebug) debug("handshake completed");
        mHandshakeComplete = true;
        processPendingWrites();
        mConnection.enableReadInterest();
    }
    
    private boolean handshake(ByteBuffer rbb) throws IOException {
        if (mHandshakeComplete) {
            if (mTrace) trace("handshake deferred: already complete");
            return mHandshakeComplete;
        }
        
        if (rbb != null && !rbb.hasRemaining()) {
            if (mDebug) debug("handshake deferred: no bytes in input");
            return mHandshakeComplete;
        }
        
        getNextFilter().waitForWriteCompletion();
        
        switch (mHandshakeStatus) {
        case NEED_UNWRAP:
            unwrap: while (mHandshakeStatus == HandshakeStatus.NEED_UNWRAP) {
                rbb.flip();
                ensureUnwrappedCapacity();
                SSLEngineResult result = mSSLEngine.unwrap(rbb, mUnwrapped);
                rbb.compact();
                if (mDebug) debug("handshake: unwrap result " + toString(result));
                mHandshakeStatus = result.getHandshakeStatus();
                switch (result.getStatus()) {
                case OK:
                    switch(mHandshakeStatus) {
                    case NOT_HANDSHAKING: 
                        throw new IOException("TLS handshake: NOT_HANDSHAKING while handshaking");
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
                if (mDebug) debug("handshake: wrap result " + toString(result));
                switch (result.getStatus()) {
                case OK:
                    if (mDebug) debug("handshake: writing wrapped bytes");
                    getNextFilter().write(dest);
                    if (mHandshakeStatus == HandshakeStatus.NEED_TASK) {
                        mHandshakeStatus = runTasks();
                    }
                    switch (mHandshakeStatus) {
                    case FINISHED:
                        handshakeCompleted();
                        break wrap;
                    case NEED_UNWRAP:
                        mConnection.enableReadInterest();
                        break wrap;
                    case NEED_WRAP:
                        break;
                    default:
                        throw new IOException("TLS handshake: can not deal with handshake status (wrap): " + mHandshakeStatus);
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
            throw new IOException("TLS handshake: can not deal with handshake status: " + mHandshakeStatus);
        }
        
        return mHandshakeComplete;
    }
    
    private HandshakeStatus runTasks() {
        Runnable runnable;
        while ((runnable = mSSLEngine.getDelegatedTask()) != null) {
            runnable.run();
        }
        
        return mSSLEngine.getHandshakeStatus();
    }
    
    private ByteBuffer mUnwrapped;
    
    private void ensureUnwrappedCapacity() {
        if (mUnwrapped.remaining() < mApplicationBufferSize) {
            // Expand buffer for large request                                                                                                                                  
            ByteBuffer bb = ByteBuffer.allocate(mUnwrapped.capacity() * 2);
            mUnwrapped.flip();
            bb.put(mUnwrapped);
            mUnwrapped = bb;
        }
    }
    
    public void read(ByteBuffer rbb) throws IOException {

        if (!handshake(rbb)) {
            return; 
        }

        SSLEngineResult result;
        while (rbb.position() != 0) {
            rbb.flip();

            ensureUnwrappedCapacity();

            String ibefore = null, obefore = null;
            if (mDebug) ibefore = OzUtil.toString(rbb);
            if (mDebug) obefore = OzUtil.toString(mUnwrapped); 
            
            result = mSSLEngine.unwrap(rbb, mUnwrapped);
            
            if (mDebug) debug("read unwrap in: " + ibefore + "->" + OzUtil.toString(rbb));
            if (mDebug) debug("read unwrap out: " + obefore + "->" + OzUtil.toString(mUnwrapped));
            if (mDebug) debug("read unwrap result: " + toString(result));
            if (mTrace) trace(OzUtil.byteBufferDebugDump("read unwrapped", mUnwrapped, true));
            
            rbb.compact();
            
            switch (result.getStatus()) {
            case BUFFER_UNDERFLOW:
            case OK:
                if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                    runTasks();
                }
                break;
            default: // CLOSED BUFFER_OVERFLOW
                throw new IOException("TLS: invalid status during read: " + result.getStatus());
            }
            
            getNextFilter().read(mUnwrapped);
            
            if (result.getStatus() == Status.OK) {
            	continue;
            }
            
            if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
            	mConnection.enableReadInterest();
            }
            break;
        }
    }
    
    private static String toString(SSLEngineResult result) {
    	return "Status=" + result.getStatus() + 
    	       " HSStatus=" + result.getHandshakeStatus() +
    	       " consumed=" + result.bytesConsumed() +
    	       " produced=" + result.bytesProduced();
	}

	List<ByteBuffer> mPendingWriteBuffers = new LinkedList<ByteBuffer>();
    
    public void processPendingWrites() throws IOException {
        synchronized (mPendingWriteBuffers) {

            for (Iterator<ByteBuffer> iter = mPendingWriteBuffers.iterator(); iter.hasNext();) {
                ByteBuffer srcbb = iter.next();
                while (srcbb.hasRemaining()) {
                	ByteBuffer dest = ByteBuffer.allocate(mPacketBufferSize);

                    if (mTrace) trace(OzUtil.byteBufferDebugDump("write wrapping ", srcbb, false));
                    String ibefore = null, obefore = null;
                    if (mDebug) ibefore = OzUtil.toString(srcbb);
                    if (mDebug) obefore = OzUtil.toString(dest); 
                    
                    SSLEngineResult result = mSSLEngine.wrap(srcbb, dest);

                    if (mDebug) debug("read unwrap in: " + ibefore + "->" + OzUtil.toString(srcbb));
                    if (mDebug) debug("read unwrap out: " + obefore + "->" + OzUtil.toString(dest));
                    if (mDebug) debug("read unwrap result: " + toString(result));
                    
                    
                    switch (result.getStatus()) {
                	case OK:
                		if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                			runTasks();
                		}
                		dest.flip();
                		getNextFilter().write(dest);
                		break;
                	default: // CLOSED BUFFER_OVERFLOW BUFFER_UNDERFLOW:
                		throw new IOException("TLS: invalid status during write: " + result.getStatus());
                	}
                }
                iter.remove();
            }
        }
    }
    
    public void write(ByteBuffer src) throws IOException {
        if (mTrace) trace("write: called");
        synchronized (mPendingWriteBuffers) {
            if (!mHandshakeComplete) {
                mPendingWriteBuffers.add(src);
                if (mDebug) debug("write: handshake in progress, queueing inside filter");
                return;
            }
            mPendingWriteBuffers.add(src);
            if (mTrace) trace("write: processing pending writes");
            processPendingWrites();
        }
    }

    private Object mCloseLock = new Object();

    private boolean mClosedOutbound = false;
    
    public void close() throws IOException {
        if (mDebug) debug("close");
    
        synchronized (mCloseLock) {
            if (mClosedOutbound) {
            	if (mDebug) debug("close - already in progress");
            	return;
            } else {
            	if (mDebug) debug("close - closing outbound");
                mSSLEngine.closeOutbound();
                mClosedOutbound = true;
            }
        }

        /* "fire and forget" close_notify */
        SSLEngineResult result = null;
        do {
            ByteBuffer dest = ByteBuffer.allocate(mPacketBufferSize);
            result = mSSLEngine.wrap(mHandshakeBB, dest);
            if (mDebug) debug("close wrap result " + toString(result));
            if (result.getStatus() != Status.CLOSED) {
                throw new SSLException("Improper close state");
            }	
            dest.flip();
            getNextFilter().write(dest);
        } while (result != null && result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP);

        getNextFilter().close();
    }

    private void debug(String msg) {
        mLog.debug("TLS: " + msg);
    }

    private void trace(String msg) {
        mLog.trace("TLS: " + msg);
    }

	public void waitForWriteCompletion() throws IOException {
		// TODO implement this - WriteManger inside OzConnection should be
		// factored out so we can share code. This will be needed when there is
		// another filter on top of the TLS filter such as the SASL filter.
	}
}

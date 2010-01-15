/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ThreadPool;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Zimbra;

/**

  Model writers via a state machine.  4 possible states:
    - CLOSED    
    - WRITING
    - IDLE
    - FLUSHING

   Valid state transitions:
      I->W - BeginWriting 
      C->W - BeginWriting
      W->I - DoneWriting
      I->F - BeginFlush
      F->C - FlushCompleted

    All writes must happen between a BeginWriting()...DoneWriting() pair.  The pair should be inside a synchronized block.

    Threads block in the following places:
      1) If they're trying to open, and need a slot (mWaitingForSlot list)
      2) In various states, if they're being flushed by another thread (latch on the writer)
      3) The sweeper waits on the global cache lock

 */
class IndexWritersCache {
	// all open writers
	private Set<IndexWriter> mOpenWriters = new LinkedHashSet<IndexWriter>();

	// writers in the IDLE state
	private Set<IndexWriter> mIdleWriters = new LinkedHashSet<IndexWriter>();

	/**
	 * How often do we walk the list of open IndexWriters looking for idle writers
	 * to close.  On very busy systems, the default time might be too long.
	 */
	private final long mSweeperTimeout = LC.zimbra_index_sweep_frequency.intValueWithinRange(1, 3600) * Constants.MILLIS_PER_SECOND;


	/**
	 * After we add a document to it, how long do we hold an index open for writing before closing it 
	 * (and therefore flushing the writes to disk)?
	 * 
	 * Note that there are other things that might cause us to flush the index to disk -- e.g. if the user
	 * does a search on the index, or if the system decides there are too many open IndexWriters (see 
	 * sLRUSize) 
	 */
	private final long mMaxIdleTime = 1000 * LC.zimbra_index_idle_flush_time.intValueWithinRange(1,60*60*24);

	/**
	 * How many open indexWriters do we allow?  This value is here so that the # open file 
	 * descriptors and amount of buffered index memory is controlled.
	 */
	private final int mMaxOpen = LC.zimbra_index_lru_size.intValueWithinRange(10, Integer.MAX_VALUE);   // maximum allowed in the LRU
	private final int mFlushPoolSize = LC.zimbra_index_flush_pool_size.intValueWithinRange(1, 100);

	private int mNumFlushing = 0; // number queued or running async flush tasks
	private boolean mShutdown = false;
	private Thread mSweeperThread = null;
	private List<CountDownLatch> mWaitingForSlot = new ArrayList<CountDownLatch>();

	private ThreadPool mPool = new ThreadPool("IndexWriterFlush", mFlushPoolSize);

	enum WriterState {
		CLOSED,
		WRITING,
		IDLE,
		FLUSHING;
	}


	public IndexWritersCache() {
		Runnable sweeper = new Runnable() {
			public void run() {
				doSweep();
			}
		};
		mSweeperThread = new Thread(sweeper, "IndexWriterSweeper");
		mSweeperThread.start();
	}

	static abstract class IndexWriter {
		/**
		 * Must handle (presumably a NoOp) the case where the writer is already open
		 * @throws IOException
		 */
		abstract void doWriterOpen() throws IOException;
		abstract void doWriterClose();
		abstract long getLastWriteTime();

		public WriterState getState() {
			return mState;
		}
		public void setState(WriterState state) {
			mState = state;
		}
		public CountDownLatch getFlushWaiterLatch() {
			return mFlushWaiterLatch;
		}
		public void resetFlushWaiterLatch() {
			mFlushWaiterLatch = new CountDownLatch(1);
		}

		private WriterState mState = WriterState.CLOSED;
		private CountDownLatch mFlushWaiterLatch = new CountDownLatch(1);
	}

	private int mNumCacheHits = 0;
	private int mNumCacheOpens = 0;

	public synchronized boolean isWriterOpen(IndexWriter w) {
		return (w.getState() != WriterState.CLOSED);
	}

	public void flushAllWriters() {
		List<IndexWriter> toFlush = new ArrayList<IndexWriter>();
		synchronized(this) {
			toFlush.addAll(mOpenWriters);
		}
		for (IndexWriter w : toFlush) {
			flush(w);
		}
	}

	public void beginWriting(IndexWriter w) throws IOException {
		assert(!Thread.holdsLock(this));
		boolean done = false;
		ZimbraPerf.COUNTER_IDX_WRT_OPENED.increment();
		WriterState curState = null;
		do {
			CountDownLatch l = null;
			synchronized(this) {
				curState = w.getState();
				switch (curState) {
				case FLUSHING:
					l = w.getFlushWaiterLatch();
					assert(!mIdleWriters.contains(w));
					break;
				case IDLE:
					assert(mIdleWriters.contains(w));
					mIdleWriters.remove(w);
					assert(!mIdleWriters.contains(w));
					w.setState(WriterState.WRITING);
					done = true;
					ZimbraPerf.COUNTER_IDX_WRT_OPENED_CACHE_HIT.increment();
					break;
				case WRITING:
					assert(false);
					done = true;
					break;
				case CLOSED:
					assert(!mIdleWriters.contains(w));
					// not already idle -- find a slot for us

					// is there a slot for us?
							if (mOpenWriters.size() < mMaxOpen) {
								done = true;
								// at this point we have a slot!
								mOpenWriters.add(w);
								w.setState(WriterState.WRITING);
							} else {
								// nope
								l = new CountDownLatch(1);
								mWaitingForSlot.add(l);
								this.notifyAll();
							}
					break;
				}
			}
			if (!done) {
				try {
					if (ZimbraLog.index.isDebugEnabled())
						ZimbraLog.index.debug("Blocked in beginWriting for "+w+" because state was "+curState+
								" NumFlushing="+mNumFlushing);
					l.await();
				} catch (InterruptedException e) {}
			}

			ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
		} while (!done);
		try {
			// at this point we have a slot, and we're in the active list
			w.doWriterOpen();
		} catch (IOException e) {
			openFailed(w);
			throw e;
		}
	}

	private synchronized void openFailed(IndexWriter w) {
		w.setState(WriterState.CLOSED);
		mIdleWriters.remove(w);
		mOpenWriters.remove(w);
		ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
		if (!mWaitingForSlot.isEmpty()) {
			CountDownLatch l = mWaitingForSlot.remove(0);
			l.countDown();
		}
		w.getFlushWaiterLatch().countDown();
		w.resetFlushWaiterLatch();
		this.notifyAll();
	}

	public synchronized void doneWriting(IndexWriter w) {
		assert(w.getState() == WriterState.WRITING);
		assert(!mIdleWriters.contains(w));
		assert(mOpenWriters.contains(w));
		w.setState(WriterState.IDLE);
		mIdleWriters.add(w);
		this.notifyAll();
	}

	public void shutdown() {
		synchronized(this) {
			mShutdown = true;
			this.notifyAll();
			while (!mOpenWriters.isEmpty()) {
				this.notifyAll();
				try {
					this.wait(100);
				} catch (InterruptedException e) {}
			}
		}
		try {
			mSweeperThread.join();
			mPool.shutdown();
		} catch (InterruptedException e) {}
	}

	/**
	 * Called in the sweeper thread
	 */
	 private void doSweep() {
		try {
			while(true) {
				Set<IndexWriter> toFlush = new HashSet<IndexWriter>();
				long idleCutoff = System.currentTimeMillis() - mMaxIdleTime;
				synchronized(this) {
					if (mShutdown && mOpenWriters.isEmpty())
						return;

					// timeouts
					for (IndexWriter w : mIdleWriters) {
						assert(w.getState() == WriterState.IDLE);
						if (mShutdown || (w.getLastWriteTime() < idleCutoff)) {
							toFlush.add(w);
						}
					}

					int additionalNeeded = mWaitingForSlot.size() - (mNumFlushing + toFlush.size());
					if (additionalNeeded > 0) {
						for (IndexWriter w : mIdleWriters) {
							assert(w.getState() == WriterState.IDLE);
							if (additionalNeeded <= 0)
								break;
							if (!toFlush.contains(w)) {
								additionalNeeded--;
								toFlush.add(w);
							}
						}
					}

					if (toFlush.isEmpty()) {
						try {
							this.wait(mSweeperTimeout);
						} catch (InterruptedException e) { }
					} else {
						for (IndexWriter w : toFlush) {
							assert(w.getState() == WriterState.IDLE);
							mNumFlushing++;
							w.setState(WriterState.FLUSHING);
							mIdleWriters.remove(w);
						}
					}
				}

				// submit outside the global lock, so we don't deadlock if the
				// work queue fills up
				for (IndexWriter w : toFlush) {
					AsyncFlush af = new AsyncFlush(w);
					try {
						mPool.execute(af);
					} catch (OutOfMemoryError e) {
						Zimbra.halt("OutOfMemory in IndexWritersCache.doSweep", e);
					} catch (InterruptedException e) {
						ZimbraLog.index.debug("Sweeper hit interruptedException attempting to async flush "+w+". Flushing synchronously.");
						flushInternal(w);
					} catch (Throwable t) {
						System.err.println("Error! "+t);
						synchronized(this) {
							mNumFlushing--;
							w.setState(WriterState.IDLE);
							w.getFlushWaiterLatch().countDown();
							w.resetFlushWaiterLatch();
							this.notifyAll();
							mIdleWriters.add(w);
						}
					}
				}
			}
		} catch (OutOfMemoryError e) {
			Zimbra.halt("OutOfMemory in IndexWritersCache.doSweep", e);
		}
	 }

	 class AsyncFlush implements Runnable {
		 private IndexWriter mTarget;

		 AsyncFlush(IndexWriter target) {
			 mTarget = target;
		 }

		 public void run() {
			 try {
				 flushInternal(mTarget);
			 } catch (OutOfMemoryError e) {
				 Zimbra.halt("OutOfMemory in IndexWritersCache.AsyncFlush", e);
			 } catch (Throwable t) {
				 ZimbraLog.index.warn("Caught exception in Async Index Flush: ", t);
			 }
		 }
	 }

	 void flush(IndexWriter target) {
		 synchronized(this) {
			 while (target.getState() == WriterState.FLUSHING) {
				 // already flushing...have to wait for that flush to finish
				 // so that we can return
				 try {
					 this.wait(100); 
				 } catch (InterruptedException e) {}
			 }
			 if (target.getState() == WriterState.CLOSED)
				 return;
			 if (target.getState() == WriterState.WRITING)
				 doneWriting(target);

			 assert(target.getState() == WriterState.IDLE);
			 mNumFlushing++;
			 target.setState(WriterState.FLUSHING);
			 mIdleWriters.remove(target);
		 }
		 flushInternal(target);
	 }

	 private void flushInternal(IndexWriter target) {
		 assert(!Thread.holdsLock(this));
		 synchronized(this) {
			 assert(target.getState() == WriterState.FLUSHING);
		 }
		 try {
			 target.doWriterClose();
		 } finally {
			 synchronized(this) {
				 assert(target.getState() == WriterState.FLUSHING);
				 assert(mOpenWriters.contains(target));
				 assert(!mIdleWriters.contains(target));
				 mOpenWriters.remove(target);
				 ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
				 target.setState(WriterState.CLOSED);
				 mNumFlushing--;
				 if (!mWaitingForSlot.isEmpty()) {
					 CountDownLatch l = mWaitingForSlot.remove(0);
					 l.countDown();
				 }
				 target.getFlushWaiterLatch().countDown();
				 target.resetFlushWaiterLatch();
				 this.notifyAll();
			 }
		 }
	 }

	 ////////////////////////////////////////////////////////////////////////////////////
	 // test harness code below here
	 ////////////////////////////////////////////////////////////////////////////////////

	 static class TestWriter extends IndexWriter {

		 Random r;
		 boolean opened = false;

		 long randomLong(long max) {
			 synchronized(r) {
				 long l = r.nextLong();
				 if (l < 0)
					 l = -1 * l;
				 return l % max;
			 }
		 }

		 public void doWriterOpen() throws IOException {
			 if (!opened) {
				 try {
					 if (r.nextFloat() > 0.9)
						 throw new IOException("Foo");
					 if (false)
						 Thread.sleep(1);
					 opened = true;
				 } catch (InterruptedException e) {}
			 }
		 }

		 public void doWriterClose() {
			 assert(opened);
			 try {
				 //                System.out.println("BeginWriterClose: "+this);
				 if (r.nextFloat() > 0.999)
					 throw new IllegalArgumentException("foo");
				 long length = randomLong(100);
				 if (length > 0)
					 Thread.sleep(length);
				 opened = false;
				 //                System.out.println("DoneWriterClose: "+this);
			 } catch (InterruptedException e) {}
		 }

		 public long getLastWriteTime() {
			 return mLastAccessTime;
		 }

		 public void write() {
			 synchronized(this) {
				 try {
					 mCache.beginWriting(this);
				 } catch (IOException e) {
					 e.printStackTrace();
				 }
				 assert(opened);
				 try {
					 if (false)
						 Thread.sleep(1);
				 } catch (InterruptedException e) {}
				 assert(opened);
				 mCache.doneWriting(this);
				 mLastAccessTime = System.currentTimeMillis();
			 }
		 }

		 public void flush() {
			 synchronized(this) {
				 if (opened) {
					 try {
						 if (false)
							 Thread.sleep(50);
					 } catch (InterruptedException e) {}
				 }
				 mCache.flush(this);
			 }
		 }

		 TestWriter(Random r, IndexWritersCache cache) {
			 this.r = r;
			 mCache = cache;
		 }

		 WriterState mState = WriterState.CLOSED;
		 long mLastAccessTime = System.currentTimeMillis();
		 IndexWritersCache mCache;
	 }

	 static class TestThread extends Thread {
		 int mNumIters;
		 Random mR;
		 IndexWritersCache mCache;
		 List<TestWriter> mWriters;
		 int mSleepTime;
		 int mWritePct;
		 int mFlushPct;
		 volatile int mCurrentIteration;
		 TestThread(IndexWritersCache cache, List<TestWriter> writers, int numIters, int writePct, int flushPct) {
			 mCache = cache;
			 mWriters = writers;
			 mWritePct = writePct;
			 mFlushPct = flushPct;
			 mNumIters = numIters;
			 mR = new Random(System.currentTimeMillis()+this.getId());
		 }
		 public void run() {
			 for (int i = 0; i < mNumIters; i++) {
				 mCurrentIteration = i;

				 //                if (i>0 && i % 100000 == 0)
					 //                    System.out.println("Thread "+this.toString()+" On iter "+i);
				 //                try {
				 //                    Thread.sleep(1);
				 //                } catch (InterruptedException ex) {};
				 int dpct = mR.nextInt(1000);
				 if (dpct <= (mWritePct + mFlushPct)) {
					 TestWriter w = mWriters.get(mR.nextInt(mWriters.size()));
					 synchronized(w) {
						 if (dpct <= mFlushPct) {
							 //                            System.out.println("Flushing");
							 //                            mCache.flush(w);
						 } else {
							 try {
								 //                                System.out.println("Writing");
								 mCache.beginWriting(w);
								 try {
									 Thread.sleep(5);
								 } catch (InterruptedException e) {}
								 mCache.doneWriting(w);
							 } catch (IOException e) {
								 //                                System.err.println("Caught IOException: "+e);
								 //                                e.printStackTrace();
							 }
						 }
					 }
				 } else {
					 System.out.println("Sleeping...");
					 //                    try {
					 //                        Thread.sleep(5);
					 //                    } catch (InterruptedException e) {}
				 }
			 }
		 }
	 }

	 /**
	  * @param args
	  */
	 public static void main(String[] args) {
		 int cacheSize = 20;
		 int numWriters = 200;
		 int numThreads = 10;
		 int sweepFrequencyS = 1;
		 int idleFlushTimeS = 2000;

		 int flushDeciPercent = 1; // 1%
		 int writeDeciPercent = 999; // 99%

		 ZimbraLog.toolSetupLog4j("WARN", "/tmp/iwc_test.txt");
		 LC.zimbra_index_lru_size.setDefault(Integer.toString(cacheSize));
		 LC.zimbra_index_sweep_frequency.setDefault(Integer.toString(sweepFrequencyS));
		 LC.zimbra_index_idle_flush_time.setDefault(Integer.toString(idleFlushTimeS));
		 //       LC.zimbra_index_flush_pool_size.setDefault(Integer.toString(100));

		 IndexWritersCache cache = new IndexWritersCache();
		 Random r = new Random(System.currentTimeMillis());

		 List<TestWriter> writers = new ArrayList<TestWriter>();
		 for (int i = 0; i < numWriters; i++) {
			 TestWriter w = new TestWriter(r, cache); 
			 writers.add(w);
		 }

		 if (false) {
			 int preloadNum = Math.min(cacheSize, numWriters);

			 System.out.println("Preloading "+preloadNum+" writers into cache"); 
			 // preload
			 for (int i = 0; i < preloadNum; i++) {
				 TestWriter w = writers.get(i);
				 synchronized(w) {
					 try {
						 cache.beginWriting(w);
						 cache.doneWriting(w);
					 } catch (IOException e) {
						 e.printStackTrace();
					 }
				 }
			 }
		 }

		 long start = System.currentTimeMillis();

		 System.out.println("Beginning run w/ cache size = "+cacheSize+
				 " numWriters="+numWriters+
				 " numThreads="+numThreads);

		 //       cache.flushAllWriters();

		 synchronized(cache) {
			 cache.mNumCacheHits = 0;
			 cache.mNumCacheOpens = 0;
		 }

		 TestThread[] t = new TestThread[numThreads];
		 for (int i = 0; i < numThreads; i++) {
			 t[i] = new TestThread(cache, writers, Integer.MAX_VALUE, writeDeciPercent, flushDeciPercent);
		 }

		 for (int i = 0; i < numThreads; i++) 
			 t[i].start();


		 boolean stop = false;
		 while(!stop) {
			 try {
				 Thread.sleep(3000);
			 } catch (InterruptedException e) {}
			 int lowestIter = Integer.MAX_VALUE;
			 int highestIter = -1;
			 for (TestThread tt : t) {
				 int threadIter = tt.mCurrentIteration;
				 lowestIter = Math.min(lowestIter, threadIter);
				 highestIter = Math.max(highestIter, threadIter);
			 }
			 System.out.println("Lowest iter is "+lowestIter+" Highest="+highestIter+
					 " NumFlushing="+cache.mNumFlushing);
		 }


		 for (int i = 0; i < numThreads; i++) {
			 try {
				 t[i].join();
			 } catch (InterruptedException e) {
				 System.err.println("Interrupted: "+e);
				 e.printStackTrace();
			 }
		 }

		 cache.mShutdown = true;
		 //       try {
		 //           sweeperThread.join();
		 //       } catch (InterruptedException e) {
		 //           System.err.println("Interrupted: "+e);
		 //           e.printStackTrace();
		 //       }

		 long end  = System.currentTimeMillis();

		 long total = end - start;
		 System.out.println("Test Done w/ cache="+cacheSize+" Took "+ total+"ms");

		 cache.shutdown();
	 }
}


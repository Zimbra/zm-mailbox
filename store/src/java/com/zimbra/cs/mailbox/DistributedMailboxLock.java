package com.zimbra.cs.mailbox;

import java.util.Stack;
import org.redisson.Redisson;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import com.zimbra.common.util.ZimbraLog;

public class DistributedMailboxLock {
	private Config config;
	private RedissonClient redisson;
	private RReadWriteLock lock;
	// use for VM
	private final static String HOST = "192.168.99.100";
	// use on docker-containers
	// private final static String HOST = "redis";
	private final static String PORT = "6379";
	private final Stack<Boolean> lockStack = new Stack<Boolean>();

	public DistributedMailboxLock(final String id, final Mailbox mbox) {
		try {
			config = new Config();
			config.useSingleServer().setAddress(HOST + ":" + PORT);
			redisson = Redisson.create(config);
			lock = redisson.getReadWriteLock("mailbox:" + id);
		} catch (Exception e) {
			ZimbraLog.system.fatal("Can't instantiate Redisson server", e);
			System.exit(1);
		}
	}

	public void lock() {
		lock(true);
	}

	public void lock(final boolean write) {
		try {
			tryLock(write);
		} catch (InterruptedException e) {
			ZimbraLog.mailbox.error(e.getMessage());
			throw new LockFailedException("interrupted", e);
		}
	}

	/**
	 * tryLock
	 *
	 * @param write
	 *            true for writelock, false for readlock
	 * @return true if the lock was acquired and false otherwise
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	private void tryLock(boolean write) throws InterruptedException {
		if (write) {
			lock.writeLock().lock();
		} else {
			lock.readLock().lock();
		}
		if (lock.writeLock().isLocked() || lock.readLock().isLocked()) {
			lockStack.push(write);
		}

	}

	public void release() {
		boolean isWriter = lockStack.pop();
		if (isWriter) {
			lock.writeLock().unlock();
		} else {
			lock.readLock().unlock();
		}
	}

	public boolean isWriteLockedByCurrentThread() {
		return lock.writeLock().isHeldByCurrentThread();
	}

	int getHoldCount() {
		return lock.writeLock().getHoldCount() + lock.readLock().getHoldCount();
	}

	public boolean isUnlocked() {
		return !isWriteLockedByCurrentThread() && lock.readLock().getHoldCount() == 0;
	}

	public void shutdown() {
		redisson.shutdown();
	}

	public final class LockFailedException extends RuntimeException {
		private static final long serialVersionUID = -6899718561860023270L;

		private LockFailedException(String message) {
			super(message);
		}

		private LockFailedException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}

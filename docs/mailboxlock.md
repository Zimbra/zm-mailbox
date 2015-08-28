# Mailbox Lock
The `MailboxLock` class exists primarily to isolate each transaction and maintain consistency between transactions. 

# Usage
The mailbox lock may be obtained by calling `Mailbox.lock.lock()` and released by `Mailbox.lock.release()`.

The lock must be held for as small a time as possible and released as soon as done. The release must be placed in a `finally` block so that it occurs even if there is an error.

```java
try {
  lock.lock();
  doWhatever();
} finally {
  lock.release();
}
```

The lock is obtained internally when a Mailbox transaction is started by `beginTransaction()` and released when `endTransaction()` is called. Those calls must also be balanced and wrapped in a `finally` block.

# Read/Write locking
Since 8.5 the `MailboxLock` has supported read/write locking. This allows multiple concurrent read accesses to proceed as long as there is no write access. Many of the frequently used read transactions have been coverted, and any new read access should be implemented as a read lock. To obtain a read lock, simply pass false to the lock call `lock.lock(false)', or call 'beginReadTransaction()' when starting a transactions.

It is important to note that read/write locking does not support lock promotion, since the underlying 'ReentrantReadWriteLock` does not support it. This means that a caller may not obtain a read lock and then convert it to a write lock; the caller must obtain a write lock as the first lock action. Asserts have been added to the code to protect against this condition during development. Developers are strongly urged to run their JVM with the -ea argument, especially when dealing with locking issues.

# Timeout and Too Many Waiters
The mailbox lock protects against system-wide thread starvation by maintaining limits on the number of waiting threads and the length of time a thread may wait for a lock. Occassionally a mailbox may be in a specific state which provokes a 'too many waiters' or 'lock timeout' error. These can of course be caused by code ineffeciency, environmental conditions, or many other factors. The logging provides a stack trace so the code may be analyzed and improved if necessary. A few occassional errors are annoying but not overly problematic, but continual errors are a sign of trouble.

# Tuning
Generally the default configuration is sufficient for most environments. However, heavily shared mailboxes may encounter higher traffic which pushes them beyond the limits for a normal mailbox. If continual errors occur and they affect users accessing shared mailboxes; the limits may be increased modestly to try to alleviate the situation. This should usually be done in conjunction with review of thread dumps and user data to confirm the environmental profile.

*Note that these values have recently been migrated into LDAP in the main branch, so older branches have similar values specified in LC. The values are listed as <new LDAP attr name>/<old LC key>*

- zimbraMailBoxLockTimeout/zimbra_mailbox_lock_timeout - Timeout in seconds to wait for a lock. Defaults to 60 seconds. Can be increased to let end users wait longer while long running transactions occur. Waiting threads are not available to process other requests, so allowing too long a wait will eventually starve the system. 
- zimbraMailboxLockMaxWaitingThreads/zimbra_mailbox_lock_max_waiting_threads - How many threads to queue for a given mailbox lock before rejecting. Defaults to 15. This limits the backlog that can occur if a mailbox lock is held for too long. Allowing more waiting threads can alleviate errors for heavily used mailboxes, but can lead to starvation.

# Legacy locking code
Versions prior to 8.0 used `synchronized` blocks instead of a `MailboxLock` object. This did not allow for timeouts and therefore could cause an entire system to become frozen due to one problematic mailbox. Important to remember for sites which experience occassional lock timeouts or 'too many waiters' errors. While annoying, those errors pale in comparison to a system wide outage. 

# Debugging
The combination of stack traces from mailbox.log and Java thread dumps is usually sufficient to identify the source of a lock contention issue or deadlock. However, details about read lock owners are not available in jstack output due to a Java internal limitation; and the full stack trace of each rejected thread is not output to avoid flooding the log. The `DebugConfig` key debug_mailbox_lock may be set to true to add these additional details to the log output.


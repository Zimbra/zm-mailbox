package com.zimbra.cs.mailbox;

import com.zimbra.soap.base.WaitSetResp;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

public class DistributedWaitSet {
    public static DistributedWaitSet getInstance() {
        return InstanceHolder.instance;
    }

    private RedissonClient redisson = RedissonClientHolder.getInstance().getRedissonClient();

    // eric: We've had a difficult time unraveling WaitSet business logic.
    // Our understanding is that WaitSet's represent an interest in changes made to a specific account id. This is
    // tracked via cb.signaledAccounts in WaitSetRequest#staticHandle
    /**
     * Notify downstream subscribers that a change has occurred.
     */
	public long publish(final String accountId, final WaitSetResp resp) {
		final RTopic<WaitSetResp> topic = redisson.getTopic(accountId);
		return topic.publish(resp);
	}

    /**
     * Use this method to register a subscriber that will be called when the target Redis topic is published to.
     * Should be called on a thread other than the thread being used to publish via DistributedWaitSet#publish
     */
	public void subscribe(final String accountId, final MessageListener<WaitSetResp> respListener) {
		final RTopic<WaitSetResp> topic = redisson.getTopic(accountId);
		topic.addListener(respListener);
	}

    private static class InstanceHolder {
        public static DistributedWaitSet instance = new DistributedWaitSet();
    }
}

package com.zimbra.cs.analytics;

import java.util.List;

import com.google.common.collect.Lists;

/** A wrapper around a sequence of behaviors associated with a message.
 * Represents a time series of behaviors that can then be classified into Priority or Not Priority.
 * The input sequence doesn't have to be sorted, as this class can do it.
 * @author iraykin
 *
 */
public class MessageBehaviorSet {

	private Integer msgId;
	private List<MessageBehavior> behaviors;

	public MessageBehaviorSet(Integer msgId) {
		this.msgId = msgId;
		this.behaviors = Lists.newArrayList();;
	}

	public MessageBehaviorSet(Integer msgId, List<MessageBehavior> behaviors) {
		this.msgId = msgId;
		this.behaviors = behaviors;
	}

	public Integer getMessageId() {
		return msgId;
	}

	public List<MessageBehavior> getBehaviors() {
		return behaviors;
	}

	public void addBehavior(MessageBehavior b) {
		behaviors.add(b);
	}
}

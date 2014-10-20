/**
 * 
 */
package com.zimbra.cs.analytics;


/**
 * @author Greg Solovyev
 * Represents some behavior applied to a message.
 */
public class MessageBehavior {
	public enum BehaviorType {
		READ, REPLIED, DELETED, UNDELETED, SPAMMED, UNSPAMMED, FORWARDED,
		RECIEVED, /* message was received in the user's inbox */
		SEEN,  /* user has seen the message in a mail client without necesseraly reading it */ 
		CONTENT /* interacted with content, such as clicked on a link */
	}
	private final long occured; /* when a behavior occurred */
	private final BehaviorType type; /* what kind of behavior occurred */
	private final String value; /* some value that can be assigned to a behavior */
	private final String accountId; /* ID of the account that performed the behavior */
	private final int itemId; /* ID of the mail item on which the behavior was performed */
	public MessageBehavior(String who, BehaviorType did, int what, long when, String how) {
		occured = when;
		type = did;
		value = how;
		accountId = who;
		itemId = what;
	}
	public long getTime() {
		return occured;
	}
	public BehaviorType getType() {
		return type;
	}
	public String getValue() {
		return value;
	}
	public String getAccountId() {
		return accountId;
	}
	public int getItemId() {
		return itemId;
	}
}

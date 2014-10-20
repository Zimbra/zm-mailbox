package com.zimbra.cs.ml;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.analytics.BehaviorManager;
import com.zimbra.cs.analytics.MessageBehaviorSet;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.ml.RuleBasedClassifier.Precedence;
/**
 *
 * @author iraykin
 *
 */
public class TrainingSetUpdate {
	private Mailbox mbox;
	private Integer behaviorTimeFrame;
	private TimeUnit timeUnit;

	public TrainingSetUpdate(Mailbox mbox, Integer behaviorTimeFrame, TimeUnit timeUnit) {
		this.mbox = mbox;
		this.behaviorTimeFrame = behaviorTimeFrame;
		this.timeUnit = timeUnit;
	}

	public void update(BehaviorManager manager, BehaviorClassifier classifier, DbConnection conn) throws ServiceException {
		long endTime = new Date().toInstant().toEpochMilli();
		long startTime = endTime - timeUnit.toMillis(behaviorTimeFrame);
		update(mbox, manager, classifier, conn, startTime, endTime);
	}

	public static void update(Mailbox mbox, BehaviorManager manager,
			BehaviorClassifier classifier, DbConnection conn,
			long startTime, long endTime) throws ServiceException {
		Collection<MessageBehaviorSet> behaviorSets = manager.findBehaviorsByTime(mbox.getAccountId(), startTime, endTime);
		TrainingSet trainingSet = mbox.getTrainingSet();
		for (MessageBehaviorSet behaviorSet: behaviorSets) {
			InternalLabel label = classifier.classify(behaviorSet);
			if (label != InternalLabel.UNCLASSIFIED) {
				trainingSet.addItem(behaviorSet.getMessageId(), label, conn, true);
			}
		}
	}

	/**Assembles the predefined rules into a behavior classifier */
	public static BehaviorClassifier getStandardClassifier() {

		RuleBasedClassifier classifier = new RuleBasedClassifier();
		classifier.addNextRule(BehaviorRules.PRIORITY_WHEN_REPLYING_QUICKLY, Precedence.NORMAL);
		classifier.addNextRule(BehaviorRules.PRIORITY_WHEN_INTERACTING_WITH_CONTENT, Precedence.NORMAL);
		classifier.addNextRule(BehaviorRules.NOT_PRIORITY_WHEN_DELETING_QUICKLY, Precedence.NORMAL);
		classifier.addNextRule(BehaviorRules.NOT_PRIORITY_WHEN_DELAYED_READ, Precedence.NORMAL);
		classifier.addNextRule(BehaviorRules.NOT_PRIORITY_WHEN_FLAGGING_AS_SPAM, Precedence.FINAL);
		return classifier;
	}
}

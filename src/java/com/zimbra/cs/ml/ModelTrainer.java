package com.zimbra.cs.ml;

import java.util.Collections;
import java.util.List;

import org.apache.mahout.classifier.sgd.AdaptiveLogisticRegression;
import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.classifier.sgd.L1;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.OperationContext;

/** A class that uses a TrainingSet to actually create a trained classifier. Can also perform validation.
 *
 * @author iraykin
 *
 */
public class ModelTrainer {

	protected float percentTest;
	protected Integer numDimensions;
	private float minAccuracy = -1;
	protected static final Integer DEFAULT_NUM_DIMENSIONS = 1000;

	public ModelTrainer(float percentTest) {
		this(percentTest, DEFAULT_NUM_DIMENSIONS);
	}

	public ModelTrainer(float percentTest, Integer numDimensions) {
		setPercentTest(percentTest);
		setDimensions(numDimensions);
	}

	public void setPercentTest(float percentTest) {
		this.percentTest = percentTest;
	}

	public void setDimensions(Integer numDimensions) {
		this.numDimensions = numDimensions;
	}

	public void setMinAccuracy(float minAccuracy) {
		if (minAccuracy <= 1 && minAccuracy >= 0) {
			this.minAccuracy = minAccuracy;
		}
	}

	public Pair<MahoutClassifier, CrossValidation> train(TrainingSet trainingSet, FeatureExtractor extractor,
			DbConnection conn, OperationContext octxt) throws ServiceException {
		List<TrainingItem> trainingSetItems = trainingSet.getItems(conn, octxt);
		Collections.shuffle(trainingSetItems);
		int cutoffIdx = Math.round((1 - percentTest) * trainingSetItems.size());
		List<TrainingItem> trainingData = trainingSetItems.subList(0, cutoffIdx);
		List<TrainingItem> testData = null;
		if (cutoffIdx < trainingSetItems.size()) {
			testData = trainingSetItems.subList(cutoffIdx, trainingSetItems.size() - 1);
		}
		MahoutClassifier classifier = trainInternal(trainingData, extractor, conn, octxt);
		ZimbraLog.analytics.info(String.format("training classifier using %s messages", trainingData.size()));
		CrossValidation validation = null;
		if (testData != null && !testData.isEmpty()) {
			ZimbraLog.analytics.info(String.format("cross-validating classifier using %s test", testData.size()));
			ZimbraLog.analytics.info(String.format("minimum classifier accuracy: %s", minAccuracy));
			validation = validate(classifier, testData, extractor);
		}
		return new Pair<MahoutClassifier, CrossValidation> (classifier, validation);
	}

	private MahoutClassifier trainInternal(List<TrainingItem> trainingData, FeatureExtractor extractor,
			DbConnection conn, OperationContext octxt) throws ServiceException {
		AdaptiveLogisticRegression reg = new AdaptiveLogisticRegression(InternalLabel.numDimensions(), numDimensions, new L1());
		for (TrainingItem item: trainingData) {
			reg.train(item.getInternalLabel().getId(), item.getFeatures(extractor).getVector());
		}
		reg.close();
		CrossFoldLearner learner = reg.getBest().getPayload().getLearner();
		MahoutClassifier classifier = new MahoutClassifier(learner);
		return classifier;
	}


	/** Train the classifier, check the cross-validation results to make sure
	 * the accuracy is high enough, and if it is, retrain on the whole data set including test data.
	 * The CrossValidation returned, however, is the from the original training run.
	 */
	public Pair<MahoutClassifier, CrossValidation> trainFull(TrainingSet trainingSet,
			FeatureExtractor extractor, DbConnection conn,
			OperationContext octxt) throws ServiceException {
		Pair<MahoutClassifier, CrossValidation> trainResults = train(trainingSet, extractor, conn, octxt);
		CrossValidation validation = trainResults.getSecond();
		if (validation.getAccuracy() > minAccuracy) {
			ZimbraLog.analytics.info(String.format("classifier accuracy is sufficiently high at %.2f; retraining with all data", validation.getAccuracy()));
			setPercentTest(0);
			MahoutClassifier fullTrain = trainInternal(trainingSet.getLastLoadedItems(), extractor, conn, octxt);
			return new Pair<MahoutClassifier, CrossValidation> (fullTrain, validation);
		} else {
			ZimbraLog.analytics.warn(String.format("classifier accuracy is insufficient at %.2f, not keeping it", validation.getAccuracy()));
			return null;
		}
	}

	private CrossValidation validate(Classifier classifier,
			List<TrainingItem> testItems, FeatureExtractor extractor)
			throws ServiceException {
		CrossValidation validation = new CrossValidation();
		for (TrainingItem item : testItems) {
			ClassifierResult result = classifier.classify(item.getMessage(), extractor, true);
			validation.add(item.getLabel(), result.getLabel(), result.getReason());
		}
		return validation;
	}

	@Override
    public String toString() {
		StringBuilder sb =  new StringBuilder("[Mahout classifier trainer with ")
		.append(numDimensions)
		.append(" dimensions]");
		return sb.toString();
	}
}

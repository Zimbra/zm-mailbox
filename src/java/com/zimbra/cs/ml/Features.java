package com.zimbra.cs.ml;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ContinuousValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.LuceneTextValueEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

/** Class for encoding features into a Mahout Vector
 * 
 * @author iraykin
 *
 */
public class Features {
	private Map<String, FeatureVectorEncoder> encoders = new HashMap<String, FeatureVectorEncoder>();
	private Vector vector;
	
	private enum FeatureType {
		CONTINUOUS, CATEGORICAL, WORD, TEXT;
	}
	
	public Features() {
		vector = new RandomAccessSparseVector(1000);
	}
	
	private FeatureVectorEncoder getEncoder(String name, FeatureType type) {
		if (encoders.containsKey(name)) {
			return encoders.get(name);
		} else {
			FeatureVectorEncoder encoder;
			switch (type) {
			case WORD:
				encoder = new StaticWordValueEncoder(name);
				break;
			case TEXT:
				encoder = new LuceneTextValueEncoder(name);
				setAnalyzer(encoder);
				break;
			case CATEGORICAL:
				encoder = new StaticWordValueEncoder(name);
				break;
			case CONTINUOUS:
				encoder = new ContinuousValueEncoder(name);
				break;
			default:
				encoder = new StaticWordValueEncoder(name);
			}
			encoders.put(name, encoder);
			encoder.setProbes(2);
			return encoder;
		}
	}
	private void setAnalyzer(FeatureVectorEncoder encoder) {
		LuceneTextValueEncoder luceneEncoder = (LuceneTextValueEncoder) encoder;
		luceneEncoder.setAnalyzer(new StandardAnalyzer(Version.LUCENE_35));
	}

	public void addStringFeature(String featureName, String featureValue) {
		FeatureVectorEncoder encoder = getEncoder(featureName, FeatureType.WORD);
		encoder.addToVector(featureValue, vector);
	}
	
	public void addContinuousFeature(String featureName, String featureValue) {
		FeatureVectorEncoder encoder = getEncoder(featureName, FeatureType.CONTINUOUS);
		encoder.addToVector(featureValue, vector);
	}

	public void addTextFeature(String featureName, String featureValue) {
		FeatureVectorEncoder encoder = getEncoder(featureName, FeatureType.TEXT);
		encoder.addToVector(featureValue, vector);
		
	}

	public void addCategoricalFeature(String featureName, String featureValue) {
		FeatureVectorEncoder encoder = getEncoder(featureName, FeatureType.CATEGORICAL);
		encoder.addToVector(featureValue, vector);
	}
	
	public Vector getVector() {
		return vector;
	}
}

package com.zimbra.cs.ml;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
/**
 *
 * @author iraykin
 *
 */
public class CrossValidation {

	private int P;
	private int N;
	private int FP;
	private int FN;
	private int TP;
	private int TN;
	private Map <String, Integer> falsePosReasons = new HashMap<String, Integer>();
	private Map <String, Integer> falseNegReasons = new HashMap<String, Integer>();

	public void add(Label expected, Label actual, String reason) {
		if (expected == Label.PRIORITY) {
			P++;
			if (expected == actual) { TP++; }
			else {
				FN++;
				incrementMap(falseNegReasons, reason);
			}
		} else {
			N++;
			if (expected == actual) {TN++; }
			else {
				FP++;
				incrementMap(falsePosReasons, reason);
				}
		}
	}

	private void incrementMap(Map<String, Integer> map, String reason) {
		if (map.containsKey(reason)) {
			map.put(reason, map.get(reason) + 1);
		} else {
			map.put(reason, 1);
		}
	}

	public float getPositivePrecision() {
		return (float)TP / (TP + FP);
	}

	public float getNegativePrecision() {
		return (float)TN / (TN + FN);
	}

	public float getPositiveRecall() {
		return (float)TP / P;
	}

	public float getNegativeRecall() {
		return (float)TN / N;
	}

	public float getAccuracy() {
		return ((float) (TP + TN)) / (P + N);
	}

	public void output(PrintStream out) {
		out.println("----------------------");
		out.println("- Validation Results -");
		out.println("----------------------");
		out.println(String.format("Positive Examples:  %s", P));
		out.println(String.format("Negative Examples:  %s", N));
		out.println(String.format("True Positives:     %s", TP));
		out.println(String.format("False Positives:    %s", FP));
		out.println(String.format("True Negatives:     %s", TN));
		out.println(String.format("False Negatives:    %s", FN));
		out.println(String.format("Positive Precision: %.2f", getPositivePrecision()));
		out.println(String.format("Negative Precision: %.2f", getNegativePrecision()));
		out.println(String.format("Positive Recall:    %.2f", getPositiveRecall()));
		out.println(String.format("Negative Recall:    %.2f", getNegativeRecall()));
		out.println(String.format("Accuracy:           %.2f", getAccuracy()));

		Set<String> fpReasons = falsePosReasons.keySet();
		Set<String> fnReasons = falseNegReasons.keySet();
		int maxLength = 0;
		for (String key: fpReasons) {
			if (key.length() > maxLength) {maxLength = key.length(); }
		}
		for (String key: fnReasons) {
			if (key.length() > maxLength) {maxLength = key.length(); }
		}
		maxLength = maxLength + 2;
		out.println("");
		out.println("--------------------------");
		out.println("- False Positive Reasons -");
		out.println("--------------------------");

		for (String key: falsePosReasons.keySet()) {
			out.println(String.format("%1$-" + maxLength + "s", key + ":") + String.valueOf(falsePosReasons.get(key)));
		}

		out.println("");
		out.println("--------------------------");
		out.println("- False Negative Reasons -");
		out.println("--------------------------");

		for (String key: falseNegReasons.keySet()) {
			out.println(String.format("%1$-" + maxLength + "s", key + ":") + String.valueOf(falseNegReasons.get(key)));
		}

	}
}
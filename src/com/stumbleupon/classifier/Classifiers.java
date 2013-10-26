package com.stumbleupon.classifier;

import java.util.List;

public abstract class Classifiers {
	
	public abstract void trainClassifier(List<List<Object>> features, String[] attributeNames) throws BuildModelException;
	public abstract EvalResult testClassifier(List<List<Object>> features, String[] attributeNames) throws EvaluationException;
	
}

package com.stumbleupon.classifier;

import java.util.List;
import java.util.Random;

import com.stumbleupon.features.FeatureGenerator;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public abstract class Classifiers {
	
	public abstract void trainClassifier(List<List<Object>> features, String[] attributeNames) throws BuildModelException;
	public abstract EvalResult testClassifier(List<List<Object>> features, String[] attributeNames) throws EvaluationException;
	
}

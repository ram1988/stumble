package com.stumbleupon.classifier;

import java.util.List;
import java.util.Random;

import com.stumbleupon.features.FeatureGenerator;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.core.Instances;

public class Classifiers {
	
	private static StumbleClassifier currentClassifier;
	
	
	private abstract static class StumbleClassifier {
	     abstract void buildModel(Instances featureSet)throws BuildModelException;
	     abstract EvalResult testModel(Instances testData)throws EvaluationException;
	}
	
	
	private static class NaiveBayesClassifier extends StumbleClassifier{
		
		private NaiveBayes bayesClassifier;
		private Instances trainingSet;
		
		NaiveBayesClassifier() {
			bayesClassifier = new NaiveBayes();
		}
		
		@Override
		void buildModel(Instances featureSet)throws BuildModelException {
			try {
				trainingSet = featureSet;
				bayesClassifier.buildClassifier(trainingSet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new BuildModelException("Build Model Exception::"+e.getMessage(),e);
			}
		}

		@Override
		EvalResult testModel(Instances testData) throws EvaluationException {
			Evaluation testModel = null;
			EvalResult result = null;
			
			try {
				testModel = new Evaluation(trainingSet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new EvaluationException("Evaluation Exception::"+e.getMessage(),e);
			}
			
				 
			try {
				//Object[] predictions = new Object[testData.numInstances()];
				System.out.println("classifier-->"+bayesClassifier);
				System.out.println("testData-->"+testData.numInstances());
				System.out.println("test data print over");
				
						
				result = new EvalResult();
				//Evaluation on trainingSet
				double[] res = testModel.evaluateModel(bayesClassifier, trainingSet);
				result.setAUC(testModel.areaUnderROC(0));
				result.setPrecision(testModel.precision(0));
				result.setRecall(testModel.recall(0));
				result.setFmeasure(testModel.fMeasure(0));
				
				System.out.println("Evaluation on training set-->"+testModel.toSummaryString());
				//Predicting the test data
				res = testModel.evaluateModel(bayesClassifier, testData);
				result.setClassLabel("");//yet to find to get the class label
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new EvaluationException("Evaluation Exception::"+e.getMessage(),e);
			}
			return result;
		}
		
	}
	
	private static class SVMClassifier extends StumbleClassifier {

		@Override
		void buildModel(Instances featureSet) {
			// TODO Auto-generated method stub
			
		}


		@Override
		EvalResult testModel(Instances testData) throws EvaluationException {
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}
	
	//have to include training dataset argument
	public static void trainClassifier(String classifier,List<List<Object>> features, String[] attributeNames) throws BuildModelException {
		
		System.out.println("Training the classifier-->"+classifier+" started");
		
		if(classifier.equals("bayes")) {
			currentClassifier = new NaiveBayesClassifier();
		}
		else if(classifier.equals("svm")) {
			currentClassifier = new SVMClassifier();
		}
		
		Instances featureSet = FeatureGenerator.convertToWekaFeatures(features, attributeNames,true);
		currentClassifier.buildModel(featureSet);
		
		System.out.println("Training the classifier-->"+classifier+" ended");
	}
	
	//have to include test dataset argument
	public static EvalResult testClassifier(List<List<Object>> features, String[] attributeNames) throws EvaluationException {
		System.out.println("Testing the classifier started");
		
		Instances featureSet = FeatureGenerator.convertToWekaFeatures(features, attributeNames,false);
		
		System.out.println("Testing the classifier ended"+featureSet);
		//returns label
		return currentClassifier.testModel(featureSet);
	}
}
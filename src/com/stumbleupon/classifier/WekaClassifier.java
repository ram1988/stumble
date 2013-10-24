package com.stumbleupon.classifier;

import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.Instances;

import com.stumbleupon.features.FeatureGenerator;

public class WekaClassifier extends Classifiers {
	
	private Classifier currentClassifier;
	private Instances trainingSet;
	private String classifier;
	
	public WekaClassifier(String name) {
		this.classifier = name;
	}


	//have to include training dataset argument
		public void trainClassifier(List<List<Object>> features, String[] attributeNames) throws BuildModelException {
			
			System.out.println("Training the classifier-->"+classifier+" started");
			
			if(classifier.equals("bayes")) {
				currentClassifier = new NaiveBayes();
			}
			else if(classifier.equals("svm")) {
				currentClassifier = new LibSVM();
			}
			else if(classifier.equals("ann")) {
				currentClassifier = new MultilayerPerceptron();
			}
			
			trainingSet = FeatureGenerator.convertToWekaFeatures(features, attributeNames,true);
			
			try {
				currentClassifier.buildClassifier(trainingSet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new BuildModelException("Build Model Exception::"+e.getMessage(),e);
			}
			
			System.out.println("Training the classifier-->"+classifier+" ended");
		}
		
		//have to include test dataset argument
		public  EvalResult testClassifier(List<List<Object>> features, String[] attributeNames) throws EvaluationException {
			System.out.println("Testing the classifier started");
			
			Instances testData = FeatureGenerator.convertToWekaFeatures(features, attributeNames,false);
			
			System.out.println("Testing the classifier ended "+testData);

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
				System.out.println("classifier-->"+currentClassifier);
				System.out.println("testData-->"+testData.numInstances());
				System.out.println("test data print over");
				
						
				result = new EvalResult();
				//Evaluation on trainingSet
				double[] res = testModel.evaluateModel(currentClassifier, trainingSet);
				result.setAUC(testModel.areaUnderROC(0));
				result.setPrecision(testModel.precision(0));
				result.setRecall(testModel.recall(0));
				result.setFmeasure(testModel.fMeasure(0));
				
				System.out.println("Evaluation on training set-->"+testModel.toSummaryString());
				//Predicting the test data
				String[] predictions = new String[testData.numInstances()];
				 
				//Predicting the test data
				Attribute classAttrib = testData.classAttribute();
				for(int i=0;i<testData.numInstances();i++) {
					double pre = currentClassifier.classifyInstance(testData.instance(i)) ;
					predictions[i] = classAttrib.value((int)pre);
				}
				//res = testModel.evaluateModel(bayesClassifier, testData);
				result.setClassLabel(predictions);//yet to find to get the class label
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new EvaluationException("Evaluation Exception::"+e.getMessage(),e);
			}
			
			return result;
		}
}

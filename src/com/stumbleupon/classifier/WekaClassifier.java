package com.stumbleupon.classifier;

import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.Stacking;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import com.stumbleupon.features.FeatureGenerator;
import com.stumbleupon.features.FeatureSelection;

public class WekaClassifier extends Classifiers {
	
	private Classifier currentClassifier;
	private Instances trainingSet;
	private String classifier;
	private String[] attributeNames;
	private int[] toRetain;
	
	public WekaClassifier(String name) {
		this.classifier = name;
	}


	//have to include training dataset argument
		public void trainClassifier(List<List<Object>> features, String[] attribNames) throws BuildModelException {
			
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
			else if(classifier.equals("logit")) {
				currentClassifier = new Logistic();
			}
			else if(classifier.equals("dectree")) {
				currentClassifier = new J48();
			}
			else if(classifier.equals("stacking")) {
				Stacking ensemble = new Stacking();
				currentClassifier = ensemble;
				ensemble.setClassifiers(new Classifier[]{new NaiveBayes(),new MultilayerPerceptron()});
				ensemble.setMetaClassifier(new LibSVM());
			}
			trainingSet = FeatureGenerator.convertToWekaFeatures(features, attribNames,true);
			//System.out.println("Training the classifier-->"+trainingSet);
			
			
			try {	
				
				if(attribNames.length > 10) {
					System.out.println("Feature Selection takes place");
					FeatureSelection ftrSelection = new FeatureSelection();
					toRetain = ftrSelection.getSelectedIndices(trainingSet, 100);
					Remove remove = new Remove();
					remove.setInvertSelection(true);
					remove.setAttributeIndicesArray(toRetain);
					remove.setInputFormat(trainingSet);
					trainingSet = Filter.useFilter(trainingSet, remove);
					
					Instance ins = trainingSet.instance(0);
					System.out.println("No. of attributes-->"+ins.numAttributes());
					
				} else {
					System.out.println("No Feature Selection");
				}
				
				attributeNames = attribNames;
				
				currentClassifier.buildClassifier(trainingSet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new BuildModelException("Build Model Exception::"+e.getMessage(),e);
			}
			
			System.out.println("Training the classifier-->"+classifier+" ended");
			
			
		}
		
		//have to include test dataset argument
		public  EvalResult testClassifier(List<List<Object>> features) throws EvaluationException {
			System.out.println("Testing the classifier started");
			
			Instances testData = FeatureGenerator.convertToWekaFeatures(features, attributeNames,false);
			
			//System.out.println("Testing the classifier ended "+testData);

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
				//System.out.println("classifier-->"+currentClassifier);
				System.out.println("testData-->"+testData.numInstances());
				System.out.println("test data print over");
				
				if(attributeNames.length > 10) {
					Remove remove = new Remove();
					remove.setInvertSelection(true);
					remove.setAttributeIndicesArray(toRetain);
					remove.setInputFormat(testData);
					testData = Filter.useFilter(testData, remove);
				}
						
				result = new EvalResult();
				//Evaluation on trainingSet
				//To include cross fold validation
				//evaluatebyCrossFold(10, trainingSet, currentClassifier);
				double[] res = testModel.evaluateModel(currentClassifier, trainingSet);
				System.out.println("Evaluation on training set-->"+testModel.toSummaryString());
				
				result.setAUC(testModel.areaUnderROC(1));
				result.setPrecision(testModel.precision(1));
				result.setRecall(testModel.recall(1));
				result.setFmeasure(testModel.fMeasure(1));
				
				
				//Predicting the test data
				String[] predictions = new String[testData.numInstances()];
				 
				//Predicting the test data
				Attribute classAttrib = testData.classAttribute();
				for(int i=0;i<testData.numInstances();i++) {
					double pre = currentClassifier.classifyInstance(testData.instance(i)) ;
					predictions[i] = classAttrib.value((int)pre);
					System.out.println(predictions[i]+"=="+pre);
				}
				//res = testModel.evaluateModel(bayesClassifier, testData);
				result.setClassLabel(predictions);//yet to find to get the class label
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new EvaluationException("Evaluation Exception::"+e.getMessage(),e);
			}
			
			Instance ins = testData.instance(0);
			Instance ins1 = trainingSet.instance(0);
			for(int i=0;i<ins.numAttributes();i++) {
				System.out.print(ins.attribute(i).name()+"=="+ins1.attribute(i).name()+"\n");
			}
			System.out.println();
			
			return result;
		}
}

package com.stumbleupon.classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import com.stumbleupon.features.FeatureSelection;

public class Logit {
	private Instances trainData;
	private Instances testData;
	private DataSource trainSource;
	private Logistic logit;
	public Logit()  throws Exception {
		//get data
		trainSource = new DataSource("data/train_categoryFixed.arff");
		trainData = trainSource.getDataSet();
		
		// randomize the data
		trainData.randomize(trainData.getRandomNumberGenerator(System.nanoTime()));
		logit = new Logistic();
	}
	
	public void selectAttributes(int numAttr)  throws Exception {
		int[] indices = new int[numAttr + 2]; //+1 is for the class attribute; another +1 for URLID
		FeatureSelection ftrSelection = new FeatureSelection();
		indices = ftrSelection.getSelectedIndices(trainData, numAttr);
		int[] toRetain = indices;
		Remove remove = new Remove();
		remove.setInvertSelection(true);
		remove.setAttributeIndicesArray(toRetain);
		remove.setInputFormat(trainData);
		Instances trainNew = Filter.useFilter(trainData, remove);
		trainData = trainNew;
		if (trainData.classIndex() == -1) {
			trainData.setClassIndex(trainData.numAttributes() - 1);
		}
		System.out.println(trainData.toSummaryString());
	}
	
	public void train() throws Exception {
		logit.buildClassifier(trainData);
	}
	
	public void test(int numFolds) throws Exception {
		int numMisClassified = 0;
		int totalTested = 0;
		int classification;
		System.out.println("Prediction" + "\t|\t" + "Actual");
		for(int n=0; n < numFolds; n++) {
			testData = trainData.testCV(numFolds, n);
			totalTested = totalTested + testData.numInstances();
			for(int i = 0; i < testData.numInstances(); i++) {
				classification = (int) logit.classifyInstance(testData.instance(i));
				System.out.println(classification + "\t|\t" + (int)testData.instance(i).value(testData.classIndex()));
				if(classification != (int) testData.instance(i).value(testData.classIndex())) {
					numMisClassified = numMisClassified + 1;
				}
			}
		//System.out.println("mis-classified categories: \n" + Utils.arrayToString(misClassifiedURLs.toArray()));
		}
		System.out.println("numMisClassified: " +  numMisClassified);
		System.out.println("totalTested: " +  totalTested);
		System.out.println("%mis-classification: " + (numMisClassified*100.0/totalTested));
	}
	
	public double[] predictClass(Instance instance) throws Exception {
		return logit.distributionForInstance(instance);
	}
	
	public static void main(String[] args) throws Exception { 
		Logit logistic = new Logit();
		logistic.selectAttributes(5);
		logistic.train();
		logistic.test(10); // num folds
	}
}

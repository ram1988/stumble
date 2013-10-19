package com.stumbleupon.classifier;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.functions.Logistic;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import com.stumbleupon.features.FeatureSelection;

public class Logit {
	private Instances trainData;
	private Instances testData;
	private DataSource trainSource;
	private DataSource testSource;
	private Logistic logit;
	
	public Logit()  throws Exception {
		trainSource = new DataSource("data/train_categoryFixed.arff");
		trainData = trainSource.getDataSet();
		if (trainData.classIndex() == -1) {
			trainData.setClassIndex(trainData.numAttributes() - 1);
		}
		for(int i = 0; i < trainData.numAttributes(); i++) {
			trainData.deleteWithMissing(i);
		}
		trainData.randomize(trainData.getRandomNumberGenerator(System.nanoTime()));
		trainData.stratify(10); //10 folds
		
		//testSource = new DataSource("data/test_from_train.arff");
		//testData = testSource.getDataSet();
		testData = trainData.testCV(10, 5);
		if (testData.classIndex() == -1) {
			testData.setClassIndex(testData.numAttributes() - 1);
		}
		for(int i = 0; i < testData.numAttributes(); i++) {
			testData.deleteWithMissing(i);
		}
		
		logit = new Logistic();
	}
	
	public void selectAttributes(int numAttr)  throws Exception {
		// load data
		int[] indices = new int[numAttr + 1]; //+1 is for the class attribute
		FeatureSelection ftrSelection = new FeatureSelection();
		indices = ftrSelection.useLowLevel(trainData, numAttr);
		int[] toRemove = indices;
		Remove remove = new Remove();
		remove.setAttributeIndicesArray(toRemove);
		remove.setInvertSelection(true);
		remove.setInputFormat(trainData);
		Instances trainNew = Filter.useFilter(trainData, remove);
		trainData = trainNew;
		System.out.println(trainData.toSummaryString());
		Instances testNew = Filter.useFilter(testData, remove);
		testData = testNew;
		System.out.println(testData.toSummaryString());
	}
	
	public void train() throws Exception {
		logit.buildClassifier(trainData);
	}
	
	public void test() throws Exception {
		List<Double> misClassifiedURLs = new ArrayList<Double>();
		int classification;
		for(int i = 0; i < testData.numInstances(); i++) {
			classification = (int) logit.classifyInstance(testData.instance(i));
			System.out.println(classification + " | " + (int)testData.instance(i).value(testData.classIndex()));
			if(classification != (int) testData.instance(i).value(testData.classIndex())) {
				misClassifiedURLs.add(testData.instance(i).value(0));
			}
		}
		System.out.println("%mis-classification: " + (misClassifiedURLs.size()*100.0/testData.numInstances()));
		//System.out.println("URL ids: \n" + Utils.arrayToString(misClassifiedURLs.toArray()));
	}
	
	public double[] predictClass(Instance instance) throws Exception {
		return logit.distributionForInstance(instance);
	}
	
	public static void main(String[] args) throws Exception { 
		Logit logistic = new Logit();
		logistic.selectAttributes(5);
		logistic.train();
		logistic.test();
	}
}

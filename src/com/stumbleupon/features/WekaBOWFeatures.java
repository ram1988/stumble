package com.stumbleupon.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.Stacking;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.tokenizers.NGramTokenizer;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

import com.stumbleupon.classifier.BuildModelException;
import com.stumbleupon.classifier.Classifiers;
import com.stumbleupon.classifier.EvalResult;
import com.stumbleupon.classifier.EvaluationException;
import com.stumbleupon.classifier.WekaClassifier;

public class WekaBOWFeatures extends FeatureGenerator {

	private List<Map<String, Object>> list;


	@Override
	public List<List<Object>> generateFeaturesFromTrainData() {

		List<List<Object>> featureList = new ArrayList<List<Object>>();
		list = getCompetitionFeaturesMap(true,false);
		int idx = 0;

		for(Map<String, Object> str:list) {
			String processedString = null;
			JSONObject jsonObj;
			List<Object> feats = new ArrayList<Object>();

			try {
				processedString = formatStringToJSON(str.get("boilerplate").toString());
				jsonObj = new JSONObject(processedString);
				processedString = (String)jsonObj.get("body").toString().toLowerCase();
				processedString = processedString.trim().replaceAll("[^0-9a-z\\s]","");
				processedString = processedString.trim().replaceAll("[0-9]","");
				feats.add(processedString);

				Map<String, Object> item = list.get(idx);
				String label = item.get("label").toString();
				feats.add(label);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			featureList.add(feats);
			idx++;
		}
		return featureList;
	}

	@Override
	public List<List<Object>> generateFeaturesFromTestData() {

		List<List<Object>> featureList = new ArrayList<List<Object>>();
		list = getCompetitionFeaturesMap(false,false);
		int idx = 0;

		for(Map<String, Object> str:list) {
			String processedString = null;
			JSONObject jsonObj;
			List<Object> feats = new ArrayList<Object>();

			try {
				processedString = formatStringToJSON(str.get("boilerplate").toString());
				jsonObj = new JSONObject(processedString);
				processedString = (String)jsonObj.get("body").toString().toLowerCase();
				processedString = processedString.trim().replaceAll("[^0-9a-z\\s]","");
				processedString = processedString.trim().replaceAll("[0-9]","");
				feats.add(processedString);
				feats.add("1");

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			featureList.add(feats);
			idx++;
		}
		return featureList;
	}
	
	private static StringToWordVector filter;

	private static Instances convertToStringWordVector(Instances ip) {
		
		filter = new StringToWordVector();
		filter.setTFTransform(true);
		filter.setIDFTransform(true);
		//filter.setUseStoplist(true);
		//NGramTokenizer  wt = new NGramTokenizer();
		//String delimiters = " \r\t\n.,;:\'\"()?!-><#$\\%&*+/@^_=[]{}|`~0123456789";
		//wt.setDelimiters(delimiters);
		//wt.setNGramMaxSize(2);
		//filter.setTokenizer(wt);
		
		Instances dataFiltered = null;
	    try {
			filter.setInputFormat(ip);
			dataFiltered = Filter.useFilter(ip, filter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return dataFiltered;
	}
	
	private static Instance convertTestInstance(Instance test) {
		try {
			filter.input(test);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return filter.output();
	}
	    
	public  Instances convertToWekaFeatures(List<List<Object>> features, String[] attributeNames,boolean isTrainPhase) {

		Instances featureSet = null;

		String[] subAttribs = Arrays.copyOfRange(attributeNames, 0, attributeNames.length-1);

		//Preparing the feature vectors
		FastVector featureVectors = new FastVector();
		List<Object> obj = features.get(0);
		int idx = 0;
		for(String attrib:subAttribs) {
			//System.out.println("Attrib-->"+attrib);
			Object str = obj.get(idx);
			if(str instanceof Double || str instanceof Integer ) {
				featureVectors.addElement(new Attribute(attrib));
			} 
			else if(str instanceof String) {
				featureVectors.addElement(new Attribute(attrib,(FastVector)null));
			}

			idx++;
		}

		FastVector classVector = new FastVector(2);
		classVector.addElement("0");//evergreen
		classVector.addElement("1");//ephemeral
		//if(!isTrainPhase) { //for decision tree no of classes should be fixed
		//classVector.addElement("?");
		//}

		Attribute classAttrib = new Attribute(attributeNames[attributeNames.length-1],classVector);
		featureVectors.addElement(classAttrib);

		featureSet = new Instances("StumbleFeatures", featureVectors, 10);
		featureSet.setClassIndex(attributeNames.length-1);

		//Forming Weka Instances
		int rec = 1;
		for(List<Object> list:features) {
			Instance feat = new Instance(attributeNames.length);
			//System.out.println("length-->"+attributeNames.length);
			idx = 0;
			//System.out.println("Record-->"+rec);
			for(Object str:list) {				
				if(str instanceof Double) {
					feat.setValue((Attribute)featureVectors.elementAt(idx),(Double)str);
				} 
				else if(str instanceof String) {
					feat.setValue((Attribute)featureVectors.elementAt(idx),str.toString());
				} 
				else if(str instanceof Integer) {
					feat.setValue((Attribute)featureVectors.elementAt(idx),(Integer)str);
				}
				idx++;
			}
			rec++;
			featureSet.add(feat);
		}

		return featureSet;

	}

	public void evaluatebyCrossFold(int numFolds,Instances trainData,Classifier cls ) throws Exception {
		 for (int i = 0; i < 2; i++) {
		      // randomize data
		      int seed = i + 1;
		      Random rand = new Random(seed);
		      Instances randData = new Instances(trainData);
		      randData.randomize(rand);
		      if (randData.classAttribute().isNominal())
		        randData.stratify(numFolds);

		      Evaluation eval = new Evaluation(randData);
		      for (int n = 0; n < numFolds; n++) {
		        Instances train = randData.trainCV(numFolds, n);
		        Instances test = randData.testCV(numFolds, n);
		        // the above code is used by the StratifiedRemoveFolds filter, the
		        // code below by the Explorer/Experimenter:
		        // Instances train = randData.trainCV(folds, n, rand);

		        // build and evaluate classifier
		        Classifier clsCopy = Classifier.makeCopy(cls);
		        clsCopy.buildClassifier(train);
		        eval.evaluateModel(clsCopy, test);
		      }

		      // output evaluation
		      System.out.println();
		      System.out.println("=== Setup run " + (i+1) + " ===");
		      System.out.println("Classifier: " + cls.getClass().getName() );
		      System.out.println("Dataset: " + trainData.relationName());
		      System.out.println("Folds: " + numFolds);
		      System.out.println("Seed: " + seed);
		      System.out.println("AUC Metric--->"+eval.areaUnderROC(1));
			  System.out.println("Precision--->"+eval.precision(1));
			  System.out.println("Recall--->"+eval.recall(1));
			  System.out.println("F-Measure--->"+eval.fMeasure(1));
		      System.out.println();
		      System.out.println(eval.toSummaryString("=== " + numFolds + "-fold Cross-validation run " + (i+1) + "===", false));
		    }
		  }
	
	public static void main(String[] args) {

		String[] attribNames = {"content","clazzz"};

		WekaBOWFeatures feat = new WekaBOWFeatures();

		String classifier = "ann";

		//Generating Train Features
		System.out.println("Preparing Train BOW");
		List<List<Object>> feats = feat.generateFeaturesFromTrainData();
		List<List<Object>> test_feats = feat.generateFeaturesFromTestData();
		
		feats.addAll(test_feats);

		Instances wekaInstances = feat.convertToWekaFeatures(feats, attribNames, true);		
		wekaInstances = convertToStringWordVector(wekaInstances);
		
		System.out.println("Size--->"+wekaInstances.numInstances());
		
		Instances trainData = new Instances(wekaInstances,0,7395);
		for(int i=0;i<=7394;i++)  {
			//System.out.println(i);
			wekaInstances.delete(0);
		}
		System.out.println("Size--->"+wekaInstances.numInstances());
		Instances testData = new Instances(wekaInstances,0,wekaInstances.numInstances()-1);
		wekaInstances = null;
		
		Classifier currentClassifier = null;
		
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
			ensemble.setClassifiers(new Classifier[]{new NaiveBayes(),new NaiveBayes()});
			ensemble.setMetaClassifier(new MultilayerPerceptron());
		}
		else if(classifier.equals("random")) {
			RandomForest tree = new RandomForest();
			tree.setNumTrees(2);
			currentClassifier = tree;
		}
		
		//Programmatic Classification 
		
		//Build Model
		int []toRetain = null;
		try {
			/*FeatureSelection ftrSelection = new FeatureSelection();
			toRetain = ftrSelection.getSelectedIndices(train_feats, 25);
			Remove remove = new Remove();
			remove.setInvertSelection(true);
			remove.setAttributeIndicesArray(toRetain);
			remove.setInputFormat(train_feats);
			train_feats = Filter.useFilter(train_feats, remove);*/
			System.out.println("10 FOLD cross validation");
			feat.evaluatebyCrossFold(10, trainData, currentClassifier);
			
			currentClassifier.buildClassifier(trainData);
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		

		//Generating Test Features
		
		
		System.out.println("Testing attribs-->"+testData.numAttributes());
		//System.out.println("Testing the classifier ended "+testData);

		Evaluation testModel = null;
		EvalResult result = null;
		
		try {
			/*Remove remove = new Remove();
			remove.setInvertSelection(true);
			remove.setAttributeIndicesArray(toRetain);
			remove.setInputFormat(testData);
			testData = Filter.useFilter(testData, remove);*/
			
			testModel = new Evaluation(trainData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			 
		try {
			
			//Object[] predictions = new Object[testData.numInstances()];
			//System.out.println("classifier-->"+currentClassifier);
			System.out.println("testData-->"+testData.numInstances());
			System.out.println("test data print over");
			
			result = new EvalResult();
			//Evaluation on trainingSet
			//To include cross fold validation
			//Classifiers cls = new WekaClassifier();
			//cls.evaluatebyCrossFold(10, train_feats, currentClassifier);
			
			double[] res = testModel.evaluateModel(currentClassifier, trainData);
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
				double pre = currentClassifier.classifyInstance( testData.instance(i))  ;
				predictions[i] = classAttrib.value((int)pre);
				//System.out.println(predictions[i]+"=="+pre);
			}
			//res = testModel.evaluateModel(bayesClassifier, testData);
			result.setClassLabel(predictions);//yet to find to get the class label
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Instance ins1 = trainData.instance(0);
		for(int i=0;i<ins1.numAttributes();i++) {
			System.out.print(ins1.attribute(i).name()+"\n");
		}
		System.out.println();
		
		FileWriter fw = null;
		try {
			fw = new FileWriter("wekabow_test_labels_"+classifier+".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		BufferedWriter bw = new BufferedWriter(fw);
		List<Map<String, Object>> list = getCompetitionFeaturesMap(false,false);

		int trueCt = 0, falseCt = 0;
		try {
			bw.write("urlid,label\n");
			int i = 0;
			for(String predicted:result.getClassLabel()) {
				Map<String, Object> item = list.get(i);
				Object original = item.get("label");
				if(original!=null && predicted.equals(original.toString()))  
					trueCt++;
				else 
					falseCt++;

				bw.write(item.get("urlid").toString()+","+predicted+"\n");
				i++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				bw.write("Truely classifiedsvm-->"+trueCt+"------Falsely classified--->"+falseCt);
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

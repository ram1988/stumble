package com.stumbleupon.features;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
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
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;


import com.stumbleupon.classifier.BuildModelException;
import com.stumbleupon.classifier.Classifiers;
import com.stumbleupon.classifier.EvalResult;
import com.stumbleupon.classifier.EvaluationException;
import com.stumbleupon.classifier.WekaClassifier;

public class BoWFeatures extends FeatureGenerator {

	private List<String[]> text;
	private List<Map<String,Integer>> tokenized;//termFreq
	private  Map<String,Integer> terms; //docFreq
	private List<Map<String, Object>> list;
	private int nsize;
	//private int n=5000;
	private DecimalFormat decifrmt = new DecimalFormat("#.##");


	private class DFSort implements Comparator<Integer> {

		@Override
		public int compare(Integer df, Integer docFreq) {
			// TODO Auto-generated method stub
			return docFreq.compareTo(df);
		}

	}



	private double getTfIdf(int tf,int df) {
		double termfreq = tf>0?(1+Math.log10(tf)):1;
		//return termfreq;
		return (  termfreq * Math.log((double)nsize/(double)df) );
	}

	public Set<String> getTerms() {
		return terms.keySet();
	}

	public  List<List<Object>> preprocessData(List<List<Object>> list) {
		System.out.println("Size--->"+list.size());
		int idx = 0;
		Pattern pat = Pattern.compile("^[0-9]");

		tokenized = new ArrayList<Map<String,Integer>>();

		terms = new WekaBOWFeatures().getTerms();
		//terms = new LinkedHashMap<String,Integer>();
	

		try {

			//br = new BufferedReader(new FileReader("tagged_train.txt"));
			for(List<Object> sub_list:list) {
				//System.out.println("Doc11before-->"+idx);
				String content = sub_list.get(0).toString();
				//System.out.println("Doc11-->"+idx+"Content--->"+content);
				content = content.replaceAll("[^0-9a-z\\s]", " . ");
				String[] toks = content.split(" ");
				
				Map<String,Integer> tokens = new HashMap<String,Integer>();
				
				for(String s:toks) {
					//System.out.println("before--"+s+"---");
					s = s.trim().replaceAll("[^0-9a-z]","");
					//s = s.trim().replaceAll("\\\\","");

					Matcher match = pat.matcher(s);

					if( !(isStopWord(s) || match.find() || s.length() <= 2 ) ){ 
						s = stemWords(s);
						if(s.length() > 2) { 
							tokens.put(s,tokens.containsKey(s)?tokens.get(s)+1:1);
						}
					} 
				}
				tokenized.add(tokens);

				Set<String>  docTerms = tokens.keySet();
				for(String term:docTerms) {
					terms.put(term, terms.containsKey(term)?terms.get(term)+1:1);
				}
				idx++;
			}

			//df < 3 terms will be removed
			
			Iterator<String> keys = terms.keySet().iterator();
			while(keys.hasNext()) {
				//Removing terms having docfreq lesser than 3
				String term = keys.next();
				if(terms.get(term)<3) {
					keys.remove();
					terms.remove(term);
				}
			}
		

		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		Map<Integer, List<Object>> trainList = new TreeMap<Integer,List<Object>>();
		int words = 0;
		
		for(String key:terms.keySet()) {
			int i = 0;
			//System.out.print(key+" ");
			//System.out.println("Preparing attribs-->"+words);
			//Changing the program for using boolean features
			int df = terms.get(key);		

			for(Map<String,Integer> map:tokenized) {
				int tf = map.containsKey(key)?map.get(key):0;

				//int tf = map.containsKey(key)?1:0;
				//for boolean values as feat. values
				/*if(trainList.containsKey(i)) {
					trainList.get(i).add(tf);
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(tf);
					trainList.put(i,temp);
				}*/
				// for having tf-idf as feature values
				double wgt = getTfIdf(tf,df);
				wgt = Double.isInfinite(wgt)?0.0:wgt;
				//System.out.println(i+".)String--->"+key+"--->tf::"+tf+"-->df::"+df);
				if(trainList.containsKey(i)) {
					trainList.get(i).add(new Double(decifrmt.format(wgt)));
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(new Double(decifrmt.format(wgt)));
					trainList.put(i,temp);
				}

				/*//for having tf as feature values only
				if(trainList.containsKey(i)) {
					trainList.get(i).add(tf);
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(tf);
					trainList.put(i,temp);
				}*/
				i++;
			}
			words++;
		}
		
		idx=0;
		for(List<Object> sub_list:list) {
			List<Object> feats = trainList.get(idx);
			String class_lbl = sub_list.get(1).toString();
			feats.add(class_lbl);
			System.out.println("list size--->"+feats.size());
			idx++;
		}

		System.out.println("Key Size:"+terms.size());
		List<List<Object>> features = new ArrayList<List<Object>>(trainList.values());

		return features;
	}
	

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
				processedString = str.get("boilerplate").toString();
				//processedString = formatStringToJSON(str.get("boilerplate").toString());
				//jsonObj = new JSONObject(processedString);
				//processedString = (String)jsonObj.get("body").toString().toLowerCase();
				//processedString = (String)jsonObj.get("url").toString().toLowerCase();
				processedString = processedString.trim().replaceAll("[^0-9a-z\\s]","");
				processedString = processedString.trim().replaceAll("[0-9]","");
				feats.add(processedString);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				//System.out.println("Doc--->"+idx);
				//e.printStackTrace();
				feats.add("");
			}
			Map<String, Object> item = list.get(idx);
			String label = item.get("label").toString();
			feats.add(label);

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
				processedString = str.get("boilerplate").toString();
				//processedString = formatStringToJSON(str.get("boilerplate").toString());
				//jsonObj = new JSONObject(processedString);
				//processedString = (String)jsonObj.get("body").toString().toLowerCase();
				//processedString = (String)jsonObj.get("url").toString().toLowerCase();
				processedString = processedString.trim().replaceAll("[^0-9a-z\\s]","");
				processedString = processedString.trim().replaceAll("[0-9]","");
				feats.add(processedString);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//System.out.println("Doc--->"+idx);
				feats.add("");
			}
			feats.add("?");
			featureList.add(feats);
			idx++;
		}
		return featureList;
	}

	
	public String[] getTermAttribs() {
		/*int len = n+1;//for body
		if(terms.size()<n) {
			len = terms.size()+1;
		}
		//int len = terms.size()+1;
		System.out.println("Length of terms-->"+len);
		String[] termArr = new String[len];
		int i=0;
		for(String t:terms.keySet()) {
			if(i == n) { break; }//for body
			termArr[i] = t;
			i++;
		}
		termArr[len-1] = "clazzz";
		System.out.println("Length of terms-->"+termArr[len-1]);*/
		
		int len = terms.size()+1;
		String[] termArr = new String[len];
		int i=0;
		for(String t:terms.keySet()) {
			termArr[i] = t;
			i++;
		}
		termArr[len-1] = "clazzz";
		return termArr;
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

		String classifier = "bayes";
		BoWFeatures bow = new BoWFeatures();

		
		//Generating Train Features
		System.out.println("Preparing Train BOW");
		List<List<Object>> feats = bow.generateFeaturesFromTrainData();
		List<List<Object>> test_feats = bow.generateFeaturesFromTestData();

		feats.addAll(test_feats);
		feats = bow.preprocessData(feats);
		
		String[] attribNames = bow.getTermAttribs();

		
		
		
		System.out.println("Size--->"+attribNames.length);
		Instances wekaInstances = bow.convertToWekaFeatures(feats, attribNames, true);		
		
		System.out.println("Size--->"+wekaInstances.numInstances());
		System.out.println("Feature Selection going on");

		/*Instance ins1 = wekaInstances.instance(0);
		for(int i=0;i<ins1.numAttributes();i++) {
			System.out.print(ins1.attribute(i).name()+"\n");
		}*/

		//Feature Selection
		int []toRetain = null;


		FeatureSelection ftrSelection = new FeatureSelection();
		try {
			toRetain = ftrSelection.getSelectedIndices(wekaInstances, 300);
			Remove remove = new Remove();
			remove.setInvertSelection(true);
			remove.setAttributeIndicesArray(toRetain);
			remove.setInputFormat(wekaInstances);
			wekaInstances = Filter.useFilter(wekaInstances, remove);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		Instance ins1 = wekaInstances.instance(0);
		for(int i=0;i<ins1.numAttributes();i++) {
			System.out.print(ins1.attribute(i).name()+"\n");
		}
		
		System.out.println("Feature Reduction going on");

		LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
		AttributeSelection selecter = new AttributeSelection();
		Ranker rank = new Ranker();

		selecter.setEvaluator(lsa);
		selecter.setSearch(rank);
		//selecter.setRanking(true);
		try {
			selecter.SelectAttributes(wekaInstances);
			wekaInstances = selecter.reduceDimensionality(wekaInstances);
		} catch (Exception e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}




		System.out.println("10 FOLD cross validation");

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
			tree.setNumTrees(3);
			currentClassifier = tree;
		}

		//Programmatic Classification 

		//Build Model


		try {
			bow.evaluatebyCrossFold(10, trainData, currentClassifier);			
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

		String[] predictions = null;	 
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
			predictions = new String[testData.numInstances()];

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


		ins1 = trainData.instance(0);
		for(int i=0;i<ins1.numAttributes();i++) {
			System.out.print(ins1.attribute(i).name()+"\n");
		}
		System.out.println();

		FileWriter fw = null;
		try {
			fw = new FileWriter("manbow_test_labels_"+classifier+".txt");
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


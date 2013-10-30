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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.stumbleupon.classifier.BuildModelException;
import com.stumbleupon.classifier.Classifiers;
import com.stumbleupon.classifier.EvalResult;
import com.stumbleupon.classifier.EvaluationException;
import com.stumbleupon.classifier.WekaClassifier;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class TagBOWFeatures extends FeatureGenerator {
	
	private List<String[]> text;
	private List<Map<String,Integer>> tokenized;//termFreq
	private Map<String,Integer> terms; //docFreq
	private int nsize;
	private int n=5000;
	private DecimalFormat decifrmt = new DecimalFormat("#.#");
	private MaxentTagger tagger;

	public TagBOWFeatures() {
		tagger =  new MaxentTagger("taggers/english-bidirectional-distsim.tagger");
	}

	private void preprocessData(boolean isTrain) {

		tokenized = new ArrayList<Map<String,Integer>>();
		StringBuffer joinTerm = new StringBuffer("");
		String prev_grammar = "";
		if(isTrain) {
			terms = new TreeMap<String,Integer>();
		}
		int idx = 0;


		BufferedReader br = null;

		Pattern pat = Pattern.compile("^[0-9]");


		try {

			//br = new BufferedReader(new FileReader("tagged_train.txt"));

			for(String[] str:text) {
				System.out.println("Doc-->"+idx+"--length-->"+str.length);

				Map<String,Integer> tokens = new HashMap<String,Integer>();
				for(String s:str) {
					//System.out.println("before--"+s+"---");
					s = s.trim().replaceAll("\\\\","");
					Matcher match = pat.matcher(s);

					if(s.equals("") || match.find() || s.length() ==1 ) { continue; }

					s = tagger.tagString(s);

					//lower case
					String[] tag_toks = s.split("_");
					//Term
					//System.out.println("before stem-->"+s);
					s = tag_toks[0].toLowerCase();
					//s = s.replaceAll("[^0-9a-z]","");

					//POS Tag
					String grammar = tag_toks[1].trim();
					
					if( !isStopWord(s) && grammar.matches("NN|NNP|NNS|NNS|FW") ) {
						if(s.length() > 2) { 
							tokens.put(s,tokens.containsKey(s)?tokens.get(s)+1:1);
						}
					}
				}
				tokenized.add(tokens);
				if(isTrain) {
					Set<String>  docTerms = tokens.keySet();
					for(String term:docTerms) {
						terms.put(term, terms.containsKey(term)?terms.get(term)+1:1);
					}
				}
				
				idx++;
			}
			
			//df < 3 terms will be removed
			  if(isTrain) {
				Iterator<String> keys = terms.keySet().iterator();
				while(keys.hasNext()) {
					//Removing terms having docfreq lesser than 3
					String term = keys.next();
					if(terms.get(term)<3) {
						keys.remove();
						terms.remove(term);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	private double getTfIdf(int tf,int df) {
		double termfreq = tf>0?(1+Math.log(tf)):1;
		return termfreq;
		//return (  termfreq * Math.log((double)nsize/(double)df) );
	}
	
	public Set<String> getTerms() {
		return terms.keySet();
	}
	
	
	@Override
	public List<List<Object>> generateFeaturesFromTrainData() {

		text = getBoilerPlateText(true, false); 
		System.out.println("List Size:"+text.size());

		preprocessData(true);
		
		nsize = text.size();
		Set<String> keys = terms.keySet();
		System.out.println("Key Size:"+keys.size()+"---"+nsize);
		
			
		Map<Integer, List<Object>> trainList = new TreeMap<Integer,List<Object>>();
		int words = 1;
		
		for(String key:keys) {
			
			System.out.println(key);
		}
		
		
		
		for(String key:keys) {
			int i = 0;
			//System.out.print(key+" ");
			if(words > n) {
				System.out.println("over");
				break;
			}
			System.out.println("Preparing features-->"+words);
			//Changing the program for using boolean features
			int df = terms.get(key);
			for(Map<String,Integer> map:tokenized) {
				//int tf = map.containsKey(key)?map.get(key):0;
				
				int tf = map.containsKey(key)?1:0;
				//for boolean values as feat. values
				if(trainList.containsKey(i)) {
					trainList.get(i).add(tf);
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(tf);
					trainList.put(i,temp);
				}
				// for having tf-idf as feature values
				/*double wgt = getTfIdf(tf,df);
				wgt = Double.isInfinite(wgt)?0.0:wgt;
				//System.out.println(i+".)String--->"+key+"--->tf::"+tf+"-->df::"+df);
				if(trainList.containsKey(i)) {
					trainList.get(i).add(new Double(decifrmt.format(wgt)));
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(new Double(decifrmt.format(wgt)));
					trainList.put(i,temp);
				}*/
				
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
		
		Set<Integer> keylist = trainList.keySet(); 
		List<String> labels = getClassLabels();
		for(Integer k:keylist) {
			List<Object> feats = trainList.get(k);
			feats.add(labels.get(k));
		}
		

		System.out.println("Key Size:"+keys.size());
		List<List<Object>> features = new ArrayList<List<Object>>(trainList.values());

		return features;
	}

	@Override
	public List<List<Object>> generateFeaturesFromTestData() {

		text = getBoilerPlateText(false, false); 
		System.out.println("List Size:"+text.size());

		preprocessData(false);

		Set<String> keys = terms.keySet();
		System.out.println("Key Size:"+keys.size()+"---"+nsize);
		
		Map<Integer, List<Object>> testList = new TreeMap<Integer,List<Object>>();
		int words = 1;
		for(String key:keys) {
			int i = 0;
			//System.out.print(key+" ");
			if(words > n) {
				System.out.println("over");
				break;
			}
			//System.out.println("Preparing features-->"+words);
			int df = terms.get(key);
			for(Map<String,Integer> map:tokenized) {
				//int tf = map.containsKey(key)?map.get(key):0;
				int tf = map.containsKey(key)?1:0;
				if(testList.containsKey(i)) {
					testList.get(i).add(tf);
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(tf);
					testList.put(i,temp);
				}
				// for having tf-idf as feature values
				/* double wgt = getTfIdf(tf,df);
				wgt = Double.isInfinite(wgt)?0.0:wgt;
				//System.out.println(i+".)String--->"+key+"--->tf::"+tf+"-->df::"+df);
				if(testList.containsKey(i)) {
					testList.get(i).add(new Double(decifrmt.format(wgt)));
				} else {
					List<Object> temp = new ArrayList<Object>();
					temp.add(new Double(decifrmt.format(wgt)));
					testList.put(i,temp);
				}*/
				
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
		
		Set<Integer> keylist = testList.keySet(); 
		for(Integer k:keylist) {
			List<Object> feats = testList.get(k);
			feats.add("?");
		}
		

		System.out.println("Key Size:"+keys.size());
		List<List<Object>> features = new ArrayList<List<Object>>(testList.values());

		return features;
	}
	
	//Overriding for trying out boolean values nominal attribute
	public  Instances convertToWekaFeatures(List<List<Object>> features, String[] attributeNames,boolean isTrainPhase) {

		Instances featureSet = null;

		String[] subAttribs = Arrays.copyOfRange(attributeNames, 0, attributeNames.length-1);

		//Preparing the feature vectors
		FastVector featureVectors = new FastVector();
		
		int idx = 0;
		for(String attrib:subAttribs) {
			//System.out.println("Attrib-->"+attrib);
			
			FastVector nominals = new FastVector(2);
			nominals.addElement("0");//evergreen
			nominals.addElement("1");//ephemeral

			Attribute attribute = new Attribute(attrib,nominals);
			featureVectors.addElement(attribute);

			idx++;
		}

		FastVector classVector = new FastVector(2);
		classVector.addElement("0");//evergreen
		classVector.addElement("1");//ephemeral
		//if(!isTrainPhase) { //for decision tree no of classes should be fixed
		classVector.addElement("?");
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

	public String[] getTermAttribs() {
		int len = n+1;//for body
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
		System.out.println("Length of terms-->"+termArr[len-1]);
		return termArr;
	}

	public static void main(String[] args) {

		String classifier = "logit";
		TagBOWFeatures bow = new TagBOWFeatures();

		List<List<Object>> list = bow.generateFeaturesFromTrainData();
		String[] attribNames = bow.getTermAttribs();
		System.out.println("Length of terms-->"+attribNames.length);
		//Programmatic Classification 
		//Build Model
		//Build Model
		Classifiers classifiers = new WekaClassifier(classifier,bow);
		FileWriter fw = null;
		try {
			fw = new FileWriter("train_bow_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedWriter bw = new BufferedWriter(fw);
		
		try {
			int len = list.get(0).size();
			for(String t:bow.getTerms()) {
				bw.write(t+",");
			}
			bw.write("class\n");
			
			for(List<Object> features:list) {
				for(int i=0;i<len-1;i++) {
					bw.write(features.get(i)+",");
				}
				bw.write(features.get(len-1)+"\n");//class label
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			classifiers.trainClassifier(list,attribNames);
		} catch (BuildModelException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		

		list = bow.generateFeaturesFromTestData();

		//Test Model
		EvalResult result = null;
		try {
			result = classifiers.testClassifier(list);
		} catch (EvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		//System.out.println("Predicted Class Label--->"+result.getClassLabel());
		System.out.println("AUC Metric--->"+result.getAUC());
		System.out.println("Precision--->"+result.getPrecision());
		System.out.println("Recall--->"+result.getRecall());
		System.out.println("F-Measure--->"+result.getFmeasure());


		fw = null;
		try {
			fw = new FileWriter("bow_test_labels"+classifier+".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		bw = new BufferedWriter(fw);
		List<Map<String, Object>> list_feats =  FeatureGenerator.getCompetitionFeaturesMap(false,false);

		int trueCt = 0, falseCt = 0;
		try {
			int i = 0;
			for(String predicted:result.getClassLabel()) {
				Map<String, Object> item = list_feats.get(i);
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
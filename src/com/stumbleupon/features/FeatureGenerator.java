package com.stumbleupon.features;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.stumbleupon.reader.DBAccess;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;



public abstract class FeatureGenerator {
	
	private List<String> stopWords;
	public abstract List<List<Object>> generateFeaturesFromTrainData();
	public abstract List<List<Object>> generateFeaturesFromTestData();
	
	
	public FeatureGenerator() {
		stopWords = new ArrayList<String>();
		loadStopWords();
	}
	
	private void loadStopWords() {
		BufferedReader br = null;
		String line = null;
		
		try {
			String[] stops = null; 
			br = new BufferedReader(new FileReader("data/stop_words.txt"));
			
			while ((line = br.readLine()) != null) {
				stops = line.split(",");
			}
			
			for(String s:stops) {
				stopWords.add(s);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
	
	public boolean isStopWord(String s) {
		return stopWords.contains(s); 
	}
	
	public String stemWords(String input) {
		return PorterStemmer.stem(input);
	}
	
	public List<Object[]> getCompetitionFeatures(boolean isTrain)  {
		DBAccess db = new DBAccess();
		List<Object[]> list = db.getRecords(isTrain?"train":"test");
		return list;
	}
	
	public String formatStringToJSON(String json_str) {
		
		json_str = json_str.substring(1, json_str.length()-1);
		//System.out.println(json_str);
		json_str = json_str.replace("{", "");
		json_str = json_str.replace("}", "");
		//json_str = json_str.replace("title\":\"", "title");
		//System.out.println(json_str);
		//System.out.println("Idnex of:"+json_str.indexOf("\"",0));
		json_str = "{" + json_str + "}";
		json_str = json_str.replace("\\","\\\\");
		if(json_str.indexOf("{title:")!=-1) {						
			json_str = json_str.replace("{title:","{\"title\":\"").replace(",body:","\",\"body\":\"").replace(",url:","\",\"url\":\"").replace("}","\"}");
		} 
		else if(json_str.indexOf("{body:")!=-1) {
			json_str = json_str.replace("{body:","{\"body\":\"").replace(",title:","\",\"title\":\"").replace(",url:","\",\"url\":\"").replace("}","\"}");
		}
		else if(json_str.indexOf("{url:")!=-1) {
			json_str = json_str.replace("{url:","{\"url\":\"").replace(",title:","\",\"title\":\"").replace(",body:","\",\"body\":\"").replace("}","\"}");
		}
		
		return json_str;
	}
	
	public static Instances convertToWekaFeatures(List<List<Object>> features, String[] attributeNames,boolean isTrainPhase) {
		
		Instances featureSet = null;
		
		String[] subAttribs = Arrays.copyOfRange(attributeNames, 0, attributeNames.length-1);
		
		//Preparing the feature vectors
		FastVector featureVectors = new FastVector();
		
		for(String attrib:subAttribs) {
			//System.out.println("Attrib-->"+attrib);
			featureVectors.addElement(new Attribute(attrib));
		}
		
		FastVector classVector = new FastVector(2);
		if(isTrainPhase) {
			classVector.addElement("e");//evergreen
			classVector.addElement("n");//ephemeral
		} else {
			classVector.addElement("?");
		}
		
		Attribute classAttrib = new Attribute(attributeNames[attributeNames.length-1],classVector);
		featureVectors.addElement(classAttrib);
		
		featureSet = new Instances("StumbleFeatures", featureVectors, 10);
		featureSet.setClassIndex(attributeNames.length-1);
		
		//Forming Weka Instances
		for(List<Object> list:features) {
			Instance feat = new Instance(attributeNames.length);
			int idx = 0;
			for(Object str:list) {
				//System.out.println(idx+".) Attrib-->"+attributeNames[idx]);
				if(str instanceof Double) {
					feat.setValue((Attribute)featureVectors.elementAt(idx),(Double)str);
				} 
				else if(str instanceof String) {
					feat.setValue((Attribute)featureVectors.elementAt(idx),str.toString());
				}
				idx++;
			}
			featureSet.add(feat);
		}
		
		return featureSet;
		
	}
	
}

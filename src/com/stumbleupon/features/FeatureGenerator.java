package com.stumbleupon.features;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

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
			String[] stops = new String[] {"a","able","about","across","after","all","almost","also","am","among","an","and","any","are","as","at","be","because","been","but","by","can","cannot","could","dear","did","do","does","either","else","ever","every","for","from","get","got","had","has","have","he","her","hers","him","his","how","however","i","if","in","into","is","it","its","just","least","let","like","likely","may","me","might","most","must","my","neither","no","nor","not","of","off","often","on","only","or","other","our","own","rather","said","say","says","she","should","since","so","some","than","that","the","their","them","then","there","these","they","this","tis","to","too","twas","us","wants","was","we","were","what","when","where","which","while","who","whom","why","will","with","would","yet","you","your"}; 
			////br = new BufferedReader(new FileReader("data/stop_words.txt"));

			/*while ((line = br.readLine()) != null) {
				stops = line.split(",");
			}*/

			for(String s:stops) {
				stopWords.add(s);
			}
		} /*catch (FileNotFoundException e) {
			e.printStackTrace();
		} */catch (Exception e) {
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

	//if isTrainWithin is true, 75% of training dataset will be for training and remaining 25% for testing
	public static List<Object[]> getCompetitionFeatures(boolean isTrain,boolean isTrainWithin)  {
		DBAccess db = new DBAccess();
		List<Object[]> list = null;

		if(isTrainWithin && isTrain) {
			list = db.getRecords("train");
			list = list.subList(0, (int)(0.75*list.size()) );
		} 
		else if(isTrainWithin && !isTrain)  {
			list = db.getRecords("train");
			list = list.subList((int)(0.75*list.size()), list.size() );
		} else { 
			list = db.getRecords(isTrain?"train":"test");
		}

		return list;
	}

	//if isTrainWithin is true, 75% of training dataset will be for training and remaining 25% for testing
	public static List<Map<String, Object>> getCompetitionFeaturesMap(boolean isTrain,boolean isTrainWithin)  {
		DBAccess db = new DBAccess();
		List<Map<String, Object>> list = null;

		if(isTrainWithin && isTrain) {
			list = db.getDataMaps("train");
			list = list.subList(0, (int)(0.75*list.size()) );
		} 
		else if(isTrainWithin && !isTrain)  {
			list = db.getDataMaps("train");
			list = list.subList((int)(0.75*list.size()), list.size() );
		} else { 
			list = db.getDataMaps(isTrain?"train":"test");
			//testing
			//list = list.subList(0, 100);
		}

		return list;
	}

	public static List<String[]> getBoilerPlateText(boolean isTrain,boolean isTrainWithin)  {
		DBAccess db = new DBAccess();
		List list = null;

		if(isTrainWithin && isTrain) {
			list = db.getDataMaps("train","boilerplate");
			list = list.subList(0, (int)(0.75*list.size()) );
		} 
		else if(isTrainWithin && !isTrain)  {
			list = db.getDataMaps("train","boilerplate");
			list = list.subList((int)(0.75*list.size()), list.size() );
		} else { 
			list = db.getDataMaps(isTrain?"train":"test","boilerplate");
			//testing
			//list = list.subList(0,500);
		}
		Iterator iter = list.iterator();
		int idx = 1;
		List<String[]> text = new ArrayList<String[]>();
		while(iter.hasNext()) {
			String json_str = formatStringToJSON(iter.next().toString());
			iter.remove();
			JSONObject jsonObj;
			try {
				//System.out.println(json_str);
				jsonObj = new JSONObject(json_str);
				//System.out.println(idx+"-->)"+jsonObj.get("title").toString());
				json_str = (String)jsonObj.get("body").toString().trim().toLowerCase();
				//json_str = (String)jsonObj.get("url").toString().trim().toLowerCase();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			json_str = json_str.replaceAll("[^0-9a-z\\s]", " . ");
			text.add(json_str.split(" "));
			idx++;
		}
		System.out.println("Collection over");
		return text;
	}

	public static List<String> getClassLabels()  {
		DBAccess db = new DBAccess();

		Iterator iter = db.getDataMaps("train","label").iterator();
		List<String> classlbl = new ArrayList<String>();
		while(iter.hasNext()) {
			classlbl.add(iter.next().toString());
		}
		System.out.println("Collection over");
		return classlbl;
	}
	
	public static List<String> getNewsLabels(boolean isTrain)  {
		DBAccess db = new DBAccess();
		
		String col = isTrain?"train":"test";
		
		Iterator iter = db.getDataMaps(col,"is_news").iterator();
		List<Object> newsFrontPage = db.getDataMaps(col, "news_front_page");
		List<String> classlbl = new ArrayList<String>();
		int idx = 0;
		while(iter.hasNext()) {
			classlbl.add(iter.next().toString()+","+newsFrontPage.get(idx));
			idx++;
		}
		System.out.println("Collection over");
		return classlbl;
	}

	public static String formatStringToJSON(String json_str) {

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

	public  Instances convertToWekaFeatures(List<List<Object>> features, String[] attributeNames,boolean isTrainPhase) {
		FastVector alchemy_catogories = new FastVector();
		alchemy_catogories.addElement("business");
		alchemy_catogories.addElement("recreation");
		alchemy_catogories.addElement("health");
		alchemy_catogories.addElement("sports");
		alchemy_catogories.addElement("arts_entertainment");
		alchemy_catogories.addElement("computer_internet");
		alchemy_catogories.addElement("science_technology");
		alchemy_catogories.addElement("culture_politics");
		alchemy_catogories.addElement("gaming");
		alchemy_catogories.addElement("law_crime");
		alchemy_catogories.addElement("religion");
		alchemy_catogories.addElement("weather");
		alchemy_catogories.addElement("unknown");
		
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
				if(attrib.equals("alchemy_category")) {
					featureVectors.addElement(new Attribute(attrib,alchemy_catogories));
				}
				else {
					featureVectors.addElement(new Attribute(attrib,(FastVector)null));
				}
			}
			
			idx++;
		}
		
		FastVector classVector = new FastVector(2);
		classVector.addElement("0");//evergreen
		classVector.addElement("1");//ephemeral
		//if(!isTrainPhase) {
		classVector.addElement("?");
		//}
		
		Attribute classAttrib = new Attribute(attributeNames[attributeNames.length-1],classVector);
		featureVectors.addElement(classAttrib);
		
		featureSet = new Instances("StumbleFeatures", featureVectors, 10);
		featureSet.setClassIndex(attributeNames.length-1);
		
		//Forming Weka Instances
		for(List<Object> list:features) {
			Instance feat = new Instance(attributeNames.length);
			//System.out.println("length-->"+attributeNames.length);
			idx = 0;
			for(Object str:list) {
				System.out.println(idx);
				System.out.println(str);
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
			featureSet.add(feat);
		}
		
		return featureSet;
	}

}

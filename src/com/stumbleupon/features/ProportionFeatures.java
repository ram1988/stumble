package com.stumbleupon.features;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.*;

import weka.core.Instances;

import com.stumbleupon.classifier.BuildModelException;
import com.stumbleupon.classifier.Classifiers;
import com.stumbleupon.classifier.EvalResult;
import com.stumbleupon.classifier.EvaluationException;
import com.stumbleupon.classifier.WekaClassifier;
import com.stumbleupon.reader.CSVReader;
import com.stumbleupon.reader.DBAccess;

public class ProportionFeatures extends FeatureGenerator {
	
	private List<Map<String, Object>> list;
	private List<List<String>> tokenized;
	private Map<String,Integer> evergreenMap;
	private Map<String,Integer> ephimeralMap;
	
	private void preprocessData(int train_or_test) {
		
		tokenized = new ArrayList<List<String>>();
		String unwanted = "^[0-9]+[^0-9a-z]$|^[a-z]$|^[^0-9a-z]|^[0-9]*$";
		
		//1-Training data,0-Test Data
		if(train_or_test == 1) {
			
			for(Map<String, Object> str:list) {
				//JSON representation 
				String processedString = null;
				JSONObject jsonObj;
				try {
					String json_str = formatStringToJSON(str.get("boilerplate").toString());
					jsonObj = new JSONObject(json_str);
					processedString = (String)jsonObj.get("body").toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//str[str.length-1] = str[str.length-1].toString().equals("1")?"e":"n";
				String[] tok = processedString.split(" ");
				
				List<String> tokens = new ArrayList<String>();
				for(String s:tok) {
					 //lower case
					 s = s.toLowerCase();
					 
					 s = s.replaceAll("[^0-9a-z]","");
					 
					 //Number or single character removal  and perform Stop word removal
					 if(s.matches(unwanted) || isStopWord(s)) {
						 continue;
					 }
					 
					//perform stemming
					s = stemWords(s);
					
					tokens.add(s);
				}
				tokenized.add(tokens);
			}
		} else {
			tokenized.clear();
			for(Map<String, Object> str:list) { 
				//JSON representation 
				String processedString = null;
				JSONObject jsonObj;
				try {					
					String json_str = formatStringToJSON(str.get("boilerplate").toString());
					jsonObj = new JSONObject(json_str);
					processedString = (String)jsonObj.get("body").toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String[] tok = processedString.split(" ");
				
				List<String> tokens = new ArrayList<String>();
				for(String s:tok) {
					 //lower case
					 s = s.toLowerCase();
					 
					 s = s.replaceAll("[^0-9a-z]","");
					 
					 //Number or single character removal  and perform Stop word removal
					 if(s.matches(unwanted) || isStopWord(s)) {
						 continue;
					 }				
					//perform stemming
					s = stemWords(s);
					tokens.add(s);
				}
				tokenized.add(tokens);
			}
		}
	}
	
	private Map<String,Integer> getWordMap(String label) {
		Map<String,Integer> wordMap = new TreeMap<String,Integer>();
		int idx = 0;
		//System.out.println("List Size:"+list.size());
		for(Map<String, Object> str:list) {
			if(label.equals(str.get("label").toString())) {
				List<String> tok = tokenized.get(idx);
				for(String s:tok) {
					//System.out.println((s.equals("2015,")?"------------EXISTS":""));
					//if term exists, increment the term freq. by one 
					if(wordMap.containsKey(s)) {
						wordMap.put(s,wordMap.get(s)+1);
					} else {
						wordMap.put(s, 1);
					}
				}
			}
			idx++;
		}
		//System.out.println("Map Size:"+wordMap.size());
		return wordMap;
	}
	
	
	@Override
	public List<List<Object>> generateFeaturesFromTrainData() {
		/*CSVReader obj = new CSVReader("data/train.tsv","\t");
		list = obj.readCSV();
		*/
		list = getCompetitionFeaturesMap(true,false);
				
		preprocessData(1);
		
		evergreenMap = getWordMap("1");
		ephimeralMap = getWordMap("0");
		
		List<List<Object>> featureList = new ArrayList<List<Object>>();
		
		int idx = 0;
		Set<String> set_toks = new TreeSet<String>();
		for(List<String> toks:tokenized) {
			//System.out.println(str[1]);
			int ever = 0, ephi = 0;
			
			//Making unique list of terms as Set
			set_toks.clear();
			set_toks.addAll(toks);
			
			for(String s:set_toks) {
				//System.out.println(s);
				int ev_ct = (evergreenMap.get(s)!=null)?evergreenMap.get(s):0;
				int ep_ct = (ephimeralMap.get(s)!=null)?ephimeralMap.get(s):0;
				
				/*
				 * If term occurence is more in evergreen, remove from ephimeral.. otherwise..
				 * So, exclusive evergreen and ephimeral maps are created
				 *  
				 */
				if(ev_ct > ep_ct) {
					ephimeralMap.remove(s);
					ever++;
				} else {
					evergreenMap.remove(s);
					ephi++;
				}
			}
			
			int total = set_toks.size();
			List<Object> features = new ArrayList<Object>();
			Map<String, Object> item = list.get(idx);
			String label = item.get("label").toString();
			
			if(total!=0) {
				features.add( new Double(((float)ever/(float)total)) );//Evergreen terms proportion
				features.add( new Double(((float)ephi/(float)total)) );//Ephimeral terms proportion
			} else {
				features.add(0.0);
				features.add(0.0);	
			}
		
			features.add(label);//ClassLabel
			
			featureList.add(features);
			idx++;
		}
		
		return featureList;
	}
	
	

	@Override
	public List<List<Object>> generateFeaturesFromTestData() {
		/*CSVReader obj = new CSVReader("data/test.tsv","\t");
		list = obj.readCSV();
		*/
		list = getCompetitionFeaturesMap(false,false);
				
		preprocessData(0);
		
		List<List<Object>> featureList = new ArrayList<List<Object>>();
		
		Set<String> set_toks = new TreeSet<String>();
		for(List<String> toks:tokenized) {
			//System.out.println(str[1]);
			int ever = 0, ephi = 0;
			
			//Making unique list of terms as Set
			set_toks.clear();
			set_toks.addAll(toks);
			
			for(String s:set_toks) {
				//System.out.println(s);
				int ev_ct = (evergreenMap.get(s)!=null)?evergreenMap.get(s):0;
				int ep_ct = (ephimeralMap.get(s)!=null)?ephimeralMap.get(s):0;
				
				/*
				 * If term occurence is more in evergreen, remove from ephimeral.. otherwise..
				 * So, exclusive evergreen and ephimeral maps are created
				 *  
				 */
				if(ev_ct > ep_ct) {
					ever++;
				} 
				else if(ev_ct < ep_ct){
					ephi++;
				} else {
					ever++;
					ephi++;
				}
			}
			
			int total = set_toks.size();
			List<Object> features = new ArrayList<Object>();
			
			if(total!=0) {	
				features.add( new Double(((float)ever/(float)total)) );//Evergreen terms proportion
				features.add( new Double(((float)ephi/(float)total)) );//Ephimeral terms proportion
			} else {
				features.add(0.0);
				features.add(0.0);	
			}
			
			features.add("?");//ClassLabel
			
			featureList.add(features);
		}
		
		return featureList;
	}
	
	public static void main(String[] args) {
		
		String[] attribNames = {"evergreen","ephimeral","class"};
		
		ProportionFeatures feat = new ProportionFeatures();
		
		String classifier = "ann";
		
		//Generating Train Features
		List<List<Object>> feats = feat.generateFeaturesFromTrainData();
		/*FileWriter fw = null;
		try {
			fw = new FileWriter("train_mongo_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedWriter bw = new BufferedWriter(fw);
		
		try {
			int len = feats.get(0).size();
			bw.write("evergreen,ephimeral,avglinksize,frameTagRatio,html_ratio,image_ratio,linkwordscore,numberOfLinks,numwords_in_url,spelling_errors_ratio,class\n");
			for(List<Object> features:feats) {
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
		}*/
	
		//Programmatic Classification 
		//Build Model
		Classifiers classifiers = new WekaClassifier(classifier);
		try {
			classifiers.trainClassifier(feats,attribNames);
		} catch (BuildModelException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		//Generating Test Features
		feats = feat.generateFeaturesFromTestData();
		/*fw = null;
		try {
			fw = new FileWriter("test_mongo_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		bw = new BufferedWriter(fw);
		
		try {
			int len = feats.get(0).size();
			bw.write("evergreen,ephimeral,avglinksize,frameTagRatio,html_ratio,image_ratio,linkwordscore,numberOfLinks,numwords_in_url,spelling_errors_ratio,class\n");
			for(List<Object> features:feats) {
				for(int i=0;i<len-1;i++) {
					bw.write(features.get(i)+",");
				}
				bw.write(features.get(len-1)+"\n");
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
		}*/
		
		//Test Model
		EvalResult result = null;
		try {
			result = classifiers.testClassifier(feats, attribNames);
		} catch (EvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
						  
		
		//System.out.println("Predicted Class Label--->"+result.getClassLabel());
		System.out.println("AUC Metric--->"+result.getAUC());
		System.out.println("Precision--->"+result.getPrecision());
		System.out.println("Recall--->"+result.getRecall());
		System.out.println("F-Measure--->"+result.getFmeasure());
		
		
		FileWriter fw = null;
		try {
			fw = new FileWriter("test_labels_"+classifier+".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedWriter bw = new BufferedWriter(fw);
		List<Map<String, Object>> list = getCompetitionFeaturesMap(false,false);
				
		int trueCt = 0, falseCt = 0;
		try {
			int i = 0;
			for(String predicted:result.getClassLabel()) {
				Map<String, Object> item = list.get(i);
				String original = item.get("label").toString();
				if(predicted.equals(original))  
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

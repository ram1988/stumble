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
import com.stumbleupon.reader.CSVReader;
import com.stumbleupon.reader.DBAccess;

public class ProportionFeatures extends FeatureGenerator {
	
	private List<Object[]> list;
	private List<List<String>> tokenized;
	private Map<String,Integer> evergreenMap;
	private Map<String,Integer> ephimeralMap;
	
	private void preprocessData(int train_or_test) {
		
		tokenized = new ArrayList<List<String>>();
		String unwanted = "^[0-9]+[^0-9a-z]$|^[a-z]$|^[^0-9a-z]|^[0-9]*$";
		
		//1-Training data,0-Test Data
		if(train_or_test == 1) {
			
			for(Object[] str:list) {
				//JSON representation 
				String processedString = null;
				JSONObject jsonObj;
				try {
					
					String json_str = str[2].toString();
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
					//System.out.println(json_str);
					jsonObj = new JSONObject(json_str);
					processedString = (String)jsonObj.get("body").toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				str[str.length-1] = str[str.length-1].toString().equals("1")?"e":"n";
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
			for(Object[] str:list) {
				//JSON representation 
				String processedString = null;
				JSONObject jsonObj;
				try {
					
					String json_str = str[2].toString();
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
					//System.out.println(json_str);
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
		for(Object[] str:list) {
			if(label.equals(str[str.length-1].toString())) {
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
		DBAccess db = new DBAccess();
		list = db.getRecords("train");
		preprocessData(1);
		
		evergreenMap = getWordMap("e");
		ephimeralMap = getWordMap("n");
		
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
			Object[] item = list.get(idx);
			String label = item[item.length-1].toString();
			
			if(total!=0) {				
				features.add(new Double(( (Float) (((float)ever/(float)total)*100) ).toString()) );//Evergreen terms proportion
				features.add(new Double(( (Float) (((float)ephi/(float)total)*100) ).toString()) );//Ephimeral terms proportion
			} else {
				//List<String> features = new ArrayList<String>();
								
				//String[] item = list.get(idx);
				//String label = item[item.length-1];
				
			
				/* if(label.equals("e")) {
					features.add("100.0");
					features.add("0");
				} else {
					features.add("0");
					features.add("100.0");
				}*/
				features.add(0.0);
				features.add(0.0);
				
			}
			features.add(new Double(item[5].toString())); //avglinksize
			features.add(new Double(item[13].toString())); //frameTagRatio
			features.add(new Double(item[15].toString()));//html_ratio
			features.add(new Double(item[16].toString()));//image_ratio
			features.add(new Double(item[19].toString()));//linkwordscore
			features.add(new Double(item[22].toString()));//numberOfLinks
			features.add(new Double(item[23].toString()));//numwords_in_url
			features.add(new Double(item[25].toString()));//spelling_errors_ratio
			
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
		DBAccess db = new DBAccess();
		list = db.getRecords("test");
		preprocessData(0);
		
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
					ever++;
				} else {
					ephi++;
				}
			}
			
			int total = set_toks.size();
			List<Object> features = new ArrayList<Object>();
			Object[] item = list.get(idx);
			String label = item[item.length-1].toString();
			
			if(total!=0) {				
				features.add( new Double(( (Float) (((float)ever/(float)total)*100) ).toString()) );//Evergreen terms proportion
				features.add( new Double(( (Float) (((float)ephi/(float)total)*100) ).toString()) );//Ephimeral terms proportion
			} else {
				//List<String> features = new ArrayList<String>();
								
				//String[] item = list.get(idx);
				//String label = item[item.length-1];
				features.add(0.0);
				features.add(0.0);	
			}
			features.add(new Double(item[5].toString())); //avglinksize
			features.add(new Double(item[13].toString())); //frameTagRatio
			features.add(new Double(item[15].toString()));//html_ratio
			features.add(new Double(item[16].toString()));//image_ratio
			features.add(new Double(item[19].toString()));//linkwordscore
			features.add(new Double(item[22].toString()));//numberOfLinks
			features.add(new Double(item[23].toString()));//numwords_in_url
			features.add(new Double(item[25].toString()));//spelling_errors_ratio
			
			features.add("?");//ClassLabel
			
			featureList.add(features);
			idx++;
		}
		
		return featureList;
	}
	
	public static void main(String[] args) {
		
		String[] attribNames = {"evergreen","ephimeral","avglinksize","frameTagRatio","html_ratio","image_ratio","linkwordscore","numberOfLinks","numwords_in_url","spelling_errors_ratio","class"};
		
		ProportionFeatures feat = new ProportionFeatures();
		
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
		try {
			Classifiers.trainClassifier("bayes", feats,attribNames);
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
			result = Classifiers.testClassifier(feats, attribNames);
		} catch (EvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
						  
		
		//System.out.println("Predicted Class Label--->"+result.getClassLabel());
		System.out.println("AUC Metric--->"+result.getAUC());
		System.out.println("Precision--->"+result.getPrecision());
		System.out.println("Recall--->"+result.getRecall());
		System.out.println("F-Measure--->"+result.getFmeasure());
	}


}

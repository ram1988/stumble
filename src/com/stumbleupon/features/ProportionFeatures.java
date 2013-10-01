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

import com.stumbleupon.reader.CSVReader;

public class ProportionFeatures extends FeatureGenerator {
	
	private List<String[]> list;
	private List<List<String>> tokenized;
	private Map<String,Integer> evergreenMap;
	private Map<String,Integer> ephimeralMap;
	
	private void preprocessData(int train_or_test) {
		
		tokenized = new ArrayList<List<String>>();
		
		//1-Training data,0-Test Data
		if(train_or_test == 1) {
			for(String[] str:list) {
				//JSON representation 
				String processedString = null;
				JSONObject jsonObj;
				try {
					//System.out.println(str[2]);
					jsonObj = new JSONObject(str[2]);
					processedString = (String)jsonObj.get("body").toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				str[str.length-1] = str[str.length-1].equals("1")?"e":"n";
				String[] tok = processedString.split(" ");
				
				List<String> tokens = new ArrayList<String>();
				for(String s:tok) {
					 //lower case
					 s = s.toLowerCase();
					 //perform Stop word removal
					 if(isStopWord(s)) {
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
			for(String[] str:list) {
				//JSON representation 
				String processedString = null;
				JSONObject jsonObj;
				try {
					//System.out.println(str[2]);
					jsonObj = new JSONObject(str[2]);
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
					 //perform Stop word removal
					 if(isStopWord(s)) {
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
		System.out.println("List Size:"+list.size());
		for(String[] str:list) {
			if(label.equals(str[str.length-1])) {
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
		System.out.println("Map Size:"+wordMap.size());
		return wordMap;
	}
	
	
	@Override
	public List<List<String>> generateFeaturesFromTrainData() {
		CSVReader obj = new CSVReader("data/train.tsv","\t");
		list = obj.readCSV();
		
		preprocessData(1);
		
		evergreenMap = getWordMap("e");
		ephimeralMap = getWordMap("n");
		
		List<List<String>> featureList = new ArrayList<List<String>>();
		
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
			List<String> features = new ArrayList<String>();
			String[] item = list.get(idx);
			String label = item[item.length-1];
			
			if(total!=0) {				
				features.add(( (Float) (((float)ever/(float)total)*100) ).toString());//Evergreen terms proportion
				features.add(( (Float) (((float)ephi/(float)total)*100) ).toString());//Ephimeral terms proportion
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
				features.add("0");
				features.add("0");
				
			}
			features.add(item[5]); //avglinksize
			features.add(item[13]); //frameTagRatio
			features.add(item[15]);//html_ratio
			features.add(item[16]);//image_ratio
			features.add(item[19]);//linkwordscore
			features.add(item[22]);//numberOfLinks
			features.add(item[23]);//numwords_in_url
			features.add(item[25]);//spelling_errors_ratio
			
			features.add(label);//ClassLabel
			
			featureList.add(features);
			idx++;
		}
		
		return featureList;
	}
	
	

	@Override
	public List<List<String>> generateFeaturesFromTestData() {
		CSVReader obj = new CSVReader("data/test.tsv","\t");
		list = obj.readCSV();
		
		preprocessData(0);
		
		List<List<String>> featureList = new ArrayList<List<String>>();
		
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
			List<String> features = new ArrayList<String>();
			String[] item = list.get(idx);
			String label = item[item.length-1];
			
			if(total!=0) {				
				features.add(( (Float) (((float)ever/(float)total)*100) ).toString());//Evergreen terms proportion
				features.add(( (Float) (((float)ephi/(float)total)*100) ).toString());//Ephimeral terms proportion
			} else {
				//List<String> features = new ArrayList<String>();
								
				//String[] item = list.get(idx);
				//String label = item[item.length-1];
				features.add("0");
				features.add("0");	
			}
			features.add(item[5]); //avglinksize
			features.add(item[13]); //frameTagRatio
			features.add(item[15]);//html_ratio
			features.add(item[16]);//image_ratio
			features.add(item[19]);//linkwordscore
			features.add(item[22]);//numberOfLinks
			features.add(item[23]);//numwords_in_url
			features.add(item[25]);//spelling_errors_ratio
			
			featureList.add(features);
			idx++;
		}
		
		return featureList;
	}
	
	public static void main(String[] args) { 
		ProportionFeatures feat = new ProportionFeatures();
		
		//Generating Train Features
		List<List<String>> feats = feat.generateFeaturesFromTrainData();
		FileWriter fw = null;
		try {
			fw = new FileWriter("train_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedWriter bw = new BufferedWriter(fw);
		
		try {
			int len = feats.get(0).size();
			bw.write("evergreen,ephimeral,avglinksize,frameTagRatio,html_ratio,image_ratio,linkwordscore,numberOfLinks,numwords_in_url,spelling_errors_ratio,class\n");
			for(List<String> features:feats) {
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
		
		System.out.println("The train feature size is--->"+feats.size());
		
		//Generating Test Features
		feats = feat.generateFeaturesFromTestData();
		fw = null;
		try {
			fw = new FileWriter("test_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		bw = new BufferedWriter(fw);
		
		try {
			int len = feats.get(0).size();
			bw.write("evergreen,ephimeral,avglinksize,frameTagRatio,html_ratio,image_ratio,linkwordscore,numberOfLinks,numwords_in_url,spelling_errors_ratio,class\n");
			for(List<String> features:feats) {
				for(int i=0;i<len-1;i++) {
					bw.write(features.get(i)+",");
				}
				bw.write(features.get(len-1)+",?\n");
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
		
		System.out.println("The test feature size is--->"+feats.size());
	}


}

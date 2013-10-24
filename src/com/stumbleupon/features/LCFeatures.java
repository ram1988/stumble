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
import java.text.DecimalFormat;

import org.json.*;

import weka.core.Instances;

import com.stumbleupon.classifier.BuildModelException;
import com.stumbleupon.classifier.Classifiers;
import com.stumbleupon.classifier.EvalResult;
import com.stumbleupon.classifier.EvaluationException;
import com.stumbleupon.classifier.WekaClassifier;
import com.stumbleupon.reader.CSVReader;
import com.stumbleupon.reader.DBAccess;

public class LCFeatures extends FeatureGenerator {

	private List<Map<String, Object>> list;
	private List<List<String>> tokenized;
	private Map<String,Integer> evergreenMap;
	private Map<String,Integer> ephimeralMap;
	private int wnd_size = 5;
	private int no_feats = 20;
	private DecimalFormat df = new DecimalFormat("#.#");

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
		for(List<String> toks:tokenized) {
			System.out.println(idx);
			int ct = 0;

			int length = toks.size();
			List<Object> features = new ArrayList<Object>();

			if(length!=0) {
				while(ct < length) {
					int ever = 0, ephi = 0;
					int init = ct;
					int fnl = ct + wnd_size;

					for(int i=init;i<fnl && i<length;i++) {							
						String s = toks.get(i);														

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
					//features.add(new Double(ever));
					//features.add(new Double(ephi));
					features.add(new Double(df.format( ((float)ever/(float)wnd_size)) ));
					features.add(new Double(df.format(((float)ephi/(float)wnd_size)) ));
					ct+=(wnd_size+1);
				}

				int feat_size = features.size();
				if(feat_size < no_feats) {
					ct = feat_size;
					for(int i=ct,j=0;i<no_feats;i++,j++) {
						features.add(features.get(j));
					}
				}
				else if(feat_size > no_feats) {
					features = features.subList(0, no_feats);
				}
			} else {
				for(int i=0;i<no_feats;i++) {
					features.add(0.0);
				}
			}

			Map<String, Object> item = list.get(idx);
			String label = item.get("label").toString();

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

		for(List<String> toks:tokenized) {
			//System.out.println(str[1]);
			int ct = 0;

			int length = toks.size();
			List<Object> features = new ArrayList<Object>();

			if(length!=0) {
				while(ct < length) {
					int ever = 0, ephi = 0;
					int init = ct;
					int fnl = ct + wnd_size;

					for(int i=init;i<=fnl && i<length;i++) {
						String s = toks.get(i);

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
					//features.add(new Double(ever));
					//features.add(new Double(ephi));
					features.add(new Double(df.format( ((float)ever/(float)wnd_size)) ));
					features.add(new Double(df.format(((float)ephi/(float)wnd_size)) ));
					ct+=wnd_size;
				}

				int feat_size = features.size();
				if(feat_size < no_feats) {
					ct = feat_size;
					for(int i=ct,j=0;i<no_feats;i++,j++) {
						features.add(features.get(j));
					}
				}
				else if(feat_size > no_feats) {
					features = features.subList(0, no_feats);
				}
			} else {
				for(int i=0;i<no_feats;i++) {
					features.add(0.0);
				}
			}

			features.add("?");//ClassLabel

			featureList.add(features);
		}

		return featureList;
	}

	public static void main(String[] args) {

		String[] attribNames = new String[21];

		for(int i=0;i<20;i++) {
			attribNames[i] = "feat"+i;
		}

		attribNames[20] = "class";

		LCFeatures feat = new LCFeatures();

		String classifier = "ann";

		//Generating Train Features
		List<List<Object>> feats = feat.generateFeaturesFromTrainData();
		FileWriter fw = null;
		try {
			fw = new FileWriter("train_mongo_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		BufferedWriter bw = new BufferedWriter(fw);

		try {
			for(int i=0;i<attribNames.length-1;i++) {
				bw.write(attribNames[i]+",");
			}
			bw.write("class\n");
			int len = feats.get(0).size();
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
		}

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

		fw = null;
		try {
			fw = new FileWriter("test_mongo_features.csv");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		bw = new BufferedWriter(fw);

		try {
			for(int i=0;i<attribNames.length-1;i++) {
				bw.write(attribNames[i]+",");
			}
			bw.write("class\n");
			int len = feats.get(0).size();
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
		}

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


		fw = null;
		try {
			fw = new FileWriter("test_labels_"+classifier+".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		bw = new BufferedWriter(fw);
		List<Map<String, Object>> list = getCompetitionFeaturesMap(false,false);

		int trueCt = 0, falseCt = 0;
		try {
			int i = 0;
			for(String predicted:result.getClassLabel()) {
				Map<String, Object> item = list.get(i);
				String original = item.get("label")!=null?item.get("label").toString():"";
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

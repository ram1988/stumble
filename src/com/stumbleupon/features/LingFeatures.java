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

public class LingFeatures extends FeatureGenerator {
	
	
	private DecimalFormat decifrmt = new DecimalFormat("#.#");
	private MaxentTagger tagger;

	public LingFeatures() {
		tagger =  new MaxentTagger("taggers/english-bidirectional-distsim.tagger");
	}
	
	
	@Override
	public List<List<Object>> generateFeaturesFromTrainData() {

		List<Map<String, Object>> text = getCompetitionFeaturesMap(true, false); 
		System.out.println("List Size:"+text.size());
		
		List<List<Object>> features = new ArrayList<List<Object>>();
		int idx = 0;
		
		try {
			Pattern pat = Pattern.compile("^[0-9]");
			//br = new BufferedReader(new FileReader("tagged_train.txt"));

			for(Map<String, Object> str:text) {
				System.out.println("Doc--->"+idx);
				String processedString = str.get("boilerplate").toString();
				processedString = processedString.trim().replaceAll("[^0-9a-z\\s]","");
				processedString = processedString.trim().replaceAll("[0-9]","");
				
				List<Object> ling_feats = new ArrayList<Object>();
				int noun_ct = 0, fw_ct = 0, nw = 0, adj_ct=0, adv_ct=0;
				
				String[] content = processedString.split(" ");
				for(String s:content) {
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
					
					if( !isStopWord(s) ) {
						nw++;
						if( grammar.matches("NN|NNP|NNS|NNS") ) {
							noun_ct++;
						}
						else if(grammar.matches("JJ|JJR|JJS")) {
							adj_ct++;
						}
						else if(grammar.matches("RB|RBR|RBS")) {
							adv_ct++;
						}
						else if(grammar.equals("FW")) {
							fw_ct++;
						}
					}
				}
				
				//ling_feats.add(new Integer(nw));
				ling_feats.add(new Integer(noun_ct));
				//ling_feats.add(new Integer(fw_ct));
				//Double ratio_adj_adv = new Double( (float)adj_ct/(float)adv_ct );
				//ling_feats.add(ratio_adj_adv.isNaN()?0.0:ratio_adj_adv);
				ling_feats.add(str.get("label").toString());
				features.add(ling_feats);
				idx++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		return features;
	}

	@Override
	public List<List<Object>> generateFeaturesFromTestData() {

		List<Map<String, Object>> text = getCompetitionFeaturesMap(false, false); 
		System.out.println("List Size:"+text.size());
		
		List<List<Object>> features = new ArrayList<List<Object>>();
		int idx = 0;
		try {
			Pattern pat = Pattern.compile("^[0-9]");
			//br = new BufferedReader(new FileReader("tagged_train.txt"));

			for(Map<String, Object> str:text) {
				//System.out.println("Doc--->"+idx);
				String processedString = str.get("boilerplate").toString();
				processedString = processedString.trim().replaceAll("[^0-9a-z\\s]","");
				processedString = processedString.trim().replaceAll("[0-9]","");
				
				List<Object> ling_feats = new ArrayList<Object>();
				int noun_ct = 0, fw_ct = 0, nw = 0, adj_ct=0, adv_ct=0;
				
				String[] content = processedString.split(" ");
				for(String s:content) {
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
					
					if( !isStopWord(s) ) {
						nw++;
						if( grammar.matches("NN|NNP|NNS|NNS") ) {
							noun_ct++;
						}
						else if(grammar.matches("JJ|JJR|JJS")) {
							adj_ct++;
						}
						else if(grammar.matches("RB|RBR|RBS")) {
							adv_ct++;
						}
						else if(grammar.equals("FW")) {
							fw_ct++;
						}
					}
				}
				
				//ling_feats.add(new Integer(nw));
				ling_feats.add(new Integer(noun_ct));
				//ling_feats.add(new Integer(fw_ct));
				//Double ratio_adj_adv = new Double( (float)adj_ct/(float)adv_ct );
				//ling_feats.add(ratio_adj_adv.isNaN()?0.0:ratio_adj_adv);
				ling_feats.add("?");
				features.add(ling_feats);
				idx++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		return features;
	}
	
	


	public static void main(String[] args) {

		String classifier = "svm";
		LingFeatures bow = new LingFeatures();

		List<List<Object>> list = bow.generateFeaturesFromTrainData();
		String[] attribNames = new String[]{ "noun_ct","class"};
		
		//Build Model
		//Build Model
		Classifiers classifiers = new WekaClassifier(classifier,bow);
		

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


		FileWriter fw = null;
		try {
			fw = new FileWriter("bow_test_labels"+classifier+".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		BufferedWriter bw = new BufferedWriter(fw);
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
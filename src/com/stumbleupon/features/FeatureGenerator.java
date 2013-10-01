package com.stumbleupon.features;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class FeatureGenerator {
	
	private List<String> stopWords;
	public abstract List<List<String>> generateFeaturesFromTrainData();
	public abstract List<List<String>> generateFeaturesFromTestData();
	
	
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
	
}

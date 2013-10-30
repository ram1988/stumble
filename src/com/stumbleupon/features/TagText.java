package com.stumbleupon.features;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
 
public class TagText {
    public static void main(String[] args) throws IOException,
            ClassNotFoundException {
 
        // Initialize the tagger
        MaxentTagger tagger = new MaxentTagger(
                "../taggers/english-bidirectional-distsim.tagger");
 
        // The sample string
        String sample = "car";
        
        // The tagged string
        String tagged = tagger.tagString(sample.toLowerCase());
 
        // Output the result
        System.out.println(tagged);
        
        FileWriter fw = null;
		try {
			fw = new FileWriter("../tagged_train.txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedWriter bw = new BufferedWriter(fw);
		
		List<String[]> text = FeatureGenerator.getBoilerPlateText(true, false);
		
		System.out.println("Tagging going on--->"+text.size());
		
		
		int idx = 1;
		
		for(String[] str:text) {
			
			System.out.println((idx)+"length--->"+str.length);
			for(String processedString:str) {
				processedString = processedString.replaceAll("\\\\","");
				processedString = tagger.tagString(processedString);
				if(!processedString.equals("")) {
					//System.out.print(processedString+" ");
					bw.write(processedString+" ");
				} 
			}
			//System.out.print("\n");
			bw.write("\n");
			bw.flush();
			idx++;			
		}

    }
}
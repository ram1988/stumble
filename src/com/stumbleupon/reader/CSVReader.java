package com.stumbleupon.reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class CSVReader {
      
	  private String file, delim;
	  public CSVReader(String file,String delim) {
		  this.file = file;
		  this.delim = delim;
	  }
	 
	  public List<String[]> readCSV() {
	 
		BufferedReader br = null;
		String line = "";
		List<String[]> records = new ArrayList<String[]>();
		 
		try {
	 
			br = new BufferedReader(new FileReader(this.file));
			int ct = 0;
			while ((line = br.readLine()) != null) {
	           
				if(ct == 0) {
					ct = 1;
					continue;
				}
			        // use comma as separator
				String[] tokens = line.split(delim);
				records.add(tokens);	 
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
	 
		System.out.println("Done");
		return records;
	  }
	 

	public static void main(String[] args) { 
		CSVReader obj = new CSVReader(args[0],"\t");
		List<String[]> listObj = obj.readCSV();
		for(String[] str:listObj) {
			JSONObject jsonObj;
			try {
				jsonObj = new JSONObject(str[2]);
				String processedString = (String)jsonObj.get("body").toString();
				System.out.println(processedString);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	 }
}

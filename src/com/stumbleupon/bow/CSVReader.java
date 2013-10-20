
package com.stumbleupon.bow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class CSVReader {
	public static ArrayList<HashMap<String, String>> processData(String filepath) {

		int uid = 2, bp = 3, lab = 27;
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		File file = new File(filepath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			int i = 0;
			int index, end;

			br.readLine();
			while ((line = br.readLine()) != null) {

				String[] values = line.split("\t");
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("urlid", values[1].replace("\"", ""));
				
				String t = values[2];
				index = t.indexOf("\"\",\"\"body\"\":\"\"");
				end = t.indexOf("\"\",\"\"url\"\":\"\"");
				if (index > 0) {
					map.put("title", t.substring(14, index));
					if (end > 0)
						map.put("body", t.substring(index + 14, end));
					else
						map.put("body", t.substring(index + 14));
					//map.put("label", values[26].replace("\"", ""));
					map.put("label", "0");
					
					
				}
				list.add(map);		
				}
			br.close();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}

		System.out.println(list.size());
		return list;
	}
	public static void main(String args[])
	{
		String filename="C:\\Users\\sadesh\\Documents\\Apps\\input\\train.tsv";
		processData(filename);
	}
}

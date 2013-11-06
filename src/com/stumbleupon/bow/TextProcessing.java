/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stumbleupon.bow;

import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class TextProcessing {

	/**
	 * @param args
	 *            the command line arguments
	 */
	static HashMap<String, Integer> globalMap = new HashMap<String, Integer>();
	static HashMap<String, Integer> dfMap = new HashMap<String, Integer>();
	static HashMap<Integer, HashMap<String, Integer>> tfMap = new HashMap<Integer, HashMap<String, Integer>>();
	static HashMap<Integer, Double> tfidfMap = new HashMap<Integer, Double>();
	static HashMap<Integer, Double> bigramMap = new HashMap<Integer, Double>();
	static HashMap<Integer, String> labelMap = new HashMap<Integer, String>();
	static HashMap<Integer, String> urlidMap = new HashMap<Integer, String>();
	static int globalcount = 1;

	public static String getContent(String url) {
		String entityContents = "";
		try {
			HttpGet newRequest = new HttpGet(url);
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse newResponse = httpClient.execute(newRequest);
			HttpEntity entity = newResponse.getEntity();
			entityContents = EntityUtils.toString(entity);
			// System.out.println("-----Entity Contents----");
			entityContents = ArticleExtractor.INSTANCE.getText(entityContents);
			// System.out.println(entityContents);
		} catch (BoilerpipeProcessingException ex) {
			Logger.getLogger(TextProcessing.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IOException ex) {
			Logger.getLogger(TextProcessing.class.getName()).log(Level.SEVERE,
					null, ex);
		}

		return entityContents;

	}

	public static HashMap<String, Integer> getTokensCount(String content) {
		HashMap<String, Integer> tokensMap = new HashMap<String, Integer>();
		HashSet<String> stopWordSet = StopWords.getStopWords();
		StringTokenizer tokens = new StringTokenizer(content,
				" ()1234567890\\/{}[]'\"@#$%&*`,._-+:;^|");
		while (tokens.hasMoreTokens()) {
			String temp = tokens.nextToken().toLowerCase();
			TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory(
					"(-|'|\\d|\\p{L})+|\\S");
			PorterStemmerTokenizerFactory pstf = new PorterStemmerTokenizerFactory(
					TOKENIZER_FACTORY);
			Tokenizer tokenizer = pstf.tokenizer(temp.toCharArray(), 0,
					temp.length());
			Iterator<String> iterate = tokenizer.iterator();
			while (iterate.hasNext()) {
				temp = iterate.next();
				if (!stopWordSet.contains(temp)) {
					if (tokensMap.get(temp) == null) {
						tokensMap.put(temp, 1);
					} else {
						tokensMap.put(temp, tokensMap.get(temp) + 1);
					}
				}
			}
		}
		return tokensMap;

	}

	public static String getStemResults(String token) {
		TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory(
				"(-|'|\\d|\\p{L})+|\\S");
		PorterStemmerTokenizerFactory pstf = new PorterStemmerTokenizerFactory(
				TOKENIZER_FACTORY);
		Tokenizer tokenizer = pstf.tokenizer(token.toCharArray(), 0,
				token.length());
		return tokenizer.nextToken();
	}

	public static HashMap<String, Integer> getBiTokensCount(String content) {
		HashMap<String, Integer> tokensMap = new HashMap<>();
		StringTokenizer tokens = new StringTokenizer(content,
				" ()1234567890\\/{}[]'\"@#$%&*`,._-+:;^|");
		String prevToken = "";
		int count = 1;
		while (tokens.hasMoreTokens()) {

			if (count == 1) {
				prevToken = tokens.nextToken().toLowerCase();
				prevToken = getStemResults(prevToken);
				count = 0;
			}

			String token = tokens.nextToken().toLowerCase();
			token = getStemResults(token);

			String BigramToken = prevToken + token;
			System.out.println(BigramToken);
			if (tokensMap.get(BigramToken) == null)
				tokensMap.put(BigramToken, 1);
			else
				tokensMap.put(BigramToken, tokensMap.get(BigramToken) + 1);

			prevToken = token;

		}

		return tokensMap;

	}

	public static HashMap<String, Integer> getrepeatedtokens(
			HashMap<String, Integer> filter) {
		Iterator<String> iter = filter.keySet().iterator();
		HashMap<String, Integer> temp = new HashMap<String, Integer>();

		while (iter.hasNext()) {
			String key = iter.next();

			// if (filter.get(key) > 2) {
			temp.put(key, filter.get(key));
			// }

		}
		return temp;
	}

	public static void main(String[] args) throws IOException {
		String path = "C:\\NUSModules\\3rd sem\\Assignments\\KDD\\Stumble\\stumbleupon\\data\\test.tsv";
		File outFile = new File(
				"C:\\NUSModules\\3rd sem\\Assignments\\KDD\\Stumble\\stumbleupon\\data\\testout.txt");
		File urlidout=new File("C:\\NUSModules\\3rd sem\\Assignments\\KDD\\Stumble\\stumbleupon\\data\\urlidout.txt");
		HashMap<String, Integer> stemMap = new HashMap<String, Integer>();
		try {
			ArrayList<HashMap<String, String>> list = CSVReader
					.processData(path);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(urlidout));
			String readLine;
			int documentcount = 1;
			int i = 1;
			for (HashMap<String, String> map : list) {
				if (map.get("body") != null && map.get("title") != null) {
					stemMap = getrepeatedtokens(getTokensCount(map.get("title")
							+ " " + map.get("body")));
					tfMap.put(documentcount, stemMap);
					incrementDocumentFreqency(stemMap);
					urlidMap.put(documentcount,map.get("urlid"));
					labelMap.put(documentcount, map.get("label"));
				}
				documentcount++;
			}
			/*
			 * while ((readLine = buff.readLine()) != null) { if (i++ == 10) {
			 * break; } // System.out.println(readLine); DBObject obj =
			 * (DBObject) JSON.parse(readLine.substring(0,
			 * readLine.lastIndexOf(","))); String contents = (String)
			 * obj.get("content"); // System.out.println(contents); stemMap =
			 * getrepeatedtokens(getBiTokensCount(contents));
			 * tfMap.put(documentcount, stemMap);
			 * incrementDocumentFreqency(stemMap); documentcount++;
			 * 
			 * }
			 */

			Iterator<Integer> iterator = tfMap.keySet().iterator();
			while (iterator.hasNext()) {
				Integer key = iterator.next();
				TreeMap<Integer, Double> svmMap = getSvmMap(tfMap.get(key),
						documentcount);
				String svmstring = generateSvmString(svmMap);
				// System.out.println(svmstring);
				if (svmstring.trim().length() > 0) {
					bw.write(labelMap.get(key));
					bw.write(svmstring);
					bw.newLine();
					bw1.write(urlidMap.get(key));
					bw1.newLine();
				}
			}
			bw.close();
			bw1.close();

		} catch (FileNotFoundException ex) {
			Logger.getLogger(TextProcessing.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private static TreeMap<Integer, Double> getSvmMap(
			HashMap<String, Integer> stemMap, int documentcount) {
		Iterator<String> iterator = stemMap.keySet().iterator();
		TreeMap<Integer, Double> svmMap = new TreeMap<Integer, Double>();

		while (iterator.hasNext()) {
			String stem = iterator.next();

			Integer globaltoken = globalMap.get(stem);
			if (null != globaltoken) {
				svmMap.put(globaltoken,
						computetf_idf(stemMap.get(stem), stem, documentcount));
				// System.out.println("Token:" + stem + " idf " +
				// computetf_idf(stemMap.get(stem), stem, documentcount));
			} else {
				svmMap.put(globalcount,
						computetf_idf(stemMap.get(stem), stem, documentcount));
				// System.out.println("Token:" + stem + "  idf  " +
				// computetf_idf(stemMap.get(stem), stem, documentcount));
				globalMap.put(stem, globalcount++);
			}

		}
		return svmMap;
	}

	private static String generateSvmString(TreeMap<Integer, Double> svmMap) {
		StringBuilder sb = new StringBuilder();
		Iterator<Integer> itr = svmMap.keySet().iterator();
		while (itr.hasNext()) {
			Integer key = itr.next();
			sb.append(" ");
			sb.append(key);
			sb.append(":");
			sb.append(svmMap.get(key));
		}

		return sb.toString();
	}

	private static void incrementDocumentFreqency(
			HashMap<String, Integer> stemMap) {
		Iterator<String> iterator = stemMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (dfMap.get(key) != null) {
				dfMap.put(key, dfMap.get(key) + 1);
			} else {
				dfMap.put(key, 1);
			}
		}
	}

	private static Double computetf_idf(Integer value, String stem,
			int documentcount) {
		Double returnvalue = (Math.log(value) + 1)
				* Math.log(documentcount / dfMap.get(stem));

		return returnvalue;

	};
}

package com.stumbleupon.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.alchemyapi.api.AlchemyAPI;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryBuilder;
//mongo related

class CategoryFixer {
	public MongoClient mongoClient;
	public DB db;
	public DBCollection coll;
	static AlchemyAPI alchemyObj;
	
	public String category;
	public double score;
	
    public static void main(String[] args)
        throws IOException, SAXException,
               ParserConfigurationException, XPathExpressionException
    {
        // Create an AlchemyAPI object.
        alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key.txt");
        
        //connect to mongo
    	CategoryFixer cf = new CategoryFixer();
    	cf.connectToMongo();
    	cf.constructColl();
    	
    	//Query all documents from mongo collection
		DBCursor cursor = cf.coll.find();
		ArrayList<DBObject> unknownObjList = new ArrayList<DBObject>();
		try {
			while (cursor.hasNext()) {
				DBObject trainDoc = cursor.next();
				String url = (String)trainDoc.get("url");
				String cat = (String)trainDoc.get("alchemy_category");
				String boilerplate = (String)trainDoc.get("boilerplate");
				boilerplate.replace("{title:", "");
				boilerplate.replace(",body:", "");
				boilerplate.replace(",url:", "");
				boilerplate.replace("html}", "");
				if(!cat.contentEquals("?")) {
					continue;
				}
				try {
					System.out.println(url + "," + cat + " calling API with Text");
			        Document doc = alchemyObj.TextGetCategory(boilerplate);
			        System.out.println(getStringFromDocument(doc));
					ReadXML readXML = new ReadXML();
					readXML.getCategory(getStringFromDocument(doc), cf);
					System.out.println(cf.category + " " + cf.score);
					DBObject query = QueryBuilder.start("_id").is(trainDoc.get("_id")).get();
					BasicDBObject updtCategory = new BasicDBObject("alchemy_category", cf.category);
					BasicDBObject updtScore = new BasicDBObject("alchemy_category_score", cf.score);
					cf.coll.update(query, new BasicDBObject("$set", updtCategory));
					cf.coll.update(query, new BasicDBObject("$set", updtScore));
		        }
		        catch(Exception e) {
		        	try {
		        		System.out.println(url + "," + cat + " calling API with url");
		        		Document doc = alchemyObj.URLGetCategory(url);
		        		System.out.println(getStringFromDocument(doc));
						ReadXML readXML = new ReadXML();
						readXML.getCategory(getStringFromDocument(doc), cf);
						System.out.println(cf.category + " " + cf.score);
						DBObject query = QueryBuilder.start("_id").is(trainDoc.get("_id")).get();
						BasicDBObject updtCategory = new BasicDBObject("alchemy_category", cf.category);
						BasicDBObject updtScore = new BasicDBObject("alchemy_category_score", cf.score);
						cf.coll.update(query, new BasicDBObject("$set", updtCategory));
						cf.coll.update(query, new BasicDBObject("$set", updtScore));
		        	}
		        	catch(Exception ex) {
		        		System.out.println("Bummer! adding to unknown object list");
		        		unknownObjList.add(trainDoc);
		        	}
		        }
			}
		}
		finally {
			cursor.close();
		}
		System.out.println(unknownObjList.size() + " urls not categorized." );

		/*
        // Categorize a web URL by topic.
        Document doc = alchemyObj.URLGetCategory("http://www.techcrunch.com/");
        System.out.println(getStringFromDocument(doc));

        // Categorize some text.
        doc = alchemyObj.TextGetCategory("Latest on the War in Iraq.");
        System.out.println(getStringFromDocument(doc));

        // Load a HTML document to analyze.
        String htmlDoc = getFileContents("data/example.html");

        // Categorize a HTML document by topic.
        doc = alchemyObj.HTMLGetCategory(htmlDoc, "http://www.test.com/");
        System.out.println(getStringFromDocument(doc));
        
        AlchemyAPI_CategoryParams categoryParams = new AlchemyAPI_CategoryParams();
        categoryParams.setOutputMode(AlchemyAPI_Params.OUTPUT_RDF);
        doc = alchemyObj.HTMLGetCategory(htmlDoc, "http://www.test.com/", categoryParams);
        System.out.println(getStringFromDocument(doc));*/
    }

    // utility function
    private static String getFileContents(String filename)
        throws IOException, FileNotFoundException
    {
        File file = new File(filename);
        StringBuilder contents = new StringBuilder();

        BufferedReader input = new BufferedReader(new FileReader(file));

        try {
            String line = null;

            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } finally {
            input.close();
        }

        return contents.toString();
    }

    // utility method
    private static String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            return writer.toString();
        } 
        catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
	private boolean connectToMongo() {
		try {
			mongoClient = new MongoClient( "localhost" , 27017 );
			db = mongoClient.getDB( "kdd" );
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (db == null) {
			return false;
		}
		return true;
	}
	
	private void constructColl() {
		this.coll = db.getCollection("train");
	}
}

package com.stumbleupon.preprocessing;

import com.alchemyapi.api.*;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import java.io.*;
import java.net.UnknownHostException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//mongo related
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBCursor;

class CategoryKdd {
	public MongoClient mongoClient;
	public DB db;
	public DBCollection coll;
	static AlchemyAPI alchemyObj;
	
    public static void main(String[] args)
        throws IOException, SAXException,
               ParserConfigurationException, XPathExpressionException
    {
        // Create an AlchemyAPI object.
        alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key.txt");
        
        //connect to mongo
    	CategoryKdd ckdd = new CategoryKdd();
    	ckdd.connectToMongo();
    	ckdd.constructColl();
    	
    	//Query all documents from mongo collection
		DBCursor cursor = ckdd.coll.find();
		
		try {
			while (cursor.hasNext()) {
				DBObject trainDoc = cursor.next();
				String url = (String)trainDoc.get("url");
				String cat = (String)trainDoc.get("alchemy_category");
				if(!cat.contentEquals("?")) {
					continue;
				}
				try {
				Document doc = alchemyObj.URLGetCategory(url);
				System.out.println(getStringFromDocument(doc)+ "," + url);
				}
				catch (Exception e) {
					
				}
			}
		}
		finally {
			cursor.close();
		}
		
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
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
	private boolean connectToMongo() {
		try {
			mongoClient = new MongoClient( "localhost" , 27017 );
			db = mongoClient.getDB( "kdd" );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
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

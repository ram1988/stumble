package com.stumbleupon.preprocessing;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
  
public class ReadXML extends DefaultHandler {  
   
public void getCategory(String xml, CategoryFixer cf){  
	final CategoryFixer obj = cf;
	try {  
			// obtain and configure a SAX based parser  
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();  
		  
		   	// obtain object for SAX parser  
		   	SAXParser saxParser = saxParserFactory.newSAXParser();  
		  
		   	// default handler for SAX handler class  
		   	// all three methods are written in handler's body  
		   	DefaultHandler defaultHandler = new DefaultHandler() {  
		      
		    String category="close";  
		    String score="close";  
		      
		    // this method is called every time the parser gets an open tag '<'  
		    // identifies which tag is being open at time by assigning an open flag  
			public void startElement(String uri, String localName, String qName, Attributes attributes)
					throws SAXException {
		
				if (qName.equalsIgnoreCase("CATEGORY")) {
					category = "open";
				}
				if (qName.equalsIgnoreCase("SCORE")) {
					score = "open";
				}
			} 
		  
		    // prints data stored in between '<' and '>' tags  
		    public void characters(char ch[], int start, int length)  
		    		throws SAXException {  
		    	if (category.equals("open")) { 
		    		obj.category = new String(ch, start, length);
		     	}  
		     	if (score.equals("open")) {  
		     		obj.score = Double.parseDouble(new String(ch, start, length));
		     	}  
			}  
		  
		    // calls by the parser whenever '>' end tag is found in xml   
		    // makes tags flag to 'close'  
		    public void endElement(String uri, String localName, String qName)  
		    		throws SAXException {  
		    		if (qName.equalsIgnoreCase("CATEGORY")) {  
		    	 		category = "close";  
		     		}  
		     		if (qName.equalsIgnoreCase("SCORE")) {  
		    	 		score = "close";  
		     		}   
				}  
			};  
		     
			// parse the XML specified in the given path and uses supplied  
			// handler to parse the document  
			// this calls startElement(), endElement() and character() methods  
			saxParser.parse(new InputSource(new StringReader(xml)), defaultHandler);  
		} 
		catch (Exception e) {  
			e.printStackTrace();  
		}  
	}  
}  
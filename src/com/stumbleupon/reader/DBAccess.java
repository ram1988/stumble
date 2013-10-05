package com.stumbleupon.reader;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DBAccess {
	
	private MongoClient client;
	private DB db;
	
	public DBAccess() {
		try {
			client = new MongoClient( "localhost" , 27017 );
			db = client.getDB("kdd");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	public List<Object[]> getRecords(String table_name) {
		
		List<Object[]> recordList = new ArrayList<Object[]>();
		DBCollection table = db.getCollection(table_name);
		
		DBCursor cursor = table.find();
	 
		System.out.println();
		while (cursor.hasNext()) {
			Map rec = cursor.next().toMap();
			rec.remove("_id");
			Object[] record = (Object[]) rec.values().toArray(new Object[0]);
			recordList.add(record);
		}
		
		return recordList;
	}
	
	public static void main(String ...a) {
		
		MongoClient client = null;
		
		try {
			client = new MongoClient( "localhost" , 27017 );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		DB db = client.getDB( "kdd" );
		DBCollection table = db.getCollection("train_proportions");
		 
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("class", "e");
	 
		DBCursor cursor = table.find();
	 
		//System.out.println(cursor.length());
		while (cursor.hasNext()) {
			//DBObject obj = cursor.next();
			Map rec = cursor.next().toMap();
			rec.remove("_id");
			Object[] rec1 = (Object[]) rec.values().toArray(new Object[0]);
			System.out.println(rec1);
		}
	}
}

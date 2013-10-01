package com.stumbleupon.reader;

import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;

public class DBAccess {

	public static void main(String ...a) {
		
		Mongo mongoClient = null;
		try {
			mongoClient = new Mongo( "localhost" , 27017 );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DB db = mongoClient.getDB( "kdd" );
		DBCollection table = db.getCollection("train_proportions");
		 
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("class", "e");
	 
		DBCursor cursor = table.find(searchQuery);
	 
		while (cursor.hasNext()) {
			System.out.println(cursor.next());
		}
	}
}

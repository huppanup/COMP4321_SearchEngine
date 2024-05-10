package com.comp4321;

import jdbm.btree.BTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

@SpringBootTest
class Comp4321ApplicationTests {

	@BeforeAll
	public static void crawlPages() {
		try {
			Crawler crawler = new Crawler(
					"https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm",
					300,
					"stopwords.txt");
			crawler.crawl();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPageSize() {
		try {
			DocumentDB db = new DocumentDB();
			BTree table = db.getTable("IDToURL");
			Assertions.assertEquals(297, table.size());
			db.closeDB();
		} catch (Exception e) {
			e.printStackTrace ();
		}
	}

	@Test
	public void testMappingTables() {
		try {
			DocumentDB db = new DocumentDB();
			BTree IDToURL = db.getTable("IDToURL");
			BTree URLToID = db.getTable("URLToID");
			BTree IDToWord = db.getTable("IDToWord");
			BTree WordToID = db.getTable("WordToID");
			BTree ParentToChild = db.getTable("ParentToChild");
			BTree ChildToParent = db.getTable("ChildToParent");

			String URL = (String) IDToURL.find(2);
			int id = (int) URLToID.find("https://www.cse.ust.hk/~kwtleung/COMP4321/Movie.htm");
			String word = (String) IDToWord.find(2);
			int wordID = (int) WordToID.find("page");
			Vector<String> childs = (Vector<String>) ParentToChild.find(2);
			HashSet<Integer> parents = (HashSet<Integer>) ChildToParent.find("https://www.cse.ust.hk/~kwtleung/COMP4321/ust_cse/PG.htm");

			Assertions.assertEquals("https://www.cse.ust.hk/~kwtleung/COMP4321/news.htm", URL);
			Assertions.assertEquals(4, id);
			Assertions.assertEquals("crawler", word);
			Assertions.assertEquals(1, wordID);
			Assertions.assertTrue(childs.contains("https://www.cse.ust.hk/~kwtleung/COMP4321/news/cnn.htm"));
			Assertions.assertTrue(parents.contains(1));

			db.closeDB();
		} catch (Exception e) {
			e.printStackTrace ();
		}
	}

	@Test
	public void testIndexes() {
		try {
			DocumentDB db = new DocumentDB();
			BTree forwardIndexBody = db.getTable("forwardIndexBody");
			BTree forwardIndexTitle = db.getTable("forwardIndexTitle");
			BTree invertedIndexBody = db.getTable("invertedIndexBody");
			BTree invertedIndexTitle = db.getTable("invertedIndexTitle");

			HashMap<String, Integer> forwardBody = (HashMap<String, Integer>) forwardIndexBody.find(2);
			HashMap<String, Integer> forwardTitle = (HashMap<String, Integer>) forwardIndexTitle.find(2);
			HashMap<Integer, Vector<Integer>> invertedBody = (HashMap<Integer, Vector<Integer>>) invertedIndexBody.find("new");
			HashMap<Integer, Vector<Integer>> invertedTitle = (HashMap<Integer, Vector<Integer>>) invertedIndexTitle.find("new");

			Assertions.assertEquals((Integer) 3, forwardBody.get("new"));
			Assertions.assertEquals((Integer) 1, forwardTitle.get("new"));
			Assertions.assertTrue(invertedBody.containsKey(2));
			Assertions.assertTrue(invertedTitle.containsKey(2));
			Assertions.assertTrue(invertedBody.get(2).contains(0));
			Assertions.assertTrue(invertedTitle.get(2).contains(0));

			db.closeDB();
		} catch (Exception e) {
			e.printStackTrace ();
		}
	}

	@Test
	public void testScoring() {
		try {
			DocumentDB db = new DocumentDB();


			db.closeDB();
		} catch (Exception e) {
			e.printStackTrace ();
		}
	}

	@Test
	public void testPageInfo() {
		try {
			DocumentDB db = new DocumentDB();
			Vector<String> metaData = db.getIDMeta(2);

			Assertions.assertEquals("News", metaData.get(0));
			Assertions.assertEquals("2023-05-16 05:03:16 GMT", metaData.get(1));
			Assertions.assertEquals("384 B", metaData.get(2));

			db.closeDB();
		} catch (Exception e) {
			e.printStackTrace ();
		}
	}

}

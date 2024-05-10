package com.comp4321;

import java.io.IOException;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.htmlparser.util.ParserException;
import org.htmlparser.beans.LinkBean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;

public class Crawler
{
	private StopStem stopStem;
	private int maxIter;
	private Vector<String> urlQueue = new Vector<>();
	public DocumentDB db = new DocumentDB();

	public Scoring score = new Scoring();

	Crawler(String rootUrl, int maxIter, String stopWords) throws IOException {
		this.stopStem = new StopStem(stopWords);
		this.maxIter = maxIter;
		urlQueue.add(rootUrl);
	}
	public Vector<String> extractWords(String parentUrl) throws Exception
	{
		Document doc;
		String body;

		doc = Jsoup.connect(parentUrl).timeout(5000).get();
		Element bodyElement = doc.selectFirst("body");
		body = (bodyElement != null) ? bodyElement.text() : null;

		String[] tokens = body.split("[\\W]+");
		Vector<String> vec_tokens = new Vector<>();
		for (int i = 0; i < tokens.length; i++) {
			if (!tokens[i].isEmpty() && !tokens[i].isBlank()) vec_tokens.add(tokens[i].toLowerCase());
		}
		return vec_tokens;
	}
	public Vector<String> extractLinks(String parentUrl) throws ParserException
	{
		// extract links in url and return them
	    LinkBean lb = new LinkBean();
		lb.setURL(parentUrl);
		URL[] links = lb.getLinks();
		Vector<String> vec_links = new Vector<String>();
		for (int i = 0; i < links.length; i++) {
			vec_links.add(links[i].toString());
		}
		return vec_links;
	}

	public String extractPageTitle(String url) throws Exception{
		Document doc;
		String title;

		doc = Jsoup.connect(url).timeout(5000).get();
		Element titleElement = doc.selectFirst("title");
		title = (titleElement != null) ? titleElement.text() : null;

		return title;
	}

	public String extractLastModifiedDate(String urlString) throws Exception{
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();

		long lastModifiedMillis = connection.getLastModified();
		if (lastModifiedMillis == -1) { lastModifiedMillis = connection.getDate();}

		Date lastModifiedDate = new Date(lastModifiedMillis);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		return dateFormat.format(lastModifiedDate);
	}

	public String extractPageSize(String urlString) throws Exception{
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();

		int contentLength = connection.getContentLength();
		String formattedSize;
		if (contentLength == -1) {
			Document doc = Jsoup.connect(urlString).get();
			contentLength = doc.text().length();
			formattedSize = Integer.toString(contentLength) + " characters";
		} else {
			formattedSize = Integer.toString(contentLength) + " B";
		}
		return formattedSize;
	}

	public Vector<String> extractPageInfo(String url) throws Exception{
		Vector<String> pageInfo = new Vector<>();

		String title = extractPageTitle(url);
		String date = extractLastModifiedDate(url);
		String size = extractPageSize(url);

		pageInfo.add(title);
		pageInfo.add(date);
		pageInfo.add(size);

		return pageInfo;
	}

	private Boolean isUpdated (String url, String newDateString) throws Exception{
		// If URL already exists, check last modified date
		int id = db.getURLID(url);
		String savedDateString = db.getIDMeta(id).get(1);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date savedDate = dateFormat.parse(savedDateString);
		Date newDate = dateFormat.parse(newDateString);

		if (savedDate.before(newDate)) return true;
		return false;
	}

	public Vector<String> fetchWords(String url) throws Exception{
		Vector<String> words = extractWords(url);
		if (words != null) {
			Vector<String> stemWords = new Vector<>();
			for (int i = 0; i < words.size(); i++) {
				if (!stopStem.isStopWord(words.get(i))) {
					String word = stopStem.stem(words.get(i));
					if (!word.isBlank() && !word.isEmpty())
						stemWords.add(word);
				}
			}
			return stemWords;
		}
		return null;
	}

	public Vector<String> fetchTitleWords(int id) throws Exception{
		String title = db.getIDMeta(id).get(0);
		if (title != null) {
			String[] tokens = title.split("[ ,?]+");
			Vector<String> words = new Vector<>();
			for (int i = 0; i < tokens.length; i++) {
				words.add(tokens[i]);
			}

			Vector<String> stemWords = new Vector<>();
			for (int i = 0; i < words.size(); i++) {
				if (!stopStem.isStopWord(words.get(i))) {
					String word = stopStem.stem(words.get(i));
					if (!word.isBlank() && !word.isEmpty())
						stemWords.add(word);
				}
			}
			return stemWords;
		}
		return null;
	}

	public void fetchPage(String url) throws Exception{
		Vector<String> pageInfo = extractPageInfo(url);

		// If url is already in database and has not been modified, return
		if (db.addURL(url) == -1 && !isUpdated(url, pageInfo.get(1))) return;

		/* Add/Update database */
		// Add page details
		int id = db.getURLID(url);
		Vector<String> links = extractLinks(url);

		db.addMetadata(id, pageInfo);
		db.addChild(id, links);
		db.addParent(links, id);

		// Add keywords
		Vector<String> stemWords = fetchWords(url);
		if (stemWords != null) db.addWords(id, stemWords);
		Vector<String> titleStemWords = fetchTitleWords(id);
		if (titleStemWords != null) db.addTitleWords(id, titleStemWords);

		links.forEach(l -> { urlQueue.add(l);});

		this.maxIter--;
	}

	public void crawl() throws Exception {
		try {
			boolean updated = false;
			for (int i = 0; urlQueue.size() > i && maxIter != 0; i++) {
				this.fetchPage(this.urlQueue.get(i));
				if (i > 0) updated = true;
			}
			db.closeDB();
			if (updated) score.calculateWeights();
		} catch (Exception e) {
			db.closeDB();
			e.printStackTrace ();
		}
	}
}

	

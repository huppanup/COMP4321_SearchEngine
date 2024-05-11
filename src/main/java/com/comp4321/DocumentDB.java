package com.comp4321;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;

import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Vector;

public class DocumentDB {
    public RecordManager dbm;
    public DocumentDB(){
        try{
            this.dbm = RecordManagerFactory.createRecordManager("Database");
            String[] IDToObj = {
                    "IDToURL",
                    "IDToWord",
                    "IDToMetadata",
                    "ParentToChild",
                    "forwardIndexBody",
                    "forwardIndexTitle",
                    "termWeights",
                    "titleWeights"};
            String[] StringToObj = {
                    "URLToID",
                    "WordToID",
                    "ChildToParent",
                    "invertedIndexBody",
                    "invertedIndexTitle"};

            for (String name : IDToObj) {
                if (dbm.getNamedObject(name) != 0) continue;
                BTree bTree = BTree.createInstance(dbm, Comparator.naturalOrder());
                dbm.setNamedObject( name, bTree.getRecid() );
            }

            for (String name : StringToObj) {
                if (dbm.getNamedObject(name) != 0) continue;
                BTree bTree = BTree.createInstance(dbm, new StringComparator());
                dbm.setNamedObject( name, bTree.getRecid() );
            }
        }
        catch(Exception e) { e.printStackTrace();};
    }
    // Get data
    public RecordManager getDB(){
        return this.dbm;
    }
    public int getURLID(String url) throws Exception{
        return (int) BTree.load(dbm, dbm.getNamedObject("URLToID")).find(url);
    }

    public int getWordID(String word) throws Exception {
        if (BTree.load(dbm, dbm.getNamedObject("WordToID")).find(word) != null) return (int) BTree.load(dbm, dbm.getNamedObject("WordToID")).find(word);
        else return -1;
    }

    public TreeMap<String, TreeSet<String>> getIndexes() throws Exception {
        TreeMap<String, TreeSet<String>> wordDict = new TreeMap<>();
        BTree wordToID = BTree.load(dbm, dbm.getNamedObject("WordToID"));
        TupleBrowser browser = wordToID.browse();
        Tuple rec = new Tuple();
        while (browser.getNext(rec)) {
            String word = (String) rec.getKey();
            if (wordDict.get(word.substring(0,1)) == null) wordDict.put(word.substring(0,1), new TreeSet<>());
            wordDict.get(word.substring(0,1)).add(word);
        }
        return wordDict;
    }
    public BTree getTable(String tableName) throws Exception{
        return BTree.load(dbm, dbm.getNamedObject(tableName));
    }

    public Vector<String> getIDMeta(int id) throws Exception{
        // Metadata : [Page title, Last modification date, size of page]
        return (Vector<String>) BTree.load(dbm, dbm.getNamedObject("IDToMetadata")).find(id);
    }

    // Add entries
    public int addURL(String url) throws Exception{
            BTree IDToURL = BTree.load(dbm, dbm.getNamedObject("IDToURL"));
            BTree URLToID = BTree.load(dbm, dbm.getNamedObject("URLToID"));

            int id = IDToURL.size();
            Object result = URLToID.insert(url, id, false);
            // If URL is already in database, return -1
            if ( result != null) return -1;
            IDToURL.insert(id, url, false);
        return id;
    }

    public void addChild(int id, Vector<String> links) throws Exception {
        BTree ParentToChild = BTree.load(dbm, dbm.getNamedObject("ParentToChild"));
        ParentToChild.insert(id, links, true);
    }

    public void addParent(Vector<String> links, int id) throws Exception {
        BTree ChildToParent = BTree.load(dbm, dbm.getNamedObject("ChildToParent"));
        links.forEach(l -> {
            try {
                HashSet<Integer> parentList = (HashSet<Integer>) ChildToParent.find(l);
                if (parentList == null) parentList = new HashSet<>();
                parentList.add(id);
                ChildToParent.insert(l, parentList, true);
            } catch(Exception e){ e.printStackTrace(); }
        });
    }

    public void addMetadata(int id, Vector<String> meta) throws Exception{
        // Metadata : [Page title, Last modification date, size of page]
        BTree IDToMetadata = BTree.load(dbm, dbm.getNamedObject("IDToMetadata"));
        IDToMetadata.insert(id, meta, true);
    }

    public HashMap<String, Integer> calculateFrequency(Vector<String> stemWords){
        HashMap<String, Integer> wordFrequency = new HashMap<>();
        for (String word : stemWords) {
            if (wordFrequency.get(word) != null) {
                Integer freq = wordFrequency.get(word);
                freq++;
                wordFrequency.put(word, freq);
            } else {
                wordFrequency.put(word, 1);
            }
        }
        return wordFrequency;
    }
    public void addTitleWords(int id, Vector<String> stemWords) throws IOException {
        HashMap<String, Integer> frequency = calculateFrequency(stemWords);

        BTree forwardIndex = BTree.load(dbm, dbm.getNamedObject("forwardIndexTitle"));
        forwardIndex.insert(id, frequency, true);

        BTree invertedIndex = BTree.load(dbm, dbm.getNamedObject("invertedIndexTitle"));
        BTree WordToID = BTree.load(dbm, dbm.getNamedObject("WordToID"));
        BTree IDToWord = BTree.load(dbm, dbm.getNamedObject("IDToWord"));

        for (int i = 0; i < stemWords.size(); i++){
            // Word Table
            int wordId = IDToWord.size();
            Object result = WordToID.insert(stemWords.get(i), wordId, false);
            if (result == null) IDToWord.insert(wordId, stemWords.get(i), false);

            // Word Indexing
            HashMap<Integer, Vector<Integer>> idList = (HashMap<Integer, Vector<Integer>>) invertedIndex.find(stemWords.get(i));
            if (idList == null) idList = new HashMap<>();
            if (idList.get(id) == null) idList.put(id, new Vector<>());
            idList.get(id).add(i);
            invertedIndex.insert(stemWords.get(i), idList, true);
        }
    }

    public void addWords(int id, Vector<String> stemWords) throws IOException {
        HashMap<String, Integer> frequency = calculateFrequency(stemWords);

        BTree forwardIndex = BTree.load(dbm, dbm.getNamedObject("forwardIndexBody"));
        forwardIndex.insert(id, frequency, true);

        BTree WordToID = BTree.load(dbm, dbm.getNamedObject("WordToID"));
        BTree IDToWord = BTree.load(dbm, dbm.getNamedObject("IDToWord"));

        BTree invertedIndex = BTree.load(dbm, dbm.getNamedObject("invertedIndexBody"));

        for (int i = 0; i < stemWords.size(); i++){
            // Word Table
            int wordId = IDToWord.size();
            Object result = WordToID.insert(stemWords.get(i), wordId, false);
            if (result == null) IDToWord.insert(wordId, stemWords.get(i), false);

            // Word Indexing
            HashMap<Integer, Vector<Integer>> idList = (HashMap<Integer, Vector<Integer>>) invertedIndex.find(stemWords.get(i));
            if (idList == null) idList = new HashMap<>();
            if (idList.get(id) == null) idList.put(id, new Vector<>());
            idList.get(id).add(i);
            invertedIndex.insert(stemWords.get(i), idList, true);
        }
    }

    public void closeDB() throws Exception{
        dbm.commit();
        dbm.close();
    }
}

package com.comp4321;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.util.*;

public class SearchResult {
    public static HashMap<Integer, HashMap<String, String>> getResults(LinkedHashMap<Integer,Double> score) throws Exception {

        DocumentDB documentDB = new DocumentDB();
        RecordManager dbm = RecordManagerFactory.createRecordManager("Database");

        BTree IDToURL = BTree.load(dbm, dbm.getNamedObject("IDToURL"));
        BTree forwardIndex = BTree.load(dbm, dbm.getNamedObject("forwardIndexBody"));
        BTree ChildToParent = BTree.load(dbm, dbm.getNamedObject("ChildToParent"));
        BTree ParentToChild = BTree.load(dbm, dbm.getNamedObject("ParentToChild"));

        // { rank : {score, title, url, mod_date, size, keywords, parent, child}}
        HashMap<Integer, HashMap<String, String>> result = new HashMap<>();
        int rank = 0;
        for (Map.Entry<Integer, Double> entry : score.entrySet()) {

            int id = entry.getKey();
            HashMap<String, String> pageInfo = new HashMap<>();
            // Get score
            pageInfo.put("score", String.format("%.2e", entry.getValue()));

            // Get URL
            pageInfo.put("url", (String) IDToURL.find(id));

            // Get MetaData
            Vector<String> metaData = documentDB.getIDMeta(id);
            if (metaData == null) {
                result.put(-1, null);
                return result;
            }
            pageInfo.put("title", metaData.get(0));
            pageInfo.put("mod_date", metaData.get(1));
            pageInfo.put("size", metaData.get(2));

            // Get Keywords
            HashMap<String, Integer> wordFreqMap = (HashMap<String, Integer>) forwardIndex.find(id);
            List<Map.Entry<String, Integer>> wordList = new ArrayList<>(wordFreqMap.entrySet());
            Collections.sort(wordList, (entry1, entry2) -> {
                return entry2.getValue().compareTo(entry1.getValue()); // Compare values in descending order
            });
            String keywords = "";
            int count = 0;
            for (Map.Entry<String, Integer> wordFreqEntry : wordList) {
                if (count >= 5) { break; }
                keywords += wordFreqEntry.getKey() + " " + wordFreqEntry.getValue() + ", ";
                count++;
            }
            if (keywords.length() > 2) { keywords = keywords.substring(0, keywords.length() - 2);}

            pageInfo.put("keywords", keywords);

            // Get Parent URL
            String parentURLString = "";
            HashSet<Integer> parentID = (HashSet<Integer>) ChildToParent.find(pageInfo.get("url"));
            for(Integer parent : parentID){
                String parentURL = (String) IDToURL.find(parent);
                parentURLString += parentURL + ", ";
            }
            if (parentURLString.length() > 2) {
                parentURLString = parentURLString.substring(0, parentURLString.length() - 2);
            }
            pageInfo.put("parent", parentURLString);

            // Get Child URL
            String childURLString = "";
            Vector<String> childLinks = (Vector<String>) ParentToChild.find(id);
            for(String child: childLinks){
                childURLString += child + ", ";
            }
            if (childURLString.length() > 2) {
                childURLString = childURLString.substring(0, childURLString.length() - 2);
            }
            pageInfo.put("child", childURLString);
            result.put(rank, pageInfo);
            rank++;
        }
        documentDB.closeDB();
        return result;
    }
}

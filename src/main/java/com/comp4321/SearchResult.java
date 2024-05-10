package com.comp4321;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.util.*;

public class SearchResult {
    public static HashMap<String, String> getResults(int num) throws Exception {


        HashMap<String, String> result = new HashMap<>();
        DocumentDB documentDB = new DocumentDB();

        RecordManager dbm = RecordManagerFactory.createRecordManager("Database");

        BTree IDToURL = BTree.load(dbm, dbm.getNamedObject("IDToURL"));
        BTree forwardIndex = BTree.load(dbm, dbm.getNamedObject("forwardIndexBody"));
        BTree ParentToChild = BTree.load(dbm, dbm.getNamedObject("ParentToChild"));

        BTree pageRank = BTree.load(dbm, dbm.getNamedObject("pageRank"));
        TupleBrowser browser = pageRank.browse();
        Tuple rec = new Tuple();

        for (int i = 0; i < num; i++) {
            Vector<String> metaData = documentDB.getIDMeta(i);
            if (metaData == null) {
                result.put("success", "false");
                return result;
            }

            result.put("page_title", metaData.get(0));
            result.put("mod_date", metaData.get(1));
            result.put("page_size", metaData.get(2));

            String keywords = "";
            HashMap<String, Integer> temp = (HashMap<String, Integer>) forwardIndex.find(i);
            List<Map.Entry<String, Integer>> wordList = new ArrayList<>(temp.entrySet());
            Collections.sort(wordList, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> entry1, Map.Entry<String, Integer> entry2) {
                    return entry2.getValue().compareTo(entry1.getValue()); // Compare values in descending order
                }
            });

            int count = 0;
            for (Map.Entry<String, Integer> entry : wordList) {
                if (count >= 5) {
                    break;
                }
                keywords += entry.getKey() + " " + String.valueOf(entry.getValue()) + ", ";
                count++;
            }

            if (keywords.length() > 2) {
                keywords = keywords.substring(0, keywords.length() - 2);
            }

            result.put("keywords", keywords);

//            Vector<String> childLinks = (Vector<String>) ParentToChild.find(i);
//            for(int j = 0; j < Math.min(childLinks.size(), 10); j++){
//                writer.write(childLinks.get(j) + "\n");
//            }
        }
        documentDB.closeDB();
        return result;
    }
}

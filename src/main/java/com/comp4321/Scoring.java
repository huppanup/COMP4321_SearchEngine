package com.comp4321;

import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.io.IOException;
import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Scoring {
    public int getMaxFrequency(BTree forwardIndex) throws Exception {
        int maxFrequency = Integer.MIN_VALUE;
        TupleBrowser browser = forwardIndex.browse();
        Tuple rec = new Tuple();
        while (browser.getNext(rec)) {
            HashMap<String, Integer> wordLists = (HashMap<String, Integer>) rec.getValue();
            for (Map.Entry<String, Integer> entry : wordLists.entrySet()) {
                int frequency = entry.getValue();
                if (frequency > maxFrequency) maxFrequency = frequency;
            }
        }
        return maxFrequency;
    }

    public void storeWeights(DocumentDB db, BTree forwardIndex, BTree invertedIndex, BTree weights) throws Exception {
        int size = forwardIndex.size();
        int maxFrequency = getMaxFrequency(forwardIndex);

        TupleBrowser browser = forwardIndex.browse();
        Tuple rec = new Tuple();
        while (browser.getNext(rec)) {
            HashMap<String, Integer> wordLists = (HashMap<String, Integer>) rec.getValue();
            HashMap<Integer, Double> listWeights = new HashMap<>();
            for (Map.Entry<String, Integer> entry : wordLists.entrySet()) {
                String word = entry.getKey();
                Integer frequency = entry.getValue();
                HashMap<Integer, Vector<Integer>> otherPages = (HashMap<Integer, Vector<Integer>>) invertedIndex.find(word);
                int numPages = otherPages.size();
                double result = (double) frequency / (double) maxFrequency * (Math.log((double) size / (double) numPages) / Math.log(2));
                listWeights.put(db.getWordID(word), result);
            }
            weights.insert(rec.getKey(), listWeights, true);
        }
    }

    public void calculateWeights() throws Exception {
        DocumentDB db = new DocumentDB();

        BTree forwardIndexBody = db.getTable("forwardIndexBody");
        BTree invertedIndexBody = db.getTable("invertedIndexBody");
        BTree termWeights = db.getTable("termWeights");
        BTree forwardIndexTitle = db.getTable("forwardIndexTitle");
        BTree invertedIndexTitle = db.getTable("invertedIndexTitle");
        BTree titleWeights = db.getTable("titleWeights");

        storeWeights(db, forwardIndexBody, invertedIndexBody, termWeights);
        storeWeights(db, forwardIndexTitle, invertedIndexTitle, titleWeights);

        db.closeDB();
    }

    private double cosineSimiliarity(double documentLength, double queryLength, double weight) {
        return weight / (documentLength * queryLength);
    }

    private boolean isPhrase(int pageID, BTree invertedIndex, Vector<String> phrase) throws IOException {
        LinkedHashMap<Integer, Vector<Integer>> phrasePositions = new LinkedHashMap<>();
        for (int i = 0; i < phrase.size(); i++) {
            HashMap<Integer, Vector<Integer>> idList = (HashMap<Integer, Vector<Integer>>) invertedIndex.find(phrase.get(i));
            Vector<Integer> positionList = idList.get(pageID);
            phrasePositions.put(i, positionList);
        }

        for (int i = 0; i < phrasePositions.get(0).size(); i++) {
            boolean isConsecutive = true;
            int currentPos = phrasePositions.get(0).get(i);
            for (int j = 1; j < phrase.size(); j++) {
                if (!phrasePositions.get(j).contains(currentPos + i)) {
                    isConsecutive = false;
                    break;
                }
            }
            if (isConsecutive) return true;
        }
        return false;
    }

    private double getDocumentLength(int pageID, HashMap<String, Integer> frequency) {
        double length = 0.0;
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            length += (double) pow(entry.getValue(), 2);
        }
        return sqrt(length);
    }

    /* Phrase is a must + word is preferable
        1. Filter out documents that do not contain phrase
        2. Give more points if the document contains words
     */
    private LinkedHashMap<Integer, Double> calculateSimilarity(DocumentDB db, HashMap<Integer, Vector<String>> query, BTree weights, BTree invertedIndex, BTree forwardIndex) throws Exception {
        LinkedHashMap<Integer, Double> score = new LinkedHashMap<>();

        Vector<String> phrase = query.get(0);
        Vector<String> words = query.get(1);
        if (phrase == null && words == null) return null;

        int querySize = words.size();
        double queryLength = sqrt(querySize);

        Vector<Integer> phraseIDs = new Vector<>();
        Vector<Integer> wordIDs = new Vector<>();
        if (phrase != null) {
            for (int i = 0; i < phrase.size(); i++) {
                int phraseID = db.getWordID(phrase.get(i));
                if (phraseID == -1) return null;
                phraseIDs.add(phraseID);
            }
        }

        if (words != null) {
            for (int i = 0; i < words.size(); i++) {
                int wordID = db.getWordID(words.get(i));
                if (wordID != -1) wordIDs.add(wordID);
            }
        }

        TupleBrowser browser = weights.browse();
        Tuple rec = new Tuple();
        while (browser.getNext(rec)) {
            HashMap<Integer, Double> wordWeights = (HashMap<Integer, Double>) rec.getValue();
            if (phraseIDs != null && !phraseIDs.isEmpty()) {
                boolean allContained = phraseIDs.stream().allMatch(wordWeights::containsKey);
                if (allContained && isPhrase((Integer) rec.getKey(), invertedIndex, phrase)) {
                    score.put((Integer) rec.getKey(), 0.0);
                    HashMap<String, Integer> frequency = (HashMap<String, Integer>) forwardIndex.find(rec.getKey());
                    double documentLength = getDocumentLength((Integer) rec.getKey(), frequency);
                    if (wordIDs != null && !wordIDs.isEmpty()) {
                        for (int i = 0; i < wordIDs.size(); i++) {
                            if (wordWeights.containsKey(wordIDs.get(i))) {
                                double result = cosineSimiliarity(documentLength, queryLength, wordWeights.get(wordIDs.get(i)));
                                if (score.get(rec.getKey()) != null) score.put((Integer) rec.getKey(), score.get(rec.getKey()) + result);
                                else score.put((Integer) rec.getKey(), result);
                            }
                        }
                    }
                }
            } else {
                HashMap<String, Integer> frequency = (HashMap<String, Integer>) forwardIndex.find(rec.getKey());
                double documentLength = getDocumentLength((Integer) rec.getKey(), frequency);
                if (wordIDs != null && !wordIDs.isEmpty()) {
                    for (int i = 0; i < wordIDs.size(); i++) {
                        if (wordWeights.containsKey(wordIDs.get(i))) {
                            double result = cosineSimiliarity(documentLength, queryLength, wordWeights.get(wordIDs.get(i)));
                            if (score.get(rec.getKey()) != null) score.put((Integer) rec.getKey(), score.get(rec.getKey()) + result);
                            else score.put((Integer) rec.getKey(), result);
                        }
                    }
                }
            }
        }
        return score;
    }

    /*
        1. parse query into phrase + word
        2. Stem if query is stopword
    */
    private HashMap<Integer, Vector<String>> parseQuery(String query) {
        HashMap<Integer, Vector<String>> parsedQuery = new HashMap<>();
        StopStem stopStem = new StopStem("stopwords.txt");

        int startIndex = query.indexOf('"'); // Find the first quotation mark
        int endIndex = query.indexOf('"', startIndex + 1); // Find the closing quotation mark

        if (startIndex != -1 && endIndex != -1) {
            String phrase = query.substring(startIndex + 1, endIndex);
            String[] tokens = phrase.split(" ");
            Vector<String> parsedPhrase = new Vector<>();
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (!token.isBlank() && !token.isEmpty() && !stopStem.isStopWord(token)) {
                    String stemPhrase = stopStem.stem(token);
                    parsedPhrase.add(stemPhrase);
                }
            }
            parsedQuery.put(0, parsedPhrase);

            String newQuery = query.replace("\"" + phrase + "\"", "");
            String[] wordTokens = newQuery.split(" ");
            Vector<String> parsedWords = new Vector<>();
            for (int i = 0; i < wordTokens.length; i++) {
                String token = wordTokens[i].trim();
                if (!token.isBlank() && !token.isEmpty() && !stopStem.isStopWord(token)) {
                    String stemWord = stopStem.stem(token);
                    parsedWords.add(stemWord);
                }
            }
            parsedQuery.put(1, parsedWords);
        } else {
            String[] tokens = query.split(" ");
            Vector<String> parsedWords = new Vector<>();
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (!token.isBlank() && !token.isEmpty() && !stopStem.isStopWord(token)) {
                    String stemWord = stopStem.stem(token);
                    parsedWords.add(stemWord);
                }
            }
            parsedQuery.put(1, parsedWords);
        }
        return parsedQuery;
    }

    public void score(String query) throws Exception {
        if (query.isEmpty() || query.isBlank()) return;

        DocumentDB db = new DocumentDB();

        BTree pageRank = db.getTable("pageRank");
        BTree termWeights = db.getTable("termWeights");
        BTree titleWeights = db.getTable("titleWeights");
        BTree invertedIndexBody = db.getTable("invertedIndexBody");
        BTree invertedIndexTitle = db.getTable("invertedIndexTitle");
        BTree forwardIndexBody = db.getTable("forwardIndexBody");
        BTree forwardIndexTitle = db.getTable("forwardIndexTitle");

        HashMap<Integer, Vector<String>> parsedQuery = parseQuery(query);

        LinkedHashMap<Integer, Double> bodyScore = calculateSimilarity(db, parsedQuery, termWeights, invertedIndexBody, forwardIndexBody);
        LinkedHashMap<Integer, Double> titleScore = calculateSimilarity(db, parsedQuery, titleWeights, invertedIndexTitle, forwardIndexTitle);
        LinkedHashMap<Integer, Double> totalScore = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : bodyScore.entrySet()) {
            totalScore.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, Double> entry : titleScore.entrySet()) {
            if (totalScore.get(entry.getKey()) == null) totalScore.put(entry.getKey(), 1.5 * entry.getValue());
            else totalScore.put(entry.getKey(), (double) totalScore.get(entry.getKey()) + 1.5 * entry.getValue());
        }

        List<Map.Entry<Integer, Double>> entryList = new ArrayList<>(totalScore.entrySet());
        entryList.sort(Comparator.comparing(Map.Entry::getValue));
        Collections.reverse(entryList);

        LinkedHashMap<Integer, Double> sortedScore = new LinkedHashMap<>();
        entryList.forEach(entry -> sortedScore.put(entry.getKey(), entry.getValue()));

        int rank = 0;
        for (Map.Entry<Integer, Double> entry : sortedScore.entrySet()) {
            HashMap<Integer, Double> result = new HashMap<>();
            result.put(entry.getKey(), entry.getValue());
            if (pageRank.size() < 50) {
                pageRank.insert(rank, result, true);
            }
            rank++;
        }

        db.closeDB();
    }
}

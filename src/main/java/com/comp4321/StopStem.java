package com.comp4321;

import com.comp4321.IRUtilities.Porter;

import java.io.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

public class StopStem
{
    private Porter porter;
    private HashSet<String> stopWords;
    public boolean isStopWord(String str)
    {
        return stopWords.contains(str);
    }
    public StopStem(String str)
    {
        super();
        porter = new Porter();
        stopWords = new HashSet<String>();

        // use BufferedReader to extract the stopwords in stopwords.txt (path passed as parameter str)
        // add them to HashSet<String> stopWords
        BufferedReader br = null;
        try {
            FileReader fr = new FileReader(str);
            br = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                stopWords.add(line);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }
    public String stem(String str)
    {
        return porter.stripAffixes(str);
    }
}

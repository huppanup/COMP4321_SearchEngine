import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

public class Main {

    public static void exportDB(int num) throws Exception {
        DocumentDB documentDB = new DocumentDB();
        RecordManager dbm = RecordManagerFactory.createRecordManager("Database");
        BufferedWriter writer = new BufferedWriter(new FileWriter("spider_result.txt"));

        BTree IDToURL = BTree.load(dbm, dbm.getNamedObject("IDToURL"));
        BTree forwardIndex = BTree.load(dbm, dbm.getNamedObject("forwardIndexBody"));
        BTree ParentToChild = BTree.load(dbm, dbm.getNamedObject("ParentToChild"));

        for (int i = 0; i < num; i++) {
            Vector<String> metaData = documentDB.getIDMeta(i);
            if (metaData == null) return;
            writer.write(metaData.get(0) + "\n");
            writer.write(IDToURL.find(i) + "\n");
            writer.write(metaData.get(1) + ", " + metaData.get(2) + "\n");
            HashMap<String, Integer> wordList = (HashMap<String, Integer>) forwardIndex.find(i);
            int iter = 0;
            for (Map.Entry<String, Integer> entry : wordList.entrySet()){
                if (iter > 9) break;
                writer.write(entry.getKey() + " " + entry.getValue() + "; ");
                iter++;
            }
            writer.write("\n");
            Vector<String> childLinks = (Vector<String>) ParentToChild.find(i);
            for(int j = 0; j < Math.min(childLinks.size(), 10); j++){
                writer.write(childLinks.get(j) + "\n");
            }
            writer.write("----------------------------------------------------------" + "\n");
        }
        writer.close();
        documentDB.closeDB();
    }
    public static void main (String[] args)
    {
        // Try out crawler in TestCrawler
        try {
            Crawler crawler = new Crawler(
                    "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm",
                    300,
                    "stopwords.txt");
            crawler.crawl();

            Scoring score = new Scoring();
            Scanner scanner = new Scanner(System.in);

            System.out.print("Write query: ");
            String query = scanner.nextLine();
            score.score(query);

            exportDB(30);
        } catch (Exception e) {
            e.printStackTrace ();
        }

    }
}

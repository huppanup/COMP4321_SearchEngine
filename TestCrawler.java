import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import org.junit.Test;
import org.junit.Assert;
public class TestCrawler {
    @Test
    public void testPageInfo() {
        try {
            Crawler crawler = new Crawler(
                    "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm",
                    10,
                    "stopwords.txt");
            crawler.crawl();
            RecordManager rm = crawler.db.dbm;
            String[] tableList = {
                    "invertedIndexBody"};

            for (String tableName : tableList) {
                System.out.println("Table : " + tableName);
                System.out.println();
                BTree table = BTree.load(rm, rm.getNamedObject(tableName));
                TupleBrowser browser = table.browse();
                if(!tableName.equals("ChildToParent")){Assert.assertEquals(10, table.size());}
                Tuple rec = new Tuple();
                while (browser.getNext(rec)) {
                    System.out.println("Key: " + rec.getKey() + ", Value: " + rec.getValue());
                }
                System.out.println();
                System.out.println("------------------");
            }
        } catch (Exception e) {
            e.printStackTrace ();
        }

    }

    @Test
    public void testIsUpdated() {

        Assert.assertEquals(5.0, 5.0);
    }

    @Test
    public void testStaticDB() {
        DocumentDB db1 = new DocumentDB();
        DocumentDB db2 = new DocumentDB();
        try{
            db1.addURL("First URL");
            db1.addURL("Second URL");
            db1.addURL("Third URL");

            db2.addURL("Test to see if instance is shared");
            System.out.println(db1.dbm.getNamedObject("IDtoURL"));
            System.out.println(db2.dbm.getNamedObject("IDtoURL"));
            TupleBrowser browser = BTree.load(db1.dbm, db1.dbm.getNamedObject("IDtoURL")).browse();
            Tuple rec = new Tuple();
            while (browser.getNext(rec)) {
                System.out.println("Key: " + rec.getKey() + ", Value: " + rec.getValue());
            }

            browser = BTree.load(db2.dbm, db2.dbm.getNamedObject("IDtoURL")).browse();
            while (browser.getNext(rec)) {
                System.out.println("Key: " + rec.getKey() + ", Value: " + rec.getValue());
            }

            db1.closeDB();
            db2.closeDB();
        }catch(Exception e){e.printStackTrace();}

        Assert.assertEquals(5.0, 5.0);
    }

    @Test
    public void testStaticDB2() {
        DocumentDB db1 = new DocumentDB();
        try{
            db1.addURL("REECHECK");
            System.out.println(db1.dbm.getNamedObject("IDtoURL"));
            TupleBrowser browser = BTree.load(db1.dbm, db1.dbm.getNamedObject("IDtoURL")).browse();
            Tuple rec = new Tuple();
            while (browser.getNext(rec)) {
                System.out.println("Key: " + rec.getKey() + ", Value: " + rec.getValue());
            }
            db1.closeDB();
        }catch(Exception e){e.printStackTrace();}

        Assert.assertEquals(5.0, 5.0);
    }
}

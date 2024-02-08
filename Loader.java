import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

public class Loader implements Serializable{

    //size of block to be written to disk
    private final int blockSize;

    //to store SiteRecord objects before writing them to disk
    private final List<SiteRecord> blockBuffer;

    //to keep track of the blocks written to disk
    private int blockIndex;

    static Map<String, Map<String, Integer>> wordFrequencyTable = new HashMap<>();

    private static final Set<String> stopWords = createStopWordsSet();

    public Loader(int blockSize) {
        this.blockSize = blockSize;
        this.blockBuffer = new ArrayList<>();
        this.blockIndex = 0;
    }

    public void addSiteRecord(SiteRecord record, PersistentHashTable hashTable) {
        blockBuffer.add(record);
        //When buffer is full, writes the block to file
        if (blockBuffer.size() >= blockSize) {
            writeBlockToFile();
        }
        hashTable.put(record.getUrl(), blockIndex, blockBuffer.size() - 1);
    }

    public static void main(String[] args) throws Exception {

        //Save data
        Loader loader = new Loader( 202);
        PersistentHashTable hashTable = new PersistentHashTable();
        if (hashTable == null) {
            hashTable = new PersistentHashTable();
        }
        List<SiteRecord> allSiteRecords = new ArrayList<>();

        List<String> urls = loadUrlsFromFile("/Users/Danmas/Desktop/Wiki.txt");

        for (String url : urls) {
            HashMap<String, Integer> wordFrequency = analyzeWebsite(url);
            SiteRecord siteRecord = new SiteRecord(url, wordFrequency);
            allSiteRecords.add(siteRecord);
            loader.addSiteRecord(siteRecord, hashTable);
        }

        // Flush any remaining records to disk
        loader.flush();

        // Save PersistentHashTable to disk
        hashTable.saveToFile("persistent_hashtable.dat");

    }

    //Read URLs from text file and returns them in list
    public static List<String> loadUrlsFromFile(String filePath) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            return stream.collect(Collectors.toList());
        }}

    public static HashMap<String, Integer> analyzeWebsite(String url) throws Exception {
        Document document = Jsoup.connect(url).get();

        Elements pTags = document.select("p");

        // Update the frequency table for the selected website
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (Element pTag : pTags) {
            String text = pTag.text().toLowerCase();
            // Add spaces between words
            String[] words = text.split("\\s+");

            for (String word : words) {
                if (!stopWords.contains(word) && word.matches("^[a-zA-Z]*$")) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Store word frequency in a persistent data structure
        wordFrequencyTable.put(url, wordFrequency);
        return (HashMap<String, Integer>) wordFrequency;
    }
    private static Set<String> createStopWordsSet() {
        Set<String> stopWords = new HashSet<>();
        // Adding prepositions and article words to this set
        stopWords.add("a");
        stopWords.add("the");
        stopWords.add("in");
        stopWords.add("at");
        stopWords.add("on");
        stopWords.add("of");
        stopWords.add("and");
        stopWords.add("to");
        stopWords.add("by");
        stopWords.add("have");
        stopWords.add("it");
        stopWords.add("for");
        stopWords.add("as");
        stopWords.add("or");
        stopWords.add("there");
        stopWords.add("what");
        stopWords.add("can");
        stopWords.add("use");
        stopWords.add("because");
        stopWords.add("most");
        stopWords.add("more");
        stopWords.add("be");
        stopWords.add("with");
        stopWords.add("may");
        stopWords.add("these");
        stopWords.add("is");
        return stopWords;
    }

    private void writeBlockToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream("block_" + blockIndex + ".dat"))) {
            out.writeObject(blockBuffer);
            blockIndex++;
            blockBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Flush any remaining SiteRecords to disk
    public void flush() {
        if (!blockBuffer.isEmpty()) {
            writeBlockToFile();
            blockBuffer.clear();  // Clear the blockBuffer after writing it to file
        }
    }
}

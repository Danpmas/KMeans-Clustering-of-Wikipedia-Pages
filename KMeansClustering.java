import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.io.Serializable;
import java.util.*;

public class KMeansClustering implements Serializable {

    private PersistentHashTable hashTable;
    private final int k;
    private final List<SiteRecord> records;
    private final List<SiteRecord> centroids;

    //map to associate centroid with its clusters.
    private Map<SiteRecord, List<SiteRecord>> clusters;

    public KMeansClustering(int k, PersistentHashTable hashTable) {
        this.k = k;
        this.hashTable = hashTable;
        this.records = loadRecordsFromHashTable();
        this.centroids = initializeCentroids();
    }
    private List<SiteRecord> loadRecordsFromHashTable() {
        List<SiteRecord> records = new ArrayList<>();
        for (String url : hashTable.getUrls()) {
            SiteRecord record = hashTable.getRecord(url);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }
    private List<SiteRecord> initializeCentroids() {
        // Random k records as initial centroids
        Collections.shuffle(records);
        return records.subList(0, k);
    }

    public void run() {
        boolean converged = false;
        int iterations = 0;
        //if centroids aren't moving anymore
        final double threshold = 0.0001;

        while (!converged) {
            iterations++;
            //assign records to nearest centroid
            Map<SiteRecord, List<SiteRecord>> newClusters = assignToClusters();

            //assume conv.
            converged = true;
            //update centroids and check for convergence
            for (SiteRecord centroid : new ArrayList<>(clusters.keySet())) {
                SiteRecord newCentroid = computeNewCentroid(clusters.get(centroid));
                if (!newCentroid.equals(centroid)) {
                    double similarity = calculateSimilarity(newCentroid.getWordFrequency(), centroid.getWordFrequency());
                    if (similarity < 1 - threshold) {
                        converged = false;
                        centroids.remove(centroid);
                        centroids.add(newCentroid);
                    }
                }
            }

            // Update the clusters only if centroids have changed
            if (!converged) {
                clusters = newClusters;
            }

            // loop check
            if (iterations >= 101) {
                break;
            }
        }

        // Print the result after convergence
        printClusters();
    }

    private void printClusters() {
        // Print URLs of SiteRecords in each cluster
        int clusterIndex = 0;
        for (SiteRecord centroid : clusters.keySet()) {
            System.out.println("Cluster " + (++clusterIndex) + " with Centroid: " + centroid.getUrl());
            List<SiteRecord> clusterSites = clusters.get(centroid);
            for (SiteRecord site : clusterSites) {
                System.out.println("  - " + site.getUrl());
            }
        }
    }


    private SiteRecord computeNewCentroid(List<SiteRecord> cluster) {

        if (cluster.isEmpty()) {
            throw new IllegalArgumentException("Cluster cannot be empty when computing a new centroid.");
        }

        // Retrieve URL of first SiteRecord in the cluster
        String firstRecordUrl = cluster.get(0).getUrl();

        // Hold combined word frequencies of all SiteRecord objects in the cluster
        HashMap<String, Integer> newWordFrequencies = new HashMap<>();

        // Loop through SiteRecords in the cluster and combine word frequencies into newWordFrequencies
        for (SiteRecord record : cluster) {
            Map<String, Integer> freqs = record.getWordFrequency();
            for (Map.Entry<String, Integer> entry : freqs.entrySet()) {
                newWordFrequencies.put(entry.getKey(), newWordFrequencies.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }

        // Average the word frequencies
        for (Map.Entry<String, Integer> entry : newWordFrequencies.entrySet()) {
            newWordFrequencies.put(entry.getKey(), entry.getValue() / cluster.size());
        }

        // Return new SiteRecord with "centroid" & URL of first record
        return new SiteRecord(firstRecordUrl, newWordFrequencies);
    }

    private double calculateSimilarity(Map<String, Integer> frequency1, Map<String, Integer> frequency2) {
        Set<String> word_Set1 = frequency1.keySet();
        Set<String> word_Set2 = frequency2.keySet();
        Set<Object> commonWords = new HashSet<>(word_Set1);
        //retain all words also found in set 2
        commonWords.retainAll(word_Set2);

        int totalWords1 = word_Set1.stream().mapToInt(frequency1::get).sum();
        int totalWords2 = word_Set2.stream().mapToInt(frequency2::get).sum();
        //For each word in commonWords set, calculates min frequency of that word between frequency1 and frequency2
        //Then calculates sum of all these minimum frequencies
        int totalCommonWords = commonWords.stream().mapToInt(word -> Math.min(frequency1.get((String) word), frequency2.get((String) word))).sum();
        return (double) totalCommonWords / Math.min(totalWords1, totalWords2);
    }

    private Map<SiteRecord, List<SiteRecord>> assignToClusters() {
        Map<SiteRecord, List<SiteRecord>> clusters = new HashMap<>();
        for (SiteRecord centroid : centroids) {
            clusters.put(centroid, new ArrayList<>());
        }
        for (SiteRecord record : records) {
            SiteRecord mostSimilarCentroid = findMostSimilarCentroid(record);
            clusters.get(mostSimilarCentroid).add(record);
        }
        this.clusters = clusters;
        return clusters;
    }

    private SiteRecord findMostSimilarCentroid(SiteRecord record) {
        double maxSimilarity = 0.0;
        SiteRecord mostSimilarCentroid = null;

        for (SiteRecord centroid : centroids) {
            double similarity = calculateSimilarity(record.getWordFrequency(), centroid.getWordFrequency());
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilarCentroid = centroid;
            }
        }

        return mostSimilarCentroid;
    }

    public Map<SiteRecord, SiteRecord> getMostSimilarSitesInClusters() {
        Map<SiteRecord, SiteRecord> mostSimilarSites = new HashMap<>();
        for (SiteRecord centroid : clusters.keySet()) {
            List<SiteRecord> clusterSites = new ArrayList<>(clusters.get(centroid));

            // Remove the centroid itself from the list in similarity calc
            clusterSites.remove(centroid);

            double maxSimilarity = 0.0;
            SiteRecord mostSimilarSite = null;
            for (SiteRecord site : clusterSites) {
                double similarity = calculateSimilarity(site.getWordFrequency(), centroid.getWordFrequency());
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    mostSimilarSite = site;
                }
            }
            mostSimilarSites.put(centroid, mostSimilarSite);
        }
        return mostSimilarSites;
    }

    public void plotClusters() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<SiteRecord, SiteRecord> mostSimilarSites = getMostSimilarSitesInClusters();
        for (SiteRecord centroid : mostSimilarSites.keySet()) {
            SiteRecord mostSimilarSite = mostSimilarSites.get(centroid);
            dataset.addValue(clusters.get(centroid).size(), "Cluster Size", centroid.getUrl() + "\nMost Similar Site: " + mostSimilarSite.getUrl());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Clusters",
                "Centroid in Cluster",
                "Number of Sites",
                dataset
        );

        CategoryPlot plot = barChart.getCategoryPlot();
        CategoryAxis xAxis = plot.getDomainAxis();

        //increased space for labels
        xAxis.setLowerMargin(0.02); // You can adjust this value as needed
        xAxis.setUpperMargin(0.02);
        xAxis.setMaximumCategoryLabelLines(3); // Allow labels to span multiple lines
        
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ChartPanel(barChart));
        frame.setSize(1400, 800); // Adjust size as needed
        frame.setVisible(true);
    }

}
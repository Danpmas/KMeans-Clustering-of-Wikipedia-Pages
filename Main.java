import java.util.Map;

public class Main {

public static void main(String[] args) throws Exception {

        //KMeansClustering
        PersistentHashTable hashTable = PersistentHashTable.loadFromFile("persistent_hashtable.dat");
                KMeansClustering kMeans = new KMeansClustering(5, hashTable);
                kMeans.run();

                // Print most similar websites in each cluster
                Map<SiteRecord, SiteRecord> mostSimilarSites = kMeans.getMostSimilarSitesInClusters();
        for (SiteRecord centroid : mostSimilarSites.keySet()) {
        System.out.println("Initial Centroid: " + centroid.getUrl());
        System.out.println("Most Similar Site: " + mostSimilarSites.get(centroid).getUrl());
        }

        kMeans.plotClusters();
        }
}

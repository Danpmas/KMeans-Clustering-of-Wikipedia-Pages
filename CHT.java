import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;
class CHT<K, V> implements java.io.Serializable {
    static final class Node<K, V> implements java.io.Serializable {
        K key;
        V value;
        Node next;

        Node(K k, V v, Node<K, V> n) {
            key = k;
            value = v;
            next = n;
        }
    }

    private Node<K, V>[] table;
    private int size;
    private static final float LOAD_FACTOR_THRESHOLD = 0.75f;

    public CHT() {
        table = new Node[8];
        size = 0;
    }

    public Map<K, V> getWordFrequenciesForWebsite(String url) {
        CHT<K, V> wordFrequencies = new CHT<>();
        Node<K, V> current = table[hash((K) url)];
        while (current != null) {
            if (current.key.equals(url)) {
                wordFrequencies.putAll((Map<? extends K, ? extends V>) current.value);
                break;
            }
            current = current.next;
        }
        return (Map<K, V>) wordFrequencies;
    }

    public void serialize(String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(this);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static CHT<String, Integer> deserialize(String fileName) {
        CHT<String, Integer> cht = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            cht = (CHT<String, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return cht;
    }

    private int hash(K key) {
        return Math.abs(key.hashCode()) % table.length;
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        for (int i = 0; i < table.length; ++i) {
            table[i] = null;
        }
        size = 0;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new HashSet<>();
        for (Node<K, V> e : table) {
            while (e != null) {
                entrySet.add(new AbstractMap.SimpleEntry<>(e.key, e.value));
                e = e.next;
            }
        }
        return entrySet;
    }

    private void resize() {
        Node<K, V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        int newCapacity = oldCapacity << 1;
        Node[] newTable = new Node[newCapacity];
        for (Node<K, V> kvNode : oldTable) {
            Node<K, V> e = kvNode;
            while (e != null) {
                Node next = e.next;
                int j = Math.abs(e.key.hashCode()) % newCapacity;
                e.next = newTable[j];
                newTable[j] = e;
                e = next;
            }
        }
        table = newTable;
    }
    public V getOrDefault(K key, V defaultValue) {
        int index = hash(key);
        Node<K, V> e = table[index];
        while (e != null) {
            if (e.key.equals(key)) {
                return e.value;
            }
            e = e.next;
        }
        return defaultValue;
    }

    public void put(K key, V value) {
        int index = hash(key);
        Node<K, V> e = table[index];
        while (e != null) {
            if (e.key.equals(key)) {
                e.value = value;
                return;
            }
            e = e.next;
        }
        table[index] = new Node<>(key, value, table[index]);
        size++;

        if ((float) size / table.length >= LOAD_FACTOR_THRESHOLD) {
            resize();
        }
    }

    public V get(K key) {
        int index = hash(key);
        Node<K, V> e = table[index];
        while (e != null) {
            if (e.key.equals(key)) {
                return e.value;
            }
            e = e.next;
        }
        return null;
    }

    public void remove(K key) {
        int index = hash(key);
        Node<K, V> e = table[index];
        Node<K, V> prev = null;
        while (e != null) {
            if (e.key.equals(key)) {
                if (prev == null) {
                    table[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                size--;
                return;
            }
            prev = e;
            e = e.next;
        }
    }
    public static CHT<String, Integer> convertStringToCHT(String wordFrequencyString) {
        CHT<String, Integer> wordFrequencies = new CHT<>();
        String[] entries = wordFrequencyString.split("\n");

        for (String entry : entries) {
            String[] parts = entry.split(": ");
            if (parts.length == 2) {
                String word = parts[0];
                int frequency = Integer.parseInt(parts[1]);
                wordFrequencies.put(word, frequency);
            }
        }

        return wordFrequencies;
    }


    public boolean contains(K key) {
        int index = hash(key);
        Node<K, V> e = table[index];
        while (e != null) {
            if (e.key.equals(key)) {
                return true;
            }
            e = e.next;
        }
        return false;
    }

    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (Node<K, V> e : table) {
            while (e != null) {
                keys.add(e.key);
                e = e.next;
            }
        }
        return keys;
    }
}

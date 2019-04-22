
import java.util.*;

public class ProxyCacheLRU {

  private static int size_utilized = 0;
  public static int cache_size;
  public static Map<String, String> map = new HashMap<String, String>();

  public ProxyCacheLRU(int cacheSizeMB) {
    this.cache_size = (cacheSizeMB * 4) * 1000000;
  }

  private static PriorityQueue<ProxyCacheItem> timestamp_queue = new PriorityQueue<ProxyCacheItem>(200, new Comparator() {
    public int compare(Object a, Object b) {
      if (!(a instanceof ProxyCacheItem) || !(b instanceof ProxyCacheItem))
        return 0;
      ProxyCacheItem n1 = (ProxyCacheItem) a;
      ProxyCacheItem n2 = (ProxyCacheItem) b;
      return n1.getTimestamp().compareTo(n2.getTimestamp());
    }
  });

  private String remove() {
    ProxyCacheItem leastUsed = timestamp_queue.poll();
    if (leastUsed != null) {
      System.out.println("--------Cache Full, Removed from cache------------");
      return leastUsed.getValue();
    }
    return "";
  }

  private void update(String mostRecentEleKey) {
    Iterator<ProxyCacheItem> pqIterator = timestamp_queue.iterator();
    while (pqIterator.hasNext()) {
      ProxyCacheItem e = pqIterator.next();
      if (e.getValue().equals(mostRecentEleKey)) {
        pqIterator.remove();
        break;
      }
    }
    ProxyCacheItem mostRecent = new ProxyCacheItem();
    mostRecent.setTimestamp(new Date());
    mostRecent.setValue(mostRecentEleKey);
    timestamp_queue.offer(mostRecent);
  }

  public String get(String key) {
    String value = map.get(key);
    return value;
  }

  public boolean containsKey(String key) {
    System.out.println("======> "+ key);
    boolean flag = false;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (entry.getKey().equals(key)) {
        flag = true;
        update(key);
      }
    }
    return flag;
  }

  public String put(String key, String value) {
    if (map.containsKey(key)) {
      update(key);
      return key;
    } else {
      while ((size_utilized + value.length()) >= cache_size && timestamp_queue.size() > 0) {
        String leastUsedKey = remove();
        // System.out.println("^^^^^^Key to be removeed--->>"+ leastUsedKey);
        String leastRecentValue = map.get(leastUsedKey);
        map.remove(leastUsedKey);
        size_utilized -= leastRecentValue.length();
      }
      ProxyCacheItem e = new ProxyCacheItem();
      e.setValue(key);
      e.setTimestamp(new Date());
      timestamp_queue.offer(e);
      map.put(key, value);
      System.out.println("Inserted into Cache...");
      size_utilized += value.length();

      System.out.println("Cache Metrics----------\nSize Utilized: " + size_utilized + " ; Size remaining: "
          + (cache_size - size_utilized));
      return key;
    }
  }
}
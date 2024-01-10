package edu.tufts.vue.zotero;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.NoSuchElementException;

public class ZoteroItem {

  private Multimap<String, String> attributes =
    Multimaps.newArrayListMultimap();

  public ZoteroItem() {}

  public void addAttribute(String key, String value) {
    attributes.put(key, value);
  }

  public Multimap<String, String> getAttributeMap() {
    return attributes;
  }
}

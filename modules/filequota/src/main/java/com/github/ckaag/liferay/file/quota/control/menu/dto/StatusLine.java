package com.github.ckaag.liferay.file.quota.control.menu.dto;

import java.util.AbstractMap;

public class StatusLine {

  final String label;
  final boolean enabled;
  final String used;
  final String total;

  public StatusLine(String label, boolean enabled, String used, String total) {
    this.label = label;
    this.enabled = enabled;
    this.used = used;
    this.total = total;
  }

  public StatusLine(AbstractMap.SimpleEntry<Long, Long> usedTotalPair, String fullName) {
    this(fullName, usedTotalPair.getKey() != null && usedTotalPair.getValue() != null,
        formatAsHumanReadableString(usedTotalPair).getKey(),
        formatAsHumanReadableString(usedTotalPair).getValue());
  }

  private static AbstractMap.SimpleEntry<String, String> formatAsHumanReadableString(
      AbstractMap.SimpleEntry<Long, Long> usedTotalPair) {
    // computation by rounding up to next int with 1-3 digits
    if (usedTotalPair.getValue() != null && usedTotalPair.getKey() != null) {
      Long a = usedTotalPair.getKey();
      Long b = usedTotalPair.getValue();
      if ((b >= 1024 * 1024 * 1024)) {
        return new AbstractMap.SimpleEntry<>(
            "" + (Math.ceil(((double) a) / (1024 * 1024 * 1024))) + "GB",
            "" + (Math.floor(((double) b) / (1024 * 1024 * 1024))) + "GB");
      } else if (between(b, 1024 * 1024, 1024 * 1024 * 1024)) {
        return new AbstractMap.SimpleEntry<>("" + (Math.ceil(((double) a) / (1024 * 1024))) + "MB",
            "" + (Math.floor(((double) b) / (1024 * 1024))) + "MB");
      } else if (between(b, 1024, 1024 * 1024)) {
        return new AbstractMap.SimpleEntry<>("" + (Math.ceil(((double) a) / (1024))) + "KB",
            "" + (Math.floor(((double) b) / (1024))) + "KB");
      } else {
        return new AbstractMap.SimpleEntry<>("" + (Math.ceil(((double) a) / (1))) + "B",
            "" + (Math.floor(((double) b) / (1))) + "B");
      }
    } else {
      return new AbstractMap.SimpleEntry<>("", "");
    }
  }

  private static boolean between(long b, long min, long max) {
    return (b >= min && b < max);
  }


  public String getLabel() {
    return label;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getUsed() {
    return used;
  }

  public String getTotal() {
    return total;
  }
}

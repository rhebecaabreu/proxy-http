import java.util.*;

class ProxyCacheItem {
  private String data_content;
  private Date timestamp;

  public String getValue() {
    return data_content;
  }

  public void setValue(String value) {
    this.data_content = value;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public boolean equals(ProxyCacheItem e) {
    return data_content.equals(e.getValue());
  }

}

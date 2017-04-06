package software.wings.helpers.ext.nexus.model;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by srinivas on 4/5/17.
 */
@XmlType(name = "data")
@XmlAccessorType(XmlAccessType.FIELD)
public class Data implements Serializable {
  private String type;
  private boolean leaf;
  private String nodeName;
  private String path;

  @XmlElementWrapper(name = "children")
  @XmlElement(name = "indexBrowserTreeNode")
  private List<IndexBrowserTreeNode> children;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isLeaf() {
    return leaf;
  }

  public void setLeaf(boolean leaf) {
    this.leaf = leaf;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<IndexBrowserTreeNode> getChildren() {
    return children;
  }

  public void setChildren(List<IndexBrowserTreeNode> children) {
    this.children = children;
  }
}

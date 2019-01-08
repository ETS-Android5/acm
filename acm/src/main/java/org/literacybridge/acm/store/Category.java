package org.literacybridge.acm.store;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public class Category {
  private final String id;
  private String name;
  private int order;
  private Category parent;
  private final List<Category> children;
  // If this is true, it should not be possible to assign a message to the category. To effect that,
  // the 'categories' tree control omits them.
  // TODO: A better and more general solution will be to make the visible categories configurable by
  // project.
  private boolean nonAssignable;
  // Should this category be shown to the user? If not, a message won't be able to be assigned
  // to the category.
  private boolean visible;

  Category(String id) {
    this.id = id;
    this.children = Lists.newLinkedList();
    this.nonAssignable = false;
    this.visible = true;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategoryName() {
    return name;
  }
  public String getFullName() {
      // If no parent, it's the root; we don't care that the root has a name of "root".
      if (parent == null) return "";
      // Name, if any, of the parent.
      String parentName = parent.getFullName();
      if (parentName.length() == 0) return name;
      // Append this node's name with the parent.
      return parentName + ":" + name;
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public Category getParent() {
    return parent;
  }

  public void setParent(Category parent) {
    this.parent = parent;
  }

  void addChild(Category childCategory) {
    children.add(childCategory);
  }

  public Iterable<Category> getChildren() {
    return children;
  }

  public Iterable<Category> getSortedChildren() {
    List<Category> sorted = Lists.newArrayList(children);
    sorted.sort((c1, c2) -> c1.getOrder() - c2.getOrder());

    return sorted;
  }

  public boolean hasChildren() {
    return !children.isEmpty();
  }

  @Deprecated // This is not really a uuid. Use getId
  public String getUuid() {
    return id;
  }
  public String getId() {
      return id;
  }

  @Override
  public String toString() {
    return this.name;
  }

    public boolean isNonAssignable() {
        return nonAssignable;
    }

    void setNonAssignable(boolean nonAssignable) {
        this.nonAssignable = nonAssignable;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean hasVisibleChildren() {
        for (Category child : children) {
            if (child.isVisible() || child.hasVisibleChildren()) return true;
        }
        return false;
    }

    public boolean isChildOf(Category cat) {
        Category test = this;
        while (test != null) {
            if (cat.equals(test)) {
                return true;
            }
            test = test.getParent();
        }
        return false;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) && Objects.equals(name, category.name)
            && Objects.equals(parent, category.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, parent);
    }

}

package com.base.spring.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Description : TODO(树状结构, type 不同，就代表不同类型的树)
 * User: h819
 * Date: 14-7-7
 * Time: 下午3:18
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "base_treenode")
@NamedEntityGraphs({
        @NamedEntityGraph(name = "treenode.parent", attributeNodes = {@NamedAttributeNode("parent")}),//级联 parent
        @NamedEntityGraph(name = "treenode.children", attributeNodes = {@NamedAttributeNode("children")}), // 级联 children
        @NamedEntityGraph(name = "treenode.parent.children", attributeNodes = {@NamedAttributeNode("parent"), @NamedAttributeNode("children")})})
// 二者都级联

// 只和 role 有关系
@Getter
@Setter
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) // 该 entity 启用 auditing
 public class TreeNodeEntity extends BaseEntity {

    private static final Logger logger = LoggerFactory.getLogger(TreeNodeEntity.class);
    /**
     * 组织名称
     */
    @Column(name = "name")
    private String name;
    /**
     * 菜单 url ，点击叶节点的时候，导航到的 url ，这个对菜单有用处。
     */
    @Column(name = "url")
    private String url;
    /**
     * url target
     * "_blank", "_self" 或 其他指定窗口名称
     */
    @Column(name = "target")
    private String target;
    /**
     * 菜单样式表 class 名称
     */
    @Column(name = "css")
    private String css;
    /**
     * 在同级中的排序。
     * 需要用机制保证排序是连续的，并且从 0 开始。
     * 如果这样，每次增加、删除或者移动，都需要把所有的节点进行重新排序。
     * 也可以不连续，但容易有点混乱，所以还是牺牲点性能，保证连续吧
     * order,index 为 mysql 关键字，不能用作字段名
     */
    @Column(name = "index_", nullable = false)
    private int index;

    /**
     * 是否为父节点，包含子节点，即为父节点 , true 时会显示为文件夹图标。
     * 添加节点时，如果没有子节点，不会显示为文件夹图标，即使是想添加父节点，也会显示为叶节点图表。
     */

    // @getter,@setter 自动生成的方法，没有 get 和 setter 会变成  isParent() 和 setParent(boolean parent)
    // 此时和有的反序列化工具如 fasterjson 对应不上 ，这是反序列化工具的 bug 么？
    //只好自己实现 getter 和 setter 方法，会覆盖 @Getter   @Setter ，但为了清晰，最好去掉
    @Column(name = "isParent", columnDefinition = "boolean default true")
    private boolean isParentNode;
    /**
     * 菜单类型 :必须
     */

    @Column(name = "type", nullable = false)
      private TreeNodeType type;
    /**
     * 父组织
     * 树状结构，root 节点（根节点），没有父节点，为 null
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TreeNodeEntity parent;
    /**
     * 子组织
     * jpa 中，orphanRemoval = true ，才可以删除子.
     */

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    //如果不用此句，默认是 FetchMode.SELECT ,查询每个被关联 child ，会发送一个查询语句，供发送 n+1个。FetchMode.SUBSELECT 通过子查询一次完成，对于 child 过多的情况下，应用。
    //n+1问题，需要根据实际情况调试
    @BatchSize(size = 100)//child 过多的情况下应用。
    @OrderBy("index ASC")
    private List<TreeNodeEntity> children = new ArrayList<>();


//    @ManyToMany(fetch = FetchType.LAZY, targetEntity = PrivilegeEntity.class)// 单向多对多，只在发出方设置，接收方不做设置
//    @JoinTable(name = "base_ref_treenodes_privileges", //指定关联表名
//            joinColumns = {@JoinColumn(name = "treenodes_id", referencedColumnName = "id")},////生成的中间表的字段，对应关系的发出端(主表) id
//            inverseJoinColumns = {@JoinColumn(name = "privilege_id", referencedColumnName = "id")}, //生成的中间表的字段，对应关系的接收端(从表) id
//            uniqueConstraints = {@UniqueConstraint(columnNames = {"treenodes_id", "privilege_id"})}) // 唯一性约束，是从表的联合字段
//    @Fetch(FetchMode.SUBSELECT)
//    @BatchSize(size = 100)//roles 过多的情况下应用。
//    private List<PrivilegeEntity> privileges = new ArrayList<>();
    /**
     * 多个节点，可以拥有同一种权限，如 节点编辑  ...
     * 一个节点，也可以又有多种权限
     */

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "treeNodes", targetEntity = RoleEntity.class)
    private List<RoleEntity> roles = new ArrayList<>();


    /**
     * 树状结构的层级,根节点 level = 0，依次递增
     * 不加 @Getter , @Setter 不自动生成
     */
    @Transient  // 不在数据库中建立字段
    @Setter(AccessLevel.NONE) // 不创建 Setter  方法，反序列化会有问题么?
    private int level;

    /**
     * JPA spec 需要无参的构造方法，用户不能直接使用。
     * 如果想要生成 Entity ，用其他有参数的构造方法。
     * DTOUtils 中使用 public 方法生成空对象
     */
    public TreeNodeEntity() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }


    /**
     * 菜单创建，只提供这一个构造函数，强制要求录入必需的数据项
     * 菜单需要初始化
     *
     * @param type         节点类型
     * @param name         名称
     * @param index        节点的排序序号 index
     * @param isParentNode 本身是否是父节点。
     *                     由于使用了 dtoUtils 设定转换深度（children 可能不在转换层次内，不会转换，会有 children = null 情况，此时无法从 children 是否有值来判断本身是否父节点），
     *                     所以 isParent 还是放在构造函数内，直接作为属性设置，可以直接判断。
     * @param parent       父菜单
     */

    public TreeNodeEntity(TreeNodeType type, String name, int index, boolean isParentNode, TreeNodeEntity parent) {

        if (!name.contains("root_")) // 初始化根节点时，需要设置 parent 为 null (InitializeService.java) ，其他情况 parent 不允许 为 null
            Assert.notNull(parent, "parent can not be null.");

        this.name = name;
        this.type = type;
        this.index = index;
        this.isParentNode = isParentNode;
        this.parent = parent;

    }

    /**
     * 添加指定节点到当前节点所有子节点队列的尾部，即添加的节点 index 最大
     *
     * @param child
     */
    public void addChildToLastIndex(TreeNodeEntity child) {

        if (child == null) return;
        child.setIndex(children.size()); // list 的 index 从 0 开始
        children.add(child);
        child.setParent(this);

    }

    /**
     * 添加指定节点到当前节点的 index 位置
     * 每次添加或者删除了子元素，都需要重新标注其 index 属性
     *
     * @param child the node to add to the tree as a child of <code>this</code>
     * @param index the position within the children list to add the child.
     */
    public void addChildToIndex(TreeNodeEntity child, int index) {

        if (child == null) return;

        if (index < 0 || index > children.size()) {             //加在第一位
            throw new IllegalArgumentException(index + " 小于 0 或者大于子节点总数");
        }

//        System.out.println(MyStringUtils.center(" original ", 80, "*"));
//        for (TreeNodeEntity entity : children)
//            System.out.println(String.format("%s  |  %s | %d", entity.getName(), entity.getIndex(), children.indexOf(entity)));

        sortChildren(children); //按照元素在 list 的位置信息，并设置 index 属性

//        System.out.println(MyStringUtils.center(" modify by sort ", 80, "*"));
//        for (TreeNodeEntity entity : children)
//            System.out.println(String.format("%s  |  %s | %d", entity.getName(), entity.getIndex(), children.indexOf(entity)));


        if (!children.contains(child)) { // 不同父节点下添加后，只变换位置
            children.add(index, child);
            while (index + 1 < children.size()) {//只变换 index 后面的元素位置
                children.get(index + 1).setIndex(index + 1);
                index++;
            }

            child.setParent(this);

        } else { // 同一个父节点下移动，不添加，只变换位置

            children.remove(child);
            sortChildren(children);
            children.add(index, child);
            while (index < children.size()) { //只变换 index 后面的元素位置
                children.get(index).setIndex(index);
                index++;

            }
        }

//        System.out.println(MyStringUtils.center(" modify by sort1 ", 80, "*"));
//
//        for (TreeNodeEntity entity : children)
//            System.out.println(String.format("%s  |  %s | %d", entity.getName(), entity.getIndex(), children.indexOf(entity)));


    }

    /**
     * 清空子
     */
    public void clearChildren() {
        if (!children.isEmpty())
            children.clear();
    }


    /**
     * list 中的 TreeNodeEntity 按照 index 属性排序后，重新设置 TreeNodeEntity 的 index 的值为 TreeNodeEntity 在 list 中的位置。
     *
     * @param children
     */
    private void sortChildren(List<TreeNodeEntity> children) {
        // Sorting 便于利用 list 的 indexOf 方法
        Collections.sort(children, new Comparator<TreeNodeEntity>() {
            @Override
            public int compare(TreeNodeEntity child1, TreeNodeEntity child2) {
                return Integer.compare(child1.getIndex(), child2.getIndex());
            }
        });

        for (TreeNodeEntity entity : children)
            entity.setIndex(children.indexOf(entity));

    }

    /**
     * 添加单个元素到指定位置，并按照元素在 list 中的位置，重新设置元素的 index 属性
     *
     * @param children
     * @param child    待添加的元素
     * @param index    添加元素到 list 的 index 位置
     */
    private void addSortChildren(List<TreeNodeEntity> children, TreeNodeEntity child, int index) {


        if (index < 0) {             //加在第一位
            index = 0;
            child.setIndex(index);
        }
        if (index > children.size()) {      //加在最后
            index = children.size();
            child.setIndex(index);
        }

        if (!children.contains(child)) { // 不同父节点下添加后，只变换位置
            children.add(index, child);
            while (index + 1 < children.size()) {
                children.get(index + 1).setIndex(index + 1);
                index++;

            }
        } else { // 同一个父节点下移动，不添加，只变换位置

            child.setIndex(index);
            while (index < children.size()) { //只变换 index 后面的元素位置
                children.get(index).setIndex(index);
                index++;

            }
        }
    }

    /**
     * Returns if this node is the root node in the tree or not.
     *
     * @return <code>true</code> if this node is the root of the tree;
     * <code>false</code> if it has a parent.
     */
    public boolean isRoot() {
        if (this.parent == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 树状结构的层级,根节点 level = 0，依次递增
     *
     * @return
     */
    public int getLevel() {
        getLevelInit(this);
        return level;
    }

    /**
     * 递归计算层级，根节点为 0 级
     *
     * @param entity
     * @return
     */
    private void getLevelInit(TreeNodeEntity entity) {

        if (entity.getParent() == null) {
            return;
        } else {
            level++;
            getLevelInit(entity.getParent());
        }
    }

}

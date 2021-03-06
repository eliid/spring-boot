package org.examples.spring.domain.onetoone.jointable;

import javax.persistence.*;

/**
 * Description : TODO()
 * User: h819
 * Date: 2014/11/18
 * Time: 16:55
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "example_person4")
@NamedEntityGraph(name = "examples.entity.onetoone.Person4Entity",//唯一id ,jpa 2.1属性
        attributeNodes = {@NamedAttributeNode("address")})
public class Person4Entity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    // 双向：发出端、接收端都需要设置。
    // 自动生成中间表，生成的字段默认是: 实体名称_id ，可以通过 @JoinColumn(name = "parent_id", referencedColumnName = "id")指定，这样可以显示的增加唯一约束条件
    // 双方实体表不发生变化
    @JoinTable(name = "example_ref_person4_address4",
            joinColumns = {@JoinColumn(name = "person_id", referencedColumnName = "id")},//生成的中间表的字段，对应关系的发出端(JoinTable 关键字所在的类即是发出端) id
            inverseJoinColumns = {@JoinColumn(name = "address_id", referencedColumnName = "id")},//生成的中间表的字段，对应关系的接收端(从表) id
            uniqueConstraints = {@UniqueConstraint(columnNames = {"person_id", "address_id"})})// 唯一性约束，是从表的联合字段)
    private Address4Entity address;

    public Person4Entity() {

    }

    public Person4Entity(String name) {

        this.name = name;
    }

    /**
     * @return
     */
    public Address4Entity getAddress() {
        return address;
    }

    /**
     * @param address
     */
    public void setAddress(Address4Entity address) {
        this.address = address;
    }

    /**
     * 添加关联关系
     */
    public void addAddress(Address4Entity address) {
        if (address != null) {
            this.setAddress(address);
            address.setPerson(this); //建立关联
        }
    }

    /**
     * 注意解除关联的方法(单向的不需要)
     */
    public void removeAddress(Address4Entity address) {
        if (address != null) {
            this.setAddress(null);
            address.setPerson(null);  //解除关联
        }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

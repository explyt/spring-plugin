package com.example;

import javax.persistence.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@DiscriminatorValue("employee")
public class Employee extends BaseEntity {

    @Column
    private String name;

    @Column
    private String position;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column
    private BigDecimal salary;

    @Column
    private LocalDate birthday;

    @Column
    private Boolean inVacation;

    public Boolean getInVacation() {
        return inVacation;
    }

    public void setInVacation(Boolean inVacation) {
        this.inVacation = inVacation;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

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

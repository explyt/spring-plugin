import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends CrudRepository<Department, Long> {
    @Query("SELECT d.employees FROM Department<caret> d WHERE d.id = :departmentId", nativeQuery = true)
    List<Employee> loadDepartmentEmployees(Long departmentId, String foo);
}

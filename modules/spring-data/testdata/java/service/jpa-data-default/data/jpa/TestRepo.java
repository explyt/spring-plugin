package data.jpa;

import data.jpa.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepo extends JpaRepository<Person, Integer> {

}

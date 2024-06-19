package data.jpa;

import data.jpa.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

interface TestRepo : JpaRepository<Person, Int> {
}

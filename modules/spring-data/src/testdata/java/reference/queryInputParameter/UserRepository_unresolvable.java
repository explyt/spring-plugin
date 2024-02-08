package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.firstName = :name")
    List<User> findByFirstName(String firstName);

    @Query("SELECT u FROM User u WHERE u.firstName = :lastName")
    List<User> findByLastName(@Param("name") String lastName);

    @Query("SELECT u FROM User u WHERE u.age >= ?2")
    List<User> findByAgeGreaterThan(int age);
}

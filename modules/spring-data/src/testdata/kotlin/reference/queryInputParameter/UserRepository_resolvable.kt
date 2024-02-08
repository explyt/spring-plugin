package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.firstName = :name")
    fun findByFirstName(@Param("name") firstName: String): List<User>

    @Query("SELECT u FROM User u WHERE u.lastName = :lastName")
    fun findByLastName(lastName: String): List<User>

    @Query("SELECT u FROM User u WHERE u.age >= ?1")
    fun findByAgeGreaterThan(age: Integer): List<User>
}

package com.app;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRepo extends JpaRepository<String, Integer> {

}

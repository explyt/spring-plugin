package com.app;

import org.springframework.data.jpa.repository.JpaRepository;

interface AppRepo : JpaRepository<String, Int>

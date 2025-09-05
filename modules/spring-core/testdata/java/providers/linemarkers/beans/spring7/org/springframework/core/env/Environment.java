package org.springframework.core.env;
public interface Environment {
    boolean matchesProfiles(String... profiles);
}

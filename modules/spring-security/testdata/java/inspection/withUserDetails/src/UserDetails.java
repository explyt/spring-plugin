package src;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;


@Component
public class UserDetails {

    @WithUserDetails(value="admin@example.com", userDetailsServiceBeanName = "admin1")
    public void getMessageAdmin() {
    }

    @WithUserDetails(value="user@example.com", userDetailsServiceBeanName = "badUser")
    public void getBadUser() {
    }

    @WithUserDetails(value="test@example.com", userDetailsServiceBeanName = "test")
    public void getTest() {
    }
    @PostConstruct
    public void init() {
        getMessageAdmin();
        getBadUser();
        getTest();
    }
}

@Service
class UserDetailsServiceImpl implements UserDetailsService{
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}

@Service
class UserDetailsServiceNotImpl {
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}

@Configuration
class UserDetailsConfiguration {
    @Bean
    UserDetailsServiceImpl admin(){
        return null;
    }

    @Bean
    UserDetailsServiceNotImpl badUser(){
        return null;
    }

    @Bean("test")
    UserDetailsService adminUserDetailsService() {
        return username -> {
            UserDetails UserDetails = new UserDetails() {
                @Override
                public Collection<? extends GrantedAuthority> getAuthorities() {
                    return Collections.emptyList();
                }

                @Override
                public String getPassword() {
                    return "pass";
                }

                @Override
                public String getUsername() {
                    return "name";
                }

                @Override
                public boolean isAccountNonExpired() {
                    return false;
                }

                @Override
                public boolean isAccountNonLocked() {
                    return false;
                }

                @Override
                public boolean isCredentialsNonExpired() {
                    return false;
                }

                @Override
                public boolean isEnabled() {
                    return false;
                }
            };
            return UserDetails;
        };
    }
}

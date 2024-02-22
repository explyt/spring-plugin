package src

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class WithUserKotlin {
    @WithUserDetails(value = "admin@example.com", userDetailsServiceBeanName = "kotlinAdmin")
    fun getMessageAdmin() {
    }

    @WithUserDetails(value = "user@example.com", userDetailsServiceBeanName = "kotlinBadUser")
    fun getBadUser() {
    }

    @WithUserDetails(value = "test@example.com", userDetailsServiceBeanName = "kotlinTest")
    fun getTest() {
    }

    @PostConstruct
    fun init() {
        getMessageAdmin()
        getBadUser()
        getTest()
    }
}

@Service
internal class KotlinUserDetailsServiceImpl : UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails? {
        return null
    }
}

@Service
internal class KotlinUserDetailsServiceNotImpl {
    @Throws(UsernameNotFoundException::class)
    fun loadUserByUsername(username: String?): UserDetails? {
        return null
    }
}

@Configuration
internal open class KotlinUserDetailsConfiguration {
    @Bean
    open fun kotlinAdmin(): KotlinUserDetailsServiceImpl? {
        return null
    }

    @Bean
    open fun kotlinBadUser(): KotlinUserDetailsServiceNotImpl? {
        return null
    }

    @Bean("kotlinTest")
    open fun kotlinAdminUserDetailsService(): UserDetailsService {
        return UserDetailsService { _: String? ->
            val userDetails = object : UserDetails {
                override fun getAuthorities(): Collection<GrantedAuthority> {
                    return emptyList()
                }

                override fun getPassword(): String {
                    return "pass"
                }

                override fun getUsername(): String {
                    return "name"
                }

                override fun isAccountNonExpired(): Boolean {
                    return false
                }

                override fun isAccountNonLocked(): Boolean {
                    return false
                }

                override fun isCredentialsNonExpired(): Boolean {
                    return false
                }

                override fun isEnabled(): Boolean {
                    return false
                }
            }
            userDetails
        }
    }
}

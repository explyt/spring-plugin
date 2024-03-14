package src

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.stereotype.Service


@Service
internal class KotlinUserDetailsServiceImpl : UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails? {
        return null
    }
}

@Configuration
internal open class KotlinUserDetailsConfiguration {
    @Bean
    open fun kotlinAdmin(): KotlinUserDetailsServiceImpl? {
        return null
    }

    @Bean("kotlinAdmin")
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

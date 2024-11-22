import com.explyt.apispring.rest.UserHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.coRouter

import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Configuration
class UserRouter(private val handler: UserHandler) {

    @Bean
    fun usersApi() = coRouter {
        "/api/users".nest {
            POST("", handler::createUser)
            "/id".nest {
                GET("", handler::getUserById)
                PATCH("", handler::updateUserData)
                "/claim".nest {
                    POST("", handler::claimTokens)
                }
                "/claims".nest {
                    GET("", handler::getClaimHistory)
                }
                "/tasks".nest {
                    GET("", handler::getTasks)
                    "/taskid".nest {
                        PATCH("", handler::startTask)
                    }
                }
            }
        }
    }

    @Bean
    fun mainRouter(userHandler: UserHandler) = coRouter {
        DELETE("/user/id", userHandler::getUserById)
        GET("/user/tasks", userHandler::getTasks)
    }
}

@Service
class UserHandler() {
    suspend fun createUser(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

    suspend fun getUserById(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

    suspend fun updateUserData(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

    suspend fun claimTokens(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

    suspend fun getClaimHistory(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

    suspend fun getTasks(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

    suspend fun startTask(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("")
    }

}
import org.springframework.stereotype.Service
import com.esprito.apispring.Response
import com.esprito.apispring.config.properties.RewardConfiguration
import com.esprito.apispring.config.properties.TimingConfiguration
import com.esprito.apispring.data.*
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

data class User(val id: Int) : com.esprito.apispring.rest.response.Response {}

@Service
class UserHandler() {
    suspend fun createUser(request: ServerRequest): ServerResponse {
        return Response.ok(User(1))
    }

    suspend fun getUserById(request: ServerRequest): ServerResponse {
        return Response.ok(User(1))
    }

    suspend fun updateUserData(request: ServerRequest): ServerResponse {
        return Response.ok(User(1))
    }
}
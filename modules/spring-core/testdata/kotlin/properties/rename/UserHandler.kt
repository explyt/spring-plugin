import org.springframework.beans.factory.annotation.Value

class UserHandler {
    @Value("\${server.timing_new.minutes_to_next_claim}")
    private val minutesToNextClaim: Long = 470L
}
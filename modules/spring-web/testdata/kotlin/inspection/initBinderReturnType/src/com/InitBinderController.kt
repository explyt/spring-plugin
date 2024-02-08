import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*


@RestController
class InitBinderController {
    @InitBinder("valid")
    fun validBinderMethod(dataBinder: WebDataBinder?) {
    }

    @InitBinder("invalid")
    fun invalidBinderMethod(dataBinder: WebDataBinder?): String {
        return ""
    }
}
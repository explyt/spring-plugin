import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "organizationFeignClient", url = "\${service.organization.api.url}")
interface OrganizationFeignClient {
    @PostMapping(value = "get")
    fun get(@RequestBody dto: Object): Object
}
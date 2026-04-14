import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

@Component
public class MockTest {
    @MockBean
    lateinit var demoServiceMock: DemoService

    @SpyBean
    lateinit var demoServiceSpy: DemoService
}

@Component
class DemoService {}
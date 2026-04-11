import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

@Component
public class MockTest {
    @MockBean
    DemoService demoServiceMock;
    @SpyBean
    DemoService demoServiceSpy;
}

@Component
class DemoService {}
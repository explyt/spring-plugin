import org.springframework.stereotype.Component

internal interface I

@Component
internal open class E

@Component
internal class A : E(), I

@Component
internal class B : E(), I

internal class C : I
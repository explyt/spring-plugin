import org.springframework.stereotype.Component

interface I

open class E

@Component
class A : I

@Component
class C : E(), I

class B : I
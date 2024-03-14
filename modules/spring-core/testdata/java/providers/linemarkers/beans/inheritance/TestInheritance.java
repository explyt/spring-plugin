import org.springframework.stereotype.Component;

interface I {}

@Component
class E {}

@Component
class A extends E implements I {}

@Component
class B extends E implements I {}

class C implements I {}

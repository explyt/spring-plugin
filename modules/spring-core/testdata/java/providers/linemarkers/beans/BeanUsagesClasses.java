import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

interface I {}

class E {}

@Component
class A implements I {}

@Component
class C extends E implements I {}

class B implements I {}




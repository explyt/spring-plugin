###

#### ENTRY is a terminal

> Valid
> ```jpaql
> SELECT p FROM Person p WHERE KEY(p.foo).bar > 0 AND VALUE(p.foo).bar < 0;
> SELECT p FROM Person p WHERE ENTRY(p.foo) > 0  
> ```
> Invalid
> ```jpaql
> SELECT p FROM Person p WHERE ENTRY(p.foo).bar > 0 
> ```

#### INDEX inputs is only an identifier token

> Valid
> ```jpaql
> SELECT INDEX(p) FROM Person p
> SELECT p FROM Person p WHERE INDEX(p) > 0  
> ```
> Invalid
> ```jpaql
> SELECT p FROM Person p WHERE INDEX(p.bar) > 0 
> ```

TODO:
- CAST
- FUNCTION('TRUNC', CURRENT_DATE)
- TREAT(_ as TYPE)


Quickfixes:
- Replace `==` with `=`

Inspections:
- enums in jpql
package math

import org.scalatest.FunSuite

class TestParser extends FunSuite {
  test("numbers 1") {
    assert(Parser("1") === Integer(1))
    assert(Parser("-23") === Integer(-23))
    assert(Parser("0") === Integer(0))
    assert(Parser("1234567890") === Integer(BigInt("1234567890")))
    assert(Parser("1/2") === Fraction(Integer(1), Integer(2)))
    assert(Parser("0.35") === Decimal(0.35))
  }
  
  test("numbers 2") {
    assert(Parser("\u22482") === ApproxNumber(2.0))
    assert(Parser("""\approx2.17""") === ApproxNumber(2.17))
    assert(Parser("-17/-22") === Fraction(Integer(-17), Integer(-22)))
    assert(Parser("\u22483.75") === ApproxNumber(3.75))
    assert(Parser("e") === ConstantE)
    assert(Parser("""\pi""") === ConstantPi)
  }
  
  test("complex numbers") {
    assert(Parser("2+3i") === ComplexNumber(Integer(2), Integer(3)))
    assert(Parser("3-2i") === ComplexNumber(Integer(3), Integer(-2)))
    assert(Parser("0.5+3.4i") === ComplexNumber(Decimal(0.5), Decimal(3.4)))
    assert(Parser("3.4-3.56i") === ComplexNumber(Decimal(3.4), Decimal(-3.56)))
    assert(Parser("7/2+(1/3)i") === ComplexNumber(Fraction(Integer(7), Integer(2)), Fraction(Integer(1), Integer(3))))
    assert(Parser("7/2-(2/3)i") === ComplexNumber(Fraction(Integer(7), Integer(2)), Fraction(Integer(-2), Integer(3))))
    //These don't work because addOrSub is called before complexNumber. Not sure how to get around that.
    //assert(Parser("7/2+1/3i") === ComplexNumber(Fraction(Integer(7), Integer(2)), Fraction(Integer(1), Integer(3))))
    //assert(Parser("7/2-2/3i") === ComplexNumber(Fraction(Integer(7), Integer(2)), Fraction(Integer(-2), Integer(3))))
  }
  
  test("complex numbers solo terms") {
    assert(Parser("4i") === ComplexNumber(Integer(0), Integer(4)))
  }

  test("vars") {
    assert(Parser("x") === Var("x"))
    assert(Parser("y") === Var("y"))
    assert(Parser("a") === Var("a"))
  }
  
  test("operations") {
    assert(Parser("x+2") === Sum(Var("x"), Integer(BigInt("2"))))
    assert(Parser("x-2") === Difference(Var("x"), Integer(BigInt("2"))))
    assert(Parser("x*2") === Product(Var("x"), Integer(BigInt("2"))))
    assert(Parser("x/2") === Quotient(Var("x"), Integer(BigInt("2"))))
    assert(Parser("x-1") === Difference(Var("x"), Integer(BigInt("1"))))
    assert(Parser("x^2") === Exponentiation(Var("x"), Integer(BigInt("2"))))
    assert(Parser("(x+2)/y") === Quotient(Sum(Var("x"), Integer(BigInt("2"))), Var("y")))
    assert(Parser("-1") === Integer(BigInt("-1")))
    assert(Parser("(x+2)^(x-1)") === {
        val x = Var("x")
        Exponentiation(Sum(x, Integer(BigInt("2"))), Difference(x, Integer(BigInt("1"))))
      }
    )
    assert(Parser("(x+2)(x-1)") === {
        val x = Var("x")
        Product(Sum(x, Integer(BigInt("2"))), Difference(x, Integer(BigInt("1"))))
      }
    )
    assert(Parser("(x+2)(x-1)") === Parser("(x+2)*(x-1)"))
    assert(Parser("log(x)") === Base10Logarithm(Var("x")))
    assert(Parser("ln(x)") === NaturalLogarithm(Var("x")))
    assert(Parser("(x+y)/(15-z)") === {
        val x = Var("x")
        val y = Var("y")
        val z = Var("z")
        val fifteen = Integer(BigInt("15"))
        Quotient(Sum(x, y), Difference(fifteen, z))
      }
    )
    assert(Parser("x/15-1") === Difference(Quotient(Var("x"), Integer(BigInt("15"))), Integer(BigInt("1"))))
    assert(Parser("(x+5)x+1") === {
        val x = Var("x")
        Sum(Product(Sum(x, Integer(BigInt("5"))), x), Integer(BigInt("1")))
        }
      )
  }

  test("precedence") {
    assert(Parser("3+2*x") === Sum(Integer(BigInt("3")), Product(Integer(BigInt("2")), Var("x"))))
    assert(Parser("y-z*x") === Difference(Var("y"), Product(Var("z"), Var("x"))))
    assert(Parser("a+b/c") === Sum(Var("a"), Quotient(Var("b"), Var("c"))))
    assert(Parser("-a^b") === Negation(Exponentiation(Var("a"), Var("b"))))
    assert(Parser("a*b/c") === Quotient(Product(Var("a"), Var("b")), Var("c")))
    assert(Parser("(-a)^b") === Exponentiation(Negation(Var("a")), Var("b")))
    assert(Parser("a-b/c") === Difference(Var("a"), Quotient(Var("b"), Var("c"))))
    assert(Parser("a-d^g*b+c/f") === Sum(Difference(Var("a"), Product(Exponentiation(Var("d"), Var("g")), Var("b"))), Quotient(Var("c"), Var("f"))))
    assert(Parser("-a*b") === Product(Negation(Var("a")), Var("b")))
    assert(Parser("-a-b") === Difference(Negation(Var("a")), Var("b")))
    assert(Parser("-a/b") === Quotient(Negation(Var("a")), Var("b")))
    assert(Parser("a/-b") === Quotient(Var("a"), Negation(Var("b"))))
    assert(Parser("a--b") === Difference(Var("a"), Negation(Var("b"))))
    assert(Parser("a+-b") === Sum(Var("a"), Negation(Var("b"))))
    assert(Parser("a^-b") === Exponentiation(Var("a"), Negation(Var("b"))))
    assert(Parser("5^2") === Exponentiation(Integer(5), Integer(2)))
    assert(Parser("-5^2") === Negation(Exponentiation(Integer(5), Integer(2))))
    assert(Parser("(-5)^2") === Exponentiation(Integer(-5), Integer(2)))
  }
  
  test("equivalence") {
    // most of these are failing
    // almost every error in TestParser and TestOperations occur when there is an implicit multiplication such
    // with a constant and a variable or a constant and a function, such as an expression in parenthesis or sqrt(x)
    // FAIL: assert(Parser("1+2").equals(Parser("2+1")) === true)
    // FAIL: assert(Parser("2*a").equals(Parser("a*2")) === true)
    // FAIL: assert(Parser("a+a").equals(Parser("2*a")) === true)
    // ERROR: assert(Parser("a+a").equals(Parser("2a")) === true)
    // FAIL: assert(Parser("2+1").equals(Parser("3")) === true)
    // FAIL: assert(Parser("(x+2)/5").equals(Parser("1/5*(x+2)")) === true)
    // ERROR: assert(Parser("2(x+3)").equals(Parser("2x+6")) === true)
    // ERROR: assert(Parser("2ab").equals(Parser("b2a")) === true)
    assert(Parser(".25").equals(Parser("1/4")) === true)
    // ERROR: assert(Parser("a^2+2ab+b^2").equals(Parser("ba2+a^2+b^2")) === true)
    // ERROR: assert(Parser("5a").equals(Parser("5*a")) === true)
    // ERROR: assert(Parser("5a").equals(Parser("a*5")) === true)
    // ERROR: assert(Parser("5sqrt(3)").equals(Parser("sqrt(3)5")) === true)
    // ERROR: assert(Parser("5sqrt(3)").equals(Parser("5*sqrt(3)")) === true)
    // ERROR: assert(Parser("5sqrt(3)").equals(Parser("sqrt(75)")) === true)
    assert(Parser("a^2").equals(Parser("a^(2)")) === true)
    // FAIL: assert(Parser("a-b").equals(Parser("a+-b")) === true)
    assert(Parser("a^b^c").equals(Parser("a^(b^c)")) === true)
    assert(Parser("a^-1").equals(Parser("a^(-1)")) === true)
    // FAIL: assert(Parser("a^-1").equals(Parser("1/(a^1)")) === true)
    assert(Parser("1/4(x+3)").equals(Parser("(1/4)(x+3)")) === true)
    assert(Parser("(a)(b)").equals(Parser("(a)*(b)")) === true)
    // ERROR: assert(Parser("(a)(b)").equals(Parser("ab")) === true)
    // FAIL: assert(Parser("a^2 * a^2").equals(Parser("a^4")) === true)
    // FAIL: assert(Parser("(a^3)/(a^2)").equals(Parser("a")) === true)
    // ERROR: assert(Parser("a^(-1)").equals(Parser("1/a")) === true)
  }
}

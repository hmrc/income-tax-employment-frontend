


val optionInt: Option[Int] = None

optionInt.fold(println("ITS EMPTY"))(_ => println("ITS NOT EMPTY"))

case class Test(name: String, age: Option[Int])

val x = Test("Ben", None)

x.copy(age = Some(100))

x.copy(name = "Tim")


val scenario: Either[Int, String] = Left(500)

scenario match {
  case Left(value) => println(value)
  case Right(value) => println(value)
}


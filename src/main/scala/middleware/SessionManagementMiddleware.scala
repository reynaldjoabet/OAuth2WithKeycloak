package middleware

//import cats.effect.kernel.Async
//import org.http4s.Request
//import org.http4s.server.HttpMiddleware
//import cats.data.Kleisli

//final case class SessionManagementMiddleware[F[_]: Async]()

object SessionManagementMiddleware {
  // def apply[F[_]: Async]: HttpMiddleware[F] = service =>
  // Kleisli { request: Request[F] =>
  // ???
  // }
  trait MyFunctor[F[_]] {
    def map[A, B](fa: F[A])(f: A => B): F[B]
  }
//We can think of a type constructor such as List[_] as a type-level function, mapping a type X to the type List[X].
//This can be expressed with a special syntax, called a type lambda:

//`[X] =>> List[X]`

  object MyFunctor {

    val option = new MyFunctor[Option] {
      override def map[A, B](fa: Option[A])(f: A => B): Option[B] = ???
    }

    val list = new MyFunctor[List] {
      override def map[A, B](fa: List[A])(f: A => B): List[B] = ???
    }
    val map = new MyFunctor[({ type T[X] = Map[Int, X] })#T] {
      override def map[A, B](fa: Map[Int, A])(f: A => B): Map[Int, B] = ???
    }

    val either = new MyFunctor[({ type T[X] = Either[Int, X] })#T] {
      override def map[A, B](fa: Either[Int, A])(f: A => B): Either[Int, B] =
        ???
    }

    def either1[X] = new MyFunctor[({ type T[Y] = Either[X, Y] })#T] {
      override def map[A, B](fa: Either[X, A])(f: A => B): Either[X, B] = ???

    }

  }

  def foo[F[_], A](fa: F[A]) = fa.toString()
  val m = foo[Function1[Int, *], Int](_ + 4)
  val n = foo { x: Int => x + 3 }

}

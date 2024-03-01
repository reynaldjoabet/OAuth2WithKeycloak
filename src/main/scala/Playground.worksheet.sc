import java.time.LocalDate

import cats.data._
import cats.Monad
import cats.Applicative
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.server._
import cats._
import cats.effect._
import doobie._
import doobie.implicits._
import cats.implicits._
import cats.effect.IO
import java.lang.Exception
import doobie.syntax._
import doobie.util._
import doobie.Meta._

import cats.implicits.catsSyntaxList
import cats.{Applicative, Monad}
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor

import cats.implicits
case class Book(id: Int, title: String, author: String)

import scala.collection.mutable

trait BookRepository {
  def getAllBooks: List[Book]
  def getBookById(id: Int): Option[Book]
  def addBook(book: Book): Unit
  def updateBook(book: Book): Unit
  def deleteBook(id: Int): Unit
}

class InMemoryBookRepository extends BookRepository {
  private val books = mutable.Map.empty[Int, Book]
  private var nextId = 1

  def getAllBooks: List[Book] = books.values.toList

  def getBookById(id: Int): Option[Book] = books.get(id)

  def addBook(book: Book): Unit = {
    val bookWithId = book.copy(id = nextId)
    books.put(nextId, bookWithId)
    nextId += 1
  }

  def updateBook(book: Book): Unit = {
    books.update(book.id, book)
  }

  def deleteBook(id: Int): Unit = {
    books.remove(id)
  }
}

//Now, let's create a service that uses the repository and provides higher-level business logic:

trait BookService[F[_]] {
  def getAllBooks: F[List[Book]]
  def getBookById(id: Int): F[Option[Book]]
  def addBook(title: String, author: String): F[Unit]
  def updateBook(id: Int, title: String, author: String): F[Unit]
  def deleteBook(id: Int): F[Unit]
}

class BookServiceImpl(repository: BookRepository) extends BookService[IO] {
  def getAllBooks: IO[List[Book]] = IO(repository.getAllBooks)

  def getBookById(id: Int): IO[Option[Book]] = IO(repository.getBookById(id))

  def addBook(title: String, author: String): IO[Unit] = IO(repository.addBook(Book(0, title, author)))

  def updateBook(id: Int, title: String, author: String): IO[Unit] = {
    IO.fromOption(Some(Book(id, title, author)))(new Exception("Invalid book data.")).flatMap { updatedBook =>
      IO(repository.updateBook(updatedBook))
    }
  }

  def deleteBook(id: Int): IO[Unit] = IO(repository.deleteBook(id))
}

trait BookRepository1[F[_]] {
  def getAllBooks: F[List[Book]]
  def getBookById(id: Int): F[Option[Book]]
  def addBook(book: Book): F[Unit]
  def updateBook(book: Book): F[Unit]
  def deleteBook(id: Int): F[Unit]
}

object BookRepository1 {
  def apply[F[_]](implicit ev: BookRepository1[F]): BookRepository1[F] = ev

  implicit class Ops[F[_]: BookRepository1](bookRepo: BookRepository1[F]) {

    def getAllBooks: F[List[Book]] = BookRepository1[F].getAllBooks
    def getBookById(id: Int): F[Option[Book]] = BookRepository1[F].getBookById(id)
    def addBook(book: Book): F[Unit] = BookRepository1[F].addBook(book)
    def updateBook(book: Book): F[Unit] = BookRepository1[F].updateBook(book)
    def deleteBook(id: Int): F[Unit] = BookRepository1[F].deleteBook(id)
  }

  def doobieInterpreter[F[_]](transactor: Transactor[F])(implicit m: MonadCancel[F, Throwable]): BookRepository1[F] =
    new BookRepository1[F] {
      def getAllBooks: F[List[Book]] =
        sql"SELECT id, title, author FROM books".query[Book].to[List].transact(transactor)

      def getBookById(id: Int): F[Option[Book]] =
        sql"SELECT id, title, author FROM books WHERE id = $id".query[Book].option.transact(transactor)

      def addBook(book: Book): F[Unit] =
        sql"INSERT INTO books (title, author) VALUES (${book.title}, ${book.author})"
          .update
          .run
          .transact(transactor)
          .void

      def updateBook(book: Book): F[Unit] =
        sql"UPDATE books SET title = ${book.title}, author = ${book.author} WHERE id = ${book.id}"
          .update
          .run
          .transact(transactor)
          .void

      def deleteBook(id: Int): F[Unit] =
        sql"DELETE FROM books WHERE id = $id".update.run.transact(transactor).void
    }
}

case class Role(id: Long, roleName: String, description: String)
case class User(id: Long, userName: String, passwordHash: String, email: String, createdAt: LocalDate, isActive: Boolean)

// CREATE TABLE roles (
//     id SERIAL PRIMARY KEY,
//     role_name VARCHAR(50) UNIQUE NOT NULL,
//     description TEXT
// );

trait UserRepository[F[_]] {
  def createUser(user: User, roles: List[Role]): F[Unit]
  def getUserById(userId: Long): F[Option[User]]
  def updateUser(user: User, roles: List[Role]): F[Unit]
  def deleteUser(userId: Long): F[Unit]
  def getUserRoles(userId: Long): F[List[Role]]
}

object UserRepository {
  // existing code...

  def doobieInterpreter[F[_]](transactor: Transactor[F])(implicit m: MonadCancel[F, Throwable]): UserRepository[F] =
    new UserRepository[F] {
      // existing code...

      def getUserById(userId: Long): F[Option[User]] = m.pure(None)
      def deleteUser(userId: Long): F[Unit] = m.unit

      def createUser(user: User, roles: List[Role]): F[Unit] = m.unit
      // for {
      //   _ <- m???//sql"INSERT INTO users (id, username, password_hash, email, created_at, is_active) VALUES (${user.id}, ${user.userName}, ${user.passwordHash}, ${user.email}, ${user.createdAt}, ${user.isActive})".update.run.transact(transactor).void
      //   _ <- addRolesToUser(user.id, roles)
      // } yield ()

      def updateUser(user: User, roles: List[Role]): F[Unit] =
        for {
          _ <-
            sql"UPDATE users SET username = ${user.userName}, password_hash = ${user.passwordHash}, email = ${user.email}, is_active = ${user.isActive} WHERE id = ${user.id}"
              .update
              .run
              .transact(transactor)
              .void
          _ <- deleteRolesFromUser(user.id)
          _ <- addRolesToUser(user.id, roles)
        } yield ()

      private def addRolesToUser(userId: Long, roles: List[Role]): F[Unit] =
        roles.traverse_ { role =>
          sql"INSERT INTO user_roles (user_id, role_id) VALUES ($userId, ${role.id})"
            .update
            .run
            .transact(transactor)
            .void
        }

      private def deleteRolesFromUser(userId: Long): F[Unit] =
        sql"DELETE FROM user_roles WHERE user_id = $userId".update.run.transact(transactor).void

      def getUserRoles(userId: Long): F[List[Role]] =
        sql"SELECT r.id, r.role_name, r.description FROM roles r INNER JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = $userId"
          .query[Role]
          .to[List]
          .transact(transactor)
    }

}

import scala.util.Random

Random.alphanumeric.map(_.toUpper).take(80)

//RBACMiddleware

//Role-based access control (RBAC) refers to the idea of assigning permissions to users based on their role within an organization. It offers a simple, manageable approach to access management that is less prone to error than assigning permissions to users individually.
val numbers = List(1, 2, 3, 4, 5)

// Performing a left scan with addition
val scanResult: List[Int] = numbers.scanLeft(0)(_ + _)

// Result: List(0, 1, 3, 6, 10, 15)

val numbers1 = List(1, 2, 3, 4, 5)

// Performing a right scan with subtraction
val scanResult1: List[Int] = numbers.scanRight(0)(_ - _)

// Result: List(3, 2, 0, -3, -5, 0)
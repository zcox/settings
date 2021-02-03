package settings

import java.util.UUID
import cats._
import cats.data._
import org.http4s._
import org.http4s.implicits._
import scala.util.Try

case class User(userId: UUID, tenantId: UUID)

object DumbAuth {

  /*
  We don't need a real auth system for this example application.
  When testing via curl, just include headers to simulate auth.
  */

  val UserIdHeader = "X-User-Id".ci
  val TenantIdHeader = "X-Tenant-Id".ci

  def toUuid(s: String): Option[UUID] = 
    Try(UUID.fromString(s)).toOption

  def toUuid(h: Header): Option[UUID] =
    toUuid(h.value)

  def impl[F[_]: Applicative]: Kleisli[OptionT[F, *], Request[F], User] = 
    Kleisli { request => 
      OptionT.fromOption[F](
        for {
          userId <- request.headers.get(UserIdHeader).flatMap(toUuid)
          tenantId <- request.headers.get(TenantIdHeader).flatMap(toUuid)
        } yield User(userId, tenantId)
      )
    }
}

import java.util.UUID
import java.time.Instant
import scala.concurrent.duration.MILLISECONDS
import cats.implicits._
import cats.Functor
import cats.effect.{Sync, Clock}

package object settings {

  def randomUuid[F[_]: Sync]: F[UUID] = 
    Sync[F].delay(UUID.randomUUID())

  def now[F[_]: Functor: Clock]: F[Instant] = 
    Clock[F].realTime(MILLISECONDS).map(Instant.ofEpochMilli)

}

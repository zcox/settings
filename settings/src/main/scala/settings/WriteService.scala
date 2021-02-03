package settings

import cats.implicits._
import cats.effect.Bracket
import doobie._
import doobie.implicits._

trait WriteService[F[_]] {
  def write(event: DisplaySettingsUpdated, view: DisplaySettings): F[Unit]
}

object WriteService {
  def impl[F[_]](
    displaySettingsRepository: DisplaySettingsRepository[ConnectionIO],
    eventRepository: EventRepository[ConnectionIO],
    xa: Transactor[F],
  )(implicit F: Bracket[F, Throwable]): WriteService[F] = 
    new WriteService[F] {

      def tx[A](ops: ConnectionIO[A]*): F[Unit] = 
        ops.sequence_.transact(xa)

      //insert event and update view in same transaction
      override def write(event: DisplaySettingsUpdated, view: DisplaySettings): F[Unit] = 
        tx(eventRepository.putEvent(event), displaySettingsRepository.putDisplaySettings(view))
    }
}

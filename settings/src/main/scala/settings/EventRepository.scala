package settings

import cats._
import cats.implicits._
import cats.effect.Bracket
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.circe.jsonb.implicits._

//https://github.com/tpolecat/doobie/releases/tag/v0.8.8
// ^^ suggests we use the legacy implicits with postgres, instead of doobie.implicits.javatime._
import doobie.implicits.legacy.instant._
import io.circe.syntax._

//default encoder for ADT includes discriminator field, e.g. {"DisplaySettingsUpdated":{...
import io.circe.generic.auto._

trait EventRepository[F[_]] {
  def putEvent(event: SettingsEvent): F[Unit]
}

object EventRepository {

  def db: EventRepository[ConnectionIO] = 
    new EventRepository[ConnectionIO] {
      override def putEvent(event: SettingsEvent): ConnectionIO[Unit] = 
        sql"INSERT INTO settings_events (event_id, created, event) VALUES (${event.eventId}, ${event.timestamp}, ${event.asJson})".update.run.void
    }

  def mapK[F[_], G[_]](r: EventRepository[F])(f: F ~> G): EventRepository[G] = 
    new EventRepository[G] {
      override def putEvent(event: SettingsEvent): G[Unit] = 
        f(r.putEvent(event))
    }

  def dbTx[F[_]](xa: Transactor[F])(implicit F: Bracket[F, Throwable]): EventRepository[F] = 
    mapK(db)(xa.trans)
}

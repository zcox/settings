package settings

import cats._
import cats.implicits._
import cats.effect.Bracket
import java.util.UUID
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

trait DisplaySettingsRepository[F[_]] {
  def getDisplaySettings(tenantId: UUID): F[Option[DisplaySettings]]
  def putDisplaySettings(settings: DisplaySettings): F[Unit]
}

object DisplaySettingsRepository {

  case class DisplaySettingsPg(
    tenantId: UUID,
    name: String,
    description: Option[String],
  )

  val DefaultDescription = "[no description yet]"

  object DisplaySettingsPg {
    def toDisplaySettings(pg: DisplaySettingsPg): DisplaySettings = 
      DisplaySettings(
        tenantId = pg.tenantId,
        name = pg.name,
        description = pg.description.getOrElse(DefaultDescription)
      )
  }

  def db: DisplaySettingsRepository[ConnectionIO] = 
    new DisplaySettingsRepository[ConnectionIO] {

      override def getDisplaySettings(tenantId: UUID): ConnectionIO[Option[DisplaySettings]] = 
        sql"SELECT tenant_id, name, description FROM display_settings WHERE tenant_id = $tenantId"
          .query[DisplaySettingsPg]
          .option
          .map(_.map(DisplaySettingsPg.toDisplaySettings))

      override def putDisplaySettings(settings: DisplaySettings): ConnectionIO[Unit] = 
        //TODO does this need to convert to DisplaySettingsPg?
        sql"""
          INSERT INTO display_settings (tenant_id, name, description) VALUES (${settings.tenantId}, ${settings.name}, ${settings.description})
          ON CONFLICT (tenant_id) DO UPDATE SET name = ${settings.name}, description = ${settings.description} WHERE display_settings.tenant_id = ${settings.tenantId}
        """.update.run.void
    }

  def mapK[F[_], G[_]](r: DisplaySettingsRepository[F])(f: F ~> G): DisplaySettingsRepository[G] = 
    new DisplaySettingsRepository[G] {
      override def getDisplaySettings(tenantId: UUID): G[Option[DisplaySettings]] = 
        f(r.getDisplaySettings(tenantId))
      override def putDisplaySettings(settings: DisplaySettings): G[Unit] =
        f(r.putDisplaySettings(settings))
    }

  def dbTx[F[_]](xa: Transactor[F])(implicit F: Bracket[F, Throwable]): DisplaySettingsRepository[F] = 
    mapK(db)(xa.trans)
}

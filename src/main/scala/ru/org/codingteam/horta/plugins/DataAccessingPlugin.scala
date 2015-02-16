package ru.org.codingteam.horta.plugins

import akka.util.Timeout
import ru.org.codingteam.horta.database.PersistentStore
import scalikejdbc.DBSession

import scala.concurrent.Future

/**
 * Trait for the plugins that can access the database.
 */
trait DataAccessingPlugin[Repository <: ru.org.codingteam.horta.database.Repository] extends BasePlugin {

  /**
   * A full plugin definition.
   * @return plugin definition.
   */
  override protected def pluginDefinition: PluginDefinition = super.pluginDefinition.copy(repository = Some(repository))

  /**
   * Repository used for data access in this plugin.
   */
  protected abstract val repository: Repository

  /**
   * Execute repository action. Use with caution - do not mess with plugin data members within action; that is not
   * context-safe.
   *
   * @param action an action.
   * @tparam T type of action result.
   * @return future that will be resolved after action execution.
   */
  protected def withDatabase[T](action: (Repository, DBSession) => T)(implicit timeout: Timeout): Future[T] = {
    PersistentStore.execute[Repository, T](name, store) { case (r, s) => action(r, s) }
  }

  private val store = context.actorSelection("/user/core/store")

}

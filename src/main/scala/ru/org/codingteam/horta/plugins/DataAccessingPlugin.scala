package ru.org.codingteam.horta.plugins

import akka.util.Timeout
import ru.org.codingteam.horta.database.{RepositoryFactory, PersistentStore}
import scalikejdbc.DBSession

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Trait for the plugins that can access the database.
 */
trait DataAccessingPlugin[Repository] extends BasePlugin {

  /**
   * A full plugin definition.
   * @return plugin definition.
   */
  override protected def pluginDefinition: PluginDefinition =
    super.pluginDefinition.copy(repositoryFactory = Some(RepositoryFactory(schema, createRepository)))

  /**
   * Schema name for database storage.
   */
  protected val schema: String

  /**
   * A function creating the repository for database access.
   */
  protected val createRepository: (DBSession) => Repository

  /**
   * Execute repository action. Use with caution - do not mess with plugin data members within action; that is not
   * context-safe.
   *
   * @param action an action.
   * @tparam T type of action result.
   * @return future that will be resolved after action execution.
   */
  protected def withDatabase[T: ClassTag](action: (Repository) => T)
                                         (implicit timeout: Timeout): Future[T] = {
    PersistentStore.execute[Repository, T](name, store)(action)
  }

  private val store = context.actorSelection("/user/core/store")

}

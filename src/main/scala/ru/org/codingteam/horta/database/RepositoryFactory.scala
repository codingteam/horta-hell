package ru.org.codingteam.horta.database

import scalikejdbc.DBSession

/**
 * A repository factory. Will be used to create the repository.
 *
 * @param schema schema name for current repository.
 * @param create a repository creation function.
 */
case class RepositoryFactory(schema: String, create: (DBSession => Any))

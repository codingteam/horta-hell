package ru.org.codingteam.horta.database

trait Repository {

  /**
   * Schema name for current DAO.
   * @return schema name.
   */
  def schema: String

}

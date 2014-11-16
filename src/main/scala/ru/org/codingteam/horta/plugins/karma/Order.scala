package ru.org.codingteam.horta.plugins.karma

abstract class Order {
  def order:String
}
case class AscendingOrder(order:String = "ASC") extends Order
case class DescendingOrder(order:String = "DESC") extends Order
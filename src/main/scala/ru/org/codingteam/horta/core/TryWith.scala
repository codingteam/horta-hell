package ru.org.codingteam.horta.core

import scala.util.{Failure, Try}
import scala.util.control.NonFatal

object TryWith {
  def apply[C <: AutoCloseable, R](resource: => C)(f: C => Try[R]): Try[R] =
    Try(resource).flatMap { resourceInstance =>
      try {
        val returnValue = f(resourceInstance)
        Try(resourceInstance.close()).flatMap(_ => returnValue)
      } catch {
        case NonFatal(exceptionInFunction) =>
          try {
            resourceInstance.close()
            Failure(exceptionInFunction)
          } catch {
            case NonFatal(exceptionInClose) =>
              exceptionInFunction.addSuppressed(exceptionInFunction)
              Failure(exceptionInFunction)
          }
      }
    }
}

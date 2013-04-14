package ru.org.codingteam.horta.security

/**
 * Scope defines command visibility and data availability.
 */
abstract sealed class Scope

/**
 * Session scope starts when user enters chat or when he starts private message session.
 */
case object SessionScope extends Scope

/**
 * User scope is permanent and shared for any identified user.
 */
case object UserScope extends Scope

/**
 * Room scope is used for room.
 */
case object RoomScope extends Scope

/**
 * Global scope is... global. That's it.
 */
case object GlobalScope extends Scope


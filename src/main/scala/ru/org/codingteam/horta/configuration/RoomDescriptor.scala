package ru.org.codingteam.horta.configuration

import ru.org.codingteam.horta.localization.LocaleDefinition

case class RoomDescriptor(id: String, room: String, locale: LocaleDefinition, nickname: String, message: String)

package ru.demid.practicebot.repos

import org.springframework.data.repository.CrudRepository
import ru.demid.practicebot.model.Message


interface MessageRepo : CrudRepository<Message, Long> {
}
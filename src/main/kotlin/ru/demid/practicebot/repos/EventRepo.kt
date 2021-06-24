package ru.demid.practicebot.repos

import org.springframework.data.repository.CrudRepository
import ru.demid.practicebot.model.Event
import ru.demid.practicebot.model.User


interface EventRepo : CrudRepository<Event, Long> {
    fun findAllByUserOrderByDateDesc(user: User):List<Event>
}
package ru.demid.practicebot.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.demid.practicebot.model.Event
import ru.demid.practicebot.model.User
import ru.demid.practicebot.repos.EventRepo
import ru.demid.practicebot.repos.UserRepo

@Service
class EventService @Autowired constructor(val eventRepo: EventRepo) {
    fun save(event: Event) {
        eventRepo.save(event)
    }

    fun findAllByUserOrderByDateDesc(curUser: User): List<Event> {
        return eventRepo.findAllByUserOrderByDateDesc(curUser)
    }


}
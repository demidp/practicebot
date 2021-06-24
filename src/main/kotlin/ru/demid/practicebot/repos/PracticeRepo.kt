package ru.demid.practicebot.repos

import org.springframework.data.repository.CrudRepository
import ru.demid.practicebot.model.Group
import ru.demid.practicebot.model.Practice

interface PracticeRepo : CrudRepository<Practice, Int> {
    fun findAllByGroupOrderByDateAsc(group: Group): List<Practice>
}
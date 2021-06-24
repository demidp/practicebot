package ru.demid.practicebot.repos

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.SQLInsert
import org.springframework.data.repository.CrudRepository
import ru.demid.practicebot.model.Group
import ru.demid.practicebot.model.User
import javax.persistence.FetchType
import javax.persistence.Table

@Table(name = "groups")
interface GroupRepo : CrudRepository<Group, Int> {
    fun findByName(name: String): List<Group>
}
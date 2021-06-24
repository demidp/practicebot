package ru.demid.practicebot.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import ru.demid.practicebot.model.Group
import ru.demid.practicebot.model.User
import javax.persistence.Table



@Table(name = "users")
interface UserRepo : CrudRepository<User, Int> {
    @Query("SELECT p FROM users p JOIN FETCH p.studentGroups WHERE p.id = (:id)")
    fun findByIdAndFetchRolesEagerly(@Param("id") id: Int): User
    @Query("SELECT p FROM users p JOIN FETCH p.teacherGroups WHERE p.id = (:id)")
    fun findAdminsGroups(@Param("id") id: Int): User
}
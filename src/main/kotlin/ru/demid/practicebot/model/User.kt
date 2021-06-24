package ru.demid.practicebot.model

import org.hibernate.annotations.WhereJoinTable
import javax.persistence.*

@Entity(name = "users")
data class User(
    @Id
    val id: Int,
    var surname: String,
    var name: String,
    var secondName: String? = null,
    var email: String = "",
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val events: MutableList<Event> = ArrayList(),
    @ManyToMany
    @JoinTable(
        name = "role",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "group_id")]
    )
    @WhereJoinTable(clause = "type='STUDENT'")
    val studentGroups: MutableList<Group> = ArrayList(),
    @ManyToMany
    @JoinTable(
        name = "role",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "group_id")]
    )
    @WhereJoinTable(clause = "type='TEACHER'")
    val teacherGroups: MutableList<Group> = ArrayList()
) {
    constructor() : this(1, "", "")
    fun getFullName():String{
        return "$surname $name ${secondName?:""}"
    }
}
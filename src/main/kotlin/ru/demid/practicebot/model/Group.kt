package ru.demid.practicebot.model

import org.hibernate.annotations.SQLInsert
import org.hibernate.annotations.WhereJoinTable
import javax.persistence.*

@Entity(name = "groups")
data class Group(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int,
    var subjectName: String,
    var name: String,
    @WhereJoinTable(clause = "type='STUDENT'")
    @ManyToMany(
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER
    )
    @JoinTable(
        name = "role",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    @SQLInsert(sql = "insert into role (group_id, user_id, type) values (?, ?, 'STUDENT')")
    val students: MutableList<User> = ArrayList(),
    @WhereJoinTable(clause = "type='TEACHER'")
    @ManyToMany(
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER
    )
    @JoinTable(
        name = "role",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    @SQLInsert(sql = "insert into role (group_id, user_id, type) values (?, ?, 'TEACHER')")
    val teachers: MutableList<User> = ArrayList(),
    @OneToMany
    @JoinColumn
    val practices: MutableList<Practice> = ArrayList(),
    var spreadsheetId: String = ""
) {
    constructor() : this(Int.MAX_VALUE, "", "")
    fun getFullName():String{
        return "$name $subjectName"
    }
    fun getLink():String{
        return "\"https://docs.google.com/spreadsheets/d/$spreadsheetId/edit\""
    }
}
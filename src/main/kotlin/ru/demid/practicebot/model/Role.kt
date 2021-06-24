package ru.demid.practicebot.model

import java.io.Serializable
import javax.persistence.*
import kotlin.random.Random


@Entity(name = "role")
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    @Column(name = "user_id")
    val userId: Int = 0,

    @Column(name = "group_id")
    val groupId: Int = 0,
    @Enumerated(EnumType.STRING)
    val type: RoleType = RoleType.STUDENT
) : Serializable {
    constructor() : this(0, 0)
}

enum class RoleType {
    STUDENT, TEACHER, STUDENT_JOINING
}

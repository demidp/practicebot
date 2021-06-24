package ru.demid.practicebot.model

import org.hibernate.annotations.CreationTimestamp
import java.util.*
import javax.persistence.*

@Entity
data class Event(
    @Id
    @GeneratedValue
    val id: Int,
    @ManyToOne
    @JoinColumn(name = "users_id")
    val user: User,
    val type: EventType,
    @CreationTimestamp
    val date: Date = Date(0),
    val extra: String = ""
) {
    constructor() : this(0, User(), EventType.NOTHING, Date(0))
}

enum class EventType {
    NOTHING, REGISTRATION, GROUP_CREATION, GROUP_JOIN, GROUP_JOIN_CONFIRMATION, CREATE_SINGLE_PRACTICE, CREATE_MULTIPLE_PRACTICE, IN_PRACTICE_MENU, EMAIL
}
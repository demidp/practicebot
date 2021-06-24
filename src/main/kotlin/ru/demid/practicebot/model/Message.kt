package ru.demid.practicebot.model

import javax.persistence.Entity
import javax.persistence.Id
import kotlin.random.Random

@Entity
data class Message(
    @Id
    val id: Int,
    val name:String
) {
    constructor() : this(Random.nextInt(),"1")
}
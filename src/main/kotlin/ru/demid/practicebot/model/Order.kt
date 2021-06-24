package ru.demid.practicebot.model

import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "orders")
data class Order(
    @Id
    @GeneratedValue
    val id: Int,
    @LazyCollection(LazyCollectionOption.FALSE)
    @ManyToOne
    @JoinColumn(name = "practice_id")
    val practice: Practice,
    @ManyToOne
    @JoinColumn(name = "users_id")
    val student: User,
    val taskId: String,
    var isChosen: OrderStatus,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val sheetName : String = ""
) {
    constructor() : this(0, Practice(), User(), "", OrderStatus.NEW)
}

enum class OrderStatus {
    NEW,CHOSEN, NOT_CHOSEN, FAILED, SUCCESSFUL;

    override fun toString(): String {
        return when(this){
            CHOSEN -> "C"
            NOT_CHOSEN -> "N"
            FAILED -> "F"
            SUCCESSFUL -> "S"
            NEW -> "V"
        }
    }
    companion object{
        fun fromString(s:String):OrderStatus{
            return when(s) {
                "C" -> CHOSEN
                "N" ->NOT_CHOSEN
                "F" -> FAILED
                "S" -> SUCCESSFUL
                else -> NEW
            }
        }
    }
}
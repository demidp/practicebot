package ru.demid.practicebot.model

import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.*
import javax.persistence.*
import kotlin.collections.ArrayList

@Entity
data class Practice(
    @Id
    @GeneratedValue
    val id: Int,
    val date: LocalDateTime,
    @ManyToOne
    val group: Group,
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL])
    val orders: MutableList<Order> = ArrayList(),
    var status:Status = Status.OPEN
) {
    constructor() : this(0, LocalDateTime.now(), Group())
    fun getName() : String{
        return date.dayOfMonth.toString() + " " +date.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"))
    }
    fun getLink() : String{
        return "\"https://docs.google.com/spreadsheets/d/${group.spreadsheetId}/edit#gid=$id\""
    }
    fun getTime(): String{
        return "${date.hour}:${date.minute}"
    }
    enum class Status {OPEN,CLOSE}
}
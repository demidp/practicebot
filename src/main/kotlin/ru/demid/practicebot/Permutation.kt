package ru.demid.practicebot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.apache.catalina.valves.StuckThreadDetectionValve
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.demid.practicebot.model.Order
import ru.demid.practicebot.model.OrderStatus
import ru.demid.practicebot.model.User
import ru.demid.practicebot.service.UserGroupService

@Component
class Permutation @Autowired constructor(
    val service: UserGroupService,
    val bot: TelegramBot,
    val practiceManagement: PracticeManagement
) {
    class Task(val taskId: String) {
        var chosenStudent: Student? = null
        override fun hashCode(): Int {
            return taskId.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Task

            if (taskId != other.taskId) return false

            return true
        }
    }

    class Student(val fullName: String, val id: Int) {
        val tasks = ArrayList<Task>()
        override fun hashCode(): Int {
            return (fullName.hashCode() + id).hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Student

            if (fullName != other.fullName) return false
            if (id != other.id) return false

            return true
        }
    }

    fun tryKuhn(student: Student): Boolean {
        student.tasks.forEach {
            if (it.chosenStudent == null || (it.chosenStudent!!!=student && (tryKuhn(it.chosenStudent!!)))) {
                it.chosenStudent = student
                return true
            }
        }
        return false
    }

    // Добавить выбор способа
    fun onPermutationBuilder(curUser: User, callbackData: String) {
        val practice = service.findPracticeById(callbackData.substring(17).toInt())
        val students: HashMap<Int, Student> = HashMap()
        val orders:HashMap<String, Order> = HashMap()
        val filteredOrders = practice.orders.filter { it.isChosen == OrderStatus.NEW }
        val tasks: HashMap<String, Task> = HashMap()
        filteredOrders.forEach { order ->
            tasks.computeIfAbsent(order.taskId) { Task(it) }
        }
        filteredOrders.forEach { order ->
            students.computeIfAbsent(order.student.id) { Student(order.student.getFullName(), it) }
            orders["${order.taskId}_${order.student.id}"] = order
            students[order.student.id]!!.tasks.add(tasks[order.taskId]!!)
        }

        var hasChanged = true
        while (hasChanged){
            hasChanged = false
            students.forEach { (_, u) -> hasChanged = hasChanged || tryKuhn(u)}
        }
        tasks.forEach{
            val order = orders["${it.value.taskId}_${it.value.chosenStudent!!.id}"]!!
            order.isChosen = OrderStatus.CHOSEN
            service.saveOrder(order)
        }
        practice.orders.filter { it.isChosen == OrderStatus.NEW }.forEach {
            it.isChosen = OrderStatus.NOT_CHOSEN
            service.saveOrder(it)
        }
        practiceManagement.refreshSheet(practice)
        bot.execute(SendMessage(curUser.id,"Распределение построено!\n" +
                "Можете посмотреть <a href=${practice.getLink()}>таблицу</a>").parseMode(ParseMode.HTML))
    }

}
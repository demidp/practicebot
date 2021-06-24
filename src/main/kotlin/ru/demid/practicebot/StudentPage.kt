package ru.demid.practicebot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.demid.practicebot.model.*
import ru.demid.practicebot.service.EventService
import ru.demid.practicebot.service.UserGroupService
import java.time.LocalDateTime

@Component
class StudentPage @Autowired constructor(
    val bot: TelegramBot,
    val service: UserGroupService,
    val eventRepo: EventService
) {


    fun onOrderClick(curUser: User) {
        val studentsGroups = service.getStudentGroups(curUser)
        if (studentsGroups.isEmpty()) {
            bot.execute(SendMessage(curUser.id, "Вы еще не состоите не в одной группе, сначала вступите в группу"))
            return
        }
        if (studentsGroups.size == 1) {
            showInformationForPractice(curUser, studentsGroups[0])
        } else {
            val keyboard = InlineKeyboardMarkup()
            studentsGroups.forEach {
                keyboard.addRow(
                    InlineKeyboardButton(it.getFullName())
                        .callbackData("group_practice_${it.id}")
                )
            }
            bot.execute(
                SendMessage(curUser.id, "Вы состоите в нескольких группах выберите нужную:").replyMarkup(
                    keyboard
                )
            )
        }
    }

    fun onGroupClick(curUser: User, callbackData: String) {
        showInformationForPractice(curUser, service.findGroupById(callbackData.substring(15).toInt()).get())
    }

    fun showInformationForPractice(curUser: User, group: Group) {
        val practices = service.getGroupsPractices(curUser, group)
        var currentPractice: Practice? = null
        practices.forEach {
            if (it.date.isAfter(LocalDateTime.now())) {
                currentPractice = it; return@forEach
            }
        }
        if (currentPractice == null) {
            bot.execute(SendMessage(curUser.id, "Для данной группы нет предстоящих практик"))
            return
        }
        displayPractice(curUser, group, currentPractice!!)
    }

    fun choosePractice(curUser: User, callbackData: String) {
        val practice = service.findPracticeById(callbackData.substring(19).toInt())
        displayPractice(curUser, practice.group, practice)
    }

    fun displayPractice(curUser: User, group: Group, practice: Practice) {
        val studentsOrders = practice.orders.filter { it.student.id == curUser.id }
        val practices = service.getGroupsPractices(curUser, group)
        var text = "Показана информация для практики по" +
                " ${group.subjectName}\nКоторая состоится ${practice.getName()} в ${practice.getTime()}\n"
        if (practice.status == Practice.Status.OPEN) {
            text += "Заявки принимаются\n"
        } else {
            text += "Приём заявок приостановлен\n"
        }
        text += if (studentsOrders.isEmpty()) {
            "Вы ещё не заявляли задачи на эту практику\n"
        } else {
            "Ваши задачи: " + studentsOrders.map { it.taskId }.sortedBy { taskIdToLong(it) }
                .joinToString(separator = ", ")
        }
        text += "\nДля добавления задач используйте /add для удаления  /delete\n" +
                "Задачи вводятся через пробел, можно использовать интервалы 1.1-1.10, для " +
                "разделения блока задач и конкретной задачи используйте точку"
        eventRepo.save(Event(Int.MAX_VALUE, curUser, EventType.IN_PRACTICE_MENU, extra = practice.id.toString()))
        val keyboard = Keyboards.generateKeyboardPractice(
            "choose_practice_id_",
            practices, practice
        )
        if (keyboard == null) {
            bot.execute(SendMessage(curUser.id, text))
        } else {
            bot.execute(SendMessage(curUser.id, text).replyMarkup(InlineKeyboardMarkup(keyboard)))
        }
    }

    fun addDeleteTask(curUser: User, upd: Update, type: String, text: String) {
        val event = eventRepo.findAllByUserOrderByDateDesc(curUser)[0]
        if (event.type != EventType.IN_PRACTICE_MENU) {
            bot.execute(SendMessage(curUser.id, "Удалять или заявлять задачи можно только из меню практики"))
            eventRepo.save(Event(Int.MAX_VALUE, curUser, EventType.NOTHING))
            return
        }
        val tasks = parseTasks(text)
        val practice = service.findPracticeById(event.extra.toInt())
        if (practice.status == Practice.Status.CLOSE) {
            bot.execute(SendMessage(curUser.id, "Приём заявок приостановлен"))
            return
        }
        when (type) {
            "/add" -> addTasks(curUser, practice, tasks)
            "/delete" -> deleteTasks(curUser, practice, tasks)
        }
    }

    fun addTasks(curUser: User, practice: Practice, tasks: List<String>) {
        tasks.forEach {
            if (it.isNotBlank()) {
                service.saveOrder(Order(Int.MAX_VALUE, practice, curUser, it, OrderStatus.NEW))
            }
        }
        showInformationForPractice(curUser, practice.group)
    }

    fun deleteTasks(curUser: User, practice: Practice, tasks: List<String>) {
        val tasksSet = HashSet(tasks)
        practice.orders.forEach {
            if (it.student.id == curUser.id && tasksSet.contains(it.taskId)) {
                service.deleteOrder(it)
            }
        }
        showInformationForPractice(curUser, practice.group)
    }

    fun parseTasks(rawTasks: String): List<String> {
        val tasks = ArrayList<String>()
        val listRaw = rawTasks.trim().split(' ')
        listRaw.forEach {
            if (it.contains('-')) {
                val taskRange = it.split('-')
                if (taskRange[0].contains('.')) {
                    val part = taskRange[0].split('.')[0] + "."
                    for (i in taskRange[0].split('.')[1].toInt()..taskRange[1].split('.')[1].toInt()) {
                        tasks.add(part + i.toString())
                    }
                } else {
                    for (i in taskRange[0].toInt()..taskRange[1].toInt()) {
                        tasks.add(i.toString())
                    }
                }
            } else {
                tasks.add(it)
            }
        }
        return tasks
    }

    companion object {
        private val notNum = Regex("[^0-9]")
        private val num = Regex("[0-9]")
        fun taskIdToLong(taskId: String): Long {
            val tasks = taskId.split('.').reversed()
            var res = 0L
            var pos = 1
            tasks.forEach {
                if (num.containsMatchIn(it)) {
                    res += pos * (notNum.replace(it, "")).toInt()
                    pos *= 100
                }
            }
            return res
        }
    }
}
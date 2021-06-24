package ru.demid.practicebot

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.demid.practicebot.model.Order
import ru.demid.practicebot.model.OrderStatus
import ru.demid.practicebot.model.Practice
import ru.demid.practicebot.model.User
import ru.demid.practicebot.service.UserGroupService
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Component
class PracticeManagement @Autowired constructor(
    val service: UserGroupService,
    val sheetsService: Sheets,
    val bot:TelegramBot
) {
    fun onRefreshSheet(callback: String) {
        val practiceId = callback.substring(14).toInt()
        val practice = service.findPracticeById(practiceId)
        refreshSheet(practice, true)
    }

    fun refreshSheet(practice: Practice, sync: Boolean = false) {
        val tasksNames: SortedSet<String> = TreeSet()
        practice.orders.forEach { tasksNames.add(it.taskId) }
        val tasksSorted = tasksNames.sortedBy { StudentPage.taskIdToLong(it) }
        val studentsNamesSorted: Array<String> =
            (TreeSet(practice.group.students.map { it.getFullName() }).toTypedArray())
        val sheets: MutableList<MutableList<String>> = ArrayList()
        sheets.add(ArrayList())
        sheets[0].add("Фамилия Имя Отчество")
        val mapIdToOrder = HashMap<String, Order>()
        tasksSorted.forEach { sheets[0].add(it) }
        studentsNamesSorted.forEach {
            sheets.add(ArrayList<String>())
            sheets.last().add(it)
            sheets.last().addAll(Array(tasksSorted.size) { "" })
        }

        practice.orders.forEach {
            val i = studentsNamesSorted.binarySearch(it.student.getFullName()) + 1
            val j = tasksSorted.binarySearchBy(key = StudentPage.taskIdToLong(it.taskId),
                selector = {taskID -> StudentPage.taskIdToLong(taskID)}) + 1
            sheets[i][j] = it.isChosen.toString()
            mapIdToOrder["${i}_$j"] = it
        }

        val range = "${practice.getName()}!A8:${convertIntToString(sheets[0].size)}${sheets.size+7}"
        if(sync) {
            val sheetsValue = sheetsService.spreadsheets().values().get(practice.group.spreadsheetId, range).execute()
            val values = sheetsValue.getValues()
            if(values != null && values.size == sheets.size) {
                for (i in 1 until values.size) {
                    for (j in 1 until values[i].size) {
                        if (values[i][j] as String != sheets[i][j]) {
                            sheets[i][j] = values[i][j] as String
                            val ord = mapIdToOrder["${i}_${j}"]!!
                            ord.isChosen = OrderStatus.fromString(values[i][j] as String)
                            service.saveOrder(ord)
                        }
                    }
                }
            }
        }
        sheetsService.spreadsheets().values()
            .update(practice.group.spreadsheetId, range, ValueRange().setValues(sheets as List<MutableList<Any>>))
            .setValueInputOption("RAW")
            .execute()
    }
    fun onStop(curUser: User,callback: String){
        val practice = service.findPracticeById(callback.substring(19).toInt())
        practice.status = Practice.Status.CLOSE
        service.savePractice(practice)
        bot.execute(SendMessage(curUser.id,"Приём заявок остановлен"))
    }
    fun onStart(curUser: User,callback: String){
        val practice = service.findPracticeById(callback.substring(22).toInt())
        practice.status = Practice.Status.OPEN
        service.savePractice(practice)
        bot.execute(SendMessage(curUser.id,"Приём заявок возобновлен"))
    }
    @OptIn(ExperimentalStdlibApi::class)
    fun convertIntToString(int: Int): String {
        return Char('A'.toInt() + int - 1).toString()
    }

}
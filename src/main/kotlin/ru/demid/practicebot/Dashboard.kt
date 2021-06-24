package ru.demid.practicebot

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.stereotype.Component
import ru.demid.practicebot.model.Event
import ru.demid.practicebot.model.EventType
import ru.demid.practicebot.model.Practice
import ru.demid.practicebot.model.User
import ru.demid.practicebot.service.EventService
import ru.demid.practicebot.service.UserGroupService
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.HashSet

@Component
class Dashboard(
    val service: UserGroupService,
    val eventService: EventService,
    val bot: TelegramBot,
    val sheets: Sheets
) {
    fun onManageGroups(curUser: User) {
        val keyboard = InlineKeyboardMarkup()
        val groups = service.getTeacherGroups(curUser)
        if (groups.isEmpty()) {
            bot.execute(SendMessage(curUser.id, "У вас нет групп для управления"))
            return
        }
        groups.forEach {
            keyboard.addRow(InlineKeyboardButton("${it.name} ${it.subjectName}").callbackData("choose_teacher_${it.id}"))
        }
        bot.execute(SendMessage(curUser.id, "Выберите группу, для управления").replyMarkup(keyboard))
    }


    fun onChoseStudent(curUser: User, callbackData: String?) {

    }

    // Todo: добавить последнюю практику в список
    fun onChooseTeacher(curUser: User, callbackData: String) {
        val group = service.findGroupById(callbackData.substring(15, callbackData.length).toInt()).get()
        val groupId = group.id
        val keyboard = InlineKeyboardMarkup()
            .addRow(InlineKeyboardButton("Выбрать практику").callbackData("choose_practice_${groupId}"))
            .addRow(InlineKeyboardButton("Создать практику").callbackData("create_practice_${groupId}"))
            .addRow(
                InlineKeyboardButton("Заявки на вступление").callbackData("d"),
                InlineKeyboardButton(("Список участников")).callbackData("f")
            )
        bot.execute(
            SendMessage(
                curUser.id, "Управление группой - ${group.getFullName()}\n" +
                        "Табличка: <a href=${group.getLink()}>link</a>"
            ).parseMode(ParseMode.HTML).replyMarkup(keyboard)
        )
    }

    fun createSinglePractice(curUser: User, callbackData: String) {
        bot.execute(
            SendMessage(
                curUser.id, "Напишите данные для практике: \n" +
                        "В следующем формате: 17.02 11-40"
            )
        )
        eventService.save(
            Event(
                Int.MAX_VALUE,
                curUser,
                EventType.CREATE_SINGLE_PRACTICE,
                extra = callbackData.substring(16)
            )
        )
    }

    fun onGetInfoSinglePractice(curUser: User, text: String) {

    }

    fun createMultiplePractices(curUser: User, callbackData: String) {
        bot.execute(
            SendMessage(
                curUser.id, "Напишите день недели, время и кол-во практик\n" +
                        "Например: \"четверг 11-40 10\" создаст практику 11-40 на ближайшие 10 четвергов"
            )
        )
        eventService.save(Event(Int.MAX_VALUE, curUser, EventType.CREATE_MULTIPLE_PRACTICE))
    }

    fun onGetInfoMultiplePractices(curUser: User, text: String) {

    }

    // Вернуть практики для текущей группы
    fun onChoosePractice(curUser: User, callbackData: String) {
        val groupId = callbackData.substring(16).toInt()
        val group = service.findGroupById(groupId)
        val practices = service.getGroupsPractices(curUser, group.get())

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
        onClickPractice(curUser, "click_on_practice_${currentPractice!!.id}")
    }

    // Todo: handle an exception
    fun onCreateSinglePractice(curUser: User, upd: Update, event: Event) {
        val group = service.findGroupById(event.extra.toInt()).get()
        val rawText = upd.message().text()
        val dt: LocalDateTime
        try {
            dt = LocalDateTime.parse("21.$rawText", DateTimeFormatter.ofPattern("yy.dd.MM HH-mm"))
        } catch (e: DateTimeException) {
            bot.execute(
                SendMessage(
                    curUser.id, "Не смог распарсить дату и время, попробуйте ещё раз\n" +
                            "Например: 17.04 11-40"
                )
            )
            return
        }
        eventService.save(Event(Int.MAX_VALUE, curUser, EventType.NOTHING))

        var practice = Practice(Int.MAX_VALUE, dt, group)
        practice = service.savePractice(practice)
        sheets.spreadsheets().batchUpdate(
            group.spreadsheetId, BatchUpdateSpreadsheetRequest()
                .setRequests(
                    listOf(
                        Request().setAddSheet(
                            AddSheetRequest().setProperties(
                                SheetProperties()
                                    .setTitle(practice.getName())
                                    .setSheetId(practice.id)
                            )
                        )
                    )
                )
        ).execute()
        val range = "${practice.getName()}!A1:A6"
        sheets.spreadsheets().values()
            .update(
                practice.group.spreadsheetId, range, ValueRange().setValues(
                    arrayListOf(
                        arrayListOf("Легенда"),
                        arrayListOf("V - Подана заявка"),
                        arrayListOf("С - Выбран распределением"),
                        arrayListOf("N - Не выбран распределением"),
                        arrayListOf("F - Проиграл при ответе"),
                        arrayListOf("S - Успешно решил")
                    ) as List<MutableList<Any>>
                )
            )
            .setValueInputOption("RAW")
            .execute()
    }

    // Show practice to a teacher
    fun onClickPractice(curUser: User, callbackData: String) {
        val practiceId = callbackData.substring(18).toInt()
        val practice = service.findPracticeById(practiceId)
        val group = service.findGroupById(practice.group.id).get()
        val studentsCount = group.students.size
        val students = HashSet<Int>()
        practice.orders.forEach { students.add(it.student.id) }
        val buttons = Keyboards.generateKeyboardPractice(
            "click_on_practice_",
            service.getGroupsPractices(curUser, group),
            practice
        )

        val keyboard = InlineKeyboardMarkup().addRow(
            InlineKeyboardButton("Обновить табличку").callbackData("refresh_sheet_${practiceId}")
        ).addRow(InlineKeyboardButton("Построить распределение").callbackData("make_permutation_${practiceId}"))
        if (practice.status == Practice.Status.OPEN) {
            keyboard.addRow(InlineKeyboardButton("Остановить прием заявок").callbackData("stop_adding_orders_${practiceId}"))
        }else{
            keyboard.addRow(InlineKeyboardButton("Возобновить прием заявок").callbackData("restart_adding_orders_${practiceId}"))
        }
        if (buttons != null) {
            keyboard.addRow(*buttons)
        }
        bot.execute(
            SendMessage(
                curUser.id, "Практика ${group.getFullName()}\n" +
                        "Дата ${practice.getName()}\n" +
                        "Количество заявок: ${practice.orders.size} \n" +
                        "Доля студентов решивших хотя бы одну задачу ${students.size} / $studentsCount \n" +
                        "Табличка: <a href=\"https://docs.google.com/spreadsheets/d/${group.spreadsheetId}/edit#gid=${practice.id}\">link</a>\n" +
                        "Прим. обновить табличку, берет обновления из таблички, если задачи нет добавит ее, если изменился статус изменит его" +
                        " и обогатит табличку данными из бд"
            ).parseMode(ParseMode.HTML).replyMarkup(keyboard)
        )


    }

}
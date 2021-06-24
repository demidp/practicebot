package ru.demid.practicebot

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.EditMessageReplyMarkup
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.demid.practicebot.model.Event
import ru.demid.practicebot.model.EventType
import ru.demid.practicebot.model.Group
import ru.demid.practicebot.model.User
import ru.demid.practicebot.service.EventService
import ru.demid.practicebot.service.UserGroupService
import kotlin.random.Random

@Component
class JoinCreate @Autowired constructor(
    val bot: TelegramBot,
    val service: UserGroupService,
    val eventRepo: EventService,
    val sheets: Sheets,
    val drive: Drive
) {
    fun onCreate(user: User) {
        bot.execute(AnswerCallbackQuery("create"))
        bot.execute(
            SendMessage(
                user.id,
                "Введите номер группы и название группы\n" +
                        "например: M3138 Алгоритмы и структуры данных"
            )
        )
        eventRepo.save(Event(0, user, EventType.GROUP_CREATION))
    }

    fun onReceiveGroupName(user: User, upd: Update) {
        val fullName = upd.message().text().trim().split(" ", limit = 2)
        if (fullName.size < 2) {
            bot.execute(
                SendMessage(
                    user.id, "Вы неправильно ввели номер группы или название предмета. Попробуйте ещё раз\n" +
                            "например: M3138 Алгоритмы и структуры данных"
                )
            )
            return
        }
        var newGroup = Group(Random.nextInt(), fullName[1], fullName[0])

        val sheet = sheets.spreadsheets()
            .create(Spreadsheet().setProperties(SpreadsheetProperties().setTitle(newGroup.getFullName()))).execute()
        newGroup.spreadsheetId = sheet.spreadsheetId
        drive.permissions().create(
            newGroup.spreadsheetId, Permission().setRole("reader")
                .setType("anyone")
        ).execute()
        newGroup = service.saveGroup(newGroup)
        newGroup.teachers.add(user)
        service.saveGroup(newGroup)
        eventRepo.save(Event(0, user, EventType.NOTHING))
        var text = "Вы удачно создали группу:\n" +
                "Номер группы: ${newGroup.name}\n" +
                "Название группы: ${newGroup.subjectName}\n" +
                "Хотите создать практики?\n" +
                "<a href=${newGroup.getLink()}>Ссылка на таблицу</a>"
        if (user.email.isEmpty()) {
            text += "Введите email, чтоб редактировать таблицу - /set-email\n"
        } else {
            drive.permissions().create(
                newGroup.spreadsheetId, Permission().setRole("writer")
                    .setEmailAddress(user.email)
                    .setType("user")
            ).execute()
        }
        bot.execute(
            SendMessage(
                user.id, text
            ).parseMode(ParseMode.HTML).replyMarkup(
                InlineKeyboardMarkup(InlineKeyboardButton("Создать практику").callbackData("create_practice_${newGroup.id}"))
//                    .addRow(InlineKeyboardButton("Создать серию практик").callbackData("create_practices_${newGroup.id}"))
            )
        )
    }

    fun onJoin(user: User) {
        bot.execute(
            SendMessage(
                user.id,
                "Напишите вашу учебную группу\n" +
                        "Например: M3138"
            )
        )
        eventRepo.save(Event(0, user, EventType.GROUP_JOIN))
    }

    fun onJoinWithName(user: User, upd: Update) {
        val groupName = upd.message().text().trim()
        val groupsFind = service.findGroupByName(groupName)
        val studentsGroup = HashSet(service.getStudentGroups(user).map { it.id })
        val groups = groupsFind.filter { !studentsGroup.contains(it.id) }
        if (groups.isEmpty()) {
            bot.execute(
                SendMessage(
                    user.id,
                    "Не нашёл такой группы\uD83D\uDE2A\nВозможно вы уже вступили в нее, попробуйте ещё раз"
                )
            )
            return
        }
        //val s = groups.stream().map { it.subjectName }.toList().joinToString { it + "\n" }
        val keyboard = InlineKeyboardMarkup()
        groups.stream().forEach {
            keyboard.addRow(
                InlineKeyboardButton(it.subjectName)
                    .callbackData("join_" + it.id)
            )
        }
        bot.execute(SendMessage(user.id, "Выберите группу, в которую хотите вступить").replyMarkup(keyboard))
        eventRepo.save(Event(0, user, EventType.NOTHING))
    }

    fun onJoinConfirmation(user: User, callbackData: String) {
        val group = service.findGroupById((callbackData.substring(5, callbackData.length)).toInt()).get()
        val students = group.students
        val teachers = group.teachers
        students.add(user)
        bot.execute(SendMessage(user.id, "Вы подали заявку на вступление в группу - ${group.subjectName}"))
        teachers.forEach { teacher: User ->
            bot.execute(
                SendMessage(
                    teacher.id, "Новая заявка на вступление в группу: ${group.name} ${group.subjectName} \n" +
                            "Хочет вступить <a href=\"tg://user?id=${user.id}\">${user.surname} ${user.name} </a>"
                ).parseMode(ParseMode.HTML)
                    .replyMarkup(
                        InlineKeyboardMarkup(
                            InlineKeyboardButton("Принять ✔️").callbackData("accept_" + user.id + "_" + group.id),
                            InlineKeyboardButton("Отклонить ❌").callbackData("reject_" + user.id + "_" + group.id),
                        )
                    )
            )
        }
    }

    /**
     *
     */
    fun acceptOrReject(user: User, callbackData: String, message: Message) {
        bot.execute(
            EditMessageReplyMarkup(
                message.chat().id(),
                message.messageId()
            ).replyMarkup(InlineKeyboardMarkup())
        )
        val parts = callbackData.split("_")
        val student = service.findUserById(parts[1].toInt()).get()
        val group = service.findGroupById(parts[2].toInt()).get()
        if (parts[0] == "accept") {
            service.getBar(parts[1].toInt(), parts[2].toInt())
            bot.execute(EditMessageText(user.id, message.messageId(), message.text() + "\nUPD: Принят в группу"))
            bot.execute(SendMessage(student.id, "Вас приняли в группу ${group.name} ${group.subjectName}"))
        } else {
            bot.execute(EditMessageText(user.id, message.messageId(), message.text() + "\nUPD: Не принят в группу"))
            bot.execute(SendMessage(student.id, "Вас не приняли в группу ${group.name} ${group.subjectName}"))
        }
    }
}
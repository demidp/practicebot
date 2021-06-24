package ru.demid.practicebot

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.demid.practicebot.model.*
import ru.demid.practicebot.service.EventService
import ru.demid.practicebot.service.UserGroupService
import java.util.*

@Component
class Registration @Autowired constructor(
    val bot: TelegramBot,
    val service: UserGroupService,
    val eventRepo: EventService,
    val drive: Drive
) {

    fun sendMsg(user: User, msg: String) {
        bot.execute(SendMessage(user.id, msg))
    }

    fun fullNameConfirmation(upd: Update, user: User) {
        val name = upd.message().text().split(" ")
        if (name.size < 2) {
            sendMsg(user, "Введите правильно фамилию и имя")
            return
        }
        user.surname = name[0]
        user.name = name[1]
        user.secondName = if (name.size == 3) name[2] else ""
        service.saveUser(user)
        eventRepo.save(Event(0, user, EventType.NOTHING))
        bot.execute(
            SendMessage(
                user.id, "Вы успешно зарегистрировались! \n " +
                        "Теперь можете создать группу, если вы преподаватель или вступить, если студент"
            )
                .replyMarkup(Keyboards.joinCreate)
        )
    }

    fun onStart(upd: Update) {
        val fromId = upd.message().from().id()
        val curUser = service.findUserById(fromId)
        curUser.ifPresentOrElse({
            bot.execute(
                SendMessage(it.id, "Выберите дальнейшее действие")
                    .replyMarkup(Keyboards.mainStudent)
            )
            //bot.execute(SendMessage(it.id, "Вы уже зарегистрированы"))
        }, {
            bot.execute(SendMessage(fromId, "Введите фамилию имя и отчество, при наличии"))
            val newUser =
                User(fromId, upd.message().from().firstName() ?: "", upd.message().from().lastName() ?: "")
            service.saveUser(newUser)
            eventRepo.save(Event(0, newUser, EventType.REGISTRATION, Date(upd.message().date().toLong())))
        })
        return
    }
    fun onSetEmail(user: User){
        bot.execute(SendMessage(user.id, "Введите email вашего аканта Google, на него будут выданы права редактирования " +
                "к таблицам групп, где вы являетесь преподавателем\n" +
                "Например: example@gmail.com"))
        eventRepo.save(Event(Int.MAX_VALUE, user, EventType.EMAIL))
    }
    fun onReceiveEmail(user:User, upd:Update){
        if(upd.message() == null || upd.message().text().isNullOrEmpty()){
            bot.execute(SendMessage(user.id,"email не может быть пустым"))
            return
        }
        val groups = service.getTeacherGroups(user)
        try {
            groups.forEach {
                if (it.spreadsheetId.isNotEmpty()) {
                    drive.permissions().create(
                        it.spreadsheetId, Permission().setRole("writer")
                            .setType("user").setEmailAddress(upd.message().text().trim())
                    ).execute()
                }
            }
        }catch (e:Exception){
            bot.execute(SendMessage(user.id, "${e}Что-то пошло не так, попробуйте позже"))
            eventRepo.save((Event(Int.MAX_VALUE,user,EventType.NOTHING)))
            return
        }
        user.email = upd.message().text().trim()
        service.saveUser(user)
        bot.execute(SendMessage(user.id,"Теперь вы можете редактировать таблицы"))
        eventRepo.save((Event(Int.MAX_VALUE,user,EventType.NOTHING)))
    }
}
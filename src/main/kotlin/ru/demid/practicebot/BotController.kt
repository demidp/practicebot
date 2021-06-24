package ru.demid.practicebot

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.pengrad.telegrambot.BotUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendSticker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import ru.demid.practicebot.model.*
import ru.demid.practicebot.service.EventService
import ru.demid.practicebot.service.UserGroupService



@JsonIgnoreProperties(ignoreUnknown = true)
@Controller
class BotController @Autowired constructor(
    val bot: TelegramBot,
    val reg: Registration,
    val joinCreate: JoinCreate,
    val service: UserGroupService,
    val eventRepo: EventService,
    val dashboard: Dashboard,
    val studentPage: StudentPage,
    val practiceManagement: PracticeManagement,
    val permutation: Permutation
) {

    val myId = 89249631

    @GetMapping("/hello")
    @ResponseBody()
    fun hello(): String {
        return "Hello"
    }


    init {
        // Use only for debug
        if (PracticeBotApplication.isDebug == true) {
            bot.setUpdatesListener(UpdatesListener { updates ->
                updates.forEach { workWithUpdate(it) }
                return@UpdatesListener updates.lastOrNull()!!.updateId()
            })
        }
    }

    fun workWithUpdate(upd: Update) {
        GlobalScope.launch {
            parseMessage(upd)
            println(upd.updateId())
        }
    }

    @PostMapping("/update")
    @ResponseBody
    fun update(@RequestBody rawJson: String): String {
        val upd = BotUtils.parseUpdate(rawJson)
        workWithUpdate(upd)
        return "ok"
    }

    fun parseMessage(upd: Update) {
        val fromId = if (upd.message() != null) {
            upd.message().from().id()
        } else {
            upd.callbackQuery().from().id()
        }
        val curUserOptional = service.findUserById(fromId)
        if (!curUserOptional.isPresent) {
            reg.onStart(upd)
            return
        }
        val curUser: User = curUserOptional.get()


        if (upd.callbackQuery() != null) {
            val callbackData = upd.callbackQuery().data()
            bot.execute(AnswerCallbackQuery(upd.callbackQuery().id()).text("Meh! Meh!"))
            when (callbackData) {
                "create" -> joinCreate.onCreate(curUser)
                "join" -> joinCreate.onJoin(curUser)
                "make order" -> studentPage.onOrderClick(curUser)
                "admin" -> dashboard.onManageGroups(curUser)
            }
            when {
                callbackData.startsWith("join_") -> joinCreate.onJoinConfirmation(curUser, callbackData)
                callbackData.startsWith("accept").or(callbackData.startsWith("reject"))
                -> joinCreate.acceptOrReject(curUser, callbackData, upd.callbackQuery().message())
                callbackData.startsWith("choose_student") -> dashboard.onChoseStudent(curUser, callbackData)
                callbackData.startsWith("choose_teacher") -> dashboard.onChooseTeacher(curUser, callbackData)
                callbackData.startsWith("choose_practice_id_") -> studentPage.choosePractice(curUser, callbackData)
                callbackData.startsWith("create_practice_") -> dashboard.createSinglePractice(curUser, callbackData)
                callbackData.startsWith("create_practices_") -> dashboard.createMultiplePractices(curUser, callbackData)
                callbackData.startsWith("choose_practice_") -> dashboard.onChoosePractice(curUser, callbackData)
                callbackData.startsWith("click_on_practice_") -> dashboard.onClickPractice(curUser, callbackData)
                callbackData.startsWith("group_practice_") -> studentPage.onGroupClick(curUser, callbackData)
                callbackData.startsWith("refresh_sheet_") -> practiceManagement.onRefreshSheet(callbackData)
                callbackData.startsWith("make_permutation_") -> permutation.onPermutationBuilder(curUser, callbackData)
                callbackData.startsWith("stop_adding_orders_") -> practiceManagement.onStop(curUser, callbackData)
                callbackData.startsWith("restart_adding_orders_") -> practiceManagement.onStart(curUser, callbackData)
            }
            return
        }
        if (upd.message().text() != null) {
            when (upd.message().text()) {
                "/start" -> {
                    reg.onStart(upd)
                    eventRepo.save(Event(Int.MAX_VALUE, curUser, EventType.NOTHING))
                    return
                }
                "/set-email" -> {
                    reg.onSetEmail(curUser)
                    return
                }
            }
        }
        // Check events
        val events = eventRepo.findAllByUserOrderByDateDesc(curUser)
        if (events.isNotEmpty()) {
            when (events[0].type) {
                EventType.REGISTRATION -> reg.fullNameConfirmation(upd, curUser)
                EventType.GROUP_CREATION -> joinCreate.onReceiveGroupName(curUser, upd)
                EventType.GROUP_JOIN -> joinCreate.onJoinWithName(curUser, upd)
                EventType.CREATE_SINGLE_PRACTICE -> dashboard.onCreateSinglePractice(curUser, upd, events[0])
                EventType.EMAIL -> reg.onReceiveEmail(curUser, upd)
                else -> {
                }
            }
        }
        if (upd.message().text() == null) {
            bot.execute(
                SendMessage(
                    curUser.id, "Не знаю, что вы там прислали, но вот стикер.\n" +
                            "Лучше нажмите /start"
                )
            )
            bot.execute(
                SendSticker(
                    curUser.id,
                    "CAACAgIAAxkBAAIFg2DTw65AINvBcyV5Rc0-0Q-zhklEAAJUAAOtZbwUJTaFSf0QNk4fBA"
                )
            )
            return
        }

        when {
            upd.message().text().startsWith("/add") ->
                studentPage.addDeleteTask(curUser, upd, "/add", upd.message().text().substring(4))
            upd.message().text().startsWith("/delete") ->
                studentPage.addDeleteTask(curUser, upd, "/delete", upd.message().text().substring(7))
        }
    }
}
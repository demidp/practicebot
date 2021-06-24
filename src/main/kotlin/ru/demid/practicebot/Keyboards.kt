package ru.demid.practicebot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import ru.demid.practicebot.model.Practice
import ru.demid.practicebot.model.User

class Keyboards {
    companion object {
        val joinCreate = InlineKeyboardMarkup(
            InlineKeyboardButton("Создать группу").callbackData("create"),
            InlineKeyboardButton("Вступить в группу").callbackData("join")
        )
        val mainStudent: InlineKeyboardMarkup = InlineKeyboardMarkup(
            InlineKeyboardButton("Заявить задачи").callbackData("make order"),
        )
            .addRow(InlineKeyboardButton("Управление группами").callbackData("admin"))
            .addRow(
                InlineKeyboardButton("Вступить в группу").callbackData("join"),
                InlineKeyboardButton("Создать группу").callbackData("create"),
            )

        fun generateKeyboardPractice(
            callback: String, practices: List<Practice>, practice: Practice
        ): Array<InlineKeyboardButton>? {
            var prev: InlineKeyboardButton? = null
            var next: InlineKeyboardButton? = null

            practices.forEachIndexed { index, pr ->
                if (practice.id == pr.id) {
                    if (index > 0) {
                        prev = InlineKeyboardButton("⬅️ Предыдущая")
                            .callbackData("$callback${practices[index - 1].id}")
                    }
                    if (index + 1 < practices.size) {
                        next = InlineKeyboardButton("Следующая ➡️")
                            .callbackData("$callback${practices[index + 1].id}")
                    }
                    return@forEachIndexed
                }
            }
            return if (prev != null && next != null) {
                arrayOf(prev!!, next!!)
            } else if (prev != null) {
                arrayOf(prev!!)
            } else if (next != null) {
                arrayOf(next!!)
            } else {
                null
            }
        }
    }
}
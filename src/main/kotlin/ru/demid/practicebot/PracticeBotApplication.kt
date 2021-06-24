package ru.demid.practicebot

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.pengrad.telegrambot.TelegramBot
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootApplication()//exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@EnableScheduling

class PracticeBotApplication {
    companion object {
        var isDebug: Boolean? = null
    }
}

fun main(args: Array<String>) {
    PracticeBotApplication.isDebug = true
    runApplication<PracticeBotApplication>(*args)
}

@Configuration
class JacksonConfiguration {

    @Bean
    fun tgBot(@Value("\${botToken}") botToken: String): TelegramBot {
        return TelegramBot(botToken)
    }
    @Bean
    fun spreadSheetService(): Sheets {
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
        val APPLICATION_NAME = "Practice bot"
        val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY, SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE)
        val credential = GoogleCredential.Builder()
            .setTransport(HTTP_TRANSPORT)
            .setJsonFactory(JSON_FACTORY)
            .setServiceAccountId("practicebot@practice-bot-317516.iam.gserviceaccount.com")
            .setServiceAccountPrivateKeyFromP12File(Files.newInputStream(Paths.get("src/main/resources/practice-bot-317516-ebc3093bd781.p12")))
            .setServiceAccountScopes(SCOPES)
            .build()
        return Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build()
    }
    @Bean
    fun driveService(): Drive {
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
        val APPLICATION_NAME = "Practice bot"
        val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY, SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE)
        val credential = GoogleCredential.Builder()
            .setTransport(HTTP_TRANSPORT)
            .setJsonFactory(JSON_FACTORY)
            .setServiceAccountId("practicebot@practice-bot-317516.iam.gserviceaccount.com")
            .setServiceAccountPrivateKeyFromP12File(Files.newInputStream(Paths.get("src/main/resources/practice-bot-317516-ebc3093bd781.p12")))
            .setServiceAccountScopes(SCOPES)
            .build()
        return Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

}
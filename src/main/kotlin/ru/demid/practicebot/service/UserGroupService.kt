package ru.demid.practicebot.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import ru.demid.practicebot.model.Group
import ru.demid.practicebot.model.Order
import ru.demid.practicebot.model.Practice
import ru.demid.practicebot.model.User
import ru.demid.practicebot.repos.GroupRepo
import ru.demid.practicebot.repos.OrderRepo
import ru.demid.practicebot.repos.PracticeRepo
import ru.demid.practicebot.repos.UserRepo
import java.util.*
import javax.transaction.Transactional

@Service
class UserGroupService @Autowired constructor(
    val userRepo: UserRepo,
    val groupRepo: GroupRepo,
    val practiceRepo: PracticeRepo,
    val orderRepo: OrderRepo
) {
    @Transactional
    fun getBar(user: Int, group: Int) {
        val user_ = userRepo.findById(user).get()
        val group_ = groupRepo.findById(group).get()
        group_.students.add(user_)
        groupRepo.save(group_)
    }

    fun saveUser(user: User): User {
        return userRepo.save(user)
    }

    fun findUserById(fromId: Int): Optional<User> {
        return userRepo.findById(fromId)
    }

    fun saveGroup(group: Group): Group {
        return groupRepo.save(group)
    }

    fun findGroupById(groupId: Int): Optional<Group> {
        return groupRepo.findById(groupId)
    }

    fun deleteUser(curUser: User) {
        return userRepo.delete(curUser)
    }

    fun findGroupByName(groupName: String): List<Group> {
        return groupRepo.findByName(groupName)
    }

    @Transactional
    fun getStudentGroups(curUser: User): List<Group> {
        return try {
            userRepo.findByIdAndFetchRolesEagerly(curUser.id).studentGroups
        } catch (e: EmptyResultDataAccessException) {
            emptyList()
        }
    }

    @Transactional
    fun getTeacherGroups(curUser: User): List<Group> {
        return try {
            userRepo.findAdminsGroups(curUser.id).teacherGroups
        } catch (e: EmptyResultDataAccessException) {
            emptyList()
        }
    }

    @Transactional
    fun getGroupsPractices(curUser: User, group: Group): List<Practice> {
        return practiceRepo.findAllByGroupOrderByDateAsc(group)
    }

    @Transactional
    fun savePractice(practice: Practice): Practice {
        return practiceRepo.save(practice)
    }

    @Transactional
    fun saveOrder(order: Order): Order {
        return orderRepo.save(order)
    }

    fun deleteOrder(order: Order) {
        orderRepo.deleteByIdCustom(order.id)
    }

    fun findPracticeById(practiceId: Int): Practice {
        return practiceRepo.findById(practiceId).get()
    }
}
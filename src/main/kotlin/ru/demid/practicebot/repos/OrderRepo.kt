package ru.demid.practicebot.repos

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import ru.demid.practicebot.model.Order
import javax.transaction.Transactional

interface OrderRepo : CrudRepository<Order, Int> {
    @Transactional
    @Modifying
    @Query(value = "delete from orders c where c.id = ?1")
    fun deleteByIdCustom(id: Int)
}
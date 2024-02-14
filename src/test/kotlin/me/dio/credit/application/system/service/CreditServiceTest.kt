package me.dio.credit.application.system.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import me.dio.credit.application.system.ennummeration.Status
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.service.impl.CreditService
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class CreditServiceTest {
    @MockK lateinit var creditRepository: CreditRepository
    @MockK lateinit var customerService: CustomerService
    @InjectMockKs @SpyK(recordPrivateCalls = true) lateinit var creditService: CreditService

    @Test
    fun `should create credit`() {
        //given
        val fakeCustomerId: Long = 1L
        val fakeCustomer: Customer = buildCustomer(id = fakeCustomerId)
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer)

        every { customerService.findById(fakeCustomerId) } returns fakeCustomer
        every { creditRepository.save(fakeCredit) } returns fakeCredit

        //when
        val actual: Credit = creditService.save(fakeCredit)

        //then
        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isSameAs(fakeCredit)
        verify(exactly = 1) { creditService["validDayFirstInstallment"](fakeCredit.dayFirstInstallment) }
        verify(exactly = 1) { customerService.findById(fakeCustomerId) }
        verify(exactly = 1) { creditRepository.save(fakeCredit) }
    }

    @Test
    fun `should find all by customer`() {
        //given
        val fakeCustomerId: Long = 1L
        val fakeCustomer: Customer = buildCustomer(id = fakeCustomerId)
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer)

        every { creditRepository.findAllByCustomer(fakeCustomerId) } returns mutableListOf(fakeCredit)

        //when
        val actuals: List<Credit> = creditService.findAllByCustomer(fakeCustomerId)

        //then
        Assertions.assertThat(actuals).isNotNull
        Assertions.assertThat(actuals).isNotEmpty
        Assertions.assertThat(actuals.get(0)).isSameAs(fakeCredit)
        verify(exactly = 1) { creditRepository.findAllByCustomer(fakeCustomerId) }
    }

    @Test
    fun `should find by credit code`() {
        //given
        val fakeCustomerId: Long = Random().nextLong()
        val fakeCreditCode: UUID = UUID.randomUUID()
        val fakeCustomer: Customer = buildCustomer(id = fakeCustomerId)
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer, creditCode = fakeCreditCode)

        every { creditRepository.findByCreditCode(fakeCreditCode) } returns fakeCredit

        //when
        val actual: Credit = creditService.findByCreditCode(fakeCustomerId, fakeCreditCode)

        //then
        Assertions.assertThat(actual).isNotNull
        Assertions.assertThat(actual).isSameAs(fakeCredit)
        Assertions.assertThat(actual.customer?.id).isEqualTo(fakeCustomerId)
        verify(exactly = 1) { creditRepository.findByCreditCode(fakeCreditCode) }
    }

    @Test
    fun `should throw BusinessException if creditcode not found`() {
        //given
        val fakeCustomerId: Long = Random().nextLong()
        val fakeCreditCode: UUID = UUID.randomUUID()

        every { creditRepository.findByCreditCode(fakeCreditCode) } returns null

        //when
        //then
        Assertions.assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { creditService.findByCreditCode(fakeCustomerId, fakeCreditCode) }
            .withMessage("Creditcode $fakeCreditCode not found")
    }

    @Test
    fun `should throw IllegalArgumentException if customerId isn't equals`() {
        //given
        val fakeCustomerId: Long = 1L
        val fakeCreditCode: UUID = UUID.randomUUID()
        val fakeCredit: Credit = buildCredit(customer = buildCustomer(id = 2L), creditCode = fakeCreditCode)

        every { creditRepository.findByCreditCode(fakeCreditCode) } returns fakeCredit

        //when
        //then
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { creditService.findByCreditCode(fakeCustomerId, fakeCreditCode) }
            .withMessage("Contact admin")
    }

    companion object {
        fun buildCredit(
            creditCode: UUID = UUID.randomUUID(),
            creditValue: BigDecimal = BigDecimal.valueOf(15000.00),
            dayFirstInstallment: LocalDate = LocalDate.now().plusDays(30),
            numberOfInstallments: Int = 12,
            status: Status = Status.IN_PROGRESS,
            customer: Customer = buildCustomer(),
            id: Long = 1L
        ) = Credit(
            creditCode = creditCode,
            creditValue = creditValue,
            dayFirstInstallment = dayFirstInstallment,
            numberOfInstallments = numberOfInstallments,
            status = status,
            customer = customer,
            id = id
        )

        fun buildCustomer(
            firstName: String = "Cami",
            lastName: String = "Cavalcante",
            cpf: String = "28475934625",
            email: String = "camila@gmail.com",
            password: String = "12345",
            zipCode: String = "12345",
            street: String = "Rua da Cami",
            income: BigDecimal = BigDecimal.valueOf(1000.0),
            id: Long = 1L
        ) = Customer(
            firstName = firstName,
            lastName = lastName,
            cpf = cpf,
            email = email,
            password = password,
            address = Address(
                zipCode = zipCode,
                street = street,
            ),
            income = income,
            id = id
        )
    }

}
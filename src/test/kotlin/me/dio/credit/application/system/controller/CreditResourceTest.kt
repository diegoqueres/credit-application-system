package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
    @Autowired
    private lateinit var creditRepository: CreditRepository
    @Autowired
    private lateinit var customerRepository: CustomerRepository
    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        const val URL: String = "/api/credits"
    }

    @BeforeEach
    fun setup() {
        customerRepository.deleteAll()
        creditRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        customerRepository.deleteAll()
        creditRepository.deleteAll()
    }

    @Test
    fun `should create a credit and return 201 status`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        val creditDto: CreditDto = builderCreditDto(customerId = customer.id!!)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value("7500.0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value("6"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value("camila@email.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value("1000.0"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not create credit with invalid day first installment and return status 400`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        val creditDto: CreditDto = builderCreditDto(
            customerId = customer.id!!, dayFirstOfInstallment = LocalDate.now().plusMonths(3).plusDays(1)
        )
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.exception")
                    .value("class me.dio.credit.application.system.exception.BusinessException")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not create credit with nonexistent customer and return status 400`() {
        //given
        val invalidCustomerId: Long = Random().nextLong()
        val creditDto: CreditDto = builderCreditDto(customerId = invalidCustomerId)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.exception")
                    .value("class me.dio.credit.application.system.exception.BusinessException")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should list credits from customer and return status 200`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())

        val credits: MutableList<Credit> = mutableListOf()
        credits.add(creditRepository.save(builderCreditDto(customerId = customer.id!!, creditValue = BigDecimal.valueOf(3000.0)).toEntity()))
        credits.add(creditRepository.save(builderCreditDto(customerId = customer.id!!, creditValue = BigDecimal.valueOf(5000.0)).toEntity()))
        credits.add(creditRepository.save(
            builderCreditDto(customerId = customer.id!!, creditValue = BigDecimal.valueOf(23200.0), numberOfInstallments = 12).toEntity())
        )

        //when
        //then
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("$URL?customerId=${customer.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        credits.forEachIndexed { index, it ->
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$[$index].creditCode").value(it.creditCode.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[$index].creditValue").value(it.creditValue))
                .andExpect(MockMvcResultMatchers.jsonPath("$[$index].numberOfInstallments").value(it.numberOfInstallments))
        }

        resultActions.andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return empty list when not find credits and return status 200`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())

        //when
        //then
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("$URL?customerId=${customer.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[*]").isEmpty())
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return credit by credit code and return status 200`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        val credit: Credit = creditRepository.save(builderCreditDto(customerId = customer.id!!).toEntity())

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.get("$URL/${credit.creditCode}?customerId=${customer.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").value(credit.creditCode.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value("7500.0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value("6"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value("camila@email.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value("1000.0"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return status 400 when not find credit`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        val invalidCreditCode: UUID = UUID.randomUUID()

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.get("$URL/${invalidCreditCode}?customerId=${customer.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.exception")
                    .value("class me.dio.credit.application.system.exception.BusinessException")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return status 500 when requester customer is not equal to credit customer`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        val credit: Credit = creditRepository.save(builderCreditDto(customerId = customer.id!!).toEntity())

        val anotherCustomer: Customer = customerRepository.save(builderCustomerDto(
            firstName = "Mazaac", lastName = "Waroy", email = "waroy19348@oprevolt.com", cpf = "22928122079").toEntity())

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.get("$URL/${credit.creditCode}?customerId=${anotherCustomer.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("INTERNAL SERVER ERROR! Contact admin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.exception").value("class java.lang.IllegalArgumentException")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    private fun builderCreditDto(
        creditValue: BigDecimal = BigDecimal.valueOf(7500.0),
        dayFirstOfInstallment: LocalDate = LocalDate.now().plusDays(30),
        numberOfInstallments: Int = 6,
        customerId: Long = 1L
    ) = CreditDto(
        creditValue = creditValue,
        dayFirstOfInstallment = dayFirstOfInstallment,
        numberOfInstallments = numberOfInstallments,
        customerId = customerId
    )

    private fun builderCustomerDto(
        firstName: String = "Cami",
        lastName: String = "Cavalcante",
        cpf: String = "28475934625",
        email: String = "camila@email.com",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        password: String = "1234",
        zipCode: String = "000000",
        street: String = "Rua da Cami, 123",
    ) = CustomerDto(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        email = email,
        income = income,
        password = password,
        zipCode = zipCode,
        street = street
    )

}
package me.dio.credit.application.system.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.dto.request.CustomerUpdateDto
import me.dio.credit.application.system.dto.response.CustomerView
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.service.impl.CustomerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer resource")
class CustomerResource(
    private val customerService: CustomerService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun saveCustomer(@RequestBody @Valid customerDTO: CustomerDto): ResponseEntity<CustomerView> {
        val savedCustomer : Customer = this.customerService.save(customerDTO.toEntity())
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CustomerView(savedCustomer))
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long) : ResponseEntity<CustomerView> {
        val customer : Customer = this.customerService.findById(id)
        return ResponseEntity.status(HttpStatus.OK)
            .body(CustomerView(customer))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCustomer(@PathVariable id: Long) = this.customerService.delete(id)

    @PatchMapping
    fun updateCustomer(@RequestParam(value = "customerId") id: Long,
                       @RequestBody @Valid customerUpdateDto: CustomerUpdateDto
    ) : ResponseEntity<CustomerView> {
        val customer : Customer = this.customerService.findById(id)
        val customerToUpdate : Customer = customerUpdateDto.toEntity(customer)
        val customerUpdated : Customer = this.customerService.save(customerToUpdate)
        return ResponseEntity.status(HttpStatus.OK)
            .body(CustomerView(customerUpdated))
    }

}
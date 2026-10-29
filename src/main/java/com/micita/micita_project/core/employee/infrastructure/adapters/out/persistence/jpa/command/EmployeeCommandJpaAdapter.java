package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.command;

import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers.EmployeeBDMapper;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;

import org.springframework.stereotype.Component;

@Component
public class EmployeeCommandJpaAdapter implements EmployeeCommandRepositoryPort {

	private final EmployeeJpaRepository employeeJpaRepository;

	private final EmployeeBDMapper employeeBDMapper;

	public EmployeeCommandJpaAdapter(EmployeeJpaRepository employeeJpaRepository, EmployeeBDMapper employeeBDMapper) {
		this.employeeJpaRepository = employeeJpaRepository;
		this.employeeBDMapper = employeeBDMapper;
	}

	@Override
	public Employee save(Employee employee) {
		return employeeBDMapper.toDomain(employeeJpaRepository.save(employeeBDMapper.toEntity(employee)));
	}

}

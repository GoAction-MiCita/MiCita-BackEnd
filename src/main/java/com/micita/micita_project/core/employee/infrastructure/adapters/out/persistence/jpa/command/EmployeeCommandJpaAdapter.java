package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.command;

import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers.EmployeeBDMapper;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeCommandJpaAdapter implements EmployeeCommandRepositoryPort {

	private final EmployeeJpaRepository employeeJpaRepository;

	private final EmployeeBDMapper employeeBDMapper;

	@Override
	public Employee save(Employee employee) {
		return employeeBDMapper.toDomain(employeeJpaRepository.save(employeeBDMapper.toEntity(employee)));
	}

}

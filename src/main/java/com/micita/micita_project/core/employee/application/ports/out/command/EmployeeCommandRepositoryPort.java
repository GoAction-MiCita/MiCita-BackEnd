package com.micita.micita_project.core.employee.application.ports.out.command;

import com.micita.micita_project.core.employee.domain.models.Employee;

public interface EmployeeCommandRepositoryPort {

	/**
	 * @return the stored employee, with its generated id
	 */
	Employee save(Employee employee);

}

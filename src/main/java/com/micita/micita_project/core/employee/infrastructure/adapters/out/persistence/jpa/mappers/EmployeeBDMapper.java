package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers;

import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;
import org.mapstruct.Mapper;

@Mapper
public interface EmployeeBDMapper {

	EmployeeEntity toEntity(Employee employee);

	Employee toDomain(EmployeeEntity entity);

}

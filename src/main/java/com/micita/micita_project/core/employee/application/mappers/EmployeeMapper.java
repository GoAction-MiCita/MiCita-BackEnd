package com.micita.micita_project.core.employee.application.mappers;

import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.domain.models.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EmployeeMapper {

	@Mapping(target = "serviceIds", source = "serviceIds")
	EmployeeResponse toResponse(Employee employee, Set<UUID> serviceIds);

}

package com.micita.micita_project.core.employee.application.mappers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.response.EmployeeDetailResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EmployeeMapper {

	@Mapping(target = "serviceIds", source = "serviceIds")
	EmployeeResponse toResponse(Employee employee, Set<UUID> serviceIds);

	@Mapping(target = "services", source = "services")
	EmployeeDetailResponse toDetailResponse(Employee employee, List<EmployeeServiceRef> services);

}

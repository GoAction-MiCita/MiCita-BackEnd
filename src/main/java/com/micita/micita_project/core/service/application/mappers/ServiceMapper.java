package com.micita.micita_project.core.service.application.mappers;

import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.domain.models.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ServiceMapper {

	@Mapping(target = "employeeIds", source = "employeeIds")
	ServiceResponse toResponse(Service service, Set<UUID> employeeIds);

}

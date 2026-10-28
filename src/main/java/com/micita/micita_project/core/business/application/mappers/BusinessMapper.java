package com.micita.micita_project.core.business.application.mappers;

import java.time.ZoneId;

import com.micita.micita_project.core.business.application.dto.response.BusinessResponse;
import com.micita.micita_project.core.business.domain.models.Business;
import org.mapstruct.Mapper;

@Mapper
public interface BusinessMapper {

	BusinessResponse toResponse(Business business);

	default String toZoneName(ZoneId zoneId) {
		return zoneId.getId();
	}

}

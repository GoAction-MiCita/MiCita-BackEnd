package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.mappers;

import java.time.ZoneId;

import com.micita.micita_project.core.business.domain.models.Business;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.entities.BusinessEntity;
import org.mapstruct.Mapper;

@Mapper
public interface BusinessBDMapper {

	BusinessEntity toEntity(Business business);

	Business toDomain(BusinessEntity entity);

	default String toZoneName(ZoneId zoneId) {
		return zoneId.getId();
	}

	default ZoneId toZoneId(String zoneName) {
		return ZoneId.of(zoneName);
	}

}

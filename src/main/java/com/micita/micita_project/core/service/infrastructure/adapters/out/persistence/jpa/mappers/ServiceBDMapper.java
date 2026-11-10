package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers;

import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import org.mapstruct.Mapper;

@Mapper
public interface ServiceBDMapper {

	ServiceEntity toEntity(Service service);

	Service toDomain(ServiceEntity entity);

}

package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceCriteria;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers.ServiceBDMapper;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceQueryJpaAdapter implements ServiceQueryRepositoryPort {

	private static final char LIKE_ESCAPE = '\\';

	private final ServiceJpaRepository serviceJpaRepository;

	private final ServiceBDMapper serviceBDMapper;

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> serviceIds) {
		return serviceJpaRepository.findIdsOfBusiness(businessId, serviceIds, ServiceStatus.DELETED);
	}

	@Override
	public boolean existsByName(UUID businessId, String name) {
		return serviceJpaRepository.existsByName(businessId, name, ServiceStatus.DELETED);
	}

	@Override
	public PageResponse<Service> findPageOfBusiness(UUID businessId, ServiceCriteria criteria, int page, int size) {
		Page<ServiceEntity> result = serviceJpaRepository.findAll(matching(businessId, criteria),
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"))));
		List<Service> content = result.getContent().stream().map(serviceBDMapper::toDomain).toList();
		return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
				result.getTotalPages());
	}

	/**
	 * Every restriction is combined with AND. The name is matched with {@code LIKE}, so its wildcards are
	 * escaped; accents are not normalized.
	 */
	static Specification<ServiceEntity> matching(UUID businessId, ServiceCriteria criteria) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("businessId"), businessId));
			predicates.add(criteria.status() == null ? cb.notEqual(root.get("status"), ServiceStatus.DELETED)
					: cb.equal(root.get("status"), criteria.status()));
			if (criteria.name() != null) {
				String pattern = "%" + escapeLike(criteria.name().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE));
			}
			if (criteria.serviceIds() != null) {
				predicates.add(criteria.serviceIds().isEmpty() ? cb.disjunction()
						: root.get("id").in(criteria.serviceIds()));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	static String escapeLike(String value) {
		String escape = String.valueOf(LIKE_ESCAPE);
		return value.replace(escape, escape + escape).replace("%", escape + "%").replace("_", escape + "_");
	}

}

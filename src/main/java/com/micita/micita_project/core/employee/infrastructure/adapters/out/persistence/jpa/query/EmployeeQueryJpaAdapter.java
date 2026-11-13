package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeCriteria;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers.EmployeeBDMapper;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeQueryJpaAdapter implements EmployeeQueryRepositoryPort {

	private static final char LIKE_ESCAPE = '\\';

	private final EmployeeJpaRepository employeeJpaRepository;

	private final EmployeeBDMapper employeeBDMapper;

	@Override
	public boolean existsByEmail(UUID businessId, String email) {
		return employeeJpaRepository.existsByBusinessIdAndEmailAndStatusNot(businessId, email, EmployeeStatus.DELETED);
	}

	@Override
	public boolean existsByPhone(UUID businessId, String phone) {
		return employeeJpaRepository.existsByBusinessIdAndPhoneAndStatusNot(businessId, phone, EmployeeStatus.DELETED);
	}

	@Override
	public boolean existsByEmailExcept(UUID businessId, String email, UUID employeeId) {
		return employeeJpaRepository.existsByBusinessIdAndEmailAndStatusNotAndIdNot(businessId, email,
				EmployeeStatus.DELETED, employeeId);
	}

	@Override
	public boolean existsByPhoneExcept(UUID businessId, String phone, UUID employeeId) {
		return employeeJpaRepository.existsByBusinessIdAndPhoneAndStatusNotAndIdNot(businessId, phone,
				EmployeeStatus.DELETED, employeeId);
	}

	@Override
	public Optional<Employee> findOfBusiness(UUID businessId, UUID employeeId) {
		return employeeJpaRepository.findByIdAndBusinessIdAndStatusNot(employeeId, businessId, EmployeeStatus.DELETED)
			.map(employeeBDMapper::toDomain);
	}

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds) {
		return employeeJpaRepository.findIdsOfBusiness(businessId, employeeIds, EmployeeStatus.DELETED);
	}

	@Override
	public List<Employee> findAllOfBusiness(UUID businessId, Collection<UUID> employeeIds) {
		return employeeJpaRepository.findByBusinessIdAndIdInAndStatusNot(businessId, employeeIds, EmployeeStatus.DELETED)
			.stream()
			.map(employeeBDMapper::toDomain)
			.toList();
	}

	@Override
	public PageResponse<Employee> findPageOfBusiness(UUID businessId, EmployeeCriteria criteria, int page,
			int size) {
		Page<EmployeeEntity> result = employeeJpaRepository.findAll(matching(businessId, criteria),
				PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
		List<Employee> content = result.getContent().stream().map(employeeBDMapper::toDomain).toList();
		return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
				result.getTotalPages());
	}

	/**
	 * Every restriction is combined with AND. The name is matched with {@code LIKE}, so its wildcards are
	 * escaped; accents are not normalized.
	 */
	static Specification<EmployeeEntity> matching(UUID businessId, EmployeeCriteria criteria) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("businessId"), businessId));
			predicates.add(criteria.status() == null ? cb.notEqual(root.get("status"), EmployeeStatus.DELETED)
					: cb.equal(root.get("status"), criteria.status()));
			if (criteria.name() != null) {
				String pattern = "%" + escapeLike(criteria.name().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("firstName")), pattern, LIKE_ESCAPE),
						cb.like(cb.lower(root.get("lastName")), pattern, LIKE_ESCAPE)));
			}
			if (criteria.employeeIds() != null) {
				predicates.add(criteria.employeeIds().isEmpty() ? cb.disjunction()
						: root.get("id").in(criteria.employeeIds()));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	static String escapeLike(String value) {
		String escape = String.valueOf(LIKE_ESCAPE);
		return value.replace(escape, escape + escape).replace("%", escape + "%").replace("_", escape + "_");
	}

	@Override
	public Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds) {
		return employeeJpaRepository.findIdsOfBusinessWithStatus(businessId, employeeIds, EmployeeStatus.ACTIVE);
	}

}

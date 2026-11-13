package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceCriteria;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers.ServiceBDMapper;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ServiceQueryJpaAdapterTest {

	@Mock
	private ServiceJpaRepository serviceJpaRepository;

	@Mock
	private ServiceBDMapper serviceBDMapper;

	@InjectMocks
	private ServiceQueryJpaAdapter adapter;

	@Test
	void pageOfTheBusinessExcludesDeletedAndSortsByNameIgnoringCase() {
		UUID businessId = UUID.randomUUID();
		ServiceEntity entity = new ServiceEntity();
		Service service = new Service(UUID.randomUUID(), businessId, "Corte", null, null, null, ServiceStatus.ACTIVE,
				Instant.now(), Instant.now());
		given(serviceJpaRepository.findAll(any(Specification.class), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 11));
		given(serviceBDMapper.toDomain(entity)).willReturn(service);

		PageResponse<Service> page = adapter.findPageOfBusiness(businessId, ServiceCriteria.none(), 1, 5);

		assertThat(page.content()).containsExactly(service);
		assertThat(page.page()).isEqualTo(1);
		assertThat(page.size()).isEqualTo(5);
		assertThat(page.totalElements()).isEqualTo(11);
		assertThat(page.totalPages()).isEqualTo(3);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(serviceJpaRepository).findAll(any(Specification.class), pageable.capture());
		assertThat(pageable.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
		assertThat(pageable.getValue().getSort().getOrderFor("name").isIgnoreCase()).isTrue();
		assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void likeWildcardsInTheNameAreEscaped() {
		assertThat(ServiceQueryJpaAdapter.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
		assertThat(ServiceQueryJpaAdapter.escapeLike("cor")).isEqualTo("cor");
	}

	@Test
	@SuppressWarnings("unchecked")
	void everyRestrictionBecomesAPredicateCombinedWithAnd() {
		UUID businessId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();
		Root<ServiceEntity> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path<Object> path = mock(Path.class);
		Path<String> stringPath = mock(Path.class);
		Predicate predicate = mock(Predicate.class);
		Expression<String> lower = mock(Expression.class);
		given(root.get(any(String.class))).willReturn((Path) path);
		given(root.<String>get("name")).willReturn(stringPath);
		given(cb.equal(any(), any(Object.class))).willReturn(predicate);
		given(cb.lower(stringPath)).willReturn(lower);
		given(cb.like(eq(lower), any(String.class), eq('\\'))).willReturn(predicate);
		given(path.in(any(Collection.class))).willReturn(predicate);
		given(cb.and(any(Predicate[].class))).willReturn(predicate);

		ServiceQueryJpaAdapter.matching(businessId,
				new ServiceCriteria("Cor_", ServiceStatus.ACTIVE, Set.of(serviceId)))
			.toPredicate(root, query, cb);

		verify(cb).equal(path, businessId);
		verify(cb).equal(path, ServiceStatus.ACTIVE);
		verify(cb).like(lower, "%cor\\_%", '\\');
		verify(path).in(Set.of(serviceId));
		ArgumentCaptor<Predicate[]> combined = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(combined.capture());
		assertThat(combined.getValue()).hasSize(4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void withoutStatusDeletedServicesAreExcluded() {
		Root<ServiceEntity> root = mock(Root.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path<Object> path = mock(Path.class);
		Predicate predicate = mock(Predicate.class);
		given(root.get(any(String.class))).willReturn((Path) path);
		given(cb.equal(any(), any(Object.class))).willReturn(predicate);
		given(cb.notEqual(any(), any(Object.class))).willReturn(predicate);
		given(cb.and(any(Predicate[].class))).willReturn(predicate);

		ServiceQueryJpaAdapter.matching(UUID.randomUUID(), ServiceCriteria.none())
			.toPredicate(root, mock(CriteriaQuery.class), cb);

		verify(cb).notEqual(path, ServiceStatus.DELETED);
		ArgumentCaptor<Predicate[]> combined = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(combined.capture());
		assertThat(combined.getValue()).hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void emptyServiceIdsMatchNothing() {
		Root<ServiceEntity> root = mock(Root.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path<Object> path = mock(Path.class);
		Predicate predicate = mock(Predicate.class);
		given(root.get(any(String.class))).willReturn((Path) path);
		given(cb.equal(any(), any(Object.class))).willReturn(predicate);
		given(cb.notEqual(any(), any(Object.class))).willReturn(predicate);
		given(cb.disjunction()).willReturn(predicate);
		given(cb.and(any(Predicate[].class))).willReturn(predicate);

		ServiceQueryJpaAdapter.matching(UUID.randomUUID(), new ServiceCriteria(null, null, Set.of()))
			.toPredicate(root, mock(CriteriaQuery.class), cb);

		verify(cb).disjunction();
	}

	@Test
	void assignableIdsExcludeDeletedServices() {
		UUID businessId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();
		given(serviceJpaRepository.findIdsOfBusiness(businessId, List.of(serviceId), ServiceStatus.DELETED))
			.willReturn(Set.of(serviceId));

		assertThat(adapter.findAssignableIds(businessId, List.of(serviceId))).containsExactly(serviceId);
	}

	@Test
	void nameLookupSkipsDeletedServices() {
		UUID businessId = UUID.randomUUID();
		given(serviceJpaRepository.existsByName(businessId, "Corte", ServiceStatus.DELETED)).willReturn(true);

		assertThat(adapter.existsByName(businessId, "Corte")).isTrue();
		assertThat(adapter.existsByName(businessId, "Otro")).isFalse();
	}

}

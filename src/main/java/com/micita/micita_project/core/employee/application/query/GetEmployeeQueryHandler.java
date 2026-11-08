package com.micita.micita_project.core.employee.application.query;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeDetailResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.in.GetEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetEmployeeQueryHandler implements GetEmployeeUseCase {

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeServicesQueryPort employeeServicesQuery;

	private final EmployeeMapper employeeMapper;

	@Override
	public EmployeeDetailResponse get(UUID ownerId, UUID employeeId) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Employee employee = employeeQueryRepository.findOfBusiness(businessId, employeeId)
			.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		return employeeMapper.toDetailResponse(employee, employeeServicesQuery
			.findActiveServicesOf(businessId, List.of(employeeId))
			.getOrDefault(employeeId, List.of()));
	}

}

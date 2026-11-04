package com.micita.micita_project.common.shared.pagination;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One page of a listing, zero-based. Shared by every paginated endpoint (HU-15 contract).
 */
@Schema(description = "One page of results")
public record PageResponse<T>(
		@Schema(description = "Items of this page") List<T> content,
		@Schema(example = "0") int page,
		@Schema(example = "10") int size,
		@Schema(example = "23") long totalElements,
		@Schema(example = "3") int totalPages) {

}

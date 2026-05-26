package com.salles.scrapper.data

import com.salles.domain.PagedResponseInterface
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    override val data: List<T>,
    override val page: Int,
    override val pageSize: Int,
    override val hasNext: Boolean,
) : PagedResponseInterface<T>

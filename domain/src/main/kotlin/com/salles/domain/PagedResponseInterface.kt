package com.salles.domain


interface PagedResponseInterface<T> {
    val data: List<T>
    val page: Int
    val pageSize: Int
    val hasNext: Boolean
}
package com.saurabhsandav.core.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState

class ListPagingSource<T : Any>(
    private val data: List<T>,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {

        // Determine the current page key. If it's null, we start from the beginning (page 0).
        val currentPage = params.key ?: 0

        // Calculate the start and end index for the current page
        val startOffset = currentPage * params.loadSize
        val endOffset = (startOffset + params.loadSize).coerceAtMost(data.size)

        // Get the data for the current page
        val pageData = when {
            startOffset <= endOffset -> data.subList(startOffset, endOffset)
            else -> emptyList()
        }

        // Calculate the next page key. If we've reached the end of the data, nextKey will be null.
        val nextKey = if (endOffset == data.size) null else currentPage + 1

        return LoadResult.Page(
            data = pageData,
            prevKey = if (currentPage == 0) null else currentPage - 1,
            nextKey = nextKey,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        // This method is used when the data needs to be refreshed (e.g., after an invalidation).
        // You can return the closest page to the anchor position.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

package com.ritense.iko.search

import org.springframework.stereotype.Service

@Service
class SearchService(
    private val searchRepository: SearchRepository
) {

    fun getPrimarySearches() = searchRepository.findAllByIsPrimaryTrueOrderByName()

    fun getSearches(): List<Search> = searchRepository.findAll()
}
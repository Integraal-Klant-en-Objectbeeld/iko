/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.aggregateddataprofile.rest

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/aggregated-data-profiles")
internal class AggregatedDataProfileController(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) {

    @GetMapping("/{name}/schema", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional(readOnly = true)
    fun getSchema(@PathVariable name: String): ResponseEntity<String> {
        val adp = aggregatedDataProfileRepository.findByNameAndIsActiveTrue(name)
            ?: return ResponseEntity.notFound().build()
        val schema = adp.jsonschema
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(schema)
    }
}
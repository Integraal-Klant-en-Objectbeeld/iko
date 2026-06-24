/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

package com.ritense.iko

/**
 * Opts a class out of the ArchUnit rule that forbids [org.springframework.data.jpa.repository.JpaRepository.getReferenceById].
 *
 * OSIV is disabled (`spring.jpa.open-in-view=false`), so `getReferenceById` returns a lazy proxy that
 * throws `LazyInitializationException` once a non-id property is touched outside an open Hibernate
 * session. The default fix is `findById(id).orElseThrow()`, which loads the entity eagerly.
 *
 * Annotate a class with this only when `getReferenceById` is used safely — i.e. the proxy is passed
 * straight to a query (Spring Data derived query, JPQL parameter) where Hibernate reads just the id and
 * never initializes the proxy. State why in [reason].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OsivSafeReference(val reason: String)
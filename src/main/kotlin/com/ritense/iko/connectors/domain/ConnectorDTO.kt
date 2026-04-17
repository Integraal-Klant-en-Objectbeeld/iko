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

package com.ritense.iko.connectors.domain

import com.ritense.iko.aggregateddataprofile.domain.EntityStatus
import java.util.UUID

data class ConnectorDTO(
    val id: UUID,
    val name: String,
    val tag: String,
    val version: String,
    val isActive: Boolean,
    val status: EntityStatus,
    val final: Boolean,
    val connectorCode: String,
)

fun Connector.toDTO() = ConnectorDTO(
    id = id,
    name = name,
    tag = tag,
    version = version.value,
    isActive = isActive,
    status = status,
    final = status == EntityStatus.FINAL,
    connectorCode = connectorCode,
)
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

package com.ritense.iko.connectors.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConnectorServiceTest {

    @Nested
    inner class NamespaceUri {

        @Test
        fun `namespaces connector from URI with version`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:connector:my-tag",
                ":1.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:connector:my-tag:1.0.0")
        }

        @Test
        fun `namespaces transform from URI with version`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:endpoint:transform:my-tag",
                ":2.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:endpoint:transform:my-tag:2.0.0")
        }

        @Test
        fun `namespaces transform from URI with operation and version`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:endpoint:transform:my-tag.get_zaak",
                ":1.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:endpoint:transform:my-tag:1.0.0.get_zaak")
        }

        @Test
        fun `does not modify unrelated URIs`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:config",
                ":1.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:config")
        }

        @Test
        fun `does not modify already-namespaced connector URIs`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:connector:my-tag:1.0.0",
                ":2.0.0",
            )
            // Already contains a colon after tag, so regex won't match
            assertThat(result).isEqualTo("direct:iko:connector:my-tag:1.0.0")
        }
    }
}
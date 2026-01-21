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

package com.ritense.iko.aggregateddataprofile.camel

import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PairAggregatorTest {

    private val context = DefaultCamelContext()

    @Test
    fun `When oldExchange is null, then it should return newExchange`() {
        // Arrange
        val newExchange = DefaultExchange(context)
        newExchange.getIn().body = "initial body"

        // Act
        val result = PairAggregator.aggregate(null, newExchange)

        // Assert
        assertThat(result).isEqualTo(newExchange)
        assertThat(result.getIn().body).isEqualTo("initial body")
    }

    @Test
    fun `When oldExchange is not null, then it should return a map with left and right`() {
        // Arrange
        val oldExchange = DefaultExchange(context)
        oldExchange.getIn().body = "left body"

        val newExchange = DefaultExchange(context)
        newExchange.getIn().body = "right body"

        // Act
        val result = PairAggregator.aggregate(oldExchange, newExchange)

        // Assert
        assertThat(result).isEqualTo(oldExchange)
        val body = result.getIn().body as Map<String, Any>
        assertThat(body).containsEntry("left", "left body")
        assertThat(body).containsEntry("right", "right body")
    }

    @Test
    fun `When newExchange has an exception, then it should throw that exception`() {
        // Arrange
        val oldExchange = DefaultExchange(context)
        val newExchange = DefaultExchange(context)
        val exception = RuntimeException("Something went wrong")
        newExchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception)

        // Act & Assert
        assertThatThrownBy {
            PairAggregator.aggregate(oldExchange, newExchange)
        }.isEqualTo(exception)
    }
}
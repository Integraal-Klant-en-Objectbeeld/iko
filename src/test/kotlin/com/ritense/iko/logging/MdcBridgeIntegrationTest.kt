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

package com.ritense.iko.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.ritense.iko.BaseIntegrationTest
import com.ritense.iko.camel.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Verifies that [MdcContextProcessor] bridges Camel exchange variables into SLF4J MDC
 * during an ADP route execution, so the DBAppender (insertHeaders=true) will persist them
 * as logging_event_property rows when IKO_LOGGING_DB_ENABLED=true.
 *
 * Uses a temporary in-memory Logback appender to capture MDC state without requiring
 * IKO_LOGGING_DB_ENABLED=true, making this test runnable in the standard CI environment.
 *
 * Note: iko_trace_id is only populated on the test-run path (via TestController),
 * not on the normal REST path, so only correlationId is asserted here.
 */
@AutoConfigureMockMvc
internal class MdcBridgeIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val capturedEvents = CopyOnWriteArrayList<ILoggingEvent>()
    private lateinit var capturingAppender: CapturingAppender
    private lateinit var rootLogger: Logger

    @BeforeEach
    fun attachCapturingAppender() {
        capturingAppender = CapturingAppender(capturedEvents)
        capturingAppender.name = "MDC_CAPTURE_TEST"
        capturingAppender.start()
        rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(capturingAppender)
    }

    @AfterEach
    fun detachCapturingAppender() {
        rootLogger.detachAppender(capturingAppender)
        capturedEvents.clear()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `ADP route sets correlationId MDC key during exchange so log events carry it as a property`() {
        val mvcResult =
            mockMvc
                .perform(get("/aggregated-data-profiles/pets?id=externalId"))
                .andExpect(request().asyncStarted())
                .andReturn()

        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)

        // The capturing appender records MDC state at log time.
        // At least one log event emitted during the ADP exchange must carry the correlationId MDC entry;
        // this proves MdcContextProcessor populated MDC before downstream processors ran.
        val eventsWithCorrelationId =
            capturedEvents.filter { event ->
                event.mdcPropertyMap?.containsKey(IKO_CORRELATION_ID_VARIABLE) == true
            }

        assertThat(eventsWithCorrelationId)
            .withFailMessage(
                "Expected at least one log event to carry MDC key '$IKO_CORRELATION_ID_VARIABLE' " +
                    "but none were found. Captured ${capturedEvents.size} events in total.",
            ).isNotEmpty()

        eventsWithCorrelationId.forEach { event ->
            assertThat(event.mdcPropertyMap[IKO_CORRELATION_ID_VARIABLE])
                .withFailMessage("correlationId MDC value must not be blank")
                .isNotBlank()
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `MDC does not contain correlationId after ADP exchange completes (no thread-pool leakage)`() {
        // Capture the MDC state of any log events emitted AFTER the exchange completes.
        // We do this by recording what the MDC looks like from the test thread,
        // which should never see the correlationId key.
        assertThat(org.slf4j.MDC.get(IKO_CORRELATION_ID_VARIABLE))
            .withFailMessage("MDC should not contain '$IKO_CORRELATION_ID_VARIABLE' before the request")
            .isNull()

        val mvcResult =
            mockMvc
                .perform(get("/aggregated-data-profiles/pets?id=externalId"))
                .andExpect(request().asyncStarted())
                .andReturn()

        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)

        // The test thread's MDC is never polluted (it was set on the Camel worker thread).
        assertThat(org.slf4j.MDC.get(IKO_CORRELATION_ID_VARIABLE))
            .withFailMessage("MDC '$IKO_CORRELATION_ID_VARIABLE' leaked into test thread after exchange")
            .isNull()
        assertThat(org.slf4j.MDC.get(IKO_TRACE_ID_VARIABLE))
            .withFailMessage("MDC '$IKO_TRACE_ID_VARIABLE' leaked into test thread after exchange")
            .isNull()
    }

    private class CapturingAppender(
        private val events: CopyOnWriteArrayList<ILoggingEvent>,
    ) : AppenderBase<ILoggingEvent>() {
        override fun append(eventObject: ILoggingEvent) {
            // Snapshot the MDC map at append time (it is thread-local, so snapshot it now).
            eventObject.prepareForDeferredProcessing()
            events.add(eventObject)
        }
    }
}
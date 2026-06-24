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

package com.ritense.iko.architecture

import com.ritense.iko.OsivSafeReference
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

class OsivLazyReferenceArchTest {
    private val productionClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.ritense.iko")

    @Test
    fun `production code does not call getReferenceById because OSIV is disabled`() {
        classes()
            .should(notCallGetReferenceById)
            .because(
                "spring.jpa.open-in-view=false: JpaRepository.getReferenceById returns a lazy proxy that throws " +
                    "LazyInitializationException when a property is accessed outside an open session. " +
                    "Use findById(id).orElseThrow(). If the reference is only passed to a query (id-only access), " +
                    "annotate the declaring class with @OsivSafeReference.",
            )
            .check(productionClasses)
    }

    private val notCallGetReferenceById =
        object : ArchCondition<JavaClass>("not call JpaRepository.getReferenceById") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                if (item.isOrEnclosedByOsivSafe()) return
                item.methodCallsFromSelf
                    .filter { it.target.name == "getReferenceById" }
                    .forEach { call -> events.add(SimpleConditionEvent.violated(call, call.description)) }
            }
        }

    private fun JavaClass.isOrEnclosedByOsivSafe(): Boolean {
        var current: JavaClass? = this
        while (current != null) {
            if (current.isAnnotatedWith(OsivSafeReference::class.java)) return true
            current = current.enclosingClass.orElse(null)
        }
        return false
    }
}
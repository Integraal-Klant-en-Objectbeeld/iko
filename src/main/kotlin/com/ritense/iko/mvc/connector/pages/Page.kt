package com.ritense.iko.mvc.connector.pages

import com.ritense.iko.mvc.controller.HomeController
import org.springframework.web.servlet.ModelAndView

open class Page<T: Page<T>>(val hxRequestFragment: String): ModelAndView() {

    init {
        this.modelMap.addAttribute("menuItems", HomeController.menuItems)
    }

    open fun isHxRequest(isHxRequest: Boolean): T {
        if (isHxRequest) {
            viewName = "$viewName :: $hxRequestFragment"
        }

        return this as T
    }

    open fun attributes(): Map<String, Any> {
        return mapOf()
    }

    open fun build(): ModelAndView {
        modelMap.addAllAttributes(attributes())

        return this
    }
}
### Frontend Guidelines (Server‑Rendered UI with Spring MVC, HTMX, and IBM Carbon Design System)

This document captures conventions and patterns for building the server‑rendered frontend of this project. It complements the existing Spring Boot (Kotlin) backend guidelines.

---

### Tech Stack Overview

- Rendering model: server‑side HTML via Spring MVC controllers and Thymeleaf templates/fragments.
- Interactivity: progressive enhancement with HTMX (AJAX, partial updates, and navigation).
- Design system: IBM Carbon Design System for components, theming, spacing, and accessibility.
- Target: accessible, fast, resilient UI that works without JS, enhanced with HTMX when available.

---

### IBM Carbon Design System

#### What to use

- Prefer Carbon Web Components (custom elements) or Carbon CSS classes (vanilla) depending on need:
    - Carbon Web Components: accessible, JS‑powered components (e.g., `cds-button`, `cds-modal`, `cds-inline-loading`). Good default choice.
    - Carbon CSS (vanilla `carbon-components`): use for utility classes, grid, spacing, and when a component has no web component counterpart.

#### Installation

- Easiest (CDN) to get started in server‑rendered apps:

```html
<!-- in layout head -->
<link rel="stylesheet" href="https://unpkg.com/@carbon/styles/css/styles.min.css">
<script type="module" src="https://unpkg.com/@carbon/web-components/es/components-all.js"></script>
```

#### Theming and tokens

- Default theme is `g10` (light). To switch themes, set the `theme` attribute on the `body` or a wrapper element:

```html
<body theme="g10"> <!-- g10 | g90 | g100 -->
```

- Use spacing and type tokens via CSS custom properties provided by Carbon (avoid hardcoded pixel values). Example:

```css
.section {
  padding-block: var(--cds-spacing-07);
}
```

#### Layout and grid

- Use Carbon grid classes with semantic HTML:

```html
<div class="cds--grid">
  <div class="cds--row">
    <div class="cds--col-lg-8 cds--col-md-6">...</div>
    <div class="cds--col-lg-4 cds--col-md-2">...</div>
  </div>
</div>
```

#### Using components with Thymeleaf

- Web components work in Thymeleaf templates; bind attributes as usual:

```html
<cds-button kind="primary" th:text="#{action.save}"></cds-button>

<cds-modal open="" size="lg" th:if="${showModal}">
  <p class="cds--modal-content__regular-content" th:text="${message}"></p>
</cds-modal>
```

- Prefer Carbon form styles/components for inputs, labels, helpers, and validation states. When using native inputs, apply Carbon classes like `cds--text-input` for consistent styling.

---

### HTMX Usage

HTMX enables AJAX/partial updates with minimal JS and integrates well with Spring MVC + Thymeleaf.

#### Core conventions

- Progressive enhancement: every page should work without HTMX (full page loads). Enhance with `hx-*` attributes for partial UX improvements.
- Fragment endpoints: controller actions that serve HTMX requests should return Thymeleaf fragments located under `src/main/resources/templates/fragments/...`.
- Response shape: return only the HTML needed for the target (not full pages) for HTMX calls.
- Targets and swaps:
    - Use `hx-target="#elementId"` to specify where the response is injected.
    - Default swap is `innerHTML`; consider `hx-swap="outerHTML"` for replacements, or `beforeend`/`afterbegin` for list appends.

Example list with pagination via HTMX:

```html
<div id="users">
  <div id="user-table" hx-get="/internal/users/table" hx-trigger="load" hx-target="#user-table" hx-swap="innerHTML">
    <!-- server fills table fragment here -->
  </div>
  <button class="cds--btn" hx-get="/internal/users/table?page=2" hx-target="#user-table">Next</button>
</div>
```

#### Forms and validation

- Submit forms with `hx-post` and handle success/error by swapping dedicated regions:

```html
<form hx-post="/internal/profile" hx-target="#profile-form" hx-swap="outerHTML">
  <!-- fields -->
  <cds-button kind="primary" type="submit">Save</cds-button>
  <div id="form-errors"></div>
</form>
```

- Controller should return the same form fragment with binding errors. Use HTTP `422 Unprocessable Entity` for validation errors when possible;
  alternatively, `200 OK` is acceptable if swapping the form fragment containing field errors.

#### Lists, search, and pagination

- Use `hx-get` for searching and paging, and return only the table body or list items as a fragment for quick updates. Favor server‑side sorting and pagination for consistency and to avoid duplicating logic in JS.

---

### Spring MVC Integration Patterns

#### Controller pattern for full page vs. fragment

- For the same route, return a full page on normal requests and a fragment when `HX-Request: true` header is present.

```kotlin
@Controller
class UserController(private val svc: UserService) {

    @GetMapping("/internal/users")
    fun users(model: Model, request: HttpServletRequest): String {
        model.addAttribute("users", svc.list())
        val isHtmx = request.getHeader("HX-Request") == "true"
        return if (isHtmx) "fragments/internal/user/list :: table" else "pages/internal/user/list"
    }

    @PostMapping("/internal/profile")
    fun saveProfile(@Valid form: ProfileForm, br: BindingResult, model: Model, request: HttpServletRequest): ResponseEntity<String> {
        if (br.hasErrors()) {
            model.addAttribute("form", form)
            val html = render("fragments/internal/profile/form", model)
            return ResponseEntity.status(422).body(html)
        }
        svc.update(form)
        model.addAttribute("profile", svc.get())
        val html = render("fragments/internal/profile/view", model)
        return ResponseEntity.ok(html)
    }
}
```

- The `render` helper refers to a server‑side view rendering utility; alternatively return `ModelAndView` and rely on `ViewResolver` to render the fragment.

---

### Folder Structure Recommendations

- `templates/pages/...` full page templates including the global layout and page composition.
- `templates/fragments/...` reusable fragments for HTMX swaps and partial rendering.
- `static/...` (if used): project‑local CSS/JS (e.g., small glue scripts). Keep custom JS minimal; prefer HTMX and Carbon behavior.

### References

- IBM Carbon Design System: `https://carbondesignsystem.com/`
- Carbon Web Components: `https://github.com/carbon-design-system/carbon-web-components`
- HTMX Documentation: `https://htmx.org/docs/`
- Thymeleaf: `https://www.thymeleaf.org/documentation.html`
- Spring MVC: `https://docs.spring.io/spring-framework/reference/web/webmvc.html`

### File Formatting
- Do not add empty trailing lines to any file.

**Explanation:**
- Keeping files compact without unnecessary trailing whitespace helps in maintaining a consistent and dense code structure.
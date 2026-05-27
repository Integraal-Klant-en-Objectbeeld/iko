/* Session timeout warning modal and inactivity timer */
(function () {
    function init() {
        const modal = document.getElementById("session-timeout-modal");
        if (!modal) {
            console.debug("[session-timeout] modal element not found");
            return;
        }

        const timeoutSec = parseInt(modal.dataset.timeoutSeconds, 10);
        const warningSec = parseInt(modal.dataset.warningSeconds, 10);
        if (
            !Number.isFinite(timeoutSec) ||
            !Number.isFinite(warningSec) ||
            timeoutSec <= warningSec
        ) {
            console.warn(
                "[session-timeout] disabled: invalid timeout/warning",
                { timeoutSec, warningSec },
            );
            return;
        }

        const idleMs = (timeoutSec - warningSec) * 1000;
        // Keep-alive throttle: while the user is active, ping the server at
        // most once per idle window. This stays within the session lifetime
        // (timeout - warning leaves the warning window as margin), so genuine
        // activity actually extends the server session instead of only
        // resetting the in-browser timer.
        const keepAliveThrottleMs = idleMs;
        const countdownEl = document.getElementById(
            "session-timeout-countdown",
        );
        let idleTimer = null;
        let countdownTimer = null;
        let warningActive = false;
        let lastServerContact = Date.now();

        console.debug(
            "[session-timeout] initialised; warning after " +
                (timeoutSec - warningSec) +
                "s of inactivity, " +
                warningSec +
                "s countdown",
        );

        function logout(event) {
            if (event) event.preventDefault();
            console.debug("[session-timeout] logging out");
            const form = document.createElement("form");
            form.method = "POST";
            form.action = "/logout";
            document.body.appendChild(form);
            form.submit();
        }

        function startCountdown() {
            let remaining = warningSec;
            countdownEl.textContent = remaining;
            countdownTimer = setInterval(function () {
                remaining -= 1;
                countdownEl.textContent = remaining;
                if (remaining <= 0) {
                    clearInterval(countdownTimer);
                    logout();
                }
            }, 1000);
        }

        function showWarning() {
            console.debug("[session-timeout] inactivity reached, showing modal");
            warningActive = true;
            modal.setAttribute("open", "");
            startCountdown();
        }

        function pingServer() {
            lastServerContact = Date.now();
            console.debug("[session-timeout] keep-alive ping");
            return fetch("/admin/session/ping", { credentials: "same-origin" });
        }

        function resetTimer() {
            // While the warning is shown, only an explicit "Continue" resets it.
            if (warningActive) return;
            clearTimeout(idleTimer);
            idleTimer = setTimeout(showWarning, idleMs);
        }

        // User activity: reset the in-browser timer and, when throttle allows,
        // ping the server so the activity also extends the server session.
        function onActivity() {
            if (warningActive) return;
            resetTimer();
            if (Date.now() - lastServerContact >= keepAliveThrottleMs) {
                pingServer().catch(function () {});
            }
        }

        // A real HTMX request already touched the server session; just record
        // the contact and reset the timer (no extra ping needed).
        function onServerRequest() {
            lastServerContact = Date.now();
            resetTimer();
        }

        function continueSession(event) {
            if (event) event.preventDefault();
            console.debug("[session-timeout] continue clicked");
            pingServer()
                .then(function (response) {
                    warningActive = false;
                    clearInterval(countdownTimer);
                    modal.removeAttribute("open");
                    if (response.ok) {
                        resetTimer();
                    } else {
                        logout();
                    }
                })
                .catch(logout);
        }

        document
            .getElementById("session-timeout-continue")
            .addEventListener("click", continueSession);
        document
            .getElementById("session-timeout-logout")
            .addEventListener("click", logout);

        ["mousemove", "keydown", "click", "scroll"].forEach(function (event) {
            document.addEventListener(event, onActivity, { passive: true });
        });
        document.body.addEventListener("htmx:afterRequest", onServerRequest);

        resetTimer();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

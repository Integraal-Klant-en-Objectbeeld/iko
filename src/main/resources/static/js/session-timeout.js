/* Session timeout warning modal and inactivity timer */
(function () {
    function init() {
        const modal = document.getElementById("session-timeout-modal");
        if (!modal) {
            console.debug("[session-timeout] modal element not found");
            return;
        }

        // These start from the values rendered at login but are updated from
        // each ping response as the Keycloak refresh token slides.
        let timeoutSec = parseInt(modal.dataset.timeoutSeconds, 10);
        let warningSec = parseInt(modal.dataset.warningSeconds, 10);
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

        let idleMs = (timeoutSec - warningSec) * 1000;
        // Keep-alive throttle: while the user is active, ping the server at
        // most once per this interval so genuine activity actually extends the
        // server session (not just the in-browser timer). Half the idle window
        // keeps the ping comfortably ahead of the warning, capped at 60s so a
        // long production timeout still refreshes frequently enough.
        let keepAliveThrottleMs = Math.max(
            Math.min(idleMs / 2, 60000),
            1000,
        );
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
            window.ikoLogout();
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

        // Adopt the refreshed timeout/warning returned by a ping so the
        // countdown tracks the (slid) Keycloak refresh-token expiry instead of
        // the initial render values.
        function applyTimeout(data) {
            if (!data) return;
            const nextTimeout = parseInt(data.timeoutSeconds, 10);
            const nextWarning = parseInt(data.warningSeconds, 10);
            if (
                !Number.isFinite(nextTimeout) ||
                !Number.isFinite(nextWarning) ||
                nextTimeout <= nextWarning
            ) {
                console.warn(
                    "[session-timeout] ignoring invalid ping timeout/warning",
                    { nextTimeout, nextWarning },
                );
                return;
            }
            timeoutSec = nextTimeout;
            warningSec = nextWarning;
            idleMs = (timeoutSec - warningSec) * 1000;
            keepAliveThrottleMs = Math.max(
                Math.min(idleMs / 2, 60000),
                1000,
            );
        }

        // Refresh the Keycloak token at the server. Resolves with the parsed
        // {timeoutSeconds, warningSeconds} JSON on success, or rejects on a
        // non-OK response (e.g. 401 when the refresh failed) so the caller can
        // log out.
        function pingServer() {
            lastServerContact = Date.now();
            console.debug("[session-timeout] keep-alive ping");
            return fetch("/admin/session/ping", {
                credentials: "same-origin",
            }).then(function (response) {
                if (!response.ok) {
                    return Promise.reject(response.status);
                }
                return response.json();
            });
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
                pingServer()
                    .then(function (data) {
                        applyTimeout(data);
                        resetTimer();
                    })
                    .catch(function () {
                        logout();
                    });
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
                .then(function (data) {
                    warningActive = false;
                    clearInterval(countdownTimer);
                    modal.removeAttribute("open");
                    applyTimeout(data);
                    resetTimer();
                })
                .catch(function () {
                    logout();
                });
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

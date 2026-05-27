/* Session timeout warning modal and inactivity timer */
(function () {
    const modal = document.getElementById("session-timeout-modal");
    if (!modal) return;

    const timeoutSec = parseInt(modal.dataset.timeoutSeconds, 10);
    const warningSec = parseInt(modal.dataset.warningSeconds, 10);
    if (
        !Number.isFinite(timeoutSec) ||
        !Number.isFinite(warningSec) ||
        timeoutSec <= warningSec
    ) {
        return;
    }

    const idleMs = (timeoutSec - warningSec) * 1000;
    const countdownEl = document.getElementById("session-timeout-countdown");
    let idleTimer = null;
    let countdownTimer = null;
    let warningActive = false;

    function logout() {
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
        warningActive = true;
        modal.setAttribute("open", "");
        startCountdown();
    }

    function resetTimer() {
        // While the warning is shown, only an explicit "Continue" resets it.
        if (warningActive) return;
        clearTimeout(idleTimer);
        idleTimer = setTimeout(showWarning, idleMs);
    }

    function continueSession() {
        fetch("/admin/session/ping", { credentials: "same-origin" })
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
        document.addEventListener(event, resetTimer, { passive: true });
    });
    // Every successful HTMX request also resets the server-side session.
    document.body.addEventListener("htmx:afterRequest", resetTimer);

    resetTimer();
})();

document.addEventListener("DOMContentLoaded", function () {

    const form = document.querySelector(".hc-signup-form");
    const clientError = document.getElementById("clientError");
    const serverError = document.getElementById("serverError");

    if (!form) return; // stop if form doesn't exist

    // =========================
    // Toast / Friendly Popup
    // =========================
    let toastTimeout;

    function showFriendlyPopup(message) {
        const toast = document.getElementById("hc-toast");
        const msgSpan = document.getElementById("hc-toast-message");
        if (!toast || !msgSpan) {
            alert(message);
            return;
        }
        msgSpan.textContent = message;
        toast.classList.add("show");
        if (toastTimeout) clearTimeout(toastTimeout);
        toastTimeout = setTimeout(() => toast.classList.remove("show"), 4000);
    }

    const toastCloseBtn = document.querySelector("#hc-toast .hc-toast-close");
    if (toastCloseBtn) {
        toastCloseBtn.addEventListener("click", function () {
            const toast = document.getElementById("hc-toast");
            if (toast) toast.classList.remove("show");
        });
    }

    // =========================
    // Inputs & error spans
    // =========================
    const fullNameInput = document.getElementById("fullName");
    const emailInput = document.getElementById("email");
    const phoneInput = document.getElementById("phone");
    const passwordInput = document.getElementById("password");
    const confirmInput = document.getElementById("confirmPassword");
    const referralInput = document.getElementById("referralCode");

    const fullNameError = document.getElementById("fullNameError");
    const emailError = document.getElementById("emailError");
    const phoneError = document.getElementById("phoneError");
    const passwordError = document.getElementById("passwordError");
    const confirmError = document.getElementById("confirmError");
    const referralError = document.getElementById("referralError");

    const strengthLabel = document.getElementById("passwordStrength");
    const strengthBar = document.getElementById("passwordStrengthBar");

    // =========================
    // Helpers
    // =========================
    function setFieldError(el, msg) { if (el) el.textContent = msg || ""; }
    function clearAllFieldErrors() {
        setFieldError(fullNameError, "");
        setFieldError(emailError, "");
        setFieldError(phoneError, "");
        setFieldError(passwordError, "");
        setFieldError(confirmError, "");
        setFieldError(referralError, "");
    }

    function updatePasswordStrength() {
        if (!passwordInput) return;
        const val = passwordInput.value;
        let score = 0;
        if (val.length >= 8) score++;
        if (/[A-Z]/.test(val)) score++;
        if (/[0-9]/.test(val)) score++;
        if (/[^A-Za-z0-9]/.test(val)) score++;

        let label = "", width = 0, color = "#ff4d4d";
        if (!val) { label = ""; width = 0; }
        else if (score <= 1) { label = "Weak"; width = 33; color = "#ff4d4d"; }
        else if (score <= 3) { label = "Medium"; width = 66; color = "#ffb84d"; }
        else { label = "Strong"; width = 100; color = "#4caf50"; }

        if (strengthLabel) strengthLabel.textContent = label;
        if (strengthBar) { strengthBar.style.width = width + "%"; strengthBar.style.backgroundColor = color; }
    }

    // =========================
    // Validators
    // =========================
    function validateFullName() {
        if (!fullNameInput) return true;
        const value = fullNameInput.value.trim();
        if (!value) { setFieldError(fullNameError, "Full name is mandatory."); return false; }
        if (value.length < 2) { setFieldError(fullNameError, "Full name must be at least 2 characters."); return false; }
        if (!/^[A-Za-z\s]+$/.test(value)) { setFieldError(fullNameError, "Full name must contain only letters."); return false; }
        setFieldError(fullNameError, ""); return true;
    }

    function validateEmail() {
        if (!emailInput) return true;
        const value = emailInput.value.trim();
        if (!value) { setFieldError(emailError, "Email is mandatory."); return false; }
        if (!/^\S+@\S+\.\S+$/.test(value)) { setFieldError(emailError, "Enter a valid email."); return false; }
        setFieldError(emailError, ""); return true;
    }

    function validatePhone() {
        if (!phoneInput) return true;
        const value = phoneInput.value.trim();
        if (!value) { setFieldError(phoneError, "Mobile number is mandatory."); return false; }
        if (!/^\d{10}$/.test(value)) { setFieldError(phoneError, "Enter a valid 10-digit mobile number."); return false; }
        setFieldError(phoneError, ""); return true;
    }

    function validatePasswords() {
        if (!passwordInput && !confirmInput) return true;
        const pwdVal = passwordInput ? passwordInput.value : "";
        const confirmVal = confirmInput ? confirmInput.value : "";
        let ok = true;
        if (passwordInput) {
            if (!pwdVal) { setFieldError(passwordError, "Password is mandatory."); ok = false; }
            else if (pwdVal.length < 8) { setFieldError(passwordError, "Password must be at least 8 characters."); ok = false; }
            else setFieldError(passwordError, "");
        }
        if (confirmInput) {
            if (!confirmVal) { setFieldError(confirmError, "Confirm password is mandatory."); ok = false; }
            else if (pwdVal !== confirmVal) { setFieldError(confirmError, "Passwords must match."); ok = false; }
            else setFieldError(confirmError, "");
        }
        return ok;
    }

    function validateReferral() {
        if (!referralInput) return true;
        const val = referralInput.value.trim();
        if (!val) { setFieldError(referralError, ""); return true; }
        if (/\s/.test(val)) { setFieldError(referralError, "Referral code must not contain spaces."); return false; }
        if (val.toUpperCase() !== "HC1-EF540C") { setFieldError(referralError, "Invalid referral code."); return false; }
        setFieldError(referralError, ""); return true;
    }

    // =========================
    // Attach live events safely
    // =========================
    if (fullNameInput) fullNameInput.addEventListener("input", validateFullName);
    if (emailInput) emailInput.addEventListener("input", validateEmail);
    if (phoneInput) phoneInput.addEventListener("input", validatePhone);
    if (passwordInput) passwordInput.addEventListener("input", () => { updatePasswordStrength(); validatePasswords(); });
    if (confirmInput) confirmInput.addEventListener("input", validatePasswords);
    if (referralInput) referralInput.addEventListener("input", validateReferral);

    // =========================
    // Submit validation
    // =========================
    form.addEventListener("submit", function (e) {
        clearAllFieldErrors();
        if (clientError) { clientError.classList.add("d-none"); clientError.innerHTML = ""; }
        if (serverError) { serverError.classList.add("d-none"); }

        const okName = validateFullName();
        const okEmail = validateEmail();
        const okPhone = validatePhone();
        updatePasswordStrength();
        const okPwd = validatePasswords();
        const okRef = validateReferral();

        if (!(okName && okEmail && okPhone && okPwd && okRef)) {
            e.preventDefault();
            if (clientError) {
                clientError.classList.remove("d-none");
                clientError.innerHTML = "Please check the form. Some fields are invalid.";
            }
            showFriendlyPopup("Oops! Some details are missing or incorrect. Please fix the fields.");
        }
    });

    // =========================
    // Toggle password visibility
    // =========================
    document.querySelectorAll(".hc-toggle-password").forEach(function (toggle) {
        toggle.addEventListener("click", function () {
            const targetId = this.getAttribute("data-target");
            const input = document.getElementById(targetId);
            if (!input) return;
            const icon = this.querySelector("i");
            if (input.type === "password") { input.type = "text"; icon.classList.remove("fa-eye"); icon.classList.add("fa-eye-slash"); }
            else { input.type = "password"; icon.classList.remove("fa-eye-slash"); icon.classList.add("fa-eye"); }
        });
    });

});

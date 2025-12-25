document.addEventListener("DOMContentLoaded", function () {
    const togglePassword = document.querySelector(".toggle-password");
    const passwordInput = document.querySelector("#password");

    if (!togglePassword || !passwordInput) return;

    togglePassword.addEventListener("click", function () {
        const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
        passwordInput.setAttribute("type", type);

        const icon = this.querySelector("i");
        if (!icon) return;

        icon.classList.toggle("fa-eye-slash");
        icon.classList.toggle("fa-eye");
    });
});

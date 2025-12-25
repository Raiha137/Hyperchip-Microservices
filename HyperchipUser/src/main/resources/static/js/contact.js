document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("supportForm");
    const clearBtn = document.getElementById("clearBtn");

    if (!form) return;

    const fields = ["name", "email", "subject", "message"];
    const fieldErrors = {
        name: "Name is required",
        email: "Email is required",
        subject: "Subject is required",
        message: "Message is required"
    };

    // Clear button
    clearBtn?.addEventListener("click", () => {
        form.reset();
        fields.forEach(f => {
            const errDiv = document.getElementById(f + "Error");
            if (errDiv) {
                errDiv.innerText = "";
                errDiv.style.display = "none";
            }
        });
    });

    // Submit validation
    form.addEventListener("submit", function (e) {
        e.preventDefault();
        let isValid = true;

        // Reset all errors
        fields.forEach(f => {
            const errDiv = document.getElementById(f + "Error");
            if (errDiv) {
                errDiv.innerText = "";
                errDiv.style.display = "none";
            }
        });

        // Full Name
        const name = document.getElementById("name").value.trim();
        if (!name) {
            const errDiv = document.getElementById("nameError");
            if (errDiv) {
                errDiv.innerText = fieldErrors.name;
                errDiv.style.display = "block";
            }
            isValid = false;
        }

        // Email
        const email = document.getElementById("email").value.trim();
        if (!email) {
            const errDiv = document.getElementById("emailError");
            if (errDiv) {
                errDiv.innerText = fieldErrors.email;
                errDiv.style.display = "block";
            }
            isValid = false;
        } else {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                const errDiv = document.getElementById("emailError");
                if (errDiv) {
                    errDiv.innerText = "Please enter a valid email address";
                    errDiv.style.display = "block";
                }
                isValid = false;
            }
        }

        // Subject
        const subject = document.getElementById("subject").value.trim();
        if (!subject) {
            const errDiv = document.getElementById("subjectError");
            if (errDiv) {
                errDiv.innerText = fieldErrors.subject;
                errDiv.style.display = "block";
            }
            isValid = false;
        }

        // Message
        const message = document.getElementById("message").value.trim();
        if (!message) {
            const errDiv = document.getElementById("messageError");
            if (errDiv) {
                errDiv.innerText = fieldErrors.message;
                errDiv.style.display = "block";
            }
            isValid = false;
        }

        if (isValid) form.submit();
    });
});

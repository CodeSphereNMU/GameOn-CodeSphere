/**
 * Login page script.
 * Handles form submission, password toggle, validation, and API interaction.
 */
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('login-form');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginBtn = document.getElementById('login-btn');
    const errorDiv = document.getElementById('login-error');
    const passwordToggle = document.querySelector('.password-toggle');

    let isSubmitting = false;

    // Password visibility toggle
    passwordToggle.addEventListener('click', () => {
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        passwordToggle.title = isPassword ? 'Hide password' : 'Show password';
        passwordToggle.querySelector('.icon-eye').classList.toggle('hidden');
        passwordToggle.querySelector('.icon-eye-off').classList.toggle('hidden');
    });

    // Form submission
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (isSubmitting) {
            return;
        }

        hideError();

        const username = usernameInput.value.trim();
        const password = passwordInput.value;

        // Client-side validation
        if (!username || !password) {
            showError('Please enter both username and password.');
            return;
        }

        isSubmitting = true;
        loginBtn.disabled = true;
        loginBtn.textContent = 'Signing in...';

        try {
            const result = await Api.post('/api/auth/login', { username, password });

            if (result.success) {
                window.location.href = '/pages/dashboard.html';
            } else {
                showError(result.error || 'Invalid username or password');
            }
        } catch (err) {
            showError('Unable to reach the server. Please try again.');
        } finally {
            isSubmitting = false;
            loginBtn.disabled = false;
            loginBtn.textContent = 'Login';
        }
    });

    function showError(message) {
        errorDiv.textContent = message;
        errorDiv.classList.remove('hidden');
    }

    function hideError() {
        errorDiv.textContent = '';
        errorDiv.classList.add('hidden');
    }
});

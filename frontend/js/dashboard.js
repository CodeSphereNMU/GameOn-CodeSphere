/**
 * Dashboard page script.
 * Calls GET /api/auth/me to verify the session and display user info.
 */
document.addEventListener('DOMContentLoaded', async () => {
    const loadingDiv = document.getElementById('dashboard-loading');
    const infoDiv = document.getElementById('dashboard-info');
    const errorDiv = document.getElementById('dashboard-error');

    try {
        const result = await Api.get('/api/auth/me');

        if (result.success && result.data) {
            document.getElementById('display-username').textContent = result.data.username;
            document.getElementById('display-user-id').textContent = result.data.userId;
            document.getElementById('display-user-type').textContent = result.data.typeOfUser || 'player';

            loadingDiv.classList.add('hidden');
            infoDiv.classList.remove('hidden');
        } else {
            loadingDiv.classList.add('hidden');
            errorDiv.classList.remove('hidden');
        }
    } catch (err) {
        loadingDiv.classList.add('hidden');
        errorDiv.classList.remove('hidden');
    }
});

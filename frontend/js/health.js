/**
 * Health page script.
 * Fetches /api/health on load and displays the result.
 */
document.addEventListener('DOMContentLoaded', async () => {
    const indicator = document.getElementById('status-indicator');
    const details = document.getElementById('health-details');

    try {
        const result = await Api.get('/api/health');

        if (result.success && result.data) {
            const data = result.data;
            indicator.textContent = data.status === 'healthy' ? 'All Systems Operational' : 'Degraded';
            indicator.className = 'status-indicator ' + data.status;

            details.innerHTML = `
                <dt>Application</dt>
                <dd>${data.application}</dd>
                <dt>Version</dt>
                <dd>${data.version}</dd>
                <dt>Database</dt>
                <dd>${data.database}</dd>
            `;
        } else {
            indicator.textContent = 'Error';
            indicator.className = 'status-indicator error';
            details.innerHTML = '<dt>Details</dt><dd>' + (result.error || 'Unknown error') + '</dd>';
        }
    } catch (err) {
        indicator.textContent = 'Unreachable';
        indicator.className = 'status-indicator error';
        details.innerHTML = '<dt>Details</dt><dd>Could not connect to API server.</dd>';
    }
});

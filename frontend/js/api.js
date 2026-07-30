/**
 * GameOn API helper.
 * Centralises fetch calls so every page script uses consistent error handling.
 */
const Api = {
    /**
     * Makes a GET request to the given API path.
     * @param {string} path - e.g. "/api/health"
     * @returns {Promise<object>} Parsed JSON response
     */
    async get(path) {
        const response = await fetch(path, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        return response.json();
    },

    /**
     * Makes a POST request with a JSON body.
     * @param {string} path - e.g. "/api/auth/login"
     * @param {object} body - Request payload
     * @returns {Promise<object>} Parsed JSON response
     */
    async post(path, body) {
        const response = await fetch(path, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(body)
        });
        return response.json();
    },

    /**
     * Makes a PUT request with a JSON body.
     * @param {string} path
     * @param {object} body
     * @returns {Promise<object>}
     */
    async put(path, body) {
        const response = await fetch(path, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(body)
        });
        return response.json();
    },

    /**
     * Makes a DELETE request.
     * @param {string} path
     * @returns {Promise<object>}
     */
    async delete(path) {
        const response = await fetch(path, {
            method: 'DELETE',
            headers: { 'Accept': 'application/json' }
        });
        return response.json();
    }
};

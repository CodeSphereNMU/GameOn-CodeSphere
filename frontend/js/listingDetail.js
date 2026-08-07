/**
 * GameOn - Listing Detail page script.
 * Fetches and displays full listing details including team roster.
 */
(function () {
    'use strict';

    // DOM references
    const loadingEl = document.getElementById('detail-loading');
    const errorEl = document.getElementById('detail-error');
    const errorMessageEl = document.getElementById('detail-error-message');
    const contentEl = document.getElementById('detail-content');

    const detailSport = document.getElementById('detail-sport');
    const detailFormat = document.getElementById('detail-format');
    const detailDate = document.getElementById('detail-date');
    const detailTime = document.getElementById('detail-time');
    const detailLocation = document.getElementById('detail-location');
    const detailSkill = document.getElementById('detail-skill');
    const detailCapacity = document.getElementById('detail-capacity');
    const detailCreator = document.getElementById('detail-creator');

    const rosterTeamA = document.getElementById('roster-team-a');
    const rosterTeamB = document.getElementById('roster-team-b');

    // Initialisation
    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        const listingId = getListingIdFromUrl();

        if (!listingId) {
            showError('Invalid listing. No listing ID provided.');
            return;
        }

        await loadListingDetail(listingId);
    }

    /**
     * Extracts the listing ID from the URL query parameter.
     * @returns {string|null}
     */
    function getListingIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get('id');
    }

    /**
     * Fetches listing detail from the API and renders the content.
     * @param {string} listingId
     */
    async function loadListingDetail(listingId) {
        showLoading();

        try {
            const res = await Api.get('/api/game-listings/' + encodeURIComponent(listingId));

            if (!res.success) {
                if (res.error && res.error.includes('Login required')) {
                    window.location.href = '../index.html';
                    return;
                }
                showError(res.error || 'Failed to load listing details.');
                return;
            }

            renderDetail(res.data);
            showContent();
        } catch (err) {
            showError('Something went wrong. Please try again.');
        }
    }

    /**
     * Renders the listing detail data into the page.
     * @param {object} data - ListingDetailDto from the API
     */
    function renderDetail(data) {
        detailSport.textContent = data.sportName;
        detailFormat.textContent = data.formatName;
        detailDate.textContent = data.date;
        detailTime.textContent = data.sessionWindow;
        detailLocation.textContent = data.location;
        detailCreator.textContent = data.creatorUsername;

        // Skill level badge
        const skillClass = 'skill-badge skill-badge--' + data.skillLevel.toLowerCase();
        detailSkill.innerHTML = '<span class="' + escapeHtml(skillClass) + '">' + escapeHtml(data.skillLevel) + '</span>';

        // Capacity
        detailCapacity.textContent = data.spotsFilled + ' / ' + data.totalSpots + ' players';

        // Render roster
        renderRoster(rosterTeamA, data.teamA, data.hasPositions);
        renderRoster(rosterTeamB, data.teamB, data.hasPositions);
    }

    /**
     * Renders a team roster list.
     * @param {HTMLElement} listEl - The <ul> element for the team
     * @param {Array} players - Array of RosterEntryDto objects
     * @param {boolean} hasPositions - Whether to show position names
     */
    function renderRoster(listEl, players, hasPositions) {
        listEl.innerHTML = '';

        if (!players || players.length === 0) {
            const emptyLi = document.createElement('li');
            emptyLi.className = 'listing-detail-roster__empty';
            emptyLi.textContent = 'No players yet';
            listEl.appendChild(emptyLi);
            return;
        }

        players.forEach(function (player) {
            const li = document.createElement('li');
            li.className = 'listing-detail-roster__player';

            let content = '<span class="listing-detail-roster__username">' + escapeHtml(player.username) + '</span>';

            if (hasPositions && player.positionName) {
                content += '<span class="listing-detail-roster__position">' + escapeHtml(player.positionName) + '</span>';
            }

            li.innerHTML = content;
            listEl.appendChild(li);
        });
    }

    // ---- UI State Helpers ----

    function showLoading() {
        loadingEl.classList.remove('hidden');
        errorEl.classList.add('hidden');
        contentEl.classList.add('hidden');
    }

    function showError(message) {
        loadingEl.classList.add('hidden');
        errorEl.classList.remove('hidden');
        contentEl.classList.add('hidden');
        errorMessageEl.textContent = message;
    }

    function showContent() {
        loadingEl.classList.add('hidden');
        errorEl.classList.add('hidden');
        contentEl.classList.remove('hidden');
    }

    // ---- Utility ----

    /**
     * Escapes HTML entities to prevent XSS when injecting text into the DOM.
     * @param {string} text
     * @returns {string}
     */
    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

})();

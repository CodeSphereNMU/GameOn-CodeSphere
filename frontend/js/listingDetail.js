/**
 * GameOn - Listing Detail page script.
 * Fetches and displays full listing details including team roster.
 * Handles join-request logic:
 *  - Positional formats: redirects to join-request.html for position selection.
 *  - Non-positional formats: immediately submits the join request from this page.
 */
(function () {
    'use strict';

    // DOM references - page state
    const loadingEl = document.getElementById('detail-loading');
    const errorEl = document.getElementById('detail-error');
    const errorMessageEl = document.getElementById('detail-error-message');
    const contentEl = document.getElementById('detail-content');

    // DOM references - listing details
    const detailSport = document.getElementById('detail-sport');
    const detailFormat = document.getElementById('detail-format');
    const detailDate = document.getElementById('detail-date');
    const detailTime = document.getElementById('detail-time');
    const detailLocation = document.getElementById('detail-location');
    const detailSkill = document.getElementById('detail-skill');
    const detailCapacity = document.getElementById('detail-capacity');
    const detailCreator = document.getElementById('detail-creator');

    // DOM references - roster
    const rosterTeamA = document.getElementById('roster-team-a');
    const rosterTeamB = document.getElementById('roster-team-b');

    // DOM references - join actions
    const joinTeamAAction = document.getElementById('join-team-a-action');
    const joinTeamBAction = document.getElementById('join-team-b-action');
    const btnJoinTeamA = document.getElementById('btn-join-team-a');
    const btnJoinTeamB = document.getElementById('btn-join-team-b');
    const joinRequestError = document.getElementById('join-request-error');
    const joinRequestErrorMessage = document.getElementById('join-request-error-message');
    const joinRequestPending = document.getElementById('join-request-pending');
    const joinRequestSuccess = document.getElementById('join-request-success');

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
            initJoinActions(res.data, listingId);
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

            if (hasPositions) {
                const positionText = formatPosition(player.positionName, player.alternatePositionName);
                content += '<span class="listing-detail-roster__position">' + escapeHtml(positionText) + '</span>';
            }

            li.innerHTML = content;
            listEl.appendChild(li);
        });
    }

    /**
     * Formats the position display string.
     * @param {string|null} primary
     * @param {string|null} alternate
     * @returns {string}
     */
    function formatPosition(primary, alternate) {
        if (primary && alternate) {
            return primary + ' / ' + alternate;
        }
        if (primary) {
            return primary;
        }
        if (alternate) {
            return alternate;
        }
        return 'Any Position';
    }

    // ---- Join Action Logic ----

    /**
     * Initialises join buttons based on user status.
     * @param {object} data - ListingDetailDto from the API
     * @param {string} listingId - The listing ID from the URL
     */
    function initJoinActions(data, listingId) {
        // Creator or already accepted participant - no join buttons
        if (data.creator || data.acceptedParticipant) {
            return;
        }

        // Player already has a pending request - show pending badge
        if (data.hasPendingRequest) {
            joinRequestPending.classList.remove('hidden');
            return;
        }

        // Eligible player - show join buttons beneath each roster
        joinTeamAAction.classList.remove('hidden');
        joinTeamBAction.classList.remove('hidden');

        // Attach click handlers
        btnJoinTeamA.addEventListener('click', function () {
            handleJoinTeamClick('A', data, listingId);
        });

        btnJoinTeamB.addEventListener('click', function () {
            handleJoinTeamClick('B', data, listingId);
        });
    }

    /**
     * Handles a click on Join Team A or Join Team B.
     * - If the format has positions, redirect to join-request.html.
     * - If not, immediately submit the join request.
     * @param {string} team - 'A' or 'B'
     * @param {object} data - ListingDetailDto
     * @param {string} listingId
     */
    function handleJoinTeamClick(team, data, listingId) {
        if (data.hasPositions) {
            // Redirect to the position-selection page
            window.location.href = 'join-request.html?id=' + encodeURIComponent(listingId) + '&team=' + encodeURIComponent(team);
        } else {
            // Immediately submit with no positions
            submitImmediateJoinRequest(team, listingId);
        }
    }

    /**
     * Submits a join request directly (non-positional formats).
     * Disables buttons during processing and handles success/failure.
     * @param {string} team - 'A' or 'B'
     * @param {string} listingId
     */
    async function submitImmediateJoinRequest(team, listingId) {
        // Disable both buttons to prevent repeated clicks
        btnJoinTeamA.disabled = true;
        btnJoinTeamB.disabled = true;
        btnJoinTeamA.textContent = 'Sending...';
        btnJoinTeamB.textContent = 'Sending...';
        hideJoinRequestError();

        try {
            const res = await Api.post('/api/game-listings/' + encodeURIComponent(listingId) + '/join-requests', {
                team: team,
                anyPosition: false,
                positionId: null,
                alternatePositionId: null
            });

            if (res.success) {
                // Hide join buttons, show success then pending
                joinTeamAAction.classList.add('hidden');
                joinTeamBAction.classList.add('hidden');
                joinRequestSuccess.classList.remove('hidden');
                // Also show pending status
                joinRequestPending.classList.remove('hidden');
            } else {
                // Server returned an error - restore buttons
                showJoinRequestError(res.error || 'Failed to submit join request.');
                restoreJoinButtons();
            }
        } catch (err) {
            showJoinRequestError('Something went wrong. Please try again.');
            restoreJoinButtons();
        }
    }

    /**
     * Restores join buttons to their original state after a failure.
     */
    function restoreJoinButtons() {
        btnJoinTeamA.disabled = false;
        btnJoinTeamB.disabled = false;
        btnJoinTeamA.textContent = 'Join Team A';
        btnJoinTeamB.textContent = 'Join Team B';
    }

    /**
     * Shows the join request error message.
     * @param {string} message
     */
    function showJoinRequestError(message) {
        joinRequestError.classList.remove('hidden');
        joinRequestErrorMessage.textContent = message;
    }

    /**
     * Hides the join request error message.
     */
    function hideJoinRequestError() {
        joinRequestError.classList.add('hidden');
        joinRequestErrorMessage.textContent = '';
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

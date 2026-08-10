/**
 * GameOn - Join Request page script (positional formats).
 * Loads positions for the listing's format and submits a join request
 * with the selected preferred and alternate positions, or "Any Position".
 *
 * URL parameters:
 *   id   - the game listing ID (required)
 *   team - 'A' or 'B' (required)
 */
(function () {
    'use strict';

    // Special value used to indicate "Any Position" selection
    const ANY_POSITION_VALUE = 'ANY';

    // DOM references
    const loadingEl = document.getElementById('jr-loading');
    const errorEl = document.getElementById('jr-error');
    const errorMessageEl = document.getElementById('jr-error-message');
    const errorBackLink = document.getElementById('jr-error-back');
    const contentEl = document.getElementById('jr-content');
    const backLink = document.getElementById('back-link');
    const cancelLink = document.getElementById('jr-cancel');

    const teamBadge = document.getElementById('jr-team-badge');
    const form = document.getElementById('jr-form');
    const positionPrimary = document.getElementById('jr-position-primary');
    const positionAlternate = document.getElementById('jr-position-alternate');
    const submitBtn = document.getElementById('jr-submit');
    const formError = document.getElementById('jr-form-error');
    const formErrorMessage = document.getElementById('jr-form-error-message');

    // Initialisation
    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        const params = new URLSearchParams(window.location.search);
        const listingId = params.get('id');
        const team = params.get('team');

        // Validate query parameters
        if (!listingId || !team) {
            showError('Invalid request. Missing listing ID or team selection.');
            return;
        }

        if (team !== 'A' && team !== 'B') {
            showError('Invalid team selection. Must be Team A or Team B.');
            return;
        }

        // Set the back/cancel links to point to the listing detail page
        const detailUrl = 'listing-detail.html?id=' + encodeURIComponent(listingId);
        backLink.href = detailUrl;
        cancelLink.href = detailUrl;
        errorBackLink.href = detailUrl;

        // Display team indicator
        teamBadge.textContent = 'Team ' + team;
        teamBadge.classList.add(team === 'A' ? 'join-request__team-badge--a' : 'join-request__team-badge--b');

        // Fetch listing detail to get formatId
        await loadListingAndPositions(listingId, team);
    }

    /**
     * Fetches the listing detail to get the formatId, then loads positions.
     * @param {string} listingId
     * @param {string} team
     */
    async function loadListingAndPositions(listingId, team) {
        showLoading();

        try {
            const listingRes = await Api.get('/api/game-listings/' + encodeURIComponent(listingId));

            if (!listingRes.success) {
                if (listingRes.error && listingRes.error.includes('Login required')) {
                    window.location.href = '../index.html';
                    return;
                }
                showError(listingRes.error || 'Failed to load listing details.');
                return;
            }

            const data = listingRes.data;

            // Verify listing actually has positions
            if (!data.hasPositions || !data.formatId) {
                showError('This listing does not use positions. Please go back and try again.');
                return;
            }

            // Load positions for the format
            const positionsLoaded = await loadPositions(data.formatId);

            if (!positionsLoaded) {
                showError('Could not load positions for this format. Please try again later.');
                return;
            }

            // Show the form
            showContent();

            // Attach form submission handler
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                handleSubmit(listingId, team);
            });

            // Attach preferred-position change handler
            positionPrimary.addEventListener('change', handlePrimaryChange);

        } catch (err) {
            showError('Something went wrong loading positions. Please try again.');
        }
    }

    /**
     * Fetches positions for the format and populates both select dropdowns.
     * Adds "Any Position" as the first selectable option in the preferred dropdown.
     * Returns true if positions were loaded successfully, false otherwise.
     * @param {number} formatId
     * @returns {Promise<boolean>}
     */
    async function loadPositions(formatId) {
        const res = await Api.get('/api/formats/' + encodeURIComponent(formatId) + '/positions');

        if (!res.success || !res.data || res.data.length === 0) {
            return false;
        }

        // Add "Any Position" option to preferred position dropdown
        const anyOption = document.createElement('option');
        anyOption.value = ANY_POSITION_VALUE;
        anyOption.textContent = 'Any Position';
        positionPrimary.appendChild(anyOption);

        // Add specific positions to both dropdowns
        res.data.forEach(function (position) {
            const primaryOption = document.createElement('option');
            primaryOption.value = position.positionId;
            primaryOption.textContent = position.positionName;
            positionPrimary.appendChild(primaryOption);

            const alternateOption = document.createElement('option');
            alternateOption.value = position.positionId;
            alternateOption.textContent = position.positionName;
            positionAlternate.appendChild(alternateOption);
        });

        return true;
    }

    /**
     * Handles changes to the preferred position dropdown.
     * When "Any Position" is selected, disables and clears the alternate dropdown.
     * When a specific position is selected, enables the alternate and filters out duplicates.
     */
    function handlePrimaryChange() {
        const selectedPrimary = positionPrimary.value;

        if (selectedPrimary === ANY_POSITION_VALUE) {
            // Any Position selected: disable and clear alternate
            positionAlternate.value = '';
            positionAlternate.disabled = true;
        } else {
            // Specific position or empty: enable alternate
            positionAlternate.disabled = false;
            filterAlternatePosition();
        }
    }

    /**
     * Disables the currently selected preferred position in the alternate dropdown
     * to prevent selecting the same position for both.
     */
    function filterAlternatePosition() {
        const selectedPrimary = positionPrimary.value;

        // Reset all alternate options to enabled
        const alternateOptions = positionAlternate.querySelectorAll('option');
        alternateOptions.forEach(function (option) {
            option.disabled = false;
        });

        // If a specific primary is selected, disable it in the alternate dropdown
        if (selectedPrimary && selectedPrimary !== ANY_POSITION_VALUE) {
            const matchingOption = positionAlternate.querySelector('option[value="' + selectedPrimary + '"]');
            if (matchingOption) {
                matchingOption.disabled = true;
                // If the alternate was set to the same value, reset it
                if (positionAlternate.value === selectedPrimary) {
                    positionAlternate.value = '';
                }
            }
        }
    }

    /**
     * Handles the form submission.
     * @param {string} listingId
     * @param {string} team
     */
    async function handleSubmit(listingId, team) {
        hideFormError();

        // Validate preferred position is selected
        const primaryValue = positionPrimary.value;
        if (!primaryValue) {
            showFormError('Please select a preferred position or Any Position.');
            return;
        }

        const isAnyPosition = (primaryValue === ANY_POSITION_VALUE);

        if (!isAnyPosition) {
            const alternateValue = positionAlternate.value;
            // Validate alternate is not the same as primary
            if (alternateValue && alternateValue === primaryValue) {
                showFormError('Alternate position cannot be the same as the preferred position.');
                return;
            }
        }

        // Build request payload
        let payload;
        if (isAnyPosition) {
            payload = {
                team: team,
                anyPosition: true,
                positionId: null,
                alternatePositionId: null
            };
        } else {
            const alternateValue = positionAlternate.value;
            payload = {
                team: team,
                anyPosition: false,
                positionId: Number(primaryValue),
                alternatePositionId: alternateValue ? Number(alternateValue) : null
            };
        }

        // Disable submit
        submitBtn.textContent = 'Sending...';
        submitBtn.disabled = true;

        try {
            const res = await Api.post('/api/game-listings/' + encodeURIComponent(listingId) + '/join-requests', payload);

            if (res.success) {
                // Show success message briefly, then redirect back to listing detail
                showSuccessAndRedirect(listingId);
            } else {
                showFormError(res.error || 'Failed to submit join request.');
                submitBtn.textContent = 'Send Join Request';
                submitBtn.disabled = false;
            }
        } catch (err) {
            showFormError('Something went wrong. Please try again.');
            submitBtn.textContent = 'Send Join Request';
            submitBtn.disabled = false;
        }
    }

    /**
     * Shows a success message and redirects back to the listing detail page.
     * @param {string} listingId
     */
    function showSuccessAndRedirect(listingId) {
        // Replace form content with success message
        contentEl.innerHTML =
            '<section class="join-request-success" aria-label="Join request success" role="status">' +
            '  <div class="join-request-success__badge">' +
            '    <span class="join-request-success__icon" aria-hidden="true">&#10003;</span>' +
            '    <span class="join-request-success__text">Join Request Sent!</span>' +
            '  </div>' +
            '  <p class="join-request-success__info">Your request has been submitted. Returning to listing...</p>' +
            '</section>';

        // Redirect after a brief delay
        setTimeout(function () {
            window.location.href = 'listing-detail.html?id=' + encodeURIComponent(listingId);
        }, 1500);
    }

    // ---- Error Helpers ----

    function showFormError(message) {
        formError.classList.remove('hidden');
        formErrorMessage.textContent = message;
    }

    function hideFormError() {
        formError.classList.add('hidden');
        formErrorMessage.textContent = '';
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

})();

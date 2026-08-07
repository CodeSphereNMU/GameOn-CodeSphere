/**
 * GameOn - Browse Listings page script.
 * Fetches paginated, filtered game listings and renders them as cards.
 */
(function () {
    'use strict';

    // State
    let currentPage = 1;
    let totalPages = 1;
    const pageSize = 20;

    // DOM references
    const filterSport = document.getElementById('filter-sport');
    const filterSkill = document.getElementById('filter-skill');
    const filterDate = document.getElementById('filter-date');
    const toggleShowFull = document.getElementById('toggle-show-full');

    const loadingEl = document.getElementById('browse-loading');
    const errorEl = document.getElementById('browse-error');
    const errorMessageEl = document.getElementById('browse-error-message');
    const emptyEl = document.getElementById('browse-empty');
    const listingsGrid = document.getElementById('listings-grid');

    const paginationControls = document.getElementById('pagination-controls');
    const btnPrev = document.getElementById('btn-prev-page');
    const btnNext = document.getElementById('btn-next-page');
    const paginationIndicator = document.getElementById('pagination-indicator');

    // Initialisation
    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        try {
            await loadUserSports();
        } catch (err) {
            showError('Failed to load your sports. Please try again.');
            return;
        }

        // Attach filter change listeners
        filterSport.addEventListener('change', resetAndLoad);
        filterSkill.addEventListener('change', resetAndLoad);
        filterDate.addEventListener('change', resetAndLoad);
        toggleShowFull.addEventListener('change', resetAndLoad);

        // Pagination buttons
        btnPrev.addEventListener('click', goToPreviousPage);
        btnNext.addEventListener('click', goToNextPage);

        // Initial load
        await loadListings();
    }

    /**
     * Fetches the user's sports and populates the sport filter dropdown.
     */
    async function loadUserSports() {
        const res = await Api.get('/api/users/me/sports');

        if (!res.success) {
            if (res.error && res.error.includes('Login required')) {
                window.location.href = '../index.html';
                return;
            }
            throw new Error(res.error || 'Failed to load sports');
        }

        const sports = res.data || [];
        sports.forEach(function (sport) {
            const option = document.createElement('option');
            option.value = sport.sportId;
            option.textContent = sport.sportName;
            filterSport.appendChild(option);
        });
    }

    /**
     * Resets to page 1 and reloads listings when a filter changes.
     */
    function resetAndLoad() {
        currentPage = 1;
        loadListings();
    }

    /**
     * Builds the query string from active filters and fetches listings.
     */
    async function loadListings() {
        showLoading();

        const params = new URLSearchParams();
        params.set('page', currentPage.toString());
        params.set('size', pageSize.toString());

        // Sport filter
        const sportId = filterSport.value;
        if (sportId) {
            params.set('sportId', sportId);
        }

        // Skill level filter
        const skillLevel = filterSkill.value;
        if (skillLevel) {
            params.set('skillLevel', skillLevel);
        }

        // Date filter
        const date = filterDate.value;
        if (date) {
            params.set('date', date);
        }

        // "Show full listings" toggle: when OFF → hideFull=true
        if (!toggleShowFull.checked) {
            params.set('hideFull', 'true');
        }

        try {
            const res = await Api.get('/api/game-listings?' + params.toString());

            if (!res.success) {
                if (res.error && res.error.includes('Login required')) {
                    window.location.href = '../index.html';
                    return;
                }
                showError(res.error || 'Failed to load listings.');
                return;
            }

            const data = res.data;
            totalPages = data.totalPages || 1;
            currentPage = data.page || 1;

            const items = data.items || [];

            if (items.length === 0) {
                showEmpty();
            } else {
                renderListings(items);
                showGrid();
            }

            updatePagination();
        } catch (err) {
            showError('Something went wrong. Please try again.');
        }
    }

    /**
     * Renders listing cards into the grid.
     * @param {Array} listings - Array of BrowseListingDto objects
     */
    function renderListings(listings) {
        listingsGrid.innerHTML = '';

        listings.forEach(function (listing) {
            const card = document.createElement('article');
            card.className = 'listing-card';
            card.setAttribute('role', 'button');
            card.setAttribute('tabindex', '0');
            card.setAttribute('aria-label', listing.sportName + ' ' + listing.formatName + ' on ' + listing.date);

            const isFull = listing.spotsFilled >= listing.totalSpots;

            card.innerHTML =
                '<div class="listing-card__header">' +
                    '<span class="listing-card__sport">' + escapeHtml(listing.sportName) + '</span>' +
                    '<span class="listing-card__format">' + escapeHtml(listing.formatName) + '</span>' +
                '</div>' +
                '<div class="listing-card__body">' +
                    '<p class="listing-card__datetime">' +
                        '<span class="listing-card__date">' + escapeHtml(listing.date) + '</span>' +
                        '<span class="listing-card__time">' + escapeHtml(listing.sessionWindow) + '</span>' +
                    '</p>' +
                    '<p class="listing-card__location">' + escapeHtml(listing.location) + '</p>' +
                    '<p class="listing-card__skill">' +
                        '<span class="skill-badge skill-badge--' + listing.skillLevel.toLowerCase() + '">' +
                            escapeHtml(listing.skillLevel) +
                        '</span>' +
                    '</p>' +
                '</div>' +
                '<div class="listing-card__footer">' +
                    '<span class="listing-card__capacity' + (isFull ? ' listing-card__capacity--full' : '') + '">' +
                        listing.spotsFilled + '/' + listing.totalSpots + ' players' +
                    '</span>' +
                    '<span class="listing-card__creator">by ' + escapeHtml(listing.creatorUsername) + '</span>' +
                '</div>';

            // Navigate to detail on click
            card.addEventListener('click', function () {
                navigateToDetail(listing.gameListingId);
            });

            // Navigate to detail on Enter/Space key
            card.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    navigateToDetail(listing.gameListingId);
                }
            });

            listingsGrid.appendChild(card);
        });
    }

    /**
     * Navigates to the listing detail page for the given listing.
     * @param {number} listingId
     */
    function navigateToDetail(listingId) {
        window.location.href = '../pages/listing-detail.html?id=' + listingId;
    }

    // ---- Pagination ----

    function goToPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadListings();
        }
    }

    function goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadListings();
        }
    }

    function updatePagination() {
        paginationIndicator.textContent = 'Page ' + currentPage + ' of ' + totalPages;

        btnPrev.disabled = currentPage <= 1;
        btnNext.disabled = currentPage >= totalPages;

        if (totalPages > 1) {
            paginationControls.classList.remove('hidden');
        } else {
            paginationControls.classList.add('hidden');
        }
    }

    // ---- UI State Helpers ----

    function showLoading() {
        loadingEl.classList.remove('hidden');
        errorEl.classList.add('hidden');
        emptyEl.classList.add('hidden');
        listingsGrid.classList.add('hidden');
        paginationControls.classList.add('hidden');
    }

    function showError(message) {
        loadingEl.classList.add('hidden');
        errorEl.classList.remove('hidden');
        emptyEl.classList.add('hidden');
        listingsGrid.classList.add('hidden');
        paginationControls.classList.add('hidden');
        errorMessageEl.textContent = message;
    }

    function showEmpty() {
        loadingEl.classList.add('hidden');
        errorEl.classList.add('hidden');
        emptyEl.classList.remove('hidden');
        listingsGrid.classList.add('hidden');
        paginationControls.classList.add('hidden');
    }

    function showGrid() {
        loadingEl.classList.add('hidden');
        errorEl.classList.add('hidden');
        emptyEl.classList.add('hidden');
        listingsGrid.classList.remove('hidden');
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

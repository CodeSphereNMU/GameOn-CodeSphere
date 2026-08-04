/**
 * GameOn - Create Listing multi-step form logic.
 * Handles the 4-step creation flow: Details → Team & Positions → Invite Friends → Confirm.
 */
(function () {
    'use strict';

    // State
    let currentStep = 1;
    let userSports = [];
    let selectedFormat = null;
    let positions = [];
    let friends = [];

    // DOM references
    const steps = {
        1: document.getElementById('step-1'),
        2: document.getElementById('step-2'),
        3: document.getElementById('step-3'),
        4: document.getElementById('step-4')
    };
    const noSportsWarning = document.getElementById('no-sports-warning');

    // Step 1 elements
    const sportSelect = document.getElementById('sport-select');
    const formatSelect = document.getElementById('format-select');
    const skillSelect = document.getElementById('skill-select');
    const dateInput = document.getElementById('date-input');
    const timeInput = document.getElementById('time-input');
    const locationInput = document.getElementById('location-input');

    // Initialisation
    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        try {
            const res = await Api.get('/api/users/me/sports');
            if (!res.success) {
                if (res.error && res.error.includes('Login required')) {
                    window.location.href = '../index.html';
                    return;
                }
                showError('step1-error', res.error || 'Failed to load sports');
                return;
            }

            userSports = res.data || [];
            if (userSports.length === 0) {
                steps[1].classList.add('hidden');
                noSportsWarning.classList.remove('hidden');
                return;
            }

            populateSports();
            bindEvents();
            setMinDate();
        } catch (err) {
            showError('step1-error', 'Failed to load page data');
        }
    }

    function populateSports() {
        userSports.forEach(function (sport) {
            const opt = document.createElement('option');
            opt.value = sport.sportId;
            opt.textContent = sport.sportName;
            opt.dataset.skillLevel = sport.skillLevel || '';
            sportSelect.appendChild(opt);
        });
    }

    function setMinDate() {
        const today = new Date().toISOString().split('T')[0];
        dateInput.setAttribute('min', today);
    }

    function bindEvents() {
        sportSelect.addEventListener('change', onSportChange);
        formatSelect.addEventListener('change', onFormatChange);

        document.getElementById('btn-next-1').addEventListener('click', goToStep2);
        document.getElementById('btn-back-2').addEventListener('click', function () { showStep(1); });
        document.getElementById('btn-next-2').addEventListener('click', goToStep3);
        document.getElementById('btn-back-3').addEventListener('click', function () { showStep(2); });
        document.getElementById('btn-next-3').addEventListener('click', goToStep4);
        document.getElementById('btn-back-4').addEventListener('click', function () { showStep(3); });
        document.getElementById('btn-create').addEventListener('click', submitListing);
    }

    async function onSportChange() {
        const sportId = sportSelect.value;
        formatSelect.innerHTML = '<option value="">Select a format</option>';
        formatSelect.disabled = true;
        selectedFormat = null;

        if (!sportId) return;

        const sport = userSports.find(function (s) { return String(s.sportId) === sportId; });
        if (sport && sport.skillLevel) {
            skillSelect.value = sport.skillLevel;
        }

        try {
            const res = await Api.get('/api/sports/' + sportId + '/formats');
            if (res.success && res.data) {
                res.data.forEach(function (f) {
                    const opt = document.createElement('option');
                    opt.value = f.formatId;
                    opt.textContent = f.formatName;
                    opt.dataset.hasPositions = f.hasPositions;
                    opt.dataset.noPlayers = f.noPlayers;
                    opt.dataset.durationMinutes = f.durationMinutes;
                    formatSelect.appendChild(opt);
                });
                formatSelect.disabled = false;
            }
        } catch (err) {
            showError('step1-error', 'Failed to load formats');
        }
    }

    function onFormatChange() {
        const opt = formatSelect.options[formatSelect.selectedIndex];
        if (opt && opt.value) {
            selectedFormat = {
                formatId: parseInt(opt.value),
                formatName: opt.textContent,
                hasPositions: opt.dataset.hasPositions === 'true',
                noPlayers: parseInt(opt.dataset.noPlayers),
                durationMinutes: parseInt(opt.dataset.durationMinutes)
            };
        } else {
            selectedFormat = null;
        }
    }

    // --- Step navigation ---

    function goToStep2() {
        hideError('step1-error');

        if (!sportSelect.value) { showError('step1-error', 'Please select a sport'); return; }
        if (!formatSelect.value || !selectedFormat) { showError('step1-error', 'Please select a format'); return; }
        if (!dateInput.value) { showError('step1-error', 'Please select a date'); return; }
        if (!timeInput.value) { showError('step1-error', 'Please select a time'); return; }
        if (!locationInput.value.trim()) { showError('step1-error', 'Please enter a location'); return; }

        const dt = new Date(dateInput.value + 'T' + timeInput.value);
        if (dt <= new Date()) {
            showError('step1-error', 'Date and time must be in the future');
            return;
        }

        // Show/hide positions section based on format
        const positionsSection = document.getElementById('positions-section');
        if (selectedFormat.hasPositions) {
            positionsSection.classList.remove('hidden');
            loadPositions();
        } else {
            positionsSection.classList.add('hidden');
        }

        showStep(2);
    }

    async function loadPositions() {
        const positionsList = document.getElementById('positions-list');
        positionsList.innerHTML = '';

        try {
            const res = await Api.get('/api/formats/' + selectedFormat.formatId + '/positions');
            if (res.success && res.data) {
                positions = res.data;

                const anyLabel = createCheckbox('pos-any', 'any', 'Any Position');
                positionsList.appendChild(anyLabel);

                positions.forEach(function (p) {
                    const label = createCheckbox('pos-' + p.positionId, p.positionId, p.positionName);
                    positionsList.appendChild(label);
                });

                positionsList.querySelectorAll('input[type="checkbox"]').forEach(function (cb) {
                    cb.addEventListener('change', onPositionChange);
                });
            }
        } catch (err) {
            showError('step2-error', 'Failed to load positions');
        }
    }

    function onPositionChange(e) {
        const checkboxes = document.querySelectorAll('#positions-list input[type="checkbox"]');
        const anyCheckbox = document.getElementById('pos-any');

        if (e.target === anyCheckbox && anyCheckbox.checked) {
            // "Any Position" is mutually exclusive — uncheck all others
            checkboxes.forEach(function (cb) {
                if (cb !== anyCheckbox) cb.checked = false;
            });
        } else if (e.target !== anyCheckbox && e.target.checked) {
            // A specific position was checked — uncheck "Any Position"
            anyCheckbox.checked = false;

            // Enforce max 2 specific positions
            const checked = Array.from(checkboxes).filter(function (cb) { return cb.checked && cb !== anyCheckbox; });
            if (checked.length > 2) {
                e.target.checked = false;
            }
        }
    }

    function goToStep3() {
        hideError('step2-error');

        // Validate positions if format uses them
        if (selectedFormat && selectedFormat.hasPositions) {
            const checked = document.querySelectorAll('#positions-list input[type="checkbox"]:checked');
            if (checked.length === 0) {
                showError('step2-error', 'Please select a position or choose "Any Position"');
                return;
            }
        }

        loadFriends();
        showStep(3);
    }

    async function loadFriends() {
        const friendsList = document.getElementById('friends-list');
        friendsList.innerHTML = '';
        updateInviteCounter();

        try {
            const res = await Api.get('/api/users/me/friends');
            if (res.success && res.data) {
                friends = res.data;

                if (friends.length === 0) {
                    friendsList.innerHTML = '<p class="no-friends-message">No mutual friends found.</p>';
                } else {
                    friends.forEach(function (f) {
                        const label = createFriendCheckbox(f);
                        friendsList.appendChild(label);
                    });

                    friendsList.querySelectorAll('input[type="checkbox"]').forEach(function (cb) {
                        cb.addEventListener('change', updateInviteCounter);
                    });
                }
            }
            updateInviteCounter();
        } catch (err) {
            showError('step3-error', 'Failed to load friends');
        }
    }

    function updateInviteCounter() {
        const checked = document.querySelectorAll('#friends-list input[type="checkbox"]:checked');
        const numSelected = checked ? checked.length : 0;
        const counter = document.getElementById('invite-counter');
        counter.textContent = numSelected + ' selected';
    }

    function goToStep4() {
        hideError('step3-error');
        populateConfirmation();
        showStep(4);
    }

    function populateConfirmation() {
        const sportName = sportSelect.options[sportSelect.selectedIndex].textContent;
        const formatName = selectedFormat.formatName;
        const skill = skillSelect.value;
        const privacy = document.querySelector('input[name="privacy"]:checked').value;
        const team = document.querySelector('input[name="team"]:checked').value;

        document.getElementById('confirm-title').textContent = sportName + ' ' + formatName;
        document.getElementById('confirm-location').textContent = locationInput.value.trim();

        // Session window display
        const sessionWindow = calculateSessionWindow();
        document.getElementById('confirm-datetime').textContent = formatDate(dateInput.value) + ' ' + sessionWindow;
        document.getElementById('confirm-session-window').textContent = 'Session: ' + sessionWindow + ' (' + selectedFormat.durationMinutes + ' min)';
        document.getElementById('confirm-team').textContent = 'Your team: Team ' + team;

        const invitedCount = document.querySelectorAll('#friends-list input[type="checkbox"]:checked').length;
        document.getElementById('confirm-invited').textContent = 'Players: 1/' + selectedFormat.noPlayers + (invitedCount > 0 ? ' (' + invitedCount + ' invited)' : '');

        const skillBadge = document.getElementById('confirm-skill');
        skillBadge.textContent = skill;
        skillBadge.className = 'skill-badge skill-badge--' + skill.toLowerCase();

        const privacyBadge = document.getElementById('confirm-privacy');
        privacyBadge.textContent = privacy === 'public' ? 'Public' : 'Private';
        privacyBadge.className = 'privacy-badge privacy-badge--' + privacy;
    }

    function calculateSessionWindow() {
        if (!timeInput.value || !selectedFormat) return '';
        const parts = timeInput.value.split(':');
        const startHour = parseInt(parts[0]);
        const startMin = parseInt(parts[1]);
        const totalMinutes = startHour * 60 + startMin + selectedFormat.durationMinutes;
        const endHour = Math.floor(totalMinutes / 60) % 24;
        const endMin = totalMinutes % 60;
        return pad(startHour) + ':' + pad(startMin) + '\u2013' + pad(endHour) + ':' + pad(endMin);
    }

    function pad(n) {
        return n < 10 ? '0' + n : '' + n;
    }

    async function submitListing() {
        hideError('step4-error');
        const createBtn = document.getElementById('btn-create');
        createBtn.disabled = true;
        createBtn.textContent = 'Creating...';

        try {
            const payload = buildPayload();
            const res = await Api.post('/api/game-listings', payload);

            if (res.success) {
                createBtn.textContent = 'Created!';
                createBtn.classList.add('btn-success');
                setTimeout(function () {
                    window.location.href = '../pages/dashboard.html';
                }, 1500);
            } else {
                showError('step4-error', res.error || 'Failed to create listing');
                createBtn.disabled = false;
                createBtn.textContent = 'Create Listing';
            }
        } catch (err) {
            showError('step4-error', 'An unexpected error occurred');
            createBtn.disabled = false;
            createBtn.textContent = 'Create Listing';
        }
    }

    function buildPayload() {
        const privacy = document.querySelector('input[name="privacy"]:checked').value;
        const team = document.querySelector('input[name="team"]:checked').value;
        const positionData = getSelectedPositions();
        const invitedIds = getSelectedFriendIds();

        const payload = {
            sportId: parseInt(sportSelect.value),
            formatId: selectedFormat.formatId,
            skillLevel: skillSelect.value,
            date: dateInput.value,
            time: timeInput.value,
            location: locationInput.value.trim(),
            isPrivate: privacy === 'private',
            team: team,
            anyPosition: positionData.anyPosition,
            positionId: positionData.positionId,
            alternatePositionId: positionData.alternatePositionId,
            invitedFriendIds: invitedIds
        };

        return payload;
    }

    function getSelectedPositions() {
        if (!selectedFormat || !selectedFormat.hasPositions) {
            return { anyPosition: false, positionId: null, alternatePositionId: null };
        }

        const anyCheckbox = document.getElementById('pos-any');
        if (anyCheckbox && anyCheckbox.checked) {
            return { anyPosition: true, positionId: null, alternatePositionId: null };
        }

        const checked = Array.from(
            document.querySelectorAll('#positions-list input[type="checkbox"]:checked')
        ).filter(function (cb) { return cb.value !== 'any'; });

        const positionId = checked.length > 0 ? parseInt(checked[0].value) : null;
        const alternatePositionId = checked.length > 1 ? parseInt(checked[1].value) : null;

        return { anyPosition: false, positionId: positionId, alternatePositionId: alternatePositionId };
    }

    function getSelectedFriendIds() {
        const checked = document.querySelectorAll('#friends-list input[type="checkbox"]:checked');
        return Array.from(checked).map(function (cb) { return parseInt(cb.value); });
    }

    // --- Helpers ---

    function showStep(step) {
        Object.keys(steps).forEach(function (key) {
            steps[key].classList.add('hidden');
            steps[key].classList.remove('active');
        });
        steps[step].classList.remove('hidden');
        steps[step].classList.add('active');
        currentStep = step;
        window.scrollTo(0, 0);
    }

    function showError(elementId, message) {
        const el = document.getElementById(elementId);
        el.textContent = message;
        el.classList.remove('hidden');
    }

    function hideError(elementId) {
        const el = document.getElementById(elementId);
        el.textContent = '';
        el.classList.add('hidden');
    }

    function createCheckbox(id, value, labelText) {
        const wrapper = document.createElement('label');
        wrapper.className = 'checkbox-label';
        wrapper.setAttribute('for', id);

        const input = document.createElement('input');
        input.type = 'checkbox';
        input.id = id;
        input.value = value;

        const span = document.createElement('span');
        span.textContent = labelText;

        wrapper.appendChild(input);
        wrapper.appendChild(span);
        return wrapper;
    }

    function createFriendCheckbox(friend) {
        const wrapper = document.createElement('label');
        wrapper.className = 'checkbox-label friend-label';

        const input = document.createElement('input');
        input.type = 'checkbox';
        input.value = friend.userId;

        const avatar = document.createElement('span');
        avatar.className = 'friend-avatar';
        avatar.textContent = friend.username.charAt(0).toUpperCase();

        const name = document.createElement('span');
        name.className = 'friend-name';
        name.textContent = friend.username;

        wrapper.appendChild(input);
        wrapper.appendChild(avatar);
        wrapper.appendChild(name);
        return wrapper;
    }

    function formatDate(isoDate) {
        const parts = isoDate.split('-');
        return parts[2] + '/' + parts[1];
    }
})();

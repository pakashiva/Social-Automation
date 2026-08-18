document.addEventListener('DOMContentLoaded', () => {

    const menuToggle = document.getElementById('menu-toggle');
    const sidebar = document.getElementById('app-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');

    const closeNav = () => {
        document.body.classList.remove('nav-open');
        if (menuToggle) {
            menuToggle.setAttribute('aria-expanded', 'false');
            menuToggle.setAttribute('aria-label', 'Open navigation');
        }
        if (backdrop) {
            backdrop.hidden = true;
        }
    };

    const openNav = () => {
        document.body.classList.add('nav-open');
        if (menuToggle) {
            menuToggle.setAttribute('aria-expanded', 'true');
            menuToggle.setAttribute('aria-label', 'Close navigation');
        }
        if (backdrop) {
            backdrop.hidden = false;
        }
    };

    if (menuToggle && sidebar) {
        menuToggle.addEventListener('click', () => {
            if (document.body.classList.contains('nav-open')) {
                closeNav();
            } else {
                openNav();
            }
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', closeNav);
    }

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeNav();
        }
    });

    const strategyFile = document.getElementById('strategy-file');
    const strategyFilename = document.getElementById('strategy-filename');
    const strategyError = document.getElementById('strategy-error');

    if (strategyFile) {
        strategyFile.addEventListener('change', () => {
            const file = strategyFile.files[0];
            if (!file) {
                strategyFilename.textContent = 'No file selected';
                return;
            }

            if (file.type !== 'application/pdf') {
                strategyError.textContent = 'Please upload a PDF document.';
                strategyFile.value = '';
                strategyFilename.textContent = 'No file selected';
            } else {
                strategyError.textContent = '';
                strategyFilename.textContent = file.name;
            }
        });
    }

    const brandContext = document.getElementById('brand-context');
    const contextCounter = document.getElementById('context-counter');
    const contextError = document.getElementById('context-error');

    if (brandContext) {
        const updateCounter = () => {
            const length = brandContext.value.length;
            contextCounter.textContent = `${length} / 10000`;
            if (length < 100) {
                contextError.textContent = 'Brand Context must contain at least 100 characters.';
            } else {
                contextError.textContent = '';
            }
        };

        brandContext.addEventListener('input', updateCounter);
        updateCounter();
    }

    const companyForm = document.getElementById('company-form');

    const scheduleForm = document.getElementById('schedule-form');
    const scheduleInput = document.getElementById('schedule-input');
    const scheduleError = document.getElementById('schedule-error');
    const scheduleErrorGlobal = document.getElementById('schedule-error-global');


    const calendar = document.getElementById('calendar');
    const selectedDatesList = document.getElementById('selected-dates');
    let selectedDates = [];

    if (calendar) {
        const today = new Date();
        const year = today.getFullYear();
        const month = today.getMonth();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const startDay = new Date(year, month, 1).getDay();

        for (let i = 0; i < startDay; i++) {
            const placeholder = document.createElement('div');
            placeholder.className = 'calendar-day';
            placeholder.style.visibility = 'hidden';
            calendar.appendChild(placeholder);
        }

        for (let day = 1; day <= daysInMonth; day++) {
            const dayButton = document.createElement('button');
            dayButton.type = 'button';
            dayButton.className = 'calendar-day';
            dayButton.textContent = day;
            dayButton.dataset.date = new Date(year, month, day).toISOString().split('T')[0];

            dayButton.addEventListener('click', () => {
                const dateString = dayButton.dataset.date;
                if (selectedDates.includes(dateString)) {
                    selectedDates = selectedDates.filter(date => date !== dateString);
                } else {
                    selectedDates.push(dateString);
                }
                selectedDates.sort();
                updateCalendarSelection();
                renderSelectedDates();
            });

            calendar.appendChild(dayButton);
        }

        const updateCalendarSelection = () => {
            const dayButtons = calendar.querySelectorAll('.calendar-day');
            dayButtons.forEach(button => {
                const dateString = button.dataset.date;
                if (!dateString) return;
                button.classList.toggle('selected', selectedDates.includes(dateString));
            });
        };

        const renderSelectedDates = () => {
            selectedDatesList.innerHTML = '';
            if (selectedDates.length === 0) {
                selectedDatesList.innerHTML = '<li>No dates selected.</li>';
                return;
            }
            selectedDates.forEach(dateString => {
                const listItem = document.createElement('li');
                listItem.textContent = new Date(dateString).toLocaleDateString(undefined, {
                    weekday: 'short',
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                });
                const removeButton = document.createElement('button');
                removeButton.type = 'button';
                removeButton.textContent = 'Remove';
                removeButton.addEventListener('click', () => {
                    selectedDates = selectedDates.filter(date => date !== dateString);
                    updateCalendarSelection();
                    renderSelectedDates();
                });
                listItem.appendChild(removeButton);
                selectedDatesList.appendChild(listItem);
            });
        };

        renderSelectedDates();
    }

    const sourceSelect = document.getElementById('content-source');
    const platformSelect = document.getElementById('content-platform');
    const input = document.getElementById('content-input');
    const sourceHelp = document.getElementById('source-help');
    const inputHelp = document.getElementById('input-help');
    const characterCount = document.getElementById('character-count');
    const generatedContent = document.getElementById('generated-content');
    const generatedCharacterCount = document.getElementById('generated-character-count');
    const result = document.getElementById('content-result');
    const generateButton = document.getElementById('generate-content');
    const generateError = document.getElementById('generate-error');
    const generateLabel = generateButton
        ? generateButton.querySelector('.button-label')
        : null;
    const contentStatus = document.getElementById('content-status');
    const regenerateButton = document.getElementById('regenerate-content');

    if (sourceSelect && input && generateButton) {
        const updateSourceUI = () => {
            const source = sourceSelect.value;

            if (source === 'inspiration') {
                input.placeholder =
                    'Example: AI agents are changing how companies handle customer support. Create a thought-leadership post around this idea.';
                sourceHelp.textContent =
                    'Give the AI an idea, topic, reference, or direction. It will create the post using your company\'s strategy and brand voice.';
                inputHelp.textContent =
                    'Your input can be short. Focus on the idea you want the post to communicate.';
            } else if (source === 'existing_post') {
                input.placeholder =
                    'Paste your existing post here. The AI will refine it while preserving the original message.';
                sourceHelp.textContent =
                    'Provide a complete existing post and the AI will refine it according to your brand voice and platform.';
                inputHelp.textContent =
                    'The original meaning and intent will be preserved while improving clarity, structure, and tone.';
            } else {
                input.placeholder =
                    'Optional: provide a topic, instruction, or specific direction. Leave empty to let the AI decide.';
                sourceHelp.textContent =
                    'The AI will generate content using your company information, content strategy, and knowledge base.';
                inputHelp.textContent =
                    'This field is optional. You can leave it empty for fully automatic content generation.';
            }

            updateCharacterCount();
        };

        const updateCharacterCount = () => {
            if (characterCount) {
                characterCount.textContent = `${input.value.length} characters`;
            }
        };

        const updateGeneratedCharacterCount = () => {
            if (generatedCharacterCount && generatedContent) {
                generatedCharacterCount.textContent =
                    `${generatedContent.value.length} characters`;
            }
        };

        const setGenerateError = (message) => {
            if (!generateError) {
                return;
            }

            if (message) {
                generateError.hidden = false;
                generateError.textContent = message;
            } else {
                generateError.hidden = true;
                generateError.textContent = '';
            }
        };

        const setLoadingState = (isLoading) => {
            generateButton.classList.toggle('is-loading', isLoading);
            generateButton.disabled = isLoading;
            generateButton.setAttribute('aria-busy', isLoading ? 'true' : 'false');

            if (generateLabel) {
                generateLabel.textContent = isLoading
                    ? 'Generating...'
                    : 'Generate Content';
            }

            if (contentStatus) {
                contentStatus.textContent = isLoading ? 'Generating' : 'Draft';
            }

            if (generatedContent) {
                generatedContent.classList.toggle('is-streaming', isLoading);
            }

            if (regenerateButton) {
                regenerateButton.disabled = isLoading;
                regenerateButton.classList.toggle('is-loading', isLoading);
                regenerateButton.setAttribute('aria-busy', isLoading ? 'true' : 'false');

                const regenerateLabel = regenerateButton.querySelector('.button-label');
                if (regenerateLabel) {
                    regenerateLabel.textContent = isLoading
                        ? 'Regenerating...'
                        : 'Regenerate';
                }
            }
        };

        const streamGeneratedContent = async () => {
            const contentSource = sourceSelect.value;
            const userInput = input.value.trim();

            setGenerateError('');

            if (contentSource !== 'generate' && !userInput) {
                setGenerateError('Please provide input for the selected content source.');
                return;
            }

            setLoadingState(true);

            if (result) {
                result.hidden = false;
            }

            if (generatedContent) {
                generatedContent.value = '';
                updateGeneratedCharacterCount();
            }

            if (result) {
                result.scrollIntoView({
                    behavior: 'auto',
                    block: 'start'
                });
            }

            try {
                const response = await fetch('/generate_content', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/plain'
                    },
                    credentials: 'same-origin',
                    body: JSON.stringify({
                        content_source: contentSource,
                        platform: platformSelect ? platformSelect.value : 'linkedin',
                        user_input: userInput
                    })
                });

                const contentType = response.headers.get('content-type') || '';

                if (!response.ok) {
                    let message = 'Unable to generate content. Please try again.';

                    if (contentType.includes('application/json')) {
                        const data = await response.json();
                        message = data.error || message;
                    } else if (response.status === 401 || response.redirected) {
                        message = 'Please log in again to generate content.';
                    }

                    throw new Error(message);
                }

                if (contentType.includes('text/html')) {
                    throw new Error('Please log in again to generate content.');
                }

                if (!response.body || !response.body.getReader) {
                    const text = await response.text();
                    generatedContent.value = text;
                    updateGeneratedCharacterCount();
                    return;
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder();

                while (true) {
                    const { done, value } = await reader.read();

                    if (done) {
                        generatedContent.value += decoder.decode();
                        updateGeneratedCharacterCount();
                        break;
                    }

                    generatedContent.value += decoder.decode(value, { stream: true });
                    updateGeneratedCharacterCount();
                    generatedContent.scrollTop = generatedContent.scrollHeight;
                }

                if (!generatedContent.value.trim()) {
                    throw new Error('No content was generated. Please try again.');
                }
            } catch (error) {
                setGenerateError(error.message || 'Unable to generate content. Please try again.');

                if (generatedContent && !generatedContent.value.trim()) {
                    generatedContent.value = '';
                    updateGeneratedCharacterCount();
                }
            } finally {
                setLoadingState(false);
            }
        };

        sourceSelect.addEventListener('change', updateSourceUI);
        input.addEventListener('input', updateCharacterCount);

        if (generatedContent) {
            generatedContent.addEventListener('input', updateGeneratedCharacterCount);
        }

        generateButton.addEventListener('click', streamGeneratedContent);

        if (regenerateButton) {
            regenerateButton.addEventListener('click', streamGeneratedContent);
        }

        updateSourceUI();
    }

    const scheduleButton = document.getElementById('schedule-content');
    const scheduleModal = document.getElementById('schedule-modal');
    const scheduleBackdrop = document.getElementById('schedule-modal-backdrop');
    const scheduleClose = document.getElementById('schedule-modal-close');
    const scheduleSave = document.getElementById('schedule-save');
    const scheduleCalendar = document.getElementById('schedule-calendar');
    const scheduleMonthSelect = document.getElementById('schedule-month');
    const scheduleYearSelect = document.getElementById('schedule-year');
    const schedulePrevMonth = document.getElementById('schedule-prev-month');
    const scheduleNextMonth = document.getElementById('schedule-next-month');
    const scheduleTime = document.getElementById('schedule-time');

    if (scheduleButton && scheduleModal && scheduleCalendar) {
        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];

        const today = new Date();
        let viewYear = today.getFullYear();
        let viewMonth = today.getMonth();
        let selectedDate = new Date(
            today.getFullYear(),
            today.getMonth(),
            today.getDate()
        );

        const pad = (value) => String(value).padStart(2, '0');

        const defaultTime = () => {
            const nextHour = new Date();
            nextHour.setMinutes(0, 0, 0);
            nextHour.setHours(nextHour.getHours() + 1);
            return `${pad(nextHour.getHours())}:${pad(nextHour.getMinutes())}`;
        };

        const fillPeriodSelects = () => {
            scheduleMonthSelect.innerHTML = '';
            monthNames.forEach((name, index) => {
                const option = document.createElement('option');
                option.value = String(index);
                option.textContent = name;
                scheduleMonthSelect.appendChild(option);
            });

            const startYear = today.getFullYear();
            scheduleYearSelect.innerHTML = '';
            for (let year = startYear; year <= startYear + 5; year += 1) {
                const option = document.createElement('option');
                option.value = String(year);
                option.textContent = String(year);
                scheduleYearSelect.appendChild(option);
            }
        };

        const isSameDay = (left, right) => (
            left.getFullYear() === right.getFullYear()
            && left.getMonth() === right.getMonth()
            && left.getDate() === right.getDate()
        );

        const renderCalendar = () => {
            scheduleMonthSelect.value = String(viewMonth);
            scheduleYearSelect.value = String(viewYear);
            scheduleCalendar.innerHTML = '';

            const firstWeekday = new Date(viewYear, viewMonth, 1).getDay();
            const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();

            for (let i = 0; i < firstWeekday; i += 1) {
                const empty = document.createElement('span');
                empty.className = 'schedule-picker__day schedule-picker__day--empty';
                empty.setAttribute('aria-hidden', 'true');
                scheduleCalendar.appendChild(empty);
            }

            for (let day = 1; day <= daysInMonth; day += 1) {
                const dayDate = new Date(viewYear, viewMonth, day);
                const dayButton = document.createElement('button');
                dayButton.type = 'button';
                dayButton.className = 'schedule-picker__day';
                dayButton.textContent = String(day);
                dayButton.dataset.date = `${viewYear}-${pad(viewMonth + 1)}-${pad(day)}`;

                if (isSameDay(dayDate, today)) {
                    dayButton.classList.add('is-today');
                }

                if (isSameDay(dayDate, selectedDate)) {
                    dayButton.classList.add('is-selected');
                    dayButton.setAttribute('aria-current', 'date');
                }

                dayButton.addEventListener('click', () => {
                    selectedDate = dayDate;
                    renderCalendar();
                });

                scheduleCalendar.appendChild(dayButton);
            }
        };

        const openScheduleModal = () => {
            fillPeriodSelects();
            viewYear = selectedDate.getFullYear();
            viewMonth = selectedDate.getMonth();

            if (!scheduleTime.value) {
                scheduleTime.value = defaultTime();
            }

            if (scheduleError) {
                scheduleError.hidden = true;
                scheduleError.textContent = '';
            }

            renderCalendar();
            scheduleModal.hidden = false;
            document.body.classList.add('schedule-modal-open');
            scheduleMonthSelect.focus();
        };

        const closeScheduleModal = () => {
            scheduleModal.hidden = true;
            document.body.classList.remove('schedule-modal-open');
            scheduleButton.focus();
        };

        const scheduleError = document.getElementById('schedule-error');
        const scheduleSaveLabel = scheduleSave
            ? scheduleSave.querySelector('.button-label')
            : null;

        const setScheduleError = (message) => {
            if (!scheduleError) {
                return;
            }

            if (message) {
                scheduleError.hidden = false;
                scheduleError.textContent = message;
            } else {
                scheduleError.hidden = true;
                scheduleError.textContent = '';
            }
        };

        const showFlashMessage = (category, message) => {
            let container = document.getElementById('flash-container');

            if (!container) {
                container = document.createElement('div');
                container.id = 'flash-container';
                container.className = 'flash-container';
                document.body.appendChild(container);
            }

            container.classList.add('is-toast');

            const alert = document.createElement('div');
            alert.className = `alert alert-${category || 'success'}`;
            alert.setAttribute('role', 'status');

            const text = document.createElement('span');
            text.textContent = message;
            alert.appendChild(text);

            const closeButton = document.createElement('button');
            closeButton.type = 'button';
            closeButton.className = 'flash-close';
            closeButton.setAttribute('aria-label', 'Dismiss message');
            closeButton.innerHTML = '&times;';
            closeButton.addEventListener('click', () => {
                alert.remove();
                if (!container.children.length) {
                    container.classList.remove('is-toast');
                }
            });
            alert.appendChild(closeButton);

            container.appendChild(alert);
            alert.focus?.();
        };

        const setSaveLoading = (isLoading) => {
            if (!scheduleSave) {
                return;
            }

            scheduleSave.disabled = isLoading;
            scheduleSave.classList.toggle('is-loading', isLoading);
            scheduleSave.setAttribute('aria-busy', isLoading ? 'true' : 'false');

            if (scheduleSaveLabel) {
                scheduleSaveLabel.textContent = isLoading ? 'Saving...' : 'Save';
            }
        };

        const saveSchedule = async () => {
            setScheduleError('');

            if (!selectedDate) {
                setScheduleError('Please choose a date.');
                return;
            }

            if (!scheduleTime || !scheduleTime.value) {
                setScheduleError('Please choose a time.');
                return;
            }

            const [hours, minutes] = scheduleTime.value.split(':');
            const scheduledAt = [
                selectedDate.getFullYear(),
                '-',
                pad(selectedDate.getMonth() + 1),
                '-',
                pad(selectedDate.getDate()),
                'T',
                pad(Number(hours) || 0),
                ':',
                pad(Number(minutes) || 0),
                ':00'
            ].join('');

            setSaveLoading(true);

            try {
                const response = await fetch('/schedule_content', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    credentials: 'same-origin',
                    body: JSON.stringify({
                        platform: platformSelect ? platformSelect.value : 'linkedin',
                        scheduled_at: scheduledAt,
                        status: 'scheduled',
                        post_content: generatedContent ? generatedContent.value : ''
                    })
                });

                const contentType = response.headers.get('content-type') || '';
                let data = {};

                if (contentType.includes('application/json')) {
                    data = await response.json();
                }

                if (!response.ok || contentType.includes('text/html')) {
                    throw new Error(
                        data.error || 'Unable to save the schedule. Please try again.'
                    );
                }

                if (contentStatus) {
                    contentStatus.textContent = 'Scheduled';
                }

                closeScheduleModal();

                const flashes = Array.isArray(data.flashes) ? data.flashes : [];
                if (flashes.length > 0) {
                    flashes.forEach((item) => {
                        showFlashMessage(item.category, item.message);
                    });
                } else {
                    showFlashMessage(
                        'success',
                        data.message || 'Content scheduled successfully.'
                    );
                }
            } catch (error) {
                setScheduleError(
                    error.message || 'Unable to save the schedule. Please try again.'
                );
            } finally {
                setSaveLoading(false);
            }
        };

        scheduleButton.addEventListener('click', openScheduleModal);

        if (scheduleBackdrop) {
            scheduleBackdrop.addEventListener('click', closeScheduleModal);
        }

        if (scheduleClose) {
            scheduleClose.addEventListener('click', closeScheduleModal);
        }

        if (scheduleSave) {
            scheduleSave.addEventListener('click', saveSchedule);
        }

        scheduleMonthSelect.addEventListener('change', () => {
            viewMonth = Number(scheduleMonthSelect.value);
            renderCalendar();
        });

        scheduleYearSelect.addEventListener('change', () => {
            viewYear = Number(scheduleYearSelect.value);
            renderCalendar();
        });

        schedulePrevMonth.addEventListener('click', () => {
            const minYear = today.getFullYear();
            viewMonth -= 1;
            if (viewMonth < 0) {
                if (viewYear > minYear) {
                    viewMonth = 11;
                    viewYear -= 1;
                } else {
                    viewMonth = 0;
                }
            }
            renderCalendar();
        });

        scheduleNextMonth.addEventListener('click', () => {
            const maxYear = today.getFullYear() + 5;
            viewMonth += 1;
            if (viewMonth > 11) {
                if (viewYear < maxYear) {
                    viewMonth = 0;
                    viewYear += 1;
                } else {
                    viewMonth = 11;
                }
            }
            renderCalendar();
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !scheduleModal.hidden) {
                closeScheduleModal();
            }
        });

        fillPeriodSelects();
        if (!scheduleTime.value) {
            scheduleTime.value = defaultTime();
        }
    }
});
// ============================================================
// CONTENT CALENDAR
// ============================================================

async function loadContentCalendar() {

    const calendarElement =
        document.getElementById("content-calendar");

    if (!calendarElement) {
        return;
    }

    try {

        const response = await fetch(
            "/api/content-calendar",
            {
                method: "GET",
                credentials: "same-origin",
                headers: {
                    "Accept": "application/json"
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                "Unable to load scheduled content."
            );
        }

        const events = await response.json();

        const calendar =
            new FullCalendar.Calendar(
                calendarElement,
                {

                    initialView: "dayGridMonth",

                    headerToolbar: {
                        left: "prev,next today",
                        center: "title",
                        right:
                            "dayGridMonth,timeGridWeek,timeGridDay"
                    },

                    height: "auto",

                    displayEventTime: true,

                    events: events,

                    eventClick: function (info) {

                        const event =
                            info.event;

                        const platform =
                            event.extendedProps.platform;

                        const status =
                            event.extendedProps.status;

                        const postContent =
                            event.extendedProps.post_content;

                        alert(
                            "Date: " +
                            event.start.toLocaleDateString() +

                            "\n\nTime: " +
                            event.start.toLocaleTimeString(
                                [],
                                {
                                    hour: "2-digit",
                                    minute: "2-digit"
                                }
                            ) +

                            "\n\nPlatform: " +
                            platform +

                            "\n\nStatus: " +
                            status +

                            "\n\nContent:\n" +
                            postContent
                        );
                    }
                }
            );

        calendar.render();

    } catch (error) {

        console.error(
            "Content calendar error:",
            error
        );

        calendarElement.innerHTML =
            "<p>Unable to load scheduled content.</p>";
    }
}
// Publish Content(Manually)

async function publishContentNow() {

    const publishButton =
        document.getElementById("publish-content");

    const messageElement =
        document.getElementById("publish-message");

    const content =
        document.getElementById("generated-content").value.trim();

    const platform =
        document.getElementById("content-platform").value;

    if (!content) {

        messageElement.textContent =
            "Please generate some content first.";

        messageElement.hidden = false;

        return;
    }

    publishButton.disabled = true;

    messageElement.hidden = false;
    messageElement.textContent = "Publishing...";

    try {

        const response = await fetch(
            "/api/publish-content",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                credentials: "same-origin",

                body: JSON.stringify({
                    content: content,
                    platform: platform
                })
            }
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(
                data.error || "Publishing failed."
            );
        }

        messageElement.textContent =
            data.message || "Published successfully.";

        messageElement.hidden = false;

    } catch (error) {

        console.error(
            "Publish error:",
            error
        );

        messageElement.textContent =
            error.message || "Unable to publish content.";

        messageElement.hidden = false;

    } finally {

        publishButton.disabled = false;
    }
}

document.addEventListener(
    "DOMContentLoaded",
    function () {

        const publishButton =
            document.getElementById("publish-content");

        if (publishButton) {

            publishButton.addEventListener(
                "click",
                publishContentNow
            );
        }
    }
);

// ============================================================
// INITIALIZE CONTENT CALENDAR
// ============================================================

document.addEventListener(
    "DOMContentLoaded",
    loadContentCalendar
);
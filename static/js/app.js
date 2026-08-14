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
});

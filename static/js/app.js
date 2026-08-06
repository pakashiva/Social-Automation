document.addEventListener('DOMContentLoaded', () => {

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
    if (companyForm) {
        companyForm.addEventListener('submit', event => {
            event.preventDefault();
            const file = strategyFile.files[0];
            const context = brandContext.value.trim();
            let valid = true;

            if (!file || file.type !== 'application/pdf') {
                strategyError.textContent = 'Please upload a PDF document.';
                valid = false;
            }

            if (context.length < 100) {
                contextError.textContent = 'Brand Context must contain at least 100 characters.';
                valid = false;
            }


        });
    }

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
});

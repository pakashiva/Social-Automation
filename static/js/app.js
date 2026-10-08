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


    // ============================================================
    // STRATEGY FILE
    // ============================================================

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

    const companyLogoInput = document.getElementById('company-logo');
    const companyLogoFilename = document.getElementById('company-logo-filename');
    const companyLogoError = document.getElementById('company-logo-error');
    let companyLogoPreviewUrl = null;

    if (companyLogoInput) {
        companyLogoInput.addEventListener('change', () => {
            const file = companyLogoInput.files?.[0];
            if (!file) return;
            if (!file.name.toLowerCase().endsWith('.png') || file.size > 5 * 1024 * 1024) {
                companyLogoError.textContent = file.size > 5 * 1024 * 1024
                    ? 'The logo must be 5 MB or smaller.'
                    : 'Please choose a PNG image.';
                companyLogoInput.value = '';
                return;
            }
            companyLogoError.textContent = '';
            companyLogoFilename.textContent = file.name;
            if (companyLogoPreviewUrl) URL.revokeObjectURL(companyLogoPreviewUrl);
            companyLogoPreviewUrl = URL.createObjectURL(file);
            let preview = document.querySelector('.company-logo-preview');
            if (!preview) {
                preview = document.createElement('img');
                preview.className = 'company-logo-preview';
                preview.alt = 'Selected company logo preview';
                document.querySelector('.company-logo-upload')?.prepend(preview);
            }
            preview.src = companyLogoPreviewUrl;
        });
    }


    // ============================================================
    // BRAND CONTEXT
    // ============================================================

    const brandContext = document.getElementById('brand-context');
    const contextCounter = document.getElementById('context-counter');
    const contextError = document.getElementById('context-error');

    if (brandContext) {

        const updateCounter = () => {

            const length = brandContext.value.length;

            contextCounter.textContent = `${length} / 10000`;

            if (length < 100) {
                contextError.textContent =
                    'Brand Context must contain at least 100 characters.';
            } else {
                contextError.textContent = '';
            }
        };

        brandContext.addEventListener('input', updateCounter);

        updateCounter();
    }


    // ============================================================
    // COMPANY / SCHEDULE
    // ============================================================

    const companyForm = document.getElementById('company-form');

    const scheduleForm = document.getElementById('schedule-form');
    const scheduleInput = document.getElementById('schedule-input');
    const scheduleError = document.getElementById('schedule-error');
    const scheduleErrorGlobal =
        document.getElementById('schedule-error-global');


    // ============================================================
    // OLD CALENDAR
    // ============================================================

    const calendar = document.getElementById('calendar');
    const selectedDatesList =
        document.getElementById('selected-dates');

    let selectedDates = [];

    if (calendar) {

        const today = new Date();

        const year = today.getFullYear();
        const month = today.getMonth();

        const daysInMonth =
            new Date(year, month + 1, 0).getDate();

        const startDay =
            new Date(year, month, 1).getDay();


        for (let i = 0; i < startDay; i++) {

            const placeholder =
                document.createElement('div');

            placeholder.className = 'calendar-day';

            placeholder.style.visibility = 'hidden';

            calendar.appendChild(placeholder);
        }


        for (let day = 1; day <= daysInMonth; day++) {

            const dayButton =
                document.createElement('button');

            dayButton.type = 'button';

            dayButton.className = 'calendar-day';

            dayButton.textContent = day;

            dayButton.dataset.date =
                new Date(
                    year,
                    month,
                    day
                )
                    .toISOString()
                    .split('T')[0];


            dayButton.addEventListener('click', () => {

                const dateString =
                    dayButton.dataset.date;

                if (selectedDates.includes(dateString)) {

                    selectedDates =
                        selectedDates.filter(
                            date => date !== dateString
                        );

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

            const dayButtons =
                calendar.querySelectorAll(
                    '.calendar-day'
                );

            dayButtons.forEach(button => {

                const dateString =
                    button.dataset.date;

                if (!dateString) return;

                button.classList.toggle(
                    'selected',
                    selectedDates.includes(dateString)
                );
            });
        };


        const renderSelectedDates = () => {

            selectedDatesList.innerHTML = '';

            if (selectedDates.length === 0) {

                selectedDatesList.innerHTML =
                    '<li>No dates selected.</li>';

                return;
            }


            selectedDates.forEach(dateString => {

                const listItem =
                    document.createElement('li');

                listItem.textContent =
                    new Date(dateString)
                        .toLocaleDateString(
                            undefined,
                            {
                                weekday: 'short',
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric'
                            }
                        );


                const removeButton =
                    document.createElement('button');

                removeButton.type = 'button';

                removeButton.textContent = 'Remove';


                removeButton.addEventListener(
                    'click',
                    () => {

                        selectedDates =
                            selectedDates.filter(
                                date =>
                                    date !== dateString
                            );

                        updateCalendarSelection();

                        renderSelectedDates();
                    }
                );


                listItem.appendChild(removeButton);

                selectedDatesList.appendChild(listItem);
            });
        };


        renderSelectedDates();
    }


    // ============================================================
    // CONTENT GENERATION
    // ============================================================

    const sourceSelect =
        document.getElementById('content-source');

    const platformSelect =
        document.getElementById('content-platform');

    const input =
        document.getElementById('content-input');

    let lastGenerationRequest = null;

    const sourceHelp =
        document.getElementById('source-help');

    const inputHelp =
        document.getElementById('input-help');

    const characterCount =
        document.getElementById('character-count');

    const generatedContent =
        document.getElementById('generated-content');

    const generatedCharacterCount =
        document.getElementById('generated-character-count');

    const result =
        document.getElementById('content-result');

    const generateButton =
        document.getElementById('generate-content');

    const generateError =
        document.getElementById('generate-error');

    const generateLabel =
        generateButton
            ? generateButton.querySelector('.button-label')
            : null;

    const contentStatus =
        document.getElementById('content-status');

    const regenerateButton =
        document.getElementById('regenerate-content');


    // ============================================================
    // IMAGE PROMPT ELEMENTS
    // ============================================================

    const imagePromptSection =
        document.getElementById('image-prompt-section');

    const imagePrompt =
        document.getElementById('image-prompt');

    const copyImagePromptButton =
        document.getElementById('copy-image-prompt');

    const imagePromptStatus =
        document.getElementById('image-prompt-status');


    // ============================================================
    // CONTENT GENERATION LOGIC
    // ============================================================

    if (
        sourceSelect &&
        input &&
        generateButton
    ) {

        const updateSourceUI = () => {

            const source =
                sourceSelect.value;


            if (source === 'inspiration') {

                input.placeholder =
                    'Example: AI agents are changing how companies handle customer support. Create a thought-leadership post around this idea.';

                sourceHelp.textContent =
                    'Give the AI an idea, topic, reference, or direction. It will create the post using your company\'s strategy and brand voice.';

                inputHelp.textContent =
                    'Your input can be short. Focus on the idea you want the post to communicate.';

            }

            else if (source === 'existing_post') {

                input.placeholder =
                    'Paste your existing post here. The AI will refine it while preserving the original message.';

                sourceHelp.textContent =
                    'Provide a complete existing post and the AI will refine it according to your brand voice and platform.';

                inputHelp.textContent =
                    'The original meaning and intent will be preserved while improving clarity, structure, and tone.';

            }

            else {

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

                characterCount.textContent =
                    `${input.value.length} characters`;
            }
        };


        const updateGeneratedCharacterCount = () => {

            if (
                generatedCharacterCount &&
                generatedContent
            ) {

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

                generateError.textContent =
                    message;

            } else {

                generateError.hidden = true;

                generateError.textContent = '';
            }
        };


        // ========================================================
        // IMAGE PROMPT UI HELPERS
        // ========================================================

        const setImagePromptStatus = (
            message,
            hidden = false
        ) => {

            if (!imagePromptStatus) {
                return;
            }

            imagePromptStatus.hidden = hidden;

            imagePromptStatus.textContent =
                message || '';
        };


        const resetImagePrompt = () => {

            if (imagePromptSection) {
                imagePromptSection.hidden = true;
            }

            if (imagePrompt) {
                imagePrompt.value = '';
            }

            setImagePromptStatus('', true);
        };


        // ========================================================
        // GENERATE IMAGE PROMPT
        //
        // IMPORTANT:
        // This function is called ONLY AFTER
        // /generate_content has completely finished.
        // ========================================================

        const generateImagePrompt = async (
            content,
            platform
        ) => {

            console.log("generateImagePrompt() STARTED");

            if (!content || !content.trim()) {

                throw new Error(
                    'Generated content is empty. Cannot generate image prompt.'
                );
            }


            if (!platform) {

                throw new Error(
                    'Platform is required to generate image prompt.'
                );
            }


            if (imagePromptSection) {
                imagePromptSection.hidden = false;
            }


            if (imagePrompt) {

                imagePrompt.value =
                    'Generating image prompt...';
            }


            setImagePromptStatus(
                'Creating image prompt from your generated post...',
                false
            );


            const response =
                await fetch(
                    '/generate_image_prompt',
                    {
                        method: 'POST',

                        headers: {
                            'Content-Type':
                                'application/json',

                            'Accept':
                                'application/json'
                        },

                        credentials:
                            'omit',

                        body:
                            JSON.stringify({
                                content:
                                    content,

                                platform:
                                    platform
                            })
                    }
                );


            const contentType =
                response.headers.get(
                    'content-type'
                ) || '';


            if (!response.ok) {

                let message =
                    'Unable to generate image prompt.';


                if (
                    contentType.includes(
                        'application/json'
                    )
                ) {

                    const data =
                        await response.json();

                    message =
                        data.error ||
                        message;

                }

                else if (
                    response.status === 401 ||
                    response.redirected
                ) {

                    message =
                        'Please log in again to generate the image prompt.';
                }


                throw new Error(message);
            }


            if (
                !contentType.includes(
                    'application/json'
                )
            ) {

                throw new Error(
                    'Invalid response from image prompt service.'
                );
            }


            const data =
                await response.json();


            if (
                !data.success ||
                !data.image_prompt ||
                !data.image_prompt.trim()
            ) {

                throw new Error(
                    data.error ||
                    'No image prompt was generated.'
                );
            }


            const finalImagePrompt =
                data.image_prompt.trim();


            if (imagePrompt) {

                imagePrompt.value =
                    finalImagePrompt;
            }


            setImagePromptStatus(
                'Image prompt generated successfully.',
                false
            );


            return finalImagePrompt;
        };


        // ========================================================
        // LOADING STATE
        // ========================================================

        const setLoadingState = (
            isLoading
        ) => {

            generateButton.classList.toggle(
                'is-loading',
                isLoading
            );

            generateButton.disabled =
                isLoading;

            generateButton.setAttribute(
                'aria-busy',
                isLoading
                    ? 'true'
                    : 'false'
            );


            if (generateLabel) {

                generateLabel.textContent =
                    isLoading
                        ? 'Generating...'
                        : 'Generate Content';
            }


            if (contentStatus) {

                contentStatus.textContent =
                    isLoading
                        ? 'Generating'
                        : 'Draft';
            }


            if (generatedContent) {

                generatedContent.classList.toggle(
                    'is-streaming',
                    isLoading
                );
            }


            if (regenerateButton) {

                regenerateButton.disabled =
                    isLoading;

                regenerateButton.classList.toggle(
                    'is-loading',
                    isLoading
                );

                regenerateButton.setAttribute(
                    'aria-busy',
                    isLoading
                        ? 'true'
                        : 'false'
                );


                const regenerateLabel =
                    regenerateButton.querySelector(
                        '.button-label'
                    );


                if (regenerateLabel) {

                    regenerateLabel.textContent =
                        isLoading
                            ? 'Regenerating...'
                            : 'Regenerate';
                }
            }
        };


        // ========================================================
        // MAIN CONTENT GENERATION
        // ========================================================

        const streamGeneratedContent =
            async () => {

                const contentSource =
                    sourceSelect
                        ? sourceSelect.value
                        : 'generate';

                const userInput =
                    input
                        ? input.value.trim()
                        : '';

                const platform =
                    platformSelect
                        ? platformSelect.value
                        : 'linkedin';

                setGenerateError('');

                resetImagePrompt();


                if (
                    contentSource !== 'generate' &&
                    !userInput
                ) {

                    setGenerateError(
                        'Please provide input for the selected content source.'
                    );

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

                    lastGenerationRequest = {
                        content_source: contentSource,
                        user_input: userInput,
                        platform
                    };

                    // ==================================================
                    // STEP 1
                    // Generate the social media post
                    // ==================================================

                    const response =
                        await fetch(
                            '/generate_content',
                            {
                                method: 'POST',

                                headers: {
                                    'Content-Type':
                                        'application/json',

                                    'Accept':
                                        'text/plain'
                                },

                                credentials:
                                    'same-origin',

                                body:
                                    JSON.stringify({
                                        content_source:
                                            contentSource,

                                        platform:
                                            platform,

                                        user_input:
                                            userInput
                                    })
                            }
                        );


                    const contentType =
                        response.headers.get(
                            'content-type'
                        ) || '';


                    if (!response.ok) {

                        let message =
                            'Unable to generate content. Please try again.';


                        if (
                            contentType.includes(
                                'application/json'
                            )
                        ) {

                            const data =
                                await response.json();

                            message =
                                data.error ||
                                message;

                        }

                        else if (
                            response.status === 401 ||
                            response.redirected
                        ) {

                            message =
                                'Please log in again to generate content.';
                        }


                        throw new Error(message);
                    }


                    if (
                        contentType.includes(
                            'text/html'
                        )
                    ) {

                        throw new Error(
                            'Please log in again to generate content.'
                        );
                    }


                    // ==================================================
                    // STEP 2
                    // Read streaming response
                    // ==================================================

                    if (
                        !response.body ||
                        !response.body.getReader
                    ) {

                        const text =
                            await response.text();

                        generatedContent.value =
                            text;

                        updateGeneratedCharacterCount();

                    }

                    else {

                        const reader =
                            response.body.getReader();

                        const decoder =
                            new TextDecoder();


                        while (true) {

                            const {
                                done,
                                value
                            } =
                                await reader.read();


                            if (done) {

                                console.log("CONTENT STREAM FINISHED");


                                generatedContent.value +=
                                    decoder.decode();

                                updateGeneratedCharacterCount();

                                break;
                            }


                            generatedContent.value +=
                                decoder.decode(
                                    value,
                                    {
                                        stream: true
                                    }
                                );


                            updateGeneratedCharacterCount();


                            generatedContent.scrollTop =
                                generatedContent.scrollHeight;
                        }
                    }


                    // ==================================================
                    // STEP 3
                    // Make sure the post actually exists
                    // ==================================================

                    if (
                        !generatedContent.value.trim()
                    ) {

                        throw new Error(
                            'No content was generated. Please try again.'
                        );
                    }


                    // ==================================================
                    // STEP 4
                    //
                    // IMPORTANT:
                    // The content generation is now COMPLETELY DONE.
                    //
                    // Only now do we call the image prompt API.
                    // ==================================================
                    
                    console.log(
                        "CALLING IMAGE PROMPT API",
                        generatedContent.value.length,
                        platform
                    );

                    await generateImagePrompt(
                        generatedContent.value,
                        platform
                    );


                } catch (error) {

                    setGenerateError(
                        error.message ||
                        'Unable to generate content. Please try again.'
                    );


                    if (
                        generatedContent &&
                        !generatedContent.value.trim()
                    ) {

                        generatedContent.value = '';

                        updateGeneratedCharacterCount();
                    }

                } finally {

                    setLoadingState(false);
                }
            };

        // ========================================================
// IMAGE UPLOAD
// ========================================================

const uploadImage = async () => {
    const imageInput = document.getElementById('image-upload');
    const status = document.getElementById('image-upload-status');
    const uploadButton = document.getElementById('upload-image-button');

    if (!imageInput?.files?.length) {
        status.textContent = 'Choose one or more photos first.';
        return;
    }

    const selectedFiles = Array.from(imageInput.files);
    const totalAfterUpload = (window.uploadedImages || []).length + selectedFiles.length;
    if (totalAfterUpload > 10) {
        status.textContent = 'You can add up to 10 photos per post.';
        return;
    }

    if (platformSelect?.value === 'instagram' && selectedFiles.some((file) => !/\.jpe?g$/i.test(file.name))) {
        status.textContent = 'Instagram carousel photos must be JPG or JPEG. Choose JPG images for this post.';
        return;
    }

    const formData = new FormData();
    selectedFiles.forEach((file) => formData.append('images', file));
    uploadButton.disabled = true;
    status.textContent = `Uploading ${selectedFiles.length} photo${selectedFiles.length === 1 ? '' : 's'}…`;

    try {
        const response = await fetch('/upload_image', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        if (!response.ok || !data.success) {
            throw new Error(data.error || 'Photo upload failed.');
        }

        window.uploadedImages = [
            ...(window.uploadedImages || []),
            ...data.images
        ];
        imageInput.value = '';
        renderUploadedImages();
        status.textContent = `${data.images.length} photo${data.images.length === 1 ? '' : 's'} uploaded.`;
    } catch (error) {
        status.textContent = error.message || 'Photo upload failed.';
    } finally {
        uploadButton.disabled = false;
    }
};

const renderUploadedImages = () => {
    const preview = document.getElementById('uploaded-images-preview');
    const count = document.getElementById('uploaded-image-count');
    const images = window.uploadedImages || [];
    if (!preview || !count) return;

    preview.replaceChildren();
    count.textContent = `${images.length} of 10`;

    images.forEach((image, index) => {
        const item = document.createElement('figure');
        item.className = 'media-preview';

        const thumbnail = document.createElement('img');
        thumbnail.src = image.image_url;
        thumbnail.alt = image.name || `Post photo ${index + 1}`;
        item.appendChild(thumbnail);

        const caption = document.createElement('figcaption');
        caption.textContent = image.name || `Photo ${index + 1}`;
        item.appendChild(caption);

        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'media-preview__remove';
        removeButton.textContent = 'Remove';
        removeButton.setAttribute('aria-label', `Remove ${image.name || `photo ${index + 1}`}`);
        removeButton.addEventListener('click', async () => {
            try {
                const response = await fetch(
                    `/upload_image/${encodeURIComponent(image.filename)}`,
                    { method: 'DELETE', credentials: 'same-origin' }
                );
                const data = await response.json();
                if (!response.ok || !data.success) {
                    throw new Error(data.error || 'Unable to remove this photo.');
                }
                window.uploadedImages.splice(index, 1);
                renderUploadedImages();
            } catch (error) {
                document.getElementById('image-upload-status').textContent =
                    error.message || 'Unable to remove this photo.';
            }
        });
        item.appendChild(removeButton);
        preview.appendChild(item);
    });
};


        // ========================================================
        // COPY IMAGE PROMPT
        // ========================================================

        if (copyImagePromptButton) {

            copyImagePromptButton.addEventListener(
                'click',
                async () => {

                    if (
                        !imagePrompt ||
                        !imagePrompt.value.trim()
                    ) {

                        return;
                    }


                    const text =
                        imagePrompt.value;


                    try {

                        await navigator.clipboard.writeText(
                            text
                        );

                        setImagePromptStatus(
                            'Image prompt copied to clipboard.',
                            false
                        );

                    } catch (error) {

                        // Fallback for browsers where
                        // navigator.clipboard is unavailable.

                        imagePrompt.focus();

                        imagePrompt.select();

                        document.execCommand('copy');

                        setImagePromptStatus(
                            'Image prompt copied to clipboard.',
                            false
                        );
                    }
                }
            );
        }


        // ========================================================
        // EVENT LISTENERS
        // ========================================================

        const uploadImageButton =
                document.getElementById(
                    'upload-image-button'
                );

            if (uploadImageButton) {

                uploadImageButton.addEventListener(
                    'click',
                    uploadImage
                );
            }
        sourceSelect.addEventListener(
            'change',
            updateSourceUI
        );

        input.addEventListener(
            'input',
            updateCharacterCount
        );


        if (generatedContent) {

            generatedContent.addEventListener(
                'input',
                updateGeneratedCharacterCount
            );
        }


        generateButton.addEventListener(
            'click',
            streamGeneratedContent
        );


        if (regenerateButton) {

            regenerateButton.addEventListener(
                'click',
                streamGeneratedContent
            );
        }


        updateSourceUI();
    }

    // ============================================================
// PUBLISH NOW
// ============================================================

const publishContentButton = document.getElementById("publish-content");

if (publishContentButton) {

    publishContentButton.addEventListener("click", async function () {
        const feedback = document.getElementById("publish-feedback");
        const feedbackMessage = document.getElementById("publish-feedback-message");
        const feedbackLink = document.getElementById("publish-feedback-link");
        const showFeedback = (message, isError = true, connectRequired = false) => {
            feedbackMessage.textContent = message;
            feedback.hidden = false;
            feedback.classList.toggle("is-success", !isError);
            feedbackLink.hidden = !connectRequired;
        };

        feedback.hidden = true;

        try {
            const content = generatedContent
                ? generatedContent.value.trim()
                : "";

            if (!content) {
                showFeedback("Add post content before publishing.");
                return;
            }

            const platform = platformSelect
                ? platformSelect.value
                : "";

            if (!platform) {
                showFeedback("Choose a platform before publishing.");
                return;
            }

            const images = window.uploadedImages || [];
            if (platform === "instagram" && images.some((image) => !/\.jpe?g$/i.test(image.name || image.filename))) {
                showFeedback("Instagram photos must be JPG or JPEG. Remove other images or upload JPG files.");
                return;
            }

            publishContentButton.disabled = true;
            publishContentButton.textContent = "Publishing…";

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
                        platform: platform,
                        images: images.map((image) => ({
                            image_url: image.image_url,
                            filename: image.filename
                        }))
                    })
                }
            );

            const responseText = await response.text();
            let data;
            try {
                data = JSON.parse(responseText);
            } catch (parseError) {
                throw new Error(
                    `Server returned non-JSON response (${response.status}): ${responseText.substring(0, 500)}`
                );
            }

            if (!response.ok || !data.success) {
                const error = new Error(data.error || "Publishing failed.");
                error.connectRequired = Boolean(data.connect_required);
                throw error;
            }

            window.uploadedImages = [];
            document.getElementById("uploaded-images-preview").replaceChildren();
            document.getElementById("uploaded-image-count").textContent = "0 of 10";
            showFeedback(data.message || "Content published successfully.", false);
        }
        catch (error) {
            showFeedback(error.message || "Failed to publish content.", true, Boolean(error.connectRequired));
        }
        finally {
            publishContentButton.disabled = false;
            publishContentButton.textContent = "Publish Now";
        }

    });

}


    // ============================================================
    // SCHEDULE MODAL
    // ============================================================

    const scheduleButton =
        document.getElementById('schedule-content');

    const scheduleModal =
        document.getElementById('schedule-modal');

    const scheduleBackdrop =
        document.getElementById(
            'schedule-modal-backdrop'
        );

    const scheduleClose =
        document.getElementById(
            'schedule-modal-close'
        );

    const scheduleSave =
        document.getElementById(
            'schedule-save'
        );

    const scheduleCalendar =
        document.getElementById(
            'schedule-calendar'
        );

    const scheduleMonthSelect =
        document.getElementById(
            'schedule-month'
        );

    const scheduleYearSelect =
        document.getElementById(
            'schedule-year'
        );

    const schedulePrevMonth =
        document.getElementById(
            'schedule-prev-month'
        );

    const scheduleNextMonth =
        document.getElementById(
            'schedule-next-month'
        );

    const scheduleTime =
        document.getElementById(
            'schedule-time'
        );

    const scheduleHitl = document.getElementById('schedule-hitl');
    const scheduleApprovalPreferences = document.getElementById('schedule-approval-preferences');
    const scheduleNotifyHours = document.getElementById('schedule-notify-hours');
    const scheduleUnapprovedAction = document.getElementById('schedule-unapproved-action');


    if (
        scheduleButton &&
        scheduleModal &&
        scheduleCalendar
    ) {

        const updateScheduleApprovalFields = () => {
            const enabled = Boolean(scheduleHitl?.checked);
            if (scheduleApprovalPreferences) scheduleApprovalPreferences.hidden = !enabled;
            if (scheduleNotifyHours) scheduleNotifyHours.required = enabled;
            if (scheduleUnapprovedAction) scheduleUnapprovedAction.required = enabled;
        };
        scheduleHitl?.addEventListener('change', updateScheduleApprovalFields);
        updateScheduleApprovalFields();

        const monthNames = [
            'January',
            'February',
            'March',
            'April',
            'May',
            'June',
            'July',
            'August',
            'September',
            'October',
            'November',
            'December'
        ];


        const today = new Date();

        let viewYear =
            today.getFullYear();

        let viewMonth =
            today.getMonth();


        let selectedDate =
            new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate()
            );


        const pad = (value) =>
            String(value).padStart(2, '0');


        const defaultTime = () => {

            const nextHour =
                new Date();

            nextHour.setMinutes(
                0,
                0,
                0
            );

            nextHour.setHours(
                nextHour.getHours() + 1
            );


            return (
                `${pad(nextHour.getHours())}:` +
                `${pad(nextHour.getMinutes())}`
            );
        };


        const fillPeriodSelects = () => {

            scheduleMonthSelect.innerHTML = '';


            monthNames.forEach(
                (name, index) => {

                    const option =
                        document.createElement(
                            'option'
                        );

                    option.value =
                        String(index);

                    option.textContent =
                        name;

                    scheduleMonthSelect.appendChild(
                        option
                    );
                }
            );


            const startYear =
                today.getFullYear();


            scheduleYearSelect.innerHTML = '';


            for (
                let year = startYear;
                year <= startYear + 5;
                year += 1
            ) {

                const option =
                    document.createElement(
                        'option'
                    );

                option.value =
                    String(year);

                option.textContent =
                    String(year);

                scheduleYearSelect.appendChild(
                    option
                );
            }
        };


        const isSameDay = (
            left,
            right
        ) => (

            left.getFullYear() ===
                right.getFullYear()

            &&

            left.getMonth() ===
                right.getMonth()

            &&

            left.getDate() ===
                right.getDate()
        );


        const renderCalendar = () => {

            scheduleMonthSelect.value =
                String(viewMonth);

            scheduleYearSelect.value =
                String(viewYear);

            scheduleCalendar.innerHTML =
                '';


            const firstWeekday =
                new Date(
                    viewYear,
                    viewMonth,
                    1
                ).getDay();


            const daysInMonth =
                new Date(
                    viewYear,
                    viewMonth + 1,
                    0
                ).getDate();


            for (
                let i = 0;
                i < firstWeekday;
                i += 1
            ) {

                const empty =
                    document.createElement(
                        'span'
                    );

                empty.className =
                    'schedule-picker__day schedule-picker__day--empty';

                empty.setAttribute(
                    'aria-hidden',
                    'true'
                );

                scheduleCalendar.appendChild(
                    empty
                );
            }


            for (
                let day = 1;
                day <= daysInMonth;
                day += 1
            ) {

                const dayDate =
                    new Date(
                        viewYear,
                        viewMonth,
                        day
                    );


                const dayButton =
                    document.createElement(
                        'button'
                    );

                dayButton.type =
                    'button';

                dayButton.className =
                    'schedule-picker__day';

                dayButton.textContent =
                    String(day);


                dayButton.dataset.date =
                    `${viewYear}-${pad(viewMonth + 1)}-${pad(day)}`;


                if (
                    isSameDay(
                        dayDate,
                        today
                    )
                ) {

                    dayButton.classList.add(
                        'is-today'
                    );
                }


                if (
                    isSameDay(
                        dayDate,
                        selectedDate
                    )
                ) {

                    dayButton.classList.add(
                        'is-selected'
                    );

                    dayButton.setAttribute(
                        'aria-current',
                        'date'
                    );
                }


                dayButton.addEventListener(
                    'click',
                    () => {

                        selectedDate =
                            dayDate;

                        renderCalendar();
                    }
                );


                scheduleCalendar.appendChild(
                    dayButton
                );
            }
        };


        const openScheduleModal = () => {

            fillPeriodSelects();


            viewYear =
                selectedDate.getFullYear();

            viewMonth =
                selectedDate.getMonth();


            if (!scheduleTime.value) {

                scheduleTime.value =
                    defaultTime();
            }


            if (scheduleError) {

                scheduleError.hidden =
                    true;

                scheduleError.textContent =
                    '';
            }


            renderCalendar();


            scheduleModal.hidden =
                false;


            document.body.classList.add(
                'schedule-modal-open'
            );


            scheduleMonthSelect.focus();
        };


        const closeScheduleModal = () => {

            scheduleModal.hidden =
                true;

            document.body.classList.remove(
                'schedule-modal-open'
            );

            scheduleButton.focus();
        };


        const scheduleError =
            document.getElementById(
                'schedule-error'
            );


        const scheduleSaveLabel =
            scheduleSave
                ? scheduleSave.querySelector(
                    '.button-label'
                )
                : null;


        const setScheduleError = (
            message
        ) => {

            if (!scheduleError) {
                return;
            }


            if (message) {

                scheduleError.hidden =
                    false;

                scheduleError.textContent =
                    message;

            } else {

                scheduleError.hidden =
                    true;

                scheduleError.textContent =
                    '';
            }
        };


        const showFlashMessage = (
            category,
            message
        ) => {

            let container =
                document.getElementById(
                    'flash-container'
                );


            if (!container) {

                container =
                    document.createElement(
                        'div'
                    );

                container.id =
                    'flash-container';

                container.className =
                    'flash-container';

                document.body.appendChild(
                    container
                );
            }


            container.classList.add(
                'is-toast'
            );


            const alert =
                document.createElement(
                    'div'
                );

            alert.className =
                `alert alert-${category || 'success'}`;

            alert.setAttribute(
                'role',
                'status'
            );


            const text =
                document.createElement(
                    'span'
                );

            text.textContent =
                message;

            alert.appendChild(text);


            const closeButton =
                document.createElement(
                    'button'
                );

            closeButton.type =
                'button';

            closeButton.className =
                'flash-close';

            closeButton.setAttribute(
                'aria-label',
                'Dismiss message'
            );

            closeButton.innerHTML =
                '&times;';


            closeButton.addEventListener(
                'click',
                () => {

                    alert.remove();

                    if (
                        !container.children.length
                    ) {

                        container.classList.remove(
                            'is-toast'
                        );
                    }
                }
            );


            alert.appendChild(
                closeButton
            );


            container.appendChild(
                alert
            );


            alert.focus?.();
        };


        const setSaveLoading = (
            isLoading
        ) => {

            if (!scheduleSave) {
                return;
            }


            scheduleSave.disabled =
                isLoading;

            scheduleSave.classList.toggle(
                'is-loading',
                isLoading
            );


            scheduleSave.setAttribute(
                'aria-busy',
                isLoading
                    ? 'true'
                    : 'false'
            );


            if (scheduleSaveLabel) {

                scheduleSaveLabel.textContent =
                    isLoading
                        ? 'Saving...'
                        : 'Save';
            }
        };


        const saveSchedule = async () => {

            setScheduleError('');


            if (!selectedDate) {

                setScheduleError(
                    'Please choose a date.'
                );

                return;
            }


            if (
                !scheduleTime ||
                !scheduleTime.value
            ) {

                setScheduleError(
                    'Please choose a time.'
                );

                return;
            }

            if (scheduleHitl?.checked && (!scheduleNotifyHours?.value || !scheduleUnapprovedAction?.value)) {
                setScheduleError('Choose a notification lead time and what to do if you do not respond.');
                return;
            }


            const [
                hours,
                minutes
            ] =
                scheduleTime.value.split(':');


            const scheduledAt = [

                selectedDate.getFullYear(),

                '-',

                pad(
                    selectedDate.getMonth() + 1
                ),

                '-',

                pad(
                    selectedDate.getDate()
                ),

                'T',

                pad(
                    Number(hours) || 0
                ),

                ':',

                pad(
                    Number(minutes) || 0
                ),

                ':00'

            ].join('');


            setSaveLoading(true);


            try {

                const response =
                    await fetch(
                        '/schedule_content',
                        {
                            method: 'POST',

                            headers: {
                                'Content-Type':
                                    'application/json',

                                'Accept':
                                    'application/json'
                            },

                            credentials:
                                'same-origin',

                            body:
                                JSON.stringify({

                                    platform:
                                        platformSelect
                                            ? platformSelect.value
                                            : 'linkedin',

                                    scheduled_at:
                                        scheduledAt,

                                    status:
                                        'scheduled',

                                    post_content:
                                        generatedContent
                                            ? generatedContent.value
                                            : '',

                                    generation_source:
                                        lastGenerationRequest?.content_source || 'generate',

                                    generation_input:
                                        lastGenerationRequest?.user_input || '',

                                    hitl_required: Boolean(scheduleHitl?.checked),

                                    notify_hours_before: scheduleHitl?.checked ? Number(scheduleNotifyHours.value) : null,

                                    unapproved_action: scheduleHitl?.checked ? scheduleUnapprovedAction.value : null,

                                    images:
                                        (window.uploadedImages || []).map((image) => ({
                                            image_url: image.image_url,
                                            filename: image.filename
                                        }))
                                })
                        }
                    );


                const contentType =
                    response.headers.get(
                        'content-type'
                    ) || '';


                let data = {};


                if (
                    contentType.includes(
                        'application/json'
                    )
                ) {

                    data =
                        await response.json();
                }


                if (
                    !response.ok ||
                    contentType.includes(
                        'text/html'
                    )
                ) {

                    throw new Error(
                        data.error ||
                        'Unable to save the schedule. Please try again.'
                    );
                }


                if (contentStatus) {

                    contentStatus.textContent =
                        'Scheduled';
                }

                window.uploadedImages = [];
                document.getElementById('uploaded-images-preview')?.replaceChildren();
                const uploadedImageCount = document.getElementById('uploaded-image-count');
                if (uploadedImageCount) uploadedImageCount.textContent = '0 of 10';


                closeScheduleModal();


                const flashes =
                    Array.isArray(
                        data.flashes
                    )
                        ? data.flashes
                        : [];


                if (
                    flashes.length > 0
                ) {

                    flashes.forEach(
                        (item) => {

                            showFlashMessage(
                                item.category,
                                item.message
                            );
                        }
                    );

                } else {

                    showFlashMessage(
                        'success',
                        data.message ||
                        'Content scheduled successfully.'
                    );
                }


            } catch (error) {

                setScheduleError(
                    error.message ||
                    'Unable to save the schedule. Please try again.'
                );

            } finally {

                setSaveLoading(false);
            }
        };


        scheduleButton.addEventListener(
            'click',
            openScheduleModal
        );


        if (scheduleBackdrop) {

            scheduleBackdrop.addEventListener(
                'click',
                closeScheduleModal
            );
        }


        if (scheduleClose) {

            scheduleClose.addEventListener(
                'click',
                closeScheduleModal
            );
        }


        if (scheduleSave) {

            scheduleSave.addEventListener(
                'click',
                saveSchedule
            );
        }


        scheduleMonthSelect.addEventListener(
            'change',
            () => {

                viewMonth =
                    Number(
                        scheduleMonthSelect.value
                    );

                renderCalendar();
            }
        );


        scheduleYearSelect.addEventListener(
            'change',
            () => {

                viewYear =
                    Number(
                        scheduleYearSelect.value
                    );

                renderCalendar();
            }
        );


        schedulePrevMonth.addEventListener(
            'click',
            () => {

                const minYear =
                    today.getFullYear();


                viewMonth -= 1;


                if (viewMonth < 0) {

                    if (
                        viewYear > minYear
                    ) {

                        viewMonth = 11;

                        viewYear -= 1;

                    } else {

                        viewMonth = 0;
                    }
                }


                renderCalendar();
            }
        );


        scheduleNextMonth.addEventListener(
            'click',
            () => {

                const maxYear =
                    today.getFullYear() + 5;


                viewMonth += 1;


                if (viewMonth > 11) {

                    if (
                        viewYear < maxYear
                    ) {

                        viewMonth = 0;

                        viewYear += 1;

                    } else {

                        viewMonth = 11;
                    }
                }


                renderCalendar();
            }
        );


        document.addEventListener(
            'keydown',
            (event) => {

                if (
                    event.key === 'Escape' &&
                    !scheduleModal.hidden
                ) {

                    closeScheduleModal();
                }
            }
        );


        fillPeriodSelects();


        if (!scheduleTime.value) {

            scheduleTime.value =
                defaultTime();
        }
    }
});


// ================================================================
// CONTENT CALENDAR
// ================================================================
async function loadCalendar() {

    const calendarEl =
        document.getElementById("content-calendar");

    if (!calendarEl) {
        return;
    }

    const response =
        await fetch(
            "/api/content-calendar",
            { cache: "no-store", credentials: "same-origin" }
        );

    if (!response.ok) {
        calendarEl.textContent = "Unable to load scheduled content.";
        return;
    }

    const events =
        await response.json();


    const eventDetailsModal =
        document.getElementById("event-details-modal");
    const eventDetailsCard =
        eventDetailsModal?.querySelector(".event-details-card");
    const eventDetailsClose =
        document.getElementById("event-details-close");
    const eventDetailsBackdrop =
        document.getElementById("event-details-backdrop");
    const eventDetailsDate =
        document.getElementById("event-details-date");
    const eventDetailsTime =
        document.getElementById("event-details-time");
    const eventDetailsPlatform =
        document.getElementById("event-details-platform");
    const eventDetailsStatus =
        document.getElementById("event-details-status");
    const eventDetailsContent =
        document.getElementById("event-details-content");
    const eventDetailsImageSection =
        document.getElementById("event-details-image-section");
    const eventDetailsImages =
        document.getElementById("event-details-images");

    let lastFocusedEvent = null;

    const closeEventDetails = () => {
        if (!eventDetailsModal || eventDetailsModal.hidden) {
            return;
        }

        eventDetailsModal.hidden = true;
        document.body.classList.remove("event-details-open");
        lastFocusedEvent?.focus();
        lastFocusedEvent = null;
    };

    const openEventDetails = (info) => {
        const event = info.event;
        const props = event.extendedProps;
        const start = event.start;

        eventDetailsDate.textContent = start
            ? start.toLocaleDateString(undefined, {
                weekday: "long",
                year: "numeric",
                month: "long",
                day: "numeric"
            })
            : "Date not available";
        eventDetailsTime.textContent = start
            ? start.toLocaleTimeString(undefined, {
                hour: "numeric",
                minute: "2-digit"
            })
            : "Time not available";
        eventDetailsPlatform.textContent = props.platform || "Not specified";
        eventDetailsStatus.textContent = props.status || "Not specified";
        eventDetailsContent.textContent = props.post_content || "No content available.";

        const imageUrls = Array.isArray(props.image_urls) && props.image_urls.length
            ? props.image_urls
            : (props.image_url ? [props.image_url] : []);
        eventDetailsImageSection.hidden = !imageUrls.length;
        eventDetailsImages.replaceChildren();
        imageUrls.forEach((imageUrl, index) => {
            const link = document.createElement("a");
            link.href = imageUrl;
            link.target = "_blank";
            link.rel = "noopener noreferrer";

            const image = document.createElement("img");
            image.src = imageUrl;
            image.alt = `Scheduled post image ${index + 1}`;
            link.appendChild(image);
            eventDetailsImages.appendChild(link);
        });

        lastFocusedEvent = info.el;
        eventDetailsModal.hidden = false;
        document.body.classList.add("event-details-open");
        eventDetailsClose.focus();
    };

    eventDetailsClose?.addEventListener("click", closeEventDetails);
    eventDetailsBackdrop?.addEventListener("click", closeEventDetails);
    eventDetailsCard?.addEventListener("keydown", (event) => {
        if (event.key !== "Tab") {
            return;
        }

        const focusableElements = Array.from(
            eventDetailsCard.querySelectorAll(
                'button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'
            )
        ).filter((element) => element.getClientRects().length > 0);

        if (!focusableElements.length) {
            event.preventDefault();
            eventDetailsClose.focus();
            return;
        }

        const first = focusableElements[0];
        const last = focusableElements[focusableElements.length - 1];

        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });
    eventDetailsModal?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeEventDetails();
        }
    });


    const calendar =
        new FullCalendar.Calendar(
            calendarEl,
            {

                initialView:
                    "dayGridMonth",

                events:
                    events,

                eventContent: (arg) => {
                    const props = arg.event.extendedProps || {};
                    const imageUrls = Array.isArray(props.image_urls) && props.image_urls.length
                        ? props.image_urls
                        : (props.image_url ? [props.image_url] : []);
                    const wrapper = document.createElement("span");
                    wrapper.className = "calendar-event-content";
                    if (imageUrls.length) {
                        const thumbnail = document.createElement("img");
                        thumbnail.className = "calendar-event-content__image";
                        thumbnail.src = imageUrls[0];
                        thumbnail.alt = "";
                        thumbnail.loading = "lazy";
                        wrapper.appendChild(thumbnail);
                    }
                    const title = document.createElement("span");
                    title.className = "calendar-event-content__title";
                    title.textContent = arg.event.title || props.platform || "Scheduled post";
                    wrapper.appendChild(title);
                    if (imageUrls.length > 1) {
                        const count = document.createElement("span");
                        count.className = "calendar-event-content__count";
                        count.textContent = `+${imageUrls.length - 1}`;
                        count.setAttribute("aria-label", `${imageUrls.length - 1} more images`);
                        wrapper.appendChild(count);
                    }
                    return { domNodes: [wrapper] };
                },

                eventClick:
                    openEventDetails
            }
        );


    calendar.render();
}


loadCalendar();

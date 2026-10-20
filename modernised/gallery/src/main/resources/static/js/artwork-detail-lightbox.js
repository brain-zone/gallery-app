(() => {
    const initialize = () => {
        const dialog = document.querySelector(".artwork-detail-lightbox");
        const trigger = document.querySelector("[data-artwork-detail-lightbox-trigger]");
        const canvasImage = document.querySelector("[data-artwork-detail-canvas-image]");
        const selectors = document.querySelectorAll("[data-artwork-detail-rendition-selector]");

        if (!trigger || !canvasImage) {
            return;
        }

        selectors.forEach((selector) => {
            selector.addEventListener("click", (event) => {
                event.preventDefault();
                canvasImage.src = selector.href;
                canvasImage.alt = `${trigger.dataset.artworkTitle} ${selector.dataset.renditionLabel} image`;

                selectors.forEach((candidate) => candidate.removeAttribute("aria-current"));
                selector.setAttribute("aria-current", "true");
            });
        });

        if (!dialog || typeof dialog.showModal !== "function") {
            return;
        }

        trigger.addEventListener("click", (event) => {
            event.preventDefault();
            dialog.showModal();
        });

        dialog.addEventListener("close", () => {
            trigger.focus();
        });
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initialize, {once: true});
    } else {
        initialize();
    }
})();

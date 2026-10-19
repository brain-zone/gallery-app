(() => {
    const initialize = () => {
        const trigger = document.querySelector("[data-artwork-lightbox-trigger]");
        const dialog = document.querySelector(".artwork-lightbox");

        if (!trigger || !dialog || typeof dialog.showModal !== "function") {
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

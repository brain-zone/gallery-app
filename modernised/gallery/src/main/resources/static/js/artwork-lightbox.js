(() => {
    const initialize = () => {
        const trigger = document.querySelector("[data-artwork-lightbox-trigger]");
        const dialog = document.querySelector(".artwork-lightbox");

        if (!trigger || !dialog || typeof dialog.showModal !== "function") {
            return;
        }

        const artworks = Array.from(document.querySelectorAll("[data-lightbox-artwork]"), (element) => ({
            id: element.dataset.artworkId,
            title: element.dataset.title,
            artist: element.dataset.artist || null,
            imageUrl: element.dataset.imageUrl || null
        }));
        const image = dialog.querySelector(".artwork-lightbox__image");
        const title = dialog.querySelector("#artwork-lightbox-title");
        const artist = dialog.querySelector("[data-artwork-lightbox-artist]");
        const detailsLink = dialog.querySelector("[data-artwork-lightbox-details-link]");
        const previousButton = dialog.querySelector("[data-artwork-lightbox-previous]");
        const nextButton = dialog.querySelector("[data-artwork-lightbox-next]");
        const selectedArtworkId = dialog.dataset.selectedArtworkId;
        const selectedArtworkIndex = artworks.findIndex((artwork) => artwork.id === selectedArtworkId);
        let currentIndex = selectedArtworkIndex;

        if (!image || !title || !artist || !previousButton || !nextButton || currentIndex < 0) {
            return;
        }

        const updateNavigationState = () => {
            previousButton.disabled = currentIndex === 0;
            nextButton.disabled = currentIndex === artworks.length - 1;
        };

        const showArtwork = (index) => {
            const artwork = artworks[index];

            if (!artwork) {
                return;
            }

            currentIndex = index;
            title.textContent = artwork.title;
            image.alt = artwork.title;

            if (artwork.imageUrl) {
                image.src = artwork.imageUrl;
                image.hidden = false;
            } else {
                image.removeAttribute("src");
                image.hidden = true;
            }

            artist.textContent = artwork.artist ? `Artwork by ${artwork.artist}` : "";
            artist.hidden = !artwork.artist;

            if (detailsLink) {
                detailsLink.href = `/artworks/${encodeURIComponent(artwork.id)}`;
            }

            updateNavigationState();
        };

        previousButton.addEventListener("click", () => showArtwork(currentIndex - 1));
        nextButton.addEventListener("click", () => showArtwork(currentIndex + 1));

        dialog.addEventListener("keydown", (event) => {
            if (!dialog.open) {
                return;
            }

            if (event.key === "ArrowLeft" && currentIndex > 0) {
                event.preventDefault();
                showArtwork(currentIndex - 1);
            } else if (event.key === "ArrowRight" && currentIndex < artworks.length - 1) {
                event.preventDefault();
                showArtwork(currentIndex + 1);
            }
        });

        updateNavigationState();

        trigger.addEventListener("click", (event) => {
            event.preventDefault();
            showArtwork(selectedArtworkIndex);
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

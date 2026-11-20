// Runs in <head> to avoid a flash of the wrong theme
(function () {
    var saved = null;
    try { saved = localStorage.getItem('theme'); } catch (e) {}
    var dark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-bs-theme', saved || (dark ? 'dark' : 'light'));
})();
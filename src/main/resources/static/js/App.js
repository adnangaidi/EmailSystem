document.addEventListener('DOMContentLoaded', function () {

    // ---- dark / light toggle
    var themeBtn = document.getElementById('theme-toggle');
    var themeIcon = document.getElementById('theme-icon');
    function syncIcon() {
        if (!themeIcon) return;
        var dark = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        themeIcon.className = 'bi ' + (dark ? 'bi-sun' : 'bi-moon-stars');
    }
    if (themeBtn) {
        themeBtn.addEventListener('click', function () {
            var next = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-bs-theme', next);
            try { localStorage.setItem('theme', next); } catch (e) {}
            syncIcon();
        });
    }
    syncIcon();

    // ---- confirmation before destructive POST forms: <form data-confirm="Are you sure?">
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.getAttribute('data-confirm'))) e.preventDefault();
        });
    });

    // ---- show / hide password: <button data-toggle-password="#inputId">
    document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var input = document.querySelector(btn.getAttribute('data-toggle-password'));
            if (!input) return;
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            var icon = btn.querySelector('i');
            if (icon) icon.className = 'bi ' + (show ? 'bi-eye-slash' : 'bi-eye');
        });
    });

    // ---- attachments: list selected files and block files over 10 MB
    var files = document.getElementById('attachments');
    var list = document.getElementById('file-list');
    if (files && list) {
        files.addEventListener('change', function () {
            list.innerHTML = '';
            Array.prototype.forEach.call(files.files, function (f) {
                var li = document.createElement('li');
                var tooBig = f.size > 10 * 1024 * 1024;
                li.className = 'small ' + (tooBig ? 'text-danger' : 'text-body-secondary');
                li.textContent = f.name + ' (' + (f.size / 1024).toFixed(1) + ' KB)' + (tooBig ? ' - too large (max 10 MB)' : '');
                list.appendChild(li);
            });
        });
    }
});
// Detalles de interfaz comunes a todas las pantallas (no tocan la lógica del negocio):
//  - Al enviar un formulario POST, el botón muestra un spinner y se bloquea para evitar dobles envíos.
//  - Los mensajes de éxito se retiran solos tras unos segundos.
(function () {
    'use strict';

    var reducir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    document.addEventListener('submit', function (e) {
        // Si un onsubmit (p. ej. el confirm de anular) canceló el envío, no hay nada que mostrar.
        if (e.defaultPrevented) { return; }
        var form = e.target;
        if (!form || (form.getAttribute('method') || '').toLowerCase() !== 'post') { return; }
        if (form.dataset.enviando) { e.preventDefault(); return; }

        var boton = e.submitter || form.querySelector('button.btn[type=submit], button.btn:not([type])');
        if (!boton || !boton.classList.contains('btn')) { return; }

        form.dataset.enviando = '1';
        boton.classList.add('cargando');
        var texto = boton.getAttribute('data-cargando');
        if (texto) { boton.dataset.original = boton.textContent; boton.textContent = texto; }
        // Se deshabilita después de que el navegador toma los datos del formulario.
        setTimeout(function () { boton.disabled = true; }, 0);
    });

    // Al volver con "atrás" el navegador puede restaurar la página tal cual: se deja el botón usable otra vez.
    window.addEventListener('pageshow', function (e) {
        if (!e.persisted) { return; }
        document.querySelectorAll('form[data-enviando]').forEach(function (f) { delete f.dataset.enviando; });
        document.querySelectorAll('.btn.cargando').forEach(function (b) {
            b.classList.remove('cargando');
            b.disabled = false;
            if (b.dataset.original) { b.textContent = b.dataset.original; }
        });
    });

    document.querySelectorAll('.msg.ok').forEach(function (m) {
        setTimeout(function () {
            m.classList.add('saliendo');
            setTimeout(function () { m.remove(); }, reducir ? 0 : 450);
        }, 6000);
    });
})();

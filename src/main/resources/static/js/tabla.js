// Filtro en vivo para listados pequeños: <input data-filtra="idDeLaTabla"> oculta las filas que no contienen el texto.
(function () {
    function normalizar(t) {
        return (t || '').toString().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }
    document.querySelectorAll('input[data-filtra]').forEach(function (entrada) {
        var tabla = document.getElementById(entrada.getAttribute('data-filtra'));
        if (!tabla) { return; }
        var vacio = tabla.querySelector('tr.sin-resultados');
        function filtrar() {
            var q = normalizar(entrada.value.trim());
            var visibles = 0;
            tabla.querySelectorAll('tbody tr:not(.sin-resultados)').forEach(function (fila) {
                if (fila.querySelector('td.empty')) { return; }
                var coincide = !q || normalizar(fila.textContent).indexOf(q) !== -1;
                fila.classList.toggle('oculta', !coincide);
                if (coincide) { visibles++; }
            });
            if (vacio) { vacio.classList.toggle('visible', q && visibles === 0); }
        }
        entrada.addEventListener('input', filtrar);
    });
})();

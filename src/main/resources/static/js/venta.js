// Agrega filas al formulario de venta clonando la última y renumerando sus campos (lineas[n].campo).
(function () {
    var boton = document.getElementById('agregar-linea');
    var cuerpo = document.querySelector('#lineas-venta tbody');
    if (!boton || !cuerpo) {
        return;
    }
    boton.addEventListener('click', function () {
        var filas = cuerpo.querySelectorAll('tr.linea');
        var nueva = filas[filas.length - 1].cloneNode(true);
        var indice = filas.length;
        nueva.querySelectorAll('select, input').forEach(function (campo) {
            campo.name = campo.name.replace(/lineas\[\d+\]/, 'lineas[' + indice + ']');
            if (campo.tagName === 'SELECT') {
                campo.selectedIndex = 0;
            } else {
                campo.value = '';
            }
        });
        cuerpo.appendChild(nueva);
    });
})();

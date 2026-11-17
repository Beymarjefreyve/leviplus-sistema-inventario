// Registrar venta: buscador de productos (por nombre, presentación o código) y carrito.
// El formulario sigue enviando lineas[n].idProducto y lineas[n].cantidad, igual que antes.
(function () {
    var raiz = document.getElementById('venta-app');
    if (!raiz) { return; }

    var urlBuscar = raiz.getAttribute('data-url');
    var entrada = document.getElementById('buscar');
    var cajaResultados = document.getElementById('resultados');
    var cajaCarrito = document.getElementById('carrito');
    var cajaOcultos = document.getElementById('lineas-ocultas');
    var lblTotal = document.getElementById('total');
    var lblUnidades = document.getElementById('unidades');
    var lblCantidad = document.getElementById('cant-productos');
    var botonConfirmar = document.getElementById('confirmar');
    var botonVaciar = document.getElementById('vaciar');
    var aviso = document.getElementById('aviso-venta');

    var carrito = (window.CARRITO_INICIAL || []).map(function (l) {
        return { id: l.idProducto, nombre: l.nombre, presentacion: l.presentacion, codigo: l.codigo,
                 precio: Number(l.precio), existencia: Number(l.existencia), cantidad: Number(l.cantidad) || 1 };
    });
    var resultados = [];
    var temporizador = null;

    function dinero(n) { return '$ ' + Math.round(n).toLocaleString('es-CO'); }
    function el(tag, clase, texto) {
        var e = document.createElement(tag);
        if (clase) { e.className = clase; }
        if (texto !== undefined) { e.textContent = texto; }
        return e;
    }
    function mensaje(texto) {
        aviso.textContent = texto || '';
        aviso.style.display = texto ? 'block' : 'none';
    }

    // ---- búsqueda ----
    function buscar() {
        var q = entrada.value.trim();
        fetch(urlBuscar + '?q=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
            .then(function (r) { return r.ok ? r.json() : []; })
            .then(function (lista) { resultados = lista; dibujarResultados(q); })
            .catch(function () { cajaResultados.replaceChildren(el('p', 'empty', 'No se pudo buscar. Intenta de nuevo.')); });
    }

    function dibujarResultados(q) {
        cajaResultados.replaceChildren();
        if (!resultados.length) {
            cajaResultados.appendChild(el('p', 'empty', q ? 'No se encontró ningún producto con existencia para "' + q + '".'
                : 'No hay productos con existencia para vender.'));
            return;
        }
        resultados.forEach(function (p) {
            var fila = el('div', 'cat-item');
            var info = el('div', 'info');
            info.appendChild(el('b', '', p.nombre));
            info.appendChild(el('small', '', p.presentacion + ' · ' + p.codigo));
            fila.appendChild(info);
            fila.appendChild(el('span', 'pill' + (p.existencia <= 5 ? ' warn' : ''), p.existencia + ' disponibles'));
            var precio = el('b', '', dinero(p.precio));
            precio.style.minWidth = '90px';
            precio.style.textAlign = 'right';
            fila.appendChild(precio);
            var boton = el('button', 'btn sm', 'Agregar');
            boton.type = 'button';
            boton.addEventListener('click', function () { agregar(p); entrada.focus(); });
            fila.appendChild(boton);
            cajaResultados.appendChild(fila);
        });
        if (resultados.length === 20) {
            cajaResultados.appendChild(el('p', 'foot', 'Se muestran los primeros 20. Escribe más letras para afinar la búsqueda.'));
        }
    }

    entrada.addEventListener('input', function () {
        clearTimeout(temporizador);
        temporizador = setTimeout(buscar, 200);
    });
    // Enter (por ejemplo, con lector de código de barras): agrega el producto que coincide con el código exacto o el único resultado.
    entrada.addEventListener('keydown', function (e) {
        if (e.key !== 'Enter') { return; }
        e.preventDefault();
        var q = entrada.value.trim().toLowerCase();
        var exacto = resultados.filter(function (p) { return p.codigo.toLowerCase() === q; })[0];
        var elegido = exacto || (resultados.length === 1 ? resultados[0] : null);
        if (elegido) { agregar(elegido); entrada.value = ''; buscar(); }
        else { clearTimeout(temporizador); buscar(); }
    });

    // ---- carrito ----
    function agregar(p) {
        mensaje('');
        var linea = carrito.filter(function (l) { return l.id === p.idProducto; })[0];
        if (linea) {
            if (linea.cantidad < linea.existencia) { linea.cantidad++; }
            else { mensaje('No hay más existencia de ' + p.nombre + ' (' + p.existencia + ' disponibles).'); }
        } else {
            carrito.push({ id: p.idProducto, nombre: p.nombre, presentacion: p.presentacion, codigo: p.codigo,
                           precio: Number(p.precio), existencia: Number(p.existencia), cantidad: 1 });
        }
        dibujarCarrito();
    }

    function cambiar(id, delta) {
        var linea = carrito.filter(function (l) { return l.id === id; })[0];
        if (!linea) { return; }
        linea.cantidad = Math.max(1, linea.cantidad + delta);
        dibujarCarrito();
    }

    function quitar(id) {
        carrito = carrito.filter(function (l) { return l.id !== id; });
        dibujarCarrito();
    }

    function dibujarCarrito() {
        cajaCarrito.replaceChildren();
        cajaOcultos.replaceChildren();
        var total = 0, unidades = 0, excede = false;

        if (!carrito.length) {
            cajaCarrito.appendChild(el('p', 'empty', 'Busca un producto y pulsa Agregar.'));
        }
        carrito.forEach(function (l, i) {
            total += l.precio * l.cantidad;
            unidades += l.cantidad;
            var pasa = l.cantidad > l.existencia;
            if (pasa) { excede = true; }

            var fila = el('div', 'line');
            var arriba = el('div', 'top');
            arriba.appendChild(el('span', '', l.nombre + ' · ' + l.presentacion));
            var quitarBtn = el('button', 'link red', 'Quitar');
            quitarBtn.type = 'button';
            quitarBtn.setAttribute('aria-label', 'Quitar ' + l.nombre);
            quitarBtn.addEventListener('click', function () { quitar(l.id); });
            arriba.appendChild(quitarBtn);
            fila.appendChild(arriba);

            var abajo = el('div', 'bot');
            var cant = el('div', 'qty');
            var menos = el('button', '', '−'); menos.type = 'button'; menos.setAttribute('aria-label', 'Menos');
            menos.addEventListener('click', function () { cambiar(l.id, -1); });
            var num = el('span', '', String(l.cantidad));
            var mas = el('button', '', '+'); mas.type = 'button'; mas.setAttribute('aria-label', 'Más');
            mas.addEventListener('click', function () { cambiar(l.id, 1); });
            cant.appendChild(menos); cant.appendChild(num); cant.appendChild(mas);
            abajo.appendChild(cant);
            abajo.appendChild(el('span', 'sub', dinero(l.precio) + ' c/u'));
            abajo.appendChild(el('b', '', dinero(l.precio * l.cantidad)));
            fila.appendChild(abajo);
            if (pasa) { fila.appendChild(el('span', 'err', 'Supera la existencia (' + l.existencia + ' disponibles)')); }
            cajaCarrito.appendChild(fila);

            var h1 = document.createElement('input'); h1.type = 'hidden'; h1.name = 'lineas[' + i + '].idProducto'; h1.value = l.id;
            var h2 = document.createElement('input'); h2.type = 'hidden'; h2.name = 'lineas[' + i + '].cantidad'; h2.value = l.cantidad;
            cajaOcultos.appendChild(h1); cajaOcultos.appendChild(h2);
        });

        lblTotal.textContent = dinero(total);
        lblUnidades.textContent = String(unidades);
        lblCantidad.textContent = carrito.length + (carrito.length === 1 ? ' producto' : ' productos');
        botonConfirmar.disabled = !carrito.length || excede;
    }

    botonVaciar.addEventListener('click', function () { carrito = []; mensaje(''); dibujarCarrito(); entrada.focus(); });

    dibujarCarrito();
    buscar();
    entrada.focus();
})();

package com.leviplus.ventas.service;

import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.controller.PedidoForm;
import com.leviplus.ventas.controller.VentaForm;
import com.leviplus.ventas.model.DetallePedido;
import com.leviplus.ventas.model.Venta;
import com.leviplus.ventas.model.ClienteFrecuente;
import com.leviplus.ventas.model.ModalidadPedido;
import com.leviplus.ventas.model.OrigenPedido;
import com.leviplus.ventas.model.Pedido;
import com.leviplus.ventas.repository.ClienteFrecuenteRepository;
import com.leviplus.ventas.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RF-29 y RF-30. El pedido se registra y se consulta; al guardarlo no mueve inventario. Desde el pedido se puede
 * registrar la venta, que descuenta el inventario como cualquier venta. No se conecta con WhatsApp ni con otro servicio.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final ClienteFrecuenteRepository clientes;
    private final UsuarioRepository usuarios;
    private final VentaService ventasServicio;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos,
                         ClienteFrecuenteRepository clientes, UsuarioRepository usuarios,
                         VentaService ventasServicio) {
        this.ventasServicio = ventasServicio;
        this.pedidos = pedidos;
        this.productos = productos;
        this.clientes = clientes;
        this.usuarios = usuarios;
    }

    @Transactional
    public Pedido registrar(PedidoForm datos, String nombreUsuario) {
        List<PedidoForm.Linea> lineas = datos.getLineas().stream()
                .filter(l -> l.getIdProducto() != null).toList();
        if (lineas.isEmpty()) {
            throw new ReglaDeNegocioException("Agregue al menos un producto al pedido");
        }
        if (datos.getFecha().isAfter(LocalDate.now())) {
            throw new ReglaDeNegocioException("La fecha del pedido no puede ser futura");
        }
        String direccion = datos.getDireccion() == null ? "" : datos.getDireccion().trim();
        if (datos.getModalidad() == ModalidadPedido.DOMICILIO && direccion.isEmpty()) {
            throw new ReglaDeNegocioException("La dirección es obligatoria en los pedidos a domicilio");
        }
        ClienteFrecuente cliente = null;
        if (datos.getIdCliente() != null) {
            cliente = clientes.findById(datos.getIdCliente())
                    .orElseThrow(() -> new ReglaDeNegocioException("El cliente frecuente no existe"));
        }
        var usuario = usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ReglaDeNegocioException("Usuario no válido"));

        String observaciones = datos.getObservaciones() == null || datos.getObservaciones().isBlank()
                ? null : datos.getObservaciones().trim();
        Pedido pedido = new Pedido(cliente, datos.getNombreCliente().trim(), datos.getTelefono().trim(),
                datos.getFecha(), datos.getOrigen(), datos.getModalidad(),
                datos.getModalidad() == ModalidadPedido.DOMICILIO ? direccion : null, observaciones, usuario);

        Set<Integer> vistos = new HashSet<>();
        for (PedidoForm.Linea linea : lineas) {
            if (linea.getCantidad() == null || linea.getCantidad() < 1) {
                throw new ReglaDeNegocioException("La cantidad de cada producto debe ser al menos 1");
            }
            if (!vistos.add(linea.getIdProducto())) {
                throw new ReglaDeNegocioException("Un producto aparece repetido en el pedido");
            }
            Producto producto = productos.findById(linea.getIdProducto())
                    .orElseThrow(() -> new ReglaDeNegocioException("El producto seleccionado no existe"));
            if (!producto.estaActivo()) {
                throw new ReglaDeNegocioException("El producto " + producto.getNombre() + " está inactivo");
            }
            pedido.agregarDetalle(producto, linea.getCantidad());
        }
        return pedidos.save(pedido);
    }

    /**
     * Registra el pedido como venta: descuenta el inventario (lote que vence primero) y cuenta en el balance, igual
     * que una venta normal. Si no hay existencia suficiente no se hace nada y el pedido sigue pendiente.
     */
    @Transactional
    public Venta registrarComoVenta(Integer idPedido, String nombreUsuario) {
        Pedido pedido = pedidos.buscarConBloqueo(idPedido)
                .orElseThrow(() -> new ReglaDeNegocioException("El pedido no existe"));
        if (pedido.isRegistrado()) {
            throw new ReglaDeNegocioException("Este pedido ya se registró como venta (V-" + pedido.getIdVenta() + ")");
        }
        VentaForm form = new VentaForm();
        for (DetallePedido d : pedido.getDetalles()) {
            VentaForm.Linea linea = new VentaForm.Linea();
            linea.setIdProducto(d.getProducto().getIdProducto());
            linea.setCantidad(d.getCantidad());
            form.getLineas().add(linea);
        }
        Venta venta = ventasServicio.registrar(form, nombreUsuario);
        pedido.registrarComoVenta(venta.getIdVenta());
        return venta;
    }

    @Transactional(readOnly = true)
    public List<Pedido> consultar(LocalDate desde, LocalDate hasta, OrigenPedido origen) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(30) : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaDeNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        return pedidos.buscar(inicio, fin, origen);
    }

    @Transactional(readOnly = true)
    public Pedido obtener(Integer id) {
        Pedido pedido = pedidos.findById(id)
                .orElseThrow(() -> new ReglaDeNegocioException("El pedido no existe"));
        pedido.getDetalles().forEach(d -> d.getProducto().getNombre());
        pedido.getUsuario().getNombreUsuario();
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<ClienteFrecuente> clientesFrecuentes() {
        return clientes.findAllByOrderByNombreAsc();
    }

    /** Productos activos para elegir en el formulario. */
    @Transactional(readOnly = true)
    public List<Producto> productosActivos() {
        return productos.findAllByOrderByNombreAsc().stream().filter(Producto::estaActivo).toList();
    }

    /** Producto que se puede pedir (no depende de la existencia: el pedido no mueve inventario). */
    public record ProductoPedible(Integer idProducto, String codigo, String nombre, String presentacion,
                                  java.math.BigDecimal precio) {
    }

    private ProductoPedible pedible(Producto p) {
        return new ProductoPedible(p.getIdProducto(), p.getCodigoProducto(), p.getNombre(), p.getPresentacion(),
                p.getPrecioVenta());
    }

    /** Buscador del formulario de pedido: nombre, presentación o código; productos activos. */
    @Transactional(readOnly = true)
    public List<ProductoPedible> buscarPedibles(String texto, int limite) {
        List<Producto> base = texto == null || texto.isBlank() ? productos.findAllByOrderByNombreAsc()
                : productos.buscar(texto.trim());
        return base.stream().filter(Producto::estaActivo).limit(limite).map(this::pedible).toList();
    }

    /** Datos de los productos indicados, para reconstruir el carrito tras un error del formulario. */
    @Transactional(readOnly = true)
    public java.util.Map<Integer, ProductoPedible> pediblesPorIds(java.util.Collection<Integer> ids) {
        java.util.Map<Integer, ProductoPedible> resultado = new java.util.HashMap<>();
        for (Producto p : productos.findAllById(ids)) {
            if (p.estaActivo()) {
                resultado.put(p.getIdProducto(), pedible(p));
            }
        }
        return resultado;
    }
}

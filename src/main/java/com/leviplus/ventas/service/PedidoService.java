package com.leviplus.ventas.service;

import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.controller.PedidoForm;
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
 * RF-29 y RF-30. El pedido solo se registra y se consulta: no genera venta, no mueve inventario y no se conecta
 * con WhatsApp ni con ningún otro servicio.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final ClienteFrecuenteRepository clientes;
    private final UsuarioRepository usuarios;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos,
                         ClienteFrecuenteRepository clientes, UsuarioRepository usuarios) {
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
}

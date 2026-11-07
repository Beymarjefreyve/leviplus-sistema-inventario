package com.leviplus.ventas.service;

import com.leviplus.ventas.controller.ClienteForm;
import com.leviplus.ventas.model.ClienteFrecuente;
import com.leviplus.ventas.repository.ClienteFrecuenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** RF-27 y RF-28. */
@Service
public class ClienteService {

    private final ClienteFrecuenteRepository clientes;

    public ClienteService(ClienteFrecuenteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<ClienteFrecuente> listar() {
        return clientes.findAllByOrderByNombreAsc();
    }

    @Transactional
    public ClienteFrecuente registrar(ClienteForm datos) {
        return clientes.save(new ClienteFrecuente(datos.getNombre().trim(), datos.getTelefono().trim()));
    }
}

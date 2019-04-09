<?php
/**
 * Obtiene todas las metas de la base de datos
 */

require 'Coordenadas.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    // Manejar petición GET
    $Coordenadas = Coordenadas::getAll();

    if ($Coordenadas) {

        $datos["estado"] = 1;
        $datos["Coordenadas"] = $Coordenadas;

        print json_encode($datos, JSON_NUMERIC_CHECK);
    } else {
        print json_encode(array(
            "estado" => 2,
            "mensaje" => "Ha ocurrido un error"
        ));
    }
}
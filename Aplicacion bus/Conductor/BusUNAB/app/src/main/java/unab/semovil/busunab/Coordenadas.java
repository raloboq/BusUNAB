package unab.semovil.busunab;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Coordenadas {

    @SerializedName("estado")
    @Expose
    private Integer estado;
    @SerializedName("Coordenadas")
    @Expose
    private List<Coordenada> Coordenadas = new ArrayList<Coordenada>();

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public List<Coordenada> getCoordenadas() {
        return Coordenadas;
    }

    public void setCoordenadas(List<Coordenada> Coordenadas) {
        this.Coordenadas = Coordenadas;
    }

}

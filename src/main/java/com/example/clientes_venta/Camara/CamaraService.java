package com.example.clientes_venta.Camara;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CamaraService {

    private final CamaraRepository camaraRepo;
    private final TiendaRepository tiendaRepo;

    public List<Camara> listarPorTienda(Long tiendaId) {
        return camaraRepo.findByTiendaIdOrderByIdDesc(tiendaId);
    }

    public Camara guardar(Long tiendaId, Camara c) {
        Tienda t = tiendaRepo.findById(tiendaId).orElseThrow();
        c.setTienda(t);

        String raw = c.getRtspUrl();
        if (raw != null) raw = raw.trim();

        // si no trae rtsp://, lo agregamos
        if (raw != null && !raw.isBlank() && !raw.startsWith("rtsp://")) {
            raw = "rtsp://" + raw;
        }

        // si hay usuario/password, inyectamos credenciales en la URL
        if (raw != null && raw.startsWith("rtsp://")
                && c.getUsuario() != null && !c.getUsuario().isBlank()
                && c.getPassword() != null && !c.getPassword().isBlank()
                && !raw.startsWith("rtsp://" + c.getUsuario() + ":")) {

            String sinProto = raw.substring("rtsp://".length());
            raw = "rtsp://" + c.getUsuario() + ":" + c.getPassword() + "@" + sinProto;
        }

        c.setRtspUrl(raw);

        return camaraRepo.save(c);
    }

    public Camara obtener(Long id) {
        return camaraRepo.findById(id).orElseThrow();
    }

    public void eliminar(Long id) {
        camaraRepo.deleteById(id);
    }

    public void toggleActivo(Long id) {
        Camara c = obtener(id);
        c.setActivo(!c.isActivo());
        camaraRepo.save(c);
    }

    /**
     * Requiere ffprobe (FFmpeg) instalado y en PATH.
     * Devuelve true si logra obtener info del stream RTSP.
     */
    public boolean probarRtsp(String rtspUrl, Duration timeout) {
        try {
            // ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of default=nw=1:nk=1 <url>
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "default=nw=1:nk=1",
                    rtspUrl
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            // esperar timeout
            boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }

            int code = p.exitValue();
            if (code != 0) return false;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String out = br.readLine();
                return out != null && !out.isBlank(); // codec encontrado
            }
        } catch (Exception e) {
            return false;
        }
    }

    public String probarRtspDetalle(String rtspUrl, Duration timeout) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "default=nw=1:nk=1",
                    rtspUrl
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                return "Timeout: no respondió en " + timeout.toSeconds() + "s";
            }

            String out;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                out = br.readLine();
            }

            if (p.exitValue() != 0) {
                return "ffprobe error: " + (out == null ? "(sin salida)" : out);
            }

            return (out != null && !out.isBlank())
                    ? "OK (codec: " + out + ")"
                    : "Conectó, pero no detectó video";
        } catch (Exception e) {
            return "Excepción: " + e.getMessage();
        }
    }

}

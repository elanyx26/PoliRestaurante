package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** RNF-06: las contraseñas nunca se guardan en texto plano (PBKDF2 con sal). */
public final class Seguridad {
    private Seguridad() { }

    private static final String PREFIJO = "pbkdf2$";
    private static final int ITERACIONES = 65_000;
    private static final int LONGITUD_BITS = 256;

    public static boolean estaProtegida(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    public static String proteger(String plano) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        byte[] hash = derivar(plano, sal);
        return PREFIJO + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String plano, String guardada) {
        if (plano == null || !estaProtegida(guardada)) {
            return false;
        }
        String[] partes = guardada.substring(PREFIJO.length()).split("\\$");
        if (partes.length != 2) {
            return false;
        }
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] esperado = Base64.getDecoder().decode(partes[1]);
        return MessageDigest.isEqual(esperado, derivar(plano, sal));
    }

    private static byte[] derivar(String plano, byte[] sal) {
        try {
            PBEKeySpec spec = new PBEKeySpec(plano.toCharArray(), sal, ITERACIONES, LONGITUD_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña", e);
        }
    }
}

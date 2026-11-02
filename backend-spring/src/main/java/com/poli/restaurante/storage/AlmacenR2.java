package com.poli.restaurante.storage;

import java.net.URI;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Cloudflare R2 (o cualquier almacenamiento compatible con S3). Cloudflare exige un medio de pago para activar R2,
 * aunque el uso gratuito (10 GB) alcance de sobra.
 */
public class AlmacenR2 implements AlmacenArchivos, AutoCloseable {

    private final S3Client s3;
    private final String bucket;

    public AlmacenR2(String endpoint, String bucket, String accessKeyId, String secretAccessKey) {
        if (vacio(endpoint) || vacio(bucket) || vacio(accessKeyId) || vacio(secretAccessKey)) {
            throw new IllegalStateException(
                    "STORAGE_PROVIDER=r2 requiere R2_ENDPOINT, R2_BUCKET, R2_ACCESS_KEY_ID y R2_SECRET_ACCESS_KEY");
        }
        this.bucket = bucket;
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                // R2 usa direcciones con el bucket en la ruta y no acepta la codificación por bloques.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
                // Desde el SDK 2.30 las sumas de verificación van por defecto y R2 las rechaza: solo cuando son obligatorias.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Override
    public void guardar(String nombre, byte[] datos, String tipoContenido) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(nombre).contentType(tipoContenido).build(),
                RequestBody.fromBytes(datos));
    }

    @Override
    public Optional<Resource> leer(String nombre) {
        try {
            return Optional.of(new ByteArrayResource(s3.getObjectAsBytes(b -> b.bucket(bucket).key(nombre)).asByteArray()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void borrar(String nombre) {
        s3.deleteObject(b -> b.bucket(bucket).key(nombre));
    }

    @Override
    public void close() {
        s3.close();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}

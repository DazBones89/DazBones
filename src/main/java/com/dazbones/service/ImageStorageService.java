package com.dazbones.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ImageStorageService {
    private final Path root;
    public ImageStorageService(@Value("${app.upload-dir}") String dir) {
        root = Paths.get(dir).toAbsolutePath().normalize();
    }

    private record ValidImage(byte[] bytes, String extension) {}

    private ValidImage validate(MultipartFile file, boolean jpegOnly) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("画像ファイルを選択してください");
        if (file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("画像は5MB以内で選択してください");
        String filename = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        if (ext.equals("jpg")) ext = "jpeg";
        if (!(jpegOnly ? ext.equals("jpeg") : Set.of("jpeg", "png", "webp").contains(ext)))
            throw new IllegalArgumentException(jpegOnly ? "JPG/JPEG画像を選択してください" : "JPG・PNG・WEBP画像を選択してください");
        byte[] bytes;
        try (InputStream in = file.getInputStream()) { bytes = in.readNBytes(5 * 1024 * 1024 + 1); }
        if (bytes.length > 5 * 1024 * 1024) throw new IllegalArgumentException("画像は5MB以内で選択してください");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("画像として読み込めないファイルです");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String actual = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (actual.equals("jpg")) actual = "jpeg";
                String mime = Objects.toString(file.getContentType(), "").toLowerCase(Locale.ROOT);
                if (!ext.equals(actual) || (!mime.isEmpty() && !mime.equals("application/octet-stream")
                        && !mime.equals("image/" + actual))) throw new IllegalArgumentException("拡張子と画像形式が一致しません");
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 25_000_000 || pixels <= 0) throw new IllegalArgumentException("画像は2500万画素以内にしてください");
                if (reader.read(0) == null) throw new IllegalArgumentException("画像が破損しています");
            } finally { reader.dispose(); }
        }
        return new ValidImage(bytes, ext.equals("jpeg") ? "jpg" : ext);
    }

    public String savePlayer(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        ValidImage image = validate(file, false);
        Path dir = root.resolve("players");
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "." + image.extension());
        atomicWrite(target, image.bytes());
        return "/uploads/images/players/" + target.getFileName();
    }

    public void saveGroup(MultipartFile file) throws IOException {
        ValidImage image = validate(file, true);
        Files.createDirectories(root);
        atomicWrite(root.resolve("group-photo.jpg"), image.bytes());
    }

    private void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    public void discard(String url) {
        if (url == null || !url.matches("/uploads/images/players/[a-zA-Z0-9-]+\\.(jpg|jpeg|png|webp)")) return;
        try { Files.deleteIfExists(root.resolve("players").resolve(url.substring(url.lastIndexOf('/') + 1))); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unused player image cleanup failed", e); }
    }
}

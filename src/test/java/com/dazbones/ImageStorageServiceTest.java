package com.dazbones;
import com.dazbones.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ImageStorageServiceTest {
    @TempDir Path directory;
    private byte[] image(String format) throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),format,out);return out.toByteArray();
    }
    @Test void validImageCanBeSavedAndOldImageSurvivesInvalidReplacement() throws Exception {
        var service=new ImageStorageService(directory.toString());
        byte[] valid=image("jpg");service.saveGroup(new MockMultipartFile("file","group.jpg","image/jpeg",valid));
        assertThatThrownBy(()->service.saveGroup(new MockMultipartFile("file","group.jpg","image/jpeg",new byte[]{1,2,3})))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Files.readAllBytes(directory.resolve("group-photo.jpg"))).isEqualTo(valid);
        try(var files=Files.list(directory)){assertThat(files.count()).isEqualTo(1);}
    }
    @Test void rejectsMismatchedFormatAndOversizeUploads() throws Exception {
        var service=new ImageStorageService(directory.toString());
        var png=new MockMultipartFile("imageFile","photo.jpg","image/jpeg",image("png"));
        assertThatThrownBy(()->service.savePlayer(png)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("一致");
        var huge=new MockMultipartFile("imageFile","photo.jpg","image/jpeg",new byte[5*1024*1024+1]);
        assertThatThrownBy(()->service.savePlayer(huge)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5MB");
    }
    @Test void createsDistinctPlayerFilesAndRestrictsCleanupToPlayerDirectory() throws Exception {
        var service=new ImageStorageService(directory.toString());
        var file=new MockMultipartFile("imageFile","../../photo.png","image/png",image("png"));
        String first=service.savePlayer(file),second=service.savePlayer(file);
        assertThat(first).isNotEqualTo(second);
        Path sentinel=directory.resolve("keep.png");Files.write(sentinel,new byte[]{9});
        service.discard("/uploads/images/players/../keep.png");assertThat(sentinel).exists();
        service.discard(first);
        try(var files=Files.list(directory.resolve("players"))){assertThat(files.count()).isEqualTo(1);}
    }
}

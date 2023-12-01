package br.com.joaovq.filestorageapi;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("api/files")
public class FileStorageController {
    private final Path fileStorageLocation;

    public FileStorageController(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
    }

    @PostMapping
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        var fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {
            var targetLocation = fileStorageLocation.resolve(fileName);
            file.transferTo(targetLocation);
            var fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("api/files/download/")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok("Upload completed! Download link: " + fileDownloadUri);
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body("Upload error");
        }
    }

    @GetMapping("download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Path file = fileStorageLocation.resolve(fileName).normalize();

        try {
            Resource resource = new UrlResource(file.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null)
                contentType = "application/octet-stream";

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("list")
    public ResponseEntity<List<String>> getFiles() {
        try {
            List<String> fileNames = Files.list(fileStorageLocation)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
            return ResponseEntity.ok(fileNames);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

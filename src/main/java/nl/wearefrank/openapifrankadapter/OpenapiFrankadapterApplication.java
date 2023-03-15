package nl.wearefrank.openapifrankadapter;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import org.xml.sax.SAXException;

import java.io.*;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Disable security
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@RestController
public class OpenapiFrankadapterApplication {

    public static void main(String[] args) {

        SpringApplication.run(OpenapiFrankadapterApplication.class, args);

        // TODO: Clean the processing folder on startup

    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> postFile(@RequestParam("file") MultipartFile file) throws IOException, URISyntaxException, SAXException {

        //// INITIALIZATION ////
        // Generate random folder for which to process the API request
        String uuid = UUID.randomUUID().toString() + LocalDateTime.now();
        uuid = uuid.replaceAll("[^a-zA-Z0-9]", "");

        // Convert the incoming JSON multipart file to String
        String json = new String(file.getBytes());

        // Read the openapi specification
        SwaggerParseResult result = new OpenAPIParser().readContents(json, null, null);

        OpenAPI openAPI = result.getOpenAPI();
        LinkedList<GenFiles> genFiles = XMLGenerator.execute(openAPI);

        byte[] response = convertToZip(genFiles);

        // Return the zip file as a resource
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uuid + ".zip\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(new ByteArrayInputStream(response)));
    }

    public static byte[] convertToZip(LinkedList<GenFiles> files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

        for (GenFiles file : files) {
            ZipEntry entry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(file.getContent());
            zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
        byteArrayOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }
}

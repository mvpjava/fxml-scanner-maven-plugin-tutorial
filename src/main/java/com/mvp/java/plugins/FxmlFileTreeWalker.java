package com.mvp.java.plugins;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FxmlFileTreeWalker extends SimpleFileVisitor<Path> {

    /*Key =  Fxml filename (i.e: "Main.fxml")
     *Value = String to fxml file starting at fxml
     *       sub-directory under resources directory. i.e: "/fxml/Main.fxml" */
    private Map<String, String> fxmlResourcesMap;

    public FxmlFileTreeWalker() {
        createResourceMap();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!attrs.isDirectory() && isFxmlFile(file)) {
            String nameWithoutExtension = Files.getNameWithoutExtension(file.getFileName().toString());
            String pathToFXML = getPathToFXML(file);
            if (notNullOrEmpty(nameWithoutExtension,pathToFXML)) {
                fxmlResourcesMap.put(nameWithoutExtension, pathToFXML);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    private boolean isFxmlFile(Path possibleFxmlFile) {
        String file = possibleFxmlFile.getFileName().toString();
        String fileExtension = Files.getFileExtension(file);
        return fileExtension.equals("fxml");
    }

    private String getPathToFXML(Path path) {
        for (int i = 0; i < path.getNameCount(); i++) {
            if (path.getName(i).toString().equals("fxml")) {
                Path subpath = path.subpath(i, path.getNameCount());
                String pathToFXMLFile = File.separator.concat(subpath.toString());
                return pathToFXMLFile.replaceAll("\\\\", "/");
            }
        }
        return ""; //when not found, return empty string (much like Guava)
    }

    public Map<String, String> getFxmlResourcesMap() {
        return Collections.unmodifiableMap(fxmlResourcesMap);
    }

    void createResourceMap() {
        fxmlResourcesMap = new HashMap<>();
    }

    //for debugging
    void printMapContents() {
        fxmlResourcesMap.forEach((key, value) -> {
            System.out.println("K<" + key + "> Value<" + value + ">");
        });
    }

    private boolean notNullOrEmpty(String nameWithoutExtension, String pathToFXML) {
        return  (!Strings.isNullOrEmpty(nameWithoutExtension) && !Strings.isNullOrEmpty(pathToFXML));
    }
}
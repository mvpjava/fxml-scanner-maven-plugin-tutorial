package com.mvp.java.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Mojo(name = "fxmlScanner", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class FxmlScannerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    //packageName
    @Override

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println("*********************************************************");
        System.out.println("************* MVP JAVA MOJO FXML PLUGIN *****************");
        System.out.println("*********************************************************");

        Map<String, String> fxmlResourcesMap = retrieveFXMLResources();
        if (fxmlResourcesMap.isEmpty()) {
            getLog().info("FXML Plugin: No FXML files found under src/main/resources/fxml/. Plugin will not auto-generate code.");
            return;
        }
        getLog().info(String.format("FXML Plugin: Found %d FXML file(s).", fxmlResourcesMap.size()));

        generateDynamicFxmlEnum(fxmlResourcesMap);
    }

    private Map<String, String> retrieveFXMLResources() {
        FxmlFileTreeWalker fxmlFileTreeWalker = new FxmlFileTreeWalker();
        for (Resource resource : resources) {
            Path fxmlResourcePath = Paths.get(resource.getDirectory()).resolve("fxml");//right under resources directory
            try {
                Files.walkFileTree(fxmlResourcePath, fxmlFileTreeWalker);
            } catch (IOException e) {
                getLog().error("Unable to retrieve FXML resources under path:" + fxmlResourcePath.toString());
            }
        }
        return fxmlFileTreeWalker.getFxmlResourcesMap();
    }

    private void generateDynamicFxmlEnum(Map<String, String> scannedFxmlFiles) {
        String destinationPackage = getPackageNameDestination();
        getLog().info("FXML Plugin: Enum will be generated in package: " + destinationPackage);

        EnumGenerator enumGenerator = new EnumGenerator(scannedFxmlFiles, destinationPackage);
        try {
            enumGenerator.autoGenerateEnum();
        } catch (Exception exception) {
            getLog().error("An error occurred while auto-generating the enum:" + exception);
        }
    }

    private String getPackageNameDestination() {
        Plugin fxmlScannerPlugin = getPluginByArtifactId("fxmlScanner-maven-plugin");
        return getPluginConfigurationValueforChild(fxmlScannerPlugin, "packageName");
    }

    private String getPluginConfigurationValueforChild(Plugin plugin, String childName) {
        Object configuration = plugin.getConfiguration();
        if (Objects.nonNull(configuration) && (configuration instanceof Xpp3Dom)) {
            Xpp3Dom xml = (Xpp3Dom) configuration;
            Xpp3Dom child = xml.getChild(childName);
            if (Objects.nonNull(child)) {
                return child.getValue();
            }
        }
        return "";
    }

    private Plugin getPluginByArtifactId(String artifactId) {
        for (Plugin plugin : getBuildPlugins()) {
            if (artifactId.equals(plugin.getArtifactId())) {
                return plugin;
            }
        }
        return null;
    }

    private List<Plugin> getBuildPlugins() {
        return project.getBuildPlugins();
    }
}

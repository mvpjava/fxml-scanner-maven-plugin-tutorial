package com.mvp.java.plugins;

import com.google.common.base.Strings;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.lang.model.element.Modifier;

public class EnumGenerator {

    private final Map<String, String> fxmlRsourceMap;
    private final String destinationPackage;

    EnumGenerator(Map<String, String> fxmlRsourceMap, String destinationPackage) {
        if (Objects.isNull(fxmlRsourceMap) || fxmlRsourceMap.isEmpty()
                || Strings.isNullOrEmpty(destinationPackage)) {
            throw new IllegalArgumentException("Cannot auto-gen code: Input parameters are either null or empty");
        }
        this.fxmlRsourceMap = fxmlRsourceMap;
        this.destinationPackage = destinationPackage;
    }

    public void autoGenerateEnum() throws Exception {
        TypeSpec enumTypeSpec = null;
        try {
            enumTypeSpec = buildEnumTypeSpec("FxmlEnum");
        } catch (Exception exception) {
            throw new Exception("An error occurred while attempting to auto-generate the enum code.", exception);
        }

        try {
            writeEnumToJavaFile(enumTypeSpec, destinationPackage);
        } catch (IOException exception) {
            throw new IOException("An IO error while writing out the auto-generated enum", exception);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// PRIVATE ///////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    private TypeSpec buildEnumTypeSpec(String enumName) throws Exception {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumName)
                .addModifiers(Modifier.PUBLIC);

        MethodSpec abstractGetFxmlFileMethod = createAbstractMethodSignature("getFxmlFile", String.class);
        MethodSpec abstractGetTitleMethod = createAbstractMethodSignature("getTitle", String.class);

        //for each enum constant, override the abstract method necessary
        fxmlRsourceMap.forEach((key, value) -> {
            MethodSpec overridenGetFxmlFileMethod
                    = overrideAbstractMethod(abstractGetFxmlFileMethod, "return $S", value);

            MethodSpec overridenGetTitleMethod
                    = overrideAbstractMethod(abstractGetTitleMethod,
                            "return getStringFromResourceBundle($S)", key.toLowerCase().concat(".title"));

            TypeSpec anonymousClassBuilder = TypeSpec.anonymousClassBuilder("")
                    .addMethod(overridenGetFxmlFileMethod)
                    .addMethod(overridenGetTitleMethod)
                    .build();

            enumBuilder.addEnumConstant(key.toUpperCase(), anonymousClassBuilder);
        });

        MethodSpec bundleMethod = createGetStringFromResourceBundleMethod();
        enumBuilder.addMethod(abstractGetFxmlFileMethod) //place abstract method decalation at end
                   .addMethod(abstractGetTitleMethod)
                   .addMethod(bundleMethod);
        return enumBuilder.build(); //auto-generated enum complete
    }

    private MethodSpec createAbstractMethodSignature(String abstractMethodName, Type returnType) {
        MethodSpec.Builder baseMethodSignature = baseMethodSignature(abstractMethodName, returnType);
        baseMethodSignature.addModifiers(Modifier.ABSTRACT);
        return baseMethodSignature.build();
    }

    private MethodSpec createMethodSignature(String methodName, Type returnType) {
        return baseMethodSignature(methodName, returnType).build();
    }

    private MethodSpec.Builder baseMethodSignature(String methodName, Type returnType) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);
    }

    String getStringFromResourceBundle(String key) {
        return ResourceBundle.getBundle("Bundle").getString(key);
    }

    private MethodSpec overrideAbstractMethod(MethodSpec abstractMethodSpec, String format, Object... args) {
        return MethodSpec
                .methodBuilder(abstractMethodSpec.name)//name of abstract method
                .addModifiers(Modifier.PUBLIC)
                .returns(abstractMethodSpec.returnType)
                .addStatement(format, args)
                .addAnnotation(Override.class)
                .build();
    }

    private MethodSpec createGetStringFromResourceBundleMethod() {
        return   baseMethodSignature("getStringFromResourceBundle", String.class)
                .addParameter(String.class, "key")
                .addStatement("return java.util.ResourceBundle.getBundle(\"Bundle\").getString(key)")
                .build();
    }
        
    private void writeEnumToJavaFile(TypeSpec enumTypeSpec, String packageName) throws IOException {
        JavaFile javaFile = JavaFile.builder(packageName, enumTypeSpec)
                .addFileComment("AUTO_GENERATED BY MVP Java Maven Plugin")
                .build();
        //write out to the actual .java file (i.e: "src.main.java.com.mvp.java.view.autogen.FxmlEnum.java")
        javaFile.writeTo(Paths.get("./src/main/java"));//root maven source
    }

}

#!/bin/bash
rm -rf out/class_files
rm -f sources.txt
find -name "*.java" >sources.txt
mkdir -p out/class_files
javac -classpath "/local/packages/idea-IC-172.4155.36/plugins/junit/lib/junit-jupiter-api-5.0.0.jar:/local/packages/idea-IC-172.4155.36/plugins/junit/lib/opentest4j-1.0.0.jar:lib/*" @sources.txt -d out/class_files/
cp -a src/fx/resources/ out/class_files/fx/
cp src/fx/layout.fxml out/class_files/fx/
java -cp out/class_files:lib/*  fx.Main

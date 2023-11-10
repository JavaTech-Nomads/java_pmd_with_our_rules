This repo is used for creating and testing our custom rules as well as building a java only distribution of pmd with our rules packaged.

To build run ./mvnw clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Pcli-dist

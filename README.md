# rest-assured-first-touch
Working examples of API testing for oauth2 using rest-assured and Junit

# Requirements:
To start using Rest Assured, you will need to add REST Assured dependencies to your project. Example for Gradle:

```
dependencies {
    testImplementation 'io.rest-assured:rest-assured:4.4.0'
    testImplementation 'io.rest-assured:json-path:4.4.0'
    testImplementation 'io.rest-assured:xml-path:4.4.0'
    testImplementation 'io.rest-assured:json-schema-validator:4.4.0'
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.7.0'
}
```
# Usage
1. Put RestTest.java in src/test/java/ directory of your Gradle Java project.
2. Run Build in Gradle and run Tests

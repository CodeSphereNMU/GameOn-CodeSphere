# .mvn Directory

## jvm.config

Contains: `-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT`

This setting tells the JVM to use the Windows root certificate store for TLS connections. It resolves PKIX certificate errors that occur when Maven downloads dependencies through JDK installations that do not include Maven Central's CA chain in their default `cacerts` file (encountered with JDK 23 on Windows).

All team members develop on Windows, so this setting is appropriate.

## wrapper/maven-wrapper.properties

Configures the Maven Wrapper to download Maven 3.9.9 automatically. No system-wide Maven installation is needed.

# Eclipse Kura Addons [![Build status](https://api.travis-ci.org/ctron/kura-addons.svg)](https://travis-ci.org/ctron/kura-addons) ![Maven Central](https://img.shields.io/maven-central/v/de.dentrassi.kura.addons/kura-addons.svg)

This repository hosts a few addons for [Eclipse Kura™](https://eclipse.org/kura "Link to Eclipse Kura™").

Also see: https://dentrassi.de/kura-addons/

## Compile it yourself

In order to compile all bundles yourself, simply clone this repository and issue the following command:

~~~sh
mvn clean install
~~~

This should build you all modules which also got uploaded to Maven Central.

In order to build other addons just change into the directory and issue `mvn install` manually:

~~~sh
cd examples
mvn clean install
~~~

## Addons

### Apache FileInstall

Allows to dynamically load additional OSGi bundles from the file system.

### Camel AMQP

Provides the Camel AMQP endpoint.

### Camel Milo – OPC UA

Provides the Camel OPC UA server and client endpoint.

### Camel Paho – MQTT

Provides the Camel MQTT endpoint.

### Camel SNMP

Provides the Camel SNMP endpoint.

### Camel Eclipse NeoSCADA

Provides the Camel NeoSCADA endpoint.

### Camel Groovy

Provides the Camel Groovy language extension.

### Camel HTTP

Provides the Camel HTTP4 endpoint.

### Camel IEC 60870-5-104

Provides the Camel IEC 60870-5-104 server and client endpoint.

### Camel Jetty

Provides the Camel Jetty endpoint.

### Camel Swagger

Provides Camel Swagger support.

### Camel Weather

Provides the Camel OpenWeather endpoint.

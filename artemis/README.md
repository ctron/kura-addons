# ActiveMQ Artemis addon

This is an addon which provides an embedded instance of ActiveMQ Artemis for Eclipse Kura.

**Note:** It does require Kura 3.0.0, which is not released yet.

The embedded version of Artemis is the release 1.5.0.

## Compile

In order to use this bundle you will need to build it yourself. This is required since Kura itself
does not publish its artifacts on Maven Central.

This can be done by executing the following command in this directory:

	mvn clean install
	
In the folder `de.dentrassi.kura.addons.artemis.server/target` you will find a `.dp` package which
can be dropped into Kura.

## Example

A very basic test setup would be to enter the following information into the configuration section
in the Kura Web UI for Artemis:

Broker XML: [de.dentrassi.kura.artemis.server/src/test/resources/example1.xml](de.dentrassi.kura.artemis.server/src/test/resources/example1.xml)

Required protocols: `AMQP, MQTT`

User configuration:

    guest=test12|guest

Default user name: `guest`
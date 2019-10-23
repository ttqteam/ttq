## Building 

### Building deployment JAR

$ sbt universal:packageBin

## Configuring

All configuration is stored in [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) 
("Human-Optimized Config Object Notation") configuration file 
(processed by the [PureConfig](https://github.com/pureconfig/pureconfig) library).

Location of the configuration file is controlled by the `config.file` property (`-Dconfig.file=...`).
Default is `./conf/ttq.conf`.

Please see [example config file](./conf/ttq.conf) provided.

### Storing Passwords

One solution is to make your main (unprotected) config file to import configuration object(s) from another (protected file).
Then run server under the account which have rights to read this second protected file. For example:

Main config file (readable by everyone):
```
...
include "secrets.conf"
db {
  url = "jdbc:clickhouse://localhost:8123"
  parameters = {
    user = user_name
    // we don't want password here
    // password = 
  }
}
...
```

File `secrets.conf` (readable only by the user under which server process runs):
```
db.parameters.password=securePassword123
```

## Running

### Running Jar
TODO

### Running With sbt
```
$ sbt
sbt> set containerPort in Jetty := 1234
sbt> set javaOptions += "-Dconfig.file=../ttq-int/conf/ttq.conf"
sbt> set javaOptions += "-Dlogs.dir=../logs"
sbt> set javaOptions += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
sbt> jetty:start
```

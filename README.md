# ergotxverify
Project to verify a transaction using a custom context!

### Follow the below steps:

1. This project has a dependency on the appkit project, which means you have to build the appkit project first or somehow resolve this dependency by getting its jar from a repository or other third parties.

2. In order to build appkit project, [downloaad Graal VM with java 8](https://github.com/graalvm/graalvm-ce-builds/releases) and use it as your JAVA_HOME to build the appkit. Particularly after extracting Graal VM and setting it as your JAVA_HOME appropriatly, use the following command in appkit project root folder to build it:
~~~bash
git checkout develop
sbt assembly
~~~
then its jar file is created in a path sth like this: `target/scala-2.12/ergo-appkit-develop-60478389-SNAPSHOT.jar`.

3. copy the created jar in lib folder of the CustomVerifier project and change your JAVA_HOME to use java 11. Then use the following command in CustomVerifier root project to build its jar.
~~~bash
sbt assembly
~~~
then its jar file is created in a path sth like this: `target/scala-2.12/tx-verify-assembly-1.0-SNAPSHOT.jar`.


NOTE: The above explained steps can also be viewed in gitlab-ci.yml file in the project.

4. You can run the project with the following command:
~~~bash
java -jar target/scala-2.12/tx-verify-assembly-1.0-SNAPSHOT.jar
~~~

IMPORTANT: Please also follow the configuration of this project:

There are a few configurations to do in the project:

1. you need to configure node address and network type in `conf/node_conf.json` file.
2. you need to configure http and https ports in `conf/application.conf` like below:
~~~bash
http.port: 9001 or anything
https.port: 9002 or anything
~~~

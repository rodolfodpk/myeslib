myeslib
=======

An Event Sourcing library / toolkit 

It was inspired by:

http://www.jayway.com/2013/06/20/dont-publish-domain-events-return-them/

and off course:

https://github.com/gregoryyoung/m-r

Getting Started
===============

cd myeslib

mvn clean install

cd myeslib-hazelcast-example

java -jar target/myeslib-hazelcast-example-0.0.1-SNAPSHOT.jar

then in other console

cd myeslib-cmd-producer

java -jar target/myeslib-cmd-producer-0.0.1-SNAPSHOT.jar

and the same as above for jdbi-example

Notes
=====

Your IDE must support http://projectlombok.org/

Disclaimer
==========

There are 2 packages within module 3rd-party with (intact) code from :

http://gsd.di.uminho.pt/members/jop/mm4j -> multimethods magic (thanks José Orlando)

https://code.google.com/p/google-gson -> since I did not found RuntimeTypeAdapter classes within gson-2.2.4 jar



camel-glite
============

I frequently find myself writing Groovy integration scripts.  Most of the
time enterprise routing is overkill in these situations, but writing
the connector code can be a real pain.  camel-glite aims at
making this easier by providing a simple class to use Apache Camel's
endpoints directly without the routing.  Essentially this is a wrapper
around camel's ConsumerTemplate and ProducerTemlpate with a few extra 
bells and whistles.

Download
========
* Download Directly

    https://bintray.com/version/show/files/thomas-p-barker/camel-glite/camel-glite/0.1/com/github/camel-script/camel-glite/0.1

* Groovy Grape
* Maven

Examples
========

Most all functionality comes from the class @CamelScript@ which has
methods for receiving and sending data to any supported camel endpoint.
All examples and functionality can be seen in the groovy docs [here](http://tbarker9.github.io/camel-glite/groovydoc/).


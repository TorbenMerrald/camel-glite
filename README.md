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

    http://dl.bintray.com/upennlib/camel/com/github/camel-glite/camel-glite/0.2.1/camel-glite-0.2.1.jar

* Groovy Grape

    ```groovy
    @Grab("com.github.camel-glite:camel-glite:0.2.1")
    @GrabResolver(name='camel-glite', root='http://jcenter.bintray.com/')    
    ```

* Maven

    ```xml
    <project>
        <repositories>
            <repository>
                <id>jcenter</id>
                <url>http://jcenter.bintray.com/</url>
            </repository>
        </repositories>
        
        ...
        
        <dependencies>
            <dependency>
                <groupId>com.github.camel-glite</groupId>
                <artifactId>camel-glite</artifact>
                <version>0.2.1</version>
            </dependency>
            ...
        </dependencies>
    </project>
    ```


Examples
========

Most all functionality comes from the class @CamelScript@ which has
methods for receiving and sending data to any supported camel endpoint.
All examples and functionality can be seen in the groovy docs [here](http://tbarker9.github.io/camel-glite/groovydoc/).


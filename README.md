camel-glite
============

I frequently find myself fighting between writing simple groovy scripts for integration or batch jobs, vs using 
something more powerful like apache camel.  This project aims to bring the best of these two worlds together to
make camel scripting easy.

Download
========
* Download Directly

    http://dl.bintray.com/upennlib/camel/com/github/camel-glite/camel-glite

* Groovy Grape

    ```groovy
    @Grab("com.github.camel-glite:camel-glite:<version>")
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
                <version>version</version>
            </dependency>
            ...
        </dependencies>
    </project>
    ```


CamelScript
===========

Most all functionality comes from the class `CamelScript` which has
methods for receiving and sending data to any supported camel endpoint.
Many functions in this class revolve around wrapping camel's `ConsumerTemplate`
and `ProducerTemplate`.  Frequently while writing integration scripts, the 
routing is easy with straight groovy, but the endpoints are still hard
to manage.  Although `camel-glite` focusses mostly on just consuming and
sending data to endpoints, it still can run routes.


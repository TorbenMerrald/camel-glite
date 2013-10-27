camel-glite
-----------

I frequently find myself fighting between writing simple groovy scripts for integration or batch jobs, vs using 
something more powerful like apache camel.  This project aims to bring the best of these two worlds together to
make camel scripting easy.

Download
--------
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
-----------

Most all functionality comes from the class `CamelScript` which has
methods for receiving and sending data to any supported camel endpoint.
Many functions in this class revolve around wrapping camel's `ConsumerTemplate`
and `ProducerTemplate`.  Frequently while writing integration scripts, the 
routing is easy with straight groovy, but the endpoints are still hard
to manage.  Although `camel-glite` focusses mostly on just consuming and
sending data to endpoints, it still can run routes.

Examples
--------

The following examples are based on this [blog](http://saltnlight5.blogspot.com/2012/08/getting-started-with-apache-camel-using.html)

Basic timer example

```groovy
@Grab("com.github.camel-glite:camel-glite:0.4.1")
@Grab('org.slf4j:slf4j-simple:1.6.6')
@GrabResolver(name = 'camel-glite', root = 'http://jcenter.bintray.com/')
import camelscript.CamelGLite

new CamelGLite().with {
    consumeForever("timer://jdkTimer?period=1000") {
        send("log://camelLogger?level=INFO")
    }
}
```

will ouput something similar to

```
498 [main] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) is starting
925 [main] INFO org.apache.camel.management.ManagementStrategyFactory - JMX enabled.
1258 [main] INFO org.apache.camel.impl.converter.DefaultTypeConverter - Loaded 172 type converters
1282 [main] INFO org.apache.camel.management.DefaultManagementLifecycleStrategy - Load performance statistics enabled.
1326 [main] INFO org.apache.camel.impl.DefaultCamelContext - Total 0 routes, of which 0 is started.
1353 [main] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) started in 0.833 seconds
2837 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
3535 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
4534 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
... (ctrl-C)
14569 [Thread-1] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) is shutting down
14574 [Thread-1] INFO org.apache.camel.impl.DefaultCamelContext - Uptime 14.082 seconds
14574 [Thread-1] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) is shutdown in 0.005 seconds
```

Timer example that prints hello

```groovy
/**
 * a redo of this <a href:"http://saltnlight5.blogspot.com/2012/08/getting-started-with-apache-camel-using.html">blog</a>
 * using CamelGLite
 */
@Grab("com.github.camel-glite:camel-glite:0.5.RC1")
@Grab('org.slf4j:slf4j-simple:1.6.6')
@GrabResolver(name = 'camel-glite', root = 'http://jcenter.bintray.com/')
import camelscript.CamelGLite

new CamelGLite().with {
    consumeForever("timer://jdkTimer?period=1000") {
        send("log://camelLogger?level=INFO").process {
            println "hello world"
        }
    }
}
```

will output something similar to

```
312 [main] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) is starting
409 [main] INFO org.apache.camel.management.ManagementStrategyFactory - JMX enabled.
634 [main] INFO org.apache.camel.impl.converter.DefaultTypeConverter - Loaded 172 type converters
658 [main] INFO org.apache.camel.management.DefaultManagementLifecycleStrategy - Load performance statistics enabled.
700 [main] INFO org.apache.camel.impl.DefaultCamelContext - Total 0 routes, of which 0 is started.
703 [main] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) started in 0.392 seconds
1956 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
hello world
2806 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
hello world
3806 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
hello world
4806 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
hello world
5807 [main] INFO camelLogger - Exchange[ExchangePattern:InOnly, BodyType:null, Body:[Body is null]]
hello world
^C5914 [Thread-1] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) is shutting down
5918 [Thread-1] INFO org.apache.camel.impl.DefaultCamelContext - Uptime 5.610 seconds
5918 [Thread-1] INFO org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.11.0 (CamelContext: camel-1) is shutdown in 0.004 seconds
```

Something more useful might be consuming from a jms endpoint.  Apache camel can consume from most mainstream messaging
systems, so adapting this to other messaging implementations should be easy.

```groovy
@Grab("com.github.camel-glite:camel-glite:0.4.1")
@Grab('org.slf4j:slf4j-simple:1.6.6')
@Grab('org.apache.activemq:activemq-camel:5.6.0')
@Grab('org.apache.activemq:activemq-core:5.6.0')
//this needs to be explicitly set since activemq-camel will use a different version
@Grab('org.apache.camel:camel-jms:2.11.0')
@GrabResolver(name = 'camel-glite', root = 'http://jcenter.bintray.com/')
import camelscript.CamelGLite
import org.apache.activemq.camel.component.ActiveMQComponent

def glite = new CamelGLite()
glite.camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"))
println "starting jms consumption"
def camelThread = Thread.start {
    glite.consumeTillDone("activemq:queue:foo", 3000) { String greeting ->
        println "Hi number [$greeting]"
    }
}

println "sending data to foo"
(1..10).each{ glite.send("activemq:queue:foo", it) }
camelThread.join()
```

This will print numbers 1 through ten.  Notice the `consumeTillDone` function.  This will consume messages from the
endpoint till there is nothing left.  The `3000` indicates that if nothing has been received for 3 seconds, then the
endpoint is considered complete.

CamelGLite Methods
------------------



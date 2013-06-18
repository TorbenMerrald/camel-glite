package camelscript

import org.apache.camel.builder.RouteBuilder

class CamelGLiteRouteBuilder extends RouteBuilder{

    Closure closure

    @Override
    void configure() throws Exception {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call(this)
    }
}

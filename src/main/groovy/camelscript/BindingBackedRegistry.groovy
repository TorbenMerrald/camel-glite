package camelscript

import org.apache.camel.spi.Registry

/**
 * Created with IntelliJ IDEA on 6/4/13
 * @author Tommy Barker
 */
class BindingBackedRegistry implements Registry, Map {

    final Binding binding

    @Delegate
    Map variables

    BindingBackedRegistry(Binding binding) {
        this.binding = binding
        this.variables = binding.variables
    }

    @Override
    Object lookup(String name) {
        binding.variables[name]
    }

    @Override
    def <T> T lookup(String name, Class<T> type) {
        binding.variables.find { key, value -> name == key && type.isAssignableFrom(value.getClass()) } ?
            lookup(name).asType(type) : null
    }

    @Override
    def <T> Map<String, T> lookupByType(Class<T> type) {
        def result = [:]
        binding.variables.each { name, value ->
            if (value.getClass().isAssignableFrom(type)) {
                result[name] = value.asType(type)
            }
        }

        return result
    }
}
